package com.example.multiaccountmail.mail

import android.content.Context
import com.example.multiaccountmail.data.Account
import com.example.multiaccountmail.data.Provider
import com.example.multiaccountmail.data.TokenStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Единая точка загрузки почты. Никаких демо-писем: только ответ сервера или ошибка.
 */
object MailRepository {

    suspend fun load(
        context: Context,
        accounts: List<Account>,
        tokenStorage: TokenStorage,
        folder: MailFolder,
        unified: Boolean
    ): FetchResult {
        val targets = if (unified) accounts else accounts.take(1)
        if (targets.isEmpty()) return FetchResult(error = "Нет выбранного аккаунта")

        val results = coroutineScope {
            targets.map { raw ->
                async {
                    val account = tokenStorage.hydrate(raw)
                    account to loadOne(context, account, folder)
                }
            }.awaitAll()
        }
        val all = results.flatMap { it.second.messages }
        val errors = results.mapNotNull { (account, result) ->
            result.error?.let { "${account.email}: $it" }
        }

        val sorted = all.sortedByDescending { it.timestamp }
        return FetchResult(
            messages = sorted,
            error = if (sorted.isEmpty()) errors.joinToString("\n").ifBlank { null } else null
        )
    }

    suspend fun loadOne(context: Context, account: Account, folder: MailFolder): FetchResult {
        return when {
            account.provider == Provider.GOOGLE && account.appPassword.isNullOrBlank() ->
                GmailFetcher.fetch(context, account, folder)
            account.provider == Provider.YANDEX ->
                ImapFetcher.fetch(account, folder)
            !account.appPassword.isNullOrBlank() || !account.accessToken.isNullOrBlank() ->
                ImapFetcher.fetch(account, folder)
            else -> FetchResult(
                error = "Для ${account.email} нет доступа. Войдите через OAuth или укажите пароль приложения."
            )
        }
    }

    /** Короткий список непрочитанных для фоновых уведомлений. */
    suspend fun unreadPreview(context: Context, account: Account): FetchResult {
        val folder = MailFolder.UNREAD
        val result = when {
            account.provider == Provider.GOOGLE && account.appPassword.isNullOrBlank() ->
                GmailFetcher.fetch(context, account, folder)
            else -> ImapFetcher.fetch(account, folder)
        }
        return result.copy(messages = result.messages.filter { it.isUnread })
    }
}
