package com.example.jarvis

import com.example.jarvis.core.network.KampalaPlacesQueryRoute
import com.example.jarvis.core.network.KampalaPlacesRepository
import com.example.jarvis.core.network.KampalaPlacesResultPolicy

/**
 * Conservative fallback for unsupported Kampala-local requests.
 *
 * Runs only after existing discovery routing returns Pass.
 * Does not invent place facts, open apps, or book services.
 */
internal object KampalaLocalIntentGuard {

    private val rideRequest = Regex(
        """\b(?:safeboda|safe\s*boda)\b""",
        RegexOption.IGNORE_CASE
    )

    private val rideAction = Regex(
        """\b(?:book|order|request|get|call|arrange|ride|boda|pickup|pick\s+me\s+up)\b""",
        RegexOption.IGNORE_CASE
    )

    private val locationRequest = Regex(
        """^(?:where\s+is|where\s+can\s+i\s+find|what\s+is\s+the\s+address\s+of|how\s+do\s+i\s+get\s+to|need\s+directions\s+to|give\s+me\s+directions\s+to|directions\s+to|locate|find|show\s+me)\s+(?:the\s+)?(.+?)[?.!]*$""",
        RegexOption.IGNORE_CASE
    )

    private val localPlaceWords = Regex(
        """\b(?:hotel|mosque|ministry|court|bank|hospital|mall|embassy|office|building|market|museum|kampala|nakasero|kololo|ntinda|bugolobi)\b""",
        RegexOption.IGNORE_CASE
    )

    private val generalQuestion = Regex(
        """^(?:what|why|how|explain|describe|compare|write|create|generate|summarize)\b""",
        RegexOption.IGNORE_CASE
    )

    suspend fun answerOrNull(
        input: String,
        repository: KampalaPlacesRepository
    ): String? {
        val text = input.trim().replace(Regex("""\s+"""), " ")

        if (text.isBlank()) return null

        if (rideRequest.containsMatchIn(text) &&
            rideAction.containsMatchIn(text)
        ) {
            return "I can help you plan a SafeBoda trip, but " +
                "this experimental Jarvis app cannot book or " +
                "request a ride. Please use the official " +
                "SafeBoda app to choose your pickup point, " +
                "confirm the destination, check the fare, " +
                "and authorize the ride. No booking has been made."
        }

        // Do not intercept ordinary informational conversations.
        if (generalQuestion.containsMatchIn(text) &&
            !text.startsWith("how do i get to", ignoreCase = true)
        ) {
            return null
        }

        val match = locationRequest.matchEntire(text) ?: return null

        val destination = match.groupValues[1]
            .replace(
                Regex("""\s+in\s+kampala$""", RegexOption.IGNORE_CASE),
                ""
            )
            .trim()

        if (destination.length !in 5..80) return null

        if (!localPlaceWords.containsMatchIn(destination)) {
            return null
        }

        if (!destination.matches(
                Regex("""[A-Za-z][A-Za-z0-9 '&()/-]+""")
            )
        ) {
            return null
        }

        // Named queries request records; they do not establish
        // that a destination exists or that directions are known.
        val request = KampalaPlacesQueryRoute.SearchRequest(
            query = destination
        )

        val result = repository.search(
            query = request.query,
            area = request.area,
            category = request.category
        )

        return KampalaPlacesResultPolicy.format(request, result)
    }
}