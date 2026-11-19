package com.filldemaia.cronos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetTextSizeTest {
    @Test
    fun `each widget has its own preference key`() {
        assertEquals(3, WidgetType.entries.map { it.preferenceKey }.toSet().size)
    }

    @Test
    fun `defaults are uniform at 27 across all clocks`() {
        assertEquals(27f, WidgetType.DATE_AND_TIME.defaultTextSizeSp, 0f)
        assertEquals(27f, WidgetType.TIME.defaultTextSizeSp, 0f)
        assertEquals(27f, WidgetType.SENYERA.defaultTextSizeSp, 0f)
    }

    @Test
    fun `time uses the same default size as date and time`() {
        assertEquals(WidgetType.DATE_AND_TIME.defaultTextSizeSp, WidgetType.TIME.defaultTextSizeSp, 0f)
    }

    @Test
    fun `chosen sizes within range are preserved`() {
        WidgetType.entries.forEach { type ->
            assertEquals(24f, type.normalizeTextSize(24f), 0f)
            assertEquals(WidgetType.MIN_TEXT_SIZE_SP, type.normalizeTextSize(WidgetType.MIN_TEXT_SIZE_SP), 0f)
            assertEquals(WidgetType.MAX_TEXT_SIZE_SP, type.normalizeTextSize(WidgetType.MAX_TEXT_SIZE_SP), 0f)
        }
    }

    @Test
    fun `out of range sizes are clamped`() {
        WidgetType.entries.forEach { type ->
            assertEquals(WidgetType.MIN_TEXT_SIZE_SP, type.normalizeTextSize(-12f), 0f)
            assertEquals(WidgetType.MAX_TEXT_SIZE_SP, type.normalizeTextSize(1000f), 0f)
        }
    }

    @Test
    fun `invalid sizes use the corresponding default`() {
        WidgetType.entries.forEach { type ->
            listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { value ->
                val normalized = type.normalizeTextSize(value)
                assertTrue(normalized.isFinite())
                assertEquals(type.defaultTextSizeSp, normalized, 0f)
            }
        }
    }
}
