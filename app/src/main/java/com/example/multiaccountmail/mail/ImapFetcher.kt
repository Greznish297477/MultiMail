package com.example.multiaccountmail.mail

import android.util.Log
import android.text.Html
import com.example.multiaccountmail.data.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Properties
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMultipart

/**
 * Реальный IMAP: XOAUTH2 (Яндекс / Mail.ru) и пароль приложения (любой ящик).
 */
object ImapFetcher {

    private const val TAG = "ImapFetcher"

    suspend fun fetch(account: Account, folder: MailFolder, max: Int = Int.MAX_VALUE): FetchResult {
        val password = account.appPassword
        val token = account.accessToken
        val host = account.imapConfig()?.host ?: account.defaultImapHost()
        if (host.isNullOrBlank()) {
            return FetchResult(error = "Не задан IMAP-сервер для ${account.email}")
        }
        if (password.isNullOrBlank() && token.isNullOrBlank()) {
            return FetchResult(error = "Нет пароля или токена для ${account.email}")
        }
        val secret = if (!password.isNullOrBlank()) password else token!!
        val oauth = password.isNullOrBlank()
        val hosts = hostsFor(account, host)
        var last = FetchResult(error = "Не удалось подключиться")
        for (candidate in hosts.distinct()) {
            last = fetchImap(
                host = candidate,
                port = account.imapPort.takeIf { it > 0 } ?: 993,
                email = account.email,
                secret = secret,
                useOAuth = oauth,
                folder = folder,
                max = max,
                account = account
            )
            if (last.error == null) return last
        }
        return last
    }

    /** Быстрая проверка реквизитов при добавлении ящика: без открытия папки и загрузки писем. */
    suspend fun verify(account: Account): String? = withContext(Dispatchers.IO) {
        val password = account.appPassword ?: return@withContext "Укажите пароль приложения"
        val host = account.imapHost?.takeIf { it.isNotBlank() } ?: account.defaultImapHost()
            ?: return@withContext "Не задан IMAP-сервер"
        var store: Store? = null
        try {
            val port = account.imapPort.takeIf { it > 0 } ?: 993
            store = Session.getInstance(imapProperties(host, port, oauth = false)).getStore("imaps")
            val login = if (host.contains("yandex")) account.email.substringBefore("@") else account.email
            store.connect(host, port, login, password)
            null
        } catch (e: Exception) {
            "${e.message ?: "Не удалось подключиться к IMAP"}. Проверь пароль приложения и сервер."
        } finally {
            runCatching { store?.close() }
        }
    }

    suspend fun fetchYandex(accessToken: String, email: String, max: Int = 25): List<MailMessage> {
        return fetchImap(
            host = "imap.yandex.ru",
            port = 993,
            email = email,
            secret = accessToken,
            useOAuth = true,
            folder = MailFolder.INBOX,
            max = max,
            account = null
        ).messages
    }

    suspend fun fetchMailRu(accessToken: String, email: String, max: Int = 25): List<MailMessage> {
        return fetchImap(
            host = "imap.mail.ru",
            port = 993,
            email = email,
            secret = accessToken,
            useOAuth = true,
            folder = MailFolder.INBOX,
            max = max,
            account = null
        ).messages
    }

