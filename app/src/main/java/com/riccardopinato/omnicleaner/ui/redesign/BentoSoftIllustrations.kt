package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * Tiny, original native vector spot illustrations for Soft Pastel Cleaner.
 * No bitmap, fonts, network, fake data, third-party library or APK asset weight.
 * Pure decoration: accessibility reads the real card name, not these shapes.
 */
internal fun bentoHasSoftIllustration(id: String): Boolean = id in setOf(
    "storage", "duplicates", "large", "gallery", "privacy",
    "trash", "folders", "galleryReview", "galleryPro",
)

@Composable
internal fun BentoSoftIllustration(
    tileId: String,
    modifier: Modifier = Modifier,
) {
    if (!bentoHasSoftIllustration(tileId)) return
    Canvas(modifier.clearAndSetSemantics { }) {
        val x = size.width / 100f
        val y = size.height / 100f
        val stroke = (minOf(size.width, size.height) / 24f).coerceAtLeast(1f)
        val ink = Color(0xFF586B78).copy(alpha = .32f)
        val lightInk = Color(0xFF586B78).copy(alpha = .14f)
        val paper = Color.White.copy(alpha = .62f)

        fun card(left: Float, top: Float, right: Float, bottom: Float, radius: Float = 12f) {
            val pos = Offset(left * x, top * y)
            val bounds = Size((right - left) * x, (bottom - top) * y)
            val round = CornerRadius(radius * minOf(x, y))
            drawRoundRect(paper, pos, bounds, round)
            drawRoundRect(ink, pos, bounds, round, style = Stroke(stroke))
        }
        fun rule(ax: Float, ay: Float, bx: Float, by: Float, width: Float = 1f) {
            drawLine(ink, Offset(ax * x, ay * y), Offset(bx * x, by * y), strokeWidth = stroke * width)
        }
        when (tileId) {
            "storage" -> {
                // Three gently stacked disk/storage slabs.
                for (i in 0..2) {
                    val top = 16f + i * 23f
                    card(12f, top, 88f, top + 18f, 9f)
                    drawCircle(lightInk, 4f * x, Offset(23f * x, (top + 9f) * y))
                    rule(37f, top + 9f, 73f, top + 9f, .8f)
                }
            }
            "duplicates" -> {
                // Overlapping photo pages.
                card(13f, 11f, 72f, 73f, 9f)
                card(29f, 27f, 88f, 89f, 9f)
                drawCircle(lightInk, 9f * x, Offset(49f * x, 47f * y))
                rule(43f, 73f, 57f, 60f)
                rule(57f, 60f, 73f, 76f)
            }
            "large" -> {
                card(22f, 10f, 77f, 91f, 9f)
                rule(37f, 27f, 63f, 27f)
                rule(37f, 42f, 60f, 42f)
                drawCircle(lightInk, 18f * x, Offset(60f * x, 66f * y))
                rule(60f, 57f, 60f, 76f, 1.2f)
            }
            "gallery", "galleryReview", "galleryPro" -> {
                card(12f, 18f, 88f, 83f, 12f)
                drawCircle(lightInk, 10f * x, Offset(65f * x, 38f * y))
                rule(19f, 74f, 43f, 49f, 1.3f)
                rule(43f, 49f, 61f, 65f, 1.3f)
                rule(61f, 65f, 76f, 52f, 1.3f)
            }
            "privacy" -> {
                // Contained shield, not an alarm symbol.
                val path = Path().apply {
                    moveTo(50f * x, 9f * y)
                    lineTo(81f * x, 22f * y)
                    lineTo(78f * x, 59f * y)
                    quadraticTo(73f * x, 78f * y, 50f * x, 91f * y)
                    quadraticTo(27f * x, 78f * y, 22f * x, 59f * y)
                    lineTo(19f * x, 22f * y)
                    close()
                }
                drawPath(path, paper)
                drawPath(path, ink, style = Stroke(stroke * 1.3f))
                drawCircle(lightInk, 12f * x, Offset(50f * x, 48f * y))
                rule(50f, 47f, 50f, 65f, 1.3f)
            }
            "trash" -> {
                card(27f, 29f, 74f, 88f, 9f)
                rule(19f, 27f, 82f, 27f, 1.5f)
                rule(38f, 18f, 64f, 18f, 1.5f)
                rule(42f, 45f, 42f, 73f)
                rule(58f, 45f, 58f, 73f)
            }
            "folders" -> {
                drawRoundRect(paper, Offset(10f * x, 30f * y), Size(80f * x, 52f * y), CornerRadius(10f * x))
                drawRoundRect(ink, Offset(10f * x, 30f * y), Size(80f * x, 52f * y), CornerRadius(10f * x), style = Stroke(stroke))
                rule(18f, 30f, 18f, 19f)
                rule(18f, 19f, 46f, 19f)
                rule(46f, 19f, 54f, 30f)
            }
        }
    }
}
