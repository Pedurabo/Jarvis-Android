package com.example.jarvis.core.ai

import com.example.jarvis.core.network.KampalaNamedDestinationRoute
import com.example.jarvis.core.network.KampalaPlacesQueryRoute
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaGroundingSafetyPolicyTest {

    @Test
    fun mulagoHospitalLocationIsProtected() {
        val response = KampalaGroundingSafetyPolicy.notice(
            "where is Mulago referral Hospital located"
        )
        assertNotNull(response)
        assertTrue(response!!.contains("Mulago"))
        assertTrue(response.contains("won't invent"))
    }

    @Test
    fun stateHouseLocationIsProtected() {
        assertNotNull(
            KampalaGroundingSafetyPolicy.notice(
                "where is the kampala state house located"
            )
        )
    }

    @Test
    fun citySquareLocationIsProtected() {
        assertNotNull(
            KampalaGroundingSafetyPolicy.notice(
                "what is the location of city Square"
            )
        )
    }

    @Test
    fun centralPoliceStationIsProtected() {
        assertNotNull(
            KampalaGroundingSafetyPolicy.notice(
                "where can I find the Central Police Station in kampala"
            )
        )
    }

    @Test
    fun tallestBuildingQuestionDoesNotInventRanking() {
        val response = KampalaGroundingSafetyPolicy.notice(
            "what is the tallest building in kampala city"
        )
        assertNotNull(response)
        assertTrue(response!!.contains("verified source"))
        assertTrue(response.contains("shouldn't guess"))
    }

    @Test
    fun existingNamedRouteStillRecognizesKingdomKampala() {
        assertNotNull(
            KampalaNamedDestinationRoute.resolve(
                "where do I find kingdom kampala"
            )
        )
    }

    @Test
    fun existingCategoryRouteStillRecognizesHotels() {
        assertNotNull(
            KampalaPlacesQueryRoute.resolve(
                "Find hotels in Kampala"
            )
        )
    }

    @Test
    fun unrelatedQuestionsAreNotIntercepted() {
        listOf(
            "Tell me about Kampala history",
            "Explain Kotlin programming",
            "Where is my phone?",
            "Book a hotel in Kampala",
            "What is the tallest animal in Africa?",
            "How are you?",
            "Where is Uganda?"
        ).forEach {
            assertNull(
                "Unexpected safety interception: $it",
                KampalaGroundingSafetyPolicy.notice(it)
            )
        }
    }
}
