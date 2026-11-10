package com.filldemaia.cronos.radio.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.UserManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.annotation.OptIn as AndroidXOptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media.MediaBrowserServiceCompat
import com.filldemaia.cronos.radio.RadioApp
import com.filldemaia.cronos.radio.data.local.AppJson
import com.filldemaia.cronos.radio.data.model.AlarmLog
import com.filldemaia.cronos.radio.data.model.AlarmOutcome
import com.filldemaia.cronos.radio.data.model.AppSettings
import com.filldemaia.cronos.radio.data.model.Schedule
import com.filldemaia.cronos.radio.data.model.Station
import com.filldemaia.cronos.radio.di.AppContainer
import com.filldemaia.cronos.radio.domain.playback.AttemptKind
import com.filldemaia.cronos.radio.domain.playback.AudioFrameIndexer
import com.filldemaia.cronos.radio.domain.playback.DiskRingBuffer
import com.filldemaia.cronos.radio.domain.playback.FallbackPlanner
import com.filldemaia.cronos.radio.domain.playback.FramePoint
import com.filldemaia.cronos.radio.domain.playback.FrameTimeline
import com.filldemaia.cronos.radio.domain.playback.PlaybackAttempt
import com.filldemaia.cronos.MainActivity
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

/**
 * Servei de reproducció en primer pla.
 *
 * És alhora un MediaBrowserService (notificació, pantalla de bloqueig,
 * Bluetooth, Android Auto bàsic) i el motor que grava el directe al disc per
 * permetre el time-shift. Les alarmes programades entren pel mateix camí que
 * la reproducció manual (BroadcastReceiver → startForegroundService).
 */
@AndroidXOptIn(UnstableApi::class)
class PlaybackService : MediaBrowserServiceCompat() {

    private lateinit var container: AppContainer
    private lateinit var notifications: NotificationHelper
    private lateinit var stateStore: PlaybackStateStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val io = Dispatchers.IO
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private lateinit var mediaSession: MediaSessionCompat
    private var player: ExoPlayer? = null
    private var playerUsage: Int = -1
    private var localPlayer: MediaPlayer? = null

    private var foregroundStarted = false
    private var activeRequest: PlaybackRequest? = null
    private var currentMode = PlaybackMode.IDLE
    private var playbackBaseTimeUs = 0L
    private var latestSettings = AppSettings()

    private var buffer: DiskRingBuffer? = null
    private var timeline: FrameTimeline? = null
    private var indexer: AudioFrameIndexer? = null
    private var recorder: StreamRecorder? = null

    private var chainJob: Job? = null
    private var watchdogJob: Job? = null
    private var autoStopJob: Job? = null
    private var sleepJob: Job? = null
    private var fadeJob: Job? = null
    private var tickerJob: Job? = null
    private var alarmSafetyJob: Job? = null

