package com.example.jarvis.skills

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovedAppPackagesTest {

    @Test
    fun permitsSupportedApplications() {
        assertTrue(ApprovedAppPackages.isAllowed("com.android.settings"))
        assertTrue(ApprovedAppPackages.isAllowed("com.whatsapp"))
        assertTrue(ApprovedAppPackages.isAllowed("com.twitter.android"))
        assertTrue(ApprovedAppPackages.isAllowed("com.google.android.youtube"))
    }

    @Test
    fun rejectsUnreviewedApplications() {
        assertFalse(ApprovedAppPackages.isAllowed("com.example.unreviewed"))
        assertFalse(ApprovedAppPackages.isAllowed("com.android.chrome"))
    }

    @Test
    fun rejectsMalformedAndEmptyTargets() {
        assertFalse(ApprovedAppPackages.isAllowed(""))
        assertFalse(ApprovedAppPackages.isAllowed("whatsapp"))
        assertFalse(ApprovedAppPackages.isAllowed("../com.whatsapp"))
    }

    @Test
    fun requiresExactPackageMatching() {
        assertFalse(ApprovedAppPackages.isAllowed("COM.WHATSAPP"))
        assertFalse(ApprovedAppPackages.isAllowed("com.whatsapp.extra"))
        assertFalse(ApprovedAppPackages.isAllowed(" com.whatsapp "))
    }
}