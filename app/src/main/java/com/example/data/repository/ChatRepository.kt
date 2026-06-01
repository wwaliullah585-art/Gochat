package com.example.data.repository

import android.util.Log
import com.example.data.db.ChatDao
import com.example.data.models.ChatMessage
import com.example.data.models.ChatSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ChatRepository(private val chatDao: ChatDao) {

    val sessions: Flow<List<ChatSession>> = chatDao.getChatSessions()

    fun getMessages(sessionId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesBySessionId(sessionId)
    }

    suspend fun clearUnreads(sessionId: String) {
        chatDao.clearUnreadCount(sessionId)
    }

    suspend fun insertMessage(message: ChatMessage) {
        chatDao.insertMessage(message)
        chatDao.updateSessionLastMessage(message.sessionId, message.messageText, message.timestamp)
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteSessionById(sessionId)
        chatDao.deleteMessagesBySessionId(sessionId)
    }

    suspend fun createNewSession(id: String, name: String, isAi: Boolean, subtitle: String, avatarColor: Int): ChatSession {
        val session = ChatSession(
            id = id,
            name = name,
            avatarColor = avatarColor,
            subtitle = subtitle,
            isAi = isAi,
            lastMessage = "Hey, let's connect!",
            lastTimestamp = System.currentTimeMillis()
        )
        chatDao.insertSession(session)
        val greeting = ChatMessage(
            sessionId = id,
            senderId = id,
            senderName = name,
            messageText = if (isAi) "Hi there! I am your AI assistant powered by Gemini 3.5-flash. Ask me anything!" else "Hello! Let's start a conversation here.",
            timestamp = System.currentTimeMillis(),
            isMine = false
        )
        chatDao.insertMessage(greeting)
        return session
    }

    suspend fun prepopulateIfEmpty() {
        val currentSessions = chatDao.getChatSessions().first()
        if (currentSessions.isEmpty()) {
            Log.d("ChatRepository", "Database is empty. Prepopulating default sessions.")
            
            // 1. Gemini AI
            val gemini = ChatSession(
                id = "gemini_assistant",
                name = "Gemini AI Assistant",
                avatarColor = 0xFF4285F4.toInt(), // Vibrant blue
                subtitle = "Powered by Gemini 3.5-flash",
                isAi = true,
                lastMessage = "Hi there! Ask me anything!",
                lastTimestamp = System.currentTimeMillis() - 2 * 3600000,
                unreadCount = 1
            )
            chatDao.insertSession(gemini)
            chatDao.insertMessage(
                ChatMessage(
                    sessionId = "gemini_assistant",
                    senderId = "gemini_assistant",
                    senderName = "Gemini",
                    messageText = "Hi there! I am your AI assistant powered by Gemini. Ask me anything about coding, travel, translations or general topics!",
                    timestamp = System.currentTimeMillis() - 2 * 3600000,
                    isMine = false
                )
            )

            // 2. GoChat Support
            val support = ChatSession(
                id = "gochat_support",
                name = "GoChat Team",
                avatarColor = 0xFF00BFA5.toInt(), // GoChat Teal
                subtitle = "In-App Expert Assistance",
                isAi = false,
                lastMessage = "Welcome to GoChat! Let me know if you need anything.",
                lastTimestamp = System.currentTimeMillis() - 3600000,
                unreadCount = 0
            )
            chatDao.insertSession(support)
            chatDao.insertMessage(
                ChatMessage(
                    sessionId = "gochat_support",
                    senderId = "gochat_support",
                    senderName = "GoChat Team",
                    messageText = "Hello! Welcome to GoChat. Our ultimate goal is to connect you instantly with friends, helpers, and smart AI agents. Explore our tabs and enjoy seamless offline-ready chat persistence!",
                    timestamp = System.currentTimeMillis() - 3600000,
                    isMine = false
                )
            )

            // 3. Sara
            val sara = ChatSession(
                id = "sara_traveler",
                name = "Sara (The Traveler)",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=200&auto=format&fit=crop",
                avatarColor = 0xFFFF4081.toInt(), // Pinkish-orange
                subtitle = "Online",
                isAi = false,
                lastMessage = "I am ready for the hiking trip this weekend!",
                lastTimestamp = System.currentTimeMillis() - 1800000,
                unreadCount = 0
            )
            chatDao.insertSession(sara)
            chatDao.insertMessage(
                ChatMessage(
                    sessionId = "sara_traveler",
                    senderId = "sara_traveler",
                    senderName = "Sara",
                    messageText = "Hey! Guess what? I am ready for the hiking trip this weekend! Should we bring supplies?",
                    timestamp = System.currentTimeMillis() - 1800000,
                    isMine = false
                )
            )

            // 4. Devon
            val devon = ChatSession(
                id = "devon_coder",
                name = "Devon Coder",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=200&auto=format&fit=crop",
                avatarColor = 0xFF7C4DFF.toInt(), // Violet
                subtitle = "Last seen today 9:15 AM",
                isAi = false,
                lastMessage = "Check out this Jetpack Compose project!",
                lastTimestamp = System.currentTimeMillis() - 900000,
                unreadCount = 0
            )
            chatDao.insertSession(devon)
            chatDao.insertMessage(
                ChatMessage(
                    sessionId = "devon_coder",
                    senderId = "devon_coder",
                    senderName = "Devon Coder",
                    messageText = "Check out this Jetpack Compose project! The list rendering is super smooth with LazyColumn.",
                    timestamp = System.currentTimeMillis() - 900000,
                    isMine = false
                )
            )
        }
    }
}
