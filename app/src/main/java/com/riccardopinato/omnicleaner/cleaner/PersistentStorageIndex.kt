package com.riccardopinato.omnicleaner.cleaner

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.util.Locale

interface DuplicateHashCache {
    fun sampleHash(item: IndexedFile): String?
    fun fullHash(item: IndexedFile): String?
    fun putSampleHash(item: IndexedFile, hash: String)
    fun putFullHash(item: IndexedFile, hash: String)
}

internal data class PersistentInventorySnapshot(
    val inventory: FileInventory,
    val createdAtMillis: Long,
    val mediaStoreGeneration: Long?,
)

internal class PersistentStorageIndex(
    context: Context,
    databaseName: String = DATABASE_NAME,
) : DuplicateHashCache {
    private val appContext = context.applicationContext
    private val helper = StorageIndexOpenHelper(appContext, databaseName)
    private val statePreferences =
        appContext.getSharedPreferences(
            INDEX_STATE_PREFERENCES_PREFIX + databaseName,
            Context.MODE_PRIVATE,
        )
    private val lock = Any()

    fun loadReusableSnapshot(
        root: File,
        nowMillis: Long,
        maxAgeMillis: Long,
    ): PersistentInventorySnapshot? = synchronized(lock) {
        if (statePreferences.getBoolean(KEY_DIRTY, false)) {
            return@synchronized null
        }
        val db = helper.readableDatabase
        val storedRoot = meta(db, META_ROOT_PATH) ?: return@synchronized null
        val rootPath = canonical(root)
        if (storedRoot != rootPath) return@synchronized null

        val completedAt = meta(db, META_LAST_COMPLETE_SCAN_AT)?.toLongOrNull()
            ?: return@synchronized null
        if (completedAt <= 0L || nowMillis < completedAt) return@synchronized null
        if (nowMillis - completedAt > maxAgeMillis) return@synchronized null

        val storedMediaGeneration = meta(db, META_MEDIASTORE_GENERATION)?.toLongOrNull()
        val currentMediaGeneration = currentMediaStoreGeneration()
        if (
            storedMediaGeneration != null &&
            currentMediaGeneration != null &&
            storedMediaGeneration != currentMediaGeneration
        ) {
            return@synchronized null
        }

        val files = ArrayList<IndexedFile>()
        db.query(
            TABLE_FILES,
            arrayOf(COL_PATH, COL_SIZE, COL_MODIFIED),
            null,
            null,
            null,
            null,
            null,
        ).use { cursor ->
            val pathIndex = cursor.getColumnIndexOrThrow(COL_PATH)
            val sizeIndex = cursor.getColumnIndexOrThrow(COL_SIZE)
            val modifiedIndex = cursor.getColumnIndexOrThrow(COL_MODIFIED)
            while (cursor.moveToNext()) {
                files += IndexedFile(
                    file = File(cursor.getString(pathIndex)),
                    size = cursor.getLong(sizeIndex).coerceAtLeast(0L),
                    modified = cursor.getLong(modifiedIndex).coerceAtLeast(0L),
                )
            }
        }

        PersistentInventorySnapshot(
            inventory = FileInventory(
                files = files,
                unreadable = meta(db, META_UNREADABLE)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                excludedDirectories =
                    meta(db, META_EXCLUDED)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            ),
            createdAtMillis = completedAt,
            mediaStoreGeneration = storedMediaGeneration,
        )
    }

    fun reconcileFull(
        root: File,
        inventory: FileInventory,
        completedAtMillis: Long,
    ) = synchronized(lock) {
        val db = helper.writableDatabase
        val rootPath = canonical(root)
        db.beginTransaction()
        try {
            val previousRoot = meta(db, META_ROOT_PATH)
            if (previousRoot != null && previousRoot != rootPath) {
                db.delete(TABLE_FILES, null, null)
            }

            val generation = (meta(db, META_SCAN_GENERATION)?.toLongOrNull() ?: 0L) + 1L

            for (item in inventory.files) {
                val path = item.file.absolutePath
                val extension = item.file.extension.lowercase(Locale.ROOT)
                val seenOnly = ContentValues().apply {
                    put(COL_EXTENSION, extension)
                    put(COL_LAST_SEEN_GENERATION, generation)
                }
                val unchanged = db.update(
                    TABLE_FILES,
                    seenOnly,
                    "$COL_PATH = ? AND $COL_SIZE = ? AND $COL_MODIFIED = ?",
                    arrayOf(path, item.size.toString(), item.modified.toString()),
                )

                if (unchanged == 0) {
                    val values = ContentValues().apply {
                        put(COL_PATH, path)
                        put(COL_SIZE, item.size)
                        put(COL_MODIFIED, item.modified)
                        put(COL_EXTENSION, extension)
                        putNull(COL_SAMPLE_HASH)
                        putNull(COL_FULL_HASH)
                        put(COL_LAST_SEEN_GENERATION, generation)
                        put(COL_LAST_VERIFIED_AT, completedAtMillis)
                    }
                    db.insertWithOnConflict(
                        TABLE_FILES,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE,
                    )
                }
            }

            db.delete(
                TABLE_FILES,
                "$COL_LAST_SEEN_GENERATION != ?",
                arrayOf(generation.toString()),
            )

            putMeta(db, META_ROOT_PATH, rootPath)
            putMeta(db, META_SCAN_GENERATION, generation.toString())
            putMeta(db, META_LAST_COMPLETE_SCAN_AT, completedAtMillis.toString())
            putMeta(db, META_UNREADABLE, inventory.unreadable.toString())
            putMeta(db, META_EXCLUDED, inventory.excludedDirectories.toString())
            currentMediaStoreGeneration()?.let {
                putMeta(db, META_MEDIASTORE_GENERATION, it.toString())
            } ?: db.delete(TABLE_META, "$COL_META_KEY = ?", arrayOf(META_MEDIASTORE_GENERATION))

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        statePreferences.edit().putBoolean(KEY_DIRTY, false).apply()
    }

    fun markStale() {
        statePreferences.edit().putBoolean(KEY_DIRTY, true).apply()
    }

    override fun sampleHash(item: IndexedFile): String? =
        boundHash(item, COL_SAMPLE_HASH)

    override fun fullHash(item: IndexedFile): String? =
        boundHash(item, COL_FULL_HASH)

    override fun putSampleHash(item: IndexedFile, hash: String) {
        putBoundHash(item, COL_SAMPLE_HASH, hash)
    }

    override fun putFullHash(item: IndexedFile, hash: String) {
        putBoundHash(item, COL_FULL_HASH, hash)
    }

    private fun boundHash(item: IndexedFile, column: String): String? = synchronized(lock) {
        helper.readableDatabase.query(
            TABLE_FILES,
            arrayOf(column),
            "$COL_PATH = ? AND $COL_SIZE = ? AND $COL_MODIFIED = ?",
            arrayOf(item.file.absolutePath, item.size.toString(), item.modified.toString()),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (!cursor.moveToFirst() || cursor.isNull(0)) null
            else cursor.getString(0)?.takeIf { it.isNotBlank() }
        }
    }

    private fun putBoundHash(
        item: IndexedFile,
        column: String,
        hash: String,
    ) = synchronized(lock) {
        if (hash.length != 64) return@synchronized
        val values = ContentValues().apply {
            put(column, hash)
            put(COL_LAST_VERIFIED_AT, System.currentTimeMillis())
        }
        helper.writableDatabase.update(
            TABLE_FILES,
            values,
            "$COL_PATH = ? AND $COL_SIZE = ? AND $COL_MODIFIED = ?",
            arrayOf(item.file.absolutePath, item.size.toString(), item.modified.toString()),
        )
    }

    private fun currentMediaStoreGeneration(): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching {
            MediaStore.getGeneration(appContext, MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }.getOrNull()
    }

    private fun meta(db: SQLiteDatabase, key: String): String? =
        db.query(
            TABLE_META,
            arrayOf(COL_META_VALUE),
            "$COL_META_KEY = ?",
            arrayOf(key),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private fun putMeta(db: SQLiteDatabase, key: String, value: String) {
        val values = ContentValues().apply {
            put(COL_META_KEY, key)
            put(COL_META_VALUE, value)
        }
        db.insertWithOnConflict(
            TABLE_META,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private fun canonical(file: File): String =
        runCatching { file.canonicalPath }.getOrElse { file.absolutePath }

    private class StorageIndexOpenHelper(
        context: Context,
        databaseName: String,
    ) : SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE_FILES (
                    $COL_PATH TEXT PRIMARY KEY NOT NULL,
                    $COL_SIZE INTEGER NOT NULL,
                    $COL_MODIFIED INTEGER NOT NULL,
                    $COL_EXTENSION TEXT NOT NULL,
                    $COL_SAMPLE_HASH TEXT,
                    $COL_FULL_HASH TEXT,
                    $COL_LAST_SEEN_GENERATION INTEGER NOT NULL,
                    $COL_LAST_VERIFIED_AT INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE $TABLE_META (
                    $COL_META_KEY TEXT PRIMARY KEY NOT NULL,
                    $COL_META_VALUE TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX idx_storage_files_size ON $TABLE_FILES($COL_SIZE)",
            )
            db.execSQL(
                "CREATE INDEX idx_storage_files_extension ON $TABLE_FILES($COL_EXTENSION)",
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // v1 is the initial persistent-index schema. Future versions must
            // add explicit migrations rather than silently deleting the index.
            if (oldVersion != newVersion) {
                throw IllegalStateException(
                    "Storage index migration required: $oldVersion -> $newVersion",
                )
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "storage_index.db"
        const val DATABASE_VERSION = 1
        const val INDEX_STATE_PREFERENCES_PREFIX = "storage_index_state_"
        const val KEY_DIRTY = "index_dirty"

        const val TABLE_FILES = "indexed_files"
        const val TABLE_META = "index_meta"

        const val COL_PATH = "path"
        const val COL_SIZE = "size_bytes"
        const val COL_MODIFIED = "modified_at"
        const val COL_EXTENSION = "extension"
        const val COL_SAMPLE_HASH = "sample_hash"
        const val COL_FULL_HASH = "full_hash"
        const val COL_LAST_SEEN_GENERATION = "last_seen_generation"
        const val COL_LAST_VERIFIED_AT = "last_verified_at"

        const val COL_META_KEY = "meta_key"
        const val COL_META_VALUE = "meta_value"

        const val META_ROOT_PATH = "root_path"
        const val META_SCAN_GENERATION = "scan_generation"
        const val META_LAST_COMPLETE_SCAN_AT = "last_complete_scan_at"
        const val META_MEDIASTORE_GENERATION = "mediastore_generation"
        const val META_UNREADABLE = "unreadable"
        const val META_EXCLUDED = "excluded"
    }
}
