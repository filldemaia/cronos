package com.filldemaia.cronos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBitmapSizeTest {
    @Test
    fun `small images keep native resolution`() {
        assertEquals(600 to 300, WidgetBitmapSize.fit(600, 300, 500_000))
    }

    @Test
    fun `large images fit the pixel budget and preserve proportions`() {
        val (width, height) = WidgetBitmapSize.fit(4000, 2000, 1_000_000)
        assertTrue(width.toLong() * height <= 1_000_000)
        assertEquals(2f, width.toFloat() / height, 0.01f)
    }

    @Test
    fun `long edges are bounded`() {
        val (width, height) = WidgetBitmapSize.fit(5000, 1000, 10_000_000)
        assertTrue(width <= 1600 && height <= 1600)
    }

    @Test
    fun `invalid dimensions still produce a valid bitmap`() {
        assertEquals(1 to 1, WidgetBitmapSize.fit(0, -20, 0))
    }

    @Test
    fun `extreme aspect ratios cannot overflow the budget`() {
        assertEquals(1 to 1, WidgetBitmapSize.fit(Int.MAX_VALUE, 1, 1))
    }
}
