package com.example.multiaccountmail.mail

import android.content.Context
import android.util.Log
import com.example.multiaccountmail.data.Account
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GmailFetcher {

    private const val TAG = "GmailFetcher"

    suspend fun fetchInbox(context: Context, maxResults: Int = 30): List<MailMessage> =
        fetch(context, null, MailFolder.INBOX, maxResults).messages

    suspend fun fetch(
        context: Context,
        account: Account?,
        folder: MailFolder,
        maxResults: Int = Int.MAX_VALUE
    ): FetchResult = withContext(Dispatchers.IO) {
        try {
            val signed = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext FetchResult(error = "Google не авторизован. Войдите снова.")

            if (account != null && !signed.email.equals(account.email, ignoreCase = true)) {
                // На устройстве активен другой Google-аккаунт — письма этого ящика не подменяем чужими.
                if (!account.appPassword.isNullOrBlank()) {
                    return@withContext ImapFetcher.fetch(account, folder, maxResults)
                }
                return@withContext FetchResult(
                    error = "Сейчас вошли как ${signed.email}. Откройте «Аккаунты» и войдите в ${account.email} снова."
                )
            }

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(GmailScopes.GMAIL_READONLY, GmailScopes.GMAIL_MODIFY)
            )
            credential.selectedAccount = signed.account

            val service = Gmail.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("MultiAccountMail").build()

            val label = folder.gmailLabel ?: "INBOX"
            val request = service.users().messages().list("me")
                .setMaxResults(minOf(500, maxResults.coerceAtLeast(1)).toLong())
            if (folder == MailFolder.UNREAD) {
                request.q = "is:unread"
                request.labelIds = listOf("INBOX")
            } else if (folder == MailFolder.STARRED) {
                request.q = "is:starred"
            } else {
                // INBOX возвращает все входящие категории Gmail: основные, личные,
                // рабочие письма и рассылки. Категория используется только для UI,
                // но никогда не является фильтром загрузки.
                if (folder == MailFolder.INBOX || folder == MailFolder.UNIFIED) {
                    // Явный запрос Gmail надёжнее labelIds: письма от обычных
                    // отправителей и письма без CATEGORY_* тоже попадают в список.
                    request.q = "in:inbox"
                } else {
                    request.labelIds = listOf(label)
                }
                request.includeSpamTrash = false
            }

            val refs = mutableListOf<com.google.api.services.gmail.model.Message>()
            var page: String? = null
            do {
                if (page != null) request.pageToken = page
                val list = request.execute()
                refs += list.messages.orEmpty().take((maxResults - refs.size).coerceAtLeast(0))
                page = list.nextPageToken
            } while (!page.isNullOrBlank() && refs.size < maxResults)
            val dateFmt = SimpleDateFormat("dd MMM HH:mm", Locale("ru"))
            val gate = Semaphore(6)
            val email = signed.email.orEmpty()
            val result = coroutineScope {
                refs.map { ref ->
                    async {
                        gate.withPermit {
                            try {
                                val full = service.users().messages().get("me", ref.id)
                                    .setFormat("metadata")
                                    .setMetadataHeaders(listOf("From", "Subject", "Date"))
                                    .execute()
                                val headers = full.payload?.headers.orEmpty()
                                fun header(name: String) = headers.find { it.name.equals(name, true) }?.value.orEmpty()
                                val (name, fromEmail) = parseFrom(header("From"))
                                val labels = full.labelIds.orEmpty()
                                val stamp = full.internalDate ?: 0L
                                MailMessage(
                                    id = "gmail-${full.id}",
                                    fromName = name,
                                    fromEmail = fromEmail,
                                    subject = header("Subject").ifBlank { "(без темы)" },
                                    snippet = full.snippet.orEmpty(),
                                    date = if (stamp > 0) dateFmt.format(Date(stamp)) else "",
                                    timestamp = stamp,
                                    isUnread = labels.contains("UNREAD"),
                                    isStarred = labels.contains("STARRED"),
                                    category = categoryFromLabels(labels),
                                    folder = folder.title,
                                    accountEmail = email,
                                    accountId = account?.id.orEmpty(),
                                    providerLabel = "Google"
                                )
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                Log.w(TAG, "skip gmail message", e)
                                null
                            }
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            // Gmail иногда возвращает одинаковое сообщение в нескольких ответах
            // при смене категорий. В списке оставляем каждое письмо один раз.
            FetchResult(result.distinctBy { it.id }.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "gmail", e)
            FetchResult(error = e.message ?: "Не удалось загрузить Gmail")
        }
    }

    private fun categoryFromLabels(labels: List<String>): MailCategory = when {
        labels.any { it == "CATEGORY_PROMOTIONS" } -> MailCategory.PROMOTIONS
        labels.any { it == "CATEGORY_SOCIAL" } -> MailCategory.SOCIAL
        labels.any { it == "CATEGORY_UPDATES" || it == "CATEGORY_FORUMS" } -> MailCategory.UPDATES
        else -> MailCategory.PRIMARY
    }

    private fun parseFrom(from: String): Pair<String, String> {
        val emailMatch = Regex("<([^>]+)>").find(from)
        val email = emailMatch?.groupValues?.get(1) ?: from.trim()
        val name = if (emailMatch != null) {
            from.substringBefore("<").trim().removeSurrounding("\"")
        } else {
            email.substringBefore("@")
        }
        return name.ifBlank { email } to email
    }
}
