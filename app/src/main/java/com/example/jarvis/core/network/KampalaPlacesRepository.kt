package com.example.jarvis.core.network

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class KampalaPlacesRepository @Inject constructor(
    private val apiService: KampalaPlacesApiService
) {
    sealed class SearchResult {
        data class Success(val places: List<KampalaPlace>) : SearchResult()
        data class Failure(val message: String) : SearchResult()
    }

    suspend fun search(
        query: String? = null,
        area: String? = null,
        category: String? = null
    ): SearchResult {
        return try {
            val response = apiService.searchPlaces(
                query = query?.trim()?.takeIf { it.isNotEmpty() },
                area = area?.trim()?.takeIf { it.isNotEmpty() },
                category = category?.trim()?.takeIf { it.isNotEmpty() }
            )

            if (
                response.schemaVersion != 1 ||
                response.count != response.places.size
            ) {
                SearchResult.Failure(
                    "The Kampala places service returned an unexpected response."
                )
            } else {
                SearchResult.Success(response.places)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            SearchResult.Failure(
                "Unable to reach the Kampala places service. Check your internet connection and try again."
            )
        } catch (e: HttpException) {
            SearchResult.Failure(
                "The Kampala places service is temporarily unavailable. Please try again."
            )
        } catch (e: Exception) {
            SearchResult.Failure(
                "Unable to read the Kampala places information right now."
            )
        }
    }
}