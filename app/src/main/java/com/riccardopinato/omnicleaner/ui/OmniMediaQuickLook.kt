package com.riccardopinato.omnicleaner.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import com.riccardopinato.omnicleaner.R

/**
 * Read-only media Quick Look for any scanner result. A long press opens it;
 * release does not delete or play media. Only user-initiated Play can emit audio.
 * File-backed and content:// MediaStore items share the same component.
 */
internal data class OmniQuickLookTarget(
    val uri: Uri,
    val name: String,
    val kind: OmniQuickLookKind,
)

internal enum class OmniQuickLookKind { PHOTO, VIDEO, AUDIO }

internal fun omniQuickLookKind(name: String, mimeType: String? = null): OmniQuickLookKind? {
    val mime = mimeType?.lowercase(Locale.ROOT).orEmpty()
    if (mime.startsWith("image/")) return OmniQuickLookKind.PHOTO
    if (mime.startsWith("video/")) return OmniQuickLookKind.VIDEO
    if (mime.startsWith("audio/")) return OmniQuickLookKind.AUDIO
    return when (name.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
        "jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp", "avif" -> OmniQuickLookKind.PHOTO
        "mp4", "m4v", "mov", "mkv", "webm", "3gp", "avi" -> OmniQuickLookKind.VIDEO
        "mp3", "m4a", "aac", "ogg", "opus", "flac", "wav", "amr", "wma" -> OmniQuickLookKind.AUDIO
        else -> null
    }
}

internal fun omniQuickLookForFile(file: File): OmniQuickLookTarget? =
    omniQuickLookKind(file.name)?.let { kind ->
        OmniQuickLookTarget(Uri.fromFile(file), file.name, kind)
    }

@Composable
internal fun OmniMediaQuickLook(
    target: OmniQuickLookTarget,
    onDismiss: () -> Unit,
    selected: Boolean? = null,
    onSelectedChange: ((Boolean) -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).testTag("omni_media_quicklook"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        target.name,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 520.dp)
                        .background(Color(0xFF101827), RoundedCornerShape(16.dp))
                        .testTag("omni_media_quicklook_content"),
                    contentAlignment = Alignment.Center,
                ) {
                    when (target.kind) {
                        OmniQuickLookKind.PHOTO -> OmniQuickLookImage(target)
                        OmniQuickLookKind.VIDEO -> OmniQuickLookVideo(target)
                        OmniQuickLookKind.AUDIO -> OmniQuickLookAudio(target)
                    }
                }
                if (selected != null && onSelectedChange != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = onSelectedChange,
                            modifier = Modifier.testTag("omni_media_quicklook_select"),
                        )
                        Text(stringResource(R.string.quicklook_select_for_review))
                    }
                }
                Text(
                    stringResource(R.string.quicklook_read_only_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OmniQuickLookImage(target: OmniQuickLookTarget) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, target.uri) {
        value = withContext(Dispatchers.IO) { loadOmniQuickLookBitmap(context, target.uri) }
    }
    val shown = bitmap
    if (shown != null) {
        Image(
            bitmap = shown.asImageBitmap(),
            contentDescription = target.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().heightIn(max = 510.dp),
        )
    } else {
        Text(stringResource(R.string.gallery_preview_unavailable), color = Color.White)
    }
}

internal fun loadOmniQuickLookBitmap(context: Context, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width.coerceAtLeast(1)
            val h = info.size.height.coerceAtLeast(1)
            val largest = maxOf(w, h)
            if (largest > 1600) {
                val scale = 1600f / largest
                decoder.setTargetSize(
                    (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1),
                )
            }
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (longest / sample > 1600) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}

@Composable
private fun OmniQuickLookVideo(target: OmniQuickLookTarget) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var playing by remember(target.uri) { mutableStateOf(false) }
    var error by remember(target.uri) { mutableStateOf(false) }
    val video = remember(target.uri) {
        VideoView(context).apply {
            setOnErrorListener { _, _, _ -> error = true; playing = false; true }
            setOnCompletionListener { playing = false }
            runCatching { setVideoURI(target.uri) }.onFailure { error = true }
        }
    }
    DisposableEffect(video, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                video.pause()
                playing = false
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            video.stopPlayback()
        }
    }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AndroidView(
            factory = { video },
            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 450.dp),
        )
        if (error) {
            Text(stringResource(R.string.quicklook_video_unavailable), color = Color.White)
        } else {
            Button(
                onClick = {
                    runCatching {
                        if (video.isPlaying) {
                            video.pause()
                            playing = false
                        } else {
                            video.start()
                            playing = true
                        }
                    }.onFailure { error = true }
                },
                modifier = Modifier.testTag("omni_media_quicklook_play"),
            ) { Text(if (playing) stringResource(R.string.quicklook_pause) else stringResource(R.string.quicklook_play_video)) }
        }
    }
}

@Composable
private fun OmniQuickLookAudio(target: OmniQuickLookTarget) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var ready by remember(target.uri) { mutableStateOf(false) }
    var playing by remember(target.uri) { mutableStateOf(false) }
    var failed by remember(target.uri) { mutableStateOf(false) }
    val player = remember(target.uri) { MediaPlayer() }
    DisposableEffect(player, target.uri, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (player.isPlaying) player.pause()
                playing = false
            }
        }
        lifecycle.addObserver(observer)
        player.setOnPreparedListener { ready = true }
        player.setOnCompletionListener { playing = false }
        player.setOnErrorListener { _, _, _ -> failed = true; playing = false; true }
        runCatching {
            player.setDataSource(context, target.uri)
            player.prepareAsync()
        }.onFailure { failed = true }
        onDispose {
            lifecycle.removeObserver(observer)
            player.release()
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("♫", style = MaterialTheme.typography.displayLarge, color = Color.White)
        if (failed) {
            Text(stringResource(R.string.quicklook_audio_unavailable), color = Color.White)
        } else {
            Button(
                onClick = {
                    if (ready) {
                        runCatching {
                            if (player.isPlaying) {
                                player.pause()
                                playing = false
                            } else {
                                player.start()
                                playing = true
                            }
                        }.onFailure { failed = true }
                    }
                },
                enabled = ready,
                modifier = Modifier.testTag("omni_media_quicklook_play"),
            ) { Text(when { playing -> stringResource(R.string.quicklook_pause); ready -> stringResource(R.string.quicklook_listen); else -> stringResource(R.string.quicklook_loading) }) }
        }
    }
}
