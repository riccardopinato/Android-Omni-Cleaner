package com.riccardopinato.omnicleaner.exif

import androidx.exifinterface.media.ExifInterface
import java.io.File

object JpegExifEditor {
    private val COLOR_TAGS = listOf(
        "ColorSpace",
        "Gamma",
        "WhitePoint",
        "PrimaryChromaticities",
        "YCbCrCoefficients",
        "YCbCrPositioning",
        "ReferenceBlackWhite",
    )

    private const val ORIENTATION_TAG = "Orientation"
    private const val XMP_TAG = "Xmp"

    fun cleanTemporaryCopy(
        file: File,
        groups: Set<ExifGroup>,
        checkpoint: () -> Unit = {},
    ) {
        require(file.isFile) { "JPEG richiesto" }
        if (groups.isEmpty()) return

        checkpoint()
        val beforeDigest = file.inputStream().buffered().use {
            JpegContentDigest.read(it, checkpoint)
        }

        val before = ExifInterface(file.absolutePath)
        val selectedTags = ExifGroup.selectedTags(groups)
        val protectedColors = COLOR_TAGS.associateWith(before::getAttribute)
        val orientation = before.getAttribute(ORIENTATION_TAG)
        validateOrientation(orientation)

        val xmp = before.getAttribute(XMP_TAG)
        if (
            xmp?.contains("MotionPhoto", ignoreCase = true) == true ||
            xmp?.contains("MicroVideo", ignoreCase = true) == true
        ) {
            error("Motion Photo non supportata")
        }

        for (tag in selectedTags) {
            checkpoint()
            before.setAttribute(tag, null)
        }
        before.saveAttributes()

        checkpoint()
        val after = ExifInterface(file.absolutePath)

        if (selectedTags.any { after.getAttribute(it) != null }) {
            error("Tag ancora presenti")
        }

        val afterOrientation = after.getAttribute(ORIENTATION_TAG)
        validateOrientation(afterOrientation)
        if (orientation != afterOrientation) {
            error("Orientamento cambiato")
        }

        for ((tag, value) in protectedColors) {
            if (value != after.getAttribute(tag)) {
                error("Metadati colore modificati")
            }
        }

        if (xmp != after.getAttribute(XMP_TAG)) {
            error("XMP modificato inaspettatamente")
        }

        val afterDigest = file.inputStream().buffered().use {
            JpegContentDigest.read(it, checkpoint)
        }

        if (!beforeDigest.contentEquals(afterDigest)) {
            error("Dati immagine o segmenti non EXIF modificati")
        }
    }

    private fun validateOrientation(raw: String?) {
        val value = raw?.toIntOrNull() ?: return
        if (value !in 1..8) {
            error("Orientamento non valido")
        }
    }
}
