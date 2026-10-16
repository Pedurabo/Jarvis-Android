package com.example.jarvis.core.ai.agents

import com.example.jarvis.skills.SkillRegistry
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolAgent @Inject constructor(
    private val skillRegistry: SkillRegistry
) : Agent {
    override val name: String = "ToolAgent"

    /**
     * ToolAgent executes a specific tool based on structured input.
     * Expected input: JSON string with "action" and "parameters".
     */
    override suspend fun execute(input: String, context: String): AgentResult {
        return try {
            val json = JSONObject(input)
            val toolName = json.getString("action")
            val args = json.optJSONObject("parameters") ?: JSONObject()

            val result = skillRegistry.invokeSkill(toolName, args)
            AgentResult(result, mapOf("tool" to toolName))
        } catch (e: Exception) {
            AgentResult("Error executing tool: ${e.message}")
        }
    }
}
