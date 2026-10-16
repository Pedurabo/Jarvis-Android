package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLaunchCommandRouteTest {

    @Test
    fun resolvesAndroidSettings() {
        assertEquals(
            "com.android.settings",
            AppLaunchCommandRoute.resolve("Open Android Settings")?.packageName
        )
    }

    @Test
    fun resolvesWhatsApp() {
        assertEquals(
            "com.whatsapp",
            AppLaunchCommandRoute.resolve("Open WhatsApp")?.packageName
        )
    }

    @Test
    fun resolvesXAndTwitterToSamePackage() {
        assertEquals(
            "com.twitter.android",
            AppLaunchCommandRoute.resolve("open x")?.packageName
        )

        assertEquals(
            "com.twitter.android",
            AppLaunchCommandRoute.resolve("Launch Twitter")?.packageName
        )
    }

    @Test
    fun resolvesYouTube() {
        assertEquals(
            "com.google.android.youtube",
            AppLaunchCommandRoute.resolve("Please open YouTube.")?.packageName
        )
    }

    @Test
    fun rejectsQuestionsAndNegativeInstructions() {
        assertNull(
            AppLaunchCommandRoute.resolve("How do I open WhatsApp?")
        )

        assertNull(
            AppLaunchCommandRoute.resolve("Do not open WhatsApp")
        )
    }

    @Test
    fun rejectsUnknownApps() {
        assertNull(
            AppLaunchCommandRoute.resolve("Open an unknown application")
        )
    }

    @Test
    fun rejectsCommandsWithAdditionalActions() {
        assertNull(
            AppLaunchCommandRoute.resolve(
                "Open WhatsApp and send a message"
            )
        )

        assertNull(
            AppLaunchCommandRoute.resolve(
                "Open Android Settings then turn on WiFi"
            )
        )
    }

    @Test
    fun rejectsNonLaunchCommands() {
        assertNull(
            AppLaunchCommandRoute.resolve("Call my friend")
        )

        assertNull(
            AppLaunchCommandRoute.resolve("What is YouTube?")
        )
    }
}