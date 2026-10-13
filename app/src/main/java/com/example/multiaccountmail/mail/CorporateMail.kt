package com.example.multiaccountmail.mail

import com.example.multiaccountmail.data.Provider

/**
 * Серверы корпоративной почты. Один домен компании может жить
 * на Google Workspace, Microsoft 365, Яндекс 360, VK WorkSpace или своём IMAP.
 */
object CorporateMail {

    fun hosts(email: String, provider: Provider, explicitHost: String?): List<String> {
        val domain = email.substringAfter("@", "").lowercase().trim()
        val own = explicitHost?.trim()?.takeIf { it.isNotBlank() && it != "imap.$domain" }
        val detected = when {
            isYandex(domain) || provider == Provider.YANDEX -> listOf("imap.yandex.com", "imap.yandex.ru")
            isMailRu(domain) || provider == Provider.MAILRU -> listOf("imap.mail.ru")
            isMicrosoft(domain) || provider == Provider.OUTLOOK -> listOf("outlook.office365.com")
            isGoogle(domain) || provider == Provider.GOOGLE -> listOf("imap.gmail.com")
            domain.isBlank() -> emptyList()
            else -> listOf(
                "imap.$domain",
                "mail.$domain",
                "imap.gmail.com",
                "outlook.office365.com",
                "imap.yandex.ru",
                "imap.mail.ru"
            )
        }
        return (listOfNotNull(own) + detected).distinct()
    }

    fun smtp(email: String, provider: Provider, imapHost: String?): String {
        val domain = email.substringAfter("@", "").lowercase()
        return when {
            imapHost?.contains("yandex") == true || isYandex(domain) || provider == Provider.YANDEX -> "smtp.yandex.ru"
            imapHost?.contains("mail.ru") == true || isMailRu(domain) || provider == Provider.MAILRU -> "smtp.mail.ru"
            imapHost?.contains("office365") == true || isMicrosoft(domain) || provider == Provider.OUTLOOK -> "smtp.office365.com"
            imapHost?.contains("gmail") == true || isGoogle(domain) || provider == Provider.GOOGLE -> "smtp.gmail.com"
            imapHost?.startsWith("imap.") == true -> "smtp." + imapHost.removePrefix("imap.")
            imapHost?.startsWith("mail.") == true -> "smtp." + imapHost.removePrefix("mail.")
            domain.isNotBlank() -> "smtp.$domain"
            else -> ""
        }
    }

    fun providerFor(domain: String): Provider = when {
        isGoogle(domain) -> Provider.GOOGLE
        isYandex(domain) -> Provider.YANDEX
        isMailRu(domain) -> Provider.MAILRU
        isMicrosoft(domain) -> Provider.OUTLOOK
        else -> Provider.OTHER
    }

    private fun isGoogle(domain: String) =
        domain == "gmail.com" || domain == "googlemail.com"

    private fun isYandex(domain: String) =
        domain == "yandex.ru" || domain == "yandex.com" || domain == "ya.ru" ||
            domain.endsWith(".yandex.ru") || domain.endsWith(".ya.ru")

    private fun isMailRu(domain: String) =
        domain == "mail.ru" || domain == "bk.ru" || domain == "list.ru" || domain == "inbox.ru"

    private fun isMicrosoft(domain: String) =
        domain == "outlook.com" || domain == "hotmail.com" || domain == "live.com" ||
            domain.endsWith(".outlook.com")
}
