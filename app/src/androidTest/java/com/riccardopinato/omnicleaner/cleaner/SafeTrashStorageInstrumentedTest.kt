package com.riccardopinato.omnicleaner.cleaner

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeTrashStorageInstrumentedTest {
    @Test
    fun safeTrashIsNotAppPrivateStorage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val trash = StorageAccess.safeTrashRoot().canonicalFile
        val privateFiles = context.filesDir.canonicalFile

        assertFalse(trash.path.startsWith(privateFiles.path))
        assertTrue(trash.path.contains(StorageAccess.APP_MANAGED_DIRECTORY))
        assertTrue(trash.path.endsWith(StorageAccess.SAFE_TRASH_DIRECTORY))
    }
}
