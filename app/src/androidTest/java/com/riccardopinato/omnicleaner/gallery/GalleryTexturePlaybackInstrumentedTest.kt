package com.riccardopinato.omnicleaner.gallery

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.TextureView
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exact production MediaPlayer + TextureView session, rather than a different
 * VideoView implementation. Synthetic local media, no user files or deletion.
 * A prepared player and a progressing timer are NOT sufficient: require real,
 * chromatic pixels to differ across frames from TextureView.getBitmap().
 */
@RunWith(AndroidJUnit4::class)
class GalleryTexturePlaybackInstrumentedTest {
    @Test
    fun productionTexturePlayerRendersTwoDistinctVideoFrames() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = instrumentation.targetContext
        val emulator = Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish" ||
            Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk_gphone")
        assertTrue("This test is restricted to disposable emulator fixtures", emulator)

        val clip = File.createTempFile("omni-texture-motion-", ".mp4", target.cacheDir)
        val activityRef = AtomicReference<Activity>()
        val textureRef = AtomicReference<TextureView>()
        val playbackRef = AtomicReference<GalleryTexturePlaybackSession>()
        val prepared = CountDownLatch(1)
        val playbackFailed = AtomicReference<String?>(null)
        try {
            instrumentation.context.assets.open("omni-native-playback.mp4").use { input ->
                clip.outputStream().use { output -> input.copyTo(output) }
            }
            assertTrue("Synthetic video asset missing", clip.length() > 10_000L)
            val launchIntent = requireNotNull(
                target.packageManager.getLaunchIntentForPackage(target.packageName)
            ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
            val activity = instrumentation.startActivitySync(launchIntent)
            activityRef.set(activity)
            instrumentation.waitForIdleSync()

            instrumentation.runOnMainSync {
                val root = activity.findViewById<FrameLayout>(android.R.id.content)
                val texture = TextureView(activity)
                textureRef.set(texture)
                root.addView(
                    texture,
                    FrameLayout.LayoutParams(720, 405, Gravity.CENTER),
                )
                val playback = GalleryTexturePlaybackSession(
                    context = target,
                    uri = Uri.fromFile(clip),
                    onReady = { prepared.countDown() },
                    onPlaying = {},
                    onComplete = {},
                    onFailed = { playbackFailed.set("production TextureView session failed") },
                )
                playbackRef.set(playback)
                playback.bind(texture)
            }
            assertTrue("MediaPlayer prepare failed: ${playbackFailed.get()}", prepared.await(22, TimeUnit.SECONDS))
            Thread.sleep(800L)
            val first = capturePixels(instrumentation, requireNotNull(textureRef.get()))
            Thread.sleep(1250L)
            val second = capturePixels(instrumentation, requireNotNull(textureRef.get()))
            assertTrue("Production media session reported failure", playbackFailed.get() == null)

            var changed = 0
            var colorful = 0
            for (i in first.indices) {
                val a = first[i]
                val b = second[i]
                val r = Color.red(b)
                val g = Color.green(b)
                val blue = Color.blue(b)
                if (maxOf(r, g, blue) - minOf(r, g, blue) >= 40 &&
                    maxOf(r, g, blue) >= 95
                ) colorful++
                if (maxOf(
                    kotlin.math.abs(Color.red(a) - r),
                    kotlin.math.abs(Color.green(a) - g),
                    kotlin.math.abs(Color.blue(a) - blue),
                ) >= 24) changed++
            }
            android.util.Log.i(
                "OMNI_TEXTURE_VIDEO_PASS",
                "actualTexturePixels changed=$changed chromatic=$colorful",
            )
            assertTrue("TextureView is black/static: chromatic=$colorful", colorful >= 200)
            assertTrue("Video has no independently changing pixels: changed=$changed", changed >= 180)
        } finally {
            if (activityRef.get() != null) {
                instrumentation.runOnMainSync {
                    playbackRef.get()?.release()
                    textureRef.get()?.let { view ->
                        (view.parent as? FrameLayout)?.removeView(view)
                    }
                }
            }
            assertTrue("Only the unique app-private QA clip may be cleaned", clip.delete())
        }
    }

    private fun capturePixels(
        instrumentation: android.app.Instrumentation,
        texture: TextureView,
    ): IntArray {
        val snapshot = AtomicReference<IntArray?>()
        instrumentation.runOnMainSync {
            val bitmap = texture.getBitmap(160, 90)
            if (bitmap != null) {
                val pixels = IntArray(160 * 90)
                bitmap.getPixels(pixels, 0, 160, 0, 0, 160, 90)
                bitmap.recycle()
                snapshot.set(pixels)
            }
        }
        return requireNotNull(snapshot.get()) {
            "TextureView did not provide actual rendered media pixels"
        }
    }
}
