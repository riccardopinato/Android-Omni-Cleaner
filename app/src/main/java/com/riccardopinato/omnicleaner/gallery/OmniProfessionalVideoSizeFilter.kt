package com.riccardopinato.omnicleaner.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import kotlin.math.roundToLong

/**
 * WML-004 / Design-App: professional, restrained native Compose port of
 * Watermelon's two-thumb Range Selection Slider.
 *
 * Visual acceptance: plain surface, hairline divider, textual current bounds,
 * one blue slider accent, neutral Apply button, no gradients/glass/rolling
 * numeral animation. RangeSlider supplies native touch, keyboard and
 * accessibility-adjustable thumb semantics.
 *
 * The parent passes only currently loaded video metadata, and applies this
 * read-only range to Gallery Pro results only after tapping Apply.
 */
@Composable
internal fun OmniProfessionalVideoSizeFilter(
    maximumMiB: Float,
    draftMiB: ClosedFloatingPointRange<Float>,
    hasAppliedRange: Boolean,
    onDraftChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
) {
    val surface = MaterialTheme.colorScheme
    val minimumText = displayVideoSizeBound(draftMiB.start)
    val maximumText = displayVideoSizeBound(draftMiB.endInclusive) +
        if (draftMiB.endInclusive >= maximumMiB) "+" else ""
    val canReset = hasAppliedRange || draftMiB.start > 0f ||
        draftMiB.endInclusive < maximumMiB

    Column(
        modifier = Modifier.fillMaxWidth()
            .testTag("omni_gallery_pro_size_filter"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(
            color = surface.outline.copy(alpha = .6f),
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
        )

        Text(
            text = stringResource(R.string.gp_size_title),
            style = MaterialTheme.typography.titleMedium,
            color = surface.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.gp_size_description),
            style = MaterialTheme.typography.bodySmall,
            color = surface.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.gp_size_min),
                    style = MaterialTheme.typography.bodySmall,
                    color = surface.onSurfaceVariant,
                )
                Text(
                    text = minimumText,
                    style = MaterialTheme.typography.titleLarge,
                    color = surface.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.gp_size_max),
                    style = MaterialTheme.typography.bodySmall,
                    color = surface.onSurfaceVariant,
                )
                Text(
                    text = maximumText,
                    style = MaterialTheme.typography.titleLarge,
                    color = surface.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        RangeSlider(
            value = draftMiB,
            onValueChange = onDraftChange,
            valueRange = 0f..maximumMiB,
            modifier = Modifier.fillMaxWidth().testTag("omni_gallery_pro_size_range"),
            colors = SliderDefaults.colors(
                thumbColor = surface.primary,
                activeTrackColor = surface.primary,
                inactiveTrackColor = surface.outline,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = displayVideoSizeBound(0f),
                style = MaterialTheme.typography.bodySmall,
                color = surface.onSurfaceVariant,
            )
            Text(
                text = displayVideoSizeBound(maximumMiB) + "+",
                style = MaterialTheme.typography.bodySmall,
                color = surface.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canReset) {
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("omni_gallery_pro_size_reset"),
                ) {
                    Text(stringResource(R.string.gp_size_reset))
                }
            } else {
                Text(
                    text = stringResource(R.string.gp_size_inactive),
                    style = MaterialTheme.typography.bodySmall,
                    color = surface.onSurfaceVariant,
                )
            }
            Button(
                onClick = onApply,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = surface.onSurface,
                    contentColor = surface.surface,
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
                modifier = Modifier.testTag("omni_gallery_pro_size_apply"),
            ) {
                Text(stringResource(R.string.gp_size_apply))
            }
        }
        if (hasAppliedRange) {
            Text(
                text = stringResource(R.string.gp_size_active),
                style = MaterialTheme.typography.bodySmall,
                color = surface.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            color = surface.outline.copy(alpha = .6f),
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
        )
    }
}

/** Labels are rounded to the same 1 MiB precision as the actual filtering. */
private fun displayVideoSizeBound(mebibytes: Float): String {
    val rounded = mebibytes.roundToLong().coerceAtLeast(0L)
    return if (rounded == 0L) "0 MB" else formatMediaSize(rounded * VIDEO_SIZE_MIB)
}
