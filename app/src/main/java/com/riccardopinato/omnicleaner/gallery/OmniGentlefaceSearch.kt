package com.riccardopinato.omnicleaner.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R

/**
 * First-party native Compose glyph adapted from archived Gentleface ICO-011.
 * Original 16x16 filled "search", source and MIT provenance:
 * Design-App/BIBLIOTECA_COMPONENTI/sorgenti/gentleface/ICO-011.svg.
 * No font or runtime dependency. Decorative field prefix; text label names input.
 */
@Composable
internal fun OmniGentlefaceSearchGlyph(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Canvas(modifier = modifier.size(20.dp).clearAndSetSemantics { }) {
        val scale = size.width / 16f
        val center = Offset(6.5f * scale, 6.5f * scale)
        // SVG has a near-circular 1px ring from x=1..12,y=1..12.
        drawCircle(
            color = color,
            radius = 5f * scale,
            center = center,
            style = Stroke(width = 1f * scale),
        )
        drawLine(
            color = color,
            start = Offset(10.02f * scale, 10.727f * scale),
            end = Offset(14.5f * scale, 14.5f * scale),
            strokeWidth = 1f * scale,
            cap = StrokeCap.Round,
        )
    }
}

/** Accessible, correctly sized affordance: resets the real text filter. */
@Composable
internal fun OmniClearVideoSearchButton(onClick: () -> Unit) {
    val clearLabel = stringResource(R.string.gp_video_clear_search)
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .testTag("omni_gallery_pro_video_search_clear")
            .semantics { contentDescription = clearLabel },
    ) {
        Canvas(Modifier.size(16.dp).clearAndSetSemantics { }) {
            drawLine(
                color, Offset(size.width * .20f, size.height * .20f),
                Offset(size.width * .80f, size.height * .80f),
                strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round,
            )
            drawLine(
                color, Offset(size.width * .80f, size.height * .20f),
                Offset(size.width * .20f, size.height * .80f),
                strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round,
            )
        }
    }
}
