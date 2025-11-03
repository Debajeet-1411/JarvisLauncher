package com.jarvis.launcher.ai.engine

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OpenAI Client - Handles communication with OpenAI API
 *
 * Features:
 * - GPT-4 / GPT-3.5-turbo support
 * - Function calling for app control
 * - Streaming responses (future)
 * - Error handling
 *
 * Usage:
 * ```
 * val client = OpenAiClient(context, apiKey)
 * val response = client.sendMessage(messages, functions)
 * ```
 */
class OpenAiClient(
    private val context: Context,
    private val apiKey: String
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val TAG = "OpenAiClient"
        private const val API_URL = "https://api.openai.com/v1/chat/completions"
        private const val DEFAULT_MODEL = "gpt-4o-mini" // Cost-effective model
    }

    /**
     * Send message to OpenAI with function calling support
     */
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        functions: List<FunctionDefinition>? = null,
        model: String = DEFAULT_MODEL
    ): OpenAiResponse = withContext(Dispatchers.IO) {
        try {
            val requestBody = OpenAiRequest(
                model = model,
                messages = messages,
                functions = functions,
                temperature = 0.7,
                maxTokens = 500
            )

            val jsonBody = gson.toJson(requestBody)
            Log.d(TAG, "Request: $jsonBody")

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            Log.d(TAG, "Response: $responseBody")

            if (!response.isSuccessful) {
                throw Exception("API Error: ${response.code} - $responseBody")
            }

            gson.fromJson(responseBody, OpenAiResponse::class.java)

        } catch (e: Exception) {
            Log.e(TAG, "Error calling OpenAI", e)
            throw e
        }
    }

    /**
     * Get available functions for app control
     */
    fun getAppControlFunctions(): List<FunctionDefinition> {
        return listOf(
            FunctionDefinition(
                name = "launch_app",
                description = "Launch an application by name",
                parameters = FunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "app_name" to PropertyDefinition(
                            type = "string",
                            description = "Name of the app to launch (e.g., 'Chrome', 'YouTube')"
                        )
                    ),
                    required = listOf("app_name")
                )
            ),
            FunctionDefinition(
                name = "control_device",
                description = "Control device settings like WiFi, Bluetooth, flashlight",
                parameters = FunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "action" to PropertyDefinition(
                            type = "string",
                            description = "Action to perform",
                            enum = listOf(
                                "wifi_on",
                                "wifi_off",
                                "bluetooth_on",
                                "bluetooth_off",
                                "flashlight_on",
                                "flashlight_off"
                            )
                        )
                    ),
                    required = listOf("action")
                )
            )
        )
    }
}

// ==================== Data Classes ====================

/**
 * OpenAI API Request
 */
data class OpenAiRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val functions: List<FunctionDefinition>? = null,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = 500,
    @SerializedName("function_call") val functionCall: String? = "auto"
)

/**
 * Chat message
 */
data class ChatMessage(
    val role: String, // "system", "user", "assistant", "function"
    val content: String?,
    val name: String? = null,
    @SerializedName("function_call") val functionCall: FunctionCall? = null
)

/**
 * Function call
 */
data class FunctionCall(
    val name: String,
    val arguments: String
)

/**
 * Function definition
 */
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: FunctionParameters
)

/**
 * Function parameters
 */
data class FunctionParameters(
    val type: String,
    val properties: Map<String, PropertyDefinition>,
    val required: List<String>? = null
)

/**
 * Property definition
 */
data class PropertyDefinition(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)

/**
 * OpenAI API Response
 */
data class OpenAiResponse(
    val id: String,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage?
)

/**
 * Response choice
 */
data class Choice(
    val index: Int,
    val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String
)

/**
 * Token usage
 */
data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)
