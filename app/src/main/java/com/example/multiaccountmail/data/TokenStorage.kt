package com.example.multiaccountmail.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Токены и пароли приложений. Сначала EncryptedSharedPreferences,
 * при сбое Keystore — обычные prefs, чтобы вход не ломался на части прошивок.
 */
class TokenStorage(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    fun saveTokens(
        accountId: String,
        accessToken: String?,
        idToken: String? = null,
        refreshToken: String? = null,
        appPassword: String? = null
    ) {
        prefs.edit().apply {
            if (accessToken != null) putString(key(accountId, "access"), accessToken)
            if (idToken != null) putString(key(accountId, "id"), idToken)
            if (refreshToken != null) putString(key(accountId, "refresh"), refreshToken)
            if (appPassword != null) putString(key(accountId, "password"), appPassword)
        }.apply()
    }

    fun getAccessToken(accountId: String): String? = prefs.getString(key(accountId, "access"), null)
    fun getIdToken(accountId: String): String? = prefs.getString(key(accountId, "id"), null)
    fun getRefreshToken(accountId: String): String? = prefs.getString(key(accountId, "refresh"), null)
    fun getAppPassword(accountId: String): String? = prefs.getString(key(accountId, "password"), null)

    fun clearTokens(accountId: String) {
        prefs.edit()
            .remove(key(accountId, "access"))
            .remove(key(accountId, "id"))
            .remove(key(accountId, "refresh"))
            .remove(key(accountId, "password"))
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /** Подмешивает секреты к модели аккаунта (они не лежат в открытом JSON). */
    fun hydrate(account: Account): Account = account.copy(
        accessToken = getAccessToken(account.id) ?: account.accessToken,
        idToken = getIdToken(account.id) ?: account.idToken,
        refreshToken = getRefreshToken(account.id) ?: account.refreshToken,
        appPassword = getAppPassword(account.id) ?: account.appPassword
    )

    private fun key(accountId: String, kind: String) = "${accountId}_$kind"

    companion object {
        private fun createPrefs(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    "secure_tokens",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (_: Exception) {
                context.getSharedPreferences("secure_tokens_fallback", Context.MODE_PRIVATE)
            }
        }
    }
}
