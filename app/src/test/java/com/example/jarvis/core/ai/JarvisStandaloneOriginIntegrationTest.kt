package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class JarvisStandaloneOriginIntegrationTest {

    @Test
    fun exactSamsungWordingIsRecognized() {
        assertEquals(
            "serena hotel kigo",
            ConversationTaskState.standaloneOriginOrNull(
                "am at serena hotel kigo"
            )
        )
    }

    @Test
    fun commonLocationStatementsAreRecognized() {
        val examples = mapOf(
            "I'm currently at Serena Hotel in Kigo." to
                "Serena Hotel in Kigo",
            "I am at Serena Hotel Kigo" to
                "Serena Hotel Kigo",
            "currently at Serena Hotel Kigo" to
                "Serena Hotel Kigo",
            "my starting location is Serena Hotel Kigo" to
                "Serena Hotel Kigo",
            "my pickup location is Serena Hotel Kigo" to
                "Serena Hotel Kigo"
        )

        examples.forEach { (message, expected) ->
            assertEquals(
                expected,
                ConversationTaskState.standaloneOriginOrNull(
                    message
                )
            )
        }
    }

    @Test
    fun unrelatedServicesRemainUnintercepted() {
        val messages = listOf(
            "Find banks near me",
            "Find Case Clinic in Nakasero",
            "Book a hotel in Kampala",
            "Show universities nearby",
            "Find Chinese restaurants",
            "Explain first aid for burns",
            "What's next?"
        )

        messages.forEach { message ->
            assertNull(
                message,
                ConversationTaskState.standaloneOriginOrNull(
                    message
                )
            )
        }
    }

    @Test
    fun userSuppliedOriginCanBeCarriedIntoJourney() {
        val place = requireNotNull(
            ConversationTaskState.standaloneOriginOrNull(
                "am at serena hotel kigo"
            )
        )

        val fact = ConversationTaskState.Fact(
            place,
            ConversationTaskState.Provenance.USER_SUPPLIED
        )

        val journey = ConversationTaskState.Task(
            kind = ConversationTaskState.Kind.NAVIGATION,
            goal = "Visit the school then the theatre",
            fields = mapOf(
                ConversationTaskState.Field.ORIGIN to fact
            )
        )

        assertEquals(
            "serena hotel kigo",
            journey.get(ConversationTaskState.Field.ORIGIN)?.value
        )

        assertEquals(
            ConversationTaskState.Provenance.USER_SUPPLIED,
            journey.get(ConversationTaskState.Field.ORIGIN)?.provenance
        )
    }

    @Test
    fun emergencyStatementsAreNotStandaloneLocations() {
        assertNull(
            ConversationTaskState.standaloneOriginOrNull(
                "I've been bitten by a snake"
            )
        )

        assertNull(
            ConversationTaskState.standaloneOriginOrNull(
                "My child is choking"
            )
        )
    }
}