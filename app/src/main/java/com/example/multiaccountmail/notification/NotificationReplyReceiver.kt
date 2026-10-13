package com.example.multiaccountmail.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.multiaccountmail.data.AccountRepository
import com.example.multiaccountmail.data.TokenStorage
import com.example.multiaccountmail.mail.MailSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Отправляет быстрый ответ, введённый прямо в системном уведомлении. */
class NotificationReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(NotificationHelper.REPLY_KEY)
            ?.toString()
            ?.trim()
            .orEmpty()
        val accountId = intent.getStringExtra(NotificationHelper.EXTRA_ACCOUNT_ID).orEmpty()
        val recipient = intent.getStringExtra(NotificationHelper.EXTRA_RECIPIENT).orEmpty()
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0)
        if (text.isBlank() || accountId.isBlank() || recipient.isBlank()) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tokens = TokenStorage(context.applicationContext)
                val account = AccountRepository(context.applicationContext).snapshot()
                    .firstOrNull { it.id == accountId }
                    ?.let(tokens::hydrate)
                if (account != null) MailSender.send(account, recipient, "Re:", text)
            } finally {
                NotificationManagerCompat.from(context).cancel(notificationId)
                pending.finish()
            }
        }
    }
}
