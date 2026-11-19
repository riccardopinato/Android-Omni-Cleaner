package com.riccardopinato.omnicleaner.ui.redesign

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Native Compose adaptation of Design-App AMX-006 / UID-0006 Accordion Loader.
 *
 * Original: Amicro AccordionLoader (MIT), four 40x4px horizontal bars with
 * scaleX 1 -> .2 -> 1, 1.5s easeInOut, staggered 150ms, origin on the left.
 * Source: Design-App/BIBLIOTECA_COMPONENTI/sorgenti/amicro/amx-006.tsx
 * Reference: Design-App/.../flutter_adapters/lib/src/amx_006_accordion_loader.dart
 *
 * Show ONLY when an actual scan is running and its total is not known; this
 * is intentionally indeterminate and never manufactures percentages.
 *
 * Removed from composition on scan completion/cancel/error, so the animation
 * cannot continue running offscreen. Honors Android's disabled animations
 * setting by drawing the four static bars instead of starting a transition.
 */
@Composable
internal fun OmniAccordionLoader(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val animate = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
        ValueAnimator.areAnimatorsEnabled()

    if (!animate) {
        AccordionBars(
            modifier = modifier,
            size = size,
            color = color,
            fractions = listOf(1f, 1f, 1f, 1f),
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "omni_accordion_scan")
    val fractions = (0 until 4).map { index ->
        val cycle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(index * 150, StartOffsetType.Delay),
            ),
            label = "omni_accordion_bar_$index",
        )
        val triangle = if (cycle < .5f) cycle * 2f else (1f - cycle) * 2f
        val smooth = triangle * triangle * (3f - 2f * triangle)
        1f - .8f * smooth
    }
    AccordionBars(modifier = modifier, size = size, color = color, fractions = fractions)
}

@Composable
private fun AccordionBars(
    modifier: Modifier,
    size: Dp,
    color: Color,
    fractions: List<Float>,
) {
    Canvas(modifier = modifier.size(size).progressSemantics()) {
        val barHeight = this.size.height * .10f
        val gap = this.size.height * .15f
        val startY = (this.size.height - 4f * barHeight - 3f * gap) / 2f
        for (index in 0 until 4) {
            drawRoundRect(
                color = color,
                topLeft = Offset(0f, startY + index * (barHeight + gap)),
                size = Size(this.size.width * fractions[index], barHeight),
                cornerRadius = CornerRadius(barHeight / 2f),
            )
        }
    }
}
