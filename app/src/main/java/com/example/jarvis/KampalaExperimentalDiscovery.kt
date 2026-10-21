package com.example.jarvis

import com.example.jarvis.core.ai.KampalaDiscoveryIntelligence
import com.example.jarvis.core.network.KampalaPlacesApiService
import com.example.jarvis.core.network.KampalaPlacesRepository
import com.example.jarvis.core.network.KampalaPlacesResultPolicy
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Firebase-independent experimental adapter for Jarvis's existing
 * Kampala places directory.
 *
 * Returns directory results and failure notices without replacing
 * unsuccessful searches with AI-generated local claims.
 */
class KampalaExperimentalDiscovery {

    private val repository: KampalaPlacesRepository by lazy {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()

        val service = Retrofit.Builder()
            .baseUrl("https://jarvis-kampala-preview.onrender.com/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KampalaPlacesApiService::class.java)

        KampalaPlacesRepository(service)
    }

    /**
     * Returns null only for requests not classified as directory searches.
     * Directory failures are returned as notices, not forwarded to Gemma.
     */
    suspend fun answerOrNull(message: String): String? {
        val decision = KampalaDiscoveryIntelligence.decide(message)

        val request =
            (decision as? KampalaDiscoveryIntelligence.Decision.Search)
                ?.request
                ?: return null

        val result = repository.search(
            query = request.query,
            area = request.area,
            category = request.category
        )

        return KampalaPlacesResultPolicy.format(request, result)
    }
}