package com.example.multiaccountmail.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.multiaccountmail.data.MailProtocol
import com.example.multiaccountmail.data.Provider
import com.example.multiaccountmail.data.TokenStorage
import com.example.multiaccountmail.mail.CorporateMail
import com.example.multiaccountmail.mail.ImapFetcher
import com.example.multiaccountmail.mail.MailFolder
import com.example.multiaccountmail.mail.PopFetcher
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
    var manualProtocol by remember { mutableStateOf(MailProtocol.IMAP) }
    var googleRetry by remember { mutableStateOf(false) }
    var showGoogleBrowser by remember { mutableStateOf(false) }

    if (showGoogleBrowser) {
        InAppGoogleBrowser(onClose = { showGoogleBrowser = false })
        return
    }

    fun saveSecrets(accountId: String, access: String?, refresh: String? = null, idToken: String? = null, password: String? = null) {
        tokenStorage.saveTokens(accountId, access, idToken, refresh, password)
        repository.updateTokens(accountId, access, refresh, idToken)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        // Некоторые версии Google Play Services возвращают RESULT_CANCELED,
        // хотя полезный результат уже лежит в intent. Всегда разбираем intent.
        val signIn = GoogleAuthHelper.handleSignInResult(result.data)
        if (signIn.account == null && result.resultCode != Activity.RESULT_OK && result.data == null) {
            statusMessage = "Вход Google отменён"
            return@rememberLauncherForActivityResult
        }
        val account = signIn.account
        val email = account?.email
        if (account == null || email == null) {
            statusMessage = signIn.error ?: "Google не вернул аккаунт. Проверь SHA-1 и Android OAuth Client в Cloud Console."
            return@rememberLauncherForActivityResult
        }
        val saved = repository.addAccount(
            email = email,
            displayName = account.displayName ?: email.substringBefore("@"),
            provider = Provider.GOOGLE,
            avatarUrl = account.photoUrl?.toString(),
            accessToken = null
        )
        saveSecrets(saved.id, null)
        statusMessage = "Добавлен Google: $email"
        onBack()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Column(Modifier.weight(1f)) {
                    Text("Добавить почту", style = MaterialTheme.typography.titleLarge)
                    Text("Подключите Google, IMAP- или POP3-ящик", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (isLoading) {
                CircularProgressIndicator()
            }
            Text("Google", style = MaterialTheme.typography.titleMedium)

            Card(
                onClick = {
                    isLoading = true
                    statusMessage = null
                    googleRetry = false
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
                    if (googleRetry) {
                        Button(
                            onClick = {
                                isLoading = true
                                statusMessage = null
                                GoogleAuthHelper.retrySignIn(context) { intent -> googleSignInLauncher.launch(intent) }
                            },
                            modifier = Modifier.padding(top = 12.dp)
                        ) { Text("Повторить вход") }
                    }
                }
            }

            Card(
                onClick = {
                    manualProvider = Provider.GOOGLE
                    manualHost = Provider.GOOGLE.defaultHost()
                    statusMessage = "Gmail выбран. Введи адрес и пароль приложения Google в форме ниже. Этот способ работает без Google Play Services и microG."
                },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(if (look.liquidGlass) 24.dp else 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (look.liquidGlass) MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f)
                    else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Gmail через IMAP", style = MaterialTheme.typography.titleMedium)
                    Text("Для Huawei и microG: вход по паролю приложения Google, без сервисов Google Play")
                    Button(
                        onClick = { showGoogleBrowser = true },
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("Войти в Google и создать пароль") }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Почта по IMAP / POP3", style = MaterialTheme.typography.titleMedium)
            Text(
                "Пароль можно не вводить при добавлении, но без него письма не загрузятся. POP3 читает только входящие.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ProviderPicker(manualProvider) { picked ->
                manualProvider = picked
                if (manualHost.isBlank() || manualHost in knownHosts) {
                    manualHost = picked.defaultHost()
                }
            }

            ProtocolPicker(manualProtocol) { picked ->
                manualProtocol = picked
                if (manualHost.isBlank() || manualHost in knownHosts || manualHost in knownPopHosts) {
                    manualHost = manualProvider.defaultHost(picked)
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
                label = { Text("Пароль приложения (необязательно)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = manualHost,
                onValueChange = { manualHost = it },
                label = { Text(if (manualProtocol == MailProtocol.IMAP) "IMAP-сервер" else "POP3-сервер") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val email = manualEmail.trim()
                    val password = manualPassword.trim()
                    val host = manualHost.trim().ifBlank { manualProvider.defaultHost(manualProtocol) }
                    if (email.isBlank() || !email.contains("@") || host.isBlank()) {
                        statusMessage = "Нужны email и ${if (manualProtocol == MailProtocol.IMAP) "IMAP" else "POP3"}-сервер"
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        statusMessage = if (password.isBlank()) "Добавляю ящик без проверки пароля..." else "Проверяю $host..."
                        val probe = com.example.multiaccountmail.data.Account(
                            id = "probe",
                            email = email,
                            displayName = manualName.ifBlank { email.substringBefore("@") },
                            provider = manualProvider,
                            appPassword = password,
                            protocol = manualProtocol,
                            imapHost = host,
                            smtpHost = CorporateMail.smtp(email, manualProvider, host)
                        )
                        if (password.isNotBlank()) {
                            val error = if (manualProtocol == MailProtocol.IMAP) {
                                ImapFetcher.verify(probe)
                            } else {
                                PopFetcher.verify(probe)
                            }
                            if (error != null) {
                                statusMessage = error
                                isLoading = false
                                return@launch
                            }
                        }
                        val saved = repository.addAccount(
                            email = email,
                            displayName = manualName.ifBlank { email.substringBefore("@") },
                            provider = manualProvider,
                            appPassword = password.ifBlank { null },
                            protocol = manualProtocol,
                            imapHost = host,
                            smtpHost = CorporateMail.smtp(email, manualProvider, host)
                        )
                        if (password.isNotBlank()) tokenStorage.saveTokens(saved.id, null, appPassword = password)
                        isLoading = false
                        onBack()
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (manualPassword.isBlank()) "Добавить без пароля" else "Проверить и добавить") }

            statusMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
                if (it.contains("код 8")) googleRetry = true
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private val knownHosts = setOf(
    "imap.gmail.com", "imap.yandex.ru", "imap.mail.ru", "outlook.office365.com"
)

private val knownPopHosts = setOf(
    "pop.gmail.com", "pop.yandex.ru", "pop.mail.ru", "outlook.office365.com"
)

private fun Provider.defaultHost(protocol: MailProtocol = MailProtocol.IMAP): String = when (this) {
    Provider.GOOGLE -> if (protocol == MailProtocol.IMAP) "imap.gmail.com" else "pop.gmail.com"
    Provider.YANDEX -> if (protocol == MailProtocol.IMAP) "imap.yandex.ru" else "pop.yandex.ru"
    Provider.MAILRU -> if (protocol == MailProtocol.IMAP) "imap.mail.ru" else "pop.mail.ru"
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

@Composable
private fun ProtocolPicker(selected: MailProtocol, onSelect: (MailProtocol) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Протокол", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MailProtocol.entries.forEach { protocol ->
                Card(
                    onClick = { onSelect(protocol) },
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (protocol == selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(protocol.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (protocol == MailProtocol.IMAP) "Папки и действия" else "Только входящие",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
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
