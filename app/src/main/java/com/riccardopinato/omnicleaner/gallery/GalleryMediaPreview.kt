package com.riccardopinato.omnicleaner.gallery

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.riccardopinato.omnicleaner.ui.theme.OmniSecondaryButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.riccardopinato.omnicleaner.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

private val TrashRed = Color(0xFFB42335)
private val KeepGreen = Color(0xFF137847)

// The selection commits just after the faster card-exit cue completes.
internal const val GALLERY_EXIT_ANIMATION_MS = 155
internal const val GALLERY_SELECTION_COMMIT_DELAY_MS = 170L

@Composable
fun GalleryMediaPreview(
    item: GalleryMediaItem,
    canUndo: Boolean,
    onKeep: () -> Unit,
    onTrash: () -> Unit,
    onUndo: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 110.dp.toPx() } }
    val exitDistancePx = remember(density) { with(density) { 240.dp.toPx() } }
    val configuration = LocalConfiguration.current
    val previewMaxHeightDp = (configuration.screenHeightDp * 0.62f).coerceIn(320f, 680f)
    val widthDp = configuration.screenWidthDp.toFloat().coerceAtLeast(300f) - 32f
    var dragX by remember(item.id) { mutableFloatStateOf(0f) }
    var releaseX by remember(item.id) { mutableFloatStateOf(0f) }
    var pendingDecision by remember(item.id) { mutableStateOf<SwipeDirection?>(null) }
    var showImmersivePreview by remember(item.id) { mutableStateOf(false) }

    // One decision at a time. The image moves to the same side as the chosen button.
    val exitTarget = galleryDecisionExitSign(pendingDecision).toFloat() * exitDistancePx
    val animatedExitX by animateFloatAsState(
        targetValue = exitTarget,
        animationSpec = tween(durationMillis = GALLERY_EXIT_ANIMATION_MS, easing = FastOutSlowInEasing),
        label = "Gallery photo choice motion",
    )

    fun select(direction: SwipeDirection, fromSwipe: Boolean = false) {
        if (pendingDecision != null) return
        releaseX = if (fromSwipe) dragX else 0f
        dragX = 0f
        pendingDecision = direction
    }

    // Commit the UI choice after the short exit cue so it remains visible.
    // This never requests Android Trash directly: trashCurrent only selects the media.
    LaunchedEffect(item.id, pendingDecision) {
        when (pendingDecision) {
            SwipeDirection.TRASH -> {
                delay(GALLERY_SELECTION_COMMIT_DELAY_MS)
                onTrash()
            }
            SwipeDirection.KEEP -> {
                delay(GALLERY_SELECTION_COMMIT_DELAY_MS)
                onKeep()
            }
            null -> Unit
        }
    }

    val bitmap by produceState<Bitmap?>(initialValue = null, item.uri, item.id) {
        value = withContext(Dispatchers.IO) { loadPreviewBitmap(context, item) }
    }
    // A provider thumbnail may be square even for 16:9 video. Prefer the
    // original MediaStore video dimensions; photos use the decoded bitmap.
    val mediaWidth = if (item.isVideo && item.width > 0) item.width
        else bitmap?.width ?: item.width
    val mediaHeight = if (item.isVideo && item.height > 0) item.height
        else bitmap?.height ?: item.height
    val previewHeightDp = galleryFitHeightDp(
        availableWidthDp = widthDp,
        maxHeightDp = previewMaxHeightDp,
        imageWidth = mediaWidth,
        imageHeight = mediaHeight,
    )

        val trashLabel = stringResource(R.string.action_trash)
        val undoLabel = stringResource(R.string.action_undo)
        val keepLabel = stringResource(R.string.action_keep)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeightDp.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF101827))
                .testTag("omni_gallery_media_full"),
            contentAlignment = Alignment.Center,
        ) {
            val currentBitmap = bitmap
            if (item.isVideo) {
                // Dispose the embedded VideoView while the immersive player owns this URI.
                // This prevents two simultaneous playback/audio sessions.
                if (!showImmersivePreview) {
                    GalleryVideoPlayer(item = item, thumbnail = currentBitmap)
                } else if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val offset = if (pendingDecision == null) dragX
                                else releaseX + animatedExitX
                            translationX = offset
                            rotationZ = (offset / 55f).coerceIn(-7f, 7f)
                            alpha = if (pendingDecision == null) 1f
                                else (1f - (abs(animatedExitX) / exitDistancePx) * 0.72f)
                                    .coerceIn(0.28f, 1f)
                        }
                        .pointerInput(item.id, pendingDecision) {
                            if (pendingDecision == null) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        val chosen = gallerySwipeDirection(dragX, thresholdPx)
                                        if (chosen != null) select(chosen, fromSwipe = true)
                                        else dragX = 0f
                                    },
                                    onDragCancel = { dragX = 0f },
                                    onHorizontalDrag = { change, amount ->
                                        change.consume()
                                        dragX += amount
                                    },
                                )
                            }
                        },
                )
            } else {
                Box(
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.gallery_preview_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Directional feedback is rendered in a separate top-level composable
            // to avoid scope collisions between ColumnScope and BoxScope animations.
            GalleryEdgeFeedback(
                direction = SwipeDirection.TRASH,
                visible = pendingDecision == SwipeDirection.TRASH,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            GalleryEdgeFeedback(
                direction = SwipeDirection.KEEP,
                visible = pendingDecision == SwipeDirection.KEEP,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        // The action buttons now sit OUTSIDE the original-aspect media frame,
        // so neither the video controls nor the edge of portrait photos is covered.
        // Do not scroll or reorder these controls: spatial mapping must match swipes.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { select(SwipeDirection.TRASH) },
                enabled = pendingDecision == null,
                modifier = Modifier.weight(1f).heightIn(min = 64.dp)
                    .semantics { contentDescription = trashLabel },
                shape = RoundedCornerShape(17.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                border = BorderStroke(2.dp, TrashRed),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrashRed,
                    contentColor = Color.White,
                ),
            ) {
                GalleryDecisionButtonLabel("✕", trashLabel)
            }
            OutlinedButton(
                onClick = onUndo,
                enabled = canUndo && pendingDecision == null,
                modifier = Modifier.weight(1f).heightIn(min = 64.dp)
                    .semantics { contentDescription = undoLabel },
                shape = RoundedCornerShape(17.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .94f),
                ),
            ) {
                GalleryDecisionButtonLabel("↶", undoLabel)
            }
            Button(
                onClick = { select(SwipeDirection.KEEP) },
                enabled = pendingDecision == null,
                modifier = Modifier.weight(1f).heightIn(min = 64.dp)
                    .semantics { contentDescription = keepLabel },
                shape = RoundedCornerShape(17.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                border = BorderStroke(2.dp, KeepGreen),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KeepGreen,
                    contentColor = Color.White,
                ),
            ) {
                GalleryDecisionButtonLabel("✓", keepLabel)
            }
        }
        OutlinedButton(
            onClick = { showImmersivePreview = true },
            enabled = pendingDecision == null,
            modifier = Modifier.fillMaxWidth().testTag("omni_gallery_immersive_open"),
        ) {
            Text(stringResource(R.string.omni_gallery_immersive_open))
        }
        Text(
            text = stringResource(R.string.gallery_swipe_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.gallery_selection_explainer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (showImmersivePreview) {
        val immersiveBitmap = bitmap // snapshot of delegated Compose state; safe smart cast
        // Full screen is inspection-only: it never changes the current selection
        // or invokes trash/keep. Original aspect ratio is preserved for both media types.
        Dialog(
            onDismissRequest = { showImmersivePreview = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF101827))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .testTag("omni_gallery_immersive")
                    .semantics { testTagsAsResourceId = true },
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        item.name,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    OutlinedButton(
                        onClick = { showImmersivePreview = false },
                        modifier = Modifier.testTag("omni_gallery_immersive_close"),
                    ) {
                        Text(stringResource(R.string.omni_gallery_immersive_close))
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .testTag("omni_gallery_immersive_content"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.isVideo) {
                        GalleryVideoPlayer(item = item, thumbnail = bitmap)
                    } else if (immersiveBitmap != null) {
                        Image(
                            bitmap = immersiveBitmap.asImageBitmap(),
                            contentDescription = item.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            stringResource(R.string.gallery_preview_unavailable),
                            color = Color.White,
                        )
                    }
                }
                Text(
                    stringResource(R.string.omni_gallery_immersive_hint),
                    color = Color(0xFFCDD8E8),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/**
 * Android's native media pipeline plays the original MediaStore URI.
 * No cropping, transcoding or write access; playback is opt-in and lifecycle-bound.
 * Video surface consumes its own taps (seek/play) instead of triggering photo swipes.
 */
@Composable
private fun GalleryVideoPlayer(item: GalleryMediaItem, thumbnail: Bitmap?) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var started by remember(item.uri) { mutableStateOf(false) }
    var error by remember(item.uri) { mutableStateOf(false) }
    val video = remember(context, item.uri) {
        VideoView(context).apply {
            val controls = MediaController(context)
            controls.setAnchorView(this)
            setMediaController(controls)
            setOnErrorListener { _, _, _ ->
                error = true
                true
            }
            runCatching { setVideoURI(item.uri) }.onFailure { error = true }
        }
    }
    DisposableEffect(lifecycle, video) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                video.pause()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            video.stopPlayback()
        }
    }
    LaunchedEffect(started) {
        if (started) {
            runCatching { video.start() }.onFailure { error = true }
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (started && !error) {
            AndroidView(
                factory = { video },
                modifier = Modifier.fillMaxSize(),
            )
        } else if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (!started && !error) {
            OmniActionButton(
                onClick = { started = true },
                modifier = Modifier.testTag("omni_gallery_video_play"),
            ) {
                Text(stringResource(R.string.gallery_video_play))
            }
        }
        if (error) {
            Text(
                text = stringResource(R.string.gallery_video_unavailable),
                color = Color.White,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun GalleryEdgeFeedback(
    direction: SwipeDirection,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val isTrash = direction == SwipeDirection.TRASH
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(120)) +
            slideInHorizontally(
                initialOffsetX = { if (isTrash) -it / 2 else it / 2 },
                animationSpec = tween(180),
            ),
        exit = fadeOut(tween(80)),
        label = if (isTrash) "Trash left feedback" else "Keep right feedback",
    ) {
        GalleryDecisionBadge(
            label = stringResource(if (isTrash) R.string.action_trash else R.string.action_keep),
            symbol = if (isTrash) "✕" else "✓",
            background = if (isTrash) TrashRed else KeepGreen,
        )
    }
}

@Composable
private fun GalleryDecisionButtonLabel(symbol: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (symbol == "✕") {
            GalleryTrashIcon()
        } else {
            Text(symbol, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text(
            label, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GalleryDecisionBadge(
    label: String,
    symbol: String,
    background: Color,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 9.dp)
            .background(background.copy(alpha = 0.94f), RoundedCornerShape(99.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (symbol == "✕") {
            GalleryTrashIcon()
        } else {
            Text(symbol, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            label, color = Color.White, fontSize = 13.sp,
            fontWeight = FontWeight.Bold, maxLines = 1,
        )
    }
}


@Composable
private fun GalleryTrashIcon() {
    // Compact white wastebasket, consistent on devices without emoji fonts.
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val lineWidth = 2.dp.toPx()
        drawLine(Color.White, Offset(w * .12f, h * .25f), Offset(w * .88f, h * .25f), strokeWidth = lineWidth, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * .40f, h * .13f), Offset(w * .60f, h * .13f), strokeWidth = lineWidth, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * .23f, h * .37f), Offset(w * .30f, h * .92f), strokeWidth = lineWidth, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * .77f, h * .37f), Offset(w * .70f, h * .92f), strokeWidth = lineWidth, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * .30f, h * .92f), Offset(w * .70f, h * .92f), strokeWidth = lineWidth, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * .42f, h * .48f), Offset(w * .42f, h * .75f), strokeWidth = lineWidth * 0.75f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * .58f, h * .48f), Offset(w * .58f, h * .75f), strokeWidth = lineWidth * 0.75f, cap = StrokeCap.Round)
    }
}

internal fun galleryDecisionExitSign(direction: SwipeDirection?): Int = when (direction) {
    SwipeDirection.TRASH -> -1
    SwipeDirection.KEEP -> 1
    null -> 0
}

internal fun gallerySwipeDirection(
    deltaX: Float,
    thresholdPx: Float,
): SwipeDirection? = when {
    deltaX >= thresholdPx -> SwipeDirection.KEEP
    deltaX <= -thresholdPx -> SwipeDirection.TRASH
    else -> null
}

internal fun loadPreviewBitmap(
    context: Context,
    item: GalleryMediaItem,
): Bitmap? = runCatching {
    if (item.isVideo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return@runCatching context.contentResolver.loadThumbnail(
                item.uri,
                Size(VIDEO_THUMBNAIL_PX, VIDEO_THUMBNAIL_PX),
                null,
            )
        }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, item.uri)
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.scaledDown(VIDEO_THUMBNAIL_PX)
        } finally {
            retriever.release()
        }
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, item.uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val natural = info.size
                val largest = max(natural.width, natural.height).coerceAtLeast(1)
                if (largest > PREVIEW_SIZE_PX) {
                    val scale = PREVIEW_SIZE_PX.toFloat() / largest.toFloat()
                    decoder.setTargetSize(
                        (natural.width * scale).toInt().coerceAtLeast(1),
                        (natural.height * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        } else {
            val sampled = decodeSampledBitmap(context, item, PREVIEW_SIZE_PX)
            if (sampled == null) null else {
                val rotation = context.contentResolver.openInputStream(item.uri)?.use {
                    ExifInterface(it).rotationDegrees
                } ?: 0
                if (rotation == 0) sampled else {
                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                    val corrected = Bitmap.createBitmap(
                        sampled, 0, 0, sampled.width, sampled.height, matrix, true,
                    )
                    if (corrected !== sampled) sampled.recycle()
                    corrected
                }
            }
        }
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

/** Height of the *entire* original-ratio frame; no 1:1 crop.
 * Portrait footage uses the available vertical area, landscape keeps its ratio.
 */
internal fun galleryFitHeightDp(
    availableWidthDp: Float,
    maxHeightDp: Float,
    imageWidth: Int,
    imageHeight: Int,
): Float {
    if (imageWidth <= 0 || imageHeight <= 0) return maxHeightDp.coerceAtLeast(220f)
    val ideal = availableWidthDp * imageHeight.toFloat() / imageWidth.toFloat()
    return ideal.coerceIn(180f, maxHeightDp.coerceAtLeast(220f))
}

private const val PREVIEW_SIZE_PX = 2160
private const val VIDEO_THUMBNAIL_PX = 1080
