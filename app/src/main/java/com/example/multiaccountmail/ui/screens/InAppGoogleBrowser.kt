package com.example.multiaccountmail.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Вход Google для создания пароля приложения на устройствах без GMS/microG. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppGoogleBrowser(onClose: () -> Unit) {
    var view by remember { mutableStateOf<WebView?>(null) }
    BackHandler {
        val browser = view
        if (browser?.canGoBack() == true) browser.goBack() else onClose()
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                val browser = view
                if (browser?.canGoBack() == true) browser.goBack() else onClose()
            }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
            Text("Вход Google", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Закрыть") }
        }
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.userAgentString = settings.userAgentString.replace("; wv", "")
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        loadUrl("https://myaccount.google.com/apppasswords")
                        view = this
                    }
                }
            )
        }
    }
}
