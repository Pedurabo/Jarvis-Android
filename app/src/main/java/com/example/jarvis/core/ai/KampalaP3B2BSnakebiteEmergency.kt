package com.example.jarvis.core.ai

/**
 * P3-B2B — urgent snakebite reporting safeguard.
 *
 * This is a deterministic safety response.
 * No geographic lookup, booking, payment or emergency-service
 * contact is performed.
 */
internal object KampalaP3B2BSnakebiteEmergency {

    private val reports = listOf(
        Regex(
            """\b(?:been|was|got)\s+bitten\s+by\s+(?:a\s+)?snake\b""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\b(?:a\s+)?snake\s+(?:just\s+)?bit\s+(?:me|us|him|her|them|my\s+\w+)\b""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\bi(?:'ve| have)?\s+(?:a\s+)?snake[\s-]?bite\b""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\bi\s+(?:was|got)\s+snake[\s-]?bitten\b""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\bsnake[\s-]?bitten\b""",
            RegexOption.IGNORE_CASE
        )
    )

    fun noticeOrNull(message: String): String? {
        val text = message.trim()

        if (text.isEmpty() || text.length > 1200) return null

        if (reports.none { it.containsMatchIn(text) }) {
            return null
        }

        return buildString {
            appendLine("URGENT: Possible snakebite")
            appendLine()

            appendLine(
                "Seek emergency medical care immediately. " +
                    "Snake venom can cause serious harm even " +
                    "before symptoms appear."
            )
            appendLine()

            appendLine(
                "Move away from the snake and avoid another bite. " +
                    "Keep the person as still and calm as possible. " +
                    "Do not walk unnecessarily."
            )
            appendLine()

            appendLine(
                "Keep the bitten limb still. Remove rings, watches " +
                    "or other tight items if this can be done " +
                    "without excessive movement."
            )
            appendLine()

            appendLine(
                "Do not apply a tourniquet, cut the wound, " +
                    "suck out venom, apply ice, or attempt " +
                    "to catch or handle the snake."
            )
            appendLine()

            appendLine(
                "Arrange urgent transport to a healthcare " +
                    "facility equipped to assess and treat " +
                    "snakebite. Tell medical staff when " +
                    "the bite occurred, if known."
            )
            appendLine()

            append(
                "I have not contacted emergency services, " +
                    "confirmed a hospital location, or arranged transport."
            )
        }
    }
}