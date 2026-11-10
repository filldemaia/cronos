package com.filldemaia.cronos.radio.data.local

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal object AtomicProtectedFile {
    fun write(target: File, value: String, move: (File, File) -> Unit = ::atomicMove) {
        val parent = target.parentFile ?: throw IllegalArgumentException("Missing parent directory")
        if (!parent.isDirectory && !parent.mkdirs() && !parent.isDirectory) {
            throw java.io.IOException("Cannot create protected directory")
        }
        val temporary = Files.createTempFile(parent.toPath(), "${target.name}.", ".tmp").toFile()
        try {
            FileOutputStream(temporary).use {
                it.write(value.toByteArray(Charsets.UTF_8))
                it.fd.sync()
            }
            move(temporary, target)
        } finally {
            Files.deleteIfExists(temporary.toPath())
        }
    }

    private fun atomicMove(source: File, target: File) {
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
}
