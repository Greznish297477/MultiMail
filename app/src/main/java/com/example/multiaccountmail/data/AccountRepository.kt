package com.example.multiaccountmail.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Аккаунты живут в SharedPreferences и переживают перезапуск.
 * Токены и пароли приложений лежат отдельно в [TokenStorage].
 */
class AccountRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _currentAccountId = MutableStateFlow<String?>(null)
    val currentAccountId: StateFlow<String?> = _currentAccountId.asStateFlow()

    init {
        val loaded = load()
        _accounts.value = loaded
        val savedCurrent = prefs.getString(KEY_CURRENT, null)
        _currentAccountId.value = when {
            savedCurrent != null && loaded.any { it.id == savedCurrent } -> savedCurrent
            else -> loaded.firstOrNull()?.id
        }
        if (_currentAccountId.value != null) {
            _accounts.value = loaded.map { it.copy(isActive = it.id == _currentAccountId.value) }
        }
    }

    fun addAccount(
        email: String,
        displayName: String,
        provider: Provider,
        accessToken: String? = null,
        idToken: String? = null,
        refreshToken: String? = null,
        avatarUrl: String? = null,
        appPassword: String? = null,
        protocol: MailProtocol = MailProtocol.IMAP,
        imapHost: String? = null,
        imapPort: Int = 993,
        smtpHost: String? = null,
        smtpPort: Int = 465
    ): Account {
        val existing = _accounts.value.find { it.email.equals(email, ignoreCase = true) }
        if (existing != null) {
            val updated = existing.copy(
                displayName = displayName.ifBlank { existing.displayName },
                provider = provider,
                avatarUrl = avatarUrl ?: existing.avatarUrl,
                accessToken = accessToken ?: existing.accessToken,
                idToken = idToken ?: existing.idToken,
                refreshToken = refreshToken ?: existing.refreshToken,
                appPassword = appPassword ?: existing.appPassword,
                protocol = protocol,
                imapHost = imapHost ?: existing.imapHost,
                imapPort = if (imapHost != null) imapPort else existing.imapPort,
                smtpHost = smtpHost ?: existing.smtpHost,
                smtpPort = if (smtpHost != null) smtpPort else existing.smtpPort
            )
            _accounts.update { list -> list.map { if (it.id == existing.id) updated else it } }
            persist()
            return updated
        }

        val newOrder = (_accounts.value.maxOfOrNull { it.order } ?: -1) + 1
        val account = Account(
            id = UUID.randomUUID().toString(),
            email = email.trim(),
            displayName = displayName.ifBlank { email.substringBefore("@") },
            provider = provider,
            avatarUrl = avatarUrl,
            order = newOrder,
            accessToken = accessToken,
            idToken = idToken,
            refreshToken = refreshToken,
            appPassword = appPassword,
            protocol = protocol,
            imapHost = imapHost,
            imapPort = imapPort,
            smtpHost = smtpHost,
            smtpPort = smtpPort
        )
        _accounts.update { current ->
            val updated = current + account
            if (current.isEmpty()) {
                _currentAccountId.value = account.id
                updated.map { if (it.id == account.id) it.copy(isActive = true) else it }
            } else {
                updated
            }
        }
        persist()
        return account
    }

    fun updateAccount(
        id: String,
        email: String,
        displayName: String,
        provider: Provider,
        imapHost: String?,
        smtpHost: String?
    ) {
        _accounts.update { list ->
            list.map { account ->
                if (account.id != id) account
                else account.copy(
                    email = email.trim(),
                    displayName = displayName.ifBlank { email.substringBefore("@") },
                    provider = provider,
                    imapHost = imapHost?.ifBlank { null },
                    smtpHost = smtpHost?.ifBlank { null }
                )
            }
        }
        persist()
    }

    fun updateTokens(id: String, accessToken: String?, refreshToken: String? = null, idToken: String? = null) {
        _accounts.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        accessToken = accessToken ?: it.accessToken,
                        refreshToken = refreshToken ?: it.refreshToken,
                        idToken = idToken ?: it.idToken
                    )
                } else it
            }
        }
        persist()
    }

    fun removeAccount(id: String) {
        _accounts.update { list ->
            list.filter { it.id != id }.mapIndexed { index, acc -> acc.copy(order = index) }
        }
        if (_currentAccountId.value == id) {
            _currentAccountId.value = _accounts.value.firstOrNull()?.id
            _accounts.update { list ->
                list.map { it.copy(isActive = it.id == _currentAccountId.value) }
            }
        }
        persist()
    }

    fun setCurrentAccount(id: String) {
        _currentAccountId.value = id
        _accounts.update { list -> list.map { it.copy(isActive = it.id == id) } }
        persist()
    }

    fun reorderAccounts(fromIndex: Int, toIndex: Int) {
        _accounts.update { current ->
            val mutable = current.sortedBy { it.order }.toMutableList()
            if (fromIndex in mutable.indices && toIndex in mutable.indices) {
                val item = mutable.removeAt(fromIndex)
                mutable.add(toIndex, item)
                mutable.mapIndexed { index, acc -> acc.copy(order = index) }
            } else {
                current
            }
        }
        persist()
    }

    fun getCurrentAccount(): Account? =
        _accounts.value.find { it.id == _currentAccountId.value }

    fun getSortedAccounts(): List<Account> = _accounts.value.sortedBy { it.order }

    fun snapshot(): List<Account> = _accounts.value

    private fun persist() {
        val arr = JSONArray()
        _accounts.value.sortedBy { it.order }.forEach { acc ->
            arr.put(
                JSONObject().apply {
                    put("id", acc.id)
                    put("email", acc.email)
                    put("displayName", acc.displayName)
                    put("provider", acc.provider.name)
                    put("avatarUrl", acc.avatarUrl ?: "")
                    put("order", acc.order)
                    put("protocol", acc.protocol.name)
                    put("imapHost", acc.imapHost ?: "")
                    put("imapPort", acc.imapPort)
                    put("smtpHost", acc.smtpHost ?: "")
                    put("smtpPort", acc.smtpPort)
                }
            )
        }
        prefs.edit()
            .putString(KEY_ACCOUNTS, arr.toString())
            .putString(KEY_CURRENT, _currentAccountId.value)
            .apply()
    }

    private fun load(): List<Account> {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    // Одна повреждённая запись не должна скрывать остальные ящики.
                    runCatching {
                        Account(
                            id = o.getString("id"),
                            email = o.getString("email"),
                            displayName = o.optString("displayName"),
                            provider = Provider.fromName(o.optString("provider")),
                            avatarUrl = o.optString("avatarUrl").ifBlank { null },
                            order = o.optInt("order", i),
                            protocol = runCatching { MailProtocol.valueOf(o.optString("protocol")) }.getOrDefault(MailProtocol.IMAP),
                            imapHost = o.optString("imapHost").ifBlank { null },
                            imapPort = o.optInt("imapPort", 993),
                            smtpHost = o.optString("smtpHost").ifBlank { null },
                            smtpPort = o.optInt("smtpPort", 465)
                        )
                    }.getOrNull()?.let(::add)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val PREFS = "accounts_store"
        private const val KEY_ACCOUNTS = "accounts_json"
        private const val KEY_CURRENT = "current_id"
    }
}
