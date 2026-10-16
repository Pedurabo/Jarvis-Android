package com.example.jarvis.skills

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DialAuthorizationTest {

    private class FakePhone : JarvisSkill {
        override val name = "PhoneAutomation"
        override val description = "Fake phone for authorization tests"
        override val keywords = emptyList<String>()

        var executions = 0
        var lastArguments = ""

        override suspend fun execute(command: String): String {
            executions++
            lastArguments = command
            return "Executed"
        }
    }

    private fun setup(): Pair<SkillRegistry, FakePhone> {
        val registry = SkillRegistry()
        val phone = FakePhone()
        registry.registerSkill(phone)
        return registry to phone
    }

    private fun dial(number: String): JSONObject =
        JSONObject()
            .put("action", "call")
            .put("target", number)

    @Test
    fun validInternationalNumberRequiresApproval() = runBlocking {
        val (registry, phone) = setup()

        val result = registry.invokeSkill(
            "PhoneAutomation",
            dial("+256700000000")
        )

        assertTrue(result.contains("explicitly approved"))
        assertNotNull(registry.pendingToolAction.value)
        assertEquals(0, phone.executions)
    }

    @Test
    fun invalidNumbersAreRejectedBeforeApproval() = runBlocking {
        val invalidNumbers = listOf(
            "",
            "12",
            "abc",
            "0700 000 000",
            "+",
            "++256700000000",
            "1234567890123456",
            "12345#",
            "*123#"
        )

        for (number in invalidNumbers) {
            val (registry, phone) = setup()

            val result = registry.invokeSkill(
                "PhoneAutomation",
                dial(number)
            )

            assertTrue(result.contains("Invalid phone number"))
            assertNull(registry.pendingToolAction.value)
            assertEquals(0, phone.executions)
        }
    }

    @Test
    fun extraAndMissingArgumentsAreRejected() = runBlocking {
        val requests = listOf(
            JSONObject().put("action", "call"),
            JSONObject().put("target", "+256700000000"),
            dial("+256700000000").put("extra", "unexpected")
        )

        for (request in requests) {
            val (registry, phone) = setup()

            val result = registry.invokeSkill(
                "PhoneAutomation",
                request
            )

            assertTrue(result.contains("unavailable"))
            assertNull(registry.pendingToolAction.value)
            assertEquals(0, phone.executions)
        }
    }

    @Test
    fun unsupportedOperationsCannotRequestApproval() = runBlocking {
        for (operation in listOf(
            "toggle_wifi",
            "toggle_flashlight",
            "set_brightness",
            "send_sms"
        )) {
            val (registry, phone) = setup()

            val result = registry.invokeSkill(
                "PhoneAutomation",
                JSONObject()
                    .put("action", operation)
                    .put("target", "+256700000000")
            )

            assertTrue(result.contains("Unsupported phone action"))
            assertNull(registry.pendingToolAction.value)
            assertEquals(0, phone.executions)
        }
    }

    @Test
    fun rejectionPreventsExecution() = runBlocking {
        val (registry, phone) = setup()

        registry.invokeSkill(
            "PhoneAutomation",
            dial("+256700000000")
        )

        assertNotNull(registry.pendingToolAction.value)
        assertTrue(registry.rejectPendingToolAction())
        assertNull(registry.pendingToolAction.value)

        assertEquals(
            "No pending action to approve.",
            registry.approvePendingToolAction()
        )

        assertEquals(0, phone.executions)
    }

    @Test
    fun approvalExecutesExactlyOnce() = runBlocking {
        val (registry, phone) = setup()

        registry.invokeSkill(
            "PhoneAutomation",
            dial("+256700000000")
        )

        assertEquals(0, phone.executions)

        assertEquals(
            "Executed",
            registry.approvePendingToolAction()
        )

        assertEquals(1, phone.executions)

        assertEquals(
            "+256700000000",
            JSONObject(phone.lastArguments).getString("target")
        )

        assertNull(registry.pendingToolAction.value)

        registry.approvePendingToolAction()

        assertEquals(1, phone.executions)
    }

    @Test
    fun laterDialRequestCannotReplacePendingApproval() = runBlocking {
        val (registry, phone) = setup()

        registry.invokeSkill(
            "PhoneAutomation",
            dial("+256700000000")
        )

        val secondResult = registry.invokeSkill(
            "PhoneAutomation",
            dial("+256711111111")
        )

        assertTrue(secondResult.contains("another action"))
        assertNotNull(registry.pendingToolAction.value)

        assertEquals(
            "+256700000000",
            JSONObject(
                registry.pendingToolAction.value!!.parametersJson
            ).getString("target")
        )

        assertEquals(0, phone.executions)

        registry.approvePendingToolAction()

        assertEquals(1, phone.executions)

        assertEquals(
            "+256700000000",
            JSONObject(phone.lastArguments).getString("target")
        )
    }
}