package com.riccardopinato.omnicleaner.exif

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

data class ExifCandidate(
    val uri: Uri,
    val tree: Uri,
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

class FolderExifCleaner(
    private val context: Context,
) {
    private val resolver = context.contentResolver

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
            val root = DocumentFile.fromTreeUri(context, treeUri)
            if (root == null || !root.exists() || !root.isDirectory) {
                failures++
                continue
            }

            val stack = ArrayDeque<DocumentFile>()
            root.listFiles().forEach(stack::add)

            while (stack.isNotEmpty()) {
                coroutineContext.ensureActive()
                val document = stack.removeLast()

                if (document.isDirectory) {
                    if (recursive) {
                        runCatching { document.listFiles().forEach(stack::add) }
                            .onFailure { failures++ }
                    }
                    continue
                }

                if (!document.isFile || !looksLikeJpeg(document)) {
                    skipped++
                    continue
                }

                val count = runCatching { inspect(document.uri, groups) }
                    .getOrElse {
                        failures++
                        0
                    }

                if (count > 0) {
                    candidates += ExifCandidate(
                        uri = document.uri,
                        tree = treeUri,
                        name = document.name ?: "photo.jpg",
                        tagsFound = count,
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
        val destinationRoot = DocumentFile.fromTreeUri(context, destination)
            ?: return@withContext ExifExport(
                created = 0,
                failed = scan.candidates.size,
                incompleteCopies = emptyList(),
                errors = listOf("Destinazione non disponibile"),
            )

        var created = 0
        var failed = 0
        val incomplete = ArrayList<String>()
        val errors = ArrayList<String>()

        scan.candidates.forEachIndexed { index, candidate ->
            coroutineContext.ensureActive()
            onProgress(index, scan.candidates.size)

            var temp: File? = null
            try {
                val workingFile = File.createTempFile(
                    "omni-exif-",
                    ".jpg",
                    context.cacheDir,
                ).also { temp = it }

                val originalDigest = resolver.openInputStream(candidate.uri)?.use { input ->
                    FileOutputStream(workingFile).use { output ->
                        val digest = copyWithDigest(input, output)
                        output.fd.sync()
                        digest
                    }
                } ?: error("Documento non leggibile")

                val tempDigest = FileInputStream(workingFile).use { input ->
                    digestCancellable(input)
                }
                if (!originalDigest.contentEquals(tempDigest)) {
                    incomplete += candidate.name
                    onIncomplete(candidate.name)
                    error("Copia temporanea incompleta")
                }

                JpegExifEditor.cleanTemporaryCopy(
                    file = workingFile,
                    groups = scan.groups,
                    checkpoint = { coroutineContext.ensureActive() },
                )

                val outputName = cleanedName(candidate.name)
                val output = createUniqueJpeg(destinationRoot, outputName)
                    ?: error("Impossibile creare il file di destinazione")

                val cleanedDigest = resolver.openOutputStream(output.uri, "w")?.use { target ->
                    FileInputStream(workingFile).use { source ->
                        copyWithDigest(source, target)
                    }
                } ?: error("Impossibile scrivere la destinazione")

                // Re-open the provider output: the write-stream digest proves
                // what was sent, while this second digest proves what Android
                // actually persisted.
                val exportedDigest = resolver.openInputStream(output.uri)?.use { input ->
                    digestCancellable(input)
                } ?: error("Copia esportata non leggibile")

                if (!exportedDigest.contentEquals(cleanedDigest)) {
                    incomplete += candidate.name
                    onIncomplete(candidate.name)
                    runCatching { output.delete() }
                    error("Copia esportata incompleta")
                }

                created++
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                failed++
                errors += "${candidate.name}: ${error.message ?: error.javaClass.simpleName}"
            } finally {
                temp?.delete()
            }
        }

        onProgress(scan.candidates.size, scan.candidates.size)

        ExifExport(
            created = created,
            failed = failed,
            incompleteCopies = incomplete.distinct(),
            errors = errors,
        )
    }

    fun inspect(
        uri: Uri,
        groups: Set<ExifGroup>,
    ): Int {
        val selectedTags = ExifGroup.selectedTags(groups)
        if (selectedTags.isEmpty()) return 0

        resolver.openInputStream(uri)?.use { input ->
            val first = input.read()
            val second = input.read()
            if (first != 0xFF || second != 0xD8) {
                error("Non è un JPEG")
            }
        } ?: error("Documento non leggibile")

        val exif = resolver.openInputStream(uri)?.use(::ExifInterface)
            ?: error("Documento non leggibile")

        return selectedTags.count { tag -> exif.getAttribute(tag) != null }
    }

    fun persistTreePermission(uri: Uri) {
        runCatching {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    private fun looksLikeJpeg(document: DocumentFile): Boolean {
        val type = document.type.orEmpty()
        val name = document.name.orEmpty().lowercase()
        return type.equals("image/jpeg", ignoreCase = true) ||
            name.endsWith(".jpg") ||
            name.endsWith(".jpeg")
    }

    private fun cleanedName(original: String): String {
        val base = original.substringBeforeLast('.', original)
        return "${base}_clean.jpg"
    }

    private fun createUniqueJpeg(
        root: DocumentFile,
        desiredName: String,
    ): DocumentFile? {
        val base = desiredName.substringBeforeLast('.', desiredName)
        for (index in 0..999) {
            val name = if (index == 0) desiredName else "${base}_${index}.jpg"
            if (root.findFile(name) == null) {
                return root.createFile("image/jpeg", name)
            }
        }
        return null
    }

    private suspend fun copyWithDigest(
        input: InputStream,
        output: OutputStream,
    ): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            coroutineContext.ensureActive()
            val read = input.read(buffer)
            if (read <= 0) break
            output.write(buffer, 0, read)
            md.update(buffer, 0, read)
        }
        output.flush()
        return md.digest()
    }

    private suspend fun digestCancellable(input: InputStream): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            coroutineContext.ensureActive()
            val read = input.read(buffer)
            if (read <= 0) break
            md.update(buffer, 0, read)
        }
        return md.digest()
    }
}
