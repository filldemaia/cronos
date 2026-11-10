package com.filldemaia.cronos.radio.data.model

enum class CheckLevel { OK, WARNING, ERROR, INFO }

data class ReliabilityCheck(
    val id: String,
    val title: String,
    val detail: String,
    val level: CheckLevel,
    val actionLabel: String? = null,
    val action: ReliabilityAction? = null
)

enum class ReliabilityAction {
    EXACT_ALARM,
    NOTIFICATIONS,
    BATTERY,
    AUTO_START,
    OPEN_APP_SETTINGS,
    SOUND_SETTINGS,
    DND_SETTINGS,
    ARM_ALARMS
}

enum class ReliabilityLevel { GREEN, YELLOW, RED }

data class ReliabilityReport(
    val level: ReliabilityLevel,
    val checks: List<ReliabilityCheck>,
    val generatedAtMillis: Long = System.currentTimeMillis(),
    val activeScheduleCount: Int = 0
) {
    val blockingIssues: List<ReliabilityCheck>
        get() = checks.filter { it.level == CheckLevel.ERROR }
    val warnings: List<ReliabilityCheck>
        get() = checks.filter { it.level == CheckLevel.WARNING }
    val summary: String
        get() = when (level) {
            ReliabilityLevel.GREEN -> "Les teves alarmes estan protegides."
            ReliabilityLevel.YELLOW -> "Hi ha punts a revisar per garantir l'engegada."
            ReliabilityLevel.RED -> "Una alarma activa no es pot garantir."
        }
}

data class PermissionStatus(
    val exactAlarms: Boolean,
    val notifications: Boolean,
    val batteryOptimizationIgnored: Boolean,
    val autoStartAcknowledged: Boolean,
    val manufacturer: String
) {
    /** Només els permisos imprescindibles per poder activar una programació. */
    val criticalReady: Boolean
        get() = exactAlarms && notifications && batteryOptimizationIgnored
}
