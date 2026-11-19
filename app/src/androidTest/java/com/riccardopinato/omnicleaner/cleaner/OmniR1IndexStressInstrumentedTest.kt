package com.riccardopinato.omnicleaner.cleaner

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Android SQLite repeatability and correctness gate for the R1 prepared UPDATE.
 *
 * This emits factual measured timings from the emulator, not a claimed physical
 * speedup. It intentionally sets no wall-clock performance threshold.
 */
class OmniR1IndexStressInstrumentedTest {
    @Test
    fun unchangedRowsReuseWithBoundedMetricsAcrossRepeatedReconciliations() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "r1-index-stress-" + UUID.randomUUID() + ".db"
        val root = File(context.cacheDir, "r1-index-stress-" + UUID.randomUUID())
        assertTrue(root.mkdirs())
        try {
            repeat(FIXTURE_COUNT) { i ->
                File(root, "item-" + i.toString().padStart(5, '0') + ".bin")
                    .writeText("synthetic indexed data " + i)
            }
            val inventory = FileInventoryScanner().collect(root)
            assertEquals(FIXTURE_COUNT, inventory.files.size)
            val index = PersistentStorageIndex(context, databaseName)
            index.reconcileFull(root, inventory, System.currentTimeMillis())

            val revisionsAvailable = inventory.files.all { it.revision != null }
            val sample = inventory.files.first()
            if (revisionsAvailable) {
                val realDigest = FileInventoryScanner().digest(sample.file)
                index.putFullHash(sample, realDigest)
                assertEquals(realDigest, index.fullHash(sample))
            }

            val reports = mutableListOf<IndexReconcileDiagnostics>()
            repeat(3) { iteration ->
                var report: IndexReconcileDiagnostics? = null
                index.reconcileFull(
                    root = root,
                    inventory = inventory,
                    completedAtMillis = System.currentTimeMillis(),
                    onMetrics = { report = it },
                )
                val measured = requireNotNull(report)
                reports += measured
                assertEquals(FIXTURE_COUNT, measured.filesReused + measured.filesReplaced)
                assertEquals(inventory.directories.size, measured.directoriesWritten)
                assertTrue(measured.totalMillis >= 0L)
                assertTrue(measured.fileRowsMillis >= 0L)
                if (revisionsAvailable) {
                    assertEquals(FIXTURE_COUNT, measured.filesReused)
                    assertEquals(0, measured.filesReplaced)
                    assertEquals(
                        FileInventoryScanner().digest(sample.file),
                        index.fullHash(sample),
                    )
                }
                // Only counts/durations, never real user paths or private content.
                Log.i(
                    "OMNI_R1_INDEX_METRICS",
                    "pass=" + iteration +
                        " indexed_files=" + FIXTURE_COUNT +
                        " reused=" + measured.filesReused +
                        " replaced=" + measured.filesReplaced +
                        " file_rows_ms=" + measured.fileRowsMillis +
                        " total_ms=" + measured.totalMillis,
                )
            }
            val snapshot = index.loadReusableSnapshot(
                root, nowMillis = System.currentTimeMillis(), maxAgeMillis = 60_000L,
            )
            assertNotNull(snapshot)
            assertEquals(FIXTURE_COUNT, requireNotNull(snapshot).inventory.files.size)
            assertEquals(3, reports.size)
        } finally {
            root.deleteRecursively()
            context.deleteDatabase(databaseName)
            context.getSharedPreferences(
                "storage_index_state_" + databaseName, android.content.Context.MODE_PRIVATE,
            ).edit().clear().commit()
        }
    }

    private companion object {
        const val FIXTURE_COUNT = 1_200
    }
}
