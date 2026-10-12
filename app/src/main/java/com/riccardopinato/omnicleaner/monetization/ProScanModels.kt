package com.riccardopinato.omnicleaner.monetization

import com.riccardopinato.omnicleaner.cleaner.AppResidueItem
import com.riccardopinato.omnicleaner.cleaner.BigFileItem
import com.riccardopinato.omnicleaner.cleaner.DuplicateGroup
import com.riccardopinato.omnicleaner.cleaner.FolderItem
import com.riccardopinato.omnicleaner.cleaner.ThumbnailCacheItem

enum class ProScanType {
    EMPTY_FOLDERS,
    DUPLICATES,
    LARGE_FILES,
    THUMBNAIL_CACHE,
    APP_RESIDUES,
}

data class ProScanSelection(
    val selectedTypes: Set<ProScanType> = ProScanType.entries.toSet(),
    val isScanning: Boolean = false,
    val completedTypes: Set<ProScanType> = emptySet(),
    val totalRecoverableBytes: Long = 0L,
    val hasCompletedScan: Boolean = false,
    val showReviewSheet: Boolean = false,
    val showResolveConfirmDialog: Boolean = false,
)

data class ProBatchResult(
    val type: ProScanType,
    val candidates: Int,
    val candidateBytes: Long,
)

data class ProBatchState(
    val running: Boolean = false,
    val resolving: Boolean = false,
    val awaitingSystemTrash: Boolean = false,
    val completedTypes: Set<ProScanType> = emptySet(),
    val results: List<ProBatchResult> = emptyList(),
    val emptyFolders: List<FolderItem> = emptyList(),
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val largeFiles: List<BigFileItem> = emptyList(),
    val thumbnails: List<ThumbnailCacheItem> = emptyList(),
    val residues: List<AppResidueItem> = emptyList(),
    val selectedFolderPaths: Set<String> = emptySet(),
    val selectedDuplicatePaths: Set<String> = emptySet(),
    val selectedLargeFilePaths: Set<String> = emptySet(),
    val selectedThumbnailPaths: Set<String> = emptySet(),
    val totalRecoverableBytes: Long = 0L,
    val hasCompletedScan: Boolean = false,
    val reviewVisible: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)
