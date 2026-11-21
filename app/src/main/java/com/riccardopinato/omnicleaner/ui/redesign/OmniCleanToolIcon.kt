package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * Restrained, first-party monochrome tool glyphs for the professional Clean hub.
 * Decorative only; each card retains its localized visible label and click semantics.
 * No new vector font or icon dependency.
 */
@Composable
internal fun OmniCleanToolIcon(
    tool: CleanerToolRoute,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(23.dp).clearAndSetSemantics { }) {
        withTransform({ scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) }) {
            val stroke = Stroke(width = 1.75f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
                drawLine(tint, Offset(x1, y1), Offset(x2, y2), strokeWidth = 1.75f,
                    cap = StrokeCap.Round)
            }
            fun path(block: Path.() -> Unit) {
                drawPath(Path().apply(block), tint, style = stroke)
            }
            when (tool) {
                CleanerToolRoute.OVERVIEW -> {
                    drawRoundRect(tint, Offset(3f, 4f),
                        androidx.compose.ui.geometry.Size(18f, 16f),
                        androidx.compose.ui.geometry.CornerRadius(3f), style = stroke)
                    line(3f, 15f, 21f, 15f)
                    line(7f, 10f, 10f, 10f)
                    drawCircle(tint, radius = 1f, center = Offset(17f, 10f))
                }
                CleanerToolRoute.INTELLIGENCE -> {
                    drawCircle(tint, radius = 8f, center = Offset(12f, 12f), style = stroke)
                    path { moveTo(12f, 4f); lineTo(12f, 12f); lineTo(19f, 16f) }
                    line(12f, 12f, 6f, 16f)
                }
                CleanerToolRoute.REVIEW -> {
                    drawRoundRect(tint, Offset(5f, 3f),
                        androidx.compose.ui.geometry.Size(14f, 18f),
                        androidx.compose.ui.geometry.CornerRadius(2f), style = stroke)
                    line(9f, 3f, 9f, 5f)
                    path { moveTo(9f, 13f); lineTo(11f, 15f); lineTo(15.5f, 10f) }
                }
                CleanerToolRoute.LARGE -> {
                    path {
                        moveTo(6f, 3f); lineTo(14f, 3f); lineTo(19f, 8f)
                        lineTo(19f, 20f); quadraticTo(19f, 21f, 18f, 21f)
                        lineTo(6f, 21f); quadraticTo(5f, 21f, 5f, 20f)
                        lineTo(5f, 4f); quadraticTo(5f, 3f, 6f, 3f)
                    }
                    path { moveTo(14f, 3f); lineTo(14f, 8f); lineTo(19f, 8f) }
                    line(8f, 13f, 16f, 13f)
                    line(8f, 17f, 13f, 17f)
                }
                CleanerToolRoute.DUPLICATES -> {
                    drawRoundRect(tint, Offset(7f, 7f),
                        androidx.compose.ui.geometry.Size(13f, 13f),
                        androidx.compose.ui.geometry.CornerRadius(2f), style = stroke)
                    path {
                        moveTo(4f, 16f); lineTo(4f, 5f)
                        quadraticTo(4f, 4f, 5f, 4f)
                        lineTo(16f, 4f)
                    }
                    line(10f, 11f, 17f, 11f)
                    line(10f, 15f, 15f, 15f)
                }
                CleanerToolRoute.FOLDERS -> {
                    path {
                        moveTo(3f, 8f); lineTo(3f, 6f)
                        quadraticTo(3f, 5f, 4f, 5f)
                        lineTo(10f, 5f); lineTo(12f, 8f); lineTo(20f, 8f)
                        quadraticTo(21f, 8f, 21f, 9f)
                        lineTo(21f, 18f); quadraticTo(21f, 19f, 20f, 19f)
                        lineTo(4f, 19f); quadraticTo(3f, 19f, 3f, 18f)
                        close()
                    }
                }
                CleanerToolRoute.FILTERS -> {
                    line(4f, 6f, 20f, 6f)
                    line(4f, 12f, 20f, 12f)
                    line(4f, 18f, 20f, 18f)
                    drawCircle(Color.Transparent, 3f, Offset(9f, 6f), style = stroke)
                    drawCircle(Color.Transparent, 3f, Offset(16f, 12f), style = stroke)
                    drawCircle(Color.Transparent, 3f, Offset(11f, 18f), style = stroke)
                    drawCircle(tint, 2.3f, Offset(9f, 6f))
                    drawCircle(tint, 2.3f, Offset(16f, 12f))
                    drawCircle(tint, 2.3f, Offset(11f, 18f))
                }
                CleanerToolRoute.TRASH -> {
                    path {
                        moveTo(5f, 7f); lineTo(19f, 7f)
                        moveTo(9f, 7f); lineTo(9f, 5f)
                        quadraticTo(9f, 4f, 10f, 4f)
                        lineTo(14f, 4f); quadraticTo(15f, 4f, 15f, 5f)
                        lineTo(15f, 7f)
                        moveTo(7f, 9f); lineTo(8f, 19f)
                        quadraticTo(8f, 20f, 9f, 20f)
                        lineTo(15f, 20f); quadraticTo(16f, 20f, 16f, 19f)
                        lineTo(17f, 9f)
                    }
                    line(11f, 11f, 11f, 17f)
                    line(13f, 11f, 13f, 17f)
                }
                CleanerToolRoute.CATEGORIES -> {
                    // Four distinct bins: this is not the maintenance tool icon.
                    for (y in 0..1) for (x in 0..1) {
                        drawRoundRect(
                            tint,
                            Offset(4f + 9f * x, 4f + 9f * y),
                            androidx.compose.ui.geometry.Size(7f, 7f),
                            androidx.compose.ui.geometry.CornerRadius(1.5f),
                            style = stroke,
                        )
                    }
                }
                CleanerToolRoute.ADVANCED -> {
                    drawRoundRect(tint, Offset(4f, 4f),
                        androidx.compose.ui.geometry.Size(16f, 16f),
                        androidx.compose.ui.geometry.CornerRadius(3f), style = stroke)
                    line(8f, 8f, 16f, 8f)
                    line(8f, 12f, 16f, 12f)
                    line(8f, 16f, 16f, 16f)
                }
            }
        }
    }
}
