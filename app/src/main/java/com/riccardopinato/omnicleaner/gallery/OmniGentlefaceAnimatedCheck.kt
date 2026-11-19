package com.riccardopinato.omnicleaner.gallery

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * Faithful native-Compose adaptation of the *user-supplied* Gentleface check SVG/SMIL.
 *
 * 16x16 check path M2.5 8.5 L5.5 11.5 L12.5 4.5, 1.001px rounded stroke.
 * Preserves the supplied 2200ms loop, disappearing stroke at 23.2%-31.1%,
 * retraced segments and subtle tip extension at 43.6%-54.5%.
 *
 * No WebView, bundled SVG interpreter or dependency. Decorative: the parent
 * button owns the localized label and actions. The animation respects the
 * Android system animator setting and immediately draws the whole check when
 * disabled.
 */
@Composable
internal fun OmniGentlefaceAnimatedCheck(
    tint: Color,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val motionAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
        ValueAnimator.areAnimatorsEnabled()
    // Do not even start an infinite animation when device motion is disabled,
    // or when this glyph is used as a static decision-feedback mark.
    val t = if (motionAllowed && animated) {
        val transition = rememberInfiniteTransition(label = "Gentleface check cycle")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "Gentleface check stroke reveal",
        ).value
    } else 1f
    val progress = when {
        t < .136f -> 1f
        t < .234f -> 1f - smoothPhase(t, .136f, .234f)
        t < .311f -> 0f
        t < .477f -> smoothPhase(t, .311f, .477f)
        else -> 1f
    }
    val visible = t < .232f || t >= .311f
    val tipExtension = when {
        t < .436f -> 0f
        t < .482f -> .4f * smoothPhase(t, .436f, .482f)
        t < .545f -> .4f * (1f - smoothPhase(t, .482f, .545f))
        else -> 0f
    }
    Canvas(modifier = modifier.size(23.dp).clearAndSetSemantics { }) {
        if (!visible || progress <= 0f) return@Canvas
        withTransform({
            scale(size.width / 16f, size.height / 16f, pivot = Offset.Zero)
        }) {
            val path = Path().apply {
                moveTo(2.5f, 8.5f)
                lineTo(5.5f, 11.5f)
                lineTo(12.5f + tipExtension, 4.5f - tipExtension)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val shown = Path()
            // Stroke extraction mirrors the SMIL dasharray's retracing,
            // without changing hit targets or the selection commit timing.
            measure.getSegment(0f, measure.length * progress, shown, true)
            drawPath(
                shown, tint,
                style = Stroke(
                    width = 1.001f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

private fun smoothPhase(t: Float, start: Float, end: Float): Float {
    val x = ((t - start) / (end - start)).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}
