package com.riccardopinato.omnicleaner.cleaner

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentStorageIndexInstrumentedTest {
    @Test
    fun persistedInventorySurvivesRepositoryObjectRestartAndInvalidatesChangedHash() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "storage-index-test-${UUID.randomUUID()}.db"
        val root = File(context.cacheDir, "persistent-index-${UUID.randomUUID()}").apply { mkdirs() }
        val file = File(root, "sample.pdf").apply {
            writeText("version-one")
            setLastModified(1_000L)
        }

        try {
            val firstInventory = FileInventoryScanner().collect(root)
            val firstItem = firstInventory.files.single()
            assertTrue(firstInventory.directories.isNotEmpty())
            val first = PersistentStorageIndex(context, databaseName)
            first.reconcileFull(root, firstInventory, completedAtMillis = 10_000L)
            first.putSampleHash(firstItem, "a".repeat(64))
            first.putFullHash(firstItem, "b".repeat(64))

            val restarted = PersistentStorageIndex(context, databaseName)
            val warm = restarted.loadReusableSnapshot(
                root = root,
                nowMillis = 11_000L,
                maxAgeMillis = 60_000L,
            )

            assertNotNull(warm)
            assertEquals(1, warm!!.inventory.files.size)
            assertEquals("a".repeat(64), restarted.sampleHash(firstItem))
            assertEquals("b".repeat(64), restarted.fullHash(firstItem))

            file.writeText("version-two-is-different")
            file.setLastModified(2_000L)
            val changedInventory = FileInventoryScanner().collect(root)
            val changedItem = changedInventory.files.single()
            restarted.reconcileFull(
                root = root,
                inventory = changedInventory,
                completedAtMillis = 20_000L,
            )

            assertNull(restarted.sampleHash(changedItem))
            assertNull(restarted.fullHash(changedItem))

            val refreshed = restarted.loadReusableSnapshot(
                root = root,
                nowMillis = 21_000L,
                maxAgeMillis = 60_000L,
            )
            assertNotNull(refreshed)
            assertEquals(changedItem.size, refreshed!!.inventory.files.single().size)

            restarted.markStale()
            assertNull(
                restarted.loadReusableSnapshot(
                    root = root,
                    nowMillis = 22_000L,
                    maxAgeMillis = 60_000L,
                ),
            )
        } finally {
            root.deleteRecursively()
            context.deleteDatabase(databaseName)
            context.getSharedPreferences(
                "storage_index_state_$databaseName",
                android.content.Context.MODE_PRIVATE,
            ).edit().clear().commit()
        }
    }
    @Test
    fun directoryMutationRejectsWarmSnapshot() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "storage-index-dir-test-${UUID.randomUUID()}.db"
        val root = File(context.cacheDir, "persistent-index-dir-${UUID.randomUUID()}").apply {
            mkdirs()
        }
        File(root, "first.txt").writeText("one")

        try {
            val index = PersistentStorageIndex(context, databaseName)
            val firstInventory = FileInventoryScanner().collect(root)
            index.reconcileFull(root, firstInventory, completedAtMillis = 10_000L)

            assertNotNull(
                index.loadReusableSnapshot(
                    root = root,
                    nowMillis = 11_000L,
                    maxAgeMillis = 60_000L,
                ),
            )

            File(root, "new-non-media.bin").writeText("new")
            // Ensure a visible directory revision change even on filesystems
            // with coarse timestamp behavior in instrumentation environments.
            root.setLastModified(System.currentTimeMillis() + 2_000L)

            assertNull(
                index.loadReusableSnapshot(
                    root = root,
                    nowMillis = 12_000L,
                    maxAgeMillis = 60_000L,
                ),
            )
        } finally {
            root.deleteRecursively()
            context.deleteDatabase(databaseName)
            context.getSharedPreferences(
                "storage_index_state_$databaseName",
                android.content.Context.MODE_PRIVATE,
            ).edit().clear().commit()
        }
    }

    @Test
    fun v1DatabaseMigratesWithoutDestructiveReset() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "storage-index-migration-${UUID.randomUUID()}.db"
        val root = File(context.cacheDir, "persistent-index-migration-${UUID.randomUUID()}").apply {
            mkdirs()
        }
        val file = File(root, "legacy.pdf").apply { writeText("legacy-index") }

        try {
            val metadata = requireNotNull(FileSystemIdentity.read(file))
            val dbPath = context.getDatabasePath(databaseName)
            dbPath.parentFile?.mkdirs()
            val db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)
            db.execSQL(
                """
                CREATE TABLE indexed_files (
                    path TEXT PRIMARY KEY NOT NULL,
                    size_bytes INTEGER NOT NULL,
                    modified_at INTEGER NOT NULL,
                    extension TEXT NOT NULL,
                    device_id INTEGER,
                    inode INTEGER,
                    ctime_seconds INTEGER,
                    ctime_nanos INTEGER,
                    sample_hash TEXT,
                    full_hash TEXT,
                    last_seen_generation INTEGER NOT NULL,
                    last_verified_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE index_meta (
                    meta_key TEXT PRIMARY KEY NOT NULL,
                    meta_value TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO indexed_files (
                    path, size_bytes, modified_at, extension,
                    device_id, inode, ctime_seconds, ctime_nanos,
                    sample_hash, full_hash, last_seen_generation, last_verified_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    file.absolutePath,
                    metadata.sizeBytes,
                    metadata.modifiedMillis,
                    "pdf",
                    metadata.physicalIdentity?.deviceId,
                    metadata.physicalIdentity?.inode,
                    metadata.revision?.changeTimeSeconds,
                    metadata.revision?.changeTimeNanos,
                    null,
                    null,
                    1L,
                    9_000L,
                ),
            )
            db.execSQL(
                "INSERT INTO index_meta(meta_key, meta_value) VALUES(?, ?)",
                arrayOf("root_path", root.canonicalPath),
            )
            db.execSQL(
                "INSERT INTO index_meta(meta_key, meta_value) VALUES(?, ?)",
                arrayOf("last_complete_scan_at", "10000"),
            )
            db.execSQL(
                "INSERT INTO index_meta(meta_key, meta_value) VALUES(?, ?)",
                arrayOf("scan_generation", "1"),
            )
            db.version = 1
            db.close()

            val migrated = PersistentStorageIndex(context, databaseName)

            // v1 has no directory fingerprints, so migration is non-destructive
            // but the old snapshot is not trusted as a warm-complete view.
            assertNull(
                migrated.loadReusableSnapshot(
                    root = root,
                    nowMillis = 11_000L,
                    maxAgeMillis = 60_000L,
                ),
            )

            val reconciled = FileInventoryScanner().collect(root)
            migrated.reconcileFull(root, reconciled, completedAtMillis = 12_000L)

            val warm = migrated.loadReusableSnapshot(
                root = root,
                nowMillis = 13_000L,
                maxAgeMillis = 60_000L,
            )
            assertNotNull(warm)
            assertEquals(1, warm!!.inventory.files.size)
            assertTrue(warm.inventory.directories.isEmpty())
        } finally {
            root.deleteRecursively()
            context.deleteDatabase(databaseName)
            context.getSharedPreferences(
                "storage_index_state_$databaseName",
                android.content.Context.MODE_PRIVATE,
            ).edit().clear().commit()
        }
    }

}
