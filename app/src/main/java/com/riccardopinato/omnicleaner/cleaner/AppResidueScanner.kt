package com.riccardopinato.omnicleaner.cleaner

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

data class AppResidueScan(
    val items: List<AppResidueItem>,
    val packageVisibilityMayBeRestricted: Boolean,
)

class AppResidueScanner(
    private val inventoryScanner: FileInventoryScanner = FileInventoryScanner(),
) {
    suspend fun scan(
        context: Context,
        root: File,
        onProgress: (Int, File) -> Unit = { _, _ -> },
    ): AppResidueScan = withContext(Dispatchers.IO) {
        val visiblePackages = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getInstalledApplications(0)
                .mapTo(hashSetOf()) { it.packageName }
        }.getOrDefault(emptySet())

        val mediaRoot = File(root, "Android/media")
        val children = runCatching { mediaRoot.listFiles() }.getOrNull()
            ?.filter { it.isDirectory }
            .orEmpty()

        val results = ArrayList<AppResidueItem>()
        var index = 0
        for (directory in children) {
            coroutineContext.ensureActive()
            index++
            onProgress(index, directory)

            val packageName = directory.name
            if (!looksLikePackageName(packageName) || packageName in visiblePackages) continue

            val inventory = inventoryScanner.collect(directory)
            if (inventory.unreadable > 0 && inventory.files.isEmpty()) continue
            val bytes = inventory.files.sumOf { it.size }
            val path = directory.absolutePath
            results += AppResidueItem(
                file = directory,
                name = packageName,
                path = path,
                displayPath = path,
                sizeBytes = bytes,
                fileCount = inventory.files.size,
            )
        }

        AppResidueScan(
            items = results.sortedByDescending { it.sizeBytes },
            packageVisibilityMayBeRestricted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        )
    }

    private fun looksLikePackageName(value: String): Boolean =
        value.length in 3..255 &&
            !value.startsWith(".") &&
            value.contains('.') &&
            value.split('.').all { segment ->
                segment.isNotBlank() &&
                    segment.first().let { it.isLetter() || it == '_' } &&
                    segment.all { it.isLetterOrDigit() || it == '_' }
            }
}
