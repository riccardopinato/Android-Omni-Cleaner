package com.riccardopinato.omnicleaner.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.riccardopinato.omnicleaner.MainActivity
import com.riccardopinato.omnicleaner.cleaner.StorageInventoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Shared application-scoped scanner. The Activity and foreground service observe
 * the SAME CleanerController, avoiding competing scans or fabricated home counts.
 * FIFO is persisted for recovery; Android may still stop work under platform limits.
 */
internal object BentoScanRuntime {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var engine: CleanerController? = null
    private var appContext: Context? = null
    private var processing: Job? = null

    var activeId by mutableStateOf<String?>(null)
        private set
    var pending by mutableStateOf<List<String>>(emptyList())
        private set
    var completedAt by mutableStateOf<Map<String, Long>>(emptyMap())
        private set
    var lastFailure by mutableStateOf<String?>(null)
        private set

    private val scanIds = setOf("storage", "duplicates", "large", "folders")

    @Synchronized
    fun controller(context: Context): CleanerController {
        val existing = engine
        if (existing != null) return existing
        val app = context.applicationContext
        StorageInventoryRepository.initialize(app)
        appContext = app
        val prefs = app.getSharedPreferences("omni_color_bento_v2_scans", Context.MODE_PRIVATE)
        pending = prefs.getString("pending", "").orEmpty().split(",")
            .filter { it in scanIds }.distinct()
        val interrupted = prefs.getString("active", "").orEmpty()
        if (interrupted in scanIds && interrupted !in pending) {
            pending = listOf(interrupted) + pending
        }
        completedAt = prefs.getString("completed", "").orEmpty().split(";")
            .mapNotNull { value ->
                val item = value.split(":")
                val time = item.getOrNull(1)?.toLongOrNull()
                if (item.size == 2 && item[0] in scanIds && time != null) item[0] to time else null
            }.toMap()
        val created = CleanerController(app, scope)
        engine = created
        return created
    }

    private fun persist(context: Context) {
        context.applicationContext.getSharedPreferences("omni_color_bento_v2_scans", Context.MODE_PRIVATE)
            .edit()
            .putString("pending", pending.joinToString(","))
            .putString("active", activeId.orEmpty())
            .putString("completed", completedAt.entries.joinToString(";") { "${it.key}:${it.value}" })
            .apply()
    }

    fun enqueue(context: Context, id: String): Boolean {
        if (id !in scanIds) return false
        val controller = controller(context)
        controller.refreshAccessAndStorage()
        if (!controller.state.hasBroadAccess) {
            lastFailure = "Per questa analisi serve l'accesso ai file del dispositivo."
            return false
        }
        if (id != activeId && id !in pending) {
            pending = pending + id
            persist(context)
        }
        if (activeId != null || pending.isNotEmpty()) {
            val intent = Intent(context.applicationContext, BentoScanService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= 26) context.applicationContext.startForegroundService(intent)
                else context.applicationContext.startService(intent)
            } catch (_: RuntimeException) {
                lastFailure = "Android ha impedito l'avvio dell'analisi in background. Riprova con l'app aperta."
                return false
            }
        }
        lastFailure = null
        return true
    }

    fun cancel(context: Context) {
        pending = emptyList()
        engine?.cancelScan()
        activeId = null
        persist(context)
        context.applicationContext.stopService(Intent(context, BentoScanService::class.java))
    }

    private fun isComplete(id: String, state: CleanerUiState) = when (id) {
        "storage" -> state.overview != null
        "duplicates" -> state.duplicatesScanned
        "large" -> state.bigFilesScanned
        "folders" -> state.foldersScanned
        else -> false
    }

    fun start(service: BentoScanService) {
        val scanner = controller(service)
        if (processing?.isActive == true) return
        processing = scope.launch {
            while (isActive && pending.isNotEmpty()) {
                // Do not cancel a manual scan already running in the Clean section.
                while (isActive && scanner.state.scanning) delay(400)
                val id = pending.firstOrNull() ?: break
                pending = pending.drop(1)
                activeId = id
                persist(service)
                service.updateNotice(id, pending.size, scanner.state.indexedFilesChecked)
                when (id) {
                    "storage" -> scanner.scanOverview()
                    "duplicates" -> scanner.scanDuplicates()
                    "large" -> scanner.scanBigFiles()
                    "folders" -> scanner.scanFolders()
                }
                // runScan starts on Main.immediate; one dispatcher turn still prevents
                // treating an unstarted operation as a completed operation.
                delay(180)
                while (isActive && scanner.state.scanning && activeId == id) {
                    service.updateNotice(id, pending.size, scanner.state.indexedFilesChecked)
                    delay(850)
                }
                if (activeId != id) break // explicit user cancellation
                if (scanner.state.error == null && isComplete(id, scanner.state)) {
                    completedAt = completedAt + (id to System.currentTimeMillis())
                } else {
                    lastFailure = "Analisi ${id}: non completata."
                }
                activeId = null
                persist(service)
            }
            service.finish()
        }
    }

    fun interruptedForTimeout(service: Service) {
        engine?.cancelScan()
        if (activeId != null) pending = listOfNotNull(activeId) + pending.filterNot { it == activeId }
        activeId = null
        lastFailure = "Analisi sospese per un limite di esecuzione di Android."
        persist(service)
        service.stopSelf()
    }
}

/**
 * One foreground notification for a whole user-initiated FIFO queue.
 * Never performs deletion, never starts from BOOT_COMPLETED.
 */
internal class BentoScanService : Service() {
    private val channel = "omni_bento_scans"
    private val notificationId = 8102

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(channel, "Analisi Omni Cleaner", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            BentoScanRuntime.cancel(this)
            return START_NOT_STICKY
        }
        val notice = makeNotification("Preparazione analisi", 0, 0)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(notificationId, notice, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notificationId, notice)
        }
        BentoScanRuntime.start(this)
        return START_STICKY
    }

    fun updateNotice(id: String, queued: Int, files: Int) {
        val label = when (id) {
            "storage" -> "Memoria"
            "duplicates" -> "Duplicati"
            "large" -> "File grandi"
            "folders" -> "Cartelle"
            else -> "Archiviazione"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, makeNotification(label, queued, files))
    }

    private fun makeNotification(name: String, queued: Int, files: Int): Notification {
        val pendingActivity = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = Intent(this, BentoScanService::class.java).setAction(ACTION_CANCEL)
        val cancelPending = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, channel)
            else Notification.Builder(this)
        return builder
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Omni Cleaner · $name")
            .setContentText(
                if (files > 0) "$files file esaminati · $queued in attesa"
                else "Analisi attiva · $queued in attesa",
            )
            .setContentIntent(pendingActivity)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Annulla", cancelPending)
            .build()
    }

    fun finish() {
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
        else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        BentoScanRuntime.interruptedForTimeout(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_CANCEL = "com.riccardopinato.omnicleaner.BENTO_CANCEL"
    }
}
