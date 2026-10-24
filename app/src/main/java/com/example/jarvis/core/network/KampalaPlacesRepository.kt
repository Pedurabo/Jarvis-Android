package com.example.jarvis.core.network

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.URI
import java.time.LocalDate
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
            } else if (response.places.any { !isValidPlace(it) }) {
                SearchResult.Failure(
                    "The Kampala places service returned incomplete or " +
                        "invalid listing information. No unvalidated " +
                        "directory results will be displayed."
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

    private fun isValidPlace(place: KampalaPlace): Boolean {
        if (
            place.id.isBlank() ||
            place.name.isBlank() ||
            place.category.isBlank() ||
            place.area.isBlank() ||
            place.address.isBlank()
        ) {
            return false
        }

        val validDate = try {
            val date = LocalDate.parse(place.verifiedAt)
            !date.isAfter(LocalDate.now())
        } catch (_: Exception) {
            false
        }

        val validSource = try {
            val uri = URI(place.sourceUrl)
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null
        } catch (_: Exception) {
            false
        }

        return validDate && validSource
    }
}
