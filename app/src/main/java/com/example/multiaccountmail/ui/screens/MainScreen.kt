package com.example.multiaccountmail.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.example.multiaccountmail.data.Account
import com.example.multiaccountmail.data.AccountRepository
import com.example.multiaccountmail.data.Provider
import com.example.multiaccountmail.data.TokenStorage
import com.example.multiaccountmail.BuildConfig
import com.example.multiaccountmail.mail.MailCache
import com.example.multiaccountmail.mail.MailSender
import com.example.multiaccountmail.mail.MailCategory
import com.example.multiaccountmail.mail.MailFolder
import com.example.multiaccountmail.mail.MailMessage
import com.example.multiaccountmail.mail.MailRepository
import com.example.multiaccountmail.ui.components.AccountAvatar
import com.example.multiaccountmail.ui.components.SenderAvatar
import com.example.multiaccountmail.ui.theme.Accent
import com.example.multiaccountmail.ui.theme.Appearance
import com.example.multiaccountmail.ui.theme.AppearanceStore
import com.example.multiaccountmail.ui.theme.LoadingStyle
import com.example.multiaccountmail.ui.theme.ThemeMode
import com.example.multiaccountmail.ui.theme.UiDensity
import com.example.multiaccountmail.ui.theme.appearance
import com.example.multiaccountmail.update.UpdateRepository
import com.example.multiaccountmail.update.UpdateInstaller
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class BottomTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)
private data class DrawerFolder(val folder: MailFolder, val icon: ImageVector)

