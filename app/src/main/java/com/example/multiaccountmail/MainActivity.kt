package com.example.multiaccountmail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.multiaccountmail.data.AccountRepository
import com.example.multiaccountmail.data.TokenStorage
import com.example.multiaccountmail.ui.screens.AddAccountScreen
import com.example.multiaccountmail.ui.screens.MainScreen
import com.example.multiaccountmail.ui.theme.MultiAccountMailTheme
import com.example.multiaccountmail.sync.MailSyncWorker

class MainActivity : ComponentActivity() {

    private lateinit var repository: AccountRepository
    private lateinit var tokenStorage: TokenStorage
    private val startupPrefs by lazy { getSharedPreferences("startup_preferences", MODE_PRIVATE) }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = AccountRepository(applicationContext)
        tokenStorage = TokenStorage(applicationContext)
        requestNotificationPermissionIfNeeded()

        enableEdgeToEdge()
        setContent {
            MultiAccountMailTheme {
                var showAddAccount by androidx.compose.runtime.remember { mutableStateOf(false) }
                var continueWithoutMail by androidx.compose.runtime.remember {
                    mutableStateOf(startupPrefs.getBoolean("continue_without_mail", false))
                }
                if (showAddAccount) {
                    AddAccountScreen(
                        repository = repository,
                        tokenStorage = tokenStorage,
                        onBack = {
                            showAddAccount = false
                        }
                    )
                } else {
                    MainScreen(
                        repository = repository,
                        tokenStorage = tokenStorage,
                        onAddAccount = { showAddAccount = true },
                        allowEmptyAccounts = continueWithoutMail,
                        onContinueWithoutMail = {
                            startupPrefs.edit().putBoolean("continue_without_mail", true).apply()
                            continueWithoutMail = true
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Проверяем почту сразу при входе в приложение, а не только по
        // отложенному периодическому расписанию Huawei/Android.
        if (::repository.isInitialized && repository.snapshot().isNotEmpty()) {
            MailSyncWorker.syncNow(applicationContext)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
