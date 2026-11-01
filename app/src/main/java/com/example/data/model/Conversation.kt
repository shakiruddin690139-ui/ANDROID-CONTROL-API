package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val providerId: String = "gemini",
    val modelId: String = "gemini-3.8-flash",
    val lastMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val userId: String = "",
    val role: String = "user", // user, model, system
    val content: String = "",
    val provider: String = "gemini",
    val model: String = "gemini-3.8-flash",
    @ServerTimestamp val createdAt: Timestamp? = null
)
