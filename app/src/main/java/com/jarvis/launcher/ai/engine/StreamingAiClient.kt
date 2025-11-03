package com.jarvis.launcher.ai.engine

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException

/**
 * Streaming AI Client for Real-Time Responses
 *
 * Day 5: Streaming token-by-token responses
 *
 * Features:
 * - Server-Sent Events (SSE) support
 * - Token-by-token streaming
 * - OpenAI streaming format
 * - Gemini streaming format
 * - Error handling with retry
 * - Flow-based API for reactive updates
 */
class StreamingAiClient(
    private val apiKey: String,
    private val provider: CloudProvider
) {

    companion object {
        private const val TAG = "StreamingAiClient"
        private const val OPENAI_STREAMING_URL = "https://api.openai.com/v1/chat/completions"
        private const val GEMINI_STREAMING_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:streamGenerateContent"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Stream AI response token by token
     */
    fun streamResponse(
        prompt: String,
        conversationHistory: List<StreamChatMessage> = emptyList()
    ): Flow<StreamChunk> = flow {
        try {
            when (provider) {
                CloudProvider.OPENAI -> streamOpenAI(
                    prompt,
                    conversationHistory
                ).collect { emit(it) }

                CloudProvider.GEMINI -> streamGemini(
                    prompt,
                    conversationHistory
                ).collect { emit(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Streaming error", e)
            emit(StreamChunk.Error(e.message ?: "Unknown error"))
        }
    }

    /**
     * Stream OpenAI response
     */
    private fun streamOpenAI(
        prompt: String,
        conversationHistory: List<StreamChatMessage>
    ): Flow<StreamChunk> = flow {
        val messages = buildOpenAIMessages(prompt, conversationHistory)

        val requestBody = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", messages)
            put("stream", true)
            put("temperature", 0.7)
            put("max_tokens", 500)
        }.toString()

        val request = Request.Builder()
            .url(OPENAI_STREAMING_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit(StreamChunk.Error("HTTP ${response.code}: ${response.message}"))
                return@flow
            }

            val reader = response.body?.byteStream()?.bufferedReader() ?: return@flow

            reader.use { bufferedReader ->
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue

                    if (currentLine.startsWith("data: ")) {
                        val data = currentLine.substring(6).trim()

                        if (data == "[DONE]") {
                            emit(StreamChunk.Done)
                            break
                        }

                        try {
                            val json = JSONObject(data)
                            val choices = json.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val choice = choices.getJSONObject(0)
                                val delta = choice.optJSONObject("delta")
                                val content = delta?.optString("content")

                                if (!content.isNullOrEmpty()) {
                                    emit(StreamChunk.Token(content))
                                }

                                val finishReason = choice.optString("finish_reason")
                                if (finishReason == "stop") {
                                    emit(StreamChunk.Done)
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing SSE chunk", e)
                        }
                    }
                }
            }
        }
    }

    /**
     * Stream Gemini response
     */
    private fun streamGemini(
        prompt: String,
        conversationHistory: List<StreamChatMessage>
    ): Flow<StreamChunk> = flow {
        val contents = buildGeminiContents(prompt, conversationHistory)

        val requestBody = JSONObject().apply {
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 500)
            })
        }.toString()

        val url = "$GEMINI_STREAMING_URL?key=$apiKey&alt=sse"

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit(StreamChunk.Error("HTTP ${response.code}: ${response.message}"))
                return@flow
            }

            val reader = response.body?.byteStream()?.bufferedReader() ?: return@flow

            reader.use { bufferedReader ->
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue

                    if (currentLine.startsWith("data: ")) {
                        val data = currentLine.substring(6).trim()

                        try {
                            val json = JSONObject(data)
                            val candidates = json.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val candidate = candidates.getJSONObject(0)
                                val content = candidate.optJSONObject("content")
                                val parts = content?.optJSONArray("parts")

                                if (parts != null && parts.length() > 0) {
                                    val part = parts.getJSONObject(0)
                                    val text = part.optString("text")

                                    if (text.isNotEmpty()) {
                                        emit(StreamChunk.Token(text))
                                    }
                                }

                                val finishReason = candidate.optString("finishReason")
                                if (finishReason == "STOP") {
                                    emit(StreamChunk.Done)
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing Gemini SSE chunk", e)
                        }
                    }
                }
            }
        }
    }

    /**
     * Build OpenAI message format
     */
    private fun buildOpenAIMessages(
        prompt: String,
        history: List<StreamChatMessage>
    ): JSONArray {
        return JSONArray().apply {
            // System message
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are JARVIS, a helpful AI assistant for an Android launcher.")
            })

            // History
            history.takeLast(10).forEach { msg ->
                put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "assistant")
                    put("content", msg.text)
                })
            }

            // Current prompt
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }
    }

    /**
     * Build Gemini content format
     */
    private fun buildGeminiContents(
        prompt: String,
        history: List<StreamChatMessage>
    ): JSONArray {
        return JSONArray().apply {
            // History
            history.takeLast(10).forEach { msg ->
                put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", msg.text)
                        })
                    })
                })
            }

            // Current prompt
            put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", prompt)
                    })
                })
            })
        }
    }
}

/**
 * Stream chunk types
 */
sealed class StreamChunk {
    data class Token(val text: String) : StreamChunk()
    object Done : StreamChunk()
    data class Error(val message: String) : StreamChunk()
}

/**
 * Chat message for streaming context
 */
data class StreamChatMessage(
    val text: String,
    val isUser: Boolean
)
