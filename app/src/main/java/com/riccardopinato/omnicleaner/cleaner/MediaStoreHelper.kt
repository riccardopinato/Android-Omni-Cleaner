package com.riccardopinato.omnicleaner.cleaner

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore
import com.riccardopinato.omnicleaner.gallery.GalleryMediaFilter
import com.riccardopinato.omnicleaner.gallery.GalleryMediaItem
import com.riccardopinato.omnicleaner.gallery.GallerySortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale

object MediaStoreHelper {
    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp", "svg",
    )
    private val videoExtensions = setOf("mp4", "mkv", "mov", "webm", "3gp", "avi")
    private val audioExtensions = setOf("mp3", "wav", "ogg", "m4a", "flac", "aac")
    private val mediaExtensions = imageExtensions + videoExtensions + audioExtensions

    fun isMediaFile(file: File): Boolean =
        file.extension.lowercase(Locale.ROOT) in mediaExtensions

    fun getMediaUri(context: Context, file: File): Uri? {
        if (!file.exists()) return null
        val extension = file.extension.lowercase(Locale.ROOT)
        val collection = when {
            extension in imageExtensions -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            extension in videoExtensions -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            extension in audioExtensions -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            else -> MediaStore.Files.getContentUri("external")
        }

        return context.contentResolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DATA} = ?",
            arrayOf(file.absolutePath),
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            ContentUris.withAppendedId(collection, id)
        }
    }

    fun loadGalleryMedia(
        context: Context,
        filter: GalleryMediaFilter = GalleryMediaFilter.ALL,
        sortOrder: GallerySortOrder = GallerySortOrder.DATE_DESC,
        limit: Int = 300,
        offset: Int = 0,
        cancellationSignal: CancellationSignal = CancellationSignal(),
    ): List<GalleryMediaItem> {
        cancellationSignal.throwIfCanceled()
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")

        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.Images.ImageColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.Files.FileColumns.MEDIA_TYPE)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
                add(MediaStore.Video.VideoColumns.DURATION)
            }
        }.toTypedArray()

        val (baseSelection, selectionArgs) = when (filter) {
            GalleryMediaFilter.ALL ->
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)" to
                    arrayOf(
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                    )
            GalleryMediaFilter.IMAGES ->
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?" to
                    arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            GalleryMediaFilter.VIDEOS ->
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?" to
                    arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        }

        val selection = buildString {
            append("(")
            append(baseSelection)
            append(")")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                append(" AND ${MediaStore.MediaColumns.IS_PENDING} = 0")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                append(" AND ${MediaStore.MediaColumns.IS_TRASHED} = 0")
            }
        }

        val order = when (sortOrder) {
            GallerySortOrder.DATE_DESC ->
                "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC, ${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            GallerySortOrder.DATE_ASC ->
                "${MediaStore.Images.ImageColumns.DATE_TAKEN} ASC, ${MediaStore.MediaColumns.DATE_MODIFIED} ASC"
            GallerySortOrder.SIZE_DESC -> "${MediaStore.MediaColumns.SIZE} DESC"
            GallerySortOrder.SIZE_ASC -> "${MediaStore.MediaColumns.SIZE} ASC"
            GallerySortOrder.NAME_ASC -> "${MediaStore.MediaColumns.DISPLAY_NAME} COLLATE NOCASE ASC"
            GallerySortOrder.ALBUM -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME} COLLATE NOCASE ASC, ${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC"
            } else {
                "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC"
            }
        } + ", ${MediaStore.MediaColumns._ID} ASC"

        val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, order)
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit.coerceAtLeast(1))
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset.coerceAtLeast(0))
            }
            resolver.query(collection, projection, queryArgs, cancellationSignal)
        } else {
            resolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "$order LIMIT ${limit.coerceAtLeast(1)} OFFSET ${offset.coerceAtLeast(0)}",
                cancellationSignal,
            )
        } ?: throw IOException("MediaStore non disponibile")

        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val takenIndex = it.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN)
            val modifiedIndex = it.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            val mimeIndex = it.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val typeIndex = it.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val widthIndex = it.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightIndex = it.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val albumIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                it.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
            } else {
                -1
            }
            val durationIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                it.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
            } else {
                -1
            }

            val result = ArrayList<GalleryMediaItem>(minOf(limit, it.count))
            while (it.moveToNext() && result.size < limit) {
                cancellationSignal.throwIfCanceled()

                val id = it.getLong(idIndex)
                val mediaType = if (typeIndex >= 0) it.getInt(typeIndex) else 0
                val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val name = nameIndex.takeIf { index -> index >= 0 }
                    ?.let(it::getString)
                    .orEmpty()
                    .ifBlank { "Media_$id" }
                val size = sizeIndex.takeIf { index -> index >= 0 }
                    ?.let(it::getLong)
                    ?.coerceAtLeast(0L) ?: 0L
                val taken = takenIndex.takeIf { index -> index >= 0 }
                    ?.let(it::getLong)
                    ?.takeIf { value -> value > 0L }
                    ?: modifiedIndex.takeIf { index -> index >= 0 }
                        ?.let(it::getLong)
                        ?.times(1000L)
                        ?: 0L
                val mime = mimeIndex.takeIf { index -> index >= 0 }
                    ?.let(it::getString)
                    .orEmpty()
                    .ifBlank { if (isVideo) "video/mp4" else "image/jpeg" }
                val album = albumIndex.takeIf { index -> index >= 0 }?.let(it::getString)
                val duration = if (isVideo && durationIndex >= 0) it.getLong(durationIndex) else 0L
                val width = widthIndex.takeIf { index -> index >= 0 }?.let(it::getInt) ?: 0
                val height = heightIndex.takeIf { index -> index >= 0 }?.let(it::getInt) ?: 0

                val baseUri = if (isVideo) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    } else {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                }

                result += GalleryMediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(baseUri, id),
                    name = name,
                    sizeBytes = size,
                    dateTaken = taken,
                    mimeType = mime,
                    album = album,
                    durationMs = duration,
                    width = width,
                    height = height,
                )
            }
            return result
        }
    }

    suspend fun loadGalleryMediaCancellable(
        context: Context,
        filter: GalleryMediaFilter = GalleryMediaFilter.ALL,
        sortOrder: GallerySortOrder = GallerySortOrder.DATE_DESC,
        limit: Int = 300,
        offset: Int = 0,
    ): List<GalleryMediaItem> = withContext(Dispatchers.IO) {
        loadGalleryMedia(
            context = context,
            filter = filter,
            sortOrder = sortOrder,
            limit = limit,
            offset = offset,
            cancellationSignal = CancellationSignal(),
        )
    }

    fun trashMediaFiles(
        resolver: ContentResolver,
        uris: List<Uri>,
    ): PendingIntent? {
        if (uris.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return MediaStore.createTrashRequest(resolver, uris, true)
    }
}
