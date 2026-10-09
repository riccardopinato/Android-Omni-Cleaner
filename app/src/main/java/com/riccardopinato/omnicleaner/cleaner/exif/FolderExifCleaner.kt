package com.riccardopinato.omnicleaner.cleaner.exif

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class ExifCandidate(
    val tree: Uri,
    val uri: Uri,
    val name: String,
    val tagsFound: Int,
)

data class ExifScan(
    val candidates: List<ExifCandidate>,
    val skipped: Int,
    val failures: Int,
    val roots: List<Uri>,
    val recursive: Boolean,
    val groups: Set<ExifGroup>,
)

data class ExifExport(
    val created: Int,
    val failed: Int,
    val incompleteCopies: List<String>,
    val errors: List<String>,
)

class FolderExifCleaner(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    suspend fun scan(
        trees: List<Uri>,
        recursive: Boolean,
        groups: Set<ExifGroup>,
    ): ExifScan = withContext(Dispatchers.IO) {
        val candidates = ArrayList<ExifCandidate>()
        var skipped = 0
        var failures = 0

        for (treeUri in trees.distinct()) {
            coroutineContext.ensureActive()
            val root = DocumentFile.fromTreeUri(appContext, treeUri)
            if (root == null || !root.exists()) {
                failures++
                continue
            }

            val queue = ArrayDeque<DocumentFile>()
            queue += root
            while (queue.isNotEmpty()) {
                coroutineContext.ensureActive()
                val current = queue.removeLast()
                if (current.isDirectory) {
                    if (current != root && !recursive) continue
                    runCatching { current.listFiles().toList() }
                        .onSuccess { children -> children.forEach(queue::add) }
                        .onFailure { failures++ }
                    continue
                }

                if (!current.isFile || !isJpeg(current)) {
                    skipped++
                    continue
                }

                val found = runCatching { inspect(current.uri, groups) }
                    .getOrElse {
                        failures++
                        0
                    }
                if (found > 0) {
                    candidates += ExifCandidate(
                        tree = treeUri,
                        uri = current.uri,
                        name = current.name ?: "image.jpg",
                        tagsFound = found,
                    )
                }
            }
        }

        ExifScan(
            candidates = candidates,
            skipped = skipped,
            failures = failures,
            roots = trees.distinct(),
            recursive = recursive,
            groups = groups,
        )
    }

    suspend fun export(
        scan: ExifScan,
        destination: Uri,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onIncomplete: (String) -> Unit = {},
    ): ExifExport = withContext(Dispatchers.IO) {
        val destinationTree = DocumentFile.fromTreeUri(appContext, destination)
            ?: return@withContext ExifExport(
                created = 0,
                failed = scan.candidates.size,
                incompleteCopies = emptyList(),
                errors = listOf("Destinazione non accessibile"),
            )

        var created = 0
        var failed = 0
        val incomplete = ArrayList<String>()
        val errors = ArrayList<String>()
        val tempRoot = File(appContext.cacheDir, "exif-cleaner").apply { mkdirs() }
        val activeJob = coroutineContext[Job]

        scan.candidates.forEachIndexed { index, candidate ->
            coroutineContext.ensureActive()
            onProgress(index, scan.candidates.size)
            val safeName = candidate.name.ifBlank { "image.jpg" }
            val temp = File.createTempFile("exif_", ".jpg", tempRoot)

            try {
                val originalDigest = resolver.openInputStream(candidate.uri)?.use {
                    JpegContentDigest.read(it) { activeJob?.ensureActive() }
                } ?: error("Impossibile leggere " + safeName)

                resolver.openInputStream(candidate.uri)?.use { input ->
                    temp.outputStream().buffered().use { output -> transfer(input, output) }
                } ?: error("Impossibile copiare " + safeName)

                JpegExifEditor.cleanTemporaryCopy(temp, scan.groups) {
                    activeJob?.ensureActive()
                }

                val cleanedDigest = temp.inputStream().buffered().use {
                    JpegContentDigest.read(it) { activeJob?.ensureActive() }
                }
                if (!originalDigest.contentEquals(cleanedDigest)) {
                    incomplete += safeName
                    onIncomplete(safeName)
                    return@forEachIndexed
                }

                val outputName = "clean_" + safeName
                val target = destinationTree.createFile("image/jpeg", outputName)
                    ?: error("Impossibile creare " + outputName)
                val output = resolver.openOutputStream(target.uri, "w")
                    ?: error("Impossibile aprire " + outputName)
                temp.inputStream().buffered().use { input ->
                    output.buffered().use { destinationStream ->
                        transfer(input, destinationStream)
                    }
                }
                created++
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                failed++
                errors += candidate.name + ": " + (error.message ?: error.javaClass.simpleName)
            } finally {
                temp.delete()
            }
        }

        onProgress(scan.candidates.size, scan.candidates.size)
        ExifExport(created, failed, incomplete, errors)
    }

    private fun inspect(uri: Uri, groups: Set<ExifGroup>): Int {
        val input = resolver.openInputStream(uri) ?: return 0
        return input.use {
            val exif = ExifInterface(it)
            ExifGroup.allTags(groups).count { tag -> exif.getAttribute(tag) != null }
        }
    }

    private fun isJpeg(file: DocumentFile): Boolean {
        val type = file.type.orEmpty().lowercase()
        val name = file.name.orEmpty().lowercase()
        return type == "image/jpeg" || name.endsWith(".jpg") || name.endsWith(".jpeg")
    }

    private suspend fun transfer(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(64 * 1024)
        while (true) {
            coroutineContext.ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
        }
        output.flush()
    }
}
