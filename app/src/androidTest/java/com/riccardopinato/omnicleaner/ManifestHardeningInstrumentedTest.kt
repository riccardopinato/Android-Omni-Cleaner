package com.riccardopinato.omnicleaner

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestHardeningInstrumentedTest {
    @Test
    fun deepCleaningPermissionIsDeclared() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        assertTrue(info.requestedPermissions.orEmpty().contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE))
    }
}
