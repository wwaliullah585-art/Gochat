package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val avatarColor: Int, // Color hex integer (e.g. 0xFF00BFA5 for teal)
    val subtitle: String,
    val isAi: Boolean,
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val senderId: String, // "me", or partner session ID
    val senderName: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isImage: Boolean = false,
    val isVoice: Boolean = false,
    val mediaUrl: String? = null,
    val isMine: Boolean,
    val status: String = "SENT" // "SENDING", "SENT", "ERROR"
)
