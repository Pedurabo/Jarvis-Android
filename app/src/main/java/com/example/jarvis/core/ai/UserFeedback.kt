package com.example.jarvis.core.ai

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class UserFeedback(
    @get:PropertyName("query") @set:PropertyName("query") var query: String = "",
    @get:PropertyName("failedResponse") @set:PropertyName("failedResponse") var failedResponse: String = "",
    @get:PropertyName("correction") @set:PropertyName("correction") var correction: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp") var timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("category") @set:PropertyName("category") var category: String = "general"
)
