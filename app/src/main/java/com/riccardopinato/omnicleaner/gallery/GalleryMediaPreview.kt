package com.riccardopinato.omnicleaner.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun GalleryMediaPreview(
    item: GalleryMediaItem,
    onKeep: () -> Unit,
    onTrash: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 110.dp.toPx() } }
    var dragX by remember(item.id) { mutableFloatStateOf(0f) }

    val bitmap by produceState<Bitmap?>(initialValue = null, item.uri, item.id) {
        value = withContext(Dispatchers.IO) {
            loadPreviewBitmap(context, item)
        }
    }

    Column {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        translationX = dragX
                        rotationZ = (dragX / 60f).coerceIn(-5f, 5f)
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .pointerInput(item.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when (gallerySwipeDirection(dragX, thresholdPx)) {
                                    SwipeDirection.KEEP -> onKeep()
                                    SwipeDirection.TRASH -> onTrash()
                                    null -> Unit
                                }
                                dragX = 0f
                            },
                            onDragCancel = { dragX = 0f },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                dragX += amount
                            },
                        )
                    },
            )
        } else {
            Text(
                text = "Anteprima non disponibile",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 36.dp),
            )
        }

        Text(
            text = "← Cestino   ·   Tieni →",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

internal fun gallerySwipeDirection(
    deltaX: Float,
    thresholdPx: Float,
): SwipeDirection? = when {
    deltaX >= thresholdPx -> SwipeDirection.KEEP
    deltaX <= -thresholdPx -> SwipeDirection.TRASH
    else -> null
}

private fun loadPreviewBitmap(
    context: Context,
    item: GalleryMediaItem,
): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return@runCatching context.contentResolver.loadThumbnail(
            item.uri,
            Size(PREVIEW_SIZE_PX, PREVIEW_SIZE_PX),
            null,
        )
    }

    if (item.isVideo) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, item.uri)
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.scaledDown(PREVIEW_SIZE_PX)
        } finally {
            retriever.release()
        }
    } else {
        decodeSampledBitmap(context, item, PREVIEW_SIZE_PX)
    }
}.getOrNull()

private fun decodeSampledBitmap(
    context: Context,
    item: GalleryMediaItem,
    maxSize: Int,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(item.uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }

    val largest = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
    var sample = 1
    while (largest / sample > maxSize * 2) {
        sample *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
    }
    return context.contentResolver.openInputStream(item.uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }?.scaledDown(maxSize)
}

private fun Bitmap.scaledDown(maxSize: Int): Bitmap {
    val largest = max(width, height)
    if (largest <= maxSize || largest <= 0) return this

    val scale = maxSize.toFloat() / largest.toFloat()
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private const val PREVIEW_SIZE_PX = 1080
