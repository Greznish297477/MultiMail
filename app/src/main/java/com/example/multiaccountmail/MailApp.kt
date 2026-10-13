package com.example.multiaccountmail

import android.app.Application
import com.example.multiaccountmail.notification.NotificationHelper
import com.example.multiaccountmail.sync.MailSyncWorker
import com.example.multiaccountmail.ui.theme.AppearanceStore
import com.example.multiaccountmail.update.UpdateInstaller

class MailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppearanceStore.init(this)
        NotificationHelper.createChannels(this)
        UpdateInstaller.resumePendingInstall(this)
        // Запускаем периодическую синхронизацию (работает и на Huawei)
        MailSyncWorker.schedule(this)
    }
}
