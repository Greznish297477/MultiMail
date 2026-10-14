package com.example.multiaccountmail.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.multiaccountmail.data.AccountRepository
import com.example.multiaccountmail.data.TokenStorage
import com.example.multiaccountmail.mail.MailRepository
import com.example.multiaccountmail.notification.NotificationHelper
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Реальная проверка непрочитанных по всем сохранённым ящикам.
 * Демо-уведомлений нет: пуш только если сервер вернул новые письма.
 */
class MailSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = AccountRepository(applicationContext)
            val tokens = TokenStorage(applicationContext)
            val accounts = repository.snapshot().map { tokens.hydrate(it) }
            if (accounts.isEmpty()) return Result.success()

            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val fresh = mutableListOf<com.example.multiaccountmail.mail.MailMessage>()

            // Один медленный корпоративный сервер больше не блокирует уведомления
            // для остальных ящиков: подключения выполняются параллельно.
            val results = coroutineScope {
                accounts.map { account ->
                    async {
                        if (!account.canFetchMail() && account.appPassword.isNullOrBlank() && account.accessToken.isNullOrBlank()
                            && account.provider != com.example.multiaccountmail.data.Provider.GOOGLE
                        ) return@async null
                        account to MailRepository.unreadPreview(applicationContext, account)
                    }
                }.awaitAll().filterNotNull()
            }
            results.forEach { (account, result) ->
                if (result.error != null) return@forEach
                val known = prefs.getStringSet(key(account.id), emptySet()).orEmpty()
                val initialized = prefs.getBoolean("initialized_${account.id}", false)
                val ids = result.messages.map { it.id }.toSet()
                if (!initialized) {
                    // Первую успешную проверку считаем базовой: старые письма не уведомляем.
                    prefs.edit().putStringSet(key(account.id), ids)
                        .putBoolean("initialized_${account.id}", true).apply()
                    return@forEach
                }
                val newcomers = result.messages.filter { it.isUnread && it.id !in known }
                fresh += newcomers
                // Запоминаем и уже прочитанные: последующее изменение флага не
                // должно ошибочно превращать старое письмо в новое уведомление.
                if (ids.isNotEmpty()) prefs.edit().putStringSet(key(account.id), known + ids).apply()
            }

            if (fresh.isNotEmpty()) {
                fresh.forEach { NotificationHelper.showMailNotification(applicationContext, it) }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun key(accountId: String) = "seen_$accountId"

    companion object {
        private const val WORK_NAME = "mail_sync_periodic"
        private const val PREFS = "mail_sync_state"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<MailSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /** Запускает короткую проверку сразу, не дожидаясь периодического окна WorkManager. */
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<MailSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "mail_sync_now",
                // Не отменяем уже выполняющуюся проверку, если Activity несколько
                // раз получила onResume подряд.
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
