package com.example.multiaccountmail.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections

private val cache = Collections.synchronizedMap(object : LinkedHashMap<String, ImageBitmap?>(128, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap?>?) = size > 200
})

private val personalDomains = setOf(
    "gmail.com", "googlemail.com", "yandex.ru", "yandex.com", "ya.ru",
    "mail.ru", "inbox.ru", "bk.ru", "list.ru", "internet.ru",
    "outlook.com", "hotmail.com", "live.com", "icloud.com", "me.com",
    "yahoo.com", "proton.me", "protonmail.com", "rambler.ru"
)

@Composable
fun SenderAvatar(name: String, email: String, size: Dp = 40.dp) {
    val domain = companyDomain(email)
    val logo = produceState(domain?.let { cache[it] }, domain) {
        if (domain != null && !cache.containsKey(domain)) value = loadLogo(domain)
    }.value
    val letter = name.ifBlank { email }.take(1).uppercase()
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        if (logo != null) {
            Image(logo, contentDescription = name, modifier = Modifier.size(size), contentScale = ContentScale.Crop)
        } else {
            Text(letter, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun companyDomain(email: String): String? {
    val domain = email.substringAfter("@", "").lowercase().trim()
    if (domain.isBlank() || "." !in domain || domain in personalDomains) return null
    return domain
}

private suspend fun loadLogo(domain: String): ImageBitmap? {
    cache[domain]?.let { return it }
    if (cache.containsKey(domain)) return null
    val bitmap = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL("https://www.google.com/s2/favicons?domain=$domain&sz=128").openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = true
            connection.inputStream.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }?.takeIf { it.width > 16 && it.height > 16 }?.asImageBitmap()
    cache[domain] = bitmap
    return bitmap
}
