package com.example.jarvis.core.network

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.URI
import javax.inject.Inject

class KampalaKnowledgeRepository @Inject constructor(
    private val api: KampalaKnowledgeApiService
) {
    sealed class Result {
        data class Success(val entry: KampalaKnowledgeEntry) : Result()
        data object NotFound : Result()
        data class Failure(val reason: String) : Result()
    }

    suspend fun getHistory(): Result {
        return try {
            val response = api.getKnowledge("history")

            if (response.schemaVersion != 1) {
                return Result.Failure("Unsupported knowledge format.")
            }

            if (
                response.count != response.entries.size ||
                response.entries.size > 1
            ) {
                return Result.Failure("Inconsistent knowledge response.")
            }

            val entry = response.entries.singleOrNull()
                ?: return Result.NotFound

            if (!isValidHistoryEntry(entry)) {
                return Result.Failure("Knowledge entry failed validation.")
            }

            Result.Success(entry)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            Result.Failure("Kampala knowledge service is unreachable.")
        } catch (error: HttpException) {
            Result.Failure("Kampala knowledge service returned an error.")
        } catch (error: Exception) {
            Result.Failure("Kampala knowledge could not be verified.")
        }
    }

    private fun isValidHistoryEntry(entry: KampalaKnowledgeEntry): Boolean {
        if (
            entry.id != "kampala-history-overview" ||
            entry.topic != "history" ||
            entry.city != "Kampala" ||
            entry.country != "Uganda" ||
            entry.title.isBlank() ||
            entry.answer.isBlank() ||
            !entry.reviewedAt.matches(
                Regex("""\d{4}-\d{2}-\d{2}""")
            ) ||
            entry.sources.size != 3
        ) {
            return false
        }

        val publishers = entry.sources.map { it.publisher }.toSet()

        if (
            publishers.size != 3 ||
            entry.sources.any { source ->
                source.publisher.isBlank() ||
                    !isTrustedUrl(source.url)
            }
        ) {
            return false
        }

        val expectedFacts = listOf(
            "capital of Uganda",
            "Kingdom of Buganda",
            "1890",
            "9 October 1962"
        )

        return expectedFacts.all {
            entry.answer.contains(it, ignoreCase = true)
        }
    }

    private fun isTrustedUrl(value: String): Boolean {
        return try {
            val url = URI(value)

            url.scheme.equals("https", ignoreCase = true) &&
                url.userInfo == null &&
                url.port == -1 &&
                url.host?.lowercase() in setOf(
                    "www.kcca.go.ug",
                    "www.loc.gov",
                    "www.vicepresident.go.ug"
                )
        } catch (_: Exception) {
            false
        }
    }
}