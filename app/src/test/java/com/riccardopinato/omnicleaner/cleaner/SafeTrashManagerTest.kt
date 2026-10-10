package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

class SafeTrashManagerTest {
    @Test
    fun moveToTrashPreservesPayloadAndMetadata() {
        val root = createTempDir(prefix = "omni-trash-")
        val trash = File(root, "trash")
        val source = File(root, "notes.txt").apply {
            writeText("important")
            setLastModified(123456L)
        }

        try {
            val manager = SafeTrashManager.forTests(trash)
            val payload = manager.moveToTrash(source)

            assertNotNull(payload)
            assertFalse(source.exists())
            assertTrue(payload!!.exists())

            val metadata = File(payload.parentFile, "original.properties")
            assertTrue(metadata.isFile)

            val properties = Properties().apply {
                metadata.inputStream().use(::load)
            }
            assertTrue(properties.getProperty("originalPath").endsWith("notes.txt"))
            assertTrue(manager.getTrashSize() >= payload.length())
            assertTrue(manager.emptyTrash())
            assertFalse(payload.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun mediaIsNotMovedIntoPrivateTrash() {
        val root = createTempDir(prefix = "omni-media-trash-")
        val source = File(root, "photo.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        try {
            val manager = SafeTrashManager.forTests(File(root, "trash"))
            val result = manager.moveToTrash(source)

            assertTrue(result == null)
            assertTrue(source.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
