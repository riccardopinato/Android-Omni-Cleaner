package com.riccardopinato.omnicleaner

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.riccardopinato.omnicleaner.cleaner.SafeTrashManager
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppContractInstrumentedTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun launcherIdentityHasRealIconAndLabel() {
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
        assertNotEquals(0, appInfo.icon)
        assertTrue(context.packageManager.getApplicationLabel(appInfo).isNotBlank())
    }

    @Test
    fun mainActivityIsExportedLauncherSurface() {
        val activityInfo = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            0,
        )
        assertTrue(activityInfo.exported)
    }

    @Test
    fun deepAndScopedMediaPermissionContractIsDeclared() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val permissions = packageInfo.requestedPermissions.orEmpty().toSet()

        assertTrue(Manifest.permission.MANAGE_EXTERNAL_STORAGE in permissions)
        assertTrue(Manifest.permission.READ_MEDIA_IMAGES in permissions)
        assertTrue(Manifest.permission.READ_MEDIA_VIDEO in permissions)
    }

    @Test
    fun safeTrashIsNotAppPrivateStorage() {
        val manager = SafeTrashManager(context)
        val privatePath = context.filesDir.canonicalPath
        val trashPath = manager.getTrashDirectory().canonicalPath

        assertFalse(trashPath == privatePath || trashPath.startsWith("$privatePath/"))
        assertTrue(
            trashPath.startsWith(
                StorageAccess.primaryRoot().canonicalPath.trimEnd('/') + "/",
            ),
        )
    }
}
