package com.example.multiaccountmail.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multiaccountmail.data.Account
import kotlin.math.roundToInt

/**
 * Переключатель аккаунтов свайпом (как в Google).
 * Также есть режим редактирования порядка.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountSwitcher(
    accounts: List<Account>,
    currentAccountId: String?,
    onAccountSelected: (String) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false
) {
    val sorted = accounts.sortedBy { it.order }
    val currentIndex = sorted.indexOfFirst { it.id == currentAccountId }.coerceAtLeast(0)

    if (editMode) {
        // Режим изменения порядка
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "Изменить порядок аккаунтов",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            sorted.forEachIndexed { index, account ->
                ReorderableAccountItem(
                    account = account,
                    isFirst = index == 0,
                    isLast = index == sorted.lastIndex,
                    onMoveUp = { onReorder(index, index - 1) },
                    onMoveDown = { onReorder(index, index + 1) }
                )
            }
        }
    } else {
        // Обычный режим: свайп по аватару
        SwipeableAvatarSwitcher(
            accounts = sorted,
            currentIndex = currentIndex,
            onAccountSelected = { id -> onAccountSelected(id) },
            modifier = modifier
        )
    }
}

@Composable
private fun SwipeableAvatarSwitcher(
    accounts: List<Account>,
    currentIndex: Int,
    onAccountSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (accounts.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Нет аккаунтов", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = dragOffset, label = "swipe")

    val threshold = 80f // порог свайпа

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Большой аватар текущего аккаунта с жестом свайпа
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .pointerInput(currentIndex, accounts) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                dragOffset > threshold && currentIndex > 0 -> {
                                    onAccountSelected(accounts[currentIndex - 1].id)
                                }
                                dragOffset < -threshold && currentIndex < accounts.lastIndex -> {
                                    onAccountSelected(accounts[currentIndex + 1].id)
                                }
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                            // Ограничиваем
                            dragOffset = dragOffset.coerceIn(-150f, 150f)
                        }
                    )
                }
        ) {
            // Предыдущий аватар (слева)
            if (currentIndex > 0) {
                AccountAvatar(
                    account = accounts[currentIndex - 1],
                    size = 36.dp,
                    modifier = Modifier
                        .offset { IntOffset((-60 + animatedOffset * 0.3f).roundToInt(), 0) }
                        .graphicsLayer { alpha = 0.5f }
                )
            }

            // Текущий аватар
            val current = accounts[currentIndex]
            AccountAvatar(
                account = current,
                size = 56.dp,
                isSelected = true,
                modifier = Modifier
                    .offset { IntOffset(animatedOffset.roundToInt(), 0) }
            )

            // Следующий аватар (справа)
            if (currentIndex < accounts.lastIndex) {
                AccountAvatar(
                    account = accounts[currentIndex + 1],
                    size = 36.dp,
                    modifier = Modifier
                        .offset { IntOffset((60 + animatedOffset * 0.3f).roundToInt(), 0) }
                        .graphicsLayer { alpha = 0.5f }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Имя и email текущего
        val current = accounts.getOrNull(currentIndex)
        if (current != null) {
            Text(
                text = current.displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = current.email,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = current.provider.displayName,
                fontSize = 12.sp,
                color = current.provider.primaryColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Мини-индикаторы (точки / маленькие аватары)
        LazyRow(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(accounts) { index, account ->
                AccountAvatar(
                    account = account,
                    size = if (index == currentIndex) 28.dp else 22.dp,
                    isSelected = index == currentIndex,
                    onClick = { onAccountSelected(account.id) }
                )
            }
        }
    }
}

@Composable
private fun ReorderableAccountItem(
    account: Account,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            AccountAvatar(account = account, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = account.email,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Выше")
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Ниже")
                }
            }
        }
    }
}
