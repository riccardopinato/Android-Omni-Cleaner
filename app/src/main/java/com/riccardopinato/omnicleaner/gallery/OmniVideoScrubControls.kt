package com.riccardopinato.omnicleaner.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import java.util.Locale
import kotlin.math.roundToInt

/**
 * WML-003 / Watermelon Scrub Slider adapted as an accessible real video seek.
 * A thin timeline and a single accent; no spring, rolling counters or fake values.
 * Duration and position come exclusively from the active Android VideoView.
 */
@Composable
internal fun OmniVideoScrubControls(
    durationMs: Int,
    positionMs: Int,
    playing: Boolean,
    onSeek: (Int) -> Unit,
    onPlayPause: () -> Unit,
) {
    val duration = durationMs.coerceAtLeast(0)
    val position = positionMs.coerceIn(0, duration)
    var dragging by remember { mutableStateOf(false) }
    var draggedPosition by remember { mutableIntStateOf(0) }
    val selected = if (dragging) draggedPosition.coerceIn(0, duration) else position
    val seekLabel = stringResource(R.string.gallery_video_seek)
    Column(
        modifier = Modifier.fillMaxWidth()
            .testTag("omni_video_scrub_controls")
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                videoTimelineTimestamp(selected),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
            Text(
                videoTimelineTimestamp(duration),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFBEC8D8),
            )
        }
        Slider(
            value = selected.toFloat(),
            onValueChange = {
                if (!dragging) dragging = true
                draggedPosition = it.roundToInt().coerceIn(0, duration)
            },
            onValueChangeFinished = {
                if (dragging) {
                    onSeek(boundedVideoSeekPosition(draggedPosition, duration))
                    dragging = false
                }
            },
            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
            enabled = duration > 0,
            modifier = Modifier.fillMaxWidth()
                .testTag("omni_video_scrub_slider")
                .semantics { contentDescription = seekLabel },
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF96B9FF),
                activeTrackColor = Color(0xFF96B9FF),
                inactiveTrackColor = Color(0xFF667286),
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { onSeek((position.toLong() - 10_000L).coerceAtLeast(0).toInt()) },
                enabled = duration > 0,
                modifier = Modifier.testTag("omni_video_seek_back"),
            ) {
                Text(stringResource(R.string.gallery_video_back_10), color = Color.White)
            }
            TextButton(
                onClick = onPlayPause,
                modifier = Modifier.testTag("omni_video_toggle_play"),
            ) {
                Text(
                    stringResource(
                        if (playing) R.string.gallery_video_pause
                        else R.string.gallery_video_resume,
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TextButton(
                onClick = {
                    onSeek((position.toLong() + 10_000L).coerceAtMost(duration.toLong()).toInt())
                },
                enabled = duration > 0,
                modifier = Modifier.testTag("omni_video_seek_forward"),
            ) {
                Text(stringResource(R.string.gallery_video_forward_10), color = Color.White)
            }
        }
    }
}

/** One-hour+ videos retain hours; negative/unavailable timestamps render 00:00. */
internal fun videoTimelineTimestamp(milliseconds: Int): String {
    val seconds = milliseconds.coerceAtLeast(0).toLong() / 1000L
    val h = seconds / 3600L
    val m = (seconds / 60L) % 60L
    val s = seconds % 60L
    return if (h > 0L) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.ROOT, "%02d:%02d", m, s)
}

internal fun boundedVideoSeekPosition(requestedMs: Int, durationMs: Int): Int =
    requestedMs.coerceIn(0, durationMs.coerceAtLeast(0))
