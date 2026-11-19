package com.riccardopinato.omnicleaner.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.ui.theme.OmniTextButton

/**
 * Read-only, professional row for an item selected for Android system Trash.
 * No deletion or selection changes are performed here: removal explicitly
 * delegates to the existing GalleryController callback in the parent.
 */
@Composable
internal fun OmniTrashQueueItem(
    media: GalleryMediaItem,
    onRemove: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val kind = stringResource(if (media.isVideo) R.string.gallery_queue_video else R.string.gallery_queue_photo)
    val removeLabel = stringResource(R.string.audit_gallery_unmark)
    Row(
        modifier = Modifier.fillMaxWidth()
            .testTag("omni_gallery_queue_item_${media.id}")
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp)
                .background(colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            OmniTrashQueueMediaGlyph(
                isVideo = media.isVideo,
                tint = colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = media.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$kind · ${media.formattedSize}",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        OmniTextButton(
            onClick = onRemove,
            modifier = Modifier.testTag("omni_gallery_queue_remove_${media.id}")
                .semantics { contentDescription = "$removeLabel: ${media.name}" },
        ) {
            Text(
                text = "×",
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(color = colorScheme.outline.copy(alpha = .65f))
}

@Composable
private fun OmniTrashQueueMediaGlyph(
    isVideo: Boolean,
    tint: Color,
) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.width / 24f
        val lineWidth = 1.6.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(3f*s, 4f*s),
            size = androidx.compose.ui.geometry.Size(18f*s, 16f*s),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f*s),
            style = Stroke(width = lineWidth),
        )
        if (isVideo) {
            val triangle = androidx.compose.ui.graphics.Path().apply {
                moveTo(10f*s, 8f*s)
                lineTo(16f*s, 12f*s)
                lineTo(10f*s, 16f*s)
                close()
            }
            drawPath(triangle, tint)
        } else {
            drawCircle(tint, radius = 1.6f*s, center = Offset(9f*s, 9f*s))
            drawLine(
                tint, Offset(5.5f*s, 17f*s), Offset(10.5f*s, 12f*s),
                strokeWidth = lineWidth, cap = StrokeCap.Round,
            )
            drawLine(
                tint, Offset(10.5f*s, 12f*s), Offset(14f*s, 15f*s),
                strokeWidth = lineWidth, cap = StrokeCap.Round,
            )
            drawLine(
                tint, Offset(14f*s, 15f*s), Offset(17.5f*s, 11.5f*s),
                strokeWidth = lineWidth, cap = StrokeCap.Round,
            )
        }
    }
}
