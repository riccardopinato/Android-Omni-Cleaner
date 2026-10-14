package com.riccardopinato.omnicleaner.cleaner

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import com.riccardopinato.omnicleaner.BuildConfig
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class HashWorkerBenchmark(
    val workers: Int,
    val elapsedMillis: Long,
    val filesHashed: Int,
    val bytesHashed: Long,
    val peakInFlight: Int,
) {
    val mebibytesPerSecond: Double
        get() {
            if (elapsedMillis <= 0L) return 0.0
            val mebibytes = bytesHashed.toDouble() / (1024.0 * 1024.0)
            return mebibytes / (elapsedMillis.toDouble() / 1000.0)
        }
}

data class PhysicalPerformanceBenchmarkResult(
    val capturedAtMillis: Long,
    val appVersion: String,
    val device: String,
    val sdk: Int,
    val availableProcessors: Int,
    val filesTotal: Int,
    val coldScanMillis: Long,
    val coldScannerMillis: Long,
    val warmScanMillis: Long,
    val warmPersistentHit: Boolean,
    val warmFilesTotal: Int,
    val hotMemoryScanMillis: Long,
    val benchmarkFiles: Int,
    val benchmarkBytes: Long,
    val hashPrewarmMillis: Long,
    val hashPrewarmBytes: Long,
    val hashRuns: List<HashWorkerBenchmark>,
    val batteryStartPercent: Int?,
    val batteryEndPercent: Int?,
    val batteryTemperatureStartC: Double?,
    val batteryTemperatureEndC: Double?,
    val thermalStart: Int?,
    val thermalEnd: Int?,
    val observedHeapMaxBytes: Long,
    val emulatorSignalsDetected: Boolean,
) {
    val warmSpeedup: Double?
        get() = if (warmScanMillis > 0L && coldScanMillis > 0L) {
            coldScanMillis.toDouble() / warmScanMillis.toDouble()
        } else {
            null
        }

    fun toReport(): String = buildString {
        appendLine("OMNI_PERF_V1")
        appendLine("captured_at_ms=$capturedAtMillis")
        appendLine("app_version=$appVersion")
        appendLine("device=$device")
        appendLine("sdk=$sdk")
        appendLine("available_processors=$availableProcessors")
        appendLine("files_total=$filesTotal")
        appendLine("cold_scan_ms=$coldScanMillis")
        appendLine("cold_scanner_only_ms=$coldScannerMillis")
        appendLine("warm_scan_ms=$warmScanMillis")
        appendLine("warm_persistent_hit=$warmPersistentHit")
        appendLine("warm_files_total=$warmFilesTotal")
        appendLine("warm_file_count_delta=${warmFilesTotal - filesTotal}")
        appendLine("warm_speedup_x=${warmSpeedup?.let { String.format(Locale.US, "%.2f", it) } ?: "n/a"}")
        appendLine("hot_memory_scan_ms=$hotMemoryScanMillis")
        appendLine("hash_sample_files=$benchmarkFiles")
        appendLine("hash_sample_bytes=$benchmarkBytes")
        appendLine("hash_prewarm_ms=$hashPrewarmMillis")
        appendLine("hash_prewarm_bytes=$hashPrewarmBytes")
        hashRuns.sortedBy { it.workers }.forEach { run ->
            appendLine("hash_${run.workers}_workers_ms=${run.elapsedMillis}")
            appendLine("hash_${run.workers}_workers_files=${run.filesHashed}")
            appendLine("hash_${run.workers}_workers_bytes=${run.bytesHashed}")
            appendLine("hash_${run.workers}_workers_peak_inflight=${run.peakInFlight}")
            appendLine(
                "hash_${run.workers}_workers_mib_s=" +
                    String.format(Locale.US, "%.2f", run.mebibytesPerSecond),
            )
        }
        appendLine("battery_start_pct=${batteryStartPercent ?: "n/a"}")
        appendLine("battery_end_pct=${batteryEndPercent ?: "n/a"}")
        appendLine(
            "battery_temp_start_c=" +
                (batteryTemperatureStartC?.let { String.format(Locale.US, "%.1f", it) } ?: "n/a"),
        )
        appendLine(
            "battery_temp_end_c=" +
                (batteryTemperatureEndC?.let { String.format(Locale.US, "%.1f", it) } ?: "n/a"),
        )
        appendLine("thermal_start=${thermalStart ?: "n/a"}")
        appendLine("thermal_end=${thermalEnd ?: "n/a"}")
        appendLine("observed_heap_max_bytes=$observedHeapMaxBytes")
        appendLine("emulator_signals_detected=$emulatorSignalsDetected")
        appendLine("destructive_actions=false")
    }
}

