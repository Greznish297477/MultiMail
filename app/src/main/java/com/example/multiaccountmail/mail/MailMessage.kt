package com.example.multiaccountmail.mail

data class MailMessage(
    val id: String,
    val fromName: String,
    val fromEmail: String,
    val subject: String,
    val snippet: String,
    val body: String = "",
    val date: String,
    val timestamp: Long = 0L,
    val isUnread: Boolean = false,
    val isStarred: Boolean = false,
    val category: MailCategory = MailCategory.PRIMARY,
    val folder: String = "INBOX",
    val accountEmail: String = "",
    val accountId: String = "",
    val providerLabel: String = ""
)

enum class MailCategory(val title: String) {
    PRIMARY("Основные"),
    PROMOTIONS("Промоакции"),
    SOCIAL("Соцсети"),
    UPDATES("Оповещения")
}

enum class MailFolder(val title: String, val imapName: String, val gmailLabel: String?) {
    UNIFIED("Все ящики", "INBOX", "INBOX"),
    INBOX("Входящие", "INBOX", "INBOX"),
    UNREAD("Непрочитанные", "INBOX", "UNREAD"),
    STARRED("С звёздочкой", "INBOX", "STARRED"),
    SENT("Отправленные", "Sent", "SENT"),
    DRAFTS("Черновики", "Drafts", "DRAFT"),
    SPAM("Спам", "Spam", "SPAM"),
    TRASH("Корзина", "Trash", "TRASH");

    companion object {
        fun fromTitle(title: String): MailFolder =
            entries.find { it.title == title } ?: INBOX
    }
}

data class DiskItem(
    val name: String,
    val type: String,
    val size: String,
    val path: String,
    val modified: String = ""
)

data class ChatSpace(
    val name: String,
    val displayName: String,
    val lastMessage: String,
    val time: String
)

data class FetchResult(
    val messages: List<MailMessage> = emptyList(),
    val error: String? = null
)