    private var sessionGeneration = 0
    private var activeAttemptId = 0
    private var attemptFailure: CompletableDeferred<Throwable>? = null
    private var lastLoggedOutcome: AlarmOutcome? = null
    private var publishCounter = 0L
    private var switchingSource = false
    private var hlsBaselineOffsetMs: Long? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private data class PlaybackRequest(
        val station: Station,
        val fallbackStations: List<Station>,
        val schedule: Schedule?,
        val isScheduled: Boolean,
        val isTest: Boolean,
        val plannedAtMillis: Long?,
        val volume: Float,
        val fadeSeconds: Int,
        val stopAfterMinutes: Int?,
        val startedAtMillis: Long = System.currentTimeMillis()
    )

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (activeRequest == null) return
            if (switchingSource) return
            when (playbackState) {
                Player.STATE_BUFFERING, Player.STATE_IDLE -> refreshProgress(forcePublish = true)
                Player.STATE_ENDED -> failCurrentAttempt(IOException("El directe s'ha acabat"))
                Player.STATE_READY -> refreshProgress(forcePublish = true)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (activeRequest == null || switchingSource) return
            if (isBufferOverrun(error)) {
                // El buffer ha sobreescrit la posició de pausa. No cal caure a
                // una altra emissora: reancorem al fotograma més antic.
                reanchorToOldest()
                return
            }
            failCurrentAttempt(error)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Un canvi de font intern (salt a la finestra) atura el player un
            // instant; no publiquem aquest fals «pausat» ni fem parpellejar la
            // notificació.
            if (activeRequest == null || switchingSource) return
            refreshProgress(forcePublish = true)
        }
    }

    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            ensureForeground()
            resumePlayback()
        }

        override fun onPause() {
            pausePlayback()
        }

        override fun onStop() {
            stopAndFinish()
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            ensureForeground()
            playStationById(mediaId)
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            ensureForeground()
            val needle = query?.trim()?.lowercase().orEmpty()
            serviceScope.launch {
                val stations = withContext(io) {
                    runCatching { container.stationRepository.current() }.getOrDefault(emptyList())
                }
                val match = if (needle.isEmpty()) {
                    null
                } else {
                    stations.firstOrNull { station ->
                        val name = station.name.lowercase()
                        name.contains(needle) ||
                            needle.contains(name) ||
                            station.tagline.lowercase().contains(needle)
                    }
                }
                playStationById(match?.id)
            }
        }

        override fun onSeekTo(pos: Long) {
            if (currentMode == PlaybackMode.HLS_LIVE) {
                player?.seekTo(pos)
            } else {
                val windowStart = stateStore.state.value.windowStartUs
                seekToWindow(windowStart + pos * 1000L)
            }
        }

        override fun onFastForward() {
            seekRelative(latestSettings.skipForwardSeconds * 1_000_000L)
        }

        override fun onRewind() {
            seekRelative(-latestSettings.skipBackSeconds * 1_000_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        container = (application as RadioApp).container
        notifications = container.notificationHelper
        stateStore = container.playbackStateStore

        mediaSession = MediaSessionCompat(this, "Cronos").apply {
            setCallback(sessionCallback, Handler(Looper.getMainLooper()))
            setSessionActivity(contentIntent())
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }
        setSessionToken(mediaSession.sessionToken)
        isRunning = true
        startTicker()

        // Manté els salts del reproductor i de la notificació sincronitzats amb
        // els ajustos sense haver de reiniciar el servei. En direct boot
        // DataStore encara no és llegible: no ha de fer caure el servei.
        serviceScope.launch {
            runCatching {
                container.settingsRepository.settings.collect { settings ->
                    latestSettings = settings
                    updateState {
                        it.copy(
                            skipBackSeconds = settings.skipBackSeconds,
                            skipForwardSeconds = settings.skipForwardSeconds
                        )
                    }
                    publish(notify = foregroundStarted)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Sempre hem de ser en primer pla abans de fer cap feina llarga.
        ensureForeground()

        when (intent?.action) {
            PlaybackCommands.ACTION_START_SCHEDULE -> handleScheduleStart(intent)
            PlaybackCommands.ACTION_PLAY_STATION -> {
                val id = intent.getStringExtra(PlaybackCommands.EXTRA_STATION_ID)
                playStationById(id)
            }
            PlaybackCommands.ACTION_TOGGLE_PLAY_PAUSE -> togglePlayPause()
            PlaybackCommands.ACTION_PLAY -> resumePlayback()
            PlaybackCommands.ACTION_PAUSE -> pausePlayback()
            PlaybackCommands.ACTION_SEEK_RELATIVE -> {
                val delta = intent.getLongExtra(PlaybackCommands.EXTRA_SEEK_US, 0L)
                seekRelative(delta)
            }
            PlaybackCommands.ACTION_SEEK_TO -> {
                seekToWindow(intent.getLongExtra(PlaybackCommands.EXTRA_SEEK_US, 0L))
            }
            PlaybackCommands.ACTION_GO_LIVE -> goLive()
            PlaybackCommands.ACTION_STOP,
            PlaybackCommands.ACTION_STOP_LOCAL_ALARM -> stopAndFinish()
            PlaybackCommands.ACTION_SLEEP_TIMER -> {
                val minutes = intent.getIntExtra(PlaybackCommands.EXTRA_SLEEP_MINUTES, 0)
                setSleepTimer(if (minutes > 0) minutes else null)
            }
            Intent.ACTION_MEDIA_BUTTON -> handleMediaButton(intent)
            null -> if (activeRequest == null) stopSelf()
            else -> Unit
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Una alarma o una reproducció en curs no s'ha d'aturar en treure l'app
        // de la llista de recents.
        if (activeRequest == null) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isRunning = false
        sessionGeneration++
        tickerJob?.cancel()
        serviceScope.cancel()
        stopRecorderAndBuffer()
        stopLocalTone()
        releaseLocks()
        runCatching { player?.release() }
        player = null
        mediaSession.isActive = false
        mediaSession.release()
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // MediaBrowserService (Android Auto / assistents)
    // ------------------------------------------------------------------

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot = BrowserRoot(ROOT_ID, null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId != ROOT_ID) {
            result.sendResult(emptyList())
            return
        }
        result.detach()
        serviceScope.launch {
            val stations = withContext(io) {
                runCatching { container.stationRepository.current() }.getOrDefault(emptyList())
            }
            result.sendResult(
                stations.map { station ->
                    val description = MediaDescriptionCompat.Builder()
                        .setMediaId(station.id)
                        .setTitle(station.name)
                        .setSubtitle(station.tagline)
                        .build()
                    MediaBrowserCompat.MediaItem(
                        description,
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                    )
                }
            )
        }
    }

    // ------------------------------------------------------------------
    // Entrada d'accions
    // ------------------------------------------------------------------

    private fun handleScheduleStart(intent: Intent) {
        val raw = intent.getStringExtra(PlaybackCommands.EXTRA_SCHEDULE_JSON)
        val schedule = raw?.let {
            runCatching { AppJson.decodeFromString<Schedule>(it) }.getOrNull()
        }
        if (schedule == null) {
            stopAndFinish()
            return
        }
        val plannedAt = intent.getLongExtra(
            PlaybackCommands.EXTRA_PLANNED_AT,
            System.currentTimeMillis()
        )
        val isTest = intent.getBooleanExtra(PlaybackCommands.EXTRA_IS_TEST, false)

        // Una alarma no pot sonar en silenci perquè el volum d'alarma estigui baix.
        ensureAlarmVolume(schedule.volume)
        // El receptor allibera el seu wake lock al cap de pocs segons: el servei
        // n'ha de tenir un des del primer moment.
        acquireLocks()

        stateStore.set(
            PlaybackSnapshot(
                isBuffering = true,
                isScheduled = true,
                isTest = isTest,
                scheduledLabel = buildString {
                    append(schedule.timeLabel())
                    if (!schedule.isOneShot) append(" · ${schedule.daysLabel()}")
                },
                startedAtMillis = System.currentTimeMillis(),
                skipBackSeconds = latestSettings.skipBackSeconds,
                skipForwardSeconds = latestSettings.skipForwardSeconds
            )
        )
        publish(notify = true)

        serviceScope.launch {
            try {
                val station = resolveStation(schedule.stationId)
                val fallbacks = schedule.fallbackStationIds
                    .filter { it != schedule.stationId }
                    .mapNotNull { resolveStation(it) }
                val primary = station ?: Station(
                    id = schedule.stationId,
                    name = "Emissora no trobada",
                    tagline = "Cal revisar la programació",
                    urls = emptyList()
                )
                beginSession(
                    PlaybackRequest(
                        station = primary,
                        fallbackStations = fallbacks,
                        schedule = schedule,
                        isScheduled = true,
                        isTest = isTest,
                        plannedAtMillis = plannedAt,
                        volume = schedule.volume,
                        fadeSeconds = schedule.fadeInSeconds,
                        stopAfterMinutes = schedule.stopAfterMinutes
                    )
                )
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                // Cap error de preparació no pot deixar l'alarma en silenci.
                playLocalAlarm(
                    "No s'ha pogut preparar la reproducció (${error.message ?: "error"}). " +
                    "To d'alarma local actiu.", schedule
                )
            }
        }
    }

    /**
     * Resol l'emissora fins i tot abans del desbloqueig: DataStore (CE) no és
     * llegible, però el catàleg de l'APK i la còpia protegida sí.
     */
    private suspend fun resolveStation(id: String): Station? {
        if (isUserUnlocked()) {
            runCatching { container.stationRepository.find(id) }.getOrNull()?.let { return it }
        }
        runCatching { container.stationRepository.builtInStations() }
            .getOrNull()
            ?.firstOrNull { it.id == id }
            ?.let { return it }
        return withContext(io) {
            container.deviceProtectedStore.loadStations().firstOrNull { it.id == id }
        }
    }

    private fun playStationById(stationId: String?) {
        serviceScope.launch {
            val settings = withContext(io) { container.settingsRepository.current() }
            val requested = stationId ?: settings.lastStationId
            val station = withContext(io) {
                container.stationRepository.find(requested)
                    ?: container.stationRepository.current().firstOrNull()
            }
            if (station == null) {
                updateState {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = "No hi ha cap emissora disponible."
                    )
                }
                return@launch
            }
            withContext(io) {
                runCatching {
                    container.settingsRepository.update { it.copy(lastStationId = station.id) }
                }
            }
            beginSession(
                PlaybackRequest(
                    station = station,
                    fallbackStations = emptyList(),
                    schedule = null,
                    isScheduled = false,
                    isTest = false,
                    plannedAtMillis = null,
                    volume = 1f,
                    fadeSeconds = 0,
                    stopAfterMinutes = null
                )
            )
        }
    }

    private fun handleMediaButton(intent: Intent) {
        @Suppress("DEPRECATION")
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return
        if (event.action != KeyEvent.ACTION_DOWN) return
        when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK -> togglePlayPause()
            KeyEvent.KEYCODE_MEDIA_PLAY -> resumePlayback()
            KeyEvent.KEYCODE_MEDIA_PAUSE -> pausePlayback()
            KeyEvent.KEYCODE_MEDIA_STOP -> stopAndFinish()
        }
    }

    // ------------------------------------------------------------------
    // Cicle de sessió
    // ------------------------------------------------------------------

    private fun beginSession(request: PlaybackRequest) {
        sessionGeneration++
        val token = sessionGeneration
        endPlaybackResources()

        activeRequest = request
        lastLoggedOutcome = null
        playbackBaseTimeUs = 0L
        attemptFailure = null

        ensurePlayer(
            if (request.isScheduled) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
        )
        acquireLocks()

        currentMode = PlaybackMode.TIMESHIFT
        stateStore.set(
            PlaybackSnapshot(
                station = request.station,
                mode = PlaybackMode.TIMESHIFT,
                isPlaying = false,
                isBuffering = true,
                isScheduled = request.isScheduled,
                isTest = request.isTest,
                scheduledLabel = request.schedule?.let {
                    buildString {
                        append(it.timeLabel())
                        if (!it.isOneShot) append(" · ${it.daysLabel()}")
                    }
                },
                startedAtMillis = request.startedAtMillis,
                volume = request.volume,
                stopAfterAtMillis = request.stopAfterMinutes?.let {
                    request.startedAtMillis + it * 60_000L
                },
                skipBackSeconds = latestSettings.skipBackSeconds,
                skipForwardSeconds = latestSettings.skipForwardSeconds
            )
        )
        publish(notify = true)
        startAutoStop(request)
        startChain(request, token)
        startAlarmSafety(request, token)
    }

    /**
     * Xarxa de seguretat de les alarmes: si al cap de [ALARM_SAFETY_TIMEOUT_MS]
     * no hi ha so, sona el to local. Així una xarxa lenta o una cadena de
     * reintents llarga no pot deixar l'usuari sense despertador.
     */
    private fun startAlarmSafety(request: PlaybackRequest, token: Int) {
        alarmSafetyJob?.cancel()
        if (!request.isScheduled) return
        alarmSafetyJob = serviceScope.launch {
            delay(ALARM_SAFETY_TIMEOUT_MS)
            if (!isCurrent(request, token)) return@launch
            if (stateStore.state.value.isPlaying) return@launch
            playLocalAlarm(
                "El directe no ha respost a temps. To d'alarma local actiu."
            )
        }
    }

    private fun startChain(request: PlaybackRequest, token: Int) {
        chainJob?.cancel()
        chainJob = serviceScope.launch {
            val attempts = FallbackPlanner.plan(
                primary = request.station,
                fallbackStations = request.fallbackStations,
                retriesPerUrl = if (request.isScheduled) 3 else 2,
                baseBackoffMs = 1_500L,
                includeLocalTone = request.isScheduled
            )
            var index = 0
            while (isActive && isCurrent(request, token)) {
                if (index >= attempts.size) {
                    handleChainExhausted(request)
                    return@launch
                }
                val attempt = attempts[index++]
                if (attempt.delayBeforeMs > 0) delay(attempt.delayBeforeMs)
                if (!isCurrent(request, token)) return@launch

                if (attempt.kind == AttemptKind.LOCAL_TONE) {
                    playLocalAlarm(
                        "Cap emissora no ha respost. El to d'alarma continuarà fins que l'aturis."
                    )
                    return@launch
                }

                val succeeded = runCatching { tryAttempt(request, attempt) }
                    .onFailure {
                        // Un error inesperat (per exemple, disc ple en crear el
                        // buffer) no ha de matar la cadena de seguretat.
                        stopRecorderAndBuffer()
                    }
                    .getOrDefault(false)
                if (succeeded) {
                    updateState {
                        it.copy(
                            isBuffering = false,
                            isPlaying = true,
                            errorMessage = null,
                            sourceLabel = attempt.label,
                            isFallbackStation = attempt.kind == AttemptKind.FALLBACK_STATION
                        )
                    }
                    publish(notify = true)
                    val failure = attemptFailure?.await()
                    if (!isCurrent(request, token)) return@launch
                    updateState {
                        it.copy(
                            isBuffering = true,
                            isPlaying = false,
                            errorMessage = failure?.message ?: "S'ha perdut el directe."
                        )
                    }
                    publish(notify = true)
                    stopRecorderAndBuffer()
                    delay(600)
                }
            }
        }
    }

    private fun isCurrent(request: PlaybackRequest, token: Int): Boolean =
        activeRequest === request && sessionGeneration == token

    private suspend fun tryAttempt(request: PlaybackRequest, attempt: PlaybackAttempt): Boolean {
        val station = attempt.station ?: return false
        val url = attempt.url ?: return false
        val attemptId = ++activeAttemptId
        attemptFailure = CompletableDeferred()

        hlsBaselineOffsetMs = null
        updateState {
            it.copy(
                station = station,
                mode = if (station.isHls(url)) PlaybackMode.HLS_LIVE else PlaybackMode.TIMESHIFT,
                isBuffering = true,
                isPlaying = false,
                isFallbackStation = attempt.kind == AttemptKind.FALLBACK_STATION,
                sourceLabel = attempt.label,
                errorMessage = null,
                windowStartUs = 0L,
                liveEdgeUs = 0L,
                bufferDurationUs = 0L,
                positionUs = 0L,
                hlsLiveOffsetMs = null
            )
        }
        publish(notify = true)

        return if (station.isHls(url)) {
            startHls(request, attempt, url, attemptId)
        } else {
            startProgressive(request, attempt, url, attemptId)
        }
    }

    private suspend fun startProgressive(
        request: PlaybackRequest,
        attempt: PlaybackAttempt,
        url: String,
        attemptId: Int
    ): Boolean {
        stopRecorderAndBuffer()
        val settings = if (isUserUnlocked()) {
            runCatching { withContext(io) { container.settingsRepository.current() } }.getOrNull()
        } else {
            null
        } ?: withContext(io) { container.deviceProtectedStore.loadSettings() }
            ?: latestSettings
        // Obrir i preassignar un fitxer de fins a ~1,5 GB no s'ha de fer al fil principal.
        val extent = withContext(io) {
            runCatching {
                DiskRingBuffer(
                    file = File(bufferDirectory(), "timeshift.buffer"),
                    capacityBytes = capacityBytes(request.station, settings)
                )
            }.getOrNull()
        } ?: return false
        val newTimeline = FrameTimeline()
        val newIndexer = AudioFrameIndexer().apply { reset(0) }
        buffer = extent
        timeline = newTimeline
        indexer = newIndexer

        val newRecorder = StreamRecorder(
            url = url,
            buffer = extent,
            timeline = newTimeline,
            indexer = newIndexer,
            client = httpClient,
            onFailure = { error -> serviceScope.launch { failAttempt(attemptId, error) } },
            onProgress = {}
        )
        recorder = newRecorder
        newRecorder.start()

        val gotAudio = waitForBufferedAudio(newRecorder, newTimeline)
        if (!gotAudio) {
            newRecorder.stop()
            stopRecorderAndBuffer()
            return false
        }

        val frames = newTimeline.snapshot()
        val oldest = extent.oldestOffset
        val startFrame = frames.firstOrNull { it.offset >= oldest } ?: frames.lastOrNull()
        if (startFrame == null) {
            newRecorder.stop()
            stopRecorderAndBuffer()
            return false
        }

        if (!setPlayerSourceAt(startFrame, playWhenReady = true)) {
            newRecorder.stop()
            stopRecorderAndBuffer()
            return false
        }
        playbackBaseTimeUs = startFrame.startTimeUs
        currentMode = PlaybackMode.TIMESHIFT
        startWatchdog(attemptId)
        applyVolume(request.volume, request.fadeSeconds)

        if (!waitForPlayerReady()) {
            newRecorder.stop()
            stopRecorderAndBuffer()
            return false
        }
        onAudible(attempt)
        return true
    }

    private suspend fun startHls(
        request: PlaybackRequest,
        attempt: PlaybackAttempt,
        url: String,
        attemptId: Int
    ): Boolean {
        stopRecorderAndBuffer()
        val p = ensurePlayer(
            if (request.isScheduled) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
        )
        val factory = OkHttpDataSource.Factory(httpClient)
            .setUserAgent("Cronos/1.0 (Android)")
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "0"))
        val source: MediaSource = HlsMediaSource.Factory(factory)
            .createMediaSource(MediaItem.fromUri(url))
        switchingSource = true
        runCatching {
            p.stop()
            p.clearMediaItems()
            p.setMediaSource(source)
            p.prepare()
            p.playWhenReady = true
        }.onFailure {
            switchingSource = false
            return false
        }
        switchingSource = false
        currentMode = PlaybackMode.HLS_LIVE
        startWatchdog(attemptId)
        applyVolume(request.volume, request.fadeSeconds)
        if (!waitForPlayerReady()) {
            p.stop()
            return false
        }
        onAudible(attempt)
        return true
    }

    private suspend fun waitForBufferedAudio(
        recorder: StreamRecorder,
        timeline: FrameTimeline
    ): Boolean {
        val deadline = SystemClock.elapsedRealtime() + START_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (recorder.lastError != null) return false
            if (timeline.latestTimeUs() >= MIN_START_BUFFER_US && timeline.size() >= 2) return true
            delay(100)
        }
        return timeline.latestTimeUs() >= MIN_START_BUFFER_US / 2
    }

    private suspend fun waitForPlayerReady(): Boolean {
        val p = player ?: return false
        val deadline = SystemClock.elapsedRealtime() + READY_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (attemptFailure?.isCompleted == true) return false
            when (p.playbackState) {
                Player.STATE_READY -> return true
                Player.STATE_BUFFERING -> Unit
                Player.STATE_ENDED, Player.STATE_IDLE -> return false
            }
            if (p.currentPosition > 0) return true
            delay(120)
        }
        return p.playbackState == Player.STATE_READY
    }

    private fun onAudible(attempt: PlaybackAttempt) {
        alarmSafetyJob?.cancel()
        alarmSafetyJob = null
        val request = activeRequest ?: return
        val outcome = if (attempt.kind == AttemptKind.FALLBACK_STATION) {
            AlarmOutcome.FALLBACK_STATION
        } else {
            AlarmOutcome.SUCCESS
        }
        logOutcome(
            request,
            outcome,
            "So actiu amb ${attempt.label}" +
                (request.plannedAtMillis?.let {
                    " · desviació ${(System.currentTimeMillis() - it).coerceAtLeast(0)} ms"
                } ?: "")
        )
    }

    private fun handleChainExhausted(request: PlaybackRequest) {
        if (request.isScheduled) {
            serviceScope.launch {
                playLocalAlarm("No s'ha pogut reproduir cap font. To d'alarma local actiu.")
            }
            return
        }
        updateState {
            it.copy(
                isBuffering = false,
                isPlaying = false,
                errorMessage = "No s'ha pogut connectar amb l'emissora."
            )
        }
        publish(notify = true)
        releaseLocks()
        val token = sessionGeneration
        serviceScope.launch {
            delay(20_000)
            if (sessionGeneration == token && activeRequest === request &&
                localPlayer?.isPlaying != true
            ) {
                stopAndFinish()
            }
        }
    }

    private suspend fun playLocalAlarm(reason: String, emergencySchedule: Schedule? = activeRequest?.schedule) {
        // Si ens crida la mateixa xarxa de seguretat, no ens hem de cancel·lar.
        val currentJob = coroutineContext[Job]
        if (alarmSafetyJob !== currentJob) alarmSafetyJob?.cancel()
        alarmSafetyJob = null
        if (chainJob !== currentJob) chainJob?.cancel()
        chainJob = null
        watchdogJob?.cancel()
        autoStopJob?.cancel()
        sleepJob?.cancel()
        fadeJob?.cancel()
        stopRecorderAndBuffer()
        runCatching {
            player?.stop()
            player?.clearMediaItems()
        }

        val started = LocalAlarmFallback.start(generated = {
            val toneFile = withContext(io) { AlarmToneGenerator.ensureFile(this@PlaybackService) }
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setDataSource(toneFile.absolutePath)
                mp.isLooping = true
                mp.prepare()
                mp.start()
                localPlayer = mp
            } catch (error: Exception) {
                runCatching { mp.release() }
                throw error
            }
        }, system = {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setDataSource(this@PlaybackService, uri)
                mp.isLooping = true
                mp.prepare()
                mp.start()
                localPlayer = mp
            } catch (error: Exception) {
                runCatching { mp.release() }
                throw error
            }
        }, emergency = {
            emergencySchedule?.let { schedule ->
                runCatching { notifications.postAlarmFallback(schedule) }
            }
        })

        currentMode = PlaybackMode.LOCAL_ALARM
        acquireLocks()
        updateState {
            it.copy(
                mode = if (started) PlaybackMode.LOCAL_ALARM else PlaybackMode.IDLE,
                isLocalAlarm = true,
                isPlaying = started,
                isBuffering = false,
                isFallbackStation = false,
                errorMessage = reason,
                sourceLabel = if (started) "To d'alarma local" else "Error de so",
                positionUs = 0L,
                windowStartUs = 0L,
                liveEdgeUs = 0L,
                bufferDurationUs = 0L,
                volume = 1f,
                stopAfterAtMillis = null,
                sleepTimerAtMillis = null
            )
        }
        activeRequest?.let { request ->
            logOutcome(
                request,
                if (started) AlarmOutcome.LOCAL_ALARM else AlarmOutcome.ERROR,
                if (started) "$reason · to local en bucle" else "$reason · no s'ha pogut iniciar el to local"
            )
        }
        publish(notify = true)
    }

    // ------------------------------------------------------------------
    // Reproductor
    // ------------------------------------------------------------------

    private fun ensurePlayer(usage: Int): ExoPlayer {
        val existing = player
        if (existing != null && playerUsage == usage) return existing
        if (existing != null) {
            existing.removeListener(playerListener)
            runCatching { existing.release() }
        }
        playerUsage = usage
        val attributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(
                if (usage == AudioAttributes.USAGE_ALARM) {
                    C.AUDIO_CONTENT_TYPE_SPEECH
                } else {
                    C.AUDIO_CONTENT_TYPE_MUSIC
                }
            )
            .build()
        val created = ExoPlayer.Builder(this)
            .setAudioAttributes(attributes, true)
            .setHandleAudioBecomingNoisy(usage != AudioAttributes.USAGE_ALARM)
            .build()
        created.addListener(playerListener)
        player = created
        return created
    }

    private fun setPlayerSourceAt(frame: FramePoint, playWhenReady: Boolean): Boolean {
        val p = player ?: return false
        val extent = buffer ?: return false
        val factory = RingBufferDataSourceFactory(extent, frame.offset)
        val source = ProgressiveMediaSource.Factory(factory)
            .createMediaSource(MediaItem.fromUri(RingBufferDataSourceFactory.uri()))
        switchingSource = true
        val result = runCatching {
            p.stop()
            p.clearMediaItems()
            p.setMediaSource(source)
            p.prepare()
            // stop() deixa playWhenReady a false: cal tornar-lo a activar
            // explícitament perquè el salt no deixi la reproducció en pausa.
            if (playWhenReady) p.play() else p.pause()
        }
        switchingSource = false
        return result.isSuccess
    }

    private fun reanchorToOldest() {
        val frames = timeline?.snapshot().orEmpty()
        val oldest = buffer?.oldestOffset ?: return
        val frame = frames.firstOrNull { it.offset >= oldest } ?: return
        val p = player ?: return
        val play = p.playWhenReady
        if (setPlayerSourceAt(frame, playWhenReady = play)) {
            playbackBaseTimeUs = frame.startTimeUs
            watchdogJob?.cancel()
            startWatchdog(activeAttemptId)
            updateState {
                it.copy(
                    positionUs = frame.startTimeUs,
                    isBuffering = true,
                    errorMessage = "La pausa ha superat el buffer. Continuem des del punt més antic."
                )
            }
        } else {
            failCurrentAttempt(IOException("No s'ha pogut recuperar el buffer"))
        }
    }

    private fun togglePlayPause() {
        if (currentMode == PlaybackMode.LOCAL_ALARM) return
        if (activeRequest == null) {
            playStationById(null)
            return
        }
        val p = player ?: return
        if (p.playWhenReady) pausePlayback() else resumePlayback()
    }

    private fun pausePlayback() {
        if (currentMode == PlaybackMode.LOCAL_ALARM) return
        val p = player ?: return
        p.pause()
        // El gravador NO s'atura: en reprendre, el time-shift continua creixent.
        updateState { it.copy(isPlaying = false) }
        publish(notify = true)
    }

    private fun resumePlayback() {
        if (currentMode == PlaybackMode.LOCAL_ALARM) return
        if (activeRequest == null) {
            playStationById(null)
            return
        }
        val p = player ?: return
        p.play()
        updateState { it.copy(isPlaying = true) }
        publish(notify = true)
    }

    private fun seekRelative(deltaUs: Long) {
        if (currentMode == PlaybackMode.LOCAL_ALARM) return
        val p = player ?: return
        if (currentMode == PlaybackMode.HLS_LIVE) {
            val target = (p.currentPosition + deltaUs / 1000L).coerceAtLeast(0L)
            p.seekTo(target)
            return
        }
        val current = playbackBaseTimeUs + p.currentPosition * 1000L
        seekToWindow(current + deltaUs)
    }

    private fun seekToWindow(targetUs: Long) {
        if (currentMode == PlaybackMode.LOCAL_ALARM) return
        val p = player ?: return
        if (currentMode == PlaybackMode.HLS_LIVE) {
            p.seekTo(targetUs.coerceAtLeast(0L))
            return
        }
        val frames = timeline?.snapshot().orEmpty()
        val extent = buffer ?: return
        if (frames.isEmpty()) return
        val oldest = extent.oldestOffset
        val startUs = frames.first().startTimeUs
        val liveUs = timeline?.latestTimeUs() ?: startUs
        val target = targetUs.coerceIn(startUs, liveUs)
        val candidate = frames
            .filter { it.offset >= oldest }
            .lastOrNull { it.startTimeUs <= target }
            ?: frames.firstOrNull { it.offset >= oldest }
            ?: return
        val play = p.playWhenReady
        if (setPlayerSourceAt(candidate, playWhenReady = play)) {
            playbackBaseTimeUs = candidate.startTimeUs
            watchdogJob?.cancel()
            startWatchdog(activeAttemptId)
            updateState {
                it.copy(
                    positionUs = candidate.startTimeUs,
                    isBuffering = true,
                    errorMessage = null
                )
            }
            publish(notify = true)
        }
    }

    private fun goLive() {
        if (currentMode == PlaybackMode.LOCAL_ALARM) return
        val p = player ?: return
        if (currentMode == PlaybackMode.HLS_LIVE) {
            p.seekToDefaultPosition()
            return
        }
        val live = timeline?.latestTimeUs() ?: return
        // Un petit marge perquè el lector no toqui exactament el final del buffer.
        seekToWindow(live - 350_000L)
    }

    private fun applyVolume(target: Float, fadeSeconds: Int) {
        val p = player ?: return
        fadeJob?.cancel()
        val bounded = target.coerceIn(0f, 1f)
        if (fadeSeconds <= 0) {
            p.volume = bounded
            updateState { it.copy(volume = bounded) }
            return
        }
        p.volume = 0f
        fadeJob = serviceScope.launch {
            val steps = fadeSeconds * 10
            for (step in 1..steps) {
                val fraction = step.toFloat() / steps
                p.volume = bounded * fraction
                if (step % 10 == 0) {
                    updateState { it.copy(volume = bounded * fraction) }
                }
                delay(100)
            }
            p.volume = bounded
            updateState { it.copy(volume = bounded) }
        }
    }

    private fun startWatchdog(attemptId: Int) {
        watchdogJob?.cancel()
        watchdogJob = serviceScope.launch {
            var lastPosition = player?.currentPosition ?: 0L
            var lastChange = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(5_000)
                val p = player ?: return@launch
                if (currentMode == PlaybackMode.LOCAL_ALARM) return@launch
                if (!p.playWhenReady) {
                    lastPosition = p.currentPosition
                    lastChange = SystemClock.elapsedRealtime()
                    continue
                }
                val position = p.currentPosition
                if (position != lastPosition) {
                    lastPosition = position
                    lastChange = SystemClock.elapsedRealtime()
                } else if (SystemClock.elapsedRealtime() - lastChange > STALL_TIMEOUT_MS) {
                    failAttempt(attemptId, IOException("La reproducció s'ha encallat"))
                    return@launch
                }
            }
        }
    }

    private fun failAttempt(attemptId: Int, error: Throwable) {
        if (activeRequest == null) return
        if (attemptId != activeAttemptId) return
        val pending = attemptFailure ?: return
        if (!pending.isCompleted) pending.complete(error)
    }

    private fun failCurrentAttempt(error: Throwable) {
        failAttempt(activeAttemptId, error)
    }

    private fun isBufferOverrun(error: Throwable): Boolean =
        generateSequence(error as Throwable?) { it.cause }
            .any { it is RingBufferDataSource.BufferOverrunSignal }

    // ------------------------------------------------------------------
    // Serveis de fons, temporitzadors i estat
    // ------------------------------------------------------------------

    private fun startAutoStop(request: PlaybackRequest) {
        autoStopJob?.cancel()
        val minutes = request.stopAfterMinutes ?: return
        autoStopJob = serviceScope.launch {
            delay(minutes * 60_000L)
            if (activeRequest === request && currentMode != PlaybackMode.LOCAL_ALARM) {
                stopAndFinish()
            }
        }
    }

    private fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null || minutes <= 0) {
            updateState { it.copy(sleepTimerAtMillis = null) }
            publish(notify = true)
            return
        }
        val at = System.currentTimeMillis() + minutes * 60_000L
        updateState { it.copy(sleepTimerAtMillis = at) }
        publish(notify = true)
        sleepJob = serviceScope.launch {
            delay(minutes * 60_000L)
            if (currentMode != PlaybackMode.LOCAL_ALARM) stopAndFinish()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1_000)
                refreshProgress(forcePublish = false)
            }
        }
    }

    private fun refreshProgress(forcePublish: Boolean) {
        if (activeRequest == null) return
        if (currentMode == PlaybackMode.LOCAL_ALARM) {
            val playing = localPlayer?.isPlaying == true
            updateState { it.copy(isPlaying = playing) }
        } else {
            val p = player ?: return
            if (currentMode == PlaybackMode.TIMESHIFT) {
                val tl = timeline
                if (tl != null) {
                    val frames = tl.snapshot()
                    if (frames.isNotEmpty()) {
                        val start = frames.first().startTimeUs
                        val live = tl.latestTimeUs()
                        val position = (playbackBaseTimeUs + p.currentPosition * 1_000L)
                            .coerceIn(start, live)
                        updateState {
                            it.copy(
                                positionUs = position,
                                windowStartUs = start,
                                liveEdgeUs = live,
                                bufferDurationUs = live - start,
                                isPlaying = p.isPlaying,
                                isBuffering = p.playbackState == Player.STATE_BUFFERING,
                                volume = p.volume
                            )
                        }
                    }
                }
            } else {
                // En HLS la latència del manifest (20-30 s) és inherent. Només
                // és «retard» allò que l'usuari hi afegeix retrocedint, així que
                // mesurem el desviament respecte de la millor latència vista.
                val rawOffset = p.currentLiveOffset
                    .takeIf { it != C.TIME_UNSET }
                    ?.coerceAtLeast(0L)
                if (rawOffset != null) {
                    val base = hlsBaselineOffsetMs
                    if (base == null || rawOffset < base) hlsBaselineOffsetMs = rawOffset
                }
                val extraDelay = rawOffset?.let {
                    (it - (hlsBaselineOffsetMs ?: it)).coerceAtLeast(0L)
                }
                updateState {
                    it.copy(
                        isPlaying = p.isPlaying,
                        isBuffering = p.playbackState == Player.STATE_BUFFERING,
                        volume = p.volume,
                        hlsLiveOffsetMs = extraDelay
                    )
                }
            }
        }
        publishCounter++
        publish(notify = forcePublish || publishCounter % 5L == 0L)
    }

    private fun updateState(transform: (PlaybackSnapshot) -> PlaybackSnapshot) {
        stateStore.update(transform)
    }

    private fun publish(notify: Boolean) {
        val snapshot = stateStore.state.value
        val durationMs = if (snapshot.hasTimeshift) snapshot.bufferDurationUs / 1000L else -1L
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    snapshot.station?.name ?: "Cronos"
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    if (snapshot.isLocalAlarm) "Alarma local" else snapshot.station?.tagline.orEmpty()
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM,
                    if (snapshot.isAtLive) "En directe" else snapshot.delayLabel
                )
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
                .build()
        )
        val playbackState = when {
            snapshot.isLocalAlarm && snapshot.isPlaying -> PlaybackStateCompat.STATE_PLAYING
            snapshot.isPlaying -> PlaybackStateCompat.STATE_PLAYING
            snapshot.isBuffering -> PlaybackStateCompat.STATE_BUFFERING
            activeRequest != null -> PlaybackStateCompat.STATE_PAUSED
            else -> PlaybackStateCompat.STATE_STOPPED
        }
        val positionMs = when {
            snapshot.hasTimeshift -> (snapshot.positionUs - snapshot.windowStartUs) / 1000L
            currentMode == PlaybackMode.HLS_LIVE -> player?.currentPosition ?: 0L
            else -> 0L
        }
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_FAST_FORWARD or
                        PlaybackStateCompat.ACTION_REWIND
                )
                .setState(
                    playbackState,
                    positionMs.coerceAtLeast(0L),
                    if (snapshot.isPlaying) 1f else 0f
                )
                .setBufferedPosition(snapshot.liveEdgeUs / 1000L)
                .build()
        )
        if (notify && foregroundStarted) {
            notifications.postPlayback(snapshot, mediaSession.sessionToken)
        }
    }

    private fun endPlaybackResources() {
        alarmSafetyJob?.cancel()
        alarmSafetyJob = null
        chainJob?.cancel()
        watchdogJob?.cancel()
        autoStopJob?.cancel()
        sleepJob?.cancel()
        fadeJob?.cancel()
        chainJob = null
        watchdogJob = null
        autoStopJob = null
        sleepJob = null
        fadeJob = null
        runCatching { player?.stop() }
        runCatching { player?.clearMediaItems() }
        stopRecorderAndBuffer()
        stopLocalTone()
        releaseLocks()
        activeRequest = null
        attemptFailure = null
        playbackBaseTimeUs = 0L
        currentMode = PlaybackMode.IDLE
    }

    private fun stopAndFinish() {
        sessionGeneration++
        endPlaybackResources()
        stateStore.reset()
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_STOPPED, 0L, 0f)
                .build()
        )
        publish(notify = false)
        notifications.cancelPlayback()
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    private fun stopRecorderAndBuffer() {
        recorder?.stop()
        recorder = null
        runCatching { buffer?.close(true) }
        buffer = null
        timeline = null
        indexer = null
    }

    private fun stopLocalTone() {
        localPlayer?.let { mp ->
            runCatching { if (mp.isPlaying) mp.stop() }
            runCatching { mp.release() }
        }
        localPlayer = null
    }

    private fun ensureForeground() {
        val snapshot = stateStore.state.value
        val notification = notifications.playbackNotification(snapshot, mediaSession.sessionToken)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                PlaybackCommands.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(PlaybackCommands.NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
    }

    private fun acquireLocks() {
        if (wakeLock?.isHeld != true) {
            val power = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = power.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Cronos:Playback"
            ).apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
        }
        if (wifiLock?.isHeld != true) {
            val wifi = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiLock = wifi.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "Cronos:Wifi"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releaseLocks() {
        wakeLock?.let { if (it.isHeld) runCatching { it.release() } }
        wakeLock = null
        wifiLock?.let { if (it.isHeld) runCatching { it.release() } }
        wifiLock = null
    }

    private fun capacityBytes(station: Station, settings: AppSettings): Long {
        val kbps = station.bitrateKbps.coerceIn(48, 192)
        val bytesPerSecond = kbps * 1000L / 8L
        val minutes = settings.bufferDurationMinutes.coerceIn(15, 12 * 60)
        // Un 50% de marge perquè el bitrate real del stream pot ser més alt.
        return (minutes * 60L * bytesPerSecond * 3L / 2L).coerceAtLeast(8L * 1024L * 1024L)
    }

    private fun isUserUnlocked(): Boolean = runCatching {
        (getSystemService(Context.USER_SERVICE) as UserManager).isUserUnlocked
    }.getOrDefault(true)

    /**
     * Abans del desbloqueig, cacheDir (CE) no és escrivible. El buffer ha d'anar
     * a l'emmagatzematge protegit pel dispositiu perquè l'alarma pugui sonar.
     */
    private fun bufferDirectory(): File =
        if (isUserUnlocked()) cacheDir else container.deviceProtectedStore.cacheDir

    /**
     * Puja el volum del flux d'alarma fins al nivell configurat. Només apuja:
     * mai rebaixa el que l'usuari hagi triat.
     */
    private fun ensureAlarmVolume(target: Float) {
        runCatching {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            if (max <= 0) return
            val wanted = (max * target.coerceIn(0f, 1f)).roundToInt().coerceIn(1, max)
            val current = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            if (current < wanted) {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, wanted, 0)
            }
        }
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            8100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun logOutcome(request: PlaybackRequest, outcome: AlarmOutcome, message: String) {
        if (!request.isScheduled) return
        if (outcome != AlarmOutcome.ERROR && lastLoggedOutcome == outcome) return
        lastLoggedOutcome = outcome
        val log = AlarmLog(
            id = UUID.randomUUID().toString(),
            scheduleId = request.schedule?.id ?: "test",
            scheduleLabel = request.schedule?.let {
                "${it.timeLabel()} · ${if (it.isOneShot) "un sol cop" else it.daysLabel()}"
            } ?: "Prova manual",
            stationName = request.station.name,
            plannedAtMillis = request.plannedAtMillis ?: request.startedAtMillis,
            actualAtMillis = System.currentTimeMillis(),
            outcome = outcome,
            message = message,
            isTest = request.isTest
        )
        serviceScope.launch(io) {
            runCatching { container.reliabilityRepository.addLog(log) }
        }
        serviceScope.launch {
            runCatching { container.refreshReliabilityNotification() }
        }
    }

    companion object {
        const val ROOT_ID = "radiocatalana-root"
        const val START_TIMEOUT_MS = 14_000L
        const val READY_TIMEOUT_MS = 10_000L
        const val MIN_START_BUFFER_US = 900_000L
        const val STALL_TIMEOUT_MS = 25_000L
        // Temps màxim sense so abans de saltar al to local en una alarma.
        const val ALARM_SAFETY_TIMEOUT_MS = 25_000L
        private const val WAKE_LOCK_TIMEOUT_MS = 12L * 60L * 60L * 1000L

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
