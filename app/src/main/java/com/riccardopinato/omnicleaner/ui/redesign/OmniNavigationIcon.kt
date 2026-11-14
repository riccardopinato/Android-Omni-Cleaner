package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * First-party, lightweight semantic navigation icons.
 *
 * Visual direction: Design-App / Minimal Controls + Design System.
 * No vendor image assets or extended icon font are bundled.
 * Each NavigationBarItem supplies the localized label/semantics.
 */
internal enum class OmniNavigationSymbol { HOME, CLEAN, GALLERY, PRIVACY, PRO }

@Composable
internal fun OmniNavigationIcon(
    symbol: OmniNavigationSymbol,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp).clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val line = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (symbol) {
            OmniNavigationSymbol.HOME -> {
                val roof = Path().apply {
                    moveTo(w * .12f, h * .48f)
                    lineTo(w * .50f, h * .14f)
                    lineTo(w * .88f, h * .48f)
                }
                drawPath(roof, tint, style = line)
                val house = Path().apply {
                    moveTo(w * .23f, h * .43f)
                    lineTo(w * .23f, h * .86f)
                    lineTo(w * .77f, h * .86f)
                    lineTo(w * .77f, h * .43f)
                }
                drawPath(house, tint, style = line)
                drawLine(tint, Offset(w * .43f, h * .86f), Offset(w * .43f, h * .62f), strokeWidth = line.width)
                drawLine(tint, Offset(w * .57f, h * .86f), Offset(w * .57f, h * .62f), strokeWidth = line.width)
            }
            OmniNavigationSymbol.CLEAN -> {
                val sparkle = Path().apply {
                    moveTo(w * .49f, h * .10f)
                    lineTo(w * .60f, h * .39f)
                    lineTo(w * .89f, h * .50f)
                    lineTo(w * .60f, h * .61f)
                    lineTo(w * .49f, h * .90f)
                    lineTo(w * .38f, h * .61f)
                    lineTo(w * .09f, h * .50f)
                    lineTo(w * .38f, h * .39f)
                    close()
                }
                drawPath(sparkle, tint, style = line)
            }
            OmniNavigationSymbol.GALLERY -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * .11f, h * .19f),
                    size = Size(w * .78f, h * .62f),
                    cornerRadius = CornerRadius(w * .09f, h * .09f),
                    style = line,
                )
                drawCircle(
                    color = tint,
                    radius = w * .07f,
                    center = Offset(w * .67f, h * .38f),
                )
                val mountains = Path().apply {
                    moveTo(w * .17f, h * .72f)
                    lineTo(w * .37f, h * .48f)
                    lineTo(w * .52f, h * .64f)
                    lineTo(w * .62f, h * .54f)
                    lineTo(w * .82f, h * .75f)
                }
                drawPath(mountains, tint, style = line)
            }
            OmniNavigationSymbol.PRIVACY -> {
                val shield = Path().apply {
                    moveTo(w * .50f, h * .12f)
                    lineTo(w * .82f, h * .25f)
                    lineTo(w * .78f, h * .61f)
                    quadraticTo(w * .72f, h * .77f, w * .50f, h * .89f)
                    quadraticTo(w * .28f, h * .77f, w * .22f, h * .61f)
                    lineTo(w * .18f, h * .25f)
                    close()
                }
                drawPath(shield, tint, style = line)
                val check = Path().apply {
                    moveTo(w * .35f, h * .50f)
                    lineTo(w * .46f, h * .61f)
                    lineTo(w * .67f, h * .38f)
                }
                drawPath(check, tint, style = line)
            }
            OmniNavigationSymbol.PRO -> {
                val star = Path().apply {
                    moveTo(w * .50f, h * .10f)
                    lineTo(w * .61f, h * .39f)
                    lineTo(w * .91f, h * .39f)
                    lineTo(w * .67f, h * .58f)
                    lineTo(w * .76f, h * .88f)
                    lineTo(w * .50f, h * .71f)
                    lineTo(w * .24f, h * .88f)
                    lineTo(w * .33f, h * .58f)
                    lineTo(w * .09f, h * .39f)
                    lineTo(w * .39f, h * .39f)
                    close()
                }
                drawPath(star, tint, style = line)
            }
        }
    }
}
