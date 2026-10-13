package com.example.multiaccountmail.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.multiaccountmail.MainActivity
import com.example.multiaccountmail.R

/**
 * Уведомления, которые работают на всех устройствах, включая Huawei
 * (без зависимости от Google Play Services / FCM).
 *
 * Использует системный NotificationManager + WorkManager для фоновой проверки.
 * На Huawei/Xiaomi/Oppo дополнительно рекомендуется:
 *  - отключить оптимизацию батареи для приложения
 *  - разрешить автозапуск
 */
object NotificationHelper {

    const val CHANNEL_MAIL = "mail_new"
    const val CHANNEL_SYNC = "mail_sync"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val mailChannel = NotificationChannel(
                CHANNEL_MAIL,
                "Новые письма",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о новых письмах из всех ящиков"
                enableVibration(true)
                enableLights(true)
            }

            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                "Синхронизация",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновая проверка почты"
            }

            nm.createNotificationChannel(mailChannel)
            nm.createNotificationChannel(syncChannel)
        }
    }

    fun showNewMailNotification(
        context: Context,
        title: String,
        body: String,
        accountEmail: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_folder", "Все ящики")
        }
        val pending = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MAIL)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(accountEmail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Нет разрешения POST_NOTIFICATIONS
            e.printStackTrace()
        }
    }

    fun showUnifiedInboxNotification(
        context: Context,
        count: Int,
        preview: String
    ) {
        showNewMailNotification(
            context = context,
            title = if (count == 1) "Новое письмо" else "Новые письма: $count",
            body = preview,
            accountEmail = "Все ящики",
            notificationId = 1001
        )
    }
}
