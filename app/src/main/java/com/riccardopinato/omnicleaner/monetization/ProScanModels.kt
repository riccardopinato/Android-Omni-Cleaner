package com.riccardopinato.omnicleaner.monetization

enum class ProScanType(val label: String, val description: String) {
    EMPTY_FOLDERS("Cartelle vuote e piccole", "Elimina cartelle orfane o prive di contenuti"),
    DUPLICATES("File duplicati", "Sposta i cloni nel cestino salvaguardando gli originali"),
    LARGE_FILES("Grandi file", "Isola file voluminosi che saturano la memoria"),
    THUMBNAIL_CACHE("Cache e miniature", "Pulisce cache temporanee e miniature obsolete"),
    APP_RESIDUES("Residui accessibili", "Elimina cartelle orfane in Android/media di app disinstallate"),
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

data class ProBatchResult(val type: ProScanType, val candidates: Int, val candidateBytes: Long)
data class ProBatchState(
    val running: Boolean = false,
    val completedTypes: Set<ProScanType> = emptySet(),
    val results: List<ProBatchResult> = emptyList(),
    val error: String? = null,
)
