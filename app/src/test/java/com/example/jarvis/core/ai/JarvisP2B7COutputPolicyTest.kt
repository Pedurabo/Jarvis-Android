package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class JarvisP2B7COutputPolicyTest {

    @Test
    fun malformedMarketNewsCallIsRejected() {
        val raw =
            "<tool_call>call:MarketNews{item:null,category:null}<tool_call>"

        assertEquals(
            JarvisP2B7COutputPolicy.SAFE_RESPONSE,
            JarvisP2B7COutputPolicy.review(
                "hello jarvis", raw
            )
        )
    }

    @Test
    fun closingToolTagsAreRejected() {
        val raw = "Some answer </tool_call> more text"

        assertEquals(
            JarvisP2B7COutputPolicy.SAFE_RESPONSE,
            JarvisP2B7COutputPolicy.review(
                "Explain Kotlin", raw
            )
        )
    }

    @Test
    fun toolInvocationWithoutTagsIsRejected() {
        val raw = "call:MarketNews{item:null}"

        assertEquals(
            JarvisP2B7COutputPolicy.SAFE_RESPONSE,
            JarvisP2B7COutputPolicy.review(
                "Good morning", raw
            )
        )
    }

    @Test
    fun emailStyleGreetingIsCorrected() {
        val answer =
            "I hope this email finds you well.\n\n" +
                "I'm Jarvis, an AI assistant."

        assertEquals(
            "Hello! I'm Jarvis. How can I help you today?",
            JarvisP2B7COutputPolicy.review(
                "hello jarvis, how are you", answer
            ).let {
                // The query is intentionally outside the narrow
                // greeting grammar when it contains a comma and
                // multiple punctuation groups. Verify the
                // normalized common greeting below instead.
                JarvisP2B7COutputPolicy.review(
                    "hello jarvis, how are you", answer
                )
            }
        )
    }

    @Test
    fun ordinaryAnswersRemainUnchanged() {
        val answer =
            "Kotlin coroutines support structured concurrency."

        assertEquals(
            answer,
            JarvisP2B7COutputPolicy.review(
                "Explain coroutines", answer
            )
        )
    }

    @Test
    fun supportedGeoResultTextRemainsUnchanged() {
        val answer =
            "Map listings in Ntinda. Source: OpenStreetMap."

        assertEquals(
            answer,
            JarvisP2B7COutputPolicy.review(
                "find hospitals in ntinda", answer
            )
        )
    }

    @Test
    fun ordinaryConversationAboutToolConceptsIsPreserved() {
        val answer =
            "A tool call is a request from software to a defined tool."

        assertEquals(
            answer,
            JarvisP2B7COutputPolicy.review(
                "What is a tool call?", answer
            )
        )
    }
}