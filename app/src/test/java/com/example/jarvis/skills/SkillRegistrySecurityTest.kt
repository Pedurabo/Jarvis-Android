package com.example.jarvis.skills

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillRegistrySecurityTest {

    private class FakeSkill(
        override val name: String
    ) : JarvisSkill {
        override val description = "Test skill"
        override val keywords = emptyList<String>()

        var executions = 0

        override suspend fun execute(command: String): String {
            executions++
            return "Executed"
        }
    }

    @Test
    fun registeredWeatherSkillCanExecute() = runBlocking {
        val registry = SkillRegistry()
        val skill = FakeSkill("weather")
        registry.registerSkill(skill)

        val result = registry.invokeSkill(
            "weather",
            JSONObject().put("city", "Kampala")
        )

        assertEquals("Executed", result)
        assertEquals(1, skill.executions)
    }

    @Test
    fun phoneAutomationCannotExecute() = runBlocking {
        val registry = SkillRegistry()
        val skill = FakeSkill("PhoneAutomation")
        registry.registerSkill(skill)

        val result = registry.invokeSkill(
            "PhoneAutomation",
            JSONObject()
                .put("action", "call")
                .put("target", "123456789")
        )

        assertTrue(result.contains("unavailable"))
        assertEquals(0, skill.executions)
    }

    @Test
    fun unknownAndUnreviewedSkillsCannotExecute() = runBlocking {
        val registry = SkillRegistry()
        val skill = FakeSkill("UnreviewedSkill")
        registry.registerSkill(skill)

        val unknown = registry.invokeSkill(
            "nonexistent",
            JSONObject()
        )

        val unreviewed = registry.invokeSkill(
            "UnreviewedSkill",
            JSONObject()
        )

        assertTrue(unknown.contains("not registered"))
        assertTrue(unreviewed.contains("unavailable"))
        assertEquals(0, skill.executions)
    }
}