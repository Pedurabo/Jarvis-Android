package com.example.jarvis.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaNamedDestinationRouteTest {

    @Test
    fun recognizesKingdomKampalaQuestion() {
        val request = KampalaNamedDestinationRoute.resolve(
            "where do I find kingdom kampala"
        )
        assertEquals("kingdom kampala", request?.query)
        assertNull(request?.area)
        assertNull(request?.category)
    }

    @Test
    fun recognizesWhereIsKingdomKampala() {
        assertEquals(
            "Kingdom Kampala",
            KampalaNamedDestinationRoute.resolve(
                "Where is Kingdom Kampala?"
            )?.query
        )
    }

    @Test
    fun recognizesExistingAcaciaMall() {
        assertEquals(
            "Acacia Mall",
            KampalaNamedDestinationRoute.resolve(
                "Where do I find Acacia Mall?"
            )?.query
        )
    }

    @Test
    fun recognizesNewTaxiParkWithoutInventingAddress() {
        val request = KampalaNamedDestinationRoute.resolve(
            "Where is New Taxi Park?"
        )
        assertEquals("New Taxi Park", request?.query)
        assertNull(request?.area)
    }

    @Test
    fun recognizesNamedHotelWithKampalaQualifier() {
        assertEquals(
            "Serena Hotel",
            KampalaNamedDestinationRoute.resolve(
                "Where is Serena Hotel in Kampala?"
            )?.query
        )
    }

    @Test
    fun uncertainSpeechRecognitionGetsClarification() {
        val text = KampalaNamedDestinationRoute.notice(
            "new tax park in kampala"
        )
        assertNotNull(text)
        assertTrue(text!!.contains("Did you mean New Taxi Park"))
        assertTrue(text.contains("not automatically changed"))
        assertNull(
            KampalaNamedDestinationRoute.resolve(
                "new tax park in kampala"
            )
        )
    }

    @Test
    fun existingCategorySearchIsNotTakenOver() {
        assertNull(
            KampalaNamedDestinationRoute.resolve(
                "Find hotels in Kampala"
            )
        )
        assertEquals(
            "accommodation",
            KampalaPlacesQueryRoute.resolve(
                "Find hotels in Kampala"
            )?.category
        )
    }

    @Test
    fun historyAndGeneralQuestionsAreProtected() {
        listOf(
            "Tell me about Kampala history",
            "What is a mosque?",
            "Where is Uganda?",
            "Explain how a computer works",
            "Where is my phone?"
        ).forEach {
            assertNull(KampalaNamedDestinationRoute.resolve(it))
            assertNull(KampalaNamedDestinationRoute.notice(it))
        }
    }

    @Test
    fun actionsAndBookingInstructionsAreProtected() {
        listOf(
            "Book Serena Hotel",
            "Call Acacia Mall",
            "Open Google Maps",
            "Reserve Kingdom Kampala"
        ).forEach {
            assertNull(KampalaNamedDestinationRoute.resolve(it))
        }
    }

    @Test
    fun unknownNamedMallCanSearchWithoutFabricating() {
        val request = KampalaNamedDestinationRoute.resolve(
            "Where is Imaginary Mall?"
        )

        assertEquals("Imaginary Mall", request?.query)
        assertNull(request?.area)
        assertNull(request?.category)
    }
}
