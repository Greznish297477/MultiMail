package com.example.multiaccountmail.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** Скачивает APK релиза и открывает штатное системное окно установки. */
object UpdateInstaller {
    private const val PREFS = "app_update"
    private const val KEY_ID = "download_id"

    fun download(context: Context, release: UpdateRepository.Release): Boolean {
        if (release.apkUrl.isBlank()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return false
        }
        val request = DownloadManager.Request(Uri.parse(release.apkUrl))
            .setTitle("MultiMail ${release.tag}")
            .setDescription("Скачиваю обновление приложения")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = manager.enqueue(request)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_ID, id).apply()
        return true
    }

    fun installCompletedDownload(context: Context, id: Long) {
        val expected = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_ID, -1)
        if (id != expected) return
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = manager.getUriForDownloadedFile(id) ?: return
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            UpdateInstaller.installCompletedDownload(
                context,
                intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            )
        }
    }
}
