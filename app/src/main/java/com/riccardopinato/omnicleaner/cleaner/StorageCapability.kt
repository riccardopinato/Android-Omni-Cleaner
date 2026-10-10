package com.riccardopinato.omnicleaner.cleaner

enum class DeepStorageAccessStatus {
    READY,
    NEEDS_ALL_FILES_ACCESS,
    NEEDS_LEGACY_PERMISSION,
    SCOPED_ONLY_ANDROID_10,
    STORAGE_UNAVAILABLE,
}

data class StorageCapabilityHealth(
    val status: DeepStorageAccessStatus,
    val standardCleaningAvailable: Boolean,
    val deepScanAvailable: Boolean,
    val canRequestDeepAccess: Boolean,
)

internal fun evaluateStorageCapability(
    sdkInt: Int,
    allFilesGranted: Boolean,
    legacyReadGranted: Boolean,
    legacyWriteGranted: Boolean,
    storageMounted: Boolean,
): StorageCapabilityHealth {
    if (!storageMounted) {
        return StorageCapabilityHealth(
            status = DeepStorageAccessStatus.STORAGE_UNAVAILABLE,
            standardCleaningAvailable = false,
            deepScanAvailable = false,
            canRequestDeepAccess = false,
        )
    }

    return when {
        sdkInt >= 30 && allFilesGranted -> StorageCapabilityHealth(
            status = DeepStorageAccessStatus.READY,
            standardCleaningAvailable = true,
            deepScanAvailable = true,
            canRequestDeepAccess = false,
        )

        sdkInt >= 30 -> StorageCapabilityHealth(
            status = DeepStorageAccessStatus.NEEDS_ALL_FILES_ACCESS,
            standardCleaningAvailable = true,
            deepScanAvailable = false,
            canRequestDeepAccess = true,
        )

        sdkInt == 29 -> StorageCapabilityHealth(
            status = DeepStorageAccessStatus.SCOPED_ONLY_ANDROID_10,
            standardCleaningAvailable = true,
            deepScanAvailable = false,
            canRequestDeepAccess = false,
        )

        legacyReadGranted && legacyWriteGranted -> StorageCapabilityHealth(
            status = DeepStorageAccessStatus.READY,
            standardCleaningAvailable = true,
            deepScanAvailable = true,
            canRequestDeepAccess = false,
        )

        else -> StorageCapabilityHealth(
            status = DeepStorageAccessStatus.NEEDS_LEGACY_PERMISSION,
            standardCleaningAvailable = true,
            deepScanAvailable = false,
            canRequestDeepAccess = true,
        )
    }
}
