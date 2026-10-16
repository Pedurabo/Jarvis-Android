package com.example.jarvis.skills

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ToolApprovalIntegrationTest {
    private class FakePhone : JarvisSkill {
        override val name = "PhoneAutomation"
        override val description = "test"
        override val keywords = emptyList<String>()
        var executions = 0
        var lastArguments = ""
        override suspend fun execute(command: String): String {
            executions++
            lastArguments = command
            return "Executed"
        }
    }

    @Test fun pendingRequiresExplicitApproval() = runBlocking {
        val registry = SkillRegistry()
        val phone = FakePhone()
        registry.registerSkill(phone)
        val params = JSONObject().put("action", "open_app").put("target", "com.google.android.youtube")
        val result = registry.invokeSkill("PhoneAutomation", params)
        assertTrue(result.contains("unavailable"))
        assertEquals(0, phone.executions)
        assertNotNull(registry.pendingToolAction.value)
        assertEquals("Executed", registry.approvePendingToolAction())
        assertEquals(1, phone.executions)
        assertEquals("com.google.android.youtube", JSONObject(phone.lastArguments).getString("target"))
        registry.approvePendingToolAction()
        assertEquals(1, phone.executions)
    }

    @Test fun rejectionNeverExecutes() = runBlocking {
        val registry = SkillRegistry()
        val phone = FakePhone()
        registry.registerSkill(phone)
        registry.invokeSkill("PhoneAutomation", JSONObject().put("action", "call").put("target", "+256700000000"))
        assertTrue(registry.rejectPendingToolAction())
        assertNull(registry.pendingToolAction.value)
        registry.approvePendingToolAction()
        assertEquals(0, phone.executions)
    }

    @Test fun invalidAndUnimplementedActionsAreDenied() = runBlocking {
        val registry = SkillRegistry()
        val phone = FakePhone()
        registry.registerSkill(phone)
        registry.invokeSkill("PhoneAutomation", JSONObject().put("action", "toggle_wifi"))
        registry.invokeSkill("PhoneAutomation", JSONObject().put("action", "call").put("target", "#123"))
        registry.invokeSkill("PhoneAutomation", JSONObject().put("action", "open_app").put("target", "malformed"))
        assertEquals(0, phone.executions)
        assertNull(registry.pendingToolAction.value)
    }

    @Test fun laterRequestsCannotReplaceAnAwaitingApproval() = runBlocking {
        val registry = SkillRegistry()
        val phone = FakePhone()
        registry.registerSkill(phone)
        registry.invokeSkill("PhoneAutomation", JSONObject().put("action", "open_app").put("target", "com.whatsapp"))
        registry.invokeSkill("PhoneAutomation", JSONObject().put("action", "open_app").put("target", "com.twitter.android"))
        registry.approvePendingToolAction()
        assertEquals(1, phone.executions)
        assertEquals("com.whatsapp", JSONObject(phone.lastArguments).getString("target"))
    }
}
