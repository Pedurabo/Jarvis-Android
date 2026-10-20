package com.example.jarvis.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class KampalaKnowledgeRepositoryTest {

    private fun validEntry(
        id: String = "kampala-history-overview",
        answer: String = "Kampala is the capital of Uganda. " +
            "Its history is linked to the Kingdom of Buganda. " +
            "A colonial fort was built in 1890. " +
            "Uganda gained independence on 9 October 1962.",
        sources: List<KampalaKnowledgeSource> = validSources()
    ) = KampalaKnowledgeEntry(
        id = id,
        topic = "history",
        city = "Kampala",
        country = "Uganda",
        title = "Kampala History",
        answer = answer,
        reviewedAt = "2026-10-16",
        sources = sources
    )

    private fun validSources() = listOf(
        KampalaKnowledgeSource(
            "Kampala Capital City Authority",
            "https://www.kcca.go.ug/news/1198"
        ),
        KampalaKnowledgeSource(
            "Library of Congress",
            "https://www.loc.gov/item/2021668576/"
        ),
        KampalaKnowledgeSource(
            "Office of the Vice President of Uganda",
            "https://www.vicepresident.go.ug/history/"
        )
    )

    private fun response(
        entries: List<KampalaKnowledgeEntry>,
        count: Int = entries.size,
        version: Int = 1
    ) = KampalaKnowledgeResponse(
        schemaVersion = version,
        count = count,
        entries = entries
    )

    private fun repository(
        answer: KampalaKnowledgeResponse,
        onTopic: (String) -> Unit = {}
    ): KampalaKnowledgeRepository {
        val fake = object : KampalaKnowledgeApiService {
            override suspend fun getKnowledge(topic: String): KampalaKnowledgeResponse {
                onTopic(topic)
                return answer
            }
        }
        return KampalaKnowledgeRepository(fake)
    }

    @Test
    fun validHistoryIsAccepted() = runBlocking {
        var requestedTopic = ""

        val result = repository(
            response(listOf(validEntry()))
        ) { requestedTopic = it }.getHistory()

        assertEquals("history", requestedTopic)
        assertTrue(result is KampalaKnowledgeRepository.Result.Success)
        assertEquals(
            "kampala-history-overview",
            (result as KampalaKnowledgeRepository.Result.Success).entry.id
        )
    }

    @Test
    fun missingHistoryReturnsNotFound() = runBlocking {
        val result = repository(response(emptyList())).getHistory()

        assertTrue(result is KampalaKnowledgeRepository.Result.NotFound)
    }

    @Test
    fun unsupportedSchemaIsRejected() = runBlocking {
        val result = repository(
            response(listOf(validEntry()), version = 2)
        ).getHistory()

        assertTrue(result is KampalaKnowledgeRepository.Result.Failure)
    }

    @Test
    fun inconsistentCountIsRejected() = runBlocking {
        val result = repository(
            response(listOf(validEntry()), count = 2)
        ).getHistory()

        assertTrue(result is KampalaKnowledgeRepository.Result.Failure)
    }

    @Test
    fun multipleHistoryEntriesAreRejected() = runBlocking {
        val result = repository(
            response(listOf(validEntry(), validEntry()))
        ).getHistory()

        assertTrue(result is KampalaKnowledgeRepository.Result.Failure)
    }

    @Test
    fun unexpectedHistoryIdentityIsRejected() = runBlocking {
        val result = repository(
            response(listOf(validEntry(id = "other-entry")))
        ).getHistory()

        assertTrue(result is KampalaKnowledgeRepository.Result.Failure)
    }

    @Test
    fun incompleteHistoricalFactsAreRejected() = runBlocking {
        val result = repository(
            response(listOf(validEntry(answer = "Kampala is in Uganda.")))
        ).getHistory()

        assertTrue(result is KampalaKnowledgeRepository.Result.Failure)
    }

    @Test
    fun untrustedSourceDomainIsRejected() = runBlocking {
        val badSources = validSources().toMutableList()
        badSources[0] = KampalaKnowledgeSource(
            "Fake KCCA",
            "https://www.kcca.go.ug.example.com/history"
        )

        val result = repository(
            response(listOf(validEntry(sources = badSources)))
        ).getHistory()

        assertTrue(result is KampalaKnowledgeRepository.Result.Failure)
    }

    @Test
    fun insecureSourceUrlIsRejected() = runBlocking {
        val badSources = validSources().toMutableList()
        badSources[1] = KampalaKnowledgeSource(
            "Library of Congress",
            "http://www.loc.gov/history"
        )

        val result = repository(
            response(listOf(validEntry(sources = badSources)))
        ).getHistory()

        assertTrue(result is KampalaKnowledgeRepository.Result.Failure)
    }

    @Test
    fun networkFailureReturnsFailure() = runBlocking {
        val fake = object : KampalaKnowledgeApiService {
            override suspend fun getKnowledge(topic: String): KampalaKnowledgeResponse {
                throw IOException("Simulated offline condition")
            }
        }

        val result = KampalaKnowledgeRepository(fake).getHistory()

        assertTrue(result is KampalaKnowledgeRepository.Result.Failure)
        assertEquals(
            "Kampala knowledge service is unreachable.",
            (result as KampalaKnowledgeRepository.Result.Failure).reason
        )
    }

    @Test
    fun cancellationIsRethrown() = runBlocking {
        val fake = object : KampalaKnowledgeApiService {
            override suspend fun getKnowledge(topic: String): KampalaKnowledgeResponse {
                throw CancellationException("Simulated cancellation")
            }
        }

        var cancelled = false

        try {
            KampalaKnowledgeRepository(fake).getHistory()
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertTrue("Coroutine cancellation must propagate", cancelled)
    }
}