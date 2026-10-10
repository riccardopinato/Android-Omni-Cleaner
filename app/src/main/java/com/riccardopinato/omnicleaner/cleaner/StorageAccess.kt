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
    fun primaryRoot(): File = Environment.getExternalStorageDirectory()

    fun capabilityHealth(context: Context): StorageCapabilityHealth {
        val mounted = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        val legacyReadGranted =
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        return evaluateStorageCapability(
            sdkInt = Build.VERSION.SDK_INT,
            allFilesGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Environment.isExternalStorageManager(),
            legacyReadGranted = legacyReadGranted,
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
