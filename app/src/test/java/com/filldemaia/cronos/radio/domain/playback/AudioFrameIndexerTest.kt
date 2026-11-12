package com.filldemaia.cronos.radio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFrameIndexerTest {
    private fun frame(size: Int, vararg header: Int): ByteArray = ByteArray(size).apply {
        header.forEachIndexed { index, value -> this[index] = value.toByte() }
    }

    @Test fun mp3HeaderSplitAcrossChunksKeepsBothFrames() {
        val frame = frame(417, 0xff, 0xfb, 0x90, 0x00)
        for (split in 1..3) {
            val indexer = AudioFrameIndexer()
            val data = frame + frame
            val frames = indexer.feed(data.copyOfRange(0, split)) + indexer.feed(data.copyOfRange(split, data.size))
            assertEquals("split=$split", listOf(0L, 417L), frames.map { it.offset })
        }
    }

    @Test fun adtsHeaderSplitAcrossChunksKeepsBothFrames() {
        val frame = frame(20, 0xff, 0xf1, 0x50, 0x80, 0x02, 0x9f, 0xfc)
        for (split in 1..6) {
            val indexer = AudioFrameIndexer()
            val data = frame + frame
            val frames = indexer.feed(data.copyOfRange(0, split)) + indexer.feed(data.copyOfRange(split, data.size))
            assertEquals("split=$split", listOf(0L, 20L), frames.map { it.offset })
        }
    }

    @Test fun mp3HeaderSplitAfterFirstFrameKeepsNextFrame() {
        val frame = frame(417, 0xff, 0xfb, 0x90, 0x00)
        for (split in 1..3) {
            val indexer = AudioFrameIndexer()
            val data = frame + frame
            val frames = indexer.feed(data.copyOfRange(0, frame.size + split)) +
                indexer.feed(data.copyOfRange(frame.size + split, data.size))
            assertEquals("split=$split", listOf(0L, 417L), frames.map { it.offset })
        }
    }
}
