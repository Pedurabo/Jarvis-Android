package com.example.jarvis.core.ai

import android.util.Log
import com.example.jarvis.core.network.KampalaPlacesQueryRoute
import com.example.jarvis.core.network.KampalaPlacesRepository
import com.example.jarvis.core.network.KampalaPlacesResponseFormatter
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
    private val kampalaPlacesRepository: KampalaPlacesRepository
) {
    private val TAG = "AssistantController"
    private val MAX_STEPS = 3
    private val promptBuilder = JarvisPromptBuilder()

    private val _events = MutableSharedFlow<AssistantEvent>()
    val events: Flow<AssistantEvent> = _events.asSharedFlow()

    enum class ResponseOrigin {
        AI_RESPONSE,
        DIRECTORY_RESULT,
        DIRECTORY_NOTICE
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

        val placesRequest = KampalaPlacesQueryRoute.resolve(query)

        if (placesRequest != null) {
            onUpdate("Searching Kampala places...")

            val searchResult = kampalaPlacesRepository.search(
                query = placesRequest.query,
                area = placesRequest.area,
                category = placesRequest.category
            )

            val placesAnswer = KampalaPlacesResponseFormatter.format(searchResult)

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
                confidence = placesConfidence.level,
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
        var currentRoute = if (intentResult.strategy == "CLOUD_REQUIRED") ModelRouter.Route.CLOUD else modelRouter.route(query)

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

        onUpdate("Polishing...")
        if (!ModelOutputGuard.isUsable(finalAnswerText)) {
            finalAnswerText = "I couldn't produce a reliable response. Please try again."
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
        val finalResponse = AssistantResponse(
            text = visibleAnswerText,
            voiceOptimizedText = finalVoiceText,
            confidence = confidence.level,
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
