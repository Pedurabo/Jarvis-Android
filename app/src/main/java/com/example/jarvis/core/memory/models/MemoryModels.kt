package com.example.jarvis.core.memory.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Memory(
    @DocumentId val id: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val tags: List<String> = emptyList(),
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = ""
)

data class ConversationMessage(
    @DocumentId val id: String = "",
    val text: String = "",
    val sender: String = "", // "user" or "assistant"
    val timestamp: Timestamp = Timestamp.now(),
    val metadata: Map<String, Any> = emptyMap()
)
