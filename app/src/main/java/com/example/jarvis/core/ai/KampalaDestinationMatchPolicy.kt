package com.example.jarvis.core.ai

/**
 * CDI.5.57-A: Exact-match selection for directory destinations.
 *
 * The policy does not invent places or fuzzy-match names.
 * An index refers only to an entry supplied by the repository.
 */
internal object KampalaDestinationMatchPolicy {

    private fun normalized(value: String): String =
        value.trim()
            .replace(Regex("""\s+"""), " ")
            .lowercase()

    fun exactIndices(
        requestedName: String,
        entries: List<Pair<String, String>>
    ): List<Int> {
        val requested = normalized(requestedName)

        if (requested.isBlank()) return emptyList()

        val seen = mutableSetOf<Pair<String, String>>()

        return entries.indices.filter { index ->
            val entry = entries[index]
            val name = normalized(entry.first)
            val address = normalized(entry.second)

            name == requested &&
                name.isNotBlank() &&
                address.isNotBlank() &&
                seen.add(name to address)
        }
    }
}