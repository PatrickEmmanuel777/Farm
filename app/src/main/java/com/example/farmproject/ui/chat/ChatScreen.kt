// =====================================================================
// ChatScreen.kt
// One conversation: message bubbles (mine on the right), sender names in
// group chats, date chips, "Sending…" / "Not sent · tap to retry" states,
// long-press to delete your own message, infinite scroll to older
// messages, and the input bar.
//
// KEYBOARD: the screen uses imePadding(). Call enableEdgeToEdge() in your
// Activity (or keep android:windowSoftInputMode="adjustResize") so the
// input bar rides above the keyboard.
// =====================================================================
package com.example.farmproject.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    title: String,
    isGroup: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val myId = viewModel.myId
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Re-fetch the latest page when returning to the app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val messages = state.messages                              // oldest -> newest
    val reversed = remember(messages) { messages.asReversed() } // list is drawn bottom-up

    // Load older messages when the user scrolls near the top.
    LaunchedEffect(listState, reversed.size, state.canLoadOlder) {
        if (!state.canLoadOlder) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= reversed.size - 2) viewModel.loadOlder()
            }
    }

    // Follow new messages, but only if the user is already near the bottom.
    val newestId = messages.lastOrNull()?.clientId
    LaunchedEffect(newestId) {
        if (newestId != null && listState.firstVisibleItemIndex <= 2) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(name = title, size = 36.dp)
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (isGroup) {
                                Text(
                                    text = "Everyone on your farm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            MessageInputBar(
                modifier = Modifier.navigationBarsPadding(),
                onSend = { text ->
                    viewModel.send(text)
                    scope.launch { listState.animateScrollToItem(0) } // always jump to my own message
                },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            state.error?.let { ErrorBanner(message = it, onRetry = viewModel::refresh) }

            when {
                state.loading && messages.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                messages.isEmpty() -> {
                    EmptyState(
                        title = "No messages yet",
                        subtitle = "Send the first message to start the conversation.",
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp, vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(reversed, key = { _, m -> m.clientId }) { index, m ->
                            val older = reversed.getOrNull(index + 1)
                            val mine = m.senderId == myId
                            val showDate = older == null || older.localDay() != m.localDay()
                            val showName = isGroup && !mine &&
                                (showDate || older.senderId != m.senderId)

                            MessageRow(
                                m = m,
                                mine = mine,
                                senderName = if (showName) {
                                    when {
                                        m.senderId == null -> "Former member"
                                        else -> state.people[m.senderId]?.fullName ?: "Someone"
                                    }
                                } else null,
                                showDate = showDate,
                                onRetry = { viewModel.retry(m.clientId) },
                                onDelete = { viewModel.delete(m) },
                            )
                        }

                        // Drawn at the very top (list is reversed): more history is loading.
                        if (state.canLoadOlder) {
                            item(key = "loading-older") {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// One message (with optional date chip and sender name above it)
// ---------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    m: UiMessage,
    mine: Boolean,
    senderName: String?,
    showDate: Boolean,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    val failed = m.status == SendStatus.FAILED
    val canDelete = mine && m.serverId != null && !m.isDeleted && m.status == SendStatus.SENT

    val bubbleColor =
        if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor =
        if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val bubbleShape = if (mine) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 4.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
    }

    val meta = when (m.status) {
        SendStatus.SENDING -> "Sending…"
        SendStatus.FAILED -> "Not sent · tap to retry"
        SendStatus.SENT -> m.createdAt?.clockTime().orEmpty()
    }

    Column(Modifier.fillMaxWidth()) {
        if (showDate) DayChip(m.localDay().dayHeader())

        if (senderName != null) {
            Text(
                text = senderName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        ) {
            var menuOpen by remember { mutableStateOf(false) }

            Box {
                Surface(
                    color = bubbleColor,
                    shape = bubbleShape,
                    modifier = Modifier.widthIn(max = 300.dp),
                ) {
                    Column(
                        Modifier
                            .combinedClickable(
                                onClick = { if (failed) onRetry() },
                                onLongClick = { if (canDelete) menuOpen = true },
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (m.isDeleted) {
                            Text(
                                text = "This message was deleted",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = textColor.copy(alpha = 0.6f),
                            )
                        } else {
                            Text(
                                text = m.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                            )
                        }

                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (failed) MaterialTheme.colorScheme.error
                            else textColor.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 2.dp),
                        )
                    }
                }

                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete message") },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayChip(label: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------
// Input bar
// ---------------------------------------------------------------------
@Composable
private fun MessageInputBar(onSend: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by rememberSaveable { mutableStateOf("") }

    Surface(tonalElevation = 3.dp, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 4000) text = it }, // matches the database limit
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Spacer(Modifier.size(8.dp))
            FilledIconButton(
                onClick = {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        onSend(trimmed)
                        text = ""
                    }
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
