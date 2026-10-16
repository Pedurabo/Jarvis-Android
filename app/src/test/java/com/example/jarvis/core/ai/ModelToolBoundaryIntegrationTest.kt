package com.example.jarvis.core.ai

import com.example.jarvis.skills.JarvisSkill
import com.example.jarvis.skills.SkillRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelToolBoundaryIntegrationTest {

    private class FakeSkill(
        override val name: String
    ) : JarvisSkill {
        override val description = "Integration test skill"
        override val keywords = emptyList<String>()

        var executions = 0

        override suspend fun execute(command: String): String {
            executions++
            return "Executed"
        }
    }

    /**
     * Exercises the same three authorization components used by
     * the controller. This helper never directly executes skills.
     */
    private suspend fun dispatch(
        registry: SkillRegistry,
        response: String,
        mode: PromptModePolicy.Mode
    ): String? {
        val parsed = ModelActionParser.parse(
            response,
            allowToolCalls = mode == PromptModePolicy.Mode.TOOL_AWARE
        )

        val candidate = parsed as? ModelActionParser.Result.ToolCall
            ?: return null

        if (!ModelToolExecutionPolicy.isAllowed(
                mode,
                true,
                candidate.name
            )
        ) {
            return null
        }

        return registry.invokeSkill(candidate.name, candidate.params)
    }

    @Test
    fun ordinaryConversationDoesNotInvokeSkills() = runBlocking {
        val registry = SkillRegistry()
        val weather = FakeSkill("weather")
        registry.registerSkill(weather)

        val result = dispatch(
            registry,
            "Kotlin variables can be declared using val or var.",
            PromptModePolicy.Mode.CONVERSATION
        )

        assertNull(result)
        assertEquals(0, weather.executions)
    }

    @Test
    fun conversationModeRejectsModelToolRequest() = runBlocking {
        val registry = SkillRegistry()
        val phone = FakeSkill("PhoneAutomation")
        registry.registerSkill(phone)

        val result = dispatch(
            registry,
            """{"tool":"PhoneAutomation","params":{"action":"call","target":"+12025550100"}}""",
            PromptModePolicy.Mode.CONVERSATION
        )

        assertNull(result)
        assertEquals(0, phone.executions)
        assertNull(registry.pendingToolAction.value)
    }

    @Test
    fun malformedToolJsonCannotReachRegistry() = runBlocking {
        val registry = SkillRegistry()
        val phone = FakeSkill("PhoneAutomation")
        registry.registerSkill(phone)

        val result = dispatch(
            registry,
            """{"tool":"PhoneAutomation","params":{"action":"call","target":"+12025550100",}}""",
            PromptModePolicy.Mode.TOOL_AWARE
        )

        assertNull(result)
        assertEquals(0, phone.executions)
        assertNull(registry.pendingToolAction.value)
    }

    @Test
    fun approvedReadOnlySkillCanExecute() = runBlocking {
        val registry = SkillRegistry()
        val weather = FakeSkill("weather")
        registry.registerSkill(weather)

        val result = dispatch(
            registry,
            """{"tool":"weather","params":{"city":"Kampala"}}""",
            PromptModePolicy.Mode.TOOL_AWARE
        )

        assertEquals("Executed", result)
        assertEquals(1, weather.executions)
    }

    @Test
    fun phoneRequestRequiresExplicitApproval() = runBlocking {
        val registry = SkillRegistry()
        val phone = FakeSkill("PhoneAutomation")
        registry.registerSkill(phone)

        val result = dispatch(
            registry,
            """{"tool":"PhoneAutomation","params":{"action":"call","target":"+12025550100"}}""",
            PromptModePolicy.Mode.TOOL_AWARE
        )

        assertNotNull(result)
        assertTrue(result!!.contains("explicitly approved"))
        assertEquals(0, phone.executions)
        assertNotNull(registry.pendingToolAction.value)

        assertTrue(registry.rejectPendingToolAction())
        assertNull(registry.pendingToolAction.value)
        assertEquals(0, phone.executions)
    }

    @Test
    fun unreviewedSkillCannotExecute() = runBlocking {
        val registry = SkillRegistry()
        val unreviewed = FakeSkill("UnreviewedSkill")
        registry.registerSkill(unreviewed)

        val result = dispatch(
            registry,
            """{"tool":"UnreviewedSkill","params":{}}""",
            PromptModePolicy.Mode.TOOL_AWARE
        )

        assertNotNull(result)
        assertTrue(result!!.contains("unavailable"))
        assertEquals(0, unreviewed.executions)
        assertNull(registry.pendingToolAction.value)
    }
}