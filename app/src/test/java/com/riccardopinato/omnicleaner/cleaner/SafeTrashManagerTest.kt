package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.util.Properties
import java.util.UUID
import kotlin.io.path.createTempDirectory

class SafeTrashManagerTest {
    @Test
    fun inventoryExposesUnknownAndPartialEntriesWithoutDeletingThem() {
        val root = createTempDirectory("omni-trash-inventory-").toFile()
        val trash = File(root, "trash")
        try {
            val manager = SafeTrashManager.forTests(trash, root)
            assertNotNull(manager.moveToTrash(File(root, "good.apk").apply { writeText("ok") }))
            val corrupt = File(trash, UUID.randomUUID().toString()).apply { mkdirs() }
            val orphan = File(corrupt, "payload").apply { writeText("valuable-partial") }
            val unknown = File(trash, "unidentified.txt").apply { writeText("keep") }

            val inventory = manager.inspect()
            assertTrue(inventory.readable)
            assertEquals(1, inventory.entries.size)
            assertEquals(2, inventory.unrecognizedEntries)
            assertEquals(2L + orphan.length(), inventory.totalPayloadBytes)
            assertTrue(orphan.exists())
            assertTrue(unknown.exists())
        } finally {
            root.deleteRecursively()
        }
    }


    @Test
    fun permanentDeletionOfLargeApkIsConfirmedAndReturnsItsBytes() {
        val root = createTempDirectory("omni-apk-purge-").toFile()
        val source = File(root, "verified-installer.apk")
        val trash = File(root, ".OmniCleaner/SafeTrash")
        try {
            java.io.RandomAccessFile(source, "rw").use { it.setLength(50L * 1024L * 1024L) }
            val manager = SafeTrashManager.forTests(trash, root)
            assertNotNull(manager.moveToTrash(source))
            val entry = manager.listEntries().single()

            val result = manager.purgeEntry(entry.id)
            assertEquals(SafeTrashPurgeStatus.DELETED, result.status)
            assertEquals(50L * 1024L * 1024L, result.reclaimedBytes)
            assertTrue(manager.listEntries().isEmpty())
            assertEquals(0L, manager.getTrashSize())
            assertFalse(File(trash, entry.id).exists())
            assertFalse(source.exists())
            assertEquals(SafeTrashPurgeStatus.ENTRY_NOT_FOUND, manager.purgeEntry(entry.id).status)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun emptyTrashDeletesZeroByteFilesAndReportsTheEntryCount() {
        val root = createTempDirectory("omni-zero-trash-").toFile()
        val trash = File(root, "trash")
        try {
            val source = File(root, "empty.apk").apply { createNewFile() }
            val manager = SafeTrashManager.forTests(trash, root)
            assertNotNull(manager.moveToTrash(source))
            assertEquals(0L, manager.getTrashSize())
            assertEquals(1, manager.listEntries().size)

            val result = manager.purgeAll()
            assertTrue(result.completed)
            assertEquals(1, result.deletedEntries)
            assertEquals(0, result.rejectedEntries)
            assertEquals(0L, result.reclaimedBytes)
            assertTrue(manager.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun purgeAllSkipsUnrecognizedEntriesAndRefusesPathsOutsideTrash() {
        val root = createTempDirectory("omni-guarded-purge-").toFile()
        val trash = File(root, "trash")
        val untouched = File(root, "keep-me.apk").apply { writeText("important") }
        try {
            val manager = SafeTrashManager.forTests(trash, root)
            assertNotNull(manager.moveToTrash(File(root, "safe.apk").apply { writeText("trash-me") }))
            val foreign = File(trash, "unexpected-note.txt").apply { writeText("keep") }
            assertFalse(manager.permanentlyDelete(untouched))
            assertEquals(SafeTrashPurgeStatus.INVALID_ENTRY, manager.purgeEntry("../keep-me.apk").status)

            val result = manager.purgeAll()
            assertFalse(result.completed)
            assertEquals(1, result.deletedEntries)
            assertEquals(1, result.rejectedEntries)
            assertTrue(foreign.exists())
            assertTrue(untouched.exists())
            assertTrue(manager.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }


    @Test
    fun fiftyMebibyteApkIsTrashedAndRestoredWithBytesIntact() {
        val root = createTempDirectory("omni-large-apk-trash-").toFile()
        val trash = File(root, ".OmniCleaner/SafeTrash")
        val source = File(root, "installer.apk")
        try {
            RandomAccessFile(source, "rw").use { raf ->
                raf.setLength(50L * 1024L * 1024L)
                raf.seek(24L * 1024L * 1024L)
                raf.write(byteArrayOf(3, 8, 12, 20, 25))
            }
            val manager = SafeTrashManager.forTests(trash, root)
            val payload = manager.moveToTrash(source)
            assertNotNull(payload)
            assertFalse(source.exists())
            assertEquals(50L * 1024L * 1024L, manager.getTrashSize())
            val entry = manager.listEntries().single()
            assertEquals("installer.apk", entry.originalName)
            assertEquals(50L * 1024L * 1024L, entry.sizeBytes)

            val restored = manager.restore(entry.id)
            assertEquals(SafeTrashRestoreStatus.RESTORED, restored.status)
            assertTrue(source.isFile)
            assertEquals(50L * 1024L * 1024L, source.length())
            RandomAccessFile(source, "r").use { raf ->
                raf.seek(24L * 1024L * 1024L)
                assertEquals(3, raf.read())
                assertEquals(8, raf.read())
                assertEquals(12, raf.read())
                assertEquals(20, raf.read())
                assertEquals(25, raf.read())
            }
            assertTrue(manager.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun overLimitApkIsBlockedAndPreserved() {
        val root = createTempDirectory("omni-overlimit-apk-trash-").toFile()
        val source = File(root, "oversized.apk")
        try {
            RandomAccessFile(source, "rw").use { raf ->
                raf.setLength(SafeTrashManager.MAX_REVERSIBLE_TRASH_BYTES + 1L)
            }
            val manager = SafeTrashManager.forTests(File(root, "trash"), root)
            assertTrue(manager.moveToTrash(source) == null)
            assertTrue(source.exists())
            assertEquals(SafeTrashManager.MAX_REVERSIBLE_TRASH_BYTES + 1L, source.length())
            assertTrue(manager.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

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
            manager.migrateLegacyTrashIfNeeded()

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
    fun failedLegacyMigrationKeepsSourceUntouched() {
        val root = createTempDirectory("omni-trash-migrate-fail-").toFile()
        val legacy = File(root, "legacy-trash")
        val persistent = File(root, "persistent-trash")
        val destination = File(root, "destination.txt")
        val entryDir = File(legacy, UUID.randomUUID().toString()).apply { mkdirs() }
        val payload = File(entryDir, "payload").apply { writeText("original-bytes") }
        val metadata = File(entryDir, "original.properties")
        Properties().apply {
            setProperty("originalPath", destination.canonicalPath)
            setProperty("originalName", destination.name)
            setProperty("sizeBytes", payload.length().toString())
            setProperty("lastModified", "0")
            setProperty("sha256", "0".repeat(64))
        }.also { properties ->
            metadata.outputStream().use { output -> properties.store(output, "tampered") }
        }

        try {
            val manager = SafeTrashManager.forTests(
                trashDirectory = persistent,
                restoreRoot = root,
                legacyTrashDirectory = legacy,
            )
            manager.migrateLegacyTrashIfNeeded()

            assertTrue(entryDir.isDirectory)
            assertTrue(payload.isFile)
            assertTrue(manager.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun constructorDoesNotRunLegacyMigrationSynchronously() {
        val root = createTempDirectory("omni-trash-deferred-").toFile()
        val legacy = File(root, "legacy-trash")
        val persistent = File(root, "persistent-trash")
        val entryId = UUID.randomUUID().toString()
        val legacyEntry = File(legacy, entryId).apply { mkdirs() }
        val payload = File(legacyEntry, "payload").apply { writeText("deferred") }
        val metadata = File(legacyEntry, "original.properties")
        Properties().apply {
            setProperty("originalPath", File(root, "restore.txt").canonicalPath)
            setProperty("originalName", "restore.txt")
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

            assertTrue(legacyEntry.isDirectory)
            assertTrue(manager.listEntries().isEmpty())

            manager.migrateLegacyTrashIfNeeded()

            assertFalse(legacyEntry.exists())
            assertEquals(1, manager.listEntries().size)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun interruptedLegacyMigrationIsRepairedAndResumed() {
        val root = createTempDirectory("omni-trash-resume-").toFile()
        val legacy = File(root, "legacy-trash")
        val persistent = File(root, "persistent-trash")
        val entryId = UUID.randomUUID().toString()
        val original = File(root, "resume.txt")
        val legacyEntry = File(legacy, entryId).apply { mkdirs() }
        val legacyPayload = File(legacyEntry, "payload").apply { writeText("complete-payload") }
        val legacyMetadata = File(legacyEntry, "original.properties")
        Properties().apply {
            setProperty("originalPath", original.canonicalPath)
            setProperty("originalName", original.name)
            setProperty("sizeBytes", legacyPayload.length().toString())
            setProperty("lastModified", "0")
        }.also { properties ->
            legacyMetadata.outputStream().use { output -> properties.store(output, "legacy") }
        }

        val partialTarget = File(persistent, entryId).apply { mkdirs() }
        File(partialTarget, "original.properties").writeText("partial")
        File(partialTarget, "payload").writeText("cut")

        try {
            val manager = SafeTrashManager.forTests(
                trashDirectory = persistent,
                restoreRoot = root,
                legacyTrashDirectory = legacy,
            )
            manager.migrateLegacyTrashIfNeeded()

            assertFalse(legacyEntry.exists())
            // Preserve the incomplete older target rather than delete user data.
            assertEquals("cut", File(partialTarget, "payload").readText())
            val migrated = manager.listEntries().single()
            assertTrue(migrated.id != entryId)
            assertEquals("complete-payload",
                File(File(persistent, migrated.id), "payload").readText())
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
            manager.migrateLegacyTrashIfNeeded()

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


    @Test
    fun thirtyFiveSyntheticEntriesCanBeReviewedRestoredAndDeletedIndividually() {
        val root = createTempDirectory("omni-trash-many-").toFile()
        val trash = File(root, "trash")
        try {
            val manager = SafeTrashManager.forTests(trash)
            for (index in 1..35) {
                val source = File(root, "sample-$index.txt").apply {
                    writeText("synthetic-only-$index")
                }
                assertNotNull("Move failed for synthetic entry $index", manager.moveToTrash(source))
                assertFalse(source.exists())
            }

            // The old visual 12-row limit must not conceal backend entries.
            assertEquals(35, manager.listEntries().size)
            val last = manager.listEntries().single { it.originalName == "sample-35.txt" }
            val restored = manager.restore(last.id)
            assertEquals(SafeTrashRestoreStatus.RESTORED, restored.status)
            assertEquals("synthetic-only-35", File(root, "sample-35.txt").readText())
            assertEquals(34, manager.listEntries().size)

            val first = manager.listEntries().single { it.originalName == "sample-1.txt" }
            assertTrue(manager.permanentlyDelete(File(trash, first.id)))
            assertEquals(33, manager.listEntries().size)
            assertFalse(File(root, "sample-1.txt").exists())
            // Only synthetic files in this temporary test root were touched.
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun purgeAndRestoreFailClosedWhenEntryContainsUnexpectedUserData() {
        val root = createTempDirectory("omni-unknown-entry-").toFile()
        try {
            val trash = File(root, "safe-trash")
            val manager = SafeTrashManager.forTests(trash, root)
            val original = File(root, "retain-me.txt").apply { writeText("valuable data") }
            assertNotNull(manager.moveToTrash(original))
            val entry = manager.listEntries().single()
            val entryDirectory = File(trash, entry.id)
            val foreign = File(entryDirectory, "user-extra.txt").apply { writeText("untouched") }

            assertEquals(SafeTrashPurgeStatus.INVALID_ENTRY, manager.purgeEntry(entry.id).status)
            assertEquals(SafeTrashRestoreStatus.PAYLOAD_INVALID, manager.restore(entry.id).status)
            assertTrue(foreign.isFile)
            assertEquals("untouched", foreign.readText())
            assertTrue(File(entryDirectory, "payload").isFile)
            assertTrue(File(entryDirectory, "original.properties").isFile)
            assertFalse(original.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun automaticLegacyMigrationNeverRemovesUnknownChildren() {
        val root = createTempDirectory("omni-migration-unknown-").toFile()
        try {
            val legacy = File(root, "legacy")
            val persistent = File(root, "persistent")
            val entry = File(legacy, UUID.randomUUID().toString()).apply { mkdirs() }
            val payload = File(entry, "payload").apply { writeText("protect this") }
            val metadata = File(entry, "original.properties")
            Properties().apply {
                setProperty("originalPath", File(root, "restore.txt").canonicalPath)
                setProperty("originalName", "restore.txt")
                setProperty("sizeBytes", payload.length().toString())
                setProperty("lastModified", "0")
            }.also { p -> metadata.outputStream().use { out -> p.store(out, "legacy") } }
            val foreign = File(entry, "personal-note.txt").apply { writeText("not owned by cleaner") }

            val manager = SafeTrashManager.forTests(persistent, root, legacy)
            manager.migrateLegacyTrashIfNeeded()

            assertEquals("protect this", payload.readText())
            assertTrue(metadata.isFile)
            assertEquals("not owned by cleaner", foreign.readText())
            assertTrue(entry.isDirectory)
            assertTrue(manager.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun existingMatchingBytesWithDifferentOriginalPathCannotConsumeLegacyEntry() {
        val root = createTempDirectory("omni-migration-collision-").toFile()
        try {
            val legacy = File(root, "legacy")
            val persistent = File(root, "persistent")
            val id = UUID.randomUUID().toString()
            val legacyEntry = File(legacy, id).apply { mkdirs() }
            val legacyPayload = File(legacyEntry, "payload").apply { writeText("same bytes") }
            val original = File(root, "real-original.txt")
            Properties().apply {
                setProperty("originalPath", original.canonicalPath)
                setProperty("originalName", original.name)
                setProperty("sizeBytes", legacyPayload.length().toString())
                setProperty("lastModified", "0")
            }.also { p ->
                File(legacyEntry, "original.properties").outputStream()
                    .use { out -> p.store(out, "legacy") }
            }

            val manager = SafeTrashManager.forTests(persistent, root, legacy)
            val collision = File(persistent, id).apply { mkdirs() }
            val otherOriginal = File(root, "different-original.txt")
            val collisionPayload = File(collision, "payload").apply { writeText("same bytes") }
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(collisionPayload.readBytes())
                .joinToString("") { "%02x".format(it) }
            Properties().apply {
                setProperty("originalPath", otherOriginal.canonicalPath)
                setProperty("originalName", otherOriginal.name)
                setProperty("sizeBytes", collisionPayload.length().toString())
                setProperty("lastModified", "0")
                setProperty("sha256", digest)
            }.also { p ->
                File(collision, "original.properties").outputStream()
                    .use { out -> p.store(out, "collision") }
            }

            manager.migrateLegacyTrashIfNeeded()

            val entries = manager.listEntries()
            assertEquals(2, entries.size)
            assertTrue(entries.any { it.id == id && it.originalPath == otherOriginal.canonicalPath })
            assertTrue(entries.any { it.id != id && it.originalPath == original.canonicalPath })
            assertTrue(collisionPayload.isFile)
            assertEquals("same bytes", collisionPayload.readText())
        } finally {
            root.deleteRecursively()
        }
    }


    @Test
    fun purgeNeverDestroysPayloadModifiedAfterItWasTrashed() {
        val root = createTempDirectory("omni-purge-hash-").toFile()
        try {
            val trash = File(root, "safe-trash")
            val manager = SafeTrashManager.forTests(trash, root)
            val source = File(root, "user-data.txt").apply { writeText("original") }
            assertNotNull(manager.moveToTrash(source))
            val entry = manager.listEntries().single()
            val payload = File(File(trash, entry.id), "payload")
            payload.writeText("modified") // Same length, deliberately different SHA-256.

            assertEquals(SafeTrashPurgeStatus.INVALID_ENTRY, manager.purgeEntry(entry.id).status)
            assertFalse(manager.purgeAll().completed)
            assertTrue(payload.isFile)
            assertEquals("modified", payload.readText())
            assertTrue(File(File(trash, entry.id), "original.properties").isFile)
        } finally {
            root.deleteRecursively()
        }
    }

}
