package com.riccardopinato.omnicleaner.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import com.riccardopinato.omnicleaner.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

// WML-001 Watermelon Carousel adapted for read-only loaded MediaStore content.
// The carousel does not know or accept any Keep, Trash, swipe-decision,
// MediaStore write or storage mutation callback.
private const val MAX_CAROUSEL_ITEMS = 12
internal fun carouselWindow(items: List<GalleryMediaItem>): List<GalleryMediaItem> =
    items.take(MAX_CAROUSEL_ITEMS)

internal fun boundedCarouselIndex(index: Int, length: Int): Int =
    index.coerceIn(0, (length - 1).coerceAtLeast(0))

@Composable
internal fun OmniReadOnlyMediaCarousel(
    loadedItems: List<GalleryMediaItem>,
    modifier: Modifier = Modifier,
) {
    val media = carouselWindow(loadedItems)
    if (media.isEmpty()) return
    // Current-page change resets selection; no stale media URI/permission is
    // retained when the loaded MediaStore scope changes.
    val identity = media.map { it.id to it.uri }
    var index by remember(identity) { mutableIntStateOf(0) }
    var openPreview by remember(identity) { mutableStateOf(false) }
    val current = media[boundedCarouselIndex(index, media.size)]
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(
        initialValue = null, key1 = current.id, key2 = current.uri,
    ) {
        value = withContext(Dispatchers.IO) { loadPreviewBitmap(context, current) }
    }
    val colors = MaterialTheme.colorScheme
    val previous = stringResource(R.string.gallery_carousel_previous)
    val next = stringResource(R.string.gallery_carousel_next)
    val open = stringResource(R.string.gallery_carousel_open)

    Column(
        modifier = modifier.fillMaxWidth().testTag("omni_gallery_readonly_carousel"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.gallery_carousel_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${index + 1} / ${media.size}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        // A distinct container, never nested in the Tinder review surface.
        // Horizontal drags here ONLY change the previewed carousel index.
        var dragDistance by remember(current.id) { androidx.compose.runtime.mutableFloatStateOf(0f) }
        Box(
            modifier = Modifier.fillMaxWidth().height(185.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, colors.outline.copy(alpha = .6f)), RoundedCornerShape(10.dp))
                .background(colors.surfaceVariant.copy(alpha = .35f))
                .pointerInput(identity) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onDragCancel = { dragDistance = 0f },
                        onDragEnd = {
                            if (abs(dragDistance) >= 70.dp.toPx()) {
                                index = boundedCarouselIndex(
                                    index + if (dragDistance < 0f) 1 else -1,
                                    media.size,
                                )
                            }
                            dragDistance = 0f
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragDistance += amount
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = current.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                )
            } else {
                Text(
                    stringResource(R.string.gallery_preview_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.TextButton(
                onClick = { index = boundedCarouselIndex(index - 1, media.size) },
                enabled = index > 0,
                modifier = Modifier.testTag("omni_gallery_carousel_previous")
                    .semantics { contentDescription = previous },
            ) { Text("‹", style = MaterialTheme.typography.titleLarge) }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    current.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    current.formattedSize,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            androidx.compose.material3.TextButton(
                onClick = { index = boundedCarouselIndex(index + 1, media.size) },
                enabled = index < media.lastIndex,
                modifier = Modifier.testTag("omni_gallery_carousel_next")
                    .semantics { contentDescription = next },
            ) { Text("›", style = MaterialTheme.typography.titleLarge) }
        }
        androidx.compose.material3.OutlinedButton(
            onClick = { openPreview = true },
            modifier = Modifier.fillMaxWidth().testTag("omni_gallery_carousel_open"),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(open)
        }
    }

    if (openPreview) {
        // Read-only preview. No Keep/Trash action exists inside this dialog.
        Dialog(
            onDismissRequest = { openPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .background(Color(0xFF101827))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .testTag("omni_gallery_carousel_fullscreen"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        current.name,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                        overflow = TextOverflow.Ellipsis,
                    )
                    androidx.compose.material3.TextButton(
                        onClick = { openPreview = false },
                        modifier = Modifier.testTag("omni_gallery_carousel_close"),
                    ) {
                        Text(stringResource(R.string.action_close), color = Color.White)
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (current.isVideo) {
                        GalleryVideoPlayer(item = current, thumbnail = thumbnail)
                    } else if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail!!.asImageBitmap(),
                            contentDescription = current.name,
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
            }
        }
    }
}
