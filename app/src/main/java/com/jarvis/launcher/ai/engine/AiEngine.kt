package com.jarvis.launcher.ai.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore extension
private val Context.dataStore by preferencesDataStore(name = "jarvis_settings")

/**
 * AI Engine - Core intelligence module for JARVIS
 *
 * Handles:
 * - Natural language command processing
 * - Intent recognition
 * - App launching commands
 * - Device control commands
 * - Local vs Cloud AI mode switching
 * - Multiple cloud providers (OpenAI, Gemini)
 */
class AiEngine(private val context: Context) {

    private var currentMode: AiMode = AiMode.LOCAL
    private var cloudProvider: CloudProvider = CloudProvider.OPENAI
    private var openAiClient: OpenAiClient? = null
    private var geminiClient: GeminiClient? = null
    private val gson = Gson()

    companion object {
        private const val TAG = "AiEngine"
        private val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val CLOUD_PROVIDER = stringPreferencesKey("cloud_provider")
    }

    /**
     * Set AI processing mode
     */
    fun setMode(mode: AiMode) {
        currentMode = mode
    }

    /**
     * Set cloud provider
     */
    suspend fun setCloudProvider(provider: CloudProvider) {
        cloudProvider = provider
        context.dataStore.edit { preferences ->
            preferences[CLOUD_PROVIDER] = provider.name
        }
    }

    /**
     * Get current cloud provider
     */
    suspend fun getCloudProvider(): CloudProvider {
        val providerName = context.dataStore.data.map { preferences ->
            preferences[CLOUD_PROVIDER]
        }.first()

        return providerName?.let {
            try {
                CloudProvider.valueOf(it)
            } catch (e: Exception) {
                CloudProvider.OPENAI
            }
        } ?: CloudProvider.OPENAI
    }

    /**
     * Save API key for specific provider
     */
    suspend fun saveApiKey(provider: CloudProvider, apiKey: String) {
        context.dataStore.edit { preferences ->
            when (provider) {
                CloudProvider.OPENAI -> preferences[OPENAI_API_KEY] = apiKey
                CloudProvider.GEMINI -> preferences[GEMINI_API_KEY] = apiKey
            }
        }

        // Reinitialize client
        if (apiKey.isNotBlank()) {
            when (provider) {
                CloudProvider.OPENAI -> openAiClient = OpenAiClient(context, apiKey)
                CloudProvider.GEMINI -> geminiClient = GeminiClient(context, apiKey)
            }
        }
    }

    /**
     * Get API key for specific provider
     */
    suspend fun getApiKey(provider: CloudProvider): String? {
        return context.dataStore.data.map { preferences ->
            when (provider) {
                CloudProvider.OPENAI -> preferences[OPENAI_API_KEY]
                CloudProvider.GEMINI -> preferences[GEMINI_API_KEY]
            }
        }.first()
    }

    /**
     * Check if API key is configured for current provider
     */
    suspend fun hasApiKey(): Boolean {
        val provider = getCloudProvider()
        return !getApiKey(provider).isNullOrBlank()
    }

    /**
     * Process user command and return AI response with optional action
     */
    suspend fun processCommand(
        command: String,
        conversationHistory: List<com.jarvis.launcher.ChatMessage> = emptyList()
    ): AiResponse {
        return when (currentMode) {
            AiMode.LOCAL -> processWithLocalAi(command)
            AiMode.CLOUD -> processWithCloudAi(command, conversationHistory)
        }
    }

    /**
     * Process command using cloud AI (OpenAI or Gemini)
     */
    private suspend fun processWithCloudAi(
        command: String,
        conversationHistory: List<com.jarvis.launcher.ChatMessage>
    ): AiResponse {
        val provider = getCloudProvider()

        return when (provider) {
            CloudProvider.OPENAI -> processWithOpenAi(command, conversationHistory)
            CloudProvider.GEMINI -> processWithGemini(command, conversationHistory)
        }
    }

