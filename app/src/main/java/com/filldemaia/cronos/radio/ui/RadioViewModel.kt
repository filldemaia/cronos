package com.filldemaia.cronos.radio.ui

import android.app.Application
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filldemaia.cronos.radio.RadioApp
import com.filldemaia.cronos.radio.data.model.AlarmLog
import com.filldemaia.cronos.radio.data.model.AppSettings
import com.filldemaia.cronos.radio.data.model.ReliabilityReport
import com.filldemaia.cronos.radio.data.model.Schedule
import com.filldemaia.cronos.radio.data.model.Station
import com.filldemaia.cronos.radio.domain.schedule.NextOccurrenceCalculator
import com.filldemaia.cronos.radio.playback.PlaybackCommands
import com.filldemaia.cronos.radio.playback.PlaybackService
import com.filldemaia.cronos.radio.playback.PlaybackSnapshot
import com.filldemaia.cronos.radio.util.PermissionManager
import java.time.Instant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RadioApp
    private val container = app.container

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val stations: StateFlow<List<Station>> = container.stationRepository.stations
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val schedules: StateFlow<List<Schedule>> = container.scheduleRepository.schedules
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val logs: StateFlow<List<AlarmLog>> = container.reliabilityRepository.logs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val onboarded: StateFlow<Boolean?> = container.reliabilityRepository.onboarded
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val oemAcknowledged: StateFlow<Boolean> = container.reliabilityRepository.oemAcknowledged
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val playback: StateFlow<PlaybackSnapshot> = container.playbackStateStore.state

    private val _reliability = MutableStateFlow<ReliabilityReport?>(null)
    val reliability: StateFlow<ReliabilityReport?> = _reliability.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    private var latestSettings = AppSettings()

    init {
        viewModelScope.launch {
            settings.collect { latestSettings = it }
        }
        refreshReliability()
    }

    // ------------------------------------------------------------------
    // Reproducció
    // ------------------------------------------------------------------

    fun playStation(station: Station) {
        warnIfMobileData()
        val intent = Intent(app, PlaybackService::class.java).apply {
            action = PlaybackCommands.ACTION_PLAY_STATION
            putExtra(PlaybackCommands.EXTRA_STATION_ID, station.id)
        }
        ContextCompat.startForegroundService(app, intent)
    }

    fun playLastStation() {
        val station = stations.value.firstOrNull { it.id == latestSettings.lastStationId }
            ?: stations.value.firstOrNull()
        if (station == null) {
            emit("Encara no hi ha cap emissora disponible.")
        } else {
            playStation(station)
        }
    }

    fun togglePlayPause() {
        val state = playback.value
        if (state.isLocalAlarm) return
        if (!PlaybackService.isRunning) {
            playLastStation()
        } else {
            sendCommand(PlaybackCommands.ACTION_TOGGLE_PLAY_PAUSE)
        }
    }

    fun stop() {
        if (PlaybackService.isRunning) sendCommand(PlaybackCommands.ACTION_STOP)
    }

    fun stopLocalAlarm() {
        if (PlaybackService.isRunning) sendCommand(PlaybackCommands.ACTION_STOP_LOCAL_ALARM)
    }

    fun seekRelative(seconds: Int) {
        if (!PlaybackService.isRunning) return
        sendCommand(PlaybackCommands.ACTION_SEEK_RELATIVE) {
            putExtra(PlaybackCommands.EXTRA_SEEK_US, seconds * 1_000_000L)
        }
    }

    fun seekToUs(positionUs: Long) {
        if (!PlaybackService.isRunning) return
        sendCommand(PlaybackCommands.ACTION_SEEK_TO) {
            putExtra(PlaybackCommands.EXTRA_SEEK_US, positionUs)
        }
    }

    fun goLive() {
        if (!PlaybackService.isRunning) return
        sendCommand(PlaybackCommands.ACTION_GO_LIVE)
    }

    fun setSleepTimer(minutes: Int?) {
        if (!PlaybackService.isRunning) return
        sendCommand(PlaybackCommands.ACTION_SLEEP_TIMER) {
            putExtra(PlaybackCommands.EXTRA_SLEEP_MINUTES, minutes ?: 0)
        }
    }

    // ------------------------------------------------------------------
    // Programacions
    // ------------------------------------------------------------------

    fun saveSchedule(schedule: Schedule) {
        viewModelScope.launch {
            container.scheduleRepository.save(schedule)
            if (schedule.enabled) {
                val next = NextOccurrenceCalculator.next(schedule, Instant.now())
                if (next == null) {
                    emit("La programació no té cap pròxima execució vàlida.")
                } else if (!container.alarmScheduler.arm(schedule, next)) {
                    emit("No s'ha pogut armar l'alarma. Revisa el permís d'alarmes exactes.")
                }
            } else {
                container.alarmScheduler.cancel(schedule.id)
            }
            refreshReliability()
        }
    }

    fun setScheduleEnabled(schedule: Schedule, enabled: Boolean) {
        viewModelScope.launch {
            val updated = schedule.copy(enabled = enabled)
            container.scheduleRepository.save(updated)
            if (enabled) {
                val next = NextOccurrenceCalculator.next(updated, Instant.now())
                if (next != null && !container.alarmScheduler.arm(updated, next)) {
                    emit("No s'ha pogut armar l'alarma. Revisa el permís d'alarmes exactes.")
                }
            } else {
                container.alarmScheduler.cancel(updated.id)
            }
            refreshReliability()
        }
    }

    fun skipTomorrow(schedule: Schedule) {
        if (schedule.isOneShot || !schedule.enabled) return
        viewModelScope.launch {
            val success = runCatching {
                com.filldemaia.cronos.radio.domain.schedule.SkipTomorrowOperation.execute(
                    current = container.scheduleRepository.find(schedule.id),
                    now = Instant.now(),
                    zone = java.time.ZoneId.systemDefault(),
                    save = { container.scheduleRepository.save(it) },
                    arm = { updated, next -> container.alarmScheduler.arm(updated, next) }
                )
            }.getOrElse {
                if (it is kotlinx.coroutines.CancellationException) throw it
                false
            }
            emit(if (success) "L'alarma no sonarà demà; després continuarà segons els dies programats."
                else "No s'ha pogut desar i rearmar l'alarma. Revisa la programació.")
            refreshReliability()
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            container.alarmScheduler.cancel(schedule.id)
            container.scheduleRepository.delete(schedule.id)
            refreshReliability()
        }
    }

    fun testNow(schedule: Schedule) {
        if (!PermissionManager.exactAlarmGranted(app)) {
            emit("Concedeix el permís d'alarmes exactes abans de fer la prova.")
            return
        }
        val ok = container.alarmScheduler.scheduleTestNow(schedule)
        emit(
            if (ok) {
                "Prova real programada per d'aquí a 5 segons. Passa pel mateix receptor i servei que una alarma."
            } else {
                "No s'ha pogut programar la prova."
            }
        )
    }

    fun armAllNow() {
        viewModelScope.launch {
            val summary = runCatching {
                container.alarmScheduler.armAll()
            }.getOrNull()
            if (summary == null) {
                emit("No s'han pogut rearmar les alarmes.")
            } else {
                emit(
                    "Alarmes armades: ${summary.armed}" +
                        if (summary.failed > 0) " · fallades: ${summary.failed}" else ""
                )
            }
            refreshReliability()
        }
    }

    // ------------------------------------------------------------------
    // Fiabilitat, permisos i ajustos
    // ------------------------------------------------------------------

    fun refreshReliability() {
        viewModelScope.launch {
            runCatching {
                val report = container.reliabilityChecker.check()
                _reliability.value = report
                container.notificationHelper.updateReliability(report, report.activeScheduleCount)
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            container.reliabilityRepository.setOnboarded(true)
            runCatching { container.alarmScheduler.armAll() }
            refreshReliability()
        }
    }

    fun acknowledgeOem(acknowledged: Boolean) {
        viewModelScope.launch {
            container.reliabilityRepository.setOemAcknowledged(acknowledged)
            refreshReliability()
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            container.settingsRepository.update(transform)
            refreshReliability()
        }
    }

    fun addCustomStation(name: String, url: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                container.stationRepository.addCustom(name, url)
            }.onSuccess {
                onResult(null)
                refreshReliability()
            }.onFailure {
                onResult(it.message ?: "No s'ha pogut afegir l'emissora.")
            }
        }
    }

    fun removeCustomStation(station: Station) {
        viewModelScope.launch {
            container.stationRepository.removeCustom(station.id)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            container.reliabilityRepository.clearLogs()
        }
    }

    fun emit(message: String) {
        _messages.tryEmit(message)
    }

    // ------------------------------------------------------------------
    // Auxiliars
    // ------------------------------------------------------------------

    private fun sendCommand(action: String, configure: Intent.() -> Unit = {}) {
        val intent = Intent(app, PlaybackService::class.java).apply {
            this.action = action
            configure()
        }
        runCatching { app.startService(intent) }
    }

    private fun warnIfMobileData() {
        if (!latestSettings.warnOnMobileData) return
        val manager = app.getSystemService(ConnectivityManager::class.java) ?: return
        val network = manager.activeNetwork ?: return
        val capabilities = manager.getNetworkCapabilities(network) ?: return
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        ) {
            emit("Estàs escoltant amb dades mòbils. El buffer de disc continua funcionant.")
        }
    }
}
