package com.aichat.app.data

import java.util.UUID

enum class Role {
    USER, ASSISTANT, SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val messages: List<ChatMessage> = emptyList(),
    val modelConfigId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
