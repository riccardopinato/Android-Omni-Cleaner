package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.StorageInfo
import java.util.Locale
import kotlin.math.roundToInt

internal fun bentoByteLabel(bytes: Long): String {
    if (bytes < 0L) return "—"
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var amount = bytes.toDouble()
    var index = -1
    while (amount >= 1024.0 && index < units.lastIndex) {
        amount /= 1024.0
        index++
    }
    return String.format(Locale.getDefault(), "%.1f %s", amount, units[index])
}

internal fun bentoPercent(info: StorageInfo?): Int? =
    info?.takeIf { it.totalBytes > 0L }
        ?.let { (it.usedPercent.coerceIn(0f, 1f) * 100f).roundToInt() }

internal fun bentoVerifiedCount(scanned: Boolean, count: Int): String =
    if (scanned) count.coerceAtLeast(0).toString() else "—"

/**
 * A brand motif, not a dataset. It is deliberately NEVER called a storage trend.
 * Real storage state is conveyed by the textual value and proportional bar.
 */
@Composable
private fun BentoWave(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        for (i in 0..5) {
            val x = w * (.12f + .15f * i)
            drawLine(
                Color.White.copy(alpha = .32f),
                start = Offset(x, h * .06f),
                end = Offset(x, h * .94f),
                strokeWidth = 1.dp.toPx(),
            )
        }
        val path = Path().apply {
            moveTo(w * .10f, h * .72f)
            cubicTo(w * .25f, h * .77f, w * .24f, h * .23f, w * .39f, h * .41f)
            cubicTo(w * .51f, h * .67f, w * .57f, h * .57f, w * .64f, h * .39f)
            cubicTo(w * .74f, h * .20f, w * .80f, h * .30f, w * .91f, h * .15f)
        }
        drawPath(path, OmniColorBento.lime, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
internal fun OmniBentoStorageCard(
    info: StorageInfo?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val percent = bentoPercent(info)
    Column(
        modifier = modifier.heightIn(min = 176.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(OmniColorBento.olive)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("omni_bento_storage")
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(57.dp)) {
            BentoWave(Modifier.fillMaxSize())
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                info?.let { bentoByteLabel(it.usedBytes) } ?: "—",
                color = OmniColorBento.lime,
                fontFamily = OmniColorBento.display,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                maxLines = 2,
            )
            Text(
                androidx.compose.ui.res.stringResource(R.string.bento_storage_used),
                color = OmniColorBento.white,
                fontFamily = OmniColorBento.body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
            )
            Box(
                Modifier.fillMaxWidth().height(6.dp)
                    .clip(CircleShape)
                    .background(OmniColorBento.white.copy(alpha = .29f)),
            ) {
                if (percent != null && percent > 0) {
                    Box(
                        Modifier.fillMaxWidth(percent / 100f).height(6.dp)
                            .background(OmniColorBento.lime, CircleShape),
                    )
                }
            }
            Text(
                if (info != null && percent != null)
                    androidx.compose.ui.res.stringResource(
                        R.string.bento_storage_capacity, bentoByteLabel(info.totalBytes), percent,
                    )
                else androidx.compose.ui.res.stringResource(R.string.bento_storage_unavailable),
                color = OmniColorBento.cream,
                fontFamily = OmniColorBento.body,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
            )
        }
    }
}

/**
 * Compact first-party pictograms composed with vector primitives.
 * They replace recycled broom and platform-font glyphs in the Bento UI.
 * 1 = document / large file, 2 = duplicates, 4 = trash queue.
 */
@Composable
internal fun OmniBentoUtilityIcon(
    kind: Int,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(27.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round)
        when (kind) {
            1 -> {
                val paper = Path().apply {
                    moveTo(w * .18f, h * .06f)
                    lineTo(w * .63f, h * .06f)
                    lineTo(w * .85f, h * .30f)
                    lineTo(w * .85f, h * .94f)
                    lineTo(w * .18f, h * .94f)
                    close()
                }
                drawPath(paper, tint, style = stroke)
                drawLine(tint, Offset(w * .63f, h * .06f), Offset(w * .63f, h * .30f), stroke.width)
                drawLine(tint, Offset(w * .63f, h * .30f), Offset(w * .85f, h * .30f), stroke.width)
                drawLine(tint, Offset(w * .29f, h * .55f), Offset(w * .70f, h * .55f), stroke.width)
                drawLine(tint, Offset(w * .29f, h * .70f), Offset(w * .61f, h * .70f), stroke.width)
            }
            2 -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * .09f, h * .09f),
                    size = Size(w * .60f, h * .66f),
                    cornerRadius = CornerRadius(w * .12f),
                    style = stroke,
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * .31f, h * .28f),
                    size = Size(w * .60f, h * .66f),
                    cornerRadius = CornerRadius(w * .12f),
                    style = stroke,
                )
            }
            4 -> {
                drawLine(tint, Offset(w * .15f, h * .27f), Offset(w * .85f, h * .27f), stroke.width)
                drawLine(tint, Offset(w * .38f, h * .14f), Offset(w * .62f, h * .14f), stroke.width)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * .25f, h * .34f),
                    size = Size(w * .50f, h * .57f),
                    cornerRadius = CornerRadius(w * .06f),
                    style = stroke,
                )
                drawLine(tint, Offset(w * .40f, h * .49f), Offset(w * .40f, h * .75f), stroke.width)
                drawLine(tint, Offset(w * .60f, h * .49f), Offset(w * .60f, h * .75f), stroke.width)
            }
        }
    }
}

