package com.example.jarvis.core.ai

import android.util.Log
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantController @Inject constructor(
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
    private val kampalaPlacesRepository: KampalaPlacesRepository,
    private val kampalaKnowledgeRepository: KampalaKnowledgeRepository
) {
    private val TAG = "AssistantController"
    private val MAX_STEPS = 3
    private val promptBuilder = JarvisPromptBuilder()

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
        onUpdate("Thinking...")
        val historyDeferred = async { conversationRepository.getLastMessages(5) }
        val memoriesDeferred = async { retrievalManager.getRelevantMemories(3) }
        val contextDataDeferred = async { contextManager.getCurrentContext() }

        val history = historyDeferred.await()
        val memories = memoriesDeferred.await()
        val contextData = contextDataDeferred.await()

        val chatHistory = PromptHistoryPolicy.format(history.map { it.role to it.text })
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

        val finalVoiceText = try {
            val candidate = responsePipeline.execute(
                query, finalAnswerText, retrievedContext, currentRoute
            )
            if (ModelOutputGuard.isUsable(candidate)) candidate else finalAnswerText
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
