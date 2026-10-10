package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageCapabilityPolicyTest {
    @Test
    fun android11PlusRequiresExplicitAllFilesAccessForDeepScan() {
        val missing = evaluateStorageCapability(
            sdkInt = 35,
            allFilesGranted = false,
            legacyReadGranted = true,
            storageMounted = true,
        )
        assertEquals(DeepStorageAccessStatus.NEEDS_ALL_FILES_ACCESS, missing.status)
        assertTrue(missing.standardCleaningAvailable)
        assertFalse(missing.deepScanAvailable)
        assertTrue(missing.canRequestDeepAccess)

        val granted = evaluateStorageCapability(
            sdkInt = 35,
            allFilesGranted = true,
            legacyReadGranted = false,
            storageMounted = true,
        )
        assertEquals(DeepStorageAccessStatus.READY, granted.status)
        assertTrue(granted.deepScanAvailable)
    }

    @Test
    fun android10DoesNotPretendLegacyReadIsFullFilesystemAccess() {
        val health = evaluateStorageCapability(
            sdkInt = 29,
            allFilesGranted = false,
            legacyReadGranted = true,
            storageMounted = true,
        )

        assertEquals(DeepStorageAccessStatus.SCOPED_ONLY_ANDROID_10, health.status)
        assertTrue(health.standardCleaningAvailable)
        assertFalse(health.deepScanAvailable)
        assertFalse(health.canRequestDeepAccess)
    }

    @Test
    fun preAndroid10UsesLegacyReadPermissionJustInTime() {
        val missing = evaluateStorageCapability(
            sdkInt = 28,
            allFilesGranted = false,
            legacyReadGranted = false,
            storageMounted = true,
        )
        assertEquals(DeepStorageAccessStatus.NEEDS_LEGACY_PERMISSION, missing.status)

        val granted = evaluateStorageCapability(
            sdkInt = 28,
            allFilesGranted = false,
            legacyReadGranted = true,
            storageMounted = true,
        )
        assertEquals(DeepStorageAccessStatus.READY, granted.status)
        assertTrue(granted.deepScanAvailable)
    }

    @Test
    fun unavailableStorageDisablesBothLanes() {
        val health = evaluateStorageCapability(
            sdkInt = 35,
            allFilesGranted = true,
            legacyReadGranted = true,
            storageMounted = false,
        )
        assertEquals(DeepStorageAccessStatus.STORAGE_UNAVAILABLE, health.status)
        assertFalse(health.standardCleaningAvailable)
        assertFalse(health.deepScanAvailable)
    }
}
