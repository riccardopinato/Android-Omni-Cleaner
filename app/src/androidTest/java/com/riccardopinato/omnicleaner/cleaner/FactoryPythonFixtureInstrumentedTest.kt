package com.riccardopinato.omnicleaner.cleaner

import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Independent Python App Factory fixture consumed by the REAL Android scanner.
 *
 * Dedicated CI injects the synthetic fixture into the disposable Android
 * emulator; no user media or non-synthetic files are deleted or modified.
 * This case does not certify OEM storage, MediaStore consent or physical speed.
 */
class FactoryPythonFixtureInstrumentedTest {
    @Test
    fun pythonGeneratedManifestMatchesNativeDuplicateGroupsAndHashes() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        // Explicitly emulator-only: no scripted file permission mutation on real phones.
        val emulator = Build.HARDWARE == "ranchu" ||
            Build.HARDWARE == "goldfish" ||
            Build.PRODUCT.contains("sdk_gphone") ||
            Build.FINGERPRINT.contains("emulator", ignoreCase = true)
        assertTrue("This cross-app fixture gate runs only on a disposable emulator", emulator)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand(
                    "appops set " + context.packageName + " MANAGE_EXTERNAL_STORAGE allow",
                ),
            ).use { it.readBytes() }
            assertTrue("Debug All files access must be authorized", Environment.isExternalStorageManager())
        }

        val root = File(StorageAccess.primaryRoot(), "Download/omni-python-qa-fixture")
        assertTrue("Python fixture was not pushed into the emulator", root.isDirectory)
        val manifest = JSONObject(File(root, "manifest.json").readText())
        assertEquals(1, manifest.getInt("schema_version"))
        assertEquals(512, manifest.getInt("files"))
        assertEquals(8, manifest.getInt("duplicate_pairs"))
        val expectedRecords = manifest.getJSONArray("records")
        val expectedDuplicateHashes = linkedSetOf<String>()
        val countsByHash = HashMap<String, Int>()
        assertEquals(512, expectedRecords.length())

        for (i in 0 until expectedRecords.length()) {
            val row = expectedRecords.getJSONObject(i)
            val name = row.getString("name")
            assertEquals("synthetic_" + i.toString().padStart(6, '0') + ".bin", name)
            val file = File(root, name)
            assertTrue("Synthetic fixture file missing", file.isFile)
            assertEquals(row.getLong("size"), file.length())
            val hash = hash(file)
            assertEquals("Native digest must match Python SHA-256 reference", row.getString("sha256"), hash)
            countsByHash[hash] = (countsByHash[hash] ?: 0) + 1
        }
        for ((digest, count) in countsByHash) {
            assertTrue("No family may contain more than two files", count <= 2)
            if (count == 2) expectedDuplicateHashes += digest
        }
        assertEquals(8, expectedDuplicateHashes.size)

        val scanner = FileInventoryScanner()
        val inventory = scanner.collect(root)
        val files = inventory.files.filter { it.file.extension == "bin" }
        assertEquals(512, files.size)
        val groups = scanner.duplicatesProgressive(files, parallelism = 2)
        assertEquals(8, groups.size)
        assertEquals(expectedDuplicateHashes, groups.map { it.hash }.toSet())
        assertTrue(groups.all { it.files.size == 2 })
        // Read-only test. No permanent deletions, Safe Trash mutation or MediaStore request.
    }

    private fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { stream ->
            val bytes = ByteArray(32 * 1024)
            while (true) {
                val read = stream.read(bytes)
                if (read < 0) break
                if (read > 0) digest.update(bytes, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
