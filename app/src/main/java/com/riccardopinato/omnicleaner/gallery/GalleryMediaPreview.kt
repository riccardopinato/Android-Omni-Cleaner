package com.riccardopinato.omnicleaner.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.riccardopinato.omnicleaner.R
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
    var showFullScreen by remember(item.id) { mutableStateOf(false) }

    val bitmap by produceState<Bitmap?>(initialValue = null, item.uri, item.id) {
        value = withContext(Dispatchers.IO) {
            loadPreviewBitmap(context, item)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = item.name,
                // Never crop photographs or video frames in the review surface.
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(previewFrameAspectRatio(currentBitmap.width, currentBitmap.height))
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
        } else if (!item.isVideo) {
            Text(
                text = stringResource(R.string.gallery_preview_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 36.dp),
            )
        }

        // Explicit action distinct from destructive swipe/Trash. Supports
        // accessibility users and never requires a hidden gesture.
        if (currentBitmap != null || item.isVideo) {
            OutlinedButton(
                onClick = { showFullScreen = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (item.isVideo) R.string.gallery_play_video
                        else R.string.gallery_open_photo,
                    ),
                )
            }
        }
        Text(
            text = stringResource(R.string.gallery_swipe_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }

    if (showFullScreen) {
        GalleryFullScreenDialog(
            item = item,
            previewBitmap = bitmap,
            onDismiss = { showFullScreen = false },
        )
    }
}

/** Layout bounds keep tall/panoramic media visible without cropping. */
internal fun previewFrameAspectRatio(width: Int, height: Int): Float =
    if (width > 0 && height > 0) {
        (width.toFloat() / height).coerceIn(0.5f, 2.0f)
    } else {
        1f
    }

@Composable
private fun GalleryFullScreenDialog(
    item: GalleryMediaItem,
    previewBitmap: Bitmap?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (item.isVideo) {
                        GalleryVideoPlayer(item)
                    } else {
                        GalleryFullPhoto(item, previewBitmap)
                    }
                }
                // Full-screen media inspection is non-destructive by design.
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_close))
                }
            }
        }
    }
}

@Composable
private fun GalleryFullPhoto(item: GalleryMediaItem, previewBitmap: Bitmap?) {
    val context = LocalContext.current
    val original by produceState<Bitmap?>(initialValue = previewBitmap, item.uri) {
        value = withContext(Dispatchers.IO) {
            decodeSampledBitmap(context, item, FULLSCREEN_SIZE_PX)
        } ?: previewBitmap
    }
    if (original == null) {
        Text(stringResource(R.string.gallery_preview_unavailable))
    } else {
        Image(
            bitmap = original!!.asImageBitmap(),
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun GalleryVideoPlayer(item: GalleryMediaItem) {
    val context = LocalContext.current
    var failed by remember(item.uri) { mutableStateOf(false) }
    val view = remember(item.uri) {
        VideoView(context).apply {
            val controls = MediaController(context)
            controls.setAnchorView(this)
            setMediaController(controls)
            setVideoURI(item.uri)
            setOnPreparedListener { player ->
                player.isLooping = false
                start()
            }
            setOnErrorListener { _, _, _ ->
                failed = true
                true
            }
        }
    }
    DisposableEffect(view) {
        onDispose { view.stopPlayback() }
    }
    if (failed) {
        Text(stringResource(R.string.gallery_video_playback_error))
    } else {
        AndroidView(
            factory = { view },
            modifier = Modifier.fillMaxSize(),
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
        // The provider thumbnail can crop. Decode the source bounds first
        // so the review image retains the original dimensions/proportions.
        decodeSampledBitmap(context, item, PREVIEW_SIZE_PX)
    }
}.getOrNull() ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    runCatching {
        context.contentResolver.loadThumbnail(
            item.uri,
            Size(PREVIEW_SIZE_PX, PREVIEW_SIZE_PX),
            null,
        )
    }.getOrNull()
} else {
    null
}

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
private const val FULLSCREEN_SIZE_PX = 2048
