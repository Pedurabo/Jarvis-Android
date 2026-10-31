package com.example.jarvis.core.ai

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * CDI.6.0-A6.2-C-F
 * Debug-only, explicitly triggered Gemma evaluation.
 * No user chat reads, task mutations, tools, or navigation.
 */
@AndroidEntryPoint
class ConversationEvaluationActivity : ComponentActivity() {

    @Inject
    internal lateinit var probe: ConversationInterpretationProbe

    private data class EvaluationCase(
        val id: Int,
        val task: ConversationTaskState.Task,
        val message: String,
        val relation: ConversationInterpretationContract.Relation,
        val field: ConversationTaskState.Field?,
        val value: String?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val diagnosticLabel = TextView(this).apply {
            text = "Jarvis Gemma evaluation running. Check Android logs."
            textSize = 18f
            setPadding(32, 64, 32, 32)
        }
        setContentView(diagnosticLabel)

        val id = intent.getStringExtra("case")?.toIntOrNull()
        val selected = cases().firstOrNull { it.id == id }

        if (selected == null) {
            Log.w("JarvisContextEval", "invalid_case")
            finish()
            return
        }

        lifecycleScope.launch {
            val start = SystemClock.elapsedRealtime()

            try {
                Log.i("JarvisContextEval", "case=${selected.id} started")

                val result = probe.evaluate(
                    task = selected.task,
                    message = selected.message,
                    explicitlyEnabled = true
                )

                val proposal = result.proposal

                val valueMatches =
                    if (selected.value == null) {
                        proposal?.value == null
                    } else {
                        proposal?.value?.equals(
                            selected.value,
                            ignoreCase = true
                        ) == true
                    }

                val passed =
                    result.status ==
                        ConversationInterpretationProbe.Status.VALID_PROPOSAL &&
                    proposal != null &&
                    proposal.relation == selected.relation &&
                    proposal.field == selected.field &&
                    valueMatches

                val safeValue = proposal?.value
                    ?.take(80)
                    ?.replace(Regex("[^A-Za-z0-9 -]"), "_")
                    ?: "NONE"

                Log.i(
                    "JarvisContextEval",
                    "case=${selected.id} " +
                        "status=${result.status.name} " +
                        "relation=${proposal?.relation?.name ?: "NONE"} " +
                        "field=${proposal?.field?.name ?: "NONE"} " +
                        "value=$safeValue " +
                        "match=$passed " +
                        "latencyMs=${SystemClock.elapsedRealtime() - start}"
                )
            } catch (cancelled: CancellationException) {
                Log.w("JarvisContextEval", "case=${selected.id} cancelled")
                throw cancelled
            } catch (error: Exception) {
                Log.e(
                    "JarvisContextEval",
                    "case=${selected.id} errorType=${error.javaClass.simpleName}"
                )
            } finally {
                finish()
            }
        }
    }

    private fun task(
        kind: ConversationTaskState.Kind,
        goal: String,
        vararg fields: Pair<ConversationTaskState.Field, String>,
        urgent: Boolean = false
    ): ConversationTaskState.Task {
        val changes = mutableListOf<ConversationTaskState.Change>(
            ConversationTaskState.Change.Start(
                kind = kind,
                goal = goal,
                requiresUrgentCare = urgent
            )
        )

        fields.forEach { (field, value) ->
            changes.add(
                ConversationTaskState.Change.SetField(
                    field = field,
                    value = value,
                    provenance = ConversationTaskState.Provenance.USER_SUPPLIED
                )
            )
        }

        return requireNotNull(
            ConversationTaskState.apply(changes = changes)
        )
    }

    private fun cases(): List<EvaluationCase> = listOf(
        EvaluationCase(
            1,
            task(
                ConversationTaskState.Kind.MEDICAL_ASSISTANCE,
                "Reach medical care",
                ConversationTaskState.Field.DESTINATION to "Case Clinic",
                ConversationTaskState.Field.ORIGIN to "Luwum Street",
                urgent = true
            ),
            "think it's in Nakasero",
            ConversationInterpretationContract.Relation.CONTINUE,
            ConversationTaskState.Field.SUGGESTED_AREA,
            "Nakasero"
        ),
        EvaluationCase(
            2,
            task(
                ConversationTaskState.Kind.NAVIGATION,
                "Reach Watoto Church",
                ConversationTaskState.Field.DESTINATION to "Watoto Church",
                ConversationTaskState.Field.ORIGIN to "Nakawa"
            ),
            "Actually, I'm starting from Ntinda",
            ConversationInterpretationContract.Relation.CORRECTION,
            ConversationTaskState.Field.ORIGIN,
            "Ntinda"
        ),
        EvaluationCase(
            3,
            task(
                ConversationTaskState.Kind.NAVIGATION,
                "Reach Watoto Church",
                ConversationTaskState.Field.DESTINATION to "Watoto Church"
            ),
            "Show me hotels instead",
            ConversationInterpretationContract.Relation.NEW_TASK,
            null,
            null
        ),
        EvaluationCase(
            4,
            task(
                ConversationTaskState.Kind.MARKET_COMPARISON,
                "Compare tomato prices",
                ConversationTaskState.Field.ITEM to "Tomatoes"
            ),
            "I want the cheapest tomatoes",
            ConversationInterpretationContract.Relation.CONTINUE,
            ConversationTaskState.Field.PREFERENCE,
            "cheapest tomatoes"
        ),
        EvaluationCase(
            5,
            task(
                ConversationTaskState.Kind.PLACE_DISCOVERY,
                "Find a restaurant",
                ConversationTaskState.Field.SUBJECT to "Restaurants"
            ),
            "maybe",
            ConversationInterpretationContract.Relation.UNSURE,
            null,
            null
        )
    )
}