package com.filldemaia.cronos

/** Independent preferences; component names remain unchanged for existing widgets. */
enum class WidgetType(
    val preferenceKey: String,
    val defaultTextSizeSp: Float,
    val defaultWidthDp: Float,
    val defaultHeightDp: Float
) {
    DATE_AND_TIME("widget_date_time_size", 27f, 250f, 110f),
    TIME("widget_time_size", 27f, 250f, 70f),
    SENYERA("widget_senyera_size", 27f, 180f, 110f);

    fun normalizeTextSize(value: Float): Float =
        if (value.isFinite()) value.coerceIn(MIN_TEXT_SIZE_SP, MAX_TEXT_SIZE_SP)
        else defaultTextSizeSp

    companion object {
        const val MIN_TEXT_SIZE_SP = 12f
        const val MAX_TEXT_SIZE_SP = 50f
    }
}
