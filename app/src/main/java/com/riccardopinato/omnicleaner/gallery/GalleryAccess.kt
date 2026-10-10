package com.riccardopinato.omnicleaner.gallery

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

enum class GalleryAccessLevel {
    FULL,
    PARTIAL,
    NONE,
}

data class GalleryAccessHealth(
    val level: GalleryAccessLevel,
    val canReadImages: Boolean,
    val canReadVideos: Boolean,
) {
    val hasAccess: Boolean
        get() = level != GalleryAccessLevel.NONE
}

internal fun evaluateGalleryAccess(
    sdkInt: Int,
    imagesGranted: Boolean,
    videosGranted: Boolean,
    selectedVisualGranted: Boolean,
    legacyReadGranted: Boolean,
): GalleryAccessHealth = when {
    sdkInt >= 34 -> {
        val selected = selectedVisualGranted
        val images = imagesGranted || selected
        val videos = videosGranted || selected
        val level = when {
            imagesGranted && videosGranted -> GalleryAccessLevel.FULL
            images || videos -> GalleryAccessLevel.PARTIAL
            else -> GalleryAccessLevel.NONE
        }
        GalleryAccessHealth(level, images, videos)
    }

    sdkInt >= 33 -> {
        val level = when {
            imagesGranted && videosGranted -> GalleryAccessLevel.FULL
            imagesGranted || videosGranted -> GalleryAccessLevel.PARTIAL
            else -> GalleryAccessLevel.NONE
        }
        GalleryAccessHealth(level, imagesGranted, videosGranted)
    }

    legacyReadGranted -> GalleryAccessHealth(
        level = GalleryAccessLevel.FULL,
        canReadImages = true,
        canReadVideos = true,
    )

    else -> GalleryAccessHealth(
        level = GalleryAccessLevel.NONE,
        canReadImages = false,
        canReadVideos = false,
    )
}

object GalleryAccess {
    fun requiredPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )

        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun health(context: Context): GalleryAccessHealth {
        fun granted(permission: String): Boolean =
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

        return evaluateGalleryAccess(
            sdkInt = Build.VERSION.SDK_INT,
            imagesGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                granted(Manifest.permission.READ_MEDIA_IMAGES),
            videosGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                granted(Manifest.permission.READ_MEDIA_VIDEO),
            selectedVisualGranted = Build.VERSION.SDK_INT >= 34 &&
                granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
            legacyReadGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                granted(Manifest.permission.READ_EXTERNAL_STORAGE),
        )
    }

    fun hasAccess(context: Context): Boolean = health(context).hasAccess
}
