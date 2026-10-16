package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelActionParserTest {

    @Test
    fun ordinaryConversationIsFinalAnswer() {
        val result = ModelActionParser.parse(
            "Kotlin supports val and var.",
            allowToolCalls = false
        )

        assertTrue(result is ModelActionParser.Result.FinalAnswer)
    }

    @Test
    fun kotlinCodeIsNotToolCall() {
        val result = ModelActionParser.parse(
            "fun add(a: Int, b: Int): Int { return a + b }",
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.FinalAnswer)
    }

    @Test
    fun fencedJsonExampleIsNotToolCall() {
        val response = """
            ```json
            {"tool":"PhoneAutomation","params":{"action":"open_app"}}
            ```
        """.trimIndent()

        val result = ModelActionParser.parse(
            response,
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.FinalAnswer)
    }

    @Test
    fun embeddedJsonIsNotToolCall() {
        val response =
            """Example: {"tool":"PhoneAutomation","params":{}}"""

        val result = ModelActionParser.parse(
            response,
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.FinalAnswer)
    }

    @Test
    fun validStandaloneToolJsonIsCandidate() {
        val response =
            """{"tool":"PhoneAutomation","params":{"action":"open_app","target":"com.android.settings"}}"""

        val result = ModelActionParser.parse(
            response,
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.ToolCall)

        val call = result as ModelActionParser.Result.ToolCall

        assertEquals("PhoneAutomation", call.name)
        assertEquals("open_app", call.params.optString("action"))
    }

    @Test
    fun conversationModeRejectsToolJson() {
        val response =
            """{"tool":"PhoneAutomation","params":{"action":"open_app"}}"""

        val result = ModelActionParser.parse(
            response,
            allowToolCalls = false
        )

        assertTrue(result is ModelActionParser.Result.Rejected)
    }

    @Test
    fun missingParametersAreRejected() {
        val result = ModelActionParser.parse(
            """{"tool":"PhoneAutomation"}""",
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.Rejected)
    }

    @Test
    fun emptyToolNameIsRejected() {
        val result = ModelActionParser.parse(
            """{"tool":"","params":{}}""",
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.Rejected)
    }

    @Test
    fun validFinalAnswerEnvelopeIsUnwrapped() {
        val result = ModelActionParser.parse(
            """{"tool":"FINAL_ANSWER","params":{"content":"Hello Jarvis"}}""",
            allowToolCalls = false
        )

        assertTrue(result is ModelActionParser.Result.FinalAnswer)
        assertEquals(
            "Hello Jarvis",
            (result as ModelActionParser.Result.FinalAnswer).text
        )
    }

    @Test
    fun emptyFinalAnswerEnvelopeIsRejected() {
        val result = ModelActionParser.parse(
            """{"tool":"FINAL_ANSWER","params":{"content":""}}""",
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.Rejected)
    }

    @Test
    fun blankResponseIsRejected() {
        val result = ModelActionParser.parse(
            "   ",
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.Rejected)
    }

    @Test
    fun malformedJsonNeverBecomesToolCall() {
        val result = ModelActionParser.parse(
            """{"tool":"PhoneAutomation","params":""",
            allowToolCalls = true
        )

        assertTrue(result !is ModelActionParser.Result.ToolCall)
    }

    @Test
    fun unknownToolRemainsOnlyCandidate() {
        val result = ModelActionParser.parse(
            """{"tool":"UnknownSkill","params":{}}""",
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.ToolCall)
        assertEquals(
            "UnknownSkill",
            (result as ModelActionParser.Result.ToolCall).name
        )
    }

    @Test
    fun rejectsTrailingJsonObject() {
        val response =
            """{"tool":"PhoneAutomation","params":{}} {"extra":1}"""

        val result = ModelActionParser.parse(
            response,
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.Rejected)
    }

    @Test
    fun rejectsTrailingJsonAfterFinalAnswer() {
        val response =
            """{"tool":"FINAL_ANSWER","params":{"content":"Done"}} {"extra":1}"""

        val result = ModelActionParser.parse(
            response,
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.Rejected)
    }

    @Test
    fun acceptsSingleJsonObjectWithWhitespace() {
        val response =
            """  {"tool":"PhoneAutomation","params":{"action":"open_app"}}  """

        val result = ModelActionParser.parse(
            response,
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.ToolCall)
    }

    @Test
    fun malformedToolJsonIsRejected() {
        val result = ModelActionParser.parse(
            """{"tool":"PhoneAutomation","params":{"action":"open_app",}}""",
            allowToolCalls = true
        )

        assertTrue(result !is ModelActionParser.Result.ToolCall)
    }

    @Test
    fun duplicateToolKeysNeverAuthorizeExecution() {
        val result = ModelActionParser.parse(
            """{"tool":"FINAL_ANSWER","tool":"PhoneAutomation","params":{}}""",
            allowToolCalls = true
        )

        assertTrue(result !is ModelActionParser.Result.ToolCall)
    }

    @Test
    fun conversationModeCannotProduceToolCandidate() {
        val result = ModelActionParser.parse(
            """{"tool":"PhoneAutomation","params":{"action":"call","target":"+12025550100"}}""",
            allowToolCalls = false
        )

        assertTrue(result !is ModelActionParser.Result.ToolCall)
    }

    @Test
    fun jsonExampleWithSurroundingExplanationIsNotToolCall() {
        val result = ModelActionParser.parse(
            """
            Here is an example:
            {"tool":"PhoneAutomation","params":{"action":"open_app"}}
            Do not execute it.
            """.trimIndent(),
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.FinalAnswer)
    }

    @Test
    fun rejectsDuplicateNestedParameterKeys() {
        val result = ModelActionParser.parse(
            """{"tool":"PhoneAutomation","params":{"action":"call","action":"open_app"}}""",
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.Rejected)
    }

    @Test
    fun rejectsTrailingCommaInToolParameters() {
        val result = ModelActionParser.parse(
            """{"tool":"PhoneAutomation","params":{"action":"open_app",}}""",
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.Rejected)
    }

    @Test
    fun rejectsMissingCommaBetweenFields() {
        val result = ModelActionParser.parse(
            """{"tool":"PhoneAutomation" "params":{}}""",
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.Rejected)
    }

    @Test
    fun rejectsSingleQuotedToolJson() {
        val result = ModelActionParser.parse(
            """{'tool':'PhoneAutomation','params':{}}""",
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.Rejected)
    }

    @Test
    fun rejectsCommentsInsideToolJson() {
        val result = ModelActionParser.parse(
            """{"tool":"PhoneAutomation",/* comment */"params":{}}""",
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.Rejected)
    }

    @Test
    fun rejectsNestedDuplicateKeys() {
        val result = ModelActionParser.parse(
            """{"tool":"PhoneAutomation","params":{"nested":{"x":1,"x":2}}}""",
            allowToolCalls = true
        )

        assertTrue(result is ModelActionParser.Result.Rejected)
    }
}