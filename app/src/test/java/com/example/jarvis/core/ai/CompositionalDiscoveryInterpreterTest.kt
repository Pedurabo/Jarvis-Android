package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionalDiscoveryInterpreterTest {

    private fun parse(query: String) =
        CompositionalDiscoveryInterpreter.interpret(query)

    private fun kinds(query: String) =
        parse(query).tasks.map { it.kind }

    @Test
    fun chineseCuisineIsRestaurantDiscovery() {
        val task = parse(
            "where can I go to have a Chinese cuisine?"
        ).tasks.single()

        assertEquals(
            CompositionalDiscoveryInterpreter.TaskKind.PLACE_DISCOVERY,
            task.kind
        )
        assertEquals("chinese", task.cuisine)
    }

    @Test
    fun turkishFoodInMakindyePreservesArea() {
        val task = parse(
            "Where can I have a Turkish meal in Makindye?"
        ).tasks.single()

        assertEquals("turkish", task.cuisine)
        assertEquals("makindye", task.area)
    }

    @Test
    fun marketInventoryRequiresDifferentEvidence() {
        val task = parse(
            "what are the different kinds of food being sold in nakasero market?"
        ).tasks.single()

        assertEquals(
            CompositionalDiscoveryInterpreter.TaskKind.PRODUCT_AVAILABILITY,
            task.kind
        )
        assertEquals(
            CompositionalDiscoveryInterpreter.EvidenceRequirement.VERIFIED_INVENTORY,
            task.evidence
        )
    }

    @Test
    fun spiceTripRetainsSeparateRequirements() {
        val result = parse(
            "Which market has spices for cooking, how do I get there, " +
                "and can I order a SafeBoda or get me an Uber?"
        )

        assertTrue(
            "Missing verified inventory requirement",
            result.tasks.any {
                it.kind ==
                    CompositionalDiscoveryInterpreter.TaskKind.PRODUCT_AVAILABILITY
            }
        )

        assertTrue(
            "Missing directions requirement",
            result.tasks.any {
                it.kind ==
                    CompositionalDiscoveryInterpreter.TaskKind.DIRECTIONS
            }
        )

        assertEquals(
            "SafeBoda and Uber must remain separate ride requirements",
            2,
            result.tasks.count {
                it.kind ==
                    CompositionalDiscoveryInterpreter.TaskKind.TRANSPORT_REQUEST
            }
        )

        assertTrue(result.requiresMultipleSources)
    }

    @Test
    fun geographicQuestionRequiresKnowledgeEvidence() {
        assertTrue(
            kinds("What are the 6 hills of Kampala?").contains(
                CompositionalDiscoveryInterpreter.TaskKind.GEOGRAPHIC_KNOWLEDGE
            )
        )
    }

    @Test
    fun marketDirectoryIsDistinctFromInventory() {
        val task = parse("Find markets in Kampala").tasks.single()

        assertEquals(
            CompositionalDiscoveryInterpreter.TaskKind.PLACE_DISCOVERY,
            task.kind
        )
        assertEquals(
            CompositionalDiscoveryInterpreter.EvidenceRequirement.VERIFIED_PLACES,
            task.evidence
        )
    }

    @Test
    fun authorizedPhoneActionRemainsOutsideInterpreter() {
        assertTrue(parse("Open Android Settings").tasks.isEmpty())
        assertTrue(parse("Call the palace").tasks.isEmpty())
    }

    @Test
    fun ordinaryProgrammingQuestionRemainsOutsideInterpreter() {
        assertTrue(
            parse("Explain dependency injection in Kotlin").tasks.isEmpty()
        )
    }

    @Test
    fun blankInputHasNoTasks() {
        assertTrue(parse("   ").tasks.isEmpty())
        assertFalse(parse("   ").requiresMultipleSources)
    }

    @Test
    fun unsupportedRideProviderIsNotInvented() {
        assertTrue(
            parse("Recommend hotels in Kampala").tasks.none {
                it.kind ==
                    CompositionalDiscoveryInterpreter.TaskKind.TRANSPORT_REQUEST
            }
        )
    }

    @Test
    fun hasSpicesIsExplicitInventoryIntent() {
        val tasks = parse("Which market has spices for cooking?").tasks

        assertTrue(tasks.any {
            it.kind ==
                CompositionalDiscoveryInterpreter.TaskKind.PRODUCT_AVAILABILITY &&
                it.product == "spices"
        })
    }
}