class PhysicalPerformanceBenchmark(
    private val context: Context,
) {
    private val appContext = context.applicationContext

    suspend fun run(
        root: File,
        onPhase: ((String) -> Unit)? = null,
        onProgress: ((Int) -> Unit)? = null,
    ): PhysicalPerformanceBenchmarkResult = withContext(Dispatchers.Default) {
        val runtime = Runtime.getRuntime()
        var observedHeapMax = usedHeap(runtime)
        val batteryStart = batterySnapshot()
        val thermalStart = thermalStatus()

        onPhase?.invoke("cold_inventory")
        val coldStarted = SystemClock.elapsedRealtimeNanos()
        val cold = StorageInventoryRepository.snapshot(
            root = root,
            forceRefresh = true,
            onProgress = { checked, _ -> onProgress?.invoke(checked) },
            onStage = { stage -> onPhase?.invoke("cold_$stage") },
        )
        val coldMillis = elapsedMillisSince(coldStarted)
        observedHeapMax = max(observedHeapMax, usedHeap(runtime))

        onPhase?.invoke("warm_persistent_inventory")
        StorageInventoryRepository.dropMemoryCacheForBenchmark()
        val warmStarted = SystemClock.elapsedRealtimeNanos()
        val warm = StorageInventoryRepository.snapshot(
            root = root,
            onProgress = { checked, _ -> onProgress?.invoke(checked) },
            onStage = { stage -> onPhase?.invoke("warm_$stage") },
        )
        val warmMillis = elapsedMillisSince(warmStarted)
        observedHeapMax = max(observedHeapMax, usedHeap(runtime))

        onPhase?.invoke("hot_memory_inventory")
        val hotStarted = SystemClock.elapsedRealtimeNanos()
        StorageInventoryRepository.snapshot(root = root)
        val hotMillis = elapsedMillisSince(hotStarted)
        observedHeapMax = max(observedHeapMax, usedHeap(runtime))

        val hashSample = selectHashSample(cold.inventory.files)
        val scanner = FileInventoryScanner()

        onPhase?.invoke("hash_prewarm")
        val prewarmStarted = SystemClock.elapsedRealtimeNanos()
        var prewarmBytes = 0L
        for ((index, item) in hashSample.withIndex()) {
            coroutineContext.ensureActive()
            if (scanner.unchanged(item)) {
                val digest = nullableUnlessCancelled {
                    scanner.digestCancellable(item.file)
                }
                if (digest != null && scanner.unchanged(item)) {
                    prewarmBytes += item.size
                }
            }
            onProgress?.invoke(index + 1)
        }
        val prewarmMillis = elapsedMillisSince(prewarmStarted)
        observedHeapMax = max(observedHeapMax, usedHeap(runtime))

        val hashRuns = ArrayList<HashWorkerBenchmark>()
        for (workers in BENCHMARK_WORKERS) {
            coroutineContext.ensureActive()
            onPhase?.invoke("hash_${workers}_workers")
            val peakInFlight = AtomicInteger(0)
            val started = SystemClock.elapsedRealtimeNanos()
            val hashed = boundedHashFiles(
                items = hashSample,
                parallelism = workers,
                onInFlightChanged = { current ->
                    while (true) {
                        val previous = peakInFlight.get()
                        if (current <= previous || peakInFlight.compareAndSet(previous, current)) break
                    }
                },
            ) { item ->
                if (!scanner.unchanged(item)) {
                    null
                } else {
                    nullableUnlessCancelled {
                        scanner.digestCancellable(item.file)
                    }?.takeIf { scanner.unchanged(item) }
                }
            }
            val elapsed = elapsedMillisSince(started)
            val hashedPaths = hashed.asSequence()
                .map { it.item.file.absolutePath }
                .toHashSet()
            val bytes = hashSample
                .asSequence()
                .filter { it.file.absolutePath in hashedPaths }
                .sumOf { it.size }

            hashRuns += HashWorkerBenchmark(
                workers = workers,
                elapsedMillis = elapsed,
                filesHashed = hashed.size,
                bytesHashed = bytes,
                peakInFlight = peakInFlight.get(),
            )
            observedHeapMax = max(observedHeapMax, usedHeap(runtime))
        }

        val batteryEnd = batterySnapshot()
        val thermalEnd = thermalStatus()

        PhysicalPerformanceBenchmarkResult(
            capturedAtMillis = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            device = listOf(Build.MANUFACTURER, Build.MODEL)
                .filter { it.isNotBlank() }
                .joinToString(" "),
            sdk = Build.VERSION.SDK_INT,
            availableProcessors = Runtime.getRuntime().availableProcessors(),
            filesTotal = cold.inventory.files.size,
            coldScanMillis = coldMillis,
            coldScannerMillis = cold.scanElapsedMillis,
            warmScanMillis = warmMillis,
            warmPersistentHit = warm.persistentHit,
            warmFilesTotal = warm.inventory.files.size,
            hotMemoryScanMillis = hotMillis,
            benchmarkFiles = hashSample.size,
            benchmarkBytes = hashSample.sumOf { it.size },
            hashPrewarmMillis = prewarmMillis,
            hashPrewarmBytes = prewarmBytes,
            hashRuns = hashRuns,
            batteryStartPercent = batteryStart.percent,
            batteryEndPercent = batteryEnd.percent,
            batteryTemperatureStartC = batteryStart.temperatureC,
            batteryTemperatureEndC = batteryEnd.temperatureC,
            thermalStart = thermalStart,
            thermalEnd = thermalEnd,
            observedHeapMaxBytes = observedHeapMax,
            emulatorSignalsDetected = emulatorSignalsDetected(),
        )
    }

    private fun selectHashSample(files: List<IndexedFile>): List<IndexedFile> {
        val candidates = uniquePhysicalFiles(files)
            .asSequence()
            .filter { it.size in MIN_HASH_FILE_BYTES..MAX_HASH_FILE_BYTES }
            .sortedWith(
                compareByDescending<IndexedFile> { it.size }
                    .thenBy { it.file.absolutePath },
            )
            .toList()

        val selected = ArrayList<IndexedFile>()
        var bytes = 0L
        for (item in candidates) {
            if (selected.size >= MAX_HASH_FILES) break
            if (bytes + item.size > MAX_HASH_BYTES && selected.isNotEmpty()) continue
            selected += item
            bytes += item.size
            if (bytes >= MAX_HASH_BYTES) break
        }
        return selected
    }

    private fun batterySnapshot(): BatterySnapshot {
        val intent = appContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val temperatureTenths =
            intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                ?: Int.MIN_VALUE

        val percent = if (level >= 0 && scale > 0) {
            ((level.toDouble() / scale.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else {
            null
        }
        val temperature = if (temperatureTenths != Int.MIN_VALUE) {
            temperatureTenths / 10.0
        } else {
            null
        }
        return BatterySnapshot(percent, temperature)
    }

    private fun emulatorSignalsDetected(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase(Locale.ROOT)
        val model = Build.MODEL.lowercase(Locale.ROOT)
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val product = Build.PRODUCT.lowercase(Locale.ROOT)
        val hardware = Build.HARDWARE.lowercase(Locale.ROOT)

        return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            manufacturer.contains("genymotion") ||
            product.contains("sdk") ||
            product.contains("emulator") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu")
    }

    private fun thermalStatus(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val power = appContext.getSystemService(PowerManager::class.java) ?: return null
        return power.currentThermalStatus
    }

    private fun usedHeap(runtime: Runtime): Long =
        (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)

    private fun elapsedMillisSince(startNanos: Long): Long =
        ((SystemClock.elapsedRealtimeNanos() - startNanos) / 1_000_000L)
            .coerceAtLeast(0L)

    private data class BatterySnapshot(
        val percent: Int?,
        val temperatureC: Double?,
    )

    private companion object {
        val BENCHMARK_WORKERS = intArrayOf(1, 2, 4)
        const val MIN_HASH_FILE_BYTES = 4L * 1024L
        const val MAX_HASH_FILE_BYTES = 32L * 1024L * 1024L
        const val MAX_HASH_BYTES = 128L * 1024L * 1024L
        const val MAX_HASH_FILES = 200
    }
}
