package com.example.jarvis.core.ai

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class KampalaFreshJourneyA69D4R4Test {

    @Test
    fun acaciaJourneyRetainsBothLocations() {
        val journey =
            KampalaFreshJourneyPriority.resolveOrNull(
                "How do I get to Acacia Mall from Ntinda?"
            )

        assertNotNull(journey)
        assertEquals("Ntinda", journey!!.origin)
        assertEquals("Acacia Mall", journey.destination)
    }

    @Test
    fun newWatotoJourneyDoesNotInheritAcacia() {
        val first =
            KampalaFreshJourneyPriority.resolveOrNull(
                "How do I get to Acacia Mall from Ntinda?"
            )

        val second =
            KampalaFreshJourneyPriority.resolveOrNull(
                "Take me from Muyenga to Watoto Church Downtown"
            )

        assertNotNull(first)
        assertNotNull(second)

        assertEquals("Muyenga", second!!.origin)
        assertEquals(
            "Watoto Church Downtown",
            second.destination
        )

        assertFalse(
            second.destination.contains(
                "Acacia Mall",
                ignoreCase = true
            )
        )
    }

    @Test
    fun emergencyDoesNotBecomeJourney() {
        assertNull(
            KampalaFreshJourneyPriority.resolveOrNull(
                "A dog has bitten me, what should I do?"
            )
        )
    }

    @Test
    fun explicitMapSearchDoesNotBecomeJourney() {
        assertNull(
            KampalaFreshJourneyPriority.resolveOrNull(
                "map search hospitals in central kampala"
            )
        )
    }

    @Test
    fun normalFollowUpDoesNotBecomeNewJourney() {
        assertNull(
            KampalaFreshJourneyPriority.resolveOrNull(
                "From Ntinda"
            )
        )

        assertNull(
            KampalaFreshJourneyPriority.resolveOrNull(
                "How do I get there?"
            )
        )
    }

    @Test
    fun unrelatedMessageDoesNotBecomeJourney() {
        assertNull(
            KampalaFreshJourneyPriority.resolveOrNull(
                "Explain Kotlin coroutines"
            )
        )
    }

    @Test
    fun controllerOrdersFreshJourneyBeforeContext() {
        val candidates = listOf(
            File(
                "src/main/java/com/example/jarvis/" +
                    "core/ai/AssistantController.kt"
            ),
            File(
                "app/src/main/java/com/example/jarvis/" +
                    "core/ai/AssistantController.kt"
            )
        )

        val sourceFile = candidates.firstOrNull {
            it.isFile
        }

        assertNotNull(
            "Cannot locate controller source for ordering test",
            sourceFile
        )

        val source = sourceFile!!.readText()

        val emergency =
            source.indexOf(
                "val emergencyNotice ="
            )

        val geographic =
            source.indexOf(
                "val explicitGeoCommand ="
            )

        val fresh =
            source.indexOf(
                "KampalaFreshJourneyPriority.resolveOrNull(query)"
            )

        val contextual =
            source.indexOf(
                "// CDI.5.61-A2: Resolve a contextual navigation follow-up"
            )

        val laterClassification =
            source.indexOf(
                "val navigationIntent ="
            )

        assertTrue(emergency >= 0)
        assertTrue(geographic > emergency)
        assertTrue(fresh > geographic)
        assertTrue(contextual > fresh)
        assertTrue(laterClassification > contextual)

        assertEquals(
            1,
            Regex(
                Regex.escape(
                    "if (directJourney != null)"
                )
            ).findAll(source).count()
        )

        assertTrue(
            source.substring(fresh, contextual)
                .contains("matchingDirectoryPlaces(")
        )

        assertTrue(
            source.substring(fresh, contextual)
                .contains("resetNavigationContext()")
        )
    }
}