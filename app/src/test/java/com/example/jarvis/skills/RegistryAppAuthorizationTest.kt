package com.example.jarvis.skills

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RegistryAppAuthorizationTest {

    private class FakePhone : JarvisSkill {
        override val name = "PhoneAutomation"
        override val description = "Fake phone for security tests"
        override val keywords = emptyList<String>()

        var executions = 0
        var lastArguments = ""

        override suspend fun execute(command: String): String {
            executions++
            lastArguments = command
            return "Executed"
        }
    }

    private fun registryWithPhone(): Pair<SkillRegistry, FakePhone> {
        val registry = SkillRegistry()
        val phone = FakePhone()
        registry.registerSkill(phone)
        return registry to phone
    }

    private fun launch(packageName: String): JSONObject =
        JSONObject()
            .put("action", "open_app")
            .put("target", packageName)

    @Test
    fun unauthorizedPackagesCannotCreateApproval() = runBlocking {
        for (target in listOf(
            "com.example.unreviewed",
            "com.android.chrome",
            "com.whatsapp.extra",
            "COM.WHATSAPP"
        )) {
            val (registry, phone) = registryWithPhone()

            val result = registry.invokeSkill(
                "PhoneAutomation",
                launch(target)
            )

            assertTrue(result.contains("unavailable"))
            assertNull(registry.pendingToolAction.value)
            assertEquals(0, phone.executions)

            registry.approvePendingToolAction()
            assertEquals(0, phone.executions)
        }
    }

    @Test
    fun malformedAndExtraArgumentsAreRejected() = runBlocking {
        val (registry, phone) = registryWithPhone()

        val requests = listOf(
            launch("not-a-package"),
            launch(""),
            JSONObject()
                .put("action", "open_app")
                .put("target", "com.whatsapp")
                .put("extra", "unexpected"),
            JSONObject()
                .put("action", "toggle_wifi")
                .put("target", "com.whatsapp")
        )

        for (request in requests) {
            registry.invokeSkill("PhoneAutomation", request)
            assertNull(registry.pendingToolAction.value)
        }

        assertEquals(0, phone.executions)
    }

    @Test
    fun unauthorizedRequestCannotReplacePendingApproval() = runBlocking {
        val (registry, phone) = registryWithPhone()

        registry.invokeSkill(
            "PhoneAutomation",
            launch("com.whatsapp")
        )

        assertNotNull(registry.pendingToolAction.value)

        registry.invokeSkill(
            "PhoneAutomation",
            launch("com.example.unreviewed")
        )

        val pending = registry.pendingToolAction.value
        assertNotNull(pending)

        assertEquals(
            "com.whatsapp",
            JSONObject(pending!!.parametersJson).getString("target")
        )

        assertEquals(0, phone.executions)

        registry.approvePendingToolAction()

        assertEquals(1, phone.executions)
        assertEquals(
            "com.whatsapp",
            JSONObject(phone.lastArguments).getString("target")
        )
    }

    @Test
    fun authorizedRequestExecutesOnlyOnce() = runBlocking {
        val (registry, phone) = registryWithPhone()

        registry.invokeSkill(
            "PhoneAutomation",
            launch("com.google.android.youtube")
        )

        assertEquals(0, phone.executions)
        assertNotNull(registry.pendingToolAction.value)

        assertEquals(
            "Executed",
            registry.approvePendingToolAction()
        )

        assertEquals(1, phone.executions)
        assertNull(registry.pendingToolAction.value)

        registry.approvePendingToolAction()
        assertEquals(1, phone.executions)
    }

    @Test
    fun rejectedRequestNeverExecutes() = runBlocking {
        val (registry, phone) = registryWithPhone()

        registry.invokeSkill(
            "PhoneAutomation",
            launch("com.twitter.android")
        )

        assertNotNull(registry.pendingToolAction.value)

        assertTrue(registry.rejectPendingToolAction())
        assertNull(registry.pendingToolAction.value)

        registry.approvePendingToolAction()

        assertEquals(0, phone.executions)
    }
}