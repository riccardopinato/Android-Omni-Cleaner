package com.riccardopinato.omnicleaner.cleaner

import java.io.File

object MediaStoreHelper {
    private val mediaExtensions = setOf(
        "jpg", "jpeg", "png", "webp", "heic", "heif", "gif", "bmp",
        "mp4", "mkv", "webm", "mov", "avi", "3gp", "m4v",
        "mp3", "aac", "m4a", "wav", "flac", "ogg", "opus",
    )

    fun isMediaFile(file: File): Boolean =
        file.isFile && file.extension.lowercase() in mediaExtensions
}
