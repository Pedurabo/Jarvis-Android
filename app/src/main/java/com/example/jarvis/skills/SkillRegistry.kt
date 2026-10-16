package com.example.jarvis.skills

import android.util.Log
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Base Interface for all Jarvis Skills.
 * Supports both keyword-based matching and LLM-driven tool calling.
 */
interface JarvisSkill {
    val name: String
    val description: String
    val keywords: List<String>

    /**
     * Define parameters for the LLM to understand how to call this skill.
     * Returns a JSON schema of expected parameters.
     */
    fun getParametersSchema(): JSONObject = JSONObject()

    /**
     * Executes the skill logic.
     * @param command The raw user command or JSON parameters from LLM.
     */
    suspend fun execute(command: String): String
}

@Singleton
class SkillRegistry @Inject constructor() {
    private val skills = mutableMapOf<String, JarvisSkill>()

    fun registerSkill(skill: JarvisSkill) {
        skills[skill.name.lowercase()] = skill
        Log.d("SkillRegistry", "Registered skill: ${skill.name}")
    }

    /**
     * Finds a skill based on manual keyword matching (fallback).
     */
    fun findSkillByKeywords(command: String): JarvisSkill? {
        return skills.values.find { skill ->
            skill.keywords.any { keyword -> command.contains(keyword, ignoreCase = true) }
        }
    }

    // Every model/tool-agent call must pass through invokeSkill.
    // Phone actions are never executed by that method.
    private val approvedReadOnlySkills = setOf("weather", "marketnews")
    private val approvals = PendingToolApproval()
    private val approvalLock = Any()
    private val _pendingToolAction = MutableStateFlow<ToolApprovalRequest?>(null)
    val pendingToolAction: StateFlow<ToolApprovalRequest?> = _pendingToolAction.asStateFlow()

    suspend fun invokeSkill(action: String, parameters: JSONObject): String {
        val name = action.trim().lowercase()
        val skill = skills[name] ?: return "Tool unavailable or not registered."

        if (name == "phoneautomation") {
            val reason = validatePhoneAction(parameters)
            if (reason != null) return "Phone automation unavailable: $reason"
            val request = ToolApprovalRequest(skill.name, parameters.toString())
            val queued = synchronized(approvalLock) {
                if (!approvals.propose(request)) false
                else {
                    _pendingToolAction.value = request
                    true
                }
            }
            return if (queued) {
                "Phone automation unavailable until explicitly approved in the app."
            } else {
                "Phone automation unavailable: another action is awaiting approval."
            }
        }

        if (name !in approvedReadOnlySkills) {
            return "This action is unavailable until its permissions and safety checks are reviewed."
        }
        return skill.execute(parameters.toString())
    }

    /** UI-only entry point. Consumes the stored request before any execution. */
    suspend fun approvePendingToolAction(): String {
        val authorized = synchronized(approvalLock) {
            val request = approvals.pending ?: return@synchronized null
            val consumed = approvals.approve(request)
            _pendingToolAction.value = null
            consumed
        } ?: return "No pending action to approve."

        if (authorized.action.lowercase() != "phoneautomation") return "Tool unavailable."
        val arguments = try { JSONObject(authorized.parametersJson) }
            catch (e: org.json.JSONException) { return "Invalid approved parameters." }
        val reason = validatePhoneAction(arguments)
        if (reason != null) return "Approved action rejected: $reason"
        val skill = skills["phoneautomation"] ?: return "Tool unavailable."
        return skill.execute(authorized.parametersJson)
    }

    fun rejectPendingToolAction(): Boolean = synchronized(approvalLock) {
        val rejected = approvals.reject()
        _pendingToolAction.value = null
        rejected
    }

    /** Only existing, implemented phone actions are eligible for approval. */
    private fun validatePhoneAction(args: JSONObject): String? {
        if (args.length() != 2 || !args.has("action") || !args.has("target")) {
            return "Expected exactly action and target"
        }
        val operation = args.optString("action")
        val target = args.optString("target")
        return when (operation) {
            "call" -> if (Regex("^[+]?[0-9]{3,15}$").matches(target)) null else "Invalid phone number"
            "open_app" -> when {
                !Regex("^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+").matches(target) -> "Invalid app package"
                !ApprovedAppPackages.isAllowed(target) -> "Unsupported app package"
                else -> null
            }
            else -> "Unsupported phone action"
        }
    }

    fun getAllSkills(): List<JarvisSkill> = skills.values.toList()

    fun getAllSkillNames(): List<String> = skills.keys.toList()

    fun getSkillsPrompt(): String {
        return skills.values.filter { it.name.lowercase() in approvedReadOnlySkills || it.name.lowercase() == "phoneautomation" }.joinToString("\n") {
            "- ${it.name}: ${it.description}. Params: ${it.getParametersSchema()}"
        }
    }
}
