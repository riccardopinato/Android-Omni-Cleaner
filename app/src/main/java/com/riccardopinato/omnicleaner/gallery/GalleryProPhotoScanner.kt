package com.riccardopinato.omnicleaner.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal data class GalleryPhotoScanSummary(
    val analyzed: Int,
    val eligible: Int,
    val candidateGroups: List<GallerySimilarPhotoGroup>,
)

/**
 * Only reads permission-scoped, downsampled MediaStore thumbnails. Max 160.
 * Does not read or alter original photo payloads, and keeps no thumbnails.
 */
internal object GalleryProPhotoScanner {
    suspend fun analyze(
        context: Context,
        items: List<GalleryMediaItem>,
    ): GalleryPhotoScanSummary = withContext(Dispatchers.IO) {
        val source = items.filter { !it.isVideo && it.dateTaken > 0L }.take(160)
        val signatures = ArrayList<GalleryPhotoSignature>(source.size)
        for (item in source) {
            coroutineContext.ensureActive()
            val bitmap = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(item.uri, Size(96, 96), null)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Thumbnails.getThumbnail(
                        context.contentResolver,
                        item.id,
                        MediaStore.Images.Thumbnails.MINI_KIND,
                        null,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
            if (bitmap == null) continue
            try {
                signatures += GalleryPhotoSignature(
                    id = item.id,
                    timestampMs = item.dateTaken,
                    differenceHash = differenceHash(bitmap),
                )
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
        GalleryPhotoScanSummary(
            analyzed = signatures.size,
            eligible = source.size,
            candidateGroups = groupSimilarGalleryPhotos(signatures),
        )
    }

    private fun differenceHash(bitmap: Bitmap): Long {
        val tiny = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        return try {
            val pixels = IntArray(9 * 8)
            tiny.getPixels(pixels, 0, 9, 0, 0, 9, 8)
            fun brightness(color: Int): Int =
                Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114
            var hash = 0L
            for (row in 0 until 8) {
                for (col in 0 until 8) {
                    hash = hash shl 1
                    if (brightness(pixels[row * 9 + col]) >
                        brightness(pixels[row * 9 + col + 1])
                    ) hash = hash or 1L
                }
            }
            hash
        } finally {
            if (tiny !== bitmap && !tiny.isRecycled) tiny.recycle()
        }
    }
}
