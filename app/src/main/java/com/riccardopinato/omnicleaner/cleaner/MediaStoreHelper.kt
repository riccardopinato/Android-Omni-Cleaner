package com.riccardopinato.omnicleaner.cleaner

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import com.riccardopinato.omnicleaner.model.GalleryMediaFilter
import com.riccardopinato.omnicleaner.model.GalleryMediaItem
import com.riccardopinato.omnicleaner.model.GallerySortOrder
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaStoreHelper {
    fun isMediaFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in com.riccardopinato.omnicleaner.model.IMAGE_EXTENSIONS ||
            extension in com.riccardopinato.omnicleaner.model.VIDEO_EXTENSIONS
    }

    fun getMediaUri(context: Context, file: File): Uri? {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = MediaStore.Files.FileColumns.DATA + "=?"
        return runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                arrayOf(file.absolutePath),
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    ContentUris.withAppendedId(collection, cursor.getLong(0))
                } else null
            }
        }.getOrNull()
    }

    suspend fun loadGalleryMediaCancellable(
        context: Context,
        filter: GalleryMediaFilter = GalleryMediaFilter.ALL,
        sortOrder: GallerySortOrder = GallerySortOrder.DATE_DESC,
        limit: Int = 500,
    ): List<GalleryMediaItem> = withContext(Dispatchers.IO) {
        loadGalleryMedia(context, filter, sortOrder, limit, CancellationSignal())
    }

    fun loadGalleryMedia(
        context: Context,
        filter: GalleryMediaFilter = GalleryMediaFilter.ALL,
        sortOrder: GallerySortOrder = GallerySortOrder.DATE_DESC,
        limit: Int = 500,
        cancellationSignal: CancellationSignal? = null,
    ): List<GalleryMediaItem> {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = buildList {
            add(MediaStore.Files.FileColumns._ID)
            add(MediaStore.Files.FileColumns.DISPLAY_NAME)
            add(MediaStore.Files.FileColumns.SIZE)
            add(MediaStore.Files.FileColumns.DATE_ADDED)
            add(MediaStore.Files.FileColumns.MEDIA_TYPE)
            add(MediaStore.Files.FileColumns.MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Files.FileColumns.RELATIVE_PATH)
            }
            add(MediaStore.Video.VideoColumns.DURATION)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
        }.distinct().toTypedArray()

        val mediaType = MediaStore.Files.FileColumns.MEDIA_TYPE
        val selection = when (filter) {
            GalleryMediaFilter.ALL -> "$mediaType=? OR $mediaType=?"
            GalleryMediaFilter.IMAGES -> "$mediaType=?"
            GalleryMediaFilter.VIDEOS -> "$mediaType=?"
        }
        val args = when (filter) {
            GalleryMediaFilter.ALL -> arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            )
            GalleryMediaFilter.IMAGES -> arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            GalleryMediaFilter.VIDEOS -> arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        }
        val order = when (sortOrder) {
            GallerySortOrder.DATE_DESC -> MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
            GallerySortOrder.DATE_ASC -> MediaStore.Files.FileColumns.DATE_ADDED + " ASC"
            GallerySortOrder.SIZE_DESC -> MediaStore.Files.FileColumns.SIZE + " DESC"
            GallerySortOrder.SIZE_ASC -> MediaStore.Files.FileColumns.SIZE + " ASC"
            GallerySortOrder.NAME_ASC -> MediaStore.Files.FileColumns.DISPLAY_NAME + " COLLATE NOCASE ASC"
            GallerySortOrder.ALBUM -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.FileColumns.RELATIVE_PATH + " COLLATE NOCASE ASC"
            } else {
                MediaStore.Files.FileColumns.DISPLAY_NAME + " COLLATE NOCASE ASC"
            }
        }

        val result = ArrayList<GalleryMediaItem>()
        val cursor = query(
            context.contentResolver,
            collection,
            projection,
            selection,
            args,
            order,
            cancellationSignal,
        ) ?: return emptyList()

        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val typeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val mimeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val relativeIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                it.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
            } else -1
            val durationIndex = it.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
            val widthIndex = it.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightIndex = it.getColumnIndex(MediaStore.MediaColumns.HEIGHT)

            while (it.moveToNext() && result.size < limit) {
                val id = it.getLong(idIndex)
                val type = it.getInt(typeIndex)
                val itemUri = when (type) {
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    else -> ContentUris.withAppendedId(collection, id)
                }
                result += GalleryMediaItem(
                    id = id,
                    uri = itemUri,
                    name = it.getString(nameIndex).orEmpty(),
                    sizeBytes = it.getLong(sizeIndex).coerceAtLeast(0L),
                    dateTaken = it.getLong(dateIndex).coerceAtLeast(0L) * 1000L,
                    mimeType = it.getString(mimeIndex).orEmpty(),
                    album = if (relativeIndex >= 0) it.getString(relativeIndex).orEmpty() else "",
                    durationMs = if (durationIndex >= 0) it.getLong(durationIndex).coerceAtLeast(0L) else 0L,
                    width = if (widthIndex >= 0) it.getInt(widthIndex) else 0,
                    height = if (heightIndex >= 0) it.getInt(heightIndex) else 0,
                )
            }
        }
        return result
    }

    fun trashMediaFiles(contentResolver: ContentResolver, uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return MediaStore.createTrashRequest(contentResolver, uris, true)
    }

    private fun query(
        resolver: ContentResolver,
        uri: Uri,
        projection: Array<String>,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        cancellationSignal: CancellationSignal?,
    ): Cursor? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                resolver.query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal)
            } else {
                @Suppress("DEPRECATION")
                resolver.query(uri, projection, selection, selectionArgs, sortOrder)
            }
        }.getOrNull()
    }
}
