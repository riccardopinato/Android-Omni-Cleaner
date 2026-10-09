package com.riccardopinato.omnicleaner.cleaner.exif

import androidx.exifinterface.media.ExifInterface
import java.io.File

object JpegExifEditor {
    fun cleanTemporaryCopy(
        file: File,
        groups: Set<ExifGroup>,
        cancelCheck: () -> Unit = {},
    ) {
        require(file.isFile) { "Temporary JPEG copy missing" }
        val exif = ExifInterface(file.absolutePath)
        ExifGroup.allTags(groups).forEach { tag ->
            cancelCheck()
            exif.setAttribute(tag, null)
        }
        cancelCheck()
        exif.saveAttributes()
    }
}
