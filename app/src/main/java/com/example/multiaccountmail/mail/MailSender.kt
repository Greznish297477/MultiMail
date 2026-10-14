package com.example.multiaccountmail.mail

import android.content.Context
import android.net.Uri
import com.example.multiaccountmail.data.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

object MailSender {

    suspend fun send(
        context: Context,
        account: Account,
        to: String,
        subject: String,
        body: String,
        attachments: List<Uri> = emptyList()
    ): String? = withContext(Dispatchers.IO) {
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
                put("mail.smtp.ssl.enable", (account.smtpPort == 465).toString())
                put("mail.smtp.starttls.enable", (account.smtpPort != 465).toString())
                put("mail.smtp.starttls.required", (account.smtpPort != 465).toString())
                put("mail.smtp.connectiontimeout", "20000")
                put("mail.smtp.timeout", "20000")
            }
            val session = Session.getInstance(props)
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(account.email, account.displayName))
                setRecipients(Message.RecipientType.TO, recipients.joinToString(","))
                setSubject(subject.ifBlank { "(без темы)" }, "UTF-8")
                if (attachments.isEmpty()) {
                    setText(body, "UTF-8")
                } else {
                    val multipart = MimeMultipart()
                    multipart.addBodyPart(MimeBodyPart().apply { setText(body, "UTF-8") })
                    attachments.forEach { uri ->
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw IllegalArgumentException("Не удалось открыть вложение")
                        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
                        } ?: "attachment"
                        multipart.addBodyPart(MimeBodyPart().apply {
                            dataHandler = javax.activation.DataHandler(
                                ByteArrayDataSource(bytes, context.contentResolver.getType(uri) ?: "application/octet-stream")
                            )
                            this.fileName = fileName
                        })
                    }
                    setContent(multipart)
                }
            }
            Transport.send(message, account.email, password)
            null
        } catch (e: Exception) {
            e.message ?: "Не удалось отправить"
        }
    }
}
