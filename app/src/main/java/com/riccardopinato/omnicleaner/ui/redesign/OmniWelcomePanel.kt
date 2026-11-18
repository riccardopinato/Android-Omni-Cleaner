package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Decorative, vector-only onboarding. Never contains actions or fake scan results. */
enum class OmniWelcomeKind { CLEAN, GALLERY, PRIVACY, PRO }

@Composable
fun OmniWelcomePanel(
    kind: OmniWelcomeKind,
    title: String,
    message: String,
    highlights: List<String>,
    modifier: Modifier = Modifier,
) {
    val accent = when (kind) {
        OmniWelcomeKind.CLEAN -> Color(0xFF285DE5)
        OmniWelcomeKind.GALLERY -> Color(0xFF7656CB)
        OmniWelcomeKind.PRIVACY -> Color(0xFF168A9D)
        OmniWelcomeKind.PRO -> Color(0xFF9659BE)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .background(accent.copy(alpha = 0.11f), RoundedCornerShape(23.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(78.dp)
                            .clearAndSetSemantics { },
                    ) {
                        drawWelcomeArtwork(kind, accent)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                highlights.forEachIndexed { index, line ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(23.dp)
                                .background(accent.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                            )
                        }
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** Scalable canvas art with zero bitmap, font or network dependencies. */
private fun DrawScope.drawWelcomeArtwork(kind: OmniWelcomeKind, accent: Color) {
    val u = size.minDimension / 100f
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun tile(x: Float, y: Float, w: Float, h: Float, col: Color, radius: Float = 11f) {
        drawRoundRect(
            color = col,
            topLeft = p(x, y),
            size = Size(w * u, h * u),
            cornerRadius = CornerRadius(radius * u),
        )
    }
    val white = Color.White
    val light = accent.copy(alpha = 0.27f)
    when (kind) {
        OmniWelcomeKind.CLEAN -> {
            tile(18f, 10f, 64f, 80f, white)
            tile(37f, 7f, 26f, 10f, accent, 5f)
            for (i in 0..2) {
                val y = 31f + i * 19f
                drawCircle(color = accent.copy(alpha = 0.17f), radius = 7f * u, center = p(31f, y))
                drawLine(color = accent, start = p(27f, y), end = p(30f, y + 3f), strokeWidth = 2.5f * u, cap = StrokeCap.Round)
                drawLine(color = accent, start = p(30f, y + 3f), end = p(35f, y - 4f), strokeWidth = 2.5f * u, cap = StrokeCap.Round)
                tile(46f, y - 2.5f, 24f, 5f, light, 2.5f)
            }
        }
        OmniWelcomeKind.GALLERY -> {
            tile(15f, 22f, 64f, 63f, accent.copy(alpha = 0.22f))
            tile(23f, 14f, 65f, 65f, white)
            drawCircle(color = Color(0xFFF7C77C), radius = 8f * u, center = p(69f, 35f))
            val mountains = Path().apply {
                moveTo(29f * u, 68f * u)
                lineTo(47f * u, 46f * u)
                lineTo(57f * u, 58f * u)
                lineTo(67f * u, 50f * u)
                lineTo(82f * u, 68f * u)
                close()
            }
            drawPath(mountains, accent)
            tile(29f, 70f, 53f, 3f, accent.copy(alpha = 0.24f), 1.5f)
        }
        OmniWelcomeKind.PRIVACY -> {
            tile(13f, 14f, 64f, 67f, white)
            tile(22f, 23f, 46f, 5f, light, 2f)
            tile(22f, 34f, 30f, 4f, light, 2f)
            val shield = Path().apply {
                moveTo(66f * u, 34f * u)
                lineTo(90f * u, 44f * u)
                lineTo(87f * u, 64f * u)
                quadraticTo(82f * u, 78f * u, 66f * u, 85f * u)
                quadraticTo(50f * u, 78f * u, 45f * u, 64f * u)
                lineTo(42f * u, 44f * u)
                close()
            }
            drawPath(shield, accent)
            drawLine(white, p(56f, 59f), p(64f, 66f), strokeWidth = 4f * u, cap = StrokeCap.Round)
            drawLine(white, p(64f, 66f), p(78f, 51f), strokeWidth = 4f * u, cap = StrokeCap.Round)
        }
        OmniWelcomeKind.PRO -> {
            val gem = Path().apply {
                moveTo(30f * u, 27f * u)
                lineTo(71f * u, 27f * u)
                lineTo(84f * u, 43f * u)
                lineTo(50f * u, 83f * u)
                lineTo(16f * u, 43f * u)
                close()
            }
            drawPath(gem, accent)
            drawLine(white.copy(alpha = 0.75f), p(16f, 43f), p(84f, 43f), strokeWidth = 2f * u)
            drawLine(white.copy(alpha = 0.75f), p(37f, 28f), p(50f, 82f), strokeWidth = 2f * u)
            drawLine(white.copy(alpha = 0.75f), p(64f, 28f), p(50f, 82f), strokeWidth = 2f * u)
            drawLine(white, p(78f, 9f), p(78f, 22f), strokeWidth = 3f * u, cap = StrokeCap.Round)
            drawLine(white, p(72f, 16f), p(85f, 16f), strokeWidth = 3f * u, cap = StrokeCap.Round)
        }
    }
}
