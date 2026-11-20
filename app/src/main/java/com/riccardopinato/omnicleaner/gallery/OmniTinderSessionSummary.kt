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
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R

/**
 * Pure UI: no mutation, no Keep/Trash callbacks and no misleading global
 * count of kept files. The persisted review count is cumulative; the
 * history is bounded to 50 and is not a durable lifetime kept counter.
 */
internal data class GallerySessionSummary(
    val reviewedTotal: Int,
    val selectedForAndroidTrash: Int,
    val recentKept: Int?,
)

internal fun gallerySessionSummary(state: GalleryUiState): GallerySessionSummary =
    GallerySessionSummary(
        reviewedTotal = state.totalReviewedCount.coerceAtLeast(0),
        selectedForAndroidTrash = state.selectedForTrash.size,
        recentKept = state.history
            .takeIf { it.isNotEmpty() }
            ?.count { it.direction == SwipeDirection.KEEP },
    )

@Composable
internal fun OmniTinderSessionSummary(state: GalleryUiState) {
    val summary = gallerySessionSummary(state)
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().testTag("omni_gallery_tinder_session_summary"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SummaryMetric(
                value = summary.reviewedTotal,
                label = stringResource(R.string.gallery_session_reviewed),
                modifier = Modifier.weight(1f),
            )
            SummaryMetric(
                value = summary.selectedForAndroidTrash,
                label = stringResource(R.string.gallery_session_selected),
                modifier = Modifier.weight(1f),
            )
            if (summary.recentKept != null) {
                SummaryMetric(
                    value = summary.recentKept,
                    label = stringResource(R.string.gallery_session_recent_kept),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = stringResource(R.string.gallery_session_android_confirmation),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryMetric(value: Int, label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        color = colors.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = .6f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
