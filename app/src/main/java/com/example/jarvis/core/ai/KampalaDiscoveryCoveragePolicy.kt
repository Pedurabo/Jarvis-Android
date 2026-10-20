package com.example.jarvis.core.ai

import com.example.jarvis.core.network.KampalaPlacesQueryRoute

/**
 * Narrow coverage routing for Kampala discovery requests observed
 * during physical-device testing.
 *
 * No addresses, live availability, bookings or payments are generated.
 * Shopping uses the existing verified Places repository.
 */
internal object KampalaDiscoveryCoveragePolicy {

    // Distinguish local place discovery from general informational requests.
    // These patterns recognize intent only; they never supply place facts.
    private val foodPlaces = Regex(
        """^(?:what\s+are|which\s+are|tell\s+me\s+about|find|list|show\s+me|recommend)\s+(?:the\s+)?(?:(?:most\s+)?famous\s+|popular\s+|best\s+|different\s+)?(?:food\s+points?|food\s+places?|restaurants?|places\s+to\s+eat)\s+in\s+kampala[.!?]*$""",
        RegexOption.IGNORE_CASE
    )

    private val cinemaDiscovery = Regex(
        """^(?:what\s+are|which\s+are|tell\s+me\s+about|find|list|show\s+me|recommend)\s+(?:the\s+)?(?:best\s+|famous\s+|popular\s+|different\s+)?(?:cinemas?|cinema\s+halls?|movie\s+theaters?|movie\s+theatres?)\s+in\s+kampala[.!?]*$""",
        RegexOption.IGNORE_CASE
    )

    private val footballDiscovery = Regex(
        """^(?:(?:what|which)\s+are\s+(?:the\s+)?(?:different\s+|main\s+|best\s+|popular\s+)?|(?:tell\s+me\s+about|find|list|show\s+me|recommend)\s+(?:the\s+)?(?:different\s+|main\s+|best\s+|popular\s+)?)(?:football\s+grounds?|football\s+pitches|playgrounds?|sports\s+grounds?|stadiums?)\s+in\s+kampala[.!?]*$""",
        RegexOption.IGNORE_CASE
    )

    private val shortFootballDiscovery = Regex(
        """^(?:football\s+grounds?|football\s+pitches|playgrounds?|sports\s+grounds?)[.!?]*$""",
        RegexOption.IGNORE_CASE
    )
    private val shopping = Regex(
        """^(?:tell\s+me\s+about|describe|list|show\s+me)\s+(?:the\s+)?(?:different\s+)?shopping\s+places\s+in\s+kampala[.!?]*$""",
        RegexOption.IGNORE_CASE
    )

    private val parliament = Regex(
        """^(?:where\s+is|where\s+can\s+i\s+find|where\s+is\s+the\s+location\s+of|what\s+is\s+the\s+location\s+of)\s+(?:the\s+)?(?:parliament|parliament\s+building|parliament\s+of\s+uganda)(?:\s+(?:found|located))?(?:\s+in\s+kampala)?[.!?]*$""",
        RegexOption.IGNORE_CASE
    )

    private val worship = Regex(
        """^(?:tell\s+me\s+about|describe|list|show\s+me)\s+(?:the\s+)?(?:different\s+)?(?:worship\s+places|places\s+of\s+worship|religious\s+institutions)\s+in\s+kampala[.!?]*$""",
        RegexOption.IGNORE_CASE
    )

    private val sports = Regex(
        """^(?:tell\s+me\s+)?(?:a\s+)?list\s+(?:of\s+)?(?:playgrounds?|football\s+grounds?|playgrounds?\s+or\s+football\s+grounds?|football\s+grounds?\s+or\s+playgrounds?)\s+in\s+kampala[.!?]*$""",
        RegexOption.IGNORE_CASE
    )

    private val roads = Regex(
        """^(?:tell\s+me\s+about|describe|list|show\s+me)\s+(?:the\s+)?(?:different\s+|main\s+|major\s+)?(?:roads|road\s+networks)\s+in\s+kampala[.!?]*$""",
        RegexOption.IGNORE_CASE
    )

    fun searchRequest(
        input: String
    ): KampalaPlacesQueryRoute.SearchRequest? {
        val query = normalize(input)
        return when {
            shopping.matches(query) ->
                KampalaPlacesQueryRoute.SearchRequest(category = "shopping")

            foodPlaces.matches(query) ->
                KampalaPlacesQueryRoute.SearchRequest(category = "restaurant")

            else -> null
        }
    }

    fun notice(input: String): String? {
        val query = normalize(input)

        return when {
            parliament.matches(query) ->
                "I can't currently confirm the Parliament building's " +
                    "address or directions from my verified Kampala catalog. " +
                    "Please consult the Parliament of Uganda's official " +
                    "information or a trusted map. This does not mean " +
                    "the building is absent."

            worship.matches(query) ->
                "Kampala has places of worship serving different faith " +
                    "communities, but I don't yet have a verified combined " +
                    "directory covering them. I can't provide an accurate " +
                    "list of names and addresses from the current catalog. " +
                    "Please use official institution listings or trusted " +
                    "local sources while coverage is expanded."

            sports.matches(query) ->
                "I don't yet have a verified list of playgrounds and " +
                    "football grounds in Kampala. Missing directory " +
                    "entries do not mean these facilities don't exist. " +
                    "I won't invent names, addresses or availability."

            roads.matches(query) ->
                "I don't yet have a sufficiently verified Kampala " +
                    "road-network reference to give a dependable list " +
                    "and descriptions of roads. I won't guess about " +
                    "road names, connections or current conditions. " +
                    "Please consult an authoritative road map or " +
                    "official transport source."

            cinemaDiscovery.matches(query) ->
                "I don't yet have verified cinema listings and comparable " +
                    "current information to identify the best cinema halls " +
                    "in Kampala. I won't invent cinema rankings, prices, " +
                    "facilities or availability. Please check official " +
                    "cinema listings while directory coverage expands."

            footballDiscovery.matches(query) ||
                shortFootballDiscovery.matches(query) ->
                "I don't yet have a verified directory of football " +
                    "grounds and sports pitches in Kampala. Missing " +
                    "catalog entries do not mean these facilities don't " +
                    "exist. I won't invent names, addresses or availability."

            else -> null
        }
    }

    private fun normalize(input: String): String =
        input.trim().replace(Regex("""\s+"""), " ")
}
