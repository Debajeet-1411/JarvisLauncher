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
 * Gemini Client - Handles communication with Google Gemini API
 *
 * Features:
 * - Gemini 2.0 Flash model support (fast and free!)
 * - Function calling for app control
 * - Streaming responses support
 * - Multi-turn conversations
 *
 * Usage:
 * ```
 * val client = GeminiClient(context, apiKey)
 * val response = client.sendMessage(messages, functions)
 * ```
 */
class GeminiClient(
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
        private const val TAG = "GeminiClient"
        private const val DEFAULT_MODEL = "gemini-2.0-flash-exp"
        private const val API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    /**
     * Send message to Gemini with function calling support
     */
    suspend fun sendMessage(
        messages: List<GeminiMessage>,
        functions: List<GeminiFunctionDeclaration>? = null,
        model: String = DEFAULT_MODEL
    ): GeminiResponse = withContext(Dispatchers.IO) {
        try {
            // Convert messages to Gemini format
            val contents = messages.map { msg ->
                GeminiContent(
                    role = if (msg.isUser) "user" else "model",
                    parts = listOf(GeminiPart(text = msg.content))
                )
            }

            // Build request body
            val requestBody = GeminiRequest(
                contents = contents,
                tools = if (functions != null) {
                    listOf(GeminiTools(functionDeclarations = functions))
                } else null,
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7,
                    maxOutputTokens = 500,
                    topP = 0.95
                )
            )

            val jsonBody = gson.toJson(requestBody)
            Log.d(TAG, "Request: $jsonBody")

            val url = "$API_BASE/$model:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            Log.d(TAG, "Response: $responseBody")

            if (!response.isSuccessful) {
                throw Exception("Gemini API Error: ${response.code} - $responseBody")
            }

            gson.fromJson(responseBody, GeminiResponse::class.java)

        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini", e)
            throw e
        }
    }

    /**
     * Get available functions for app control
     */
    fun getAppControlFunctions(): List<GeminiFunctionDeclaration> {
        return listOf(
            GeminiFunctionDeclaration(
                name = "launch_app",
                description = "Launch an application by name. Use this when the user wants to open, launch, or start an app.",
                parameters = GeminiFunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "app_name" to GeminiPropertySchema(
                            type = "string",
                            description = "Name of the app to launch (e.g., 'Chrome', 'YouTube', 'Gmail')"
                        )
                    ),
                    required = listOf("app_name")
                )
            ),
            GeminiFunctionDeclaration(
                name = "control_device",
                description = "Control device settings like WiFi, Bluetooth, or flashlight. Use this for device-level automation tasks.",
                parameters = GeminiFunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "action" to GeminiPropertySchema(
                            type = "string",
                            description = "Action to perform on the device",
                            enum = listOf(
                                "wifi_on",
                                "wifi_off",
                                "wifi_settings",
                                "bluetooth_on",
                                "bluetooth_off",
                                "bluetooth_settings",
                                "flashlight_on",
                                "flashlight_off",
                                "open_settings",
                                "volume_up",
                                "volume_down"
                            )
                        )
                    ),
                    required = listOf("action")
                )
            ),
            GeminiFunctionDeclaration(
                name = "search_apps",
                description = "Search for installed applications by name or category",
                parameters = GeminiFunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "query" to GeminiPropertySchema(
                            type = "string",
                            description = "Search query for finding apps"
                        )
                    ),
                    required = listOf("query")
                )
            ),
            GeminiFunctionDeclaration(
                name = "get_time_info",
                description = "Get current time, date, or day information",
                parameters = GeminiFunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "info_type" to GeminiPropertySchema(
                            type = "string",
                            description = "Type of time information to retrieve",
                            enum = listOf("time", "date", "day", "full")
                        )
                    ),
                    required = listOf("info_type")
                )
            ),
            GeminiFunctionDeclaration(
                name = "get_battery_status",
                description = "Get device battery level and charging status",
                parameters = GeminiFunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "detailed" to GeminiPropertySchema(
                            type = "boolean",
                            description = "Whether to return detailed battery information"
                        )
                    ),
                    required = listOf()
                )
            )
        )
    }
}

// ==================== Data Classes ====================

/**
 * Gemini API Request
 */
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val tools: List<GeminiTools>? = null,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

/**
 * Message for conversation
 */
data class GeminiMessage(
    val content: String,
    val isUser: Boolean
)

/**
 * Content in conversation
 */
data class GeminiContent(
    val role: String, // "user" or "model"
    val parts: List<GeminiPart>
)

/**
 * Part of content (text or function call)
 */
data class GeminiPart(
    val text: String? = null,
    @SerializedName("functionCall") val functionCall: GeminiFunctionCall? = null,
    @SerializedName("functionResponse") val functionResponse: GeminiFunctionResponse? = null
)

/**
 * Function call
 */
data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, Any>
)

/**
 * Function response
 */
data class GeminiFunctionResponse(
    val name: String,
    val response: Map<String, Any>
)

/**
 * Tools (functions) definition
 */
data class GeminiTools(
    @SerializedName("functionDeclarations") val functionDeclarations: List<GeminiFunctionDeclaration>
)

/**
 * Function declaration
 */
data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: GeminiFunctionParameters
)

/**
 * Function parameters
 */
data class GeminiFunctionParameters(
    val type: String,
    val properties: Map<String, GeminiPropertySchema>,
    val required: List<String>? = null
)

/**
 * Property schema
 */
data class GeminiPropertySchema(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)

/**
 * Generation config
 */
data class GeminiGenerationConfig(
    val temperature: Double = 0.7,
    @SerializedName("maxOutputTokens") val maxOutputTokens: Int = 500,
    val topP: Double = 0.95,
    val topK: Int? = null
)

/**
 * Gemini API Response
 */
data class GeminiResponse(
    val candidates: List<GeminiCandidate>,
    @SerializedName("usageMetadata") val usageMetadata: GeminiUsageMetadata?
)

/**
 * Response candidate
 */
data class GeminiCandidate(
    val content: GeminiContent,
    @SerializedName("finishReason") val finishReason: String?,
    val index: Int
)

/**
 * Usage metadata
 */
data class GeminiUsageMetadata(
    @SerializedName("promptTokenCount") val promptTokenCount: Int,
    @SerializedName("candidatesTokenCount") val candidatesTokenCount: Int,
    @SerializedName("totalTokenCount") val totalTokenCount: Int
)
