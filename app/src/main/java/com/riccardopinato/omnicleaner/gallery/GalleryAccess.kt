package com.riccardopinato.omnicleaner.gallery

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object GalleryAccess {
    fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasAccess(context: Context): Boolean =
        requiredPermissions().all { permission ->
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
}
