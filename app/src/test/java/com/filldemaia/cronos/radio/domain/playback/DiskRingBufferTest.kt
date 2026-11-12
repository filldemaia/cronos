package com.filldemaia.cronos.radio.domain.playback

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DiskRingBufferTest {
    @get:Rule val directory = TemporaryFolder()

    @Test fun wrappingRetainsNewestBytesAndRejectsOverwrittenOffsets() {
        val buffer = DiskRingBuffer(directory.newFile("buffer"), 8)
        try {
            buffer.append(byteArrayOf(0, 1, 2, 3, 4, 5))
            buffer.append(byteArrayOf(6, 7, 8, 9))
            assertEquals(2L, buffer.oldestOffset)
            assertEquals(10L, buffer.writeOffset)
            val data = ByteArray(8)
            assertEquals(8, buffer.read(2, data, 0, data.size))
            assertArrayEquals(byteArrayOf(2, 3, 4, 5, 6, 7, 8, 9), data)
            assertThrows(DiskRingBuffer.BufferOverrunException::class.java) {
                buffer.read(1, ByteArray(1), 0, 1)
            }
        } finally {
            buffer.close()
        }
    }

    @Test fun largeWriteRetainsLastCapacityBytes() {
        val buffer = DiskRingBuffer(directory.newFile("large"), 4)
        try {
            buffer.append(byteArrayOf(0, 1, 2, 3, 4, 5))
            val data = ByteArray(4)
            assertEquals(4, buffer.read(2, data, 0, data.size))
            assertArrayEquals(byteArrayOf(2, 3, 4, 5), data)
        } finally {
            buffer.close()
        }
    }
}
