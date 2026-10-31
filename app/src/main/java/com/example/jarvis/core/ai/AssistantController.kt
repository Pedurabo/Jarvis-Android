package com.example.jarvis.core.ai

import android.util.Log
import com.example.jarvis.core.chat.LocalChatRepository
import com.example.jarvis.core.network.KampalaPlacesQueryRoute
import com.example.jarvis.core.network.KampalaPlacesRepository
import com.example.jarvis.core.network.KampalaKnowledgeQueryRoute
import com.example.jarvis.core.network.KampalaKnowledgeRepository
import com.example.jarvis.core.network.KampalaPlacesResponseFormatter
import com.example.jarvis.core.network.KampalaPlacesResultPolicy
import com.example.jarvis.core.network.KampalaNamedDestinationRoute
import com.example.jarvis.core.ai.KampalaGroundingSafetyPolicy
import com.example.jarvis.core.context.ContextManager
import com.example.jarvis.skills.SkillRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantController @Inject internal constructor(
    private val contextManager: ContextManager,
    private val modelRouter: ModelRouter,
    private val conversationRepository: ConversationRepository,
    private val skillRegistry: SkillRegistry,
    private val personalizationEngine: PersonalizationEngine,
    private val feedbackManager: FeedbackManager,
    private val retrievalManager: RetrievalManager,
    private val responseValidator: ResponseValidator,
    private val confidenceScorer: ConfidenceScorer,
    private val responsePipeline: ResponsePipeline,
    private val intentAnalyzer: IntentAnalyzer,
    private val responseCache: ResponseCache,
    private val kampalaDeviceGeocoder: KampalaDeviceGeocoder,
    private val kampalaPlacesRepository: KampalaPlacesRepository,
    private val kampalaKnowledgeRepository: KampalaKnowledgeRepository,
    private val localChatRepository: LocalChatRepository,
    private val conversationInterpretationProbe: ConversationInterpretationProbe
) {
    private val TAG = "AssistantController"
    private val MAX_STEPS = 3
    private val promptBuilder = JarvisPromptBuilder()

    // Reuse the configured geographic backend and its
    // provider reliability/cooldown handling.
    private val navigationGeoSuggestion by lazy {
        val origin =
            com.example.jarvis.core.network
                .KampalaGeoBackendConfig.HTTPS_ORIGIN

        if (origin.isBlank()) {
            null
        } else {
            KampalaNavigationGeoSuggestion(
                com.example.jarvis.core.network
                    .KampalaGeoDiscoveryGateway
                    .createBackend(origin)
            )
        }
    }

    // CDI.6.0-A6.9-D4C-R1:
    // One instance preserves the provider's cache and cooldown.
    // Construction does not contact Overpass.
    // P3-B1D: Staging provider is constructed lazily.
    // Only an explicit map-journey command may invoke it.
    private val p3b1dWaypointSearch by lazy {
        KampalaP3B1DWaypointSearch.createStaging()
    }
    private val explicitGeoDiscovery by lazy {
        com.example.jarvis.core.network
            .KampalaExplicitGeoDiscovery.createConfiguredBackend()
    }

    // CDI.6.0-A6.4-B3R:
    // Experimental context interpretation is disabled by default.
    // This state is in-memory only and does not authorize actions.
    @Volatile
    private var contextInterpretationEnabled = false

    @Volatile
    private var activeConversationTask: ConversationTaskState.Task? = null

    internal fun setContextInterpretationEnabledForTesting(enabled: Boolean) {
        contextInterpretationEnabled = enabled

        if (!enabled) {
            activeConversationTask = null
        }
    }

    // Active navigation request only; not persistent journey history.
    @Volatile
    private var pendingNavigationDestination: String? = null

    // CDI.6.0-A6.5-E:
    // A user-requested place name with no verified directory result.
    // Never treat this as a verified address or navigation target.
    @Volatile
    private var pendingUnverifiedDestination: String? = null

    // CDI.6.0-A6.6-C:
    // Explicit user-supplied origin for an unverified destination.
    // Not a GPS reading or verified address.
    @Volatile
    private var pendingUnverifiedOrigin: String? = null

    // CDI.6.0-A6.7-A2:
    // Source-backed destination facts remain separate
    // from directory navigation and route calculation.
    @Volatile
    private var sourceBackedJourney: KampalaDestinationJourney? = null

    // CDI.5.53-B1: Temporary, directory-backed branch choices.
    @Volatile
    private var pendingDestinationChoices: List<Pair<String, String>>? = null

    // A single directory listing available for one contextual follow-up.
    @Volatile
    private var lastDirectoryDestination: String? = null

    // CDI.5.64-A2: One pending, user-supplied emergency destination.
    // This is neither an address verification nor journey history.
    @Volatile
    private var pendingEmergencyContext:
        KampalaEmergencyContextPolicy.Context? = null

    // CDI.5.54-A: A cleared conversation must not retain old journeys.
    fun resetNavigationContext() {
        pendingNavigationDestination = null
        pendingUnverifiedDestination = null
        pendingUnverifiedOrigin = null
        sourceBackedJourney = null
        pendingDestinationChoices = null
        lastDirectoryDestination = null
        pendingEmergencyContext = null
        activeConversationTask = null
    }

    private val _events = MutableSharedFlow<AssistantEvent>()
    val events: Flow<AssistantEvent> = _events.asSharedFlow()

    enum class ResponseOrigin {
        CURATED_FACT,
        AI_RESPONSE,
        DIRECTORY_RESULT,
        DIRECTORY_NOTICE,
        KNOWLEDGE_RESULT,
        KNOWLEDGE_NOTICE
    }

    data class AssistantResponse(
        val text: String,
        val voiceOptimizedText: String,
        val confidence: ConfidenceLevel,
        val metadata: ResponseMetadata,
        val origin: ResponseOrigin = ResponseOrigin.AI_RESPONSE
    )

    data class ResponseMetadata(
        val route: ModelRouter.Route,
        val processingTimeMs: Long,
        val toolUsage: Int,
        val memoryUsage: Int
    )

    sealed class AssistantEvent {
        data class Progress(val message: String) : AssistantEvent()
        data class Error(val message: String) : AssistantEvent()
    }

    suspend fun onUserQuery(query: String, onUpdate: (String) -> Unit): AssistantResponse = coroutineScope {
        val startTime = System.currentTimeMillis()

        // Capture existing pending navigation context without
        // changing the navigation subsystem or verifying an address.
        val pendingForContext = pendingNavigationDestination

        if (contextInterpretationEnabled &&
            activeConversationTask == null &&
            !pendingForContext.isNullOrBlank()
        ) {
            activeConversationTask = ConversationTaskState.Task(
                kind = ConversationTaskState.Kind.NAVIGATION,
                goal = "Continue pending navigation",
                fields = mapOf(
                    ConversationTaskState.Field.DESTINATION to
                        ConversationTaskState.Fact(
                            value = pendingForContext,
                            provenance =
                                ConversationTaskState.Provenance.USER_SUPPLIED
                        )
                )
            )
        }

        // CDI.5.64-A2: Handle one contextual emergency follow-up
        // before ordinary navigation or model generation.
        val emergencyContext = pendingEmergencyContext

        if (emergencyContext != null &&
            KampalaEmergencyResponsePolicy.respondOrNull(query) == null
        ) {
            // Do not retain emergency location context across
            // unrelated requests or repeated speculative replies.
            pendingEmergencyContext = null

            val contextualNotice =
                KampalaEmergencyContextPolicy.followUpResponse(
                    emergencyContext, query
                )

            if (contextualNotice != null) {
                onUpdate("Checking emergency conversation context...")

                val contextualResponse = AssistantResponse(
                    text = contextualNotice,
                    voiceOptimizedText = contextualNotice,
                    confidence = EvidenceConfidencePolicy.level(
                        ResponseOrigin.DIRECTORY_NOTICE
                    ),
                    origin = ResponseOrigin.DIRECTORY_NOTICE,
                    metadata = ResponseMetadata(
                        route = ModelRouter.Route.LOCAL,
                        processingTimeMs =
                            System.currentTimeMillis() - startTime,
                        toolUsage = 0,
                        memoryUsage = 0
                    )
                )

                conversationRepository.saveMessage("user", query)
                conversationRepository.saveMessage(
                    "assistant", contextualNotice
                )

                return@coroutineScope contextualResponse
            }
        }

        // CDI.5.63-A3R: Handle acute dog-bite emergencies before
        // contextual navigation, directory routing, or model generation.
        val emergencyNotice =
            KampalaEmergencyResponsePolicy.respondOrNull(query)

        if (emergencyNotice != null) {
            // Prevent an earlier destination or route from being reused.
            resetNavigationContext()

            // Retain only explicitly supplied clinic details.
            pendingEmergencyContext =
                KampalaEmergencyContextPolicy.capture(query)

            onUpdate("Preparing emergency safety guidance...")

            val response = AssistantResponse(
                text = emergencyNotice,
                voiceOptimizedText = emergencyNotice,
                confidence = EvidenceConfidencePolicy.level(
                    ResponseOrigin.DIRECTORY_NOTICE
                ),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage(
                "assistant", emergencyNotice
            )

            return@coroutineScope response
        }

        // CDI.6.0-A6.9-D4C-R1:
        // Only recognized explicit map-search commands may
        // invoke the external geographic discovery provider.
        // Emergency handling above retains priority.
        // Preserve the original conversational wording for the
        // stability normalizer before legacy normalization can
        // absorb a trailing question into the requested area.
        // P3-B2B: Snakebite emergency protection.
        // Existing dog-bite handling remains above this block.
        // This executes before geographic discovery, navigation
        // and unrestricted local-model generation.
        val snakebiteNotice =
            KampalaP3B2BSnakebiteEmergency.noticeOrNull(query)

        if (snakebiteNotice != null) {
            // Never carry an earlier itinerary into an emergency.
            resetNavigationContext()

            onUpdate("Preparing urgent medical safety guidance...")

            val emergencyResponse = AssistantResponse(
                text = snakebiteNotice,
                voiceOptimizedText = snakebiteNotice,
                confidence = EvidenceConfidencePolicy.level(
                    ResponseOrigin.DIRECTORY_NOTICE
                ),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage(
                "assistant",
                snakebiteNotice
            )

            return@coroutineScope emergencyResponse
        }
        // JARVIS-LANDMARK-CLARIFICATION-V1
        // A relative landmark is a clue, never a replacement
        // for the destination or an independently verified fact.
        // Emergency handlers above retain absolute priority.
        val landmarkFollowUp = Regex(
            """^(?:it's|it is|the place is|the destination is|it's located|it is located)\s+(opposite|near|next to|beside|behind|adjacent to|across from)\s+(?:the\s+)?([A-Za-z][A-Za-z0-9 '\-]{1,79})\s*[.!?]*$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(query.trim())

        if (landmarkFollowUp != null) {
            val currentDestination = pendingUnverifiedDestination
            val currentOrigin = pendingUnverifiedOrigin

            if (
                !currentDestination.isNullOrBlank() &&
                !currentOrigin.isNullOrBlank() &&
                pendingDestinationChoices == null
            ) {
                val relation =
                    landmarkFollowUp.groupValues[1].lowercase()
                val landmark =
                    landmarkFollowUp.groupValues[2].trim()

                // Do not change pendingUnverifiedDestination,
                // pendingUnverifiedOrigin or sourceBackedJourney.
                val notice =
                    "Understood. You're describing " +
                        "$currentDestination as $relation " +
                        "$landmark.\n\n" +
                        "Your journey remains:\n" +
                        "From: $currentOrigin\n" +
                        "To: $currentDestination\n\n" +
                        "I haven't independently verified " +
                        "this landmark relationship. " +
                        "Google Maps should be used to " +
                        "confirm the intended destination."

                val response = AssistantResponse(
                    text = notice,
                    voiceOptimizedText = notice,
                    confidence = EvidenceConfidencePolicy.level(
                        ResponseOrigin.DIRECTORY_NOTICE
                    ),
                    origin = ResponseOrigin.DIRECTORY_NOTICE,
                    metadata = ResponseMetadata(
                        route = ModelRouter.Route.LOCAL,
                        processingTimeMs =
                            System.currentTimeMillis() - startTime,
                        toolUsage = 0,
                        memoryUsage = 0
                    )
                )

                conversationRepository.saveMessage(
                    "user", query
                )
                conversationRepository.saveMessage(
                    "assistant", notice
                )

                return@coroutineScope response
            }
        }
        // A complete new journey takes precedence over origin-only context.
        val isFreshCompleteJourney =
            KampalaFreshJourneyPriority.resolveOrNull(query) != null

        // P3-JOURNEY-CONTINUITY:
        // After emergency protections and before geographic routing,
        // accept origin/detail follow-ups for the active journey.
        // JARVIS-NAV-CONTINUITY-V2
        // Retained locations are user supplied, not verified GPS.
        // Never override a newly supplied complete journey.
        val followUpDestination = pendingUnverifiedDestination
        val followUpOrigin = pendingUnverifiedOrigin

        if (
            !isFreshCompleteJourney &&
            pendingDestinationChoices == null &&
            !followUpDestination.isNullOrBlank() &&
            !followUpOrigin.isNullOrBlank()
        ) {
            val message = query.trim()
                .trimEnd('.', '?', '!')
                .trim()

            val changeOrigin = Regex(
                """^(?:actually[, ]*)?(?:i'm|i am|am)\s+(?:currently\s+)?(?:starting\s+)?(?:from|in|at)\s+([A-Za-z][A-Za-z0-9 '\-]{1,79})$""",
                RegexOption.IGNORE_CASE
            ).matchEntire(message)

            val destinationClue = Regex(
                """^(?:it's|it is|the place is|the destination is)\s+(opposite|near|next to|beside|behind|adjacent to)\s+([A-Za-z][A-Za-z0-9 '\-]{1,79})$""",
                RegexOption.IGNORE_CASE
            ).matchEntire(message)

            val openRoute = Regex(
                """^(?:take me there|show me directions|open directions|open maps|navigate there)$""",
                RegexOption.IGNORE_CASE
            ).matches(message)

            val continuityNotice: String? = when {
                changeOrigin != null -> {
                    val newOrigin =
                        changeOrigin.groupValues[1].trim()

                    pendingUnverifiedOrigin = newOrigin

                    sourceBackedJourney =
                        sourceBackedJourney?.withUserOrigin(
                            newOrigin
                        )

                    "Starting point updated to $newOrigin. " +
                        "Your destination remains " +
                        "$followUpDestination. " +
                        "Say 'Take me there' to open directions."
                }

                destinationClue != null -> {
                    val relationship =
                        destinationClue.groupValues[1].lowercase()
                    val landmark =
                        destinationClue.groupValues[2].trim()

                    "You are describing $followUpDestination " +
                        "as $relationship $landmark. " +
                        "I'll keep the journey from " +
                        "$followUpOrigin to $followUpDestination. " +
                        "This location clue is not verified. " +
                        "Confirm the intended place in Google Maps."
                }

                openRoute -> {
                    val encodedOrigin =
                        java.net.URLEncoder.encode(
                            "$followUpOrigin, Kampala, Uganda",
                            "UTF-8"
                        )

                    val encodedDestination =
                        java.net.URLEncoder.encode(
                            "$followUpDestination, Kampala, Uganda",
                            "UTF-8"
                        )

                    val mapsUrl =
                        "https://www.google.com/maps/dir/?api=1" +
                            "&origin=$encodedOrigin" +
                            "&destination=$encodedDestination"

                    "Your journey\n\n" +
                        "From: $followUpOrigin\n" +
                        "To: $followUpDestination\n\n" +
                        "Check both locations and the route " +
                        "in Google Maps.\n\n" +
                        "Open directions: $mapsUrl"
                }

                else -> null
            }

            if (continuityNotice != null) {
                val response = AssistantResponse(
                    text = continuityNotice,
                    voiceOptimizedText = continuityNotice,
                    confidence = EvidenceConfidencePolicy.level(
                        ResponseOrigin.DIRECTORY_NOTICE
                    ),
                    origin = ResponseOrigin.DIRECTORY_NOTICE,
                    metadata = ResponseMetadata(
                        route = ModelRouter.Route.LOCAL,
                        processingTimeMs =
                            System.currentTimeMillis() - startTime,
                        toolUsage = 0,
                        memoryUsage = 0
                    )
                )

                conversationRepository.saveMessage(
                    "user", query
                )
                conversationRepository.saveMessage(
                    "assistant", continuityNotice
                )

                return@coroutineScope response
            }
        }
        // JARVIS-STANDALONE-ORIGIN:
        // Emergency handlers above retain priority.
        // Record a standalone user-supplied origin without
        // geocoding it, accessing GPS or invoking Llama.
        if (activeConversationTask == null && !isFreshCompleteJourney) {
            val suppliedLocation =
                ConversationTaskState.standaloneOriginOrNull(query)

            if (suppliedLocation != null) {
                val fact = ConversationTaskState.Fact(
                    value = suppliedLocation,
                    provenance =
                        ConversationTaskState.Provenance.USER_SUPPLIED
                )

                activeConversationTask = ConversationTaskState.Task(
                    kind = ConversationTaskState.Kind.GENERAL,
                    goal = "User-supplied starting location",
                    fields = mapOf(
                        ConversationTaskState.Field.ORIGIN to fact
                    )
                )

                val notice =
                    "I've recorded your stated location as " +
                        "$suppliedLocation. " +
                        "This is information you provided, " +
                        "not a verified GPS position. " +
                        "You can tell me where you want to go, " +
                        "which places to find, or what errands " +
                        "you need to complete."

                val response = AssistantResponse(
                    text = notice,
                    voiceOptimizedText = notice,
                    confidence = EvidenceConfidencePolicy.level(
                        ResponseOrigin.DIRECTORY_NOTICE
                    ),
                    origin = ResponseOrigin.DIRECTORY_NOTICE,
                    metadata = ResponseMetadata(
                        route = ModelRouter.Route.LOCAL,
                        processingTimeMs =
                            System.currentTimeMillis() - startTime,
                        toolUsage = 0,
                        memoryUsage = 0
                    )
                )

                conversationRepository.saveMessage("user", query)
                conversationRepository.saveMessage(
                    "assistant",
                    notice
                )

                return@coroutineScope response
            }
        }
        val journeyContinuation =
            ConversationTaskState.journeyFollowUpOrNull(
                activeConversationTask,
                query
            )

        if (journeyContinuation != null && !isFreshCompleteJourney) {
            activeConversationTask = journeyContinuation.first
            val notice = journeyContinuation.second

            val response = AssistantResponse(
                text = notice,
                voiceOptimizedText = notice,
                confidence = EvidenceConfidencePolicy.level(
                    ResponseOrigin.DIRECTORY_NOTICE
                ),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage(
                "assistant", notice
            )

            return@coroutineScope response
        }
        val geoRoutingQuery =
            JarvisGeoRoutingRepair.normalize(
                JarvisGeoConversationWording.normalize(
                    JarvisConversationStabilityPolicy
                        .normalizeGeographicRequest(query)
                )
            )


        // P3-B1D: Explicit, staging-only geographic waypoint search.
        // Emergency handling remains above this block.
        // Existing ordinary journey handling remains below it.
        // JARVIS-GEO-CONTROLLER-FOLLOWUP-V1
        // Explicit place-category requests are handled before
        // unrestricted local-model generation.
        // They never inherit GPS or navigation destinations.
        val geoFollowUpText = query.trim()
            .lowercase(java.util.Locale.ROOT)
            .replace(Regex("""\s+"""), " ")
            .trimEnd('.', '?', '!')
            .trim()

        val geoCategories = listOf(
            "hospitals", "clinics", "pharmacies",
            "schools", "universities", "banks",
            "hotels", "guesthouses", "embassies"
        )

        val geoCategoryPattern =
            geoCategories.joinToString("|")

        val explicitCategoryFollowUp = Regex(
            "^(?:find|show|show me)\\s+(?:the\\s+)?" +
                "($geoCategoryPattern)(?:\\s+too)?" +
                "(?:\\s*,?\\s+and\\s+(?:the\\s+)?" +
                "($geoCategoryPattern)(?:\\s+too)?)?$"
        ).matchEntire(geoFollowUpText)

        if (explicitCategoryFollowUp != null) {
            val firstCategory =
                explicitCategoryFollowUp.groupValues[1]
            val secondCategory =
                explicitCategoryFollowUp.groupValues[2]

            val isClearMultiCategoryRequest =
                secondCategory.isNotBlank()

            val isExplicitAdditionalSearch =
                Regex("""\btoo\b""")
                    .containsMatchIn(geoFollowUpText)

            if (
                !isFreshCompleteJourney &&
                (isClearMultiCategoryRequest ||
                    isExplicitAdditionalSearch)
            ) {
                val requestedCategories =
                    listOf(firstCategory, secondCategory)
                        .filter { it.isNotBlank() }
                        .distinct()

                onUpdate("Searching Kampala place categories...")

                val parts = mutableListOf<String>()

                for (category in requestedCategories) {
                    val result =
                        explicitGeoDiscovery.answerOrNull(
                            "find $category across kampala"
                        )

                    parts.add(
                        result
                            ?: "Geographic discovery could not " +
                                "process $category."
                    )
                }

                val geoNotice =
                    parts.joinToString("\n\n---\n\n")

                val geoResponse = AssistantResponse(
                    text = geoNotice,
                    voiceOptimizedText = geoNotice,
                    confidence = EvidenceConfidencePolicy.level(
                        ResponseOrigin.DIRECTORY_NOTICE
                    ),
                    origin = ResponseOrigin.DIRECTORY_NOTICE,
                    metadata = ResponseMetadata(
                        route = ModelRouter.Route.LOCAL,
                        processingTimeMs =
                            System.currentTimeMillis() - startTime,
                        toolUsage = requestedCategories.size,
                        memoryUsage = 0
                    )
                )

                conversationRepository.saveMessage(
                    "user", query
                )
                conversationRepository.saveMessage(
                    "assistant", geoNotice
                )

                return@coroutineScope geoResponse
            }
        }
        val p3b1dNotice =
            p3b1dWaypointSearch.answerOrNull(query)

        if (p3b1dNotice != null) {
            resetNavigationContext()

            val response = AssistantResponse(
                text = p3b1dNotice,
                voiceOptimizedText = p3b1dNotice,
                confidence = EvidenceConfidencePolicy.level(
                    ResponseOrigin.DIRECTORY_NOTICE
                ),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage(
                "assistant",
                p3b1dNotice
            )

            return@coroutineScope response
        }
        // P3-B1F: Recognize clear multi-stage errand requests
        // before category-only food discovery or general AI fallback.
        // No location is geocoded and no route is authorized.
        val p3b1fNotice =
            KampalaP3B1FJourneyGuard.noticeOrNull(query)

        if (p3b1fNotice != null) {
            // Carry an explicitly stated origin into a new
            // itinerary before resetting older navigation state.
            val carriedOrigin =
                activeConversationTask
                    ?.takeIf {
                        it.kind == ConversationTaskState.Kind.GENERAL &&
                            it.goal == "User-supplied starting location"
                    }
                    ?.get(ConversationTaskState.Field.ORIGIN)

            resetNavigationContext()

            // P3-JOURNEY-CONTINUITY:
            // Keep the user-supplied goal across follow-up turns.
            activeConversationTask = ConversationTaskState.Task(
                kind = ConversationTaskState.Kind.NAVIGATION,
                goal = query,
                fields = if (carriedOrigin != null) {
                    mapOf(
                        ConversationTaskState.Field.ORIGIN to carriedOrigin
                    )
                } else {
                    emptyMap()
                }
            )

            val journeyNotice = if (carriedOrigin != null) {
                p3b1fNotice.replace(
                    "Starting point: Not provided",
                    "Starting point: ${carriedOrigin.value} (user supplied)"
                )
            } else {
                p3b1fNotice
            }

            val response = AssistantResponse(
                text = journeyNotice,
                voiceOptimizedText = journeyNotice,
                confidence = EvidenceConfidencePolicy.level(
                    ResponseOrigin.DIRECTORY_NOTICE
                ),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage(
                "assistant", journeyNotice
            )

            return@coroutineScope response
        }
        val explicitGeoCommand =
            explicitGeoDiscovery.answerOrNull(geoRoutingQuery)


        if (explicitGeoCommand != null) {
            onUpdate("Checking experimental Kampala map listings...")

            // A new geographic search is not a continuation
            // of a previous navigation request.
            resetNavigationContext()

            val geoNotice = explicitGeoCommand

            val geoResponse = AssistantResponse(
                text = geoNotice,
                voiceOptimizedText = geoNotice,
                confidence = EvidenceConfidencePolicy.level(
                    ResponseOrigin.DIRECTORY_NOTICE
                ),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage("assistant", geoNotice)

            return@coroutineScope geoResponse
        }

        // P3-B1B:
        // Explicit multi-stop journeys must not enter the existing
        // two-point Google Maps handoff.
        // Emergency and geographic search priority remain unchanged.
        //
        // No geographic bounds have been confirmed for these waypoints.
        // Therefore no provider query, coordinates, route or ETA
        // are produced here.
        val multiStopJourney =
            KampalaJourneyIntentResolver.resolve(query)
                ?.takeIf { it.stops.isNotEmpty() }

        if (multiStopJourney != null) {
            onUpdate("Checking the requested journey stops...")

            // Clear stale two-point destination and follow-up state.
            resetNavigationContext()

            val notice =
                KampalaP3B1MultiStopNotice.format(multiStopJourney)

            val response = AssistantResponse(
                text = notice,
                voiceOptimizedText = notice,
                confidence = EvidenceConfidencePolicy.level(
                    ResponseOrigin.DIRECTORY_NOTICE
                ),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage(
                "assistant",
                notice
            )

            return@coroutineScope response
        }
        // CDI.6.0-A6.9-D4E-R4:
        // A fresh complete journey must supersede any older
        // unresolved destination or navigation follow-up.
        // Emergency and explicit map-search handling stay above.
        val directJourney =
            KampalaFreshJourneyPriority.resolveOrNull(query)

        // CDI.6.0-A6.9-D4E-R2:
        // Resolve a complete journey against source-backed data.
        // A user-supplied name alone is not a verified destination.
        if (directJourney != null) {
            onUpdate("Checking Kampala journey destination...")

            // A complete new journey replaces the previous
            // navigation and destination-selection context.
            resetNavigationContext()

            val destinationName = directJourney.destination
            val requestedOrigin = directJourney.origin

            val directoryResult = kampalaPlacesRepository.search(
                query = destinationName
            )

            val exactPlaces =
                com.example.jarvis.core.network
                    .KampalaDirectoryResolutionAdapter
                    .matchingDirectoryPlaces(
                        requestedName = destinationName,
                        result = directoryResult
                    )

            val journeyNotice: String
            val journeyOrigin: ResponseOrigin

            when {
                exactPlaces.size == 1 -> {
                    val place = exactPlaces.single()

                    // Retain user-supplied journey endpoints.
                    // The directory result remains separate.
                    pendingUnverifiedOrigin = requestedOrigin
                    pendingUnverifiedDestination =
                        destinationName.take(120)

                    // A unique directory listing provides a
                    // source-backed destination, not a verified route.
                    val encodedOrigin = java.net.URLEncoder.encode(
                        "$requestedOrigin, Kampala, Uganda",
                        "UTF-8"
                    )

                    val encodedDestination = java.net.URLEncoder.encode(
                        "${place.name}, ${place.address}",
                        "UTF-8"
                    )

                    val mapsUrl =
                        "https://www.google.com/maps/dir/?api=1" +
                            "&origin=$encodedOrigin" +
                            "&destination=$encodedDestination"

                    journeyOrigin = ResponseOrigin.DIRECTORY_RESULT

                    journeyNotice =
                        "Your journey\n\n" +
                            "From: $requestedOrigin\n" +
                            "To: ${place.name}\n" +
                            "Listed address: ${place.address}\n" +
                            "Area: ${place.area}\n" +
                            "Directory source: ${place.sourceUrl}\n" +
                            "Source checked: ${place.verifiedAt}\n\n" +
                            "Jarvis has not calculated or verified " +
                            "the route. Confirm both locations " +
                            "in Google Maps before travelling.\n\n" +
                            "Open directions: $mapsUrl"
                }

                exactPlaces.size > 1 -> {
                    journeyOrigin = ResponseOrigin.DIRECTORY_NOTICE

                    if (exactPlaces.size <= 10) {
                        pendingDestinationChoices =
                            exactPlaces.map { place ->
                                place.name to place.address
                            }.distinct()

                        journeyNotice =
                            buildString {
                                appendLine(
                                    "Your starting point is " +
                                        "$requestedOrigin."
                                )
                                appendLine(
                                    "Several directory listings match " +
                                        destinationName +
                                        ". Choose the exact destination:"
                                )
                                appendLine()

                                exactPlaces
                                    .map { it.name to it.address }
                                    .distinct()
                                    .forEachIndexed { index, choice ->
                                        appendLine(
                                            "${index + 1}. " +
                                                "${choice.first} — " +
                                                choice.second
                                        )
                                    }

                                appendLine()
                                append(
                                    "No route has been calculated. " +
                                        "Confirm the destination " +
                                        "before opening directions."
                                )
                            }
                    } else {
                        journeyNotice =
                            "Your starting point is $requestedOrigin. " +
                                "More than ten directory listings match " +
                                "$destinationName. Please specify an " +
                                "area, branch, or street address. " +
                                "No route has been calculated."
                    }
                }

                else -> {
                    // A published institutional destination may
                    // be available even when the general directory
                    // did not return an exact listing.
                    val canonicalName =
                        KampalaSourceBackedPlacePolicy
                            .normalizeRequestedName(destinationName)

                    val publishedPlace =
                        KampalaSourceBackedPlacePolicy
                            .resolveOrNull(canonicalName)

                    journeyOrigin = ResponseOrigin.DIRECTORY_NOTICE

                    if (publishedPlace != null) {
                        pendingUnverifiedDestination =
                            canonicalName.take(120)
                        pendingUnverifiedOrigin = requestedOrigin

                        sourceBackedJourney =
                            KampalaDestinationJourney
                                .fromPublishedPlace(
                                    publishedPlace,
                                    requestedOrigin
                                )

                        val publishedNotice =
                            KampalaSourceBackedPlacePolicy
                                .sourceNoticeOrNull(canonicalName)
                                .orEmpty()

                        // CDI.6.0-A6.9-D4F-B:
                        // Reuse the established published-place
                        // Maps policy and existing UI button parser.
                        // The link is a user-controlled handoff,
                        // not a calculated or verified route.
                        val publishedJourneyNotice =
                            "Your starting point is " +
                                "$requestedOrigin.\n\n" +
                                publishedNotice +
                                "\n\nJarvis has not calculated " +
                                "or verified a route. Confirm the " +
                                "published destination and your " +
                                "starting point in Google Maps."

                        journeyNotice =
                            KampalaPublishedJourneyAction.append(
                                publishedJourneyNotice,
                                sourceBackedJourney
                            )
                    } else {
                        // Neither the directory nor the
                        // published-place policy verified a
                        // unique destination. No maps URL.
                        pendingUnverifiedDestination =
                            canonicalName.take(120)
                        pendingUnverifiedOrigin = requestedOrigin

                        // A missing directory record does not prevent
                        // a user-controlled Maps search.
                        // The supplied locations remain unverified.
                        val encodedOrigin =
                            java.net.URLEncoder.encode(
                                "$requestedOrigin, Kampala, Uganda",
                                "UTF-8"
                            )

                        val encodedDestination =
                            java.net.URLEncoder.encode(
                                "$destinationName, Kampala, Uganda",
                                "UTF-8"
                            )

                        val mapsUrl =
                            "https://www.google.com/maps/dir/?api=1" +
                                "&origin=$encodedOrigin" +
                                "&destination=$encodedDestination"

                        // Optional device-geocoder suggestion.
                        // Never silently replace the user's destination.
                        val providerSuggestion =
                            navigationGeoSuggestion
                                ?.suggest(destinationName)
                                ?: kampalaDeviceGeocoder.suggest(
                                    destinationName
                                )

                        val providerSuggestionLine =
                            providerSuggestion?.let {
                                "Provider suggestion: $it\n"
                            }.orEmpty()

                        val coverageNotice =
                            if (
                                directoryResult is
                                    com.example.jarvis.core.network
                                        .KampalaPlacesRepository
                                        .SearchResult.Failure
                            ) {
                                "The Kampala directory is " +
                                    "currently unavailable."
                            } else {
                                "I couldn't confirm an exact " +
                                    "directory listing."
                            }

                        journeyNotice =
                            "Your journey\n\n" +
                                "From: $requestedOrigin\n" +
                                "To: $destinationName\n\n" +
                                "$coverageNotice " + providerSuggestionLine +
                                "These locations were supplied " +
                                "by you and have not been " +
                                "independently verified by Jarvis. " +
                                "You can open Google Maps to " +
                                "search for both locations and " +
                                "check available routes. " +
                                "Confirm the correct destination " +
                                "and starting point before " +
                                "travelling.\n\n" +
                                "Open directions: $mapsUrl"
                    }
                }
            }

            val response = AssistantResponse(
                text = journeyNotice,
                voiceOptimizedText = journeyNotice,
                confidence = EvidenceConfidencePolicy.level(
                    journeyOrigin
                ),
                origin = journeyOrigin,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage(
                "assistant",
                journeyNotice
            )

            return@coroutineScope response
        }
        // CDI.5.61-A2: Resolve a contextual navigation follow-up
        // before the request can fall through to the local model.
        // CDI.6.0-A6.7-A2:
        // Preserve the published destination when the user
        // supplies or corrects the journey origin.
        // This does not authorize an unverified journey card.
        val sourceJourney = sourceBackedJourney
        val correctedSourceOrigin =
            KampalaContextualNavigationPolicy.correctedOriginOrNull(query)

        if (
            sourceJourney != null &&
            pendingUnverifiedDestination != null &&
            correctedSourceOrigin != null
        ) {
            val updatedJourney =
                sourceJourney.withUserOrigin(correctedSourceOrigin)

            sourceBackedJourney = updatedJourney
            pendingUnverifiedOrigin = correctedSourceOrigin

            val notice = updatedJourney.originFollowUpNotice()

            val response = AssistantResponse(
                text = notice,
                voiceOptimizedText = notice,
                confidence = EvidenceConfidencePolicy.level(
                    ResponseOrigin.DIRECTORY_NOTICE
                ),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage("assistant", notice)
            return@coroutineScope response
        }

        // CDI.6.0-A6.6-F:
        // A split correction such as "actually," must not discard
        // the pending place before the next message supplies its origin.
        if (
            KampalaSplitCorrectionPolicy.shouldPreserveUnverifiedDestination(
                pendingUnverifiedDestination,
                query
            )
        ) {
            val notice =
                "I'm following your correction. " +
                    "Your destination is still " +
                    "${pendingUnverifiedDestination}. " +
                    "Please finish telling me what you want to change. " +
                    "I haven't calculated a route."

            val response = AssistantResponse(
                text = notice,
                voiceOptimizedText = notice,
                confidence = EvidenceConfidencePolicy.level(
                    ResponseOrigin.DIRECTORY_NOTICE
                ),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage("assistant", notice)
            return@coroutineScope response
        }

        if (KampalaContextualNavigationPolicy.isFollowUp(query)) {
            val remembered = lastDirectoryDestination
                ?: pendingNavigationDestination
                ?: pendingUnverifiedDestination

            val rememberedIsUnverified =
                pendingUnverifiedDestination != null &&
                    pendingNavigationDestination == null &&
                    lastDirectoryDestination == null

            // CDI.6.0-A6.5-C: Origin comes directly from the user.
            // Never replace the remembered destination with it.
            val correctedOrigin =
                KampalaContextualNavigationPolicy.correctedOriginOrNull(query)

            lastDirectoryDestination = null

            val notice = when {
                pendingDestinationChoices != null ->
                    "Please select a destination from the directory " +
                        "results first, then I can help with directions."

                remembered != null -> {
                    // CDI.6.0-A6.6-C:
                    // Never promote an unverified place name into
                    // the existing confirmed-navigation state.
                    if (rememberedIsUnverified) {
                        pendingNavigationDestination = null
                        if (correctedOrigin != null) {
                            pendingUnverifiedOrigin = correctedOrigin
                        }
                    } else {
                        pendingNavigationDestination = remembered
                        pendingUnverifiedOrigin = null
                    }

                    pendingDestinationChoices = null

                    if (correctedOrigin != null && rememberedIsUnverified) {
                        "You're starting from $correctedOrigin and " +
                            "want to reach $remembered. " +
                            "I haven't verified which exact location " +
                            "you mean, so I can't provide reliable " +
                            "directions yet. Which branch or exact " +
                            "location are you heading to?"
                    } else if (correctedOrigin != null) {
                        "Your starting point is $correctedOrigin, " +
                            "and the destination remains $remembered. " +
                            "I have not calculated or verified a route. " +
                            "Please confirm both locations in Google Maps " +
                            "before starting navigation."
                    } else {
                        "I can help you get to $remembered. " +
                            "Where will you be starting from? " +
                            "Google Maps can calculate directions after " +
                            "you confirm both locations."
                    }
                }

                else ->
                    "Which place would you like directions to? " +
                        "Please tell me the destination first."
            }

            val response = AssistantResponse(
                text = notice,
                voiceOptimizedText = notice,
                confidence = EvidenceConfidencePolicy.level(
                    ResponseOrigin.DIRECTORY_NOTICE
                ),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage("assistant", notice)
            return@coroutineScope response
        }

        // CDI.6.0-A6.6-A:
        // A brief explicit branch clarification may complete the
        // previous unverified destination name. It still requires
        // a fresh directory lookup and source verification.
        val clarifiedDestinationQuery =
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    pendingUnverifiedDestination,
                    query
                )

        // Ordinary requests must not inherit stale location context.
        // The only retained value is the locally prepared lookup query.
        lastDirectoryDestination = null
        pendingUnverifiedDestination = null

        // Preserve the origin only for an explicit branch
        // clarification of the immediately pending destination.
        if (clarifiedDestinationQuery == null) {
            pendingUnverifiedOrigin = null
            sourceBackedJourney = null
        }

        // CDI.5.53-B1: Resolve a numbered directory selection.
        val pendingChoices = pendingDestinationChoices
        if (pendingChoices != null) {
            val selectionText = query.trim()
            val selectionMatch = Regex(
                """^(?:(?:option|number)\s+)?(\d{1,2})[.]?$""",
                RegexOption.IGNORE_CASE
            ).matchEntire(selectionText)

            val cancelSelection = selectionText.equals(
                "cancel", ignoreCase = true
            )

            if (selectionMatch != null || cancelSelection) {
                val selectedIndex = selectionMatch
                    ?.groupValues?.get(1)?.toIntOrNull()
                    ?.minus(1)

                val notice = when {
                    cancelSelection -> {
                        pendingDestinationChoices = null
                        pendingNavigationDestination = null
                        "Destination selection cancelled."
                    }

                    selectedIndex != null &&
                        selectedIndex in pendingChoices.indices -> {
                        val selected = pendingChoices[selectedIndex]
                        pendingDestinationChoices = null
                        pendingNavigationDestination =
                            "${selected.first}, ${selected.second}"

                        "Selected ${selected.first}. " +
                            "Listed address: ${selected.second}.\n\n" +
                            "Where will you be starting from? " +
                            "Google Maps will calculate the route " +
                            "when you open directions."
                    }

                    else ->
                        "Please select a number from 1 to " +
                            "${pendingChoices.size}, or say cancel."
                }

                val response = AssistantResponse(
                    text = notice,
                    voiceOptimizedText = notice,
                    confidence = EvidenceConfidencePolicy.level(
                        ResponseOrigin.DIRECTORY_NOTICE
                    ),
                    origin = ResponseOrigin.DIRECTORY_NOTICE,
                    metadata = ResponseMetadata(
                        route = ModelRouter.Route.LOCAL,
                        processingTimeMs =
                            System.currentTimeMillis() - startTime,
                        toolUsage = 0,
                        memoryUsage = 0
                    )
                )

                conversationRepository.saveMessage("user", query)
                conversationRepository.saveMessage("assistant", notice)
                return@coroutineScope response
            }

            // Unrelated questions cancel the pending selection.
            pendingDestinationChoices = null
        }

        // CDI.5.53-C1: Handle explicitly supplied two-point
        // journeys before general on-device AI processing.
        // CDI.5.58-A2: Resolve navigation intent once.
        val navigationIntent =
            KampalaNavigationIntentCoordinator.classify(
                query,
                awaitingOrigin = pendingNavigationDestination != null
            )

        // CDI.5.58-A2: Use the classified origin follow-up.
        val startingPoint =
            (navigationIntent as?
                KampalaNavigationIntentCoordinator.Intent.OriginReply)
                ?.origin

        val pendingDestination = pendingNavigationDestination

        // CDI.6.0-A6.6-C:
        // A pending destination clarification is never a new origin.
        // The existing directions-card path may only run when
        // an actual verified/directory destination is pending.
        if (
            KampalaNavigationTurnSafetyPolicy.canHandleOriginReply(
                pendingDestination,
                clarifiedDestinationQuery
            ) &&
            !startingPoint.isNullOrBlank() &&
            startingPoint.length <= 80
        ) {
            pendingNavigationDestination = null

            onUpdate("Checking navigation context...")

            val mapOrigin = java.net.URLEncoder.encode(
                "$startingPoint, Kampala, Uganda",
                "UTF-8"
            )
            val mapDestination = java.net.URLEncoder.encode(
                "$pendingDestination, Kampala, Uganda",
                "UTF-8"
            )

            val mapsUrl =
                "https://www.google.com/maps/dir/?api=1" +
                    "&origin=$mapOrigin" +
                    "&destination=$mapDestination"

            val navigationNotice =
                "Journey: $startingPoint to $pendingDestination, Kampala. " +
                    "I haven't calculated or verified a route. " +
                    "You can open Google Maps to check the available " +
                    "directions and confirm both locations.\n\n" +
                    "Open directions: $mapsUrl"

            val navigationResponse = AssistantResponse(
                text = navigationNotice,
                voiceOptimizedText = navigationNotice,
                confidence = EvidenceConfidencePolicy.level(
                    ResponseOrigin.DIRECTORY_NOTICE
                ),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage(
                "assistant", navigationNotice
            )

            return@coroutineScope navigationResponse
        }

        // A different question ends the pending navigation exchange.
        if (pendingDestination != null) {
            pendingNavigationDestination = null
        }

        // CDI.5.57-A: Check user-supplied destination-only requests
        // against exact directory listings before asking for origin.
        val destinationOnly =
            (navigationIntent as?
                KampalaNavigationIntentCoordinator.Intent.DestinationOnly)
                ?.destination

        if (destinationOnly != null) {
            onUpdate("Checking Kampala directory for destination...")

            val directoryResult = kampalaPlacesRepository.search(
                query = destinationOnly
            )

            // CDI.5.58-D: Keep directory lookup confidence explicit.
            val destinationConfidence = when (directoryResult) {
                is KampalaPlacesRepository.SearchResult.Success ->
                    KampalaDestinationConfidencePolicy.resolve(
                        requestedName = destinationOnly,
                        entries = directoryResult.places.map {
                            it.name to it.address
                        },
                        directoryAvailable = true
                    )

                is KampalaPlacesRepository.SearchResult.Failure ->
                    KampalaDestinationConfidencePolicy.resolve(
                        requestedName = destinationOnly,
                        entries = emptyList(),
                        directoryAvailable = false
                    )
            }

            val exactMatches = when (directoryResult) {
                is KampalaPlacesRepository.SearchResult.Success -> {
                    val entries = directoryResult.places

                    val indices = when (destinationConfidence) {
                        is KampalaDestinationConfidencePolicy
                            .Resolution.ExactListing ->
                            listOf(destinationConfidence.index)

                        is KampalaDestinationConfidencePolicy
                            .Resolution.Ambiguous ->
                            destinationConfidence.indices

                        is KampalaDestinationConfidencePolicy
                            .Resolution.TooManyMatches ->
                            KampalaDestinationMatchPolicy.exactIndices(
                                destinationOnly,
                                entries.map { it.name to it.address }
                            )

                        else -> emptyList()
                    }

                    indices.map { entries[it] }
                }

                is KampalaPlacesRepository.SearchResult.Failure ->
                    emptyList()
            }

            val notice: String
            val responseOrigin: ResponseOrigin

            when {
                exactMatches.size == 1 -> {
                    val place = exactMatches.single()

                    pendingDestinationChoices = null
                    pendingNavigationDestination =
                        "${place.name}, ${place.address}"

                    responseOrigin = ResponseOrigin.DIRECTORY_RESULT

                    notice =
                        "I found an exact listing for ${place.name} " +
                            "in the Kampala directory.\n" +
                            "Listed address: ${place.address}\n" +
                            "Directory source: ${place.sourceUrl}\n" +
                            "Source checked: ${place.verifiedAt}\n\n" +
                            "Where will you be starting from? " +
                            "Google Maps will calculate the route " +
                            "after you choose to open directions."
                }

                exactMatches.size in 2..10 -> {
                    pendingNavigationDestination = null

                    pendingDestinationChoices =
                        exactMatches.map { it.name to it.address }

                    responseOrigin = ResponseOrigin.DIRECTORY_NOTICE

                    notice = buildString {
                        appendLine(
                            "I found several exact directory listings " +
                                "for $destinationOnly:"
                        )
                        appendLine()

                        exactMatches.forEachIndexed { index, place ->
                            appendLine(
                                "${index + 1}. ${place.name} — " +
                                    place.address
                            )
                        }

                        appendLine()
                        append(
                            "Reply with the destination number, " +
                                "or say cancel."
                        )
                    }
                }

                exactMatches.size > 10 -> {
                    pendingNavigationDestination = null
                    pendingDestinationChoices = null
                    responseOrigin = ResponseOrigin.DIRECTORY_NOTICE

                    notice =
                        "There are more than ten distinct exact " +
                            "directory listings for $destinationOnly. " +
                            "Please specify the area or address."
                }

                else -> {
                    pendingDestinationChoices = null
                    pendingNavigationDestination = destinationOnly
                    responseOrigin = ResponseOrigin.DIRECTORY_NOTICE

                    val verificationNotice =
                        if (
                            destinationConfidence is
                                KampalaDestinationConfidencePolicy
                                    .Resolution.DirectoryUnavailable
                        ) {
                            "The directory could not verify the " +
                                "destination right now."
                        } else {
                            "I found no usable exact directory listing " +
                                "for this destination."
                        }

                    notice =
                        "You want directions to $destinationOnly. " +
                            "$verificationNotice " +
                            "Where will you be starting from? " +
                            "This is your supplied destination, not a " +
                            "verified address. Please confirm both " +
                            "locations in Google Maps."
                }
            }

            val response = AssistantResponse(
                text = notice,
                voiceOptimizedText = notice,
                confidence = EvidenceConfidencePolicy.level(
                    responseOrigin
                ),
                origin = responseOrigin,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage("assistant", notice)

            return@coroutineScope response
        }

        // CDI.5.52-B3: Keep explicit Kampala directions requests
        // away from ungrounded local-model route generation.
        val directionsRequest = Regex(
            """^(?:how\s+(?:do|can)\s+i\s+get\s+to|directions\s+to)\s+(.+?)\s*\??$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(query.trim())

        val requestedDestination = directionsRequest
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.trimEnd('?')
            ?.trim()

        val localAreas = setOf(
            "muyenga", "ntinda", "kololo", "nakasero",
            "kisementi", "old kampala", "bugolobi",
            "makindye"
        )

        val normalizedDestination = requestedDestination
            ?.lowercase()
            ?.replace(Regex("""\s+"""), " ")

        val isKampalaDirections =
            normalizedDestination != null &&
                (
                    normalizedDestination in localAreas ||
                        normalizedDestination.endsWith(" in kampala")
                )

        if (isKampalaDirections) {
            pendingNavigationDestination = requestedDestination
            onUpdate("Checking Kampala directions coverage...")

            val destination = requestedDestination!!
            val isMuyenga = normalizedDestination == "muyenga"

            val directionsNotice = if (isMuyenga) {
                "Muyenga is an area of Kampala. " +
                    "Where will you be starting from? " +
                    "I don't have a verified route for this journey yet, " +
                    "so I won't invent directions, distances or travel times."
            } else {
                "Where will you be starting from for your journey to " +
                    "$destination? I don't have a verified route for " +
                    "this journey yet, so I won't invent directions, " +
                    "distances or travel times."
            }

            val directionsResponse = AssistantResponse(
                text = directionsNotice,
                voiceOptimizedText = directionsNotice,
                confidence = EvidenceConfidencePolicy.level(
                    ResponseOrigin.DIRECTORY_NOTICE
                ),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage(
                "assistant", directionsNotice
            )

            return@coroutineScope directionsResponse
        }

        // CDI.5.53-A1: Navigate only to an exact, directory-backed
        // named destination. Existing neighbourhood requests above
        // retain their established behavior.
        val namedNavigationMatch = Regex(
            """^(?:take\s+me\s+to|navigate\s+to|directions\s+to|how\s+(?:do|can)\s+i\s+get\s+to)\s+(.+?)\s*[?.!]?$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(
            (
                if (clarifiedDestinationQuery != null) {
                    "how do i get to $clarifiedDestinationQuery"
                } else {
                    query
                }
            ).trim()
        )

        if (namedNavigationMatch != null) {
            // CDI.6.0-A6.6-C:
            // A new named navigation request cannot inherit the
            // previous unverified journey's starting point.
            if (clarifiedDestinationQuery == null) {
                pendingUnverifiedOrigin = null
            }

            val destinationQuery = namedNavigationMatch.groupValues[1]
                .trim()
                .trimEnd('?', '.', '!')
                .trim()
                .replace(Regex("""^the\s+""", RegexOption.IGNORE_CASE), "")
                .replace(
                    Regex("""\s+in\s+kampala$""", RegexOption.IGNORE_CASE),
                    ""
                )
                .trim()

            if (destinationQuery.length in 3..90) {
                onUpdate("Checking Kampala directory for destination...")

                val destinationResult = kampalaPlacesRepository.search(
                    query = destinationQuery
                )

                // CDI.6.0-A6.9-C2:
                // Resolve named directory destinations through the
                // shared, source-aware place resolution contract.
                // Never promote unavailable or ambiguous data
                // into an automatically selected destination.
                val matches =
                    com.example.jarvis.core.network
                        .KampalaDirectoryResolutionAdapter
                        .matchingDirectoryPlaces(
                            requestedName = destinationQuery,
                            result = destinationResult
                        )

                val destinationNotice = when {
                    matches.size == 1 -> {
                        pendingDestinationChoices = null
                        val place = matches.single()
                        pendingNavigationDestination =
                            "${place.name}, ${place.address}"

                        "I found ${place.name} in the Kampala directory. " +
                            "Listed address: ${place.address}. " +
                            "Directory source: ${place.sourceUrl}\n" +
                            "Source checked: ${place.verifiedAt}\n\n" +
                            "Where will you be starting from? " +
                            "Google Maps will calculate the route " +
                            "after you choose to open directions."
                    }

                    matches.size > 1 -> {
                        pendingNavigationDestination = null

                        if (matches.size > 10) {
                            pendingDestinationChoices = null
                            "The directory returned more than ten " +
                                "matching destinations for $destinationQuery. " +
                                "Please provide a more specific location."
                        } else {
                            val choices = matches.map { place ->
                                place.name to place.address
                            }.distinct()

                            if (choices.size == 1) {
                                pendingDestinationChoices = null
                                "The directory returned repeated listings " +
                                    "for $destinationQuery. Please provide " +
                                    "a more specific destination."
                            } else {
                                pendingDestinationChoices = choices

                                buildString {
                                    appendLine(
                                        "I found several directory listings " +
                                            "for $destinationQuery:"
                                    )
                                    appendLine()

                                    choices.forEachIndexed { index, choice ->
                                        appendLine(
                                            "${index + 1}. ${choice.first} — " +
                                                choice.second
                                        )
                                    }

                                    appendLine()
                                    append(
                                        "Reply with the number of the " +
                                            "destination you want, or say cancel."
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        pendingDestinationChoices = null
                        pendingNavigationDestination = null

                        // Retain the user's words, not an inferred address.
                        // CDI.6.0-A6.6-D2:
                        // Normalize the place name without upgrading
                        // it into a confirmed navigation destination.
                        val canonicalDestination =
                            KampalaSourceBackedPlacePolicy
                                .normalizeRequestedName(destinationQuery)

                        pendingUnverifiedDestination =
                            canonicalDestination.take(120)

                        val officialPlace =
                            KampalaSourceBackedPlacePolicy.resolveOrNull(
                                canonicalDestination
                            )

                        val retainedOrigin = pendingUnverifiedOrigin

                        // Keep published place provenance with the
                        // conversational destination context.
                        sourceBackedJourney =
                            officialPlace?.let { place ->
                                KampalaDestinationJourney
                                    .fromPublishedPlace(
                                        place,
                                        retainedOrigin
                                    )
                            }

                        if (officialPlace != null) {
                            val publishedNotice =
                                KampalaSourceBackedPlacePolicy
                                    .sourceNoticeOrNull(
                                        canonicalDestination
                                    ).orEmpty()

                            if (clarifiedDestinationQuery != null) {
                                // CDI.6.0-A6.7-B:
                                // This is a clarification of the same
                                // published place, not a new route.
                                KampalaSourceClarificationResponsePolicy
                                    .respond(
                                        officialPlace,
                                        retainedOrigin
                                    )
                            } else if (retainedOrigin != null) {
                                "Your starting point remains " +
                                    "$retainedOrigin. $publishedNotice"
                            } else {
                                publishedNotice
                            }
                        } else if (retainedOrigin != null &&
                            clarifiedDestinationQuery != null
                        ) {
                            "Your starting point remains $retainedOrigin. " +
                                "You want to reach $destinationQuery. " +
                                "I searched the directory again but " +
                                "couldn't verify an exact destination. " +
                                "I won't create a directions link from " +
                                "an unverified location. Please provide " +
                                "a street address, nearby landmark, or " +
                                "another specific destination detail."
                        } else {
                            "You want to get to $destinationQuery. " +
                                "I couldn't verify an exact directory " +
                                "listing, so this is still an unverified " +
                                "place name rather than a confirmed address. " +
                                "Which branch or exact location do you mean?"
                        }
                    }
                }

                val destinationOrigin =
                    if (matches.size == 1) {
                        ResponseOrigin.DIRECTORY_RESULT
                    } else {
                        ResponseOrigin.DIRECTORY_NOTICE
                    }

                val destinationResponse = AssistantResponse(
                    text = destinationNotice,
                    voiceOptimizedText = destinationNotice,
                    confidence = EvidenceConfidencePolicy.level(
                        destinationOrigin
                    ),
                    origin = destinationOrigin,
                    metadata = ResponseMetadata(
                        route = ModelRouter.Route.LOCAL,
                        processingTimeMs =
                            System.currentTimeMillis() - startTime,
                        toolUsage = 0,
                        memoryUsage = 0
                    )
                )

                conversationRepository.saveMessage("user", query)
                conversationRepository.saveMessage(
                    "assistant", destinationNotice
                )

                return@coroutineScope destinationResponse
            }
        }
        // CDI.5.53-B3: Search a named business on a requested road
        // through the existing directory before generic service notices.
        val businessRoadRequest = Regex(
            """^(?:find(?:\s+me)?|locate|show\s+me|where\s+is)\s+(?:a\s+|an\s+|the\s+)?(.+?)\s+(?:on|along)\s+([A-Za-z][A-Za-z0-9 '\-]*\s+(?:road|street|avenue|lane|drive))\s*[?.!]*$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(query.trim())

        if (businessRoadRequest != null) {
            val businessName = businessRoadRequest.groupValues[1]
                .trim()
                .replace(Regex("""\s+"""), " ")

            val requestedRoad = businessRoadRequest.groupValues[2]
                .trim()
                .replace(Regex("""\s+"""), " ")

            if (
                businessName.length in 3..80 &&
                requestedRoad.length in 5..80
            ) {
                onUpdate("Searching Kampala directory for $businessName...")

                val directoryResult = kampalaPlacesRepository.search(
                    query = businessName
                )

                val matchingPlaces = when (directoryResult) {
                    is KampalaPlacesRepository.SearchResult.Success ->
                        directoryResult.places.filter { place ->
                            place.name.trim().equals(
                                businessName,
                                ignoreCase = true
                            ) &&
                                place.address.contains(
                                    requestedRoad,
                                    ignoreCase = true
                                )
                        }

                    is KampalaPlacesRepository.SearchResult.Failure ->
                        emptyList()
                }

                val answer = when (directoryResult) {
                    is KampalaPlacesRepository.SearchResult.Failure ->
                        "I couldn't access the Kampala directory to " +
                            "check $businessName on $requestedRoad. " +
                            "Please try again when the service is available."

                    is KampalaPlacesRepository.SearchResult.Success ->
                        if (matchingPlaces.isEmpty()) {
                            "I couldn't confirm a listing for " +
                                "$businessName on $requestedRoad in my " +
                                "current Kampala directory. This doesn't " +
                                "mean the business isn't there. " +
                                "I won't invent an address or opening hours."
                        } else {
                            buildString {
                                appendLine("Kampala Places Directory")
                                appendLine()

                                matchingPlaces.forEachIndexed { index, place ->
                                    if (index > 0) appendLine()

                                    appendLine(place.name)
                                    appendLine("Address: ${place.address}")
                                    appendLine("Area: ${place.area}")
                                    appendLine(
                                        "Source: ${place.sourceUrl}"
                                    )
                                    appendLine(
                                        "Source checked: ${place.verifiedAt}"
                                    )
                                }

                                appendLine()
                                append(
                                    "Directory information only. " +
                                        "Confirm current opening hours and " +
                                        "availability with the business."
                                )
                            }
                        }
                }

                val origin = if (matchingPlaces.isNotEmpty()) {
                    ResponseOrigin.DIRECTORY_RESULT
                } else {
                    ResponseOrigin.DIRECTORY_NOTICE
                }

                val response = AssistantResponse(
                    text = answer,
                    voiceOptimizedText = answer,
                    confidence = EvidenceConfidencePolicy.level(origin),
                    origin = origin,
                    metadata = ResponseMetadata(
                        route = ModelRouter.Route.LOCAL,
                        processingTimeMs =
                            System.currentTimeMillis() - startTime,
                        toolUsage = 0,
                        memoryUsage = 0
                    )
                )

                conversationRepository.saveMessage("user", query)
                conversationRepository.saveMessage("assistant", answer)

                return@coroutineScope response
            }
        }
        val discoveryDecision = KampalaDiscoveryIntelligence.decide(query)

        val placesRequest =
            (discoveryDecision as? KampalaDiscoveryIntelligence.Decision.Search)
                ?.request

        if (placesRequest != null) {
            onUpdate("Searching Kampala places...")

            val searchResult = kampalaPlacesRepository.search(
                query = placesRequest.query,
                area = placesRequest.area,
                category = placesRequest.category
            )

            // Remember only one nonempty directory listing.
            // Never infer a chosen place from multiple results.
            lastDirectoryDestination = when (searchResult) {
                is KampalaPlacesRepository.SearchResult.Success ->
                    KampalaContextualNavigationPolicy.uniqueDestination(
                        searchResult.places.map {
                            it.name to it.address
                        }
                    )

                is KampalaPlacesRepository.SearchResult.Failure ->
                    null
            }

            val placesAnswer = KampalaPlacesResultPolicy.format(placesRequest, searchResult)

            val placesConfidence = confidenceScorer.calculateConfidence(
                query, placesAnswer, 0, 0
            )

            val placesOrigin = when (searchResult) {
                is KampalaPlacesRepository.SearchResult.Success ->
                    if (searchResult.places.isNotEmpty()) {
                        ResponseOrigin.DIRECTORY_RESULT
                    } else {
                        ResponseOrigin.DIRECTORY_NOTICE
                    }

                is KampalaPlacesRepository.SearchResult.Failure ->
                    ResponseOrigin.DIRECTORY_NOTICE
            }

            val placesResponse = AssistantResponse(
                text = placesAnswer,
                voiceOptimizedText = placesAnswer,
                confidence = EvidenceConfidencePolicy.level(placesOrigin),
                origin = placesOrigin,
                metadata = ResponseMetadata(
                    route = modelRouter.route(query),
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage("assistant", placesAnswer)

            return@coroutineScope placesResponse
        }

        // M1F-I3C.120K.2: intercept unresolved place-discovery
        // requests before general AI processing.
        // The original Places router contract remains unchanged.
        val locationNotice = KampalaLocationFallback.notice(query) ?: KampalaNamedDestinationRoute.notice(query)

        if (locationNotice != null) {
            onUpdate("Checking Kampala location spelling...")

            val locationConfidence = confidenceScorer.calculateConfidence(
                query, locationNotice, 0, 0
            )

            val locationResponse = AssistantResponse(
                text = locationNotice,
                voiceOptimizedText = locationNotice,
                confidence = EvidenceConfidencePolicy.level(ResponseOrigin.DIRECTORY_NOTICE),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage("assistant", locationNotice)

            return@coroutineScope locationResponse
        }

        // Source-grounded Kampala history. Never fall back to model-generated
        // history when the knowledge service is unavailable.
        if (KampalaKnowledgeQueryRoute.resolve(query) ==
            KampalaKnowledgeQueryRoute.Topic.HISTORY
        ) {
            onUpdate("Checking Kampala history sources...")

            val knowledgeResult = kampalaKnowledgeRepository.getHistory()

            val historyAnswer = when (knowledgeResult) {
                is KampalaKnowledgeRepository.Result.Success -> {
                    val entry = knowledgeResult.entry

                    buildString {
                        appendLine(entry.title)
                        appendLine()
                        appendLine(entry.answer)
                        appendLine()
                        appendLine("Sources:")

                        entry.sources.forEach { source ->
                            appendLine("${source.publisher}: ${source.url}")
                        }

                        appendLine()
                        append("Knowledge reviewed: ${entry.reviewedAt}.")
                    }
                }

                KampalaKnowledgeRepository.Result.NotFound ->
                    "I don't currently have a verified Kampala history entry. " +
                    "Please try again when the Kampala Knowledge service has been updated."

                is KampalaKnowledgeRepository.Result.Failure ->
                    "I couldn't verify Kampala's history from the Knowledge service " +
                    "right now. Please try again later. " +
                    "I won't substitute an unverified historical account."
            }

            val historyOrigin = when (knowledgeResult) {
                is KampalaKnowledgeRepository.Result.Success ->
                    ResponseOrigin.KNOWLEDGE_RESULT

                else ->
                    ResponseOrigin.KNOWLEDGE_NOTICE
            }

            val historyConfidence = confidenceScorer.calculateConfidence(
                query, historyAnswer, 0, 0
            )

            val historyResponse = AssistantResponse(
                text = historyAnswer,
                voiceOptimizedText = historyAnswer,
                confidence = EvidenceConfidencePolicy.level(historyOrigin),
                origin = historyOrigin,
                metadata = ResponseMetadata(
                    route = modelRouter.route(query),
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage("assistant", historyAnswer)

            return@coroutineScope historyResponse
        }

        // Kampala Life discovery only. No bookings or payments.
        val lifeClassification = KampalaLifeClassifier.classify(query)
        val lifeNotice = lifeClassification?.let {
            KampalaLifeResponsePolicy.notice(it)
        }

        if (lifeNotice != null) {
            onUpdate("Checking Kampala Life service availability...")

            val lifeConfidence = confidenceScorer.calculateConfidence(
                query, lifeNotice, 0, 0
            )

            val lifeResponse = AssistantResponse(
                text = lifeNotice,
                voiceOptimizedText = lifeNotice,
                confidence = EvidenceConfidencePolicy.level(ResponseOrigin.DIRECTORY_NOTICE),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage("assistant", lifeNotice)

            return@coroutineScope lifeResponse
        }

        // Specific local-discovery questions must not fall through to
        // an ungrounded model when directory coverage is incomplete.
        val factualNotice =
            KampalaEvidenceAndRelevancePolicy.notice(query)
                ?: KampalaFactualReliabilityBoundary.notice(query)

        val compositionalNotice =
            CompositionalDiscoveryResponsePolicy.notice(query)

        val discoveryNotice = compositionalNotice
            ?: factualNotice
            ?: (discoveryDecision as? KampalaDiscoveryIntelligence.Decision.Notice)?.message

        if (discoveryNotice != null) {
            onUpdate("Checking Kampala discovery coverage...")

            val discoveryConfidence = confidenceScorer.calculateConfidence(
                query, discoveryNotice, 0, 0
            )

            val discoveryResponse = AssistantResponse(
                text = discoveryNotice,
                voiceOptimizedText = discoveryNotice,
                confidence = EvidenceConfidencePolicy.level(ResponseOrigin.DIRECTORY_NOTICE),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage("assistant", discoveryNotice)

            return@coroutineScope discoveryResponse
        }
        // Protect unsupported Kampala location and landmark questions
        // before entering the ungrounded language-model response path.
        val groundingNotice = KampalaGroundingSafetyPolicy.notice(query)

        if (groundingNotice != null) {
            onUpdate("Checking verified Kampala coverage...")

            val groundingConfidence = confidenceScorer.calculateConfidence(
                query, groundingNotice, 0, 0
            )

            val groundingResponse = AssistantResponse(
                text = groundingNotice,
                voiceOptimizedText = groundingNotice,
                confidence = EvidenceConfidencePolicy.level(ResponseOrigin.DIRECTORY_NOTICE),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage("assistant", groundingNotice)

            return@coroutineScope groundingResponse
        }
        // Allow the existing deterministic journey handlers above
        // to run first. Only unresolved navigation follow-ups reach
        // this fail-safe before unrestricted model generation.
        val unresolvedJourneyNotice =
            JarvisConversationStabilityPolicy
                .unresolvedJourneyFollowUpNotice(query)


        if (unresolvedJourneyNotice != null) {
            val response = AssistantResponse(
                text = unresolvedJourneyNotice,
                voiceOptimizedText = unresolvedJourneyNotice,
                confidence = EvidenceConfidencePolicy.level(
                    ResponseOrigin.DIRECTORY_NOTICE
                ),
                origin = ResponseOrigin.DIRECTORY_NOTICE,
                metadata = ResponseMetadata(
                    route = ModelRouter.Route.LOCAL,
                    processingTimeMs =
                        System.currentTimeMillis() - startTime,
                    toolUsage = 0,
                    memoryUsage = 0
                )
            )

            conversationRepository.saveMessage("user", query)
            conversationRepository.saveMessage(
                "assistant", unresolvedJourneyNotice
            )

            return@coroutineScope response
        }
        // CDI.6.0-A6.4-B3R:
        // Run only after existing protected deterministic routes.
        // Model proposals never initiate navigation or tool actions.
        if (contextInterpretationEnabled) {
            val taskSnapshot = activeConversationTask

            val eligibility = ConversationInterpretationEligibility.evaluate(
                task = taskSnapshot,
                message = query,
                explicitlyEnabled = true,
                protectedRoute = false
            )

            if (eligibility ==
                ConversationInterpretationEligibility.Decision.ELIGIBLE
            ) {
                val observation = withTimeoutOrNull(15000L) {
                    conversationInterpretationProbe.evaluate(
                        task = taskSnapshot,
                        message = query,
                        explicitlyEnabled = true
                    )
                }

                if (observation?.status ==
                    ConversationInterpretationProbe.Status.VALID_PROPOSAL
                ) {
                    val proposal = observation.proposal

                    val existingDestination = taskSnapshot?.get(
                        ConversationTaskState.Field.DESTINATION
                    )

                    // Do not allow a model classification to replace an
                    // existing destination, even when it says CORRECTION.
                    if (proposal != null &&
                        !(proposal.field ==
                            ConversationTaskState.Field.DESTINATION &&
                            existingDestination != null)
                    ) {
                        val guarded = ConversationProposalApplication.apply(
                            current = taskSnapshot,
                            proposal = proposal
                        )

                        if (guarded.outcome ==
                            ConversationProposalApplication.Outcome.APPLIED
                        ) {
                            activeConversationTask = guarded.task
                        }

                        Log.i(
                            "JarvisContextIntegration",
                            "outcome=${guarded.outcome.name}"
                        )
                    } else {
                        Log.i(
                            "JarvisContextIntegration",
                            "outcome=REJECTED_DESTINATION_CHANGE"
                        )
                    }
                } else {
                    Log.i(
                        "JarvisContextIntegration",
                        "outcome=NO_VALID_PROPOSAL"
                    )
                }
            }
        }

        onUpdate("Thinking...")
        // CDI.6.0-A3: Use the same local Room history that powers
        // the visible chat. Do not depend on Firestore authentication
        // or connectivity for conversational continuity.
        val historyDeferred = async { localChatRepository.load() }
        val memoriesDeferred = async { retrievalManager.getRelevantMemories(3) }
        val contextDataDeferred = async { contextManager.getCurrentContext() }

        val history = historyDeferred.await()
        val memories = memoriesDeferred.await()
        val contextData = contextDataDeferred.await()

        val chatHistory = LocalConversationContextPolicy.prepare(
            history.map { it.role to it.text },
            currentQuery = query
        )
        val retrievedContext = memories.joinToString("\n") { "- ${it.text}" }
        val promptMode = PromptModePolicy.select(query)
        val availableTools =
            if (promptMode == PromptModePolicy.Mode.CONVERSATION) {
                ""
            } else {
                skillRegistry.getSkillsPrompt()
            }

        val intentResult = intentAnalyzer.analyze(query)
        val proposedRoute = if (intentResult.strategy == "CLOUD_REQUIRED") {
            ModelRouter.Route.CLOUD
        } else {
            modelRouter.route(query)
        }
        var currentRoute = CloudInferenceAvailabilityPolicy.effectiveRoute(proposedRoute)

        var currentStep = 0
        val executionHistory = mutableListOf<String>()
        var finalAnswerText = ""
        var toolUsageCount = 0

        while (currentStep < MAX_STEPS) {
            // Supported app requests use validated tool routing.
            // This does not bypass SkillRegistry authorization.
            val dialTarget =
                if (currentStep == 0) DialCommandRoute.resolve(query)
                else null

            if (dialTarget != null) {
                onUpdate("Requesting approval...")

                val parameters = JSONObject().apply {
                    put("action", "call")
                    put("target", dialTarget)
                }

                val result = skillRegistry.invokeSkill(
                    "PhoneAutomation",
                    parameters
                )

                toolUsageCount++

                finalAnswerText =
                    if (result.startsWith("Phone automation unavailable until explicitly approved")) {
                        "I've requested your approval in the app. No phone action has been performed."
                    } else {
                        result
                    }

                break
            }

            val appProposal =
                if (currentStep == 0) AppLaunchCommandRoute.resolve(query)
                else null

            if (appProposal != null) {
                onUpdate("Requesting approval...")

                val parameters = JSONObject().apply {
                    put("action", "open_app")
                    put("target", appProposal.packageName)
                }

                val result = skillRegistry.invokeSkill(
                    "PhoneAutomation",
                    parameters
                )

                toolUsageCount++

                finalAnswerText =
                    if (result.startsWith("Phone automation unavailable until explicitly approved")) {
                        "I've requested your approval in the app. No phone action has been performed."
                    } else {
                        result
                    }

                break
            }
            val prompt = promptBuilder.build(
                query = query,
                chatHistory = chatHistory,
                retrievedContext = retrievedContext,
                deviceContext = contextData.toJsonObject().toString(),
                tools = availableTools,
                stepHistory = executionHistory,
                route = currentRoute
            )

            val response = modelRouter.generateResponse(prompt, currentRoute)

            if (!ModelOutputGuard.isUsable(response)) {
                Log.w(TAG, "Malformed model response rejected")
                finalAnswerText = "I couldn't produce a reliable response. Please try again."
                break
            }

            val actionPlan = when (
                val parsed = ModelActionParser.parse(
                    response = response,
                    allowToolCalls =
                        promptMode == PromptModePolicy.Mode.TOOL_AWARE
                )
            ) {
                is ModelActionParser.Result.FinalAnswer ->
                    ActionPlan(
                        ActionType.FINAL_ANSWER,
                        content = parsed.text
                    )

                is ModelActionParser.Result.ToolCall ->
                    ActionPlan(
                        ActionType.TOOL_CALL,
                        toolName = parsed.name,
                        toolArgs = parsed.params
                    )

                is ModelActionParser.Result.Rejected -> {
                    Log.w(TAG, "Model action rejected: ${parsed.reason}")
                    ActionPlan(
                        ActionType.FINAL_ANSWER,
                        content = "I couldn't safely process that response. Please try again."
                    )
                }
            }
            Log.i("JarvisToolTrace", "Parsed tool call=${actionPlan.type == ActionType.TOOL_CALL}; phone tool=${actionPlan.toolName.equals("PhoneAutomation", ignoreCase = true)}")

            if (actionPlan.type == ActionType.FINAL_ANSWER) {
                finalAnswerText = actionPlan.content
                break
            }

            if (ModelToolExecutionPolicy.isAllowed(
                    promptMode,
                    actionPlan.type == ActionType.TOOL_CALL,
                    actionPlan.toolName
                )) {
                toolUsageCount++
                onUpdate("Using ${actionPlan.toolName}...")
                val toolResult = try {
                    skillRegistry.invokeSkill(actionPlan.toolName, actionPlan.toolArgs)
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
                Log.i("JarvisToolTrace", "Phone approval queued=${toolResult.startsWith("Phone automation unavailable until explicitly approved")}")
                if (toolResult.startsWith("Phone automation unavailable until explicitly approved")) {
                    finalAnswerText = "I've requested your approval in the app. No phone action has been performed."
                    break
                }
                executionHistory.add("Tool ${actionPlan.toolName} result: $toolResult")
            } else {
                finalAnswerText = if (actionPlan.type == ActionType.TOOL_CALL) {
                    "I couldn't safely process that tool request. No action was performed."
                } else {
                    response
                }
                break
            }
            currentStep++
        }

        if (finalAnswerText.isBlank()) {
            finalAnswerText = "I encountered a processing issue. Could you try again?"
        }

        // Apply narrowly scoped answer-quality checks only to
        // ordinary model conversation with no executed tools.
        if (promptMode == PromptModePolicy.Mode.CONVERSATION &&
            toolUsageCount == 0) {
            finalAnswerText = ConversationalAnswerQualityPolicy.review(
                query, finalAnswerText
            )
        }
        // Reject known Kampala geographical contradictions before
        // generating voice output, displaying, or saving the answer.
        if (KampalaFactualAccuracyPolicy.shouldReject(query, finalAnswerText)) {
            Log.w(TAG, "Kampala factual contradiction detected")
            finalAnswerText = KampalaFactualAccuracyPolicy.SAFE_RESPONSE
        }

        if (toolUsageCount == 0) {
            finalAnswerText = KampalaEvidenceAndRelevancePolicy.review(
                query, finalAnswerText
            )
        }

        onUpdate("Polishing...")
        if (toolUsageCount == 0 &&
            !ModelOutputIntegrityPolicy.isSafe(query, finalAnswerText)) {
            Log.w(TAG, "CDI model output integrity rejection")
            finalAnswerText =
                "I couldn't produce a reliable response to that request. " +
                    "Please try rephrasing it."
        }

        if (!ModelOutputGuard.isUsable(finalAnswerText)) {
            finalAnswerText = "I couldn't produce a reliable response. Please try again."
        }

        if (toolUsageCount == 0) {
            finalAnswerText = KampalaTrustedFactPolicy.review(
                query, finalAnswerText
            )
        }

        // P2-B7C: final model-output guard before voice and persistence.
        finalAnswerText = JarvisConversationStabilityPolicy.reviewModelAnswer(
            query, finalAnswerText, toolUsageCount
        )
        finalAnswerText = JarvisP2B7COutputPolicy.review(
            query, finalAnswerText
        )
        val finalVoiceText = try {
            val candidate = responsePipeline.execute(
                query, finalAnswerText, retrievedContext, currentRoute
            )
            if (
                ModelOutputGuard.isUsable(candidate) &&
                !JarvisP2B7COutputPolicy.containsRawToolSyntax(candidate)
            ) candidate else finalAnswerText
        } catch (e: Exception) {
            Log.w(TAG, "Voice generation failed; using safe answer", e)
            finalAnswerText
        }
        val confidence = confidenceScorer.calculateConfidence(query, finalAnswerText, memories.size, toolUsageCount)

        val visibleAnswerText = KotlinAnswerWarningPolicy.decorate(
            query = query,
            answer = finalAnswerText
        )
        val curatedFact = if (toolUsageCount == 0) {
            KampalaTrustedFactPolicy.answer(query)
        } else {
            null
        }

        val finalOrigin = if (curatedFact != null &&
            finalAnswerText == curatedFact) {
            ResponseOrigin.CURATED_FACT
        } else {
            ResponseOrigin.AI_RESPONSE
        }

        val finalResponse = AssistantResponse(
            text = visibleAnswerText,
            voiceOptimizedText = finalVoiceText,
            confidence = EvidenceConfidencePolicy.level(finalOrigin),
            origin = finalOrigin,
            metadata = ResponseMetadata(
                route = currentRoute,
                processingTimeMs = System.currentTimeMillis() - startTime,
                toolUsage = toolUsageCount,
                memoryUsage = memories.size
            )
        )

        conversationRepository.saveMessage("user", query)
        conversationRepository.saveMessage("assistant", visibleAnswerText)

        return@coroutineScope finalResponse
    }

    enum class ActionType { TOOL_CALL, FINAL_ANSWER }
    data class ActionPlan(val type: ActionType, val toolName: String = "", val toolArgs: JSONObject = JSONObject(), val content: String = "")
    suspend fun provideFeedback(q: String, r: String, c: String) = feedbackManager.saveCorrection(q, r, c)
}
