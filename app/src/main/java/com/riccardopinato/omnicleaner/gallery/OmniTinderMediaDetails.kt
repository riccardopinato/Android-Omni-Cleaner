package com.riccardopinato.omnicleaner.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R

/**
 * Informational only. The Tinder review stays in GalleryMediaPreview with its
 * original Keep/Trash callbacks, swipes and Android Trash consent workflow.
 * Only metadata already loaded from MediaStore is displayed.
 */
@Composable
internal fun OmniTinderMediaDetails(media: GalleryMediaItem) {
    val colors = MaterialTheme.colorScheme
    val typeLabel = stringResource(
        if (media.isVideo) R.string.gallery_details_video
        else R.string.gallery_details_photo,
    )
    val format = galleryDisplayMimeSubtype(media.mimeType)
    val title = if (format.isEmpty()) typeLabel else "$typeLabel · $format"
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("omni_gallery_tinder_media_details"),
        color = colors.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = .55f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = media.name,
                color = colors.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetadataCell(
                    label = stringResource(R.string.gallery_details_size),
                    value = media.formattedSize,
                    modifier = Modifier.weight(1f),
                )
                if (media.dateTaken > 0L) {
                    MetadataCell(
                        label = stringResource(R.string.gallery_details_date),
                        value = media.formattedDate,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    androidx.compose.foundation.layout.Box(Modifier.weight(1f))
                }
            }
            val album = media.album?.takeIf { it.isNotBlank() }
            val resolution = galleryDisplayResolution(media.width, media.height)
            if (album != null || resolution != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (album != null) {
                        MetadataCell(
                            label = stringResource(R.string.gallery_details_album),
                            value = album,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f))
                    }
                    if (resolution != null) {
                        MetadataCell(
                            label = stringResource(R.string.gallery_details_resolution),
                            value = resolution,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f))
                    }
                }
            }
            if (media.isVideo && media.durationMs > 0L) {
                MetadataCell(
                    label = stringResource(R.string.gallery_details_duration),
                    value = media.formattedDuration,
                )
            }
        }
    }
}

@Composable
private fun MetadataCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Omit unavailable or malformed dimensions rather than inventing resolution. */
internal fun galleryDisplayResolution(width: Int, height: Int): String? =
    if (width > 0 && height > 0) "$width × $height" else null

/** Display only the MIME subtype, never the content URI or a guessed extension. */
internal fun galleryDisplayMimeSubtype(mime: String): String =
    mime.substringAfter('/', "").substringBefore(';').trim()
        .takeIf { mime.contains('/') && it.isNotBlank() }
        ?.uppercase(java.util.Locale.ROOT).orEmpty()
