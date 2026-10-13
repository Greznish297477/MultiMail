package com.example.multiaccountmail.mail

import com.example.multiaccountmail.data.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object MailSender {

    suspend fun send(account: Account, to: String, subject: String, body: String): String? = withContext(Dispatchers.IO) {
        val password = account.appPassword
        if (password.isNullOrBlank()) {
            return@withContext "Чтобы отправить из приложения, добавьте ящик с паролем приложения. Обычный пароль от почты сервер не примет."
        }
        val host = account.smtpHost?.takeIf { it.isNotBlank() } ?: account.defaultSmtpHost()
        if (host.isNullOrBlank()) return@withContext "Не задан SMTP-сервер"
        val recipients = to.split(',', ';').map { it.trim() }.filter { it.contains("@") }
        if (recipients.isEmpty()) return@withContext "Укажите адрес получателя"
        try {
            val props = Properties().apply {
                put("mail.smtp.host", host)
                put("mail.smtp.port", account.smtpPort.toString())
                put("mail.smtp.auth", "true")
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.ssl.trust", "*")
                put("mail.smtp.connectiontimeout", "20000")
                put("mail.smtp.timeout", "20000")
            }
            val session = Session.getInstance(props)
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(account.email, account.displayName))
                setRecipients(Message.RecipientType.TO, recipients.joinToString(","))
                setSubject(subject.ifBlank { "(без темы)" }, "UTF-8")
                setText(body, "UTF-8")
            }
            Transport.send(message, account.email, password)
            null
        } catch (e: Exception) {
            e.message ?: "Не удалось отправить"
        }
    }
}
