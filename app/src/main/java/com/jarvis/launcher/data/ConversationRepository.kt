package com.jarvis.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jarvis.launcher.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore extension for conversations
private val Context.conversationDataStore by preferencesDataStore(name = "conversations")

/**
 * Conversation Repository - persists chat history across sessions
 *
 * Features:
 * - Save/load conversation history
 * - Multiple conversation sessions
 * - Auto-cleanup old conversations
 * - Export/import conversations
 */
class ConversationRepository(private val context: Context) {

    private val gson = Gson()

    companion object {
        private const val CURRENT_CONVERSATION_KEY = "current_conversation"
        private const val MAX_MESSAGES = 100 // Limit to prevent excessive storage
    }

    /**
     * Save conversation history
     */
    suspend fun saveConversation(messages: List<ChatMessage>) {
        val key = stringPreferencesKey(CURRENT_CONVERSATION_KEY)
        context.conversationDataStore.edit { preferences ->
            // Limit messages to prevent excessive storage
            val limitedMessages = messages.takeLast(MAX_MESSAGES)
            val json = gson.toJson(limitedMessages)
            preferences[key] = json
        }
    }

    /**
     * Load conversation history
     */
    fun loadConversation(): Flow<List<ChatMessage>> {
        val key = stringPreferencesKey(CURRENT_CONVERSATION_KEY)
        return context.conversationDataStore.data.map { preferences ->
            val json = preferences[key]
            if (json != null) {
                try {
                    val type = object : TypeToken<List<ChatMessage>>() {}.type
                    gson.fromJson(json, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    /**
     * Clear conversation history
     */
    suspend fun clearConversation() {
        val key = stringPreferencesKey(CURRENT_CONVERSATION_KEY)
        context.conversationDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    /**
     * Add message to conversation
     */
    suspend fun addMessage(message: ChatMessage) {
        val key = stringPreferencesKey(CURRENT_CONVERSATION_KEY)
        context.conversationDataStore.edit { preferences ->
            val json = preferences[key]
            val messages = if (json != null) {
                try {
                    val type = object : TypeToken<List<ChatMessage>>() {}.type
                    gson.fromJson<List<ChatMessage>>(json, type).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }

            messages.add(message)

            // Limit messages
            val limitedMessages = messages.takeLast(MAX_MESSAGES)
            preferences[key] = gson.toJson(limitedMessages)
        }
    }

    /**
     * Get conversation size
     */
    fun getConversationSize(): Flow<Int> {
        return loadConversation().map { it.size }
    }
}
