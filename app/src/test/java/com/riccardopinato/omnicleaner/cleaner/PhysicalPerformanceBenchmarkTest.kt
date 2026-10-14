package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicalPerformanceBenchmarkTest {
    @Test
    fun reportContainsStableEvidenceKeys() {
        val result = PhysicalPerformanceBenchmarkResult(
            capturedAtMillis = 123L,
            appVersion = "0.13.0-recovery-debug",
            device = "Example Phone",
            sdk = 36,
            availableProcessors = 8,
            filesTotal = 10_000,
            coldScanMillis = 4_000L,
            coldScannerMillis = 3_500L,
            warmScanMillis = 1_000L,
            warmPersistentHit = true,
            warmFilesTotal = 10_000,
            hotMemoryScanMillis = 2L,
            hotMemoryHit = true,
            benchmarkFiles = 20,
            benchmarkBytes = 64L * 1024L * 1024L,
            hashPrewarmMillis = 1_200L,
            hashPrewarmBytes = 64L * 1024L * 1024L,
            hashRuns = listOf(
                HashWorkerBenchmark(1, 1_000L, 20, 64L * 1024L * 1024L, 1),
                HashWorkerBenchmark(2, 600L, 20, 64L * 1024L * 1024L, 2),
                HashWorkerBenchmark(4, 550L, 20, 64L * 1024L * 1024L, 4),
            ),
            batteryStartPercent = 80,
            batteryEndPercent = 79,
            batteryTemperatureStartC = 30.1,
            batteryTemperatureEndC = 31.2,
            thermalStart = 0,
            thermalEnd = 1,
            observedHeapMaxBytes = 42L * 1024L * 1024L,
            emulatorSignalsDetected = false,
        )

        val report = result.toReport()

        assertTrue(report.startsWith("OMNI_PERF_V1"))
        assertTrue("cold_scan_ms=4000" in report)
        assertTrue("warm_scan_ms=1000" in report)
        assertTrue("warm_persistent_hit=true" in report)
        assertTrue("warm_speedup_x=4.00" in report)
        assertTrue("hot_memory_hit=true" in report)
        assertTrue("hash_prewarm_ms=1200" in report)
        assertTrue("hash_prewarm_bytes=67108864" in report)
        assertTrue("hash_1_workers_peak_inflight=1" in report)
        assertTrue("hash_2_workers_peak_inflight=2" in report)
        assertTrue("hash_4_workers_peak_inflight=4" in report)
        assertTrue("emulator_signals_detected=false" in report)
        assertTrue("destructive_actions=false" in report)
    }

    @Test
    fun hashThroughputUsesMebibytesPerSecond() {
        val run = HashWorkerBenchmark(
            workers = 2,
            elapsedMillis = 2_000L,
            filesHashed = 10,
            bytesHashed = 100L * 1024L * 1024L,
            peakInFlight = 2,
        )

        assertEquals(50.0, run.mebibytesPerSecond, 0.0001)
    }
}
