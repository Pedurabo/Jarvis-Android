package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationTaskStateTest {

    @Test
    fun emergencyConversationRetainsRelevantDetails() {
        val result = ConversationTaskState.apply(
            changes = listOf(
                ConversationTaskState.Change.Start(
                    kind = ConversationTaskState.Kind.MEDICAL_ASSISTANCE,
                    goal = "Help reach medical care",
                    requiresUrgentCare = true
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.DESTINATION,
                    "Case Clinic",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.ORIGIN,
                    "Luwum Street",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.SUGGESTED_AREA,
                    "Nakasero",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                )
            )
        )!!

        assertTrue(result.requiresUrgentCare)
        assertEquals(
            "Case Clinic",
            result.get(ConversationTaskState.Field.DESTINATION)?.value
        )
        assertEquals(
            "Luwum Street",
            result.get(ConversationTaskState.Field.ORIGIN)?.value
        )
        assertEquals(
            "Nakasero",
            result.get(ConversationTaskState.Field.SUGGESTED_AREA)?.value
        )
    }

    @Test
    fun suggestedAreaRemainsUnverified() {
        val result = ConversationTaskState.apply(
            changes = listOf(
                ConversationTaskState.Change.Start(
                    ConversationTaskState.Kind.NAVIGATION,
                    "Reach clinic"
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.SUGGESTED_AREA,
                    "Nakasero",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                )
            )
        )!!

        assertEquals(
            ConversationTaskState.Provenance.USER_SUPPLIED,
            result.get(
                ConversationTaskState.Field.SUGGESTED_AREA
            )?.provenance
        )
        assertNull(
            result.get(ConversationTaskState.Field.DESTINATION)
        )
    }

    @Test
    fun correctingOriginPreservesDestination() {
        val original = ConversationTaskState.apply(
            changes = listOf(
                ConversationTaskState.Change.Start(
                    ConversationTaskState.Kind.NAVIGATION,
                    "Reach Watoto Church"
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.DESTINATION,
                    "Watoto Church",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.ORIGIN,
                    "Nakawa",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                )
            )
        )

        val corrected = ConversationTaskState.reduce(
            original,
            ConversationTaskState.Change.SetField(
                ConversationTaskState.Field.ORIGIN,
                "Ntinda",
                ConversationTaskState.Provenance.USER_SUPPLIED
            )
        )!!

        assertEquals(
            "Ntinda",
            corrected.get(
                ConversationTaskState.Field.ORIGIN
            )?.value
        )
        assertEquals(
            "Watoto Church",
            corrected.get(
                ConversationTaskState.Field.DESTINATION
            )?.value
        )
        assertEquals(
            "Nakawa",
            original!!.get(
                ConversationTaskState.Field.ORIGIN
            )?.value
        )
    }

    @Test
    fun switchingTasksClearsOldContext() {
        val navigation = ConversationTaskState.apply(
            changes = listOf(
                ConversationTaskState.Change.Start(
                    ConversationTaskState.Kind.NAVIGATION,
                    "Find Hotel Africana"
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.DESTINATION,
                    "Hotel Africana",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                )
            )
        )

        val shopping = ConversationTaskState.reduce(
            navigation,
            ConversationTaskState.Change.Start(
                ConversationTaskState.Kind.SHOPPING,
                "Buy matooke"
            )
        )!!

        assertEquals(
            ConversationTaskState.Kind.SHOPPING,
            shopping.kind
        )
        assertNull(
            shopping.get(ConversationTaskState.Field.DESTINATION)
        )
        assertFalse(shopping.requiresUrgentCare)
    }

    @Test
    fun provenanceIsRetained() {
        val task = ConversationTaskState.apply(
            changes = listOf(
                ConversationTaskState.Change.Start(
                    ConversationTaskState.Kind.PLACE_DISCOVERY,
                    "Find place"
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.DESTINATION,
                    "Example Place",
                    ConversationTaskState.Provenance.DIRECTORY_LISTING
                )
            )
        )!!

        assertEquals(
            ConversationTaskState.Provenance.DIRECTORY_LISTING,
            task.get(
                ConversationTaskState.Field.DESTINATION
            )?.provenance
        )
    }

    @Test
    fun clearingTaskRemovesState() {
        val task = ConversationTaskState.reduce(
            null,
            ConversationTaskState.Change.Start(
                ConversationTaskState.Kind.NAVIGATION,
                "Find destination"
            )
        )

        assertNotNull(task)
        assertNull(
            ConversationTaskState.reduce(
                task,
                ConversationTaskState.Change.Clear
            )
        )
    }

    @Test
    fun blankFieldCannotEraseKnownOrigin() {
        val task = ConversationTaskState.apply(
            changes = listOf(
                ConversationTaskState.Change.Start(
                    ConversationTaskState.Kind.NAVIGATION,
                    "Reach destination"
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.ORIGIN,
                    "Makindye",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                )
            )
        )

        val result = ConversationTaskState.reduce(
            task,
            ConversationTaskState.Change.SetField(
                ConversationTaskState.Field.ORIGIN,
                " ",
                ConversationTaskState.Provenance.USER_SUPPLIED
            )
        )

        assertEquals(
            "Makindye",
            result?.get(
                ConversationTaskState.Field.ORIGIN
            )?.value
        )
    }

    @Test
    fun removeFieldPreservesOtherDetails() {
        val task = ConversationTaskState.apply(
            changes = listOf(
                ConversationTaskState.Change.Start(
                    ConversationTaskState.Kind.NAVIGATION,
                    "Reach clinic"
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.ORIGIN,
                    "Luwum Street",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.DESTINATION,
                    "Case Clinic",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                )
            )
        )

        val updated = ConversationTaskState.reduce(
            task,
            ConversationTaskState.Change.RemoveField(
                ConversationTaskState.Field.ORIGIN
            )
        )!!

        assertNull(
            updated.get(
                ConversationTaskState.Field.ORIGIN
            )
        )
        assertEquals(
            "Case Clinic",
            updated.get(
                ConversationTaskState.Field.DESTINATION
            )?.value
        )
    }

    @Test
    fun noActiveTaskMeansNoFieldUpdate() {
        assertNull(
            ConversationTaskState.reduce(
                null,
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.ORIGIN,
                    "Nakawa",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                )
            )
        )
    }

    @Test
    fun marketComparisonCanRetainItemAndPreference() {
        val task = ConversationTaskState.apply(
            changes = listOf(
                ConversationTaskState.Change.Start(
                    ConversationTaskState.Kind.MARKET_COMPARISON,
                    "Compare prices"
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.ITEM,
                    "Tomatoes",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.PREFERENCE,
                    "Lowest price",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                )
            )
        )!!

        assertEquals(
            "Tomatoes",
            task.get(
                ConversationTaskState.Field.ITEM
            )?.value
        )
        assertEquals(
            "Lowest price",
            task.get(
                ConversationTaskState.Field.PREFERENCE
            )?.value
        )
    }
}