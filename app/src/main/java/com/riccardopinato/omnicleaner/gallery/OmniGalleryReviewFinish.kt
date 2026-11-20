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
import com.riccardopinato.omnicleaner.ui.theme.OmniSecondaryButton

/** Read-only description of a finished *loaded page*, not proof that
 * the entire device gallery was scanned or anything was deleted.
 */
internal data class GalleryReviewFinishState(
    val reviewed: Int,
    val selectedForTrash: Int,
    val moreToLoad: Boolean,
)

internal fun galleryReviewFinishState(state: GalleryUiState): GalleryReviewFinishState? {
    if (state.loading || state.loadingNextPage ||
        state.currentItem != null || state.items.isEmpty()
    ) return null
    return GalleryReviewFinishState(
        reviewed = state.totalReviewedCount.coerceAtLeast(0),
        selectedForTrash = state.selectedForTrash.size,
        moreToLoad = state.hasMore,
    )
}

@Composable
internal fun OmniGalleryReviewFinish(
    finish: GalleryReviewFinishState,
    onReviewQueue: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().testTag("omni_gallery_review_finished"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(
                    if (finish.moreToLoad) R.string.gallery_finish_page_title
                    else R.string.gallery_finish_title,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(
                    if (finish.moreToLoad) R.string.gallery_finish_page_description
                    else R.string.gallery_finish_description,
                ),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FinishCount(
                    value = finish.reviewed,
                    label = stringResource(R.string.gallery_finish_reviewed),
                    modifier = Modifier.weight(1f),
                )
                FinishCount(
                    value = finish.selectedForTrash,
                    label = stringResource(R.string.gallery_finish_selected),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(R.string.gallery_finish_safety),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            if (finish.selectedForTrash > 0) {
                OmniSecondaryButton(
                    onClick = onReviewQueue,
                    modifier = Modifier.fillMaxWidth().testTag("omni_gallery_finish_review_queue"),
                ) {
                    Text(stringResource(R.string.gallery_finish_review_queue))
                }
            }
            if (finish.moreToLoad) {
                OmniSecondaryButton(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth().testTag("omni_gallery_finish_load_more"),
                ) {
                    Text(stringResource(R.string.gallery_load_more))
                }
            }
        }
    }
}

@Composable
private fun FinishCount(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
