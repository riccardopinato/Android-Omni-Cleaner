package com.riccardopinato.omnicleaner.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.riccardopinato.omnicleaner.cleaner.FileScanner
import com.riccardopinato.omnicleaner.cleaner.GalleryPreferencesManager
import com.riccardopinato.omnicleaner.cleaner.MediaStoreHelper
import com.riccardopinato.omnicleaner.cleaner.PermissionHealth
import com.riccardopinato.omnicleaner.cleaner.ProPreferencesManager
import com.riccardopinato.omnicleaner.cleaner.SafeTrashManager
import com.riccardopinato.omnicleaner.cleaner.SpaceOverviewScanner
import com.riccardopinato.omnicleaner.cleaner.StorageCapabilityHealth
import com.riccardopinato.omnicleaner.cleaner.ThumbnailFileCleanup
import com.riccardopinato.omnicleaner.cleaner.exif.ExifGroup
import com.riccardopinato.omnicleaner.cleaner.exif.FolderExifCleaner
import com.riccardopinato.omnicleaner.model.BigFileSizeFilter
import com.riccardopinato.omnicleaner.model.DuplicateFile
import com.riccardopinato.omnicleaner.model.FolderSubTab
import com.riccardopinato.omnicleaner.model.GalleryMediaFilter
import com.riccardopinato.omnicleaner.model.GallerySortOrder
import com.riccardopinato.omnicleaner.model.GallerySwipeHistoryItem
import com.riccardopinato.omnicleaner.model.ProScanType
import com.riccardopinato.omnicleaner.model.SwipeDirection
import com.riccardopinato.omnicleaner.model.ThresholdUnit
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OmniCleanerViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val root: File get() = Environment.getExternalStorageDirectory()
    private val safeTrashManager = SafeTrashManager(context)
    private val galleryPrefs = GalleryPreferencesManager(context)
    private val proPrefs = ProPreferencesManager(context)
    private val folderExifCleaner = FolderExifCleaner(context)

    private val _uiState = MutableStateFlow(OmniUiState())
    val uiState: StateFlow<OmniUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var galleryJob: Job? = null

    init {
        checkPermission()
        refreshStorageInfo()
        viewModelScope.launch {
            val session = galleryPrefs.sessionFlow.first()
            _uiState.update {
                it.copy(
                    galleryFilter = session.filter,
                    gallerySortOrder = session.sort,
                )
            }
        }
        viewModelScope.launch {
            val types = proPrefs.selectedTypesFlow.first()
            _uiState.update { state ->
                state.copy(proScanSelection = state.proScanSelection.copy(selectedTypes = types))
            }
        }
    }

    fun setTab(tab: CleanTab) {
        _uiState.update { it.copy(currentTab = tab, lastMessage = null) }
    }

    fun setFolderSubTab(tab: FolderSubTab) {
        _uiState.update { it.copy(folderSubTab = tab, lastMessage = null) }
    }

    fun checkPermission() {
        _uiState.update { it.copy(capabilityHealth = PermissionHealth.storageHealth(context)) }
    }

    fun refreshStorageInfo() {
        val stat = android.os.StatFs(root.absolutePath)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = (total - free).coerceAtLeast(0L)
        val percent = if (total == 0L) 0f else used.toFloat() / total.toFloat()
        _uiState.update {
            it.copy(
                storageInfo = StorageInfo(total, used, free, percent),
                safeTrashBytes = safeTrashManager.getTrashSize(),
            )
        }
    }

    fun scanSpaceOverview() = launchScan("Analisi spazio") {
        val overview = SpaceOverviewScanner.scan(root)
        _uiState.update { it.copy(spaceOverview = overview) }
    }

    fun startFolderScan() {
        if (!requireDeepAccess()) return
        val threshold = thresholdBytes()
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    folderScanProgress = GeneralScanProgress(
                        isScanning = true,
                        phaseDescription = "Analisi delle cartelle scelte...",
                    ),
                    lastMessage = null,
                )
            }
            runCatching {
                FileScanner.scanFolders(root, threshold) { checked, file ->
                    _uiState.update { state ->
                        state.copy(
                            folderScanProgress = state.folderScanProgress.copy(
                                itemsChecked = checked,
                                currentPath = file.absolutePath,
                            ),
                        )
                    }
                }
            }.onSuccess { folders ->
                _uiState.update {
                    it.copy(
                        folders = folders,
                        hasScannedFoldersOnce = true,
                        folderScanProgress = GeneralScanProgress(
                            matchingFound = folders.size,
                            phaseDescription = "Scansione completata",
                        ),
                    )
                }
            }.onFailure { error -> failScan("Cartelle", error) }
        }
    }

    fun startBigFilesScan() {
        if (!requireDeepAccess()) return
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            val minBytes = _uiState.value.bigFileSizeFilter.minBytes
            _uiState.update {
                it.copy(
                    bigFileScanProgress = GeneralScanProgress(true, phaseDescription = "Ricerca file grandi..."),
                    lastMessage = null,
                )
            }
            runCatching {
                FileScanner.findBigFiles(root, minBytes) { checked ->
                    _uiState.update { state ->
                        state.copy(
                            bigFileScanProgress = state.bigFileScanProgress.copy(itemsChecked = checked),
                        )
                    }
                }
            }.onSuccess { files ->
                _uiState.update {
                    it.copy(
                        bigFiles = files,
                        hasScannedBigFilesOnce = true,
                        bigFileScanProgress = GeneralScanProgress(
                            matchingFound = files.size,
                            phaseDescription = "Scansione completata",
                        ),
                    )
                }
            }.onFailure { error -> failScan("File grandi", error) }
        }
    }

    fun startDuplicateScan() {
        if (!requireDeepAccess()) return
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    duplicateScanProgress = GeneralScanProgress(true, phaseDescription = "Indicizzazione file..."),
                    lastMessage = null,
                )
            }
            runCatching {
                val inventory = FileScanner.collectFileInventory(root, _uiState.value.minDuplicateSizeBytes) { checked ->
                    _uiState.update { state ->
                        state.copy(
                            duplicateScanProgress = state.duplicateScanProgress.copy(
                                itemsChecked = checked,
                                phaseDescription = "Indicizzazione file...",
                            ),
                        )
                    }
                }
                FileScanner.findDuplicates(
                    root = root,
                    minBytes = _uiState.value.minDuplicateSizeBytes,
                    inventory = inventory,
                ) { progress ->
                    _uiState.update { state ->
                        state.copy(
                            duplicateScanProgress = state.duplicateScanProgress.copy(
                                itemsChecked = progress,
                                phaseDescription = "Verifica contenuto duplicati...",
                            ),
                        )
                    }
                }
            }.onSuccess { groups ->
                _uiState.update {
                    it.copy(
                        duplicateGroups = groups,
                        hasScannedDuplicatesOnce = true,
                        duplicateScanProgress = GeneralScanProgress(
                            matchingFound = groups.size,
                            phaseDescription = "Scansione completata",
                        ),
                    )
                }
            }.onFailure { error -> failScan("Duplicati", error) }
        }
    }

    fun startThumbnailCacheScan() {
        if (!requireDeepAccess()) return
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update { it.copy(isScanningThumbnailCache = true, lastMessage = null) }
            runCatching { FileScanner.scanThumbnailCache(root) }
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            thumbnailCaches = items,
                            isScanningThumbnailCache = false,
                            hasScannedThumbnailCacheOnce = true,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isScanningThumbnailCache = false,
                            lastMessage = "Miniature: ${error.message ?: "errore di scansione"}",
                        )
                    }
                }
        }
    }

    fun startAppResiduesScan() {
        if (!requireDeepAccess()) return
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update { it.copy(isScanningResidues = true, lastMessage = null) }
            val installed = runCatching {
                @Suppress("DEPRECATION")
                context.packageManager.getInstalledApplications(0).mapTo(hashSetOf()) { it.packageName }
            }.getOrDefault(emptySet())
            runCatching { FileScanner.scanAppResidues(root, installed) }
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            appResidues = items,
                            isScanningResidues = false,
                            hasScannedResiduesOnce = true,
                            lastMessage = if (items.isNotEmpty()) {
                                "Residui potenziali: verifica ogni cartella prima di eliminarla."
                            } else null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isScanningResidues = false,
                            lastMessage = "Residui: ${error.message ?: "errore di scansione"}",
                        )
                    }
                }
        }
    }

    fun loadGalleryMedia(force: Boolean = false) {
        if (!PermissionHealth.mediaReadGranted(context) && !PermissionHealth.allFilesGranted()) {
            _uiState.update { it.copy(lastMessage = "Concedi l'accesso a foto e video per usare la galleria.") }
            return
        }
        if (_uiState.value.galleryIsLoading && !force) return
        galleryJob?.cancel()
        galleryJob = viewModelScope.launch {
            _uiState.update { it.copy(galleryIsLoading = true, lastMessage = null) }
            runCatching {
                MediaStoreHelper.loadGalleryMediaCancellable(
                    context = context,
                    filter = _uiState.value.galleryFilter,
                    sortOrder = _uiState.value.gallerySortOrder,
                )
            }.onSuccess { media ->
                _uiState.update {
                    it.copy(
                        galleryItems = media,
                        galleryCurrentIndex = 0,
                        galleryHistory = emptyList(),
                        galleryTrashedItems = emptyList(),
                        galleryIsLoading = false,
                        hasLoadedGalleryOnce = true,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        galleryIsLoading = false,
                        lastMessage = "Galleria: ${error.message ?: "errore di caricamento"}",
                    )
                }
            }
        }
    }

    fun swipeKeepCurrent() {
        val state = _uiState.value
        val item = state.galleryItems.getOrNull(state.galleryCurrentIndex) ?: return
        val history = state.galleryHistory + GallerySwipeHistoryItem(item, SwipeDirection.KEEP, state.galleryCurrentIndex)
        val next = (state.galleryCurrentIndex + 1).coerceAtMost(state.galleryItems.size)
        _uiState.update { it.copy(galleryHistory = history, galleryCurrentIndex = next) }
        persistGalleryProgress(item.id, next, history.size)
    }

    fun swipeTrashCurrent() {
        val state = _uiState.value
        val item = state.galleryItems.getOrNull(state.galleryCurrentIndex) ?: return
        val history = state.galleryHistory + GallerySwipeHistoryItem(item, SwipeDirection.TRASH, state.galleryCurrentIndex)
        val next = (state.galleryCurrentIndex + 1).coerceAtMost(state.galleryItems.size)
        _uiState.update {
            it.copy(
                galleryHistory = history,
                galleryTrashedItems = it.galleryTrashedItems + item,
                galleryCurrentIndex = next,
            )
        }
        persistGalleryProgress(item.id, next, history.size)
    }

    fun undoLastGalleryAction() {
        val state = _uiState.value
        val last = state.galleryHistory.lastOrNull() ?: return
        _uiState.update {
            it.copy(
                galleryHistory = it.galleryHistory.dropLast(1),
                galleryTrashedItems = if (last.direction == SwipeDirection.TRASH) {
                    it.galleryTrashedItems.filterNot { trashed -> trashed.uri == last.item.uri }
                } else it.galleryTrashedItems,
                galleryCurrentIndex = last.previousIndex.coerceAtLeast(0),
            )
        }
    }

    fun galleryTrashRequest(): android.app.PendingIntent? {
        return MediaStoreHelper.trashMediaFiles(
            context.contentResolver,
            _uiState.value.galleryTrashedItems.map { it.uri },
        )
    }

    fun onMediaStoreTrashCompleted(success: Boolean) {
        if (success) {
            _uiState.update {
                it.copy(
                    galleryItems = it.galleryItems.filterNot { item ->
                        it.galleryTrashedItems.any { trash -> trash.uri == item.uri }
                    },
                    galleryTrashedItems = emptyList(),
                    galleryHistory = emptyList(),
                    galleryCurrentIndex = 0,
                    lastMessage = "Elementi spostati nel cestino di sistema.",
                )
            }
        } else {
            _uiState.update { it.copy(lastMessage = "Operazione cestino annullata.") }
        }
    }

    fun toggleFolderSelection(path: String) {
        _uiState.update { state ->
            state.copy(folders = state.folders.map { if (it.path == path) it.copy(isSelected = !it.isSelected) else it })
        }
    }

    fun toggleBigFileSelection(path: String) {
        _uiState.update { state ->
            state.copy(bigFiles = state.bigFiles.map { if (it.path == path) it.copy(isSelected = !it.isSelected) else it })
        }
    }

    fun toggleDuplicateFileSelection(path: String) {
        _uiState.update { state ->
            state.copy(
                duplicateGroups = state.duplicateGroups.map { group ->
                    group.copy(files = group.files.map { file ->
                        if (file.path == path) file.copy(isSelected = !file.isSelected) else file
                    })
                },
            )
        }
    }

    fun toggleThumbnailCacheSelection(path: String) {
        _uiState.update { state ->
            state.copy(
                thumbnailCaches = state.thumbnailCaches.map {
                    if (it.path == path) it.copy(isSelected = !it.isSelected) else it
                },
            )
        }
    }

    fun smartSelectAllDuplicates() {
        _uiState.update { state ->
            state.copy(
                duplicateGroups = state.duplicateGroups.map { group ->
                    val keeper = group.files.minWithOrNull(compareBy<DuplicateFile> { it.lastModified }.thenBy { it.path })
                    group.copy(files = group.files.map { it.copy(isSelected = it.path != keeper?.path) })
                },
            )
        }
    }

    fun smartSelectKeepOldest() {
        _uiState.update { state ->
            state.copy(
                duplicateGroups = state.duplicateGroups.map { group ->
                    val keeper = group.files.minByOrNull { it.lastModified }
                    group.copy(files = group.files.map { it.copy(isSelected = it.path != keeper?.path) })
                },
            )
        }
    }

    fun smartSelectKeepNewest() {
        _uiState.update { state ->
            state.copy(
                duplicateGroups = state.duplicateGroups.map { group ->
                    val keeper = group.files.maxByOrNull { it.lastModified }
                    group.copy(files = group.files.map { it.copy(isSelected = it.path != keeper?.path) })
                },
            )
        }
    }

    fun cleanSelectedFolders() = cleanFiles(
        _uiState.value.folders.filter { it.isSelected }.map { it.file },
    ) {
        _uiState.update { state -> state.copy(folders = state.folders.filter { it.file.exists() }) }
    }

    fun cleanSelectedBigFiles() = cleanFiles(
        _uiState.value.bigFiles.filter { it.isSelected }.map { it.file },
    ) {
        _uiState.update { state -> state.copy(bigFiles = state.bigFiles.filter { it.file.exists() }) }
    }

    fun cleanSelectedDuplicates() {
        val files = _uiState.value.duplicateGroups.flatMap { group ->
            val selected = group.files.filter { it.isSelected }
            if (selected.size >= group.files.size) selected.drop(1) else selected
        }.map { it.file }
        cleanFiles(files) {
            _uiState.update { state ->
                state.copy(
                    duplicateGroups = state.duplicateGroups.mapNotNull { group ->
                        val remaining = group.files.filter { it.file.exists() }
                        if (remaining.size >= 2) group.copy(files = remaining) else null
                    },
                )
            }
        }
    }

    fun cleanSelectedThumbnails() {
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, lastMessage = null) }
            var removed = 0
            var rejected = 0
            for (item in _uiState.value.thumbnailCaches.filter { it.isSelected }) {
                val snapshot = item.inventory ?: continue
                val result = ThumbnailFileCleanup.clean(snapshot)
                removed += result.removed
                rejected += result.rejected
            }
            _uiState.update {
                it.copy(
                    thumbnailCaches = it.thumbnailCaches.filter { item -> item.file.exists() && item.file.listFiles()?.isNotEmpty() == true },
                    isDeleting = false,
                    lastMessage = "Miniature rimosse: $removed. Non rimosse perché cambiate/protette: $rejected.",
                )
            }
            refreshStorageInfo()
        }
    }

    fun emptySafeTrash() {
        viewModelScope.launch {
            val ok = safeTrashManager.emptyTrash()
            refreshStorageInfo()
            _uiState.update {
                it.copy(lastMessage = if (ok) "Cestino sicuro svuotato." else "Alcuni elementi del cestino non sono stati rimossi.")
            }
        }
    }

    fun updateThresholdValue(value: String) {
        _uiState.update { it.copy(thresholdValue = value.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }) }
    }

    fun updateThresholdUnit(unit: ThresholdUnit) {
        _uiState.update { it.copy(thresholdUnit = unit) }
    }

    fun updateBigFileSizeFilter(filter: BigFileSizeFilter) {
        _uiState.update { it.copy(bigFileSizeFilter = filter) }
    }

    fun updateFolderSearchQuery(query: String) = _uiState.update { it.copy(folderSearchQuery = query) }
    fun updateBigFileSearchQuery(query: String) = _uiState.update { it.copy(bigFileSearchQuery = query) }
    fun updateDuplicateSearchQuery(query: String) = _uiState.update { it.copy(duplicateSearchQuery = query) }

    fun updateGalleryFilter(filter: GalleryMediaFilter) {
        _uiState.update { it.copy(galleryFilter = filter) }
        viewModelScope.launch {
            galleryPrefs.saveFilterAndSort(filter, _uiState.value.gallerySortOrder)
            loadGalleryMedia(force = true)
        }
    }

    fun updateGallerySortOrder(order: GallerySortOrder) {
        _uiState.update { it.copy(gallerySortOrder = order) }
        viewModelScope.launch {
            galleryPrefs.saveFilterAndSort(_uiState.value.galleryFilter, order)
            loadGalleryMedia(force = true)
        }
    }

    fun addExifRoot(uri: android.net.Uri) {
        _uiState.update { state ->
            state.copy(
                exifRoots = (state.exifRoots + uri).distinct(),
                exifScan = null,
                exifLastExport = null,
            )
        }
    }

    fun removeExifRoot(uri: android.net.Uri) {
        _uiState.update { state ->
            state.copy(
                exifRoots = state.exifRoots.filterNot { it == uri },
                exifScan = null,
                exifLastExport = null,
            )
        }
    }

    fun toggleExifGroup(group: ExifGroup) {
        _uiState.update { state ->
            val groups = state.exifGroups.toMutableSet().apply {
                if (!add(group)) remove(group)
            }
            state.copy(exifGroups = groups, exifScan = null, exifLastExport = null)
        }
    }

    fun setExifRecursive(enabled: Boolean) {
        _uiState.update { it.copy(exifRecursive = enabled, exifScan = null) }
    }

    fun startExifScan() {
        val state = _uiState.value
        if (state.exifRoots.isEmpty() || state.exifGroups.isEmpty() || state.exifIsScanning) return
        viewModelScope.launch {
            _uiState.update { it.copy(exifIsScanning = true, exifLastExport = null, lastMessage = null) }
            runCatching {
                folderExifCleaner.scan(
                    trees = _uiState.value.exifRoots,
                    recursive = _uiState.value.exifRecursive,
                    groups = _uiState.value.exifGroups,
                )
            }.onSuccess { scan ->
                _uiState.update {
                    it.copy(
                        exifIsScanning = false,
                        exifScan = scan,
                        lastMessage = "Privacy: " + scan.candidates.size + " immagini con metadati selezionati.",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        exifIsScanning = false,
                        lastMessage = "Privacy EXIF: " + (error.message ?: "errore di scansione"),
                    )
                }
            }
        }
    }

    fun exportExifCleanCopies(destination: android.net.Uri) {
        val scan = _uiState.value.exifScan ?: return
        if (_uiState.value.exifIsExporting || scan.candidates.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(exifIsExporting = true, lastMessage = null) }
            runCatching {
                folderExifCleaner.export(scan, destination)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        exifIsExporting = false,
                        exifLastExport = result,
                        lastMessage = "Copie EXIF create: " + result.created +
                            ". Fallite: " + result.failed +
                            ". Contenuto non verificato: " + result.incompleteCopies.size + ".",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        exifIsExporting = false,
                        lastMessage = "Export EXIF: " + (error.message ?: "errore"),
                    )
                }
            }
        }
    }

    fun setPremium(enabled: Boolean) {
        _uiState.update { it.copy(isPremium = enabled) }
    }

    fun toggleProScanType(type: ProScanType) {
        val next = _uiState.value.proScanSelection.selectedTypes.toMutableSet().apply {
            if (!add(type)) remove(type)
        }
        _uiState.update {
            it.copy(proScanSelection = it.proScanSelection.copy(selectedTypes = next))
        }
        viewModelScope.launch { proPrefs.saveSelectedTypes(next) }
    }

    fun cancelActiveScan() {
        activeJob?.cancel()
        activeJob = null
        _uiState.update {
            it.copy(
                folderScanProgress = it.folderScanProgress.copy(isScanning = false),
                duplicateScanProgress = it.duplicateScanProgress.copy(isScanning = false),
                bigFileScanProgress = it.bigFileScanProgress.copy(isScanning = false),
                isScanningResidues = false,
                isScanningThumbnailCache = false,
                lastMessage = "Scansione interrotta.",
            )
        }
    }

    private fun requireDeepAccess(): Boolean {
        checkPermission()
        if (_uiState.value.capabilityHealth != StorageCapabilityHealth.HEALTHY) {
            _uiState.update { it.copy(lastMessage = "Accesso a tutti i file richiesto per questa scansione.") }
            return false
        }
        return true
    }

    private fun thresholdBytes(): Long {
        val numeric = _uiState.value.thresholdValue.replace(',', '.').toDoubleOrNull() ?: 0.0
        return (numeric * _uiState.value.thresholdUnit.multiplier).toLong().coerceAtLeast(0L)
    }

    private fun cleanFiles(files: List<File>, onComplete: () -> Unit) {
        if (files.isEmpty() || _uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, lastMessage = null) }
            var moved = 0
            var skipped = 0
            files.distinctBy { it.absolutePath }.forEach { file ->
                val destination = safeTrashManager.moveToTrash(file)
                if (destination != null) moved++ else skipped++
            }
            onComplete()
            refreshStorageInfo()
            _uiState.update {
                it.copy(
                    isDeleting = false,
                    lastMessage = "Spostati nel cestino sicuro: $moved. Non rimossi: $skipped.",
                )
            }
        }
    }

    private fun launchScan(label: String, block: suspend () -> Unit) {
        if (!requireDeepAccess()) return
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            runCatching { block() }
                .onFailure { error ->
                    _uiState.update { it.copy(lastMessage = "$label: ${error.message ?: "errore"}") }
                }
        }
    }

    private fun failScan(label: String, error: Throwable) {
        _uiState.update {
            it.copy(
                folderScanProgress = it.folderScanProgress.copy(isScanning = false),
                duplicateScanProgress = it.duplicateScanProgress.copy(isScanning = false),
                bigFileScanProgress = it.bigFileScanProgress.copy(isScanning = false),
                lastMessage = "$label: ${error.message ?: "errore"}",
            )
        }
    }

    private fun persistGalleryProgress(anchorId: Long, index: Int, reviewed: Int) {
        viewModelScope.launch { galleryPrefs.saveProgress(anchorId, index, reviewed) }
    }
}
