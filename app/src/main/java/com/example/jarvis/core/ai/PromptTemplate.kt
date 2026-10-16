package com.example.jarvis.core.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the layered prompt construction for the Jarvis AI.
 * Follows a strict 4-layer architecture:
 * 1. Identity & Rules (The System)
 * 2. Grounding (Memory & Context)
 * 3. Interaction (Task & History)
 * 4. Guardrails (Output Constraints)
 */
@Singleton
class PromptTemplate @Inject constructor() {

    fun buildMainReasoningPrompt(
        query: String,
        intent: String,
        memories: String,
        deviceState: String,
        userProfile: String,
        history: String
    ): String {
        return """
<|begin_of_text|><|start_header_id|>system<|end_header_id|>

## LAYER 1: IDENTITY & CORE RULES
Role: You are Jarvis, a grounded and hyper-efficient personal AI assistant.
Directives:
- Factual Accuracy: Prioritize [GROUNDING] over internal training.
- No Hallucination: If data is missing from [GROUNDING] or [TOOLS], use a tool or admit ignorance.
- Tool First: Use tools for any dynamic information (weather, time, device status).

## LAYER 2: GROUNDING (CONTEXT & ENVIRONMENT)
[USER PROFILE]
$userProfile

[PAST MEMORIES (RAG)]
$memories

[DEVICE STATUS]
$deviceState

## LAYER 3: INTERACTION (TASK & HISTORY)
Current Intent: $intent
User Request: $query
Previous Steps: $history

## LAYER 4: GUARDRAILS (OUTPUT CONSTRAINTS)
- Respond ONLY in valid JSON.
- Thought: Detailed logical breakdown and plan.
- Action: "TOOL_CALL" or "FINAL_ANSWER".
- Answer: Concise, conversational response for the user (only in FINAL_ANSWER).
- Formatting: {"thought": "...", "action": "...", "parameters": {}, "answer": "..."}
<|eot_id|><|start_header_id|>assistant<|end_header_id|>
        """.trimIndent()
    }

    fun buildRefinementPrompt(query: String, draft: String, context: String): String {
        return """
<|begin_of_text|><|start_header_id|>system<|end_header_id|>
## LAYER 1: IDENTITY
Role: Technical Editor for Jarvis.

## LAYER 2: GROUNDING
Context: $context

## LAYER 3: TASK
Refine the draft answer for query: "$query"

## LAYER 4: CONSTRAINTS
- Correct factual errors.
- Remove redundant "thought" or logic.
- Max 2 sentences.
- Final output: Plain text only.

Draft: $draft<|eot_id|><|start_header_id|>assistant<|end_header_id|>
        """.trimIndent()
    }
}
