package com.riccardopinato.omnicleaner.cleaner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import java.io.File

object StorageAccess {
    internal const val APP_MANAGED_DIRECTORY = ".OmniCleaner"
    internal const val SAFE_TRASH_DIRECTORY = "SafeTrash"
    fun primaryRoot(): File = Environment.getExternalStorageDirectory()

    /**
     * User-owned reversible trash must survive application uninstall. Keep the
     * payload outside app-private filesDir, under a hidden app-managed folder in
     * primary shared storage. Deep Cleaning access is required to mutate/restore
     * entries on Android versions that enforce scoped storage.
     */
    fun safeTrashRoot(root: File = primaryRoot()): File =
        File(File(root, APP_MANAGED_DIRECTORY), SAFE_TRASH_DIRECTORY)

    fun capabilityHealth(context: Context): StorageCapabilityHealth {
        val mounted = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        val legacyReadGranted =
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val legacyWriteGranted =
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        return evaluateStorageCapability(
            sdkInt = Build.VERSION.SDK_INT,
            allFilesGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Environment.isExternalStorageManager(),
            legacyReadGranted = legacyReadGranted,
            legacyWriteGranted = legacyWriteGranted,
            storageMounted = mounted,
        )
    }

    fun hasBroadAccess(context: Context): Boolean =
        capabilityHealth(context).deepScanAvailable

    fun settingsIntent(context: Context): Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    }

    fun storageInfo(root: File = primaryRoot()): StorageInfo {
        val stat = StatFs(root.absolutePath)
        return StorageInfo(
            totalBytes = stat.totalBytes.coerceAtLeast(0L),
            freeBytes = stat.availableBytes.coerceAtLeast(0L),
        )
    }
}
