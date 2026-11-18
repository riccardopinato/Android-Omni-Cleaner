package com.riccardopinato.omnicleaner.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Source images are loaded by the existing original-aspect Gallery preview
 * decoder, capped at 2160px to bound memory. No original file is modified.
 * Selecting the next pair drops references to prior preview bitmaps.
 */
@Composable
internal fun GalleryPhotoComparison(
    photos: List<GalleryMediaItem>,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var pairIndex by remember(photos.map { it.id }) { mutableStateOf(0) }
    val left = photos.getOrNull(pairIndex)
    val right = photos.getOrNull(pairIndex + 1)
    var loaded by remember { mutableStateOf<Pair<Bitmap?, Bitmap?>?>(null) }

    LaunchedEffect(left?.id, right?.id) {
        loaded = null
        if (left != null && right != null) {
            loaded = withContext(Dispatchers.IO) {
                loadPreviewBitmap(context, left) to loadPreviewBitmap(context, right)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("omni_gallery_compare"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(stringResource(R.string.gp_compare_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.gp_compare_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (left != null && right != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(left, right).forEachIndexed { index, item ->
                        Column(
                            modifier = Modifier.weight(1f).testTag("omni_gallery_compare_${index+1}"),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(230.dp)) {
                                val bitmap = if (index == 0) loaded?.first else loaded?.second
                                if (bitmap != null && !bitmap.isRecycled) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxWidth().height(230.dp),
                                    )
                                } else {
                                    Text(
                                        stringResource(R.string.gp_compare_loading),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(
                                    R.string.gp_compare_meta,
                                    item.width, item.height,
                                    formatMediaSize(item.sizeBytes),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (pairIndex + 2 < photos.size) {
                    OmniActionButton(
                        onClick = { pairIndex += 1 },
                        modifier = Modifier.fillMaxWidth().testTag("omni_gallery_compare_next"),
                    ) { Text(stringResource(R.string.gp_compare_next)) }
                }
                if (pairIndex > 0) {
                    OmniActionButton(
                        onClick = { pairIndex -= 1 },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.gp_compare_previous)) }
                }
            }
            OmniActionButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_close))
            }
        }
    }
}
