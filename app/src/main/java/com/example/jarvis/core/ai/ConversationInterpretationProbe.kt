package com.example.jarvis.core.ai

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * CDI.6.0-A6.2-B
 *
 * Explicit Gemma-only evaluation endpoint.
 *
 * Does not use ModelRouter, so it cannot silently fall back
 * to another model. Not invoked by normal chat processing.
 *
 * Outputs are observations only. No state updates, navigation,
 * tool execution, bookings, or message persistence.
 */
internal class ConversationInterpretationProbe @Inject constructor(
    private val gemmaClient: GemmaCompanionClient
) {

    enum class Status {
        DISABLED_OR_INVALID_INPUT,
        MODEL_UNAVAILABLE,
        INVALID_MODEL_OUTPUT,
        VALID_PROPOSAL,
        INTERNAL_ERROR
    }

    data class Result(
        val status: Status,
        val proposal: ConversationInterpretationContract.Proposal? = null
    )

    suspend fun evaluate(
        task: ConversationTaskState.Task?,
        message: String,
        explicitlyEnabled: Boolean = false
    ): Result {
        val prompt = ConversationInterpretationProbePolicy.prepare(
            task = task,
            message = message,
            explicitlyEnabled = explicitlyEnabled
        ) ?: return Result(Status.DISABLED_OR_INVALID_INPUT)

        val start = SystemClock.elapsedRealtime()

        return try {
            val answer = gemmaClient.generate(prompt)

            val proposal =
                ConversationInterpretationProbePolicy.validate(answer)

            val status = when {
                answer.isNullOrBlank() -> Status.MODEL_UNAVAILABLE
                proposal == null -> Status.INVALID_MODEL_OUTPUT
                else -> Status.VALID_PROPOSAL
            }

            Log.i(
                "JarvisContextProbe",
                "status=${status.name} " +
                    "latencyMs=${SystemClock.elapsedRealtime() - start}"
            )

            Result(status, proposal)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(
                "JarvisContextProbe",
                "status=INTERNAL_ERROR " +
                    "type=${error.javaClass.simpleName}"
            )
            Result(Status.INTERNAL_ERROR)
        }
    }
}