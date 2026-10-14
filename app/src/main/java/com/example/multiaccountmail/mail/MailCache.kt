package com.example.multiaccountmail.mail

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Последний успешный список писем. Экран открывается из кэша,
 * сеть только обновляет его.
 */
object MailCache {

    private const val PREFS = "mail_list_cache"

    fun key(accountIds: List<String>, folder: MailFolder, unified: Boolean): String {
        val ids = accountIds.sorted().joinToString(",")
        return "${if (unified) "all" else "one"}|${folder.name}|$ids"
    }

    fun load(context: Context, cacheKey: String): List<MailMessage> {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(cacheKey, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        MailMessage(
                            id = o.getString("id"),
                            fromName = o.optString("fromName"),
                            fromEmail = o.optString("fromEmail"),
                            subject = o.optString("subject"),
                            snippet = o.optString("snippet"),
                            body = o.optString("body"),
                            date = o.optString("date"),
                            timestamp = o.optLong("timestamp"),
                            isUnread = o.optBoolean("unread"),
                            isStarred = o.optBoolean("starred"),
                            category = runCatching { MailCategory.valueOf(o.optString("category")) }
                                .getOrDefault(MailCategory.PRIMARY),
                            folder = o.optString("folder"),
                            accountEmail = o.optString("accountEmail"),
                            accountId = o.optString("accountId"),
                            providerLabel = o.optString("provider")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, cacheKey: String, messages: List<MailMessage>) {
        val arr = JSONArray()
        messages.forEach { m ->
            arr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("fromName", m.fromName)
                    put("fromEmail", m.fromEmail)
                    put("subject", m.subject)
                    put("snippet", m.snippet)
                    put("body", m.body)
                    put("date", m.date)
                    put("timestamp", m.timestamp)
                    put("unread", m.isUnread)
                    put("starred", m.isStarred)
                    put("category", m.category.name)
                    put("folder", m.folder)
                    put("accountEmail", m.accountEmail)
                    put("accountId", m.accountId)
                    put("provider", m.providerLabel)
                }
            )
        }
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(cacheKey, arr.toString())
            .apply()
    }
}
