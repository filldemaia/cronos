package com.filldemaia.cronos.radio.reliability

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.filldemaia.cronos.radio.alarm.AlarmScheduler
import com.filldemaia.cronos.radio.data.model.CheckLevel
import com.filldemaia.cronos.radio.data.model.ReliabilityAction
import com.filldemaia.cronos.radio.data.model.ReliabilityCheck
import com.filldemaia.cronos.radio.data.model.ReliabilityLevel
import com.filldemaia.cronos.radio.data.model.ReliabilityReport
import com.filldemaia.cronos.radio.data.repository.ReliabilityRepository
import com.filldemaia.cronos.radio.data.repository.ScheduleRepository
import com.filldemaia.cronos.radio.data.repository.StationRepository
import com.filldemaia.cronos.radio.domain.schedule.NextOccurrenceCalculator
import com.filldemaia.cronos.radio.domain.schedule.NextAlarmPlanner
import com.filldemaia.cronos.radio.playback.NotificationHelper
import com.filldemaia.cronos.radio.util.PermissionManager
import java.time.Instant

class ReliabilityChecker(
    private val context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val stationRepository: StationRepository,
    private val reliabilityRepository: ReliabilityRepository,
    private val alarmScheduler: AlarmScheduler
) {

    suspend fun check(now: Instant = Instant.now()): ReliabilityReport {
        val oemAck = reliabilityRepository.currentOemAcknowledged()
        val permissions = PermissionManager.status(context, oemAck)
        val schedules = scheduleRepository.active()
        val stations = stationRepository.current().associateBy { it.id }
        val checks = mutableListOf<ReliabilityCheck>()

        checks += ReliabilityCheck(
            id = "exact_alarm",
            title = "Alarmes exactes",
            detail = if (permissions.exactAlarms) {
                "Permís concedit. L'alarma no depèn de Doze."
            } else {
                "Android pot retardar o bloquejar l'engegada. Cal concedir 'Alarmes i recordatoris'."
            },
            level = if (permissions.exactAlarms) CheckLevel.OK else CheckLevel.ERROR,
            actionLabel = if (permissions.exactAlarms) null else "Concedeix",
            action = if (permissions.exactAlarms) null else ReliabilityAction.EXACT_ALARM
        )

        checks += ReliabilityCheck(
            id = "notifications",
            title = "Notificacions",
            detail = if (permissions.notifications) {
                "Pots veure i aturar la reproducció des de la notificació."
            } else {
                "Sense notificacions no es pot garantir el control de l'alarma ni els avisos."
            },
            level = if (permissions.notifications) CheckLevel.OK else CheckLevel.ERROR,
            actionLabel = if (permissions.notifications) null else "Activa",
            action = if (permissions.notifications) null else ReliabilityAction.NOTIFICATIONS
        )

        val alarmVolume = PermissionManager.alarmVolume(context)
        checks += ReliabilityCheck(
            id = "alarm_volume",
            title = "Volum de l'alarma",
            detail = if (alarmVolume > 0) {
                "El flux d'alarma és audible (nivell $alarmVolume)."
            } else {
                "El volum d'alarma és a zero. L'alarma no se sentirà encara que arrenqui."
            },
            level = if (alarmVolume > 0) CheckLevel.OK else CheckLevel.ERROR,
            actionLabel = if (alarmVolume > 0) null else "Puja'l",
            action = if (alarmVolume > 0) null else ReliabilityAction.SOUND_SETTINGS
        )

        val dndBlocks = PermissionManager.dndBlocksAlarms(context)
        checks += ReliabilityCheck(
            id = "dnd",
            title = "No molestar",
            detail = if (dndBlocks) {
                "El mode de silenci total pot mutar l'alarma. Permet les alarmes o desactiva'l."
            } else {
                "El mode No molestar no bloqueja el flux d'alarma."
            },
            level = if (dndBlocks) CheckLevel.WARNING else CheckLevel.OK,
            actionLabel = if (dndBlocks) "Revisa" else null,
            action = if (dndBlocks) ReliabilityAction.DND_SETTINGS else null
        )

        checks += ReliabilityCheck(
            id = "battery",
            title = "Optimització de bateria",
            detail = if (permissions.batteryOptimizationIgnored) {
                "L'app està exempta de l'optimització de bateria."
            } else {
                "Cal excloure Cronos de l'estalvi de bateria perquè pugui arrencar en segon pla."
            },
            level = if (permissions.batteryOptimizationIgnored) CheckLevel.OK else CheckLevel.ERROR,
            actionLabel = if (permissions.batteryOptimizationIgnored) null else "Exclou",
            action = if (permissions.batteryOptimizationIgnored) null else ReliabilityAction.BATTERY
        )

        checks += ReliabilityCheck(
            id = "oem",
            title = "Restriccions del fabricant",
            detail = if (!PermissionManager.isKnownOem() || permissions.autoStartAcknowledged) {
                if (PermissionManager.isKnownOem()) {
                    "Has confirmat la revisió de l'inici automàtic (${permissions.manufacturer})."
                } else {
                    "No s'ha detectat una restricció específica del fabricant."
                }
            } else {
                "${permissions.manufacturer} pot aturar l'app en segon pla. Revisa l'inici automàtic i la guia de dontkillmyapp.com."
            },
            level = if (!PermissionManager.isKnownOem() || permissions.autoStartAcknowledged) {
                CheckLevel.OK
            } else {
                CheckLevel.WARNING
            },
            actionLabel = if (!PermissionManager.isKnownOem() || permissions.autoStartAcknowledged) null else "Revisa",
            action = if (!PermissionManager.isKnownOem() || permissions.autoStartAcknowledged) null else ReliabilityAction.AUTO_START
        )

        checks += ReliabilityCheck(
            id = "notification_channel",
            title = "Canal de reproducció",
            detail = if (PermissionManager.notificationChannelEnabled(
                    context, NotificationHelper.CHANNEL_PLAYBACK
                )
            ) {
                "El canal de reproducció està actiu."
            } else {
                "El canal de reproducció està silenciat. L'alarma sonarà, però no podràs aturar-la des de la notificació."
            },
            level = if (PermissionManager.notificationChannelEnabled(
                    context, NotificationHelper.CHANNEL_PLAYBACK
                )
            ) {
                CheckLevel.OK
            } else {
                CheckLevel.WARNING
            },
            actionLabel = "Obre",
            action = ReliabilityAction.OPEN_APP_SETTINGS
        )

        val planned = NextAlarmPlanner.plan(schedules, now)
        val plannedById = planned.associateBy { it.schedule.id }
        schedules.forEach { schedule ->
            val station = stations[schedule.stationId]
            if (station == null) {
                checks += ReliabilityCheck(
                    id = "station_${schedule.id}",
                    title = "Emissora de ${schedule.timeLabel()}",
                    detail = "L'emissora configurada ja no existeix. Edita la programació.",
                    level = CheckLevel.ERROR,
                    actionLabel = "Programacions"
                )
            }
            val next = plannedById[schedule.id]
            if (next == null) {
                checks += ReliabilityCheck(
                    id = "next_${schedule.id}",
                    title = "Propera execució ${schedule.timeLabel()}",
                    detail = "No s'ha pogut calcular cap pròxima execució. Revisa els dies o torna a activar-la.",
                    level = CheckLevel.ERROR,
                    actionLabel = "Rearma",
                    action = ReliabilityAction.ARM_ALARMS
                )
            } else {
                val armed = alarmScheduler.isArmed(schedule.id)
                checks += ReliabilityCheck(
                    id = "armed_${schedule.id}",
                    title = "Alarma ${schedule.timeLabel()} · ${station?.name ?: ""}",
                    detail = buildString {
                        append(NextOccurrenceCalculator.label(next.nextAt, now))
                        append(" · ")
                        append(if (armed) "armada al sistema" else "PENDENT D'ARMAR")
                    },
                    level = if (armed && permissions.exactAlarms) CheckLevel.OK else CheckLevel.ERROR,
                    actionLabel = if (armed) null else "Rearma",
                    action = if (armed) null else ReliabilityAction.ARM_ALARMS
                )
            }
        }

        val networkOk = hasInternet()
        checks += ReliabilityCheck(
            id = "network",
            title = "Connexió per al directe",
            detail = if (networkOk) {
                "Hi ha connexió. Si el stream falla, s'activarà el to local."
            } else {
                "Ara mateix no hi ha internet. L'alarma només podrà sonar amb el to local."
            },
            level = if (networkOk) CheckLevel.OK else CheckLevel.WARNING
        )

        if (schedules.isEmpty()) {
            checks += ReliabilityCheck(
                id = "schedules",
                title = "Programacions",
                detail = "No tens cap programació activa.",
                level = CheckLevel.INFO
            )
        }

        val level = when {
            checks.any { it.level == CheckLevel.ERROR } -> ReliabilityLevel.RED
            checks.any { it.level == CheckLevel.WARNING } -> ReliabilityLevel.YELLOW
            else -> ReliabilityLevel.GREEN
        }
        return ReliabilityReport(
            level = level,
            checks = checks,
            activeScheduleCount = schedules.size
        )
    }

    private fun hasInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
