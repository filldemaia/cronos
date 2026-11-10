package com.filldemaia.cronos.radio.data.local

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import com.filldemaia.cronos.radio.data.model.CheckLevel
import com.filldemaia.cronos.radio.reliability.protectedStorageCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProtectedWritesTest {
    @get:Rule val directory = TemporaryFolder()

    @Test fun failedAtomicMovePreservesPreviousFileAndRecordsError() = runBlocking {
        val file = directory.newFile("alarms.json").apply { writeText("previous") }
        val logged = mutableListOf<String>()
        val writes = SerializedProtectedWrites { name, _ -> logged += name }
        val failure = runCatching {
            writes.write(file.name) {
                AtomicProtectedFile.write(file, "new") { _, _ -> throw IOException("rename failed") }
            }
        }
        assertTrue(failure.isFailure)
        assertEquals("previous", file.readText())
        assertEquals(listOf(file.name), logged)
        assertEquals("rename failed", writes.errors()[file.name])
        assertEquals(CheckLevel.ERROR, protectedStorageCheck(writes.errors())?.level)
        assertEquals(listOf(file.name), directory.root.list()!!.toList())
    }

    @Test fun successfulWriteClearsOnlyItsOwnError() = runBlocking {
        val writes = SerializedProtectedWrites { _, _ -> }
        runCatching { writes.write("alarms") { throw IOException("failed") } }
        runCatching { writes.write("settings") { throw IOException("failed") } }
        val file = directory.newFile("alarms.json")
        writes.write("alarms") { AtomicProtectedFile.write(file, "new") }
        assertEquals("new", file.readText())
        assertEquals(setOf("settings"), writes.errors().keys)
    }

    @Test fun concurrentReadModifyWritesAreSerialized() = runBlocking {
        val writes = SerializedProtectedWrites { _, _ -> }
        val active = AtomicInteger()
        val maxActive = AtomicInteger()
        var value = 0
        (1..100).map {
            async(Dispatchers.Default) {
                writes.write("alarms") {
                    maxActive.updateAndGet { previous -> maxOf(previous, active.incrementAndGet()) }
                    val previous = value
                    Thread.yield()
                    value = previous + 1
                    active.decrementAndGet()
                }
            }
        }.awaitAll()
        assertEquals(1, maxActive.get())
        assertEquals(100, value)
    }
}
