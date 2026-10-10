package com.riccardopinato.omnicleaner.cleaner

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object ManualMaintenance {
    fun clearClipboard(activity: Activity): Boolean {
        if (!activity.hasWindowFocus()) return false
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                @Suppress("DEPRECATION")
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
            true
        }.getOrDefault(false)
    }

    fun openAppManagement(context: Context): Boolean = runCatching {
        val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
