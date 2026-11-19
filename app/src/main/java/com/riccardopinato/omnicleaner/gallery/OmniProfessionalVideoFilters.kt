package com.riccardopinato.omnicleaner.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import java.util.Locale

/**
 * DSK-011: a quiet, product-owned single-select filter row.
 *
 * This replaces the four stock FilterChips visually, not functionally.
 * The parent still owns the original GalleryVideoReviewFilter and its
 * existing combination with search and WML-004 file-size range.
 */
@Composable
internal fun OmniProfessionalVideoFilters(
    selected: GalleryVideoReviewFilter,
    onSelect: (GalleryVideoReviewFilter) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier.fillMaxWidth()
            .testTag("omni_gallery_video_quick_filters"),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GalleryVideoReviewFilter.entries.forEach { option ->
            val isSelected = option == selected
            val title = when (option) {
                GalleryVideoReviewFilter.ALL -> R.string.gp_video_all
                GalleryVideoReviewFilter.LARGE_100_MB -> R.string.gp_video_large
                GalleryVideoReviewFilter.LONG_5_MIN -> R.string.gp_video_long
                GalleryVideoReviewFilter.UHD_4K -> R.string.gp_video_4k
            }
            Box(
                modifier = Modifier.weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isSelected) colors.onSurface
                            else colors.outline.copy(alpha = .58f),
                        ),
                        shape,
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .testTag(
                        "omni_gallery_video_filter_${option.name.lowercase(Locale.ROOT)}",
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(title),
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold
                        else FontWeight.Normal,
                    color = if (isSelected) colors.onSurface
                        else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
        }
    }
}
