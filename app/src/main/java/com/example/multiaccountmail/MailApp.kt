package com.example.multiaccountmail

import android.app.Application
import com.example.multiaccountmail.notification.NotificationHelper
import com.example.multiaccountmail.sync.MailSyncWorker
import com.example.multiaccountmail.ui.theme.AppearanceStore

class MailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppearanceStore.init(this)
        NotificationHelper.createChannels(this)
        // Запускаем периодическую синхронизацию (работает и на Huawei)
        MailSyncWorker.schedule(this)
    }
}
