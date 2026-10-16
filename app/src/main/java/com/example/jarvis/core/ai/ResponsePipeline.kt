package com.example.jarvis.core.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Produces text for speech without another language-model generation pass.
 * The authoritative assistant answer is not rewritten by this component.
 */
@Singleton
class ResponsePipeline @Inject constructor() {
    suspend fun execute(
        query: String,
        draft: String,
        context: String,
        route: ModelRouter.Route
    ): String = VoiceOutputPolicy.format(draft)
}