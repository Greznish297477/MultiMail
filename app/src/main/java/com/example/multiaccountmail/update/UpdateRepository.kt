package com.example.multiaccountmail.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Укажите owner/repository после создания GitHub-репозитория.
 * Например: "ivanov/MultiMail". Пока значение пустое, проверка обновлений скрыта.
 */
object UpdateRepository {
    const val GITHUB_REPOSITORY = "Greznish297477/MultiMail"

    data class Release(
        val tag: String,
        val notes: String,
        val pageUrl: String,
        val apkUrl: String
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun latest(): Result<Release> = withContext(Dispatchers.IO) {
        if (GITHUB_REPOSITORY.isBlank()) return@withContext Result.failure(
            IllegalStateException("GitHub-репозиторий ещё не указан")
        )
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$GITHUB_REPOSITORY/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "MultiMail-Android")
                .build()
            client.newCall(request).execute().use { response ->
                require(response.isSuccessful) { "GitHub ответил: ${response.code}" }
                val json = JSONObject(response.body?.string().orEmpty())
                val assets: JSONArray = json.optJSONArray("assets") ?: JSONArray()
                val apkUrl = (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                    ?.optString("browser_download_url")
                    .orEmpty()
                Release(
                    tag = json.optString("tag_name"),
                    notes = json.optString("body").ifBlank { "Описание изменений не добавлено." },
                    pageUrl = json.optString("html_url"),
                    apkUrl = apkUrl
                )
            }
        }
    }
}
