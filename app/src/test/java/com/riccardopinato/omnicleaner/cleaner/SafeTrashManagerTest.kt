package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties
import java.util.UUID
import kotlin.io.path.createTempDirectory

class SafeTrashManagerTest {
    @Test
    fun moveToTrashPreservesPayloadAndMetadata() {
        val root = createTempDirectory("omni-trash-").toFile()
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
                metadata.inputStream().use { input -> load(input) }
            }
            assertTrue(properties.getProperty("originalPath").endsWith("notes.txt"))
            assertEquals(64, properties.getProperty("sha256").length)
            assertEquals(payload.length(), manager.getTrashSize())
            assertTrue(manager.emptyTrash())
            assertFalse(payload.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun restoresVerifiedEntryToOriginalPath() {
        val root = createTempDirectory("omni-restore-").toFile()
        val trash = File(root, "trash")
        val source = File(root, "notes.txt").apply {
            writeText("restore-me")
            setLastModified(234567L)
        }

        try {
            val manager = SafeTrashManager.forTests(trash)
            assertNotNull(manager.moveToTrash(source))
            val entry = manager.listEntries().single()

            val result = manager.restore(entry.id)

            assertEquals(SafeTrashRestoreStatus.RESTORED, result.status)
            assertTrue(source.isFile)
            assertEquals("restore-me", source.readText())
            assertTrue(manager.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun restoreNeverOverwritesExistingDestination() {
        val root = createTempDirectory("omni-restore-collision-").toFile()
        val trash = File(root, "trash")
        val source = File(root, "notes.txt").apply { writeText("original") }

        try {
            val manager = SafeTrashManager.forTests(trash)
            assertNotNull(manager.moveToTrash(source))
            val entry = manager.listEntries().single()

            source.writeText("new-file")

            val result = manager.restore(entry.id)

            assertEquals(SafeTrashRestoreStatus.DESTINATION_EXISTS, result.status)
            assertEquals("new-file", source.readText())
            assertEquals(1, manager.listEntries().size)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun restoreRejectsTamperedPrivatePayload() {
        val root = createTempDirectory("omni-restore-tamper-").toFile()
        val trash = File(root, "trash")
        val source = File(root, "notes.txt").apply { writeText("123456789") }

        try {
            val manager = SafeTrashManager.forTests(trash)
            assertNotNull(manager.moveToTrash(source))
            val entry = manager.listEntries().single()
            val payload = File(File(trash, entry.id), "payload")
            payload.writeText("987654321")

            val result = manager.restore(entry.id)

            assertEquals(SafeTrashRestoreStatus.PAYLOAD_INVALID, result.status)
            assertFalse(source.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun legacyEntryWithoutStoredDigestRemainsRestorable() {
        val root = createTempDirectory("omni-restore-legacy-").toFile()
        val trash = File(root, "trash")
        val destination = File(root, "legacy.txt")
        val entryDir = File(trash, UUID.randomUUID().toString()).apply { mkdirs() }
        val payload = File(entryDir, "payload").apply { writeText("legacy") }
        val metadata = File(entryDir, "original.properties")
        Properties().apply {
            setProperty("originalPath", destination.canonicalPath)
            setProperty("originalName", destination.name)
            setProperty("sizeBytes", payload.length().toString())
            setProperty("lastModified", "0")
        }.also { properties ->
            metadata.outputStream().use { output -> properties.store(output, "legacy") }
        }

        try {
            val manager = SafeTrashManager.forTests(trash)
            val entry = manager.listEntries().single()
            val migratedProperties = Properties().apply {
                metadata.inputStream().use { input -> load(input) }
            }
            assertEquals(64, migratedProperties.getProperty("sha256").length)

            val result = manager.restore(entry.id)

            assertEquals(SafeTrashRestoreStatus.RESTORED, result.status)
            assertEquals("legacy", destination.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun legacyPrivateTrashMigratesToPersistentTrash() {
        val root = createTempDirectory("omni-trash-migrate-").toFile()
        val legacy = File(root, "legacy-trash")
        val persistent = File(root, "persistent-trash")
        val restoreRoot = root
        val original = File(root, "migrated.txt")
        val entryDir = File(legacy, UUID.randomUUID().toString()).apply { mkdirs() }
        val payload = File(entryDir, "payload").apply { writeText("migrate-me") }
        val metadata = File(entryDir, "original.properties")
        Properties().apply {
            setProperty("originalPath", original.canonicalPath)
            setProperty("originalName", original.name)
            setProperty("sizeBytes", payload.length().toString())
            setProperty("lastModified", "0")
        }.also { properties ->
            metadata.outputStream().use { output -> properties.store(output, "legacy") }
        }

        try {
            val manager = SafeTrashManager.forTests(
                trashDirectory = persistent,
                restoreRoot = restoreRoot,
                legacyTrashDirectory = legacy,
            )

            val migrated = manager.listEntries().single()
            assertFalse(File(legacy, migrated.id).exists())
            assertTrue(File(persistent, migrated.id).isDirectory)

            val result = manager.restore(migrated.id)
            assertEquals(SafeTrashRestoreStatus.RESTORED, result.status)
            assertEquals("migrate-me", original.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun mediaIsNotMovedIntoPrivateTrash() {
        val root = createTempDirectory("omni-media-trash-").toFile()
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
    @Test
    fun persistentTrashPathLivesOutsideAppPrivateStorageShape() {
        val root = createTempDirectory("omni-shared-root-").toFile()
        try {
            val trash = StorageAccess.safeTrashRoot(root)
            assertTrue(trash.path.contains(StorageAccess.APP_MANAGED_DIRECTORY))
            assertTrue(trash.path.endsWith(StorageAccess.SAFE_TRASH_DIRECTORY))
            assertTrue(PathSafety.isWithinRoot(trash, root))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun migratesLegacyPrivateEntryOnlyAfterIntegrityValidation() {
        val root = createTempDirectory("omni-trash-migrate-").toFile()
        val legacy = File(root, "legacy-trash")
        val persistent = File(root, ".OmniCleaner/SafeTrash")
        val original = File(root, "restore-me.txt")
        val entryId = UUID.randomUUID().toString()
        val legacyEntry = File(legacy, entryId).apply { mkdirs() }
        val payload = File(legacyEntry, "payload").apply { writeText("survive-uninstall") }
        val metadata = File(legacyEntry, "original.properties")

        Properties().apply {
            setProperty("originalPath", original.canonicalPath)
            setProperty("originalName", original.name)
            setProperty("sizeBytes", payload.length().toString())
            setProperty("lastModified", "0")
        }.also { properties ->
            metadata.outputStream().use { output -> properties.store(output, "legacy") }
        }

        try {
            val manager = SafeTrashManager.forTests(
                trashDirectory = persistent,
                restoreRoot = root,
                legacyTrashDirectory = legacy,
            )

            assertTrue(manager.isPersistentTrashAvailable())
            assertFalse(legacyEntry.exists())
            assertEquals(1, manager.listEntries().size)
            assertEquals(
                SafeTrashRestoreStatus.RESTORED,
                manager.restore(entryId).status,
            )
            assertEquals("survive-uninstall", original.readText())
        } finally {
            root.deleteRecursively()
        }
    }

}
