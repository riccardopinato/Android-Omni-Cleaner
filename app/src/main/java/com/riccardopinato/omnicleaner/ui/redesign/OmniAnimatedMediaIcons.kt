package com.riccardopinato.omnicleaner.ui.redesign

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
 * User-approved Gentleface "images" and "video" SVG/SMIL motifs, redrawn using
 * lightweight native Compose paths. Original SVG motion is 3.2s / 3s and loops;
 * the in-app adaptation instead plays ONCE upon opening the matching surface.
 *
 * Source: https://gentleface.com/icons/images and /icons/video (MIT);
 * SVG animation sequences were supplied explicitly by the product owner.
 * Presentation-only: labels, state, and click semantics belong to the parent.
 * No WebView, renderer dependency, background loop, or media/data side effect.
 */
private fun motionAllowed(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()

private fun phase(time: Float, start: Float, end: Float): Float =
    ((time - start) / (end - start)).coerceIn(0f, 1f)

private fun eased(time: Float, start: Float, end: Float): Float {
    val x = phase(time, start, end)
    return x * x * (3f - 2f * x)
}

@Composable
internal fun OmniAnimatedGalleryIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val enabled = motionAllowed()
    val progress = remember { Animatable(1f) }
    LaunchedEffect(active, enabled) {
        if (active && enabled) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(3200, easing = LinearEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    val t = progress.value
    // SMIL reference: top frame shifts up-left and back; photo slides out
    // before its outline and photo point redraw. At rest all contours match.
    val outerShift = when {
        t < .225f -> 0f
        t < .338f -> -2f * eased(t, .225f, .338f)
        t < .488f -> -2f
        t < .644f -> -2f * (1f - eased(t, .5f, .644f))
        else -> 0f
    }
    val foregroundX = when {
        t < .094f -> 0f
        t < .156f -> .6f * eased(t, .094f, .156f)
        t < .30f -> .6f - 15.6f * eased(t, .156f, .30f)
        else -> 0f
    }
    val foregroundY = -foregroundX
    val visibleFront = t < .30f || t >= .338f
    val visibleBack = t < .488f || t >= .5f

    Canvas(modifier = modifier.size(24.dp).clearAndSetSemantics { }) {
        withTransform({
            scale(size.width / 16f, size.height / 16f, pivot = Offset.Zero)
        }) {
            val line = Stroke(1.001f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (visibleBack) {
                withTransform({ translate(outerShift, -outerShift) }) {
                    val rear = Path().apply {
                        moveTo(3.5f, 2.5f)
                        lineTo(13f, 2.5f)
                        cubicTo(13.828f, 2.5f, 14.5f, 3.172f, 14.5f, 4f)
                        lineTo(14.5f, 11.5f)
                    }
                    drawPath(rear, tint, style = line)
                }
            }
            if (visibleFront) {
                withTransform({ translate(foregroundX, foregroundY) }) {
                    val photo = Path().apply {
                        moveTo(7f, 4.5f)
                        lineTo(11f, 4.5f)
                        cubicTo(11.828f, 4.5f, 12.5f, 5.172f, 12.5f, 6f)
                        lineTo(12.5f, 12f)
                        cubicTo(12.5f, 12.828f, 11.828f, 13.5f, 11f, 13.5f)
                        lineTo(3f, 13.5f)
                        cubicTo(2.172f, 13.5f, 1.5f, 12.828f, 1.5f, 12f)
                        lineTo(1.5f, 6f)
                        cubicTo(1.5f, 5.172f, 2.172f, 4.5f, 3f, 4.5f)
                        lineTo(7f, 4.5f)
                        close()
                    }
                    drawPath(photo, tint, style = line)
                    val mountain = Path().apply {
                        moveTo(1.5f, 11.5f)
                        lineTo(4.439f, 8.561f)
                        cubicTo(5.025f, 7.975f, 5.975f, 7.975f, 6.561f, 8.561f)
                        lineTo(11.5f, 13.5f)
                    }
                    drawPath(mountain, tint, style = line)
                    val dotScale = when {
                        t < .538f -> 1f
                        t < .6f -> .1f + 1.15f * eased(t, .538f, .6f)
                        t < .656f -> 1.25f - .25f * eased(t, .6f, .656f)
                        else -> 1f
                    }
                    drawCircle(tint, radius = dotScale, center = Offset(10f, 7f))
                }
            }
        }
    }
}

@Composable
internal fun OmniAnimatedVideoIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val enabled = motionAllowed()
    val progress = remember { Animatable(1f) }
    LaunchedEffect(enabled) {
        if (enabled) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(3000, easing = LinearEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    val t = progress.value
    val expansion = when {
        t < .10f -> 0f
        t < .22f -> -.45f * eased(t, .10f, .22f)
        t < .287f -> -.45f
        t < .42f -> -.45f + 1.45f * eased(t, .287f, .42f)
        t < .52f -> 1f
        t < .793f -> 1f - eased(t, .52f, .793f)
        else -> 0f
    }
    val recordVisible = (t in .12f.. .327f) || (t in .38f.. .52f) || (t in .573f.. .753f)
    val recordRadius = when {
        t < .167f -> .2f
        t < .207f -> .2f + 1.7f * eased(t, .167f, .207f)
        t < .70f -> 1.6f
        t < .753f -> 1.6f * (1f - eased(t, .70f, .753f)) + .2f
        else -> .2f
    }

    Canvas(modifier = modifier.size(20.dp).clearAndSetSemantics { }) {
        withTransform({
            scale(size.width / 16f, size.height / 16f, pivot = Offset.Zero)
        }) {
            val line = Stroke(1.001f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val body = Path().apply {
                moveTo(6f, 3.5f)
                lineTo(9f, 3.5f)
                cubicTo(9.828f, 3.5f, 10.5f, 4.172f, 10.5f, 5f)
                lineTo(10.5f, 11f)
                cubicTo(10.5f, 11.828f, 9.828f, 12.5f, 9f, 12.5f)
                lineTo(3f, 12.5f)
                cubicTo(2.172f, 12.5f, 1.5f, 11.828f, 1.5f, 11f)
                lineTo(1.5f, 5f)
                cubicTo(1.5f, 4.172f, 2.172f, 3.5f, 3f, 3.5f)
                lineTo(6f, 3.5f)
                close()
            }
            drawPath(body, tint, style = line)
            val rightX = 13.776f + .541f * expansion
            val topY = 4.862f - 1.272f * expansion
            val bottomY = 11.138f + 1.272f * expansion
            val lens = Path().apply {
                moveTo(10.5f, 6.5f)
                lineTo(rightX, topY)
                cubicTo(
                    rightX + .333f, topY - .166f,
                    rightX + .724f, topY + .075f,
                    rightX + .724f, topY + .447f,
                )
                lineTo(rightX + .724f, bottomY - .447f)
                cubicTo(
                    rightX + .724f, bottomY - .075f,
                    rightX + .333f, bottomY + .166f,
                    rightX, bottomY,
                )
                lineTo(10.5f, 9.5f)
            }
            drawPath(lens, tint, style = line)
            if (recordVisible) {
                drawCircle(tint, radius = recordRadius, center = Offset(6f, 8f))
            }
        }
    }
}
