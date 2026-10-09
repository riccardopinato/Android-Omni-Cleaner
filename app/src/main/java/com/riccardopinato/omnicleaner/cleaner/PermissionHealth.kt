package com.riccardopinato.omnicleaner.cleaner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

enum class StorageCapabilityHealth {
    HEALTHY,
    LIMITED,
    DENIED,
}

object PermissionHealth {
    fun allFilesGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun mediaReadGranted(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val image = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                val video = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
                image == PackageManager.PERMISSION_GRANTED || video == PackageManager.PERMISSION_GRANTED
            }
            else -> ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun storageHealth(context: Context): StorageCapabilityHealth {
        if (allFilesGranted()) return StorageCapabilityHealth.HEALTHY
        if (mediaReadGranted(context)) return StorageCapabilityHealth.LIMITED
        return StorageCapabilityHealth.DENIED
    }
}
