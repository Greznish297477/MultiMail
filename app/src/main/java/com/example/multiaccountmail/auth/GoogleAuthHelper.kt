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
import com.google.api.services.drive.DriveScopes

/**
 * Помощник для Google Sign-In.
 *
 * Android Client ID: 730720800458-bh94728mt4hct9frhuigicj2ru8u3blu.apps.googleusercontent.com
 * Web Client ID:     730720800458-o89om3n1r9cgb1adk983b4u9gke4ugd1.apps.googleusercontent.com
 */
object GoogleAuthHelper {

    // Android Client ID (привязан к package name + SHA-1)
    const val ANDROID_CLIENT_ID = "730720800458-bh94728mt4hct9frhuigicj2ru8u3blu.apps.googleusercontent.com"

    // Web Client ID — используется для requestIdToken и requestServerAuthCode
    const val WEB_CLIENT_ID = "730720800458-o89om3n1r9cgb1adk983b4u9gke4ugd1.apps.googleusercontent.com"

    // Скоупы: Gmail + Drive + Google Chat
    private val SCOPES = listOf(
        Scope(GmailScopes.GMAIL_READONLY),
        Scope(GmailScopes.GMAIL_SEND),
        Scope(GmailScopes.GMAIL_MODIFY),
        Scope(DriveScopes.DRIVE_READONLY),
        Scope(DriveScopes.DRIVE_FILE),
        // Google Chat
        Scope("https://www.googleapis.com/auth/chat.messages.readonly"),
        Scope("https://www.googleapis.com/auth/chat.spaces.readonly")
    )

    fun getSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken(WEB_CLIENT_ID)          // обязательно Web Client ID
            .requestServerAuthCode(WEB_CLIENT_ID)   // для offline access (refresh token)
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
    fun handleSignInResult(data: Intent?): GoogleSignInAccount? {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            e.printStackTrace()
            null
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
}