@Composable
private fun AppLoadingIndicator() {
    when (appearance().loadingStyle) {
        LoadingStyle.CIRCLE -> CircularProgressIndicator()
        LoadingStyle.LINE -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.6f))
        LoadingStyle.DOTS -> Text("●  ●  ●", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun GlassPillNavigation(
    tabs: List<BottomTab>,
    selectedTab: Int,
    showLabels: Boolean,
    liquidGlass: Boolean,
    onSelect: (Int) -> Unit
) {
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(if (showLabels) 68.dp else 58.dp)
            .clip(RoundedCornerShape(50))
            .pointerInput(selectedTab) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, amount ->
                        dragOffsetPx += amount
                    },
                    onDragEnd = {
                        // Плашку можно провести через любое число вкладок. После отпускания
                        // она фиксируется на ближайшей вкладке вместе с её содержимым.
                        val segmentPx = size.width.toFloat() / tabs.size
                        val target = (selectedTab + dragOffsetPx / segmentPx)
                            .roundToInt()
                            .coerceIn(0, tabs.lastIndex)
                        if (target != selectedTab) onSelect(target)
                        dragOffsetPx = 0f
                    },
                    onDragCancel = {
                        dragOffsetPx = 0f
                    }
                )
            }
    ) {
        val segmentWidth = maxWidth / tabs.size
        val segmentPx = with(androidx.compose.ui.platform.LocalDensity.current) { segmentWidth.toPx() }
        // Движение не ограничено одним соседним разделом: можно протянуть плашку
        // сразу к любому разделу. Ограничение действует только по краям панели.
        val leftLimit = -selectedTab * segmentPx
        val rightLimit = (tabs.lastIndex - selectedTab) * segmentPx
        val dragOffset = with(androidx.compose.ui.platform.LocalDensity.current) {
            dragOffsetPx.coerceIn(leftLimit, rightLimit).toDp()
        }
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedTab,
            animationSpec = tween(durationMillis = 260),
            label = "pill-tab-indicator"
        )
        Box(
            Modifier
                .padding(4.dp)
                .offset(x = indicatorOffset + dragOffset)
                .width(segmentWidth - 8.dp)
                .height(if (showLabels) 60.dp else 50.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (liquidGlass) 0.72f else 0.30f))
        )
        Row(Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, tab ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title,
                        tint = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(23.dp)
                    )
                    if (showLabels) Text(tab.title, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: AccountRepository,
    tokenStorage: TokenStorage,
    onAddAccount: () -> Unit,
    allowEmptyAccounts: Boolean,
    onContinueWithoutMail: () -> Unit
) {
    val context = LocalContext.current
    val accounts by repository.accounts.collectAsState()
    val currentId by repository.currentAccountId.collectAsState()
    val currentAccount = accounts.find { it.id == currentId }

    if (accounts.isEmpty() && !allowEmptyAccounts) {
        LoginRequiredScreen(
            onAddAccount = onAddAccount,
            onContinueWithoutMail = onContinueWithoutMail
        )
        return
    }

    val navigationPrefs = remember {
        context.applicationContext.getSharedPreferences("navigation_state", android.content.Context.MODE_PRIVATE)
    }
    var selectedTab by remember {
        mutableIntStateOf(navigationPrefs.getInt("selected_tab", 0).coerceIn(0, 2))
    }
    var selectedFolder by remember {
        mutableStateOf(MailFolder.fromTitle(navigationPrefs.getString("selected_folder", MailFolder.INBOX.title).orEmpty()))
    }
    var openedMail by remember { mutableStateOf<MailMessage?>(null) }
    var composing by remember { mutableStateOf(false) }
    var showAccountManager by remember { mutableStateOf(false) }
    var refreshToken by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val look = appearance()
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedTab, selectedFolder) {
        navigationPrefs.edit()
            .putInt("selected_tab", selectedTab)
            .putString("selected_folder", selectedFolder.title)
            .apply()
    }

    val tabs = listOf(
        BottomTab("Почта", Icons.Filled.Email, Icons.Outlined.Email),
        BottomTab("Все", Icons.Filled.AllInbox, Icons.Filled.AllInbox),
        BottomTab("Ещё", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    val folders = listOf(
        DrawerFolder(MailFolder.UNIFIED, Icons.Default.AllInbox),
        DrawerFolder(MailFolder.INBOX, Icons.Default.Inbox),
        DrawerFolder(MailFolder.UNREAD, Icons.Default.Email),
        DrawerFolder(MailFolder.STARRED, Icons.Default.Star),
        DrawerFolder(MailFolder.SENT, Icons.AutoMirrored.Filled.Send),
        DrawerFolder(MailFolder.DRAFTS, Icons.Default.Drafts),
        DrawerFolder(MailFolder.SPAM, Icons.Default.Report),
        DrawerFolder(MailFolder.TRASH, Icons.Default.Delete)
    )

    BackHandler(enabled = openedMail != null || composing || showAccountManager) {
        openedMail = null
        composing = false
        showAccountManager = false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.AllInbox, contentDescription = null) },
                    label = {
                        Column {
                            Text("Все ящики", fontWeight = FontWeight.SemiBold)
                            Text("Общая входящая почта", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    selected = selectedFolder == MailFolder.UNIFIED,
                    onClick = {
                        selectedFolder = MailFolder.UNIFIED
                        selectedTab = 1
                        showAccountManager = false
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                Text(
                    "Аккаунты",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                )
                accounts.sortedBy { it.order }.forEach { acc ->
                    NavigationDrawerItem(
                        icon = {
                            AccountAvatar(account = acc, size = 32.dp, isSelected = acc.id == currentId)
                        },
                        label = {
                            Column {
                                Text(acc.displayName, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(acc.email, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        selected = acc.id == currentId,
                        onClick = {
                            repository.setCurrentAccount(acc.id)
                            showAccountManager = false
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                Text(
                    "Папки",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
                )
                folders.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.folder.title) },
                        selected = selectedFolder == item.folder,
                        onClick = {
                            selectedFolder = item.folder
                            selectedTab = 0
                            showAccountManager = false
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                GlassPillNavigation(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    showLabels = look.showLabels,
                    liquidGlass = look.liquidGlass,
                    onSelect = { index ->
                        openedMail = null
                        composing = false
                        showAccountManager = false
                        selectedTab = index
                    }
                )
            },
            floatingActionButtonPosition = if (look.fabLeft) {
                androidx.compose.material3.FabPosition.Start
            } else {
                androidx.compose.material3.FabPosition.End
            },
            floatingActionButton = {
                when {
                    composing || openedMail != null -> Unit
                    selectedTab == 0 && currentAccount != null -> {
                        FloatingActionButton(onClick = { composing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Написать")
                        }
                    }
                    selectedTab == 1 -> {
                        FloatingActionButton(onClick = onAddAccount) {
                            Icon(Icons.Default.Add, contentDescription = "Добавить")
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (showAccountManager) {
                    AccountsTab(
                        accounts = accounts,
                        currentId = currentId,
                        tokenStorage = tokenStorage,
                        onSelect = {
                            repository.setCurrentAccount(it)
                            showAccountManager = false
                        },
                        onBack = { showAccountManager = false },
                        onRemove = {
                            tokenStorage.clearTokens(it)
                            repository.removeAccount(it)
                        },
                        onReorder = { from, to -> repository.reorderAccounts(from, to) },
                        onSave = { id, email, name, provider, host, smtp, password ->
                            repository.updateAccount(id, email, name, provider, host, smtp)
                            if (!password.isNullOrBlank()) tokenStorage.saveTokens(id, null, appPassword = password)
                        }
                    )
                } else if (composing && currentAccount != null) {
                    ComposeMail(
                        account = tokenStorage.hydrate(currentAccount),
                        onDone = { composing = false }
                    )
                } else {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val forward = targetState > initialState
                            (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                                (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
                        },
                        label = "bottom-tab-content"
                    ) { tab ->
                        when (tab) {
                    0 -> MailTab(
                        accounts = if (selectedFolder == MailFolder.UNIFIED) accounts.sortedBy { it.order }
                        else listOfNotNull(currentAccount),
                        folder = if (selectedFolder == MailFolder.UNIFIED) MailFolder.INBOX else selectedFolder,
                        unified = selectedFolder == MailFolder.UNIFIED,
                        tokenStorage = tokenStorage,
                        refreshKey = refreshToken,
                        opened = openedMail,
                        onOpen = { openedMail = it },
                        accountLabel = currentAccount?.email,
                        onMenu = { scope.launch { drawerState.open() } },
                        onRefresh = { refreshToken++ },
                        onBack = {
                            openedMail = null
                            composing = false
                        }
                    )
                    1 -> MailTab(
                        accounts = accounts.sortedBy { it.order },
                        folder = MailFolder.INBOX,
                        unified = true,
                        tokenStorage = tokenStorage,
                        refreshKey = refreshToken,
                        opened = openedMail,
                        onOpen = { openedMail = it },
                        accountLabel = "Все ящики",
                        onMenu = { scope.launch { drawerState.open() } },
                        onRefresh = { refreshToken++ },
                        onBack = {
                            openedMail = null
                            composing = false
                        }
                    )
                    2 -> SettingsTab(
                        onSyncNow = { refreshToken++ },
                        onManageAccounts = {
                            showAccountManager = true
                        }
                    )
                    else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginRequiredScreen(onAddAccount: () -> Unit, onContinueWithoutMail: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.Email, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text(
                "Добро пожаловать",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Добавьте почту: Google, Яндекс, Mail.ru\nили любой ящик по паролю приложения",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
            )
            Spacer(Modifier.height(32.dp))
            FloatingActionButton(onClick = onAddAccount, modifier = Modifier.fillMaxWidth(0.8f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить аккаунт")
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onContinueWithoutMail) {
                Text("Продолжить без почты")
            }
            Text(
                "Ящик можно добавить позже в разделе «Все».",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MailTab(
    accounts: List<Account>,
    folder: MailFolder,
    unified: Boolean,
    tokenStorage: TokenStorage,
    refreshKey: Int,
    opened: MailMessage?,
    onOpen: (MailMessage) -> Unit,
    accountLabel: String?,
    onMenu: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val showCategories = folder == MailFolder.INBOX || folder == MailFolder.UNIFIED
    val cacheKey = remember(accounts.map { it.id }, folder, unified) {
        MailCache.key(accounts.map { it.id }, folder, unified)
    }
    val cached = remember(cacheKey) { MailCache.load(context, cacheKey) }
    // Отдельный снимок на каждый ящик и папку. Это не даёт списку становиться
    // пустым, если пользователь быстро переключает аккаунты.
    var mailSnapshots by remember { mutableStateOf<Map<String, List<MailMessage>>>(emptyMap()) }
    var loadingKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var errors by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    var category by remember(cacheKey) { mutableStateOf<MailCategory?>(null) }
    val mails = mailSnapshots[cacheKey] ?: cached
    val loading = loadingKeys.contains(cacheKey)
    val error = errors[cacheKey]

    LaunchedEffect(accounts.map { it.id }, folder, unified, refreshKey) {
        val sameMailbox = cached.isNotEmpty() && cached.all { cachedMail ->
            accounts.any { it.email.equals(cachedMail.accountEmail, ignoreCase = true) || it.id == cachedMail.accountId }
        }
        if (sameMailbox && mailSnapshots[cacheKey].isNullOrEmpty()) {
            mailSnapshots = mailSnapshots + (cacheKey to cached)
        }
        loadingKeys = loadingKeys + cacheKey
        errors = errors + (cacheKey to null)
        val result = MailRepository.load(
            context = context,
            accounts = accounts,
            tokenStorage = tokenStorage,
            folder = if (folder == MailFolder.UNIFIED) MailFolder.INBOX else folder,
            unified = unified
        )
        // Результат сохраняется только в снимок именно этого ящика/папки.
        // Поздний ответ предыдущего запроса не затронет открытую сейчас почту.
        if (result.messages.isNotEmpty()) {
            mailSnapshots = mailSnapshots + (cacheKey to result.messages)
        }
        if (result.messages.isNotEmpty()) {
            MailCache.save(context, cacheKey, result.messages)
        }
        errors = errors + (cacheKey to result.error?.takeUnless { text ->
            text.contains("403") || text.contains("HTTP ") || text.contains("<html", ignoreCase = true)
        })
        loadingKeys = loadingKeys - cacheKey
    }

    if (opened != null) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text(opened.subject, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        }
        MailDetail(opened)
        return
    }

    val look = appearance()
    val cardShape = if (look.roundCards) RoundedCornerShape(16.dp) else RoundedCornerShape(4.dp)
    val rowPadding = when (look.density) {
        UiDensity.COMPACT -> 10.dp
        UiDensity.COMFORTABLE -> 16.dp
        UiDensity.SPACIOUS -> 22.dp
    }
    val visible = if (showCategories && category != null) mails.filter { it.category == category } else mails
    val counts = MailCategory.entries.associateWith { cat -> mails.count { it.category == cat } }

    when {
        loading && mails.isEmpty() -> MailEmptyContent(
            title = accountLabel ?: folder.title,
            onMenu = onMenu,
            onRefresh = onRefresh
        ) { AppLoadingIndicator() }
        mails.isEmpty() -> MailEmptyContent(
            title = accountLabel ?: folder.title,
            onMenu = onMenu,
            onRefresh = onRefresh
        ) {
            EmptyState(
                error ?: "В этой папке писем нет",
                modifier = Modifier.weight(1f)
            )
        }
        else -> {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Меню") }
                        Text(
                            accountLabel ?: folder.title,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Обновить") }
                    }
                }
                if (showCategories) {
                    item {
                        CategoryRow(selected = category, counts = counts, total = mails.size, onSelect = { category = it })
                    }
                }
                item {
                    Text(
                        buildString {
                            append(category?.title ?: if (unified) "Все письма" else folder.title)
                            append(" · ${visible.size}")
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    error?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                if (visible.isEmpty()) {
                    item {
                        Text(
                            category?.let { "В разделе «${it.title}» писем нет" } ?: "Писем нет",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(visible, key = { it.id + it.accountEmail }) { mail ->
                    Card(
                        onClick = { onOpen(mail) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (mail.isUnread)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(Modifier.fillMaxWidth().padding(rowPadding), verticalAlignment = Alignment.CenterVertically) {
                            if (look.showAvatars) {
                                SenderAvatar(mail.fromName, mail.fromEmail)
                                Spacer(Modifier.width(12.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(mail.fromName, fontWeight = if (mail.isUnread) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(mail.subject, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (unified && mail.accountEmail.isNotBlank()) {
                                    Text(mail.accountEmail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                                }
                                if (look.showSnippets) {
                                    Text(
                                        mail.snippet.ifBlank { " " },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(mail.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (mail.isStarred) {
                                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    selected: MailCategory?,
    counts: Map<MailCategory, Int>,
    total: Int,
    onSelect: (MailCategory?) -> Unit
) {
    val icons = mapOf(
        MailCategory.PRIMARY to Icons.Default.Inbox,
        MailCategory.PROMOTIONS to Icons.Default.LocalOffer,
        MailCategory.SOCIAL to Icons.Default.People,
        MailCategory.UPDATES to Icons.Default.Notifications
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("Все $total") },
            leadingIcon = { Icon(Icons.Default.AllInbox, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        MailCategory.entries.forEach { cat ->
            val count = counts[cat] ?: 0
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(if (selected == cat) null else cat) },
                label = { Text(if (count > 0) "${cat.title} $count" else cat.title) },
                leadingIcon = {
                    Icon(icons.getValue(cat), contentDescription = null, modifier = Modifier.size(18.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun MailDetail(mail: MailMessage) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(mail.subject, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(mail.fromName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (mail.fromEmail.isNotBlank()) {
            Text(mail.fromEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(mail.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (mail.accountEmail.isNotBlank()) {
            Text(mail.accountEmail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            (mail.body.ifBlank { mail.snippet }).ifBlank { "Текст письма сервер не отдал." },
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ComposeMail(account: Account, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var to by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("От: ${account.email}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(to, { to = it }, label = { Text("Кому") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(subject, { subject = it }, label = { Text("Тема") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(body, { body = it }, label = { Text("Текст") }, modifier = Modifier.fillMaxWidth().height(220.dp))
        Button(
            onClick = {
                sending = true
                status = null
                scope.launch {
                    val error = MailSender.send(account, to, subject, body)
                    sending = false
                    if (error == null) onDone() else status = error
                }
            },
            enabled = !sending && to.contains("@"),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (sending) "Отправка..." else "Отправить")
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (account.appPassword.isNullOrBlank()) {
            Text(
                "Для отправки нужен пароль приложения у этого ящика. Его задают при добавлении аккаунта.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountsTab(
    accounts: List<Account>,
    currentId: String?,
    tokenStorage: TokenStorage,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onRemove: (String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onSave: (String, String, String, Provider, String?, String?, String?) -> Unit
) {
    var editing by remember { mutableStateOf<Account?>(null) }
    val editor = editing
    if (editor != null) {
        EditAccount(tokenStorage.hydrate(editor), onCancel = { editing = null }) { email, name, provider, host, smtp, password ->
            onSave(editor.id, email, name, provider, host, smtp, password)
            editing = null
        }
        return
    }
    val sorted = accounts.sortedBy { it.order }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Text("Управление ящиками", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                "Список сохраняется на устройстве. Токены лежат в зашифрованном хранилище.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
            )
        }
        items(sorted.size) { index ->
            val account = sorted[index]
            Card(
                onClick = { onSelect(account.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (account.id == currentId) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AccountAvatar(account = account, size = 48.dp, isSelected = account.id == currentId)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account.displayName, fontWeight = FontWeight.Medium)
                        Text(account.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(account.provider.displayName, style = MaterialTheme.typography.labelSmall, color = account.provider.primaryColor)
                    }
                    Column {
                        IconButton(onClick = { if (index > 0) onReorder(index, index - 1) }, enabled = index > 0, modifier = Modifier.size(32.dp)) { Text("↑") }
                        IconButton(onClick = { if (index < sorted.lastIndex) onReorder(index, index + 1) }, enabled = index < sorted.lastIndex, modifier = Modifier.size(32.dp)) { Text("↓") }
                    }
                    IconButton(onClick = { editing = account }) {
                        Icon(Icons.Default.Edit, contentDescription = "Изменить")
                    }
                    IconButton(onClick = { onRemove(account.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditAccount(
    account: Account,
    onCancel: () -> Unit,
    onSave: (String, String, Provider, String, String, String) -> Unit
) {
    var email by remember(account.id) { mutableStateOf(account.email) }
    var name by remember(account.id) { mutableStateOf(account.displayName) }
    var provider by remember(account.id) { mutableStateOf(account.provider) }
    var host by remember(account.id) { mutableStateOf(account.imapHost ?: account.defaultImapHost().orEmpty()) }
    var password by remember(account.id) { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
            Text("Изменить ящик", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
        OutlinedTextField(name, { name = it }, label = { Text("Имя") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(host, { host = it }, label = { Text("IMAP-сервер") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            password,
            { password = it },
            label = { Text(if (account.appPassword.isNullOrBlank()) "Пароль приложения" else "Новый пароль, если нужно сменить") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Text("Сервис", style = MaterialTheme.typography.labelLarge)
        Provider.entries.forEach { item ->
            FilterChip(selected = provider == item, onClick = { provider = item }, label = { Text(item.displayName) })
        }
        Button(
            onClick = {
                if (email.contains("@")) {
                    onSave(email.trim(), name.trim(), provider, host.trim(), com.example.multiaccountmail.mail.CorporateMail.smtp(email, provider, host), password.trim())
                }
            },
            enabled = email.contains("@"),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Сохранить") }
    }
}

@Composable
private fun SettingsTab(onSyncNow: () -> Unit, onManageAccounts: () -> Unit) {
    val context = LocalContext.current
    val look = appearance()
    val scope = rememberCoroutineScope()
    var release by remember { mutableStateOf<UpdateRepository.Release?>(null) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    fun versionNumber(value: String): List<Int> = value.removePrefix("v").split('.')
        .map { it.toIntOrNull() ?: 0 }
    fun isNewer(remote: String): Boolean {
        val latest = versionNumber(remote)
        val current = versionNumber(BuildConfig.VERSION_NAME)
        val length = maxOf(latest.size, current.size)
        return (0 until length).firstOrNull { (latest.getOrElse(it) { 0 }) != (current.getOrElse(it) { 0 }) }
            ?.let { latest.getOrElse(it) { 0 } > current.getOrElse(it) { 0 } } ?: false
    }
    fun save(next: Appearance) = AppearanceStore.update(context, next)

    fun openAppSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Настройки", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp)) }
        item { Text("Тема", fontWeight = FontWeight.SemiBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = look.mode == mode,
                        onClick = { save(look.copy(mode = mode)) },
                        label = { Text(mode.title) }
                    )
                }
            }
        }
        item { Text("Цвет", fontWeight = FontWeight.SemiBold) }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Accent.entries.forEach { accent ->
                    val selected = look.accent == accent
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accent.color)
                            .then(
                                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { save(look.copy(accent = accent)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Text("✓", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item { Text("Размер текста", fontWeight = FontWeight.SemiBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                com.example.multiaccountmail.ui.theme.FontScale.entries.forEach { scale ->
                    FilterChip(
                        selected = look.fontScale == scale,
                        onClick = { save(look.copy(fontScale = scale)) },
                        label = { Text(scale.title) }
                    )
                }
            }
        }
        item { Text("Нижняя панель", fontWeight = FontWeight.SemiBold) }
        item {
            Text("Пилюля без тёмных краёв. Проведи по ней влево или вправо, чтобы сменить вкладку.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Text("Плотность списка", fontWeight = FontWeight.SemiBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                UiDensity.entries.forEach { density ->
                    FilterChip(
                        selected = look.density == density,
                        onClick = { save(look.copy(density = density)) },
                        label = { Text(density.title) }
                    )
                }
            }
        }
        item { Text("Загрузка", fontWeight = FontWeight.SemiBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                LoadingStyle.entries.forEach { style ->
                    FilterChip(
                        selected = look.loadingStyle == style,
                        onClick = { save(look.copy(loadingStyle = style)) },
                        label = { Text(style.title) }
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Чёрный фон", fontWeight = FontWeight.Medium)
                        Text("AMOLED в тёмной теме", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = look.amoled, onCheckedChange = { save(look.copy(amoled = it)) })
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Аватары отправителей", fontWeight = FontWeight.Medium)
                        Text("Показывать логотипы и инициалы рядом с письмами", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = look.showAvatars, onCheckedChange = { save(look.copy(showAvatars = it)) })
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Предпросмотр писем", fontWeight = FontWeight.Medium)
                        Text("Показывать строку текста письма в списке", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = look.showSnippets, onCheckedChange = { save(look.copy(showSnippets = it)) })
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (look.liquidGlass) Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                            RoundedCornerShape(20.dp)
                        ) else Modifier
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (look.liquidGlass) MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
                    else MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Liquid Glass", fontWeight = FontWeight.Medium)
                        Text("Прозрачные поверхности, мягкие контуры и стеклянная капсула снизу", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = look.liquidGlass, onCheckedChange = { save(look.copy(liquidGlass = it)) })
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Скруглённые карточки", fontWeight = FontWeight.Medium)
                        Text("Выключи — углы станут почти прямыми", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = look.roundCards, onCheckedChange = { save(look.copy(roundCards = it)) })
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Подписи вкладок", fontWeight = FontWeight.Medium)
                        Text("Показывать названия кнопок снизу всегда", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = look.showLabels, onCheckedChange = { save(look.copy(showLabels = it)) })
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Кнопка слева", fontWeight = FontWeight.Medium)
                        Text("«Написать» и «Добавить» у левого края", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = look.fabLeft, onCheckedChange = { save(look.copy(fabLeft = it)) })
                }
            }
        }
        item {
            Card(onClick = onSyncNow, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Обновить почту", fontWeight = FontWeight.Medium)
                    Text("Загрузить письма с сервера ещё раз", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(onClick = onManageAccounts, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Управление ящиками", fontWeight = FontWeight.Medium)
                    Text("Добавить, изменить, удалить или изменить порядок аккаунтов", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(
                onClick = {
                    if (UpdateRepository.GITHUB_REPOSITORY.isBlank()) {
                        updateStatus = "Укажите адрес репозитория в UpdateRepository.kt, затем соберите новую версию."
                    } else {
                        updateStatus = "Проверяю GitHub…"
                        scope.launch {
                            UpdateRepository.latest().fold(
                                onSuccess = { latest ->
                                    release = latest
                                    updateStatus = if (!isNewer(latest.tag)) {
                                        "Установлена последняя версия."
                                    } else "Доступна версия ${latest.tag}."
                                },
                                onFailure = { updateStatus = it.message ?: "Не удалось проверить обновление" }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Обновление приложения", fontWeight = FontWeight.Medium)
                    Text("Версия ${BuildConfig.VERSION_NAME} · проверить новую версию на GitHub", style = MaterialTheme.typography.bodySmall)
                    updateStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) }
                    release?.let { latest ->
                        if (isNewer(latest.tag)) {
                            Text(latest.notes, style = MaterialTheme.typography.bodySmall, maxLines = 5, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                            TextButton(onClick = {
                                if (UpdateInstaller.download(context, latest)) {
                                    updateStatus = "Обновление скачивается. После загрузки Android попросит подтвердить установку."
                                } else if (latest.apkUrl.isBlank()) {
                                    updateStatus = "В последнем релизе нет APK."
                                } else {
                                    updateStatus = "Разреши установку из MultiMail в системных настройках, затем нажми ещё раз."
                                }
                            }) { Text("Скачать и установить") }
                        }
                    }
                }
            }
        }
        item {
            Card(onClick = {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
                context.startActivity(intent)
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Уведомления", fontWeight = FontWeight.Medium)
                    Text("Системные настройки. Фон проверяет почту каждые 15 минут.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(onClick = {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }.onFailure { openAppSettings() }
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Батарея", fontWeight = FontWeight.Medium)
                    Text("На Huawei и Xiaomi отключите оптимизацию, иначе фон не проснётся.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(onClick = { openAppSettings() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("О приложении", fontWeight = FontWeight.Medium)
                    Text("Версия ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MailEmptyContent(
    title: String,
    onMenu: () -> Unit,
    onRefresh: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Меню") }
            Text(title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Обновить") }
        }
        content()
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
