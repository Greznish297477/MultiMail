package com.example.multiaccountmail.data

import androidx.compose.ui.graphics.Color

enum class MailProtocol(val title: String) { IMAP("IMAP"), POP3("POP3") }

enum class Provider(
    val displayName: String,
    val primaryColor: Color,
    val supportsChat: Boolean,
    val supportsDrive: Boolean
) {
    GOOGLE("Google", Color(0xFF4285F4), true, true),
    YANDEX("Yandex", Color(0xFFFC3F1D), true, true),
    MAILRU("Mail.ru", Color(0xFF168DE2), false, true),
    OUTLOOK("Outlook", Color(0xFF0078D4), false, true),
    OTHER("Другое", Color(0xFF757575), false, false);

    companion object {
        fun fromName(name: String): Provider =
            entries.find { it.name == name } ?: OTHER
    }
}

/**
 * IMAP-настройки для ручного входа (пароль приложения).
 * OAuth-аккаунты Google/Yandex/Mail.ru используют токен, а не пароль.
 */
data class ImapConfig(
    val host: String,
    val port: Int = 993,
    val smtpHost: String = "",
    val smtpPort: Int = 465,
    val username: String
)

data class Account(
    val id: String,
    val email: String,
    val displayName: String,
    val provider: Provider,
    val avatarUrl: String? = null,
    val order: Int = 0,
    val isActive: Boolean = false,
    val accessToken: String? = null,
    val idToken: String? = null,
    val refreshToken: String? = null,
    val appPassword: String? = null,
    val protocol: MailProtocol = MailProtocol.IMAP,
    val imapHost: String? = null,
    val imapPort: Int = 993,
    val smtpHost: String? = null,
    val smtpPort: Int = 465
) {
    fun imapConfig(): ImapConfig? {
        if (appPassword.isNullOrBlank()) return null
        val host = imapHost?.takeIf { it.isNotBlank() } ?: defaultImapHost() ?: return null
        return ImapConfig(
            host = host,
            port = imapPort,
            smtpHost = smtpHost?.takeIf { it.isNotBlank() } ?: defaultSmtpHost().orEmpty(),
            smtpPort = smtpPort,
            username = email
        )
    }

    fun defaultImapHost(): String? = when (provider) {
        Provider.GOOGLE -> "imap.gmail.com"
        Provider.YANDEX -> "imap.yandex.ru"
        Provider.MAILRU -> "imap.mail.ru"
        Provider.OUTLOOK -> "outlook.office365.com"
        Provider.OTHER -> imapHost
    }

    fun defaultSmtpHost(): String? = when (provider) {
        Provider.GOOGLE -> "smtp.gmail.com"
        Provider.YANDEX -> "smtp.yandex.ru"
        Provider.MAILRU -> "smtp.mail.ru"
        Provider.OUTLOOK -> "smtp.office365.com"
        Provider.OTHER -> smtpHost
    }

    fun canFetchMail(): Boolean =
        provider == Provider.GOOGLE ||
            !accessToken.isNullOrBlank() ||
            !appPassword.isNullOrBlank()
}
