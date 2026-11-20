package com.riccardopinato.omnicleaner.cleaner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Independent, GPL-free source enumeration designed from platform APIs.
 * The primary scanner remains the authoritative source for filesystem cleanup.
 * These sources are read-only discovery; SAF documents are NEVER implicitly
 * converted to a raw filesystem path or passed to a raw-file deleter.
 */
internal enum class OmniScanSourceKind {
    PRIMARY_FILES, MEDIASTORE, SAF_TREE, SECONDARY_VOLUME,
}

internal enum class OmniScanCoverageStatus { COMPLETE, PARTIAL, DENIED, UNAVAILABLE }

internal data class OmniDiscoveredMedia(
    val sourceId: String,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val modifiedMillis: Long,
    val mimeType: String?,
)

internal data class OmniScanCoverageSource(
    val id: String,
    val kind: OmniScanSourceKind,
    val label: String,
    val status: OmniScanCoverageStatus,
    val scannedCount: Int,
    val unreadable: Int,
    val detail: String,
)

internal data class OmniScanCoverageResult(
    val sources: List<OmniScanCoverageSource>,
    val visibleResources: List<OmniDiscoveredMedia>,
) {
    val accessibleSourceCount: Int get() =
        sources.count { it.status == OmniScanCoverageStatus.COMPLETE || it.status == OmniScanCoverageStatus.PARTIAL }
    val excludedSourceCount: Int get() =
        sources.count { it.status == OmniScanCoverageStatus.DENIED || it.status == OmniScanCoverageStatus.UNAVAILABLE }
}

internal fun omniUniqueUris(items: List<OmniDiscoveredMedia>): List<OmniDiscoveredMedia> =
    items.distinctBy { it.uri.toString() }

internal class OmniSourceCoverageScanner {
    suspend fun discover(context: Context, maxVisiblePerSource: Int = 400, nameQuery: String = ""): OmniScanCoverageResult =
        withContext(Dispatchers.IO) {
            val sources = ArrayList<OmniScanCoverageSource>()
            val resources = ArrayList<OmniDiscoveredMedia>()
            val cap = maxVisiblePerSource.coerceIn(10, 2000)
            val needle = nameQuery.trim().take(120)

            val primary = StorageAccess.primaryRoot()
            try {
                if (!StorageAccess.hasBroadAccess(context)) {
                    sources += OmniScanCoverageSource(
                        "primary", OmniScanSourceKind.PRIMARY_FILES, "Memoria condivisa",
                        OmniScanCoverageStatus.DENIED, 0, 0,
                        "Accesso completo ai file non autorizzato; gli altri metodi restano indipendenti.",
                    )
                } else {
                    val snapshot = StorageInventoryRepository.snapshot(primary)
                    val inventory = snapshot.inventory
                    sources += OmniScanCoverageSource(
                        "primary", OmniScanSourceKind.PRIMARY_FILES, "Memoria condivisa",
                        if (inventory.unreadable > 0 || inventory.excludedDirectories > 0) OmniScanCoverageStatus.PARTIAL
                        else OmniScanCoverageStatus.COMPLETE,
                        inventory.files.size, inventory.unreadable,
                        "File indicizzati: ${inventory.files.size}; esclusi: ${inventory.excludedDirectories}. " +
                            "Le cartelle protette non sono state lette.",
                    )
                    // Existing primary index already powers Storage Intelligence search.
                    // Do not double-count it as additional accessible media here.
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                sources += OmniScanCoverageSource(
                    "primary", OmniScanSourceKind.PRIMARY_FILES, "Memoria condivisa",
                    OmniScanCoverageStatus.DENIED, 0, 1, "Accesso al filesystem negato o non disponibile.",
                )
            }

            val mediaVolumes: Set<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { MediaStore.getExternalVolumeNames(context) }.getOrDefault(emptySet())
            } else setOf("external")
            for (volume in mediaVolumes.sorted()) {
                coroutineContext.ensureActive()
                val id = "media:$volume"
                val base = MediaStore.Files.getContentUri(volume)
                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.MIME_TYPE,
                )
                val baseWhere = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    "${MediaStore.MediaColumns.IS_TRASHED}=0 AND ${MediaStore.MediaColumns.IS_PENDING}=0"
                } else null
                val where = listOfNotNull(
                    baseWhere,
                    if (needle.isNotEmpty()) "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?" else null,
                ).joinToString(" AND ").ifBlank { null }
                val whereArgs = if (needle.isNotEmpty()) arrayOf("%$needle%") else null
                try {
                    var checked = 0
                    context.contentResolver.query(
                        base, projection, where, whereArgs, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
                    )?.use { cursor ->
                        val ixId = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                        val ixName = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                        val ixSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                        val ixDate = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                        val ixMime = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                        while (cursor.moveToNext()) {
                            coroutineContext.ensureActive()
                            checked++
                            if (checked <= cap) {
                                resources += OmniDiscoveredMedia(
                                    id,
                                    ContentUris.withAppendedId(base, cursor.getLong(ixId)),
                                    cursor.getString(ixName) ?: "Media",
                                    cursor.getLong(ixSize).coerceAtLeast(0L),
                                    cursor.getLong(ixDate).coerceAtLeast(0L) * 1000L,
                                    cursor.getString(ixMime),
                                )
                            } else break
                        }
                    }
                    sources += OmniScanCoverageSource(
                        id, OmniScanSourceKind.MEDIASTORE, "MediaStore · $volume",
                        if (checked > cap) OmniScanCoverageStatus.PARTIAL else OmniScanCoverageStatus.COMPLETE,
                        minOf(checked, cap), 0,
                        if (checked > cap) "Anteprima dei primi $cap media; il catalogo contiene altri risultati."
                        else "Risultati leggibili mediante permessi MediaStore disponibili.",
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: SecurityException) {
                    sources += OmniScanCoverageSource(id, OmniScanSourceKind.MEDIASTORE, "MediaStore · $volume",
                        OmniScanCoverageStatus.DENIED, 0, 0, "Autorizzazione media non disponibile.")
                } catch (_: Exception) {
                    sources += OmniScanCoverageSource(id, OmniScanSourceKind.MEDIASTORE, "MediaStore · $volume",
                        OmniScanCoverageStatus.PARTIAL, 0, 1, "Sorgente momentaneamente non leggibile.")
                }
            }

