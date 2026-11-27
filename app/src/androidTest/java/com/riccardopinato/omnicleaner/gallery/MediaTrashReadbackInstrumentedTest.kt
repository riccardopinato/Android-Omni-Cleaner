package com.riccardopinato.omnicleaner.gallery

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.net.Uri
import kotlinx.coroutines.runBlocking
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.riccardopinato.omnicleaner.cleaner.MediaStoreHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** An app-created synthetic media item; no existing user media is touched. */
@RunWith(AndroidJUnit4::class)
class MediaTrashReadbackInstrumentedTest {
    @Test
    fun queryIncludesTrashedItemsAfterVerifiedOwnedMediaChange() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "omni-trash-qa-" + UUID.randomUUID() + ".jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OmniCleanerQA/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(collection, values))
        try {
            resolver.openOutputStream(uri)?.use { out ->
                out.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
            } ?: error("Synthetic provider URI could not be opened")
            assertEquals(1, resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null))
            assertEquals(false, MediaStoreHelper.verifiedTrashState(resolver, uri))
            assertEquals(1, resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_TRASHED, 1)
            }, null, null))
            assertEquals(true, MediaStoreHelper.verifiedTrashState(resolver, uri))
        } finally {
            val matchTrashed = Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            }
            assertTrue("Only the unique synthetic item is deleted", resolver.delete(uri, matchTrashed) == 1)
        }
        assertNull(MediaStoreHelper.verifiedTrashState(resolver, uri))
    }

    @Test
    fun staleIdentityAndUnavailableUrisFailClosedBeforeAndAfterConsent() = runBlocking {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@runBlocking
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val name = "omni-identity-qa-" + UUID.randomUUID() + ".jpg"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OmniCleanerQA/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(collection, values))
        val unknown = Uri.parse("content://media/external/images/media/999999999")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                out.write(byteArrayOf(1, 2, 3, 4))
            } ?: error("Could not write synthetic test file")
            assertEquals(1, resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null))
            assertTrue(MediaStoreHelper.isCurrentMediaMatch(resolver, uri, 4L, name))
            assertEquals(false, MediaStoreHelper.isCurrentMediaMatch(resolver, uri, 5L, name))
            assertEquals(false, MediaStoreHelper.isCurrentMediaMatch(resolver, uri, 4L, "replaced.jpg"))
            val pending = MediaStoreHelper.verifyTrashedUris(resolver, listOf(uri, unknown))
            assertTrue(pending.confirmedUris.isEmpty())
            assertTrue(uri in pending.unverifiedUris)
            assertTrue(unknown in pending.unverifiedUris)

            assertEquals(1, resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_TRASHED, 1)
            }, null, null))
            assertEquals(false, MediaStoreHelper.isCurrentMediaMatch(resolver, uri, 4L, name))
            val verified = MediaStoreHelper.verifyTrashedUris(resolver, listOf(uri, unknown))
            assertTrue(uri in verified.confirmedUris)
            assertTrue(unknown in verified.unverifiedUris)
        } finally {
            val includeTrashed = Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            }
            assertEquals(1, resolver.delete(uri, includeTrashed))
        }
    }

}
