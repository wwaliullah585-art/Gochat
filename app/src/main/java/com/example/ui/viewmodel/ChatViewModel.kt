package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiPart
import com.example.data.api.GenerateContentRequest
import com.example.data.api.RetrofitClient
import com.example.data.db.ChatDatabase
import com.example.data.models.ChatMessage
import com.example.data.models.ChatSession
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    // Active session selection
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    // Status tracker for API key existence
    val isApiKeyConfigured: Boolean
        get() = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    // Search query for filtering chats/contacts
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Chat sessions flow
    val sessions: StateFlow<List<ChatSession>> = repository.sessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pre-populate data instantly
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    // Active messages flow, dynamically loads when selected session changes
    val activeMessages: StateFlow<List<ChatMessage>> = _activeSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessages(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectSession(sessionId: String?) {
        _activeSessionId.value = sessionId
        if (sessionId != null) {
            viewModelScope.launch {
                repository.clearUnreads(sessionId)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Creating a session dynamically
    fun startNewSession(name: String, subtitle: String, isAi: Boolean, colorHex: Int) {
        viewModelScope.launch {
            val formattedId = name.lowercase().replace("\\s".toRegex(), "_") + "_" + System.currentTimeMillis() % 10000
            val session = repository.createNewSession(
                id = formattedId,
                name = name,
                isAi = isAi,
                subtitle = subtitle,
                avatarColor = colorHex
            )
            selectSession(session.id)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = null
            }
        }
    }

    fun sendMessage(text: String, mediaUrl: String? = null, isImage: Boolean = false) {
        val currentSessionId = _activeSessionId.value ?: return
        if (text.isBlank() && mediaUrl == null) return

        viewModelScope.launch {
            val userMsg = ChatMessage(
                sessionId = currentSessionId,
                senderId = "me",
                senderName = "You",
                messageText = text,
                timestamp = System.currentTimeMillis(),
                isMine = true,
                isImage = isImage,
                mediaUrl = mediaUrl,
                status = "SENT"
            )
            repository.insertMessage(userMsg)

            // Trigger automation reply
            val sessionsList = sessions.value
            val activeSession = sessionsList.find { it.id == currentSessionId } ?: return@launch

            if (activeSession.isAi) {
                handleAiReply(currentSessionId, text)
            } else {
                handleSimulatedReply(activeSession)
            }
        }
    }

    private suspend fun handleAiReply(sessionId: String, lastUserMessage: String) {
        // Inject a placeholder "typing" reply status or loading state indicators
        val typingMsgId = System.currentTimeMillis()
        val typingMsg = ChatMessage(
            id = typingMsgId % 100000, // Safe temp ID override
            sessionId = sessionId,
            senderId = sessionId,
            senderName = "Gemini AI",
            messageText = "...",
            timestamp = System.currentTimeMillis(),
            isMine = false,
            status = "SENDING"
        )
        repository.insertMessage(typingMsg)

        withContext(Dispatchers.IO) {
            try {
                // Compile conversation history for contextual intelligence
                val messagesRef = activeMessages.value.filter { it.sessionId == sessionId }
                // Use prompt consolidating
                val promptBuilder = StringBuilder()
                promptBuilder.append("You are Gemini, an intelligent companion directly integrated into the GoChat Super App.\n")
                promptBuilder.append("Keep your replies warm, helpful, and concise (under 2-3 paragraphs max). Format clear list points or code blocks where appropriate.\n\n")
                
                // Fetch last 6 turns to keep context boundaries optimal
                val lastTurns = messagesRef.filter { it.messageText != "..." }.takeLast(6)
                lastTurns.forEach { msg ->
                    val sender = if (msg.isMine) "User" else "Gemini"
                    promptBuilder.append("$sender: ${msg.messageText}\n")
                }
                promptBuilder.append("Gemini:")

                val prompt = promptBuilder.toString()
                
                val req = GenerateContentRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    )
                )

                val key = BuildConfig.GEMINI_API_KEY
                val response = RetrofitClient.service.generateContent(key, req)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "I received your message, but I couldn't generate empty text answers. Could you try rephrasing your message?"

                // Update typing message to the actual answer
                val finalMsg = ChatMessage(
                    id = typingMsg.id,
                    sessionId = sessionId,
                    senderId = sessionId,
                    senderName = "Gemini",
                    messageText = responseText,
                    timestamp = System.currentTimeMillis(),
                    isMine = false,
                    status = "SENT"
                )
                repository.insertMessage(finalMsg)

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Gemini API call failed", e)
                val errorMsg = if (!isApiKeyConfigured) {
                    "API Key is missing or invalid. Please configure your GEMINI_API_KEY securely inside the Secrets panel of Google AI Studio securely to chat with me directly!"
                } else {
                    "Connection error: ${e.localizedMessage ?: "Please check internet status and retry."}"
                }
                val errorResponseMsg = ChatMessage(
                    id = typingMsg.id,
                    sessionId = sessionId,
                    senderId = sessionId,
                    senderName = "Gemini",
                    messageText = errorMsg,
                    timestamp = System.currentTimeMillis(),
                    isMine = false,
                    status = "ERROR"
                )
                repository.insertMessage(errorResponseMsg)
            }
        }
    }

    private fun handleSimulatedReply(session: ChatSession) {
        viewModelScope.launch {
            delay(1000) // Visual feeling of typing/thinking simulation
            
            val replyText = when (session.id) {
                "gochat_support" -> {
                    val faqs = listOf(
                        "Thanks for reaching out! Did you know you can chat with a real Gemini AI agent directly in GoChat? Check out our Gemini AI Assistant chat in the list!",
                        "Sure thing! GoChat automatically persists all messaging histories in a secure, local Room SQLite database on your Android device.",
                        "We support custom image attachments and beautiful user profile cards. Feel free to explore our settings panel anytime."
                    )
                    faqs.random()
                }
                "sara_traveler" -> {
                    val saraReplies = listOf(
                        "Yes! Let's pack some extra snacks and plenty of water. It's supposed to be sunny! ☀️",
                        "Should we meet up at 8:00 AM, or is that too early? Let me know!",
                        "I am so excited! I'll map out our hiking route and send you a screenshot shortly."
                    )
                    saraReplies.random()
                }
                "devon_coder" -> {
                    val devonReplies = listOf(
                        "Awesome! Yes, Compose makes handling lists exceptionally simple. Modifier.animateItemPlacement() makes reordering messages look amazing.",
                        "Have you tried adding database triggers or flows? Room makes reactive stream emission direct and elegant.",
                        "Let's write a simple UI test using Robolectric & Roborazzi next. It's great to confirm rendering behavior instantly."
                    )
                    devonReplies.random()
                }
                else -> "Got your message! Let's connect again soon. GoChat is the best place to talk."
            }

            val replyMsg = ChatMessage(
                sessionId = session.id,
                senderId = session.id,
                senderName = session.name,
                messageText = replyText,
                timestamp = System.currentTimeMillis(),
                isMine = false,
                status = "SENT"
            )
            repository.insertMessage(replyMsg)
        }
    }
}

class ChatViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