    private suspend fun fetchImap(
        host: String,
        port: Int,
        email: String,
        secret: String,
        useOAuth: Boolean,
        folder: MailFolder,
        max: Int,
        account: Account?
    ): FetchResult = withContext(Dispatchers.IO) {
        var store: Store? = null
        var box: Folder? = null
        try {
            val login = if (host.contains("yandex")) email.substringBefore("@") else email
            val attempts = listOf(login, email).distinct()
            var connected = false
            var lastError: Exception? = null
            for (name in attempts) {
                runCatching { store?.close() }
                val next = Session.getInstance(imapProperties(host, port, oauth = false)).getStore("imaps")
                store = next
                try {
                    next.connect(host, port, name, secret)
                    connected = true
                    break
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "IMAP login $name@$host failed: ${e.message}")
                }
            }
            if (!connected) throw lastError ?: IllegalStateException("IMAP не подключился")
            val connectedStore = store ?: throw IllegalStateException("IMAP не подключился")

            val opened = openFolder(connectedStore, folder)
            box = opened
            val count = opened.messageCount
            if (count <= 0) {
                return@withContext FetchResult(emptyList())
            }
            val start = if (max == Int.MAX_VALUE) 1 else maxOf(1, count - max + 1)
            val dateFmt = SimpleDateFormat("dd MMM HH:mm", Locale("ru"))
            val result = ArrayList<MailMessage>(count - start + 1)
            var end = count
            while (end >= start) {
                val chunkStart = maxOf(start, end - 49)
                val messages = opened.getMessages(chunkStart, end)
                val profile = javax.mail.FetchProfile().apply {
                    add(javax.mail.FetchProfile.Item.ENVELOPE)
                    add(javax.mail.FetchProfile.Item.FLAGS)
                }
                opened.fetch(messages, profile)
                for (index in messages.indices.reversed()) {
                    val msg = messages[index]
                    mapMessage(msg, folder, email, host, account, dateFmt, chunkStart + index)?.let(result::add)
                }
                end = chunkStart - 1
            }
            FetchResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "IMAP $host $email", e)
            val hint = if (useOAuth) {
                "Для Яндекса нужен пароль приложения: id.yandex.ru → Безопасность → Пароли приложений, тип Почта."
            } else {
                "Проверь пароль приложения и IMAP-сервер."
            }
            FetchResult(error = (e.message ?: "IMAP ошибка") + ". " + hint)
        } finally {
            runCatching { box?.close(false) }
            runCatching { store?.close() }
        }
    }

    private fun mapMessage(
        msg: Message,
        folder: MailFolder,
        email: String,
        host: String,
        account: Account?,
        dateFmt: SimpleDateFormat,
        number: Int
    ): MailMessage? {
        return try {
            val unread = !msg.isSet(Flags.Flag.SEEN)
            val starred = msg.isSet(Flags.Flag.FLAGGED)
            if (folder == MailFolder.UNREAD && !unread) return null
            if (folder == MailFolder.STARRED && !starred) return null
            val from = msg.from?.firstOrNull() as? InternetAddress
            val fromName = from?.personal?.takeIf { it.isNotBlank() }
                ?: from?.address?.substringBefore("@")
                ?: "Неизвестно"
            val fromEmail = from?.address.orEmpty()
            val stamp = msg.sentDate?.time ?: msg.receivedDate?.time ?: 0L
            val body = messageText(msg)
            MailMessage(
                id = "${account?.id ?: host}-$number",
                fromName = fromName,
                fromEmail = fromEmail,
                subject = msg.subject?.ifBlank { null } ?: "(без темы)",
                snippet = previewText(body),
                body = body,
                date = msg.sentDate?.let { dateFmt.format(it) }.orEmpty(),
                timestamp = stamp,
                isUnread = unread,
                isStarred = starred,
                category = guessCategory(fromEmail, msg.subject.orEmpty()),
                folder = folder.title,
                accountEmail = email,
                accountId = account?.id.orEmpty(),
                providerLabel = account?.provider?.displayName.orEmpty()
            )
        } catch (e: Exception) {
            Log.w(TAG, "skip message", e)
            null
        }
    }

    /** Берёт HTML или текст письма, сохраняя стили/фон HTML для экрана чтения. */
    private fun messageText(message: Message): String = runCatching {
        messageParts(message).firstOrNull { it.first }?.second
            ?: messageParts(message).firstOrNull()?.second.orEmpty()
    }.getOrDefault("")

    private fun messageParts(part: javax.mail.Part): List<Pair<Boolean, String>> = runCatching {
        when {
            part.isMimeType("text/html") -> listOf(true to (part.content as? String).orEmpty())
            part.isMimeType("text/plain") -> listOf(false to (part.content as? String).orEmpty())
            part.content is MimeMultipart -> {
                val multipart = part.content as MimeMultipart
                (0 until multipart.count).flatMap { messageParts(multipart.getBodyPart(it)) }
            }
            else -> emptyList()
        }
    }.getOrDefault(emptyList())

    private fun previewText(value: String): String {
        val plain = if (value.contains("<", ignoreCase = false)) {
            Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
        } else value
        return plain.replace(Regex("\\s+"), " ").trim().take(160)
    }

    private fun hostsFor(account: Account, host: String): List<String> =
        CorporateMail.hosts(account.email, account.provider, host)

    private fun imapProperties(host: String, port: Int, oauth: Boolean) = Properties().apply {
        put("mail.store.protocol", "imaps")
        put("mail.imaps.host", host)
        put("mail.imaps.port", port.toString())
        put("mail.imaps.ssl.enable", "true")
        put("mail.imaps.ssl.trust", "*")
        put("mail.imaps.starttls.enable", "true")
        put("mail.imaps.auth", "true")
        put("mail.imaps.connectiontimeout", "10000")
        put("mail.imaps.timeout", "15000")
        put("mail.imaps.partialfetch", "false")
        if (oauth) {
            put("mail.imaps.auth.mechanisms", "XOAUTH2")
            put("mail.imaps.sasl.enable", "true")
            put("mail.imaps.sasl.mechanisms", "XOAUTH2")
        }
    }

    private fun openFolder(store: Store, folder: MailFolder): Folder {
        val candidates = when (folder) {
            MailFolder.SENT -> listOf(
                "Sent", "[Gmail]/Sent Mail", "Отправленные", "INBOX.Sent", "Sent Items", "Sent Messages"
            )
            MailFolder.DRAFTS -> listOf(
                "Drafts", "[Gmail]/Drafts", "Черновики", "INBOX.Drafts"
            )
            MailFolder.SPAM -> listOf(
                "Spam", "Junk", "[Gmail]/Spam", "Спам", "INBOX.Spam", "Junk E-mail"
            )
            MailFolder.TRASH -> listOf(
                "Trash", "[Gmail]/Trash", "Корзина", "INBOX.Trash", "Deleted", "Deleted Items"
            )
            else -> listOf("INBOX")
        }
        val names = LinkedHashSet<String>()
        names += candidates
        runCatching { store.defaultFolder.list("*") }.getOrNull()?.forEach { existing ->
            val full = existing.fullName
            val low = full.lowercase(Locale.US)
            val matches = when (folder) {
                MailFolder.SENT -> low.contains("sent") || full.contains("Отправлен")
                MailFolder.DRAFTS -> low.contains("draft") || full.contains("Черновик")
                MailFolder.SPAM -> low.contains("spam") || low.contains("junk") || full.contains("Спам")
                MailFolder.TRASH -> low.contains("trash") || low.contains("deleted") || full.contains("Корзин") || full.contains("Удален")
                else -> full.equals("INBOX", true)
            }
            if (matches) names += full
        }
        var last: Exception? = null
        for (name in names) {
            try {
                val f = store.getFolder(name)
                if (f.exists()) {
                    f.open(Folder.READ_ONLY)
                    return f
                }
            } catch (e: Exception) {
                last = e
            }
        }
        if (folder != MailFolder.INBOX && folder != MailFolder.UNREAD && folder != MailFolder.STARRED && folder != MailFolder.UNIFIED) {
            throw last ?: IllegalStateException("Папка «${folder.title}» не найдена на сервере")
        }
        val inbox = store.getFolder("INBOX")
        inbox.open(Folder.READ_ONLY)
        return inbox
    }

    private fun guessCategory(from: String, subject: String): MailCategory {
        val text = "$from $subject".lowercase(Locale.US)
        val social = listOf(
            "facebook", "instagram", "vk.com", "vkontakte", "twitter", "x.com", "linkedin",
            "tiktok", "telegram", "ok.ru", "pinterest", "reddit", "discord"
        )
        val promo = listOf(
            "promo", "sale", "скидк", "распродаж", "акци", "newsletter", "unsubscribe",
            "marketing", "offer", "купон", "advert", "noreply-deals", "digest"
        )
        val updates = listOf(
            "notify", "notification", "alert", "security", "billing", "invoice", "receipt",
            "statement", "noreply", "no-reply", "support", "уведом", "чек", "оплат", "заказ"
        )
        return when {
            social.any { text.contains(it) } -> MailCategory.SOCIAL
            promo.any { text.contains(it) } -> MailCategory.PROMOTIONS
            updates.any { text.contains(it) } -> MailCategory.UPDATES
            else -> MailCategory.PRIMARY
        }
    }

    private fun extractSnippet(msg: Message): String {
        return try {
            when (val content = msg.content) {
                is String -> content
                is MimeMultipart -> firstText(content)
                else -> ""
            }.replace(Regex("\\s+"), " ").trim().take(180)
        } catch (_: Exception) {
            ""
        }
    }

    private fun firstText(part: MimeMultipart): String {
        for (i in 0 until part.count) {
            val body = part.getBodyPart(i)
            val type = body.contentType.lowercase(Locale.US)
            when (val c = body.content) {
                is String -> if (type.contains("text/plain") || type.contains("text/html")) {
                    return c.replace(Regex("<[^>]+>"), " ")
                }
                is MimeMultipart -> {
                    val nested = firstText(c)
                    if (nested.isNotBlank()) return nested
                }
            }
        }
        return ""
    }
}
