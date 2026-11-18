package com.riccardopinato.omnicleaner.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import com.riccardopinato.omnicleaner.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.max
import java.util.Locale

private sealed interface GalleryPreviewLoad {
    data object Loading : GalleryPreviewLoad
    data class Ready(val bitmap: Bitmap) : GalleryPreviewLoad
    data object Unavailable : GalleryPreviewLoad
}

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

    // New media must never display the previous item's bitmap while the decoder is busy.
    val previewState = remember(item.id, item.uri) {
        mutableStateOf<GalleryPreviewLoad>(GalleryPreviewLoad.Loading)
    }
    LaunchedEffect(item.id, item.uri, context) {
        val decoded = withContext(Dispatchers.IO) {
            loadPreviewBitmap(context, item)
        }
        previewState.value = decoded?.let(GalleryPreviewLoad::Ready)
            ?: GalleryPreviewLoad.Unavailable
    }
    val previewLoad = previewState.value
    val bitmap = (previewLoad as? GalleryPreviewLoad.Ready)?.bitmap

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
                    .heightIn(max = 340.dp)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (previewLoad == GalleryPreviewLoad.Loading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(30.dp))
                        Text(stringResource(R.string.gallery_preview_loading))
                    }
                } else {
                    Text(stringResource(R.string.gallery_preview_unavailable))
                }
            }
        }

        // Explicit action distinct from destructive swipe/Trash. Supports
        // accessibility users and never requires a hidden gesture.
        if (previewLoad != GalleryPreviewLoad.Loading) {
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
            color = Color.Black,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = item.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    ) {
                        Text(stringResource(R.string.action_close))
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.isVideo) {
                        GalleryVideoPlayer(item)
                    } else {
                        GalleryFullPhoto(item, previewBitmap)
                    }
                }
                Text(
                    text = stringResource(R.string.gallery_close_to_review),
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun GalleryFullPhoto(item: GalleryMediaItem, previewBitmap: Bitmap?) {
    val context = LocalContext.current
    val original by produceState<Bitmap?>(initialValue = previewBitmap, item.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching { decodeSampledBitmap(context, item, FULLSCREEN_SIZE_PX) }.getOrNull()
        } ?: previewBitmap
    }
    if (original == null) {
        Text(stringResource(R.string.gallery_preview_unavailable), color = Color.White)
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
    var prepared by remember(item.uri) { mutableStateOf(false) }
    var playing by remember(item.uri) { mutableStateOf(false) }
    var durationMs by remember(item.uri) { mutableIntStateOf(0) }
    var positionMs by remember(item.uri) { mutableIntStateOf(0) }
    var scrubbing by remember(item.uri) { mutableStateOf(false) }
    var scrubFraction by remember(item.uri) { mutableFloatStateOf(0f) }

    val view = remember(context, item.uri) {
        VideoView(context).apply {
            setOnPreparedListener { player ->
                player.isLooping = false
                durationMs = player.duration.coerceAtLeast(0)
                prepared = true
                start()
                playing = true
            }
            setOnCompletionListener {
                positionMs = durationMs
                playing = false
            }
            setOnErrorListener { _, _, _ ->
                failed = true
                playing = false
                true
            }
            setVideoURI(item.uri)
        }
    }
    DisposableEffect(view) {
        onDispose {
            // Closing the inspection dialog must release decoding/audio immediately.
            view.stopPlayback()
        }
    }
    LaunchedEffect(view, prepared) {
        if (prepared) {
            while (isActive) {
                if (!scrubbing) {
                    positionMs = view.currentPosition.coerceIn(0, durationMs.coerceAtLeast(0))
                }
                playing = view.isPlaying
                delay(250)
            }
        }
    }

    if (failed) {
        Text(
            text = stringResource(R.string.gallery_video_playback_error),
            color = Color.White,
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { view },
                    modifier = Modifier.fillMaxSize(),
                )
                if (!prepared) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(30.dp))
                }
            }

            val seekLabel = stringResource(R.string.gallery_player_seek)
            Slider(
                value = if (scrubbing) scrubFraction else galleryPlaybackFraction(positionMs, durationMs),
                onValueChange = { fraction ->
                    scrubbing = true
                    scrubFraction = fraction
                },
                onValueChangeFinished = {
                    val target = gallerySeekPositionMs(scrubFraction, durationMs)
                    view.seekTo(target)
                    positionMs = target
                    scrubbing = false
                },
                enabled = prepared && durationMs > 0,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = seekLabel },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        val target = gallerySkipPositionMs(positionMs, -10_000, durationMs)
                        view.seekTo(target)
                        positionMs = target
                    },
                    enabled = prepared,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.omni_media_back),
                        contentDescription = stringResource(R.string.gallery_player_back),
                        tint = Color.White,
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (view.isPlaying) {
                            view.pause()
                            playing = false
                        } else {
                            if (durationMs > 0 && positionMs >= durationMs - 200) {
                                view.seekTo(0)
                                positionMs = 0
                            }
                            view.start()
                            playing = true
                        }
                    },
                    enabled = prepared,
                ) {
                    Text(
                        stringResource(
                            if (playing) R.string.gallery_player_pause else R.string.gallery_player_play,
                        ),
                        color = Color.White,
                    )
                }
                IconButton(
                    onClick = {
                        val target = gallerySkipPositionMs(positionMs, 10_000, durationMs)
                        view.seekTo(target)
                        positionMs = target
                    },
                    enabled = prepared,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.omni_media_forward),
                        contentDescription = stringResource(R.string.gallery_player_forward),
                        tint = Color.White,
                    )
                }
                Text(
                    galleryTimeLabel(positionMs) + " / " + galleryTimeLabel(durationMs),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Pure playback UI math: invalid metadata never generates impossible seek targets. */
internal fun galleryPlaybackFraction(positionMs: Int, durationMs: Int): Float =
    if (durationMs > 0) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }

internal fun gallerySeekPositionMs(fraction: Float, durationMs: Int): Int =
    if (durationMs > 0 && fraction.isFinite()) {
        (fraction.coerceIn(0f, 1f) * durationMs).toInt().coerceIn(0, durationMs)
    } else {
        0
    }

internal fun gallerySkipPositionMs(positionMs: Int, deltaMs: Int, durationMs: Int): Int =
    if (durationMs > 0) {
        (positionMs.toLong() + deltaMs.toLong()).coerceIn(0L, durationMs.toLong()).toInt()
    } else {
        0
    }

internal fun galleryTimeLabel(ms: Int): String {
    val seconds = (ms.coerceAtLeast(0) / 1000L)
    val hours = seconds / 3600L
    val minutes = (seconds % 3600L) / 60L
    val remainder = seconds % 60L
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remainder)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, remainder)
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
): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(item.uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }
    val largest = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
    var sample = 1
    while (largest / sample > maxSize * 2) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = context.contentResolver.openInputStream(item.uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: return@runCatching null
    decoded.scaledDown(maxSize).applyExifOrientation(context, item)
}.getOrNull()

/** Respect camera EXIF rotation/mirroring when BitmapFactory reads raw pixels. */
private fun Bitmap.applyExifOrientation(
    context: Context,
    item: GalleryMediaItem,
): Bitmap {
    val exif = runCatching {
        context.contentResolver.openInputStream(item.uri)?.use { ExifInterface(it) }
    }.getOrNull() ?: return this
    val degrees = exif.rotationDegrees
    val flipped = exif.isFlipped
    if (degrees == 0 && !flipped) return this
    val transform = Matrix().apply {
        if (flipped) postScale(-1f, 1f)
        if (degrees != 0) postRotate(degrees.toFloat())
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, transform, true)
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
