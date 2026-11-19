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
    animateOnEnter: Boolean = false,
) {
    if (symbol == OmniNavigationSymbol.GALLERY) {
        OmniAnimatedGalleryIcon(tint = tint, modifier = modifier, active = animateOnEnter)
        return
    }
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
                // Compact original Premium broom, custom drawn as a vector rather
                // than a bitmap. It tints with the destination's selected state.
                // A rounded diagonal handle leads into a full bristle fan.
                drawLine(
                    color = tint,
                    start = Offset(w * .77f, h * .10f),
                    end = Offset(w * .47f, h * .54f),
                    strokeWidth = w * .105f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White.copy(alpha = .42f),
                    start = Offset(w * .76f, h * .11f),
                    end = Offset(w * .53f, h * .46f),
                    strokeWidth = w * .026f,
                    cap = StrokeCap.Round,
                )
                val collar = Path().apply {
                    moveTo(w * .38f, h * .48f)
                    lineTo(w * .57f, h * .59f)
                    lineTo(w * .53f, h * .67f)
                    lineTo(w * .33f, h * .55f)
                    close()
                }
                drawPath(collar, tint)
                val bristleFan = Path().apply {
                    moveTo(w * .34f, h * .56f)
                    quadraticTo(w * .45f, h * .60f, w * .55f, h * .65f)
                    cubicTo(w * .66f, h * .76f, w * .67f, h * .84f, w * .61f, h * .93f)
                    lineTo(w * .51f, h * .87f)
                    lineTo(w * .44f, h * .96f)
                    lineTo(w * .35f, h * .88f)
                    lineTo(w * .22f, h * .92f)
                    cubicTo(w * .17f, h * .80f, w * .22f, h * .67f, w * .34f, h * .56f)
                    close()
                }
                drawPath(bristleFan, tint)
                // Subtle grooves keep the broom identifiable at 24 dp.
                drawLine(
                    color = Color.White.copy(alpha = .55f),
                    start = Offset(w * .41f, h * .69f),
                    end = Offset(w * .35f, h * .84f),
                    strokeWidth = w * .031f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White.copy(alpha = .55f),
                    start = Offset(w * .49f, h * .71f),
                    end = Offset(w * .52f, h * .84f),
                    strokeWidth = w * .031f,
                    cap = StrokeCap.Round,
                )
                // One tiny four-point sparkle completes the visual motif.
                val sparkle = Path().apply {
                    moveTo(w * .23f, h * .17f)
                    lineTo(w * .26f, h * .25f)
                    lineTo(w * .34f, h * .28f)
                    lineTo(w * .26f, h * .31f)
                    lineTo(w * .23f, h * .39f)
                    lineTo(w * .20f, h * .31f)
                    lineTo(w * .12f, h * .28f)
                    lineTo(w * .20f, h * .25f)
                    close()
                }
                drawPath(sparkle, tint.copy(alpha = .83f))
            }
            OmniNavigationSymbol.GALLERY -> Unit // Native Gentleface-style icon above.
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
                val crown = Path().apply {
                    moveTo(w * .13f, h * .34f)
                    lineTo(w * .34f, h * .58f)
                    lineTo(w * .50f, h * .22f)
                    lineTo(w * .66f, h * .58f)
                    lineTo(w * .87f, h * .34f)
                    lineTo(w * .82f, h * .82f)
                    lineTo(w * .18f, h * .82f)
                    close()
                }
                drawPath(crown, tint, style = line)
                drawCircle(tint, radius = w * .042f, center = Offset(w * .13f, h * .29f))
                drawCircle(tint, radius = w * .042f, center = Offset(w * .50f, h * .16f))
                drawCircle(tint, radius = w * .042f, center = Offset(w * .87f, h * .29f))
            }
        }
    }
}