            val treePermissions = context.contentResolver.persistedUriPermissions.filter { it.isReadPermission }
            for (permission in treePermissions) {
                coroutineContext.ensureActive()
                val id = "saf:${permission.uri}"
                var found = 0
                var unreadable = 0
                try {
                    val root = DocumentFile.fromTreeUri(context, permission.uri)
                    val stack = ArrayDeque<DocumentFile>()
                    if (root != null) stack.add(root)
                    while (stack.isNotEmpty() && found < cap) {
                        coroutineContext.ensureActive()
                        val folder = stack.removeLast()
                        val children = try { folder.listFiles() }
                        catch (_: Exception) { unreadable++; continue }
                        for (child in children) {
                            coroutineContext.ensureActive()
                            if (child.isDirectory) {
                                stack.add(child)
                            } else if (child.isFile &&
                                (needle.isEmpty() || child.name.orEmpty().contains(needle, ignoreCase = true))
                            ) {
                                found++
                                if (found <= cap) resources += OmniDiscoveredMedia(
                                    id, child.uri, child.name ?: "Documento",
                                    child.length().coerceAtLeast(0L),
                                    child.lastModified().coerceAtLeast(0L), child.type,
                                )
                            }
                            if (found >= cap) break
                        }
                    }
                    sources += OmniScanCoverageSource(
                        id, OmniScanSourceKind.SAF_TREE, "Cartella SAF autorizzata",
                        if (stack.isNotEmpty() || unreadable > 0) OmniScanCoverageStatus.PARTIAL
                        else OmniScanCoverageStatus.COMPLETE,
                        found, unreadable,
                        "Sorgente concessa dall'utente. Nessuna eliminazione automatica dei documenti.",
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    sources += OmniScanCoverageSource(id, OmniScanSourceKind.SAF_TREE, "Cartella SAF autorizzata",
                        OmniScanCoverageStatus.DENIED, found, unreadable + 1,
                        "Permesso revocato o provider non disponibile.")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val manager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                for (volume in manager.storageVolumes.filter { !it.isPrimary }) {
                    coroutineContext.ensureActive()
                    val path = volume.directory
                    val id = "volume:${volume.uuid ?: volume.getDescription(context)}"
                    if (path == null || !path.isDirectory) {
                        sources += OmniScanCoverageSource(id, OmniScanSourceKind.SECONDARY_VOLUME,
                            volume.getDescription(context), OmniScanCoverageStatus.UNAVAILABLE,
                            0, 0, "Volume smontato o non leggibile.")
                        continue
                    }
                    var count = 0
                    var unreadable = 0
                    val visited = HashSet<String>()
                    val stack = ArrayDeque<File>()
                    stack.add(path)
                    while (stack.isNotEmpty() && count < cap) {
                        coroutineContext.ensureActive()
                        val folder = stack.removeLast()
                        val canonical = runCatching { folder.canonicalPath }.getOrNull()
                        if (canonical == null || !visited.add(canonical)) continue
                        if (folder != path && FileInventoryPolicy.shouldIgnore(path, folder)) continue
                        val children = folder.listFiles()
                        if (children == null) { unreadable++; continue }
                        for (child in children) {
                            coroutineContext.ensureActive()
                            if (child.isDirectory) stack.add(child)
                            else if (child.isFile &&
                                (needle.isEmpty() || child.name.contains(needle, ignoreCase = true))
                            ) {
                                count++
                                if (count <= cap) resources += OmniDiscoveredMedia(
                                    id, Uri.fromFile(child), child.name,
                                    child.length().coerceAtLeast(0L),
                                    child.lastModified().coerceAtLeast(0L), null,
                                )
                            }
                            if (count >= cap) break
                        }
                    }
                    sources += OmniScanCoverageSource(id, OmniScanSourceKind.SECONDARY_VOLUME,
                        volume.getDescription(context),
                        if (stack.isNotEmpty() || unreadable > 0) OmniScanCoverageStatus.PARTIAL
                        else OmniScanCoverageStatus.COMPLETE,
                        count, unreadable,
                        if (stack.isNotEmpty()) "Anteprima parziale: altri file non ancora indicizzati."
                        else "File accessibili nella memoria esterna.",
                    )
                }
            }
            OmniScanCoverageResult(sources, omniUniqueUris(resources))
        }
}
