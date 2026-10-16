package com.example.jarvis.core.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudLLMManager @Inject constructor() {
    private val TAG = "CloudLLMManager"

    // Using your computer's IP address
    private val BASE_URL = "http://192.168.100.4:3000"

    private val apiService: JarvisApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JarvisApiService::class.java)
    }

    suspend fun generateResponse(fullPrompt: String): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Routing to Cloud Proxy at $BASE_URL...")

        return@withContext try {
            val request = ChatRequest(
                messages = listOf(
                    ChatMessage(role = "system", content = "You are Jarvis, a helpful AI assistant."),
                    ChatMessage(role = "user", content = fullPrompt)
                )
            )

            val response = apiService.getChatCompletion(request)
            if (response.success) {
                response.data.content
            } else {
                "Error: Backend failed to generate response."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloud API Call failed. Check if phone/PC are on same Wi-Fi.", e)
            "Error: Could not reach Jarvis backend. Ensure it is running at $BASE_URL"
        }
    }
}
