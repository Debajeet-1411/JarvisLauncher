package com.jarvis.launcher.ai.engine

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Vision Client - Handles image analysis with OpenAI and Gemini
 *
 * Features:
 * - Image description
 * - OCR (text extraction)
 * - Visual question answering
 * - Object detection
 */
class VisionClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val TAG = "VisionClient"
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        private const val GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent"
    }

    /**
     * Analyze image using OpenAI GPT-4 Vision
     */
    suspend fun analyzeWithOpenAI(
        bitmap: Bitmap,
        prompt: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(bitmap)

            val messages = JsonObject().apply {
                add(
                    "messages", gson.toJsonTree(
                        listOf(
                            mapOf(
                                "role" to "user",
                                "content" to listOf(
                                    mapOf("type" to "text", "text" to prompt),
                                    mapOf(
                                        "type" to "image_url",
                                        "image_url" to mapOf(
                                            "url" to "data:image/jpeg;base64,$base64Image"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                addProperty("model", "gpt-4o-mini")
                addProperty("max_tokens", 500)
            }

            val requestBody = messages.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("OpenAI Vision API error: ${response.code}")
                }

                val responseBody = response.body?.string() ?: ""
                val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)

                return@withContext jsonResponse
                    .getAsJsonArray("choices")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("message")
                    ?.get("content")?.asString
                    ?: "Unable to analyze image"
            }

        } catch (e: Exception) {
            Log.e(TAG, "OpenAI Vision error", e)
            throw e
        }
    }

    /**
     * Analyze image using Gemini Pro Vision
     */
    suspend fun analyzeWithGemini(
        bitmap: Bitmap,
        prompt: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(bitmap)

            val requestJson = JsonObject().apply {
                add(
                    "contents", gson.toJsonTree(
                        listOf(
                            mapOf(
                                "parts" to listOf(
                                    mapOf("text" to prompt),
                                    mapOf(
                                        "inlineData" to mapOf(
                                            "mimeType" to "image/jpeg",
                                            "data" to base64Image
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                add(
                    "generationConfig", gson.toJsonTree(
                        mapOf(
                            "temperature" to 0.4,
                            "topK" to 32,
                            "topP" to 1,
                            "maxOutputTokens" to 500
                        )
                    )
                )
            }

            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$GEMINI_API_URL?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Gemini Vision API error: ${response.code}")
                }

                val responseBody = response.body?.string() ?: ""
                val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)

                return@withContext jsonResponse
                    .getAsJsonArray("candidates")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("content")
                    ?.getAsJsonArray("parts")
                    ?.get(0)?.asJsonObject
                    ?.get("text")?.asString
                    ?: "Unable to analyze image"
            }

        } catch (e: Exception) {
            Log.e(TAG, "Gemini Vision error", e)
            throw e
        }
    }

    /**
     * Convert bitmap to base64 string
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress to reduce size (max 20MB for APIs)
        val quality = if (bitmap.byteCount > 5_000_000) 50 else 80
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Predefined vision prompts for quick actions
     */
    object VisionPrompts {
        const val DESCRIBE = "Describe what you see in this image in detail."
        const val OCR = "Extract and list all text visible in this image."
        const val IDENTIFY_OBJECTS = "List all objects and items you can identify in this image."
        const val ANALYZE_SCENE =
            "Analyze this scene and describe the context, setting, and any notable details."
        const val ANSWER_QUESTION = "Answer this question about the image: "
    }
}

/**
 * Vision response with analyzed content
 */
data class VisionResponse(
    val description: String,
    val confidence: Float = 0f,
    val detectedText: List<String> = emptyList(),
    val objects: List<String> = emptyList()
)
