package com.example.multiaccountmail.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.gmail.GmailScopes

/**
 * Помощник для Google Sign-In.
 *
 * Для Gmail достаточно Android OAuth Client в Google Cloud Console,
 * привязанного к package name и SHA-1 подписи приложения.
 */
object GoogleAuthHelper {

    // Только разрешения, которые использует почтовый клиент.
    private val SCOPES = listOf(
        Scope(GmailScopes.GMAIL_READONLY),
        Scope(GmailScopes.GMAIL_SEND),
        Scope(GmailScopes.GMAIL_MODIFY)
    )

    fun getSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .apply {
                SCOPES.forEach { requestScopes(it) }
            }
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(context: Context): Intent {
        return getSignInClient(context).signInIntent
    }

    /**
     * Обработка результата Google Sign-In.
     * Возвращает аккаунт или null при ошибке.
     */
    data class SignInResult(val account: GoogleSignInAccount?, val error: String? = null)

    fun handleSignInResult(data: Intent?): SignInResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            SignInResult(task.getResult(ApiException::class.java))
        } catch (e: ApiException) {
            e.printStackTrace()
            val message = when (e.statusCode) {
                7 -> "Нет подключения к Google. Проверь интернет."
                8 -> "Сбой Google Play Services (код 8). Нажми «Повторить вход». Если не поможет — обнови Сервисы Google Play и перезапусти приложение."
                10 -> "Google отклонил вход (код 10). Добавь SHA-1 текущей подписи и Android OAuth Client в Google Cloud Console."
                12500 -> "Google Sign-In не настроен для этого APK. Проверь package name и SHA-1 в Google Cloud Console."
                12501 -> "Вход Google отменён."
                else -> "Ошибка входа Google: ${e.statusCode}. ${e.localizedMessage.orEmpty()}"
            }
            SignInResult(null, message)
        }
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        getSignInClient(context).signOut().addOnCompleteListener {
            onComplete()
        }
    }

    fun chooseAccount(context: Context, onReady: (Intent) -> Unit) {
        // Сбрасываем прошлый выбор, чтобы при добавлении ящика всегда появился выбор аккаунта.
        getSignInClient(context).signOut().addOnCompleteListener {
            onReady(getSignInIntent(context))
        }
    }

    /** Сбрасывает только авторизацию MultiMail и заново открывает выбор Google-аккаунта. */
    fun retrySignIn(context: Context, onReady: (Intent) -> Unit) {
        getSignInClient(context).revokeAccess().addOnCompleteListener {
            onReady(getSignInIntent(context))
        }
    }
}