@Composable
internal fun OmniBentoMetricCard(
    value: String,
    label: String,
    symbol: OmniNavigationSymbol,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String,
) {
    Column(
        modifier = modifier.heightIn(min = 176.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 13.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (tag == "omni_bento_duplicates") {
            OmniBentoUtilityIcon(kind = 2, tint = foreground, modifier = Modifier.size(28.dp))
        } else {
            OmniNavigationIcon(symbol = symbol, tint = foreground, modifier = Modifier.size(28.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                value,
                color = foreground,
                fontFamily = OmniColorBento.display,
                fontWeight = FontWeight.Bold,
                fontSize = 33.sp,
                lineHeight = 36.sp,
                maxLines = 1,
            )
            Text(
                label,
                color = foreground,
                fontFamily = OmniColorBento.body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Original Compose illustration, with no external imagery or sample phone files. */
@Composable
private fun BentoPhotoStack() {
    Box(
        modifier = Modifier.size(width = 93.dp, height = 111.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(89.dp).background(OmniColorBento.cyan, CircleShape))
        Box(
            Modifier.width(61.dp).height(83.dp)
                .rotate(16f).background(OmniColorBento.forest, RoundedCornerShape(13.dp)),
        )
        Box(
            Modifier.width(68.dp).height(83.dp)
                .rotate(-13f).background(OmniColorBento.pink, RoundedCornerShape(13.dp)),
        ) {
            Canvas(Modifier.fillMaxSize().padding(7.dp)) {
                val mountain = Path().apply {
                    moveTo(0f, size.height * .83f)
                    lineTo(size.width * .46f, size.height * .29f)
                    lineTo(size.width * .71f, size.height * .53f)
                    lineTo(size.width, size.height * .17f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(mountain, OmniColorBento.violet)
                drawCircle(
                    color = OmniColorBento.ochre,
                    radius = size.width * .095f,
                    center = Offset(size.width * .21f, size.height * .24f),
                )
            }
        }
    }
}

@Composable
internal fun OmniBentoGalleryCard(
    title: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.heightIn(min = 186.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(OmniColorBento.cobalt)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("omni_bento_gallery")
            .padding(horizontal = 9.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        BentoPhotoStack()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                color = OmniColorBento.white,
                fontFamily = OmniColorBento.display,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                lineHeight = 22.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                caption,
                color = OmniColorBento.white.copy(alpha = .86f),
                fontFamily = OmniColorBento.body,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 3,
            )
            Text(
                "↗",
                color = OmniColorBento.lime,
                fontFamily = OmniColorBento.display,
                fontSize = 25.sp,
            )
        }
    }
}
