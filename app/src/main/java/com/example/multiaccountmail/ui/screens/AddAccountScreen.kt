package com.example.multiaccountmail.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.multiaccountmail.auth.GoogleAuthHelper
import com.example.multiaccountmail.data.AccountRepository
import com.example.multiaccountmail.data.Provider
import com.example.multiaccountmail.data.TokenStorage
import com.example.multiaccountmail.mail.CorporateMail
import com.example.multiaccountmail.mail.ImapFetcher
import com.example.multiaccountmail.mail.MailFolder
import com.example.multiaccountmail.ui.theme.appearance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    repository: AccountRepository,
    tokenStorage: TokenStorage,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val look = appearance()
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var manualEmail by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualPassword by remember { mutableStateOf("") }
    var manualHost by remember { mutableStateOf("") }
    var manualProvider by remember { mutableStateOf(Provider.OTHER) }

    fun saveSecrets(accountId: String, access: String?, refresh: String? = null, idToken: String? = null, password: String? = null) {
        tokenStorage.saveTokens(accountId, access, idToken, refresh, password)
        repository.updateTokens(accountId, access, refresh, idToken)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        if (result.resultCode != Activity.RESULT_OK) {
            statusMessage = "Вход Google отменён"
            return@rememberLauncherForActivityResult
        }
        val account = GoogleAuthHelper.handleSignInResult(result.data)
        val email = account?.email
        if (account == null || email == null) {
            statusMessage = "Google не вернул аккаунт. Проверь SHA-1 и Client ID в Cloud Console."
            return@rememberLauncherForActivityResult
        }
        val saved = repository.addAccount(
            email = email,
            displayName = account.displayName ?: email.substringBefore("@"),
            provider = Provider.GOOGLE,
            idToken = account.idToken,
            avatarUrl = account.photoUrl?.toString(),
            accessToken = account.serverAuthCode
        )
        saveSecrets(saved.id, account.serverAuthCode, idToken = account.idToken)
        statusMessage = "Добавлен Google: $email"
        onBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Добавить аккаунт") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            }
            Text("Google", style = MaterialTheme.typography.titleMedium)

            Card(
                onClick = {
                    isLoading = true
                    statusMessage = null
                    googleSignInLauncher.launch(GoogleAuthHelper.getSignInIntent(context))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(if (look.liquidGlass) 24.dp else 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (look.liquidGlass) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                    else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Google", style = MaterialTheme.typography.titleLarge)
                    Text("Вход в Gmail через аккаунт устройства")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Почта по IMAP", style = MaterialTheme.typography.titleMedium)
            Text(
                "Добавьте Яндекс, Mail.ru, Outlook, рабочий или другой ящик. Используйте пароль приложения или пароль от администратора.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ProviderPicker(manualProvider) { picked ->
                manualProvider = picked
                if (manualHost.isBlank() || manualHost in knownHosts) {
                    manualHost = picked.defaultHost()
                }
            }

            OutlinedTextField(
                value = manualEmail,
                onValueChange = {
                    manualEmail = it
                    manualProvider = CorporateMail.providerFor(it.substringAfter("@", "").lowercase())
                    val suggested = CorporateMail.hosts(it, manualProvider, null).firstOrNull().orEmpty()
                    if (manualHost.isBlank() || manualHost in knownHosts) manualHost = suggested
                },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = manualName,
                onValueChange = { manualName = it },
                label = { Text("Имя") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = manualPassword,
                onValueChange = { manualPassword = it },
                label = { Text("Пароль приложения") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = manualHost,
                onValueChange = { manualHost = it },
                label = { Text("IMAP-сервер") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val email = manualEmail.trim()
                    val password = manualPassword.trim()
                    val host = manualHost.trim().ifBlank {
                        CorporateMail.hosts(email, manualProvider, null).firstOrNull().orEmpty()
                    }
                    if (email.isBlank() || !email.contains("@") || password.isBlank() || host.isBlank()) {
                        statusMessage = "Нужны email, пароль приложения и IMAP-сервер"
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        statusMessage = "Проверяю $host..."
                        val probe = com.example.multiaccountmail.data.Account(
                            id = "probe",
                            email = email,
                            displayName = manualName.ifBlank { email.substringBefore("@") },
                            provider = manualProvider,
                            appPassword = password,
                            imapHost = host,
                            smtpHost = CorporateMail.smtp(email, manualProvider, host)
                        )
                        val result = ImapFetcher.fetch(probe, MailFolder.INBOX, max = 1)
                        if (result.error != null && result.messages.isEmpty()) {
                            statusMessage = result.error
                            isLoading = false
                            return@launch
                        }
                        val saved = repository.addAccount(
                            email = email,
                            displayName = manualName.ifBlank { email.substringBefore("@") },
                            provider = manualProvider,
                            appPassword = password,
                            imapHost = host,
                            smtpHost = CorporateMail.smtp(email, manualProvider, host)
                        )
                        tokenStorage.saveTokens(saved.id, null, appPassword = password)
                        isLoading = false
                        onBack()
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Проверить и добавить") }

            statusMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private val knownHosts = setOf(
    "imap.gmail.com", "imap.yandex.ru", "imap.mail.ru", "outlook.office365.com"
)

private fun Provider.defaultHost(): String = when (this) {
    Provider.GOOGLE -> "imap.gmail.com"
    Provider.YANDEX -> "imap.yandex.ru"
    Provider.MAILRU -> "imap.mail.ru"
    Provider.OUTLOOK -> "outlook.office365.com"
    Provider.OTHER -> ""
}

private fun Provider.defaultSmtp(): String = when (this) {
    Provider.GOOGLE -> "smtp.gmail.com"
    Provider.YANDEX -> "smtp.yandex.ru"
    Provider.MAILRU -> "smtp.mail.ru"
    Provider.OUTLOOK -> "smtp.office365.com"
    Provider.OTHER -> ""
}

private fun guessProvider(email: String, current: Provider): Provider {
    val domain = email.substringAfter("@", "").lowercase()
    return when {
        domain.startsWith("gmail.") || domain == "googlemail.com" -> Provider.GOOGLE
        domain.startsWith("yandex.") || domain == "ya.ru" || domain.endsWith(".ya.ru") -> Provider.YANDEX
        domain.startsWith("mail.ru") || domain == "inbox.ru" || domain == "bk.ru" || domain == "list.ru" -> Provider.MAILRU
        domain.contains("outlook.") || domain.contains("hotmail.") || domain == "live.com" -> Provider.OUTLOOK
        domain.isBlank() -> current
        else -> if (current == Provider.OTHER || current.defaultHost().isNotBlank() && domain.isNotEmpty()) Provider.OTHER else current
    }
}

@Composable
private fun ProviderPicker(selected: Provider, onSelect: (Provider) -> Unit) {
    val look = appearance()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Сервис", style = MaterialTheme.typography.labelLarge)
        Provider.entries.forEach { provider ->
            Card(
                onClick = { onSelect(provider) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (provider == selected && look.liquidGlass) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    else if (provider == selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(if (look.liquidGlass) 20.dp else 12.dp)
            ) {
                Text(
                    provider.displayName + provider.defaultHost().let { if (it.isBlank()) "" else " · $it" },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}