    /**
     * Process with OpenAI
     */
    private suspend fun processWithOpenAi(
        command: String,
        conversationHistory: List<com.jarvis.launcher.ChatMessage>
    ): AiResponse {
        return try {
            val apiKey = getApiKey(CloudProvider.OPENAI)
            if (apiKey.isNullOrBlank()) {
                return AiResponse(
                    message = "Please set your OpenAI API key in settings first.",
                    action = null
                )
            }

            // Initialize client if not already
            if (openAiClient == null) {
                openAiClient = OpenAiClient(context, apiKey)
            }

            // Build conversation messages
            val messages = mutableListOf<ChatMessage>()

            // System message
            messages.add(
                ChatMessage(
                    role = "system",
                    content = """You are JARVIS, an AI assistant integrated into an Android launcher. 
                        |You can help users launch apps, control device settings, and answer questions.
                        |Be concise and helpful. Use the provided functions to perform actions.""".trimMargin()
                )
            )

            // Add conversation history
            conversationHistory.forEach { msg ->
                messages.add(
                    ChatMessage(
                        role = if (msg.isUser) "user" else "assistant",
                        content = msg.text
                    )
                )
            }

            // Add current user message
            messages.add(ChatMessage(role = "user", content = command))

            // Get available functions
            val functions = openAiClient!!.getAppControlFunctions()

            // Send to OpenAI
            val response = openAiClient!!.sendMessage(messages, functions)

            // Process response
            val choice = response.choices.firstOrNull()
            val assistantMessage = choice?.message

            // Check if function call is needed
            if (assistantMessage?.functionCall != null) {
                val functionCall = assistantMessage.functionCall
                Log.d(
                    TAG,
                    "OpenAI Function call: ${functionCall.name} with args: ${functionCall.arguments}"
                )

                return handleOpenAiFunctionCall(functionCall, assistantMessage.content ?: "")
            }

            // Regular text response
            AiResponse(
                message = assistantMessage?.content ?: "I'm not sure how to respond to that.",
                action = null
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error with OpenAI", e)
            AiResponse(
                message = "Sorry, I encountered an error: ${e.message}",
                action = null
            )
        }
    }

    /**
     * Process with Gemini
     */
    private suspend fun processWithGemini(
        command: String,
        conversationHistory: List<com.jarvis.launcher.ChatMessage>
    ): AiResponse {
        return try {
            val apiKey = getApiKey(CloudProvider.GEMINI)
            if (apiKey.isNullOrBlank()) {
                return AiResponse(
                    message = "Please set your Gemini API key in settings first.",
                    action = null
                )
            }

            // Initialize client if not already
            if (geminiClient == null) {
                geminiClient = GeminiClient(context, apiKey)
            }

            // Build conversation messages
            val messages = mutableListOf<GeminiMessage>()

            // Add conversation history
            conversationHistory.forEach { msg ->
                messages.add(
                    GeminiMessage(
                        content = msg.text,
                        isUser = msg.isUser
                    )
                )
            }

            // Add current user message
            messages.add(GeminiMessage(content = command, isUser = true))

            // Get available functions
            val functions = geminiClient!!.getAppControlFunctions()

            // Send to Gemini
            val response = geminiClient!!.sendMessage(messages, functions)

            // Process response
            val candidate = response.candidates.firstOrNull()
            val content = candidate?.content
            val part = content?.parts?.firstOrNull()

            // Check if function call is needed
            if (part?.functionCall != null) {
                val functionCall = part.functionCall
                Log.d(
                    TAG,
                    "Gemini Function call: ${functionCall.name} with args: ${functionCall.args}"
                )

                return handleGeminiFunctionCall(functionCall, part.text ?: "")
            }

            // Regular text response
            AiResponse(
                message = part?.text ?: "I'm not sure how to respond to that.",
                action = null
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error with Gemini", e)
            AiResponse(
                message = "Sorry, I encountered an error: ${e.message}",
                action = null
            )
        }
    }

    /**
     * Handle function call from OpenAI
     */
    private fun handleOpenAiFunctionCall(functionCall: FunctionCall, message: String): AiResponse {
        return try {
            val args = gson.fromJson(functionCall.arguments, JsonObject::class.java)

            when (functionCall.name) {
                "launch_app" -> {
                    val appName = args.get("app_name")?.asString ?: ""
                    return handleAppLaunchByName(appName, message)
                }

                "control_device" -> {
                    val action = args.get("action")?.asString ?: ""
                    return AiResponse(
                        message = message.ifBlank { "Executing device control..." },
                        action = AiAction(
                            type = "device_control",
                            data = mapOf("action" to action)
                        )
                    )
                }

                else -> {
                    AiResponse(
                        message = "Unknown function: ${functionCall.name}",
                        action = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling OpenAI function call", e)
            AiResponse(
                message = "Error executing action: ${e.message}",
                action = null
            )
        }
    }

    /**
     * Handle function call from Gemini
     */
    private fun handleGeminiFunctionCall(
        functionCall: GeminiFunctionCall,
        message: String
    ): AiResponse {
        return try {
            when (functionCall.name) {
                "launch_app" -> {
                    val appName = functionCall.args["app_name"]?.toString() ?: ""
                    return handleAppLaunchByName(appName, message)
                }

                "control_device" -> {
                    val action = functionCall.args["action"]?.toString() ?: ""
                    return AiResponse(
                        message = message.ifBlank { "Executing device control..." },
                        action = AiAction(
                            type = "device_control",
                            data = mapOf("action" to action)
                        )
                    )
                }

                else -> {
                    AiResponse(
                        message = "Unknown function: ${functionCall.name}",
                        action = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Gemini function call", e)
            AiResponse(
                message = "Error executing action: ${e.message}",
                action = null
            )
        }
    }

    /**
     * Handle app launch by name (for AI function calling)
     */
    private fun handleAppLaunchByName(appName: String, message: String): AiResponse {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val matchingApp = packageManager.queryIntentActivities(mainIntent, 0)
            .find { resolveInfo ->
                val label = resolveInfo.loadLabel(packageManager).toString()
                label.equals(appName, ignoreCase = true) ||
                        label.contains(appName, ignoreCase = true) ||
                        appName.contains(label, ignoreCase = true)
            }

        return if (matchingApp != null) {
            val packageName = matchingApp.activityInfo.packageName
            val appLabel = matchingApp.loadLabel(packageManager).toString()
            AiResponse(
                message = message.ifBlank { "Opening $appLabel" },
                action = AiAction(
                    type = "launch_app",
                    data = mapOf("packageName" to packageName)
                )
            )
        } else {
            AiResponse(
                message = "I couldn't find an app named '$appName'.",
                action = null
            )
        }
    }

    /**
     * Process command using local AI (rule-based)
     * Current implementation uses pattern matching
     */
    private fun processWithLocalAi(command: String): AiResponse {
        val lowerCommand = command.lowercase().trim()

        // App launching commands
        when {
            lowerCommand.startsWith("open ") || lowerCommand.startsWith("launch ") -> {
                val appName = lowerCommand.removePrefix("open ").removePrefix("launch ").trim()
                return handleAppLaunch(appName)
            }

            // Device control commands (stubbed)
            lowerCommand.contains("turn on wifi") || lowerCommand.contains("enable wifi") -> {
                return AiResponse(
                    message = "I'll enable WiFi for you.",
                    action = AiAction(
                        type = "device_control",
                        data = mapOf("action" to "wifi_on")
                    )
                )
            }

            lowerCommand.contains("turn off wifi") || lowerCommand.contains("disable wifi") -> {
                return AiResponse(
                    message = "I'll disable WiFi for you.",
                    action = AiAction(
                        type = "device_control",
                        data = mapOf("action" to "wifi_off")
                    )
                )
            }

            lowerCommand.contains("flashlight on") || lowerCommand.contains("turn on flashlight") -> {
                return AiResponse(
                    message = "Turning on the flashlight.",
                    action = AiAction(
                        type = "device_control",
                        data = mapOf("action" to "flashlight_on")
                    )
                )
            }

            // Greeting responses
            lowerCommand.contains("hello") || lowerCommand.contains("hi ") -> {
                return AiResponse(
                    message = "Hello! I'm JARVIS. How may I assist you today?",
                    action = AiAction(type = "speak", data = emptyMap())
                )
            }

            lowerCommand.contains("how are you") -> {
                return AiResponse(
                    message = "I'm functioning at optimal capacity, thank you for asking!",
                    action = AiAction(type = "speak", data = emptyMap())
                )
            }

            // Default response
            else -> {
                return AiResponse(
                    message = "I'm still learning. Could you rephrase that, or try 'open [app name]' to launch an application?",
                    action = null
                )
            }
        }
    }

    /**
     * Handle app launching intent (local mode)
     * Searches for app by name and returns launch action
     */
    private fun handleAppLaunch(appName: String): AiResponse {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // Find matching app
        val matchingApp = packageManager.queryIntentActivities(mainIntent, 0)
            .find { resolveInfo ->
                val label = resolveInfo.loadLabel(packageManager).toString()
                label.lowercase().contains(appName.lowercase()) ||
                        appName.lowercase().contains(label.lowercase())
            }

        return if (matchingApp != null) {
            val packageName = matchingApp.activityInfo.packageName
            val appLabel = matchingApp.loadLabel(packageManager).toString()
            AiResponse(
                message = "Opening $appLabel",
                action = AiAction(
                    type = "launch_app",
                    data = mapOf("packageName" to packageName)
                )
            )
        } else {
            AiResponse(
                message = "I couldn't find an app named '$appName'. Please check the name and try again.",
                action = null
            )
        }
    }
}

/**
 * AI processing mode
 */
enum class AiMode {
    LOCAL,  // On-device AI processing
    CLOUD   // Cloud-based AI (OpenAI, Gemini, etc.)
}

/**
 * Cloud AI provider
 */
enum class CloudProvider {
    OPENAI,  // OpenAI GPT models
    GEMINI   // Google Gemini models
}

/**
 * AI response with optional action
 */
data class AiResponse(
    val message: String,
    val action: AiAction? = null
)

/**
 * Action to be executed after AI response
 */
data class AiAction(
    val type: String,  // "launch_app", "device_control", "speak", etc.
    val data: Map<String, String>
)
