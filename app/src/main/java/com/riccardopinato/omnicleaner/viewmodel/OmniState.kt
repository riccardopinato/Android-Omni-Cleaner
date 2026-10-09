package com.riccardopinato.omnicleaner.viewmodel

import android.net.Uri
import com.riccardopinato.omnicleaner.cleaner.SpaceOverview
import com.riccardopinato.omnicleaner.cleaner.StorageCapabilityHealth
import com.riccardopinato.omnicleaner.cleaner.exif.ExifExport
import com.riccardopinato.omnicleaner.cleaner.exif.ExifGroup
import com.riccardopinato.omnicleaner.cleaner.exif.ExifScan
import com.riccardopinato.omnicleaner.model.AppResidueItem
import com.riccardopinato.omnicleaner.model.BigFileItem
import com.riccardopinato.omnicleaner.model.BigFileSizeFilter
import com.riccardopinato.omnicleaner.model.DuplicateGroup
import com.riccardopinato.omnicleaner.model.FolderItem
import com.riccardopinato.omnicleaner.model.FolderSubTab
import com.riccardopinato.omnicleaner.model.GalleryMediaFilter
import com.riccardopinato.omnicleaner.model.GalleryMediaItem
import com.riccardopinato.omnicleaner.model.GallerySortOrder
import com.riccardopinato.omnicleaner.model.GallerySwipeHistoryItem
import com.riccardopinato.omnicleaner.model.ProScanSelection
import com.riccardopinato.omnicleaner.model.ThresholdUnit
import com.riccardopinato.omnicleaner.model.ThumbnailCacheItem

enum class CleanTab(val title: String, val subtitle: String) {
    EMPTY_FOLDERS("Cartelle", "Vuote, residue e miniature"),
    DUPLICATES("Duplicati", "Trova copie identiche"),
    BIG_FILES("File grandi", "Recupera spazio dai file pesanti"),
    UNLOCK_PRO("Pro", "Pulizia combinata e strumenti avanzati"),
}

data class StorageInfo(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedPercent: Float = 0f,
)

data class GeneralScanProgress(
    val isScanning: Boolean = false,
    val itemsChecked: Int = 0,
    val currentPath: String = "",
    val matchingFound: Int = 0,
    val phaseDescription: String = "",
)

data class OmniUiState(
    val currentTab: CleanTab = CleanTab.EMPTY_FOLDERS,
    val capabilityHealth: StorageCapabilityHealth = StorageCapabilityHealth.DENIED,
    val storageInfo: StorageInfo = StorageInfo(),
    val spaceOverview: SpaceOverview? = null,

    val folderSubTab: FolderSubTab = FolderSubTab.EMPTY_FOLDERS,
    val thresholdValue: String = "0",
    val thresholdUnit: ThresholdUnit = ThresholdUnit.BYTES,
    val folderScanProgress: GeneralScanProgress = GeneralScanProgress(),
    val folders: List<FolderItem> = emptyList(),
    val folderSearchQuery: String = "",
    val hasScannedFoldersOnce: Boolean = false,

    val appResidues: List<AppResidueItem> = emptyList(),
    val appResidueSearchQuery: String = "",
    val isScanningResidues: Boolean = false,
    val hasScannedResiduesOnce: Boolean = false,

    val thumbnailCaches: List<ThumbnailCacheItem> = emptyList(),
    val thumbnailCacheSearchQuery: String = "",
    val isScanningThumbnailCache: Boolean = false,
    val hasScannedThumbnailCacheOnce: Boolean = false,

    val duplicateScanProgress: GeneralScanProgress = GeneralScanProgress(),
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val duplicateSearchQuery: String = "",
    val hasScannedDuplicatesOnce: Boolean = false,
    val minDuplicateSizeBytes: Long = 1L,

    val bigFileScanProgress: GeneralScanProgress = GeneralScanProgress(),
    val bigFiles: List<BigFileItem> = emptyList(),
    val bigFileSearchQuery: String = "",
    val bigFileSizeFilter: BigFileSizeFilter = BigFileSizeFilter.SIZE_50MB,
    val hasScannedBigFilesOnce: Boolean = false,

    val galleryItems: List<GalleryMediaItem> = emptyList(),
    val galleryCurrentIndex: Int = 0,
    val galleryHistory: List<GallerySwipeHistoryItem> = emptyList(),
    val galleryTrashedItems: List<GalleryMediaItem> = emptyList(),
    val galleryFilter: GalleryMediaFilter = GalleryMediaFilter.ALL,
    val gallerySortOrder: GallerySortOrder = GallerySortOrder.DATE_DESC,
    val galleryIsLoading: Boolean = false,
    val hasLoadedGalleryOnce: Boolean = false,

    val isDeleting: Boolean = false,
    val safeTrashBytes: Long = 0L,
    val lastMessage: String? = null,

    val exifRoots: List<Uri> = emptyList(),
    val exifGroups: Set<ExifGroup> = ExifGroup.entries.toSet(),
    val exifRecursive: Boolean = true,
    val exifIsScanning: Boolean = false,
    val exifIsExporting: Boolean = false,
    val exifScan: ExifScan? = null,
    val exifLastExport: ExifExport? = null,

    val isPremium: Boolean = false,
    val proScanSelection: ProScanSelection = ProScanSelection(),
) {
    val selectedFolderBytes: Long get() = folders.filter { it.isSelected }.sumOf { it.sizeBytes }
    val selectedBigFileBytes: Long get() = bigFiles.filter { it.isSelected }.sumOf { it.sizeBytes }
    val selectedDuplicateBytes: Long
        get() = duplicateGroups.sumOf { group ->
            group.files.filter { it.isSelected }.sumOf { it.sizeBytes }
        }
    val selectedThumbnailBytes: Long get() = thumbnailCaches.filter { it.isSelected }.sumOf { it.sizeBytes }

    val filteredFolders: List<FolderItem>
        get() = folders.filter {
            folderSearchQuery.isBlank() ||
                it.name.contains(folderSearchQuery, ignoreCase = true) ||
                it.path.contains(folderSearchQuery, ignoreCase = true)
        }

    val filteredBigFiles: List<BigFileItem>
        get() = bigFiles.filter {
            bigFileSearchQuery.isBlank() ||
                it.name.contains(bigFileSearchQuery, ignoreCase = true) ||
                it.path.contains(bigFileSearchQuery, ignoreCase = true)
        }

    val filteredDuplicateGroups: List<DuplicateGroup>
        get() = duplicateGroups.mapNotNull { group ->
            if (duplicateSearchQuery.isBlank()) group
            else {
                val matches = group.files.filter {
                    it.name.contains(duplicateSearchQuery, ignoreCase = true) ||
                        it.path.contains(duplicateSearchQuery, ignoreCase = true)
                }
                if (matches.size >= 2) group.copy(files = matches) else null
            }
        }
}
