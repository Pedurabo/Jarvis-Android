package com.example.jarvis.skills

/**
 * Central authorization policy for supported Android app launches.
 *
 * This policy does not launch applications or grant user approval.
 */
object ApprovedAppPackages {

    private val allowedPackages = setOf(
        "com.android.settings",
        "com.whatsapp",
        "com.twitter.android",
        "com.google.android.youtube"
    )

    fun isAllowed(packageName: String): Boolean =
        packageName in allowedPackages
}