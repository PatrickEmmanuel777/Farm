// =====================================================================
// NewChatScreen.kt
// Contact picker: people on the same farm, with a debounced search box.
// Tapping a person opens (or creates) the direct chat with them.
// =====================================================================
package com.example.farmproject.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.farmproject.data.model.Contact
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    search: suspend (query: String) -> List<Contact>,
    startChat: suspend (Contact) -> String,
    onBack: () -> Unit,
    onChatReady: (conversationId: String, title: String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var openingId by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // Re-runs when the query changes (after a 300 ms typing pause) or on Retry.
    LaunchedEffect(query, reloadKey) {
        loading = true
        if (query.isNotEmpty()) delay(300.milliseconds)
        runCatching { search(query.trim()) }
            .onSuccess {
                contacts = it
                error = null
            }
            .onFailure { error = it.message ?: "Could not load contacts" }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            error?.let { ErrorBanner(message = it, onRetry = { reloadKey++ }) }

            when {
                loading && contacts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                !loading && contacts.isEmpty() && error == null -> {
                    EmptyState(
                        title = if (query.isBlank()) "No one to message yet" else "No matches",
                        subtitle = if (query.isBlank()) {
                            "People on your farm will appear here."
                        } else {
                            "Try a different name."
                        },
                    )
                }

                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(contacts, key = { it.accountId }) { c ->
                            ContactRow(
                                contact = c,
                                opening = openingId == c.accountId,
                                onClick = {
                                    if (openingId == null) {
                                        openingId = c.accountId
                                        scope.launch {
                                            runCatching { startChat(c) }
                                                .onSuccess { id -> onChatReady(id, c.fullName) }
                                                .onFailure {
                                                    snackbar.showSnackbar(
                                                        it.message ?: "Could not start the chat"
                                                    )
                                                }
                                            openingId = null
                                        }
                                    }
                                },
                            )
                            HorizontalDivider(Modifier.padding(start = 72.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: Contact, opening: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(name = contact.fullName, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = contact.fullName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (opening) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}
