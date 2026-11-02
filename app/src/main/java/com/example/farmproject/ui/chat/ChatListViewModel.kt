// =====================================================================
// ChatListViewModel.kt
// Drives the chat list screen: loads my_conversations() and keeps it
// fresh (unread counts, ordering, previews) as new messages arrive.
// =====================================================================
package com.example.farmproject.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.repository.ChatRepository
import com.example.farmproject.data.model.Contact
import com.example.farmproject.data.model.ConversationSummary
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatListState(
    val loading: Boolean = true,
    val conversations: List<ConversationSummary> = emptyList(),
    val error: String? = null,
) {
    /** Handy for a bottom-tab badge. */
    @Suppress("unused")
    val totalUnread: Long get() = conversations.sumOf { it.unreadCount }
}

class ChatListViewModel(private val repo: ChatRepository) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    init {
        refresh()
        listenForNewMessages()
    }

    /** Call from the screen's ON_RESUME too, to catch anything missed while offline. */
    fun refresh() {
        viewModelScope.launch {
            runCatching { repo.conversations() }
                .onSuccess { list ->
                    _state.update { it.copy(loading = false, conversations = list, error = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message) }
                }
        }
    }

    // Any new message in any of my chats -> re-query the list.
    private fun listenForNewMessages() {
        viewModelScope.launch {
            val (channel, inserts) = repo.messageInserts(conversationId = null)
            try {
                channel.subscribe(blockUntilSubscribed = true)
                inserts.collect { refresh() }
            } finally {
                withContext(NonCancellable) { repo.closeChannel(channel) }
            }
        }
    }

    /** For the "new chat" picker: returns the conversation id to navigate to. */
    suspend fun startChatWith(contact: Contact): String = repo.openDirectChat(contact.accountId)

    /** For the "new chat" picker's search box. */
    suspend fun searchContacts(query: String): List<Contact> = repo.searchContacts(query)
}
