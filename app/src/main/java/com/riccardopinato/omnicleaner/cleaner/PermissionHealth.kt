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
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager()
    }

    fun mediaReadGranted(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val image = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES,
                )
                val video = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO,
                )
                image == PackageManager.PERMISSION_GRANTED ||
                    video == PackageManager.PERMISSION_GRANTED
            }

            else -> ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun legacyStorageGranted(context: Context): Boolean {
        val read = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return read

        val write = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
        return read && write
    }

    fun storageHealth(context: Context): StorageCapabilityHealth {
        val deepHealthy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            allFilesGranted()
        } else {
            legacyStorageGranted(context)
        }
        if (deepHealthy) return StorageCapabilityHealth.HEALTHY
        if (mediaReadGranted(context)) return StorageCapabilityHealth.LIMITED
        return StorageCapabilityHealth.DENIED
    }
}
