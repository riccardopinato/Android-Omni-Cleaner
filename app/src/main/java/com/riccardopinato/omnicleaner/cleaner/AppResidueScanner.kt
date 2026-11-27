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
    val inaccessibleRoots: List<String> = emptyList(),
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

        // Different storage locations are independent capabilities; inaccessible
        // Android/data or Android/obb is not proof that no app data exists there.
        val locations = listOf("Android/media", "Android/data", "Android/obb")
        val unavailable = ArrayList<String>()
        val candidates = ArrayList<File>()
        for (name in locations) {
            coroutineContext.ensureActive()
            val parent = File(root, name)
            val directories = runCatching { parent.listFiles() }.getOrNull()
            if (directories == null) {
                unavailable += name
                continue
            }
            candidates += directories.filter { it.isDirectory && !PathSafety.isSymlink(it) }
        }

        val results = ArrayList<AppResidueItem>()
        var index = 0
        val reported = HashSet<String>()
        for (directory in candidates) {
            coroutineContext.ensureActive()
            index++
            onProgress(index, directory)
            val packageName = directory.name
            if (!looksLikePackageName(packageName) || packageName in visiblePackages) continue

            // PackageManager visibility can hide installed or archived apps.
            // Query an individual package when permitted, but never regard a
            // NameNotFound as definitive proof that uninstall occurred.
            val knownInstalled = runCatching {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0) != null
            }.getOrDefault(false)
            if (knownInstalled) continue

            val canonical = runCatching { directory.canonicalFile }.getOrNull() ?: continue
            if (canonical != directory.absoluteFile || !reported.add(canonical.path)) continue
            val inventory = inventoryScanner.collect(canonical)
            if (inventory.unreadable > 0 && inventory.files.isEmpty()) continue
            val bytes = inventory.files.sumOf { it.size }
            results += AppResidueItem(
                file = directory,
                name = packageName,
                path = canonical.path,
                displayPath = canonical.path,
                sizeBytes = bytes,
                fileCount = inventory.files.size,
            )
        }

        AppResidueScan(
            items = results.sortedByDescending { it.sizeBytes },
            packageVisibilityMayBeRestricted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
            inaccessibleRoots = unavailable,
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
