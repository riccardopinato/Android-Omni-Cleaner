package com.riccardopinato.omnicleaner.gallery

import android.content.Context
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riccardopinato.omnicleaner.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

private val TrashRed = Color(0xFFB42335)
private val KeepGreen = Color(0xFF137847)

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
    var dragX by remember(item.id) { mutableFloatStateOf(0f) }
    var releaseX by remember(item.id) { mutableFloatStateOf(0f) }
    var pendingDecision by remember(item.id) { mutableStateOf<SwipeDirection?>(null) }

    // One decision at a time. The image moves to the same side as the chosen button.
    val exitTarget = galleryDecisionExitSign(pendingDecision).toFloat() * exitDistancePx
    val animatedExitX by animateFloatAsState(
        targetValue = exitTarget,
        animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing),
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
                delay(225)
                onTrash()
            }
            SwipeDirection.KEEP -> {
                delay(225)
                onKeep()
            }
            null -> Unit
        }
    }

    val bitmap by produceState<Bitmap?>(initialValue = null, item.uri, item.id) {
        value = withContext(Dispatchers.IO) { loadPreviewBitmap(context, item) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center,
        ) {
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
                    Modifier.fillMaxWidth().aspectRatio(1f)
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

        val trashLabel = stringResource(R.string.action_trash)
        val undoLabel = stringResource(R.string.action_undo)
        val keepLabel = stringResource(R.string.action_keep)

        // Do not scroll or reorder these controls: spatial mapping must match swipes.
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = KeepGreen,
                    contentColor = Color.White,
                ),
            ) {
                GalleryDecisionButtonLabel("✓", keepLabel)
            }
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
