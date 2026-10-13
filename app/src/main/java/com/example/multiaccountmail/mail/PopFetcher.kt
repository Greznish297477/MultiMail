package com.example.multiaccountmail.mail

import com.example.multiaccountmail.data.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Properties
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.InternetAddress

/** POP3S предназначен только для чтения входящих: папки и серверные действия POP3 не поддерживает. */
object PopFetcher {
    /** Быстрая проверка POP3S без чтения списка писем. */
    suspend fun verify(account: Account): String? = withContext(Dispatchers.IO) {
        val password = account.appPassword ?: return@withContext "Укажите пароль приложения"
        val host = account.imapHost?.takeIf { it.isNotBlank() }
            ?: "pop.${account.email.substringAfter('@')}"
        var store: Store? = null
        try {
            val port = account.imapPort.takeIf { it > 0 } ?: 995
            val properties = Properties().apply {
                put("mail.store.protocol", "pop3s")
                put("mail.pop3s.host", host)
                put("mail.pop3s.port", port.toString())
                put("mail.pop3s.ssl.enable", "true")
                put("mail.pop3s.ssl.trust", "*")
                put("mail.pop3s.connectiontimeout", "10000")
                put("mail.pop3s.timeout", "15000")
            }
            store = Session.getInstance(properties).getStore("pop3s")
            store.connect(host, port, account.email, password)
            null
        } catch (e: Exception) {
            "${e.message ?: "Не удалось подключиться к POP3"}. Проверь пароль приложения и сервер."
        } finally {
            runCatching { store?.close() }
        }
    }

    suspend fun fetch(account: Account, folder: MailFolder, max: Int = Int.MAX_VALUE): FetchResult =
        withContext(Dispatchers.IO) {
            if (folder !in listOf(MailFolder.INBOX, MailFolder.UNIFIED, MailFolder.UNREAD)) {
                return@withContext FetchResult(error = "POP3 поддерживает только входящие письма")
            }
            val password = account.appPassword
                ?: return@withContext FetchResult(error = "Для POP3 укажите пароль приложения")
            val host = account.imapHost?.takeIf { it.isNotBlank() }
                ?: "pop.${account.email.substringAfter('@')}"
            var store: Store? = null
            var inbox: Folder? = null
            try {
                val properties = Properties().apply {
                    put("mail.store.protocol", "pop3s")
                    put("mail.pop3s.host", host)
                    put("mail.pop3s.port", account.imapPort.takeIf { it > 0 }?.toString() ?: "995")
                    put("mail.pop3s.ssl.enable", "true")
                    put("mail.pop3s.ssl.trust", "*")
                    put("mail.pop3s.connectiontimeout", "20000")
                    put("mail.pop3s.timeout", "30000")
                }
                store = Session.getInstance(properties).getStore("pop3s")
                store.connect(host, account.imapPort.takeIf { it > 0 } ?: 995, account.email, password)
                inbox = store.getFolder("INBOX")
                inbox.open(Folder.READ_ONLY)
                val count = inbox.messageCount
                val start = if (max == Int.MAX_VALUE) 1 else maxOf(1, count - max + 1)
                val dateFormat = SimpleDateFormat("dd MMM HH:mm", Locale("ru"))
                val messages: List<MailMessage> = if (count == 0) emptyList() else inbox.getMessages(start, count)
                    .toList()
                    .asReversed()
                    .mapNotNull { message ->
                        val unread = !message.isSet(Flags.Flag.SEEN)
                        if (folder == MailFolder.UNREAD && !unread) return@mapNotNull null
                        val from = message.from?.firstOrNull() as? InternetAddress
                        MailMessage(
                            id = "pop-${account.id}-${message.messageNumber}",
                            fromName = from?.personal?.takeIf { it.isNotBlank() }
                                ?: from?.address?.substringBefore("@") ?: "Неизвестно",
                            fromEmail = from?.address.orEmpty(),
                            subject = message.subject?.ifBlank { null } ?: "(без темы)",
                            snippet = "",
                            date = message.sentDate?.let(dateFormat::format).orEmpty(),
                            timestamp = message.sentDate?.time ?: message.receivedDate?.time ?: 0L,
                            isUnread = unread,
                            folder = MailFolder.INBOX.title,
                            accountEmail = account.email,
                            accountId = account.id,
                            providerLabel = "POP3"
                        )
                    }
                FetchResult(messages)
            } catch (e: Exception) {
                FetchResult(error = "${e.message ?: "Ошибка POP3"}. Проверь POP3-сервер и пароль приложения.")
            } finally {
                runCatching { inbox?.close(false) }
                runCatching { store?.close() }
            }
        }
}
