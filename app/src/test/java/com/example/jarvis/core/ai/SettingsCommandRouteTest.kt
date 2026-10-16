package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCommandRouteTest {

    @Test
    fun recognizesDirectSettingsRequest() {
        assertTrue(
            SettingsCommandRoute.isSettingsLaunch(
                "Open Android Settings"
            )
        )
    }

    @Test
    fun recognizesObservedDeviceCommand() {
        assertTrue(
            SettingsCommandRoute.isSettingsLaunch(
                "open android settings. ask for my approval before doing anything"
            )
        )
    }

    @Test
    fun recognizesPoliteSettingsRequest() {
        assertTrue(
            SettingsCommandRoute.isSettingsLaunch(
                "Please open the settings app."
            )
        )
    }

    @Test
    fun doesNotInterpretQuestionsAsActions() {
        assertFalse(
            SettingsCommandRoute.isSettingsLaunch(
                "How do I open Android Settings?"
            )
        )
    }

    @Test
    fun doesNotInterpretNegativeInstructionsAsActions() {
        assertFalse(
            SettingsCommandRoute.isSettingsLaunch(
                "Do not open Android Settings"
            )
        )
    }

    @Test
    fun doesNotRouteOtherAppsOrPhoneCalls() {
        assertFalse(
            SettingsCommandRoute.isSettingsLaunch(
                "Open my banking app"
            )
        )
        assertFalse(
            SettingsCommandRoute.isSettingsLaunch(
                "Call 123456789"
            )
        )
    }
}