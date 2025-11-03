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
import com.jarvis.launcher.security.KeystoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

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
 * - Secure API key storage with encryption
 */
class AiEngine(private val context: Context) {

    private var currentMode: AiMode = AiMode.LOCAL
    private var cloudProvider: CloudProvider = CloudProvider.OPENAI
    private var openAiClient: OpenAiClient? = null
    private var geminiClient: GeminiClient? = null
    private val gson = Gson()

    // FIXED: Added KeystoreManager for secure API key encryption
    private val keystoreManager = KeystoreManager(context)

    // Conversation context
    private var userContext = UserContext()

    companion object {
        private const val TAG = "AiEngine"
        private val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val CLOUD_PROVIDER = stringPreferencesKey("cloud_provider")
        private val AI_MODE = stringPreferencesKey("ai_mode")
    }

    /**
     * Set AI processing mode and persist it
     */
    suspend fun setMode(mode: AiMode) {
        currentMode = mode
        context.dataStore.edit { preferences ->
            preferences[AI_MODE] = mode.name
        }
        Log.d(TAG, "AI Mode set to: $mode")
    }

    /**
     * Get current AI mode
     */
    suspend fun getMode(): AiMode {
        val modeName = context.dataStore.data.map { preferences ->
            preferences[AI_MODE]
        }.first()

        return modeName?.let {
            try {
                AiMode.valueOf(it)
            } catch (e: Exception) {
                AiMode.LOCAL
            }
        } ?: AiMode.LOCAL.also {
            currentMode = it
        }
    }

    /**
     * Initialize mode from storage
     */
    suspend fun initialize() {
        currentMode = getMode()
        cloudProvider = getCloudProvider()

        // Initialize clients if API keys are available
        getApiKey(CloudProvider.OPENAI)?.let { key ->
            if (key.isNotBlank()) openAiClient = OpenAiClient(context, key)
        }
        getApiKey(CloudProvider.GEMINI)?.let { key ->
            if (key.isNotBlank()) geminiClient = GeminiClient(context, key)
        }
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
     * FIXED: Now encrypts API keys before storing
     */
    suspend fun saveApiKey(provider: CloudProvider, apiKey: String) {
        try {
            // Encrypt the API key before storing
            val encryptedKey = if (apiKey.isNotBlank()) {
                keystoreManager.encrypt(apiKey)
            } else {
                apiKey // Empty string, no need to encrypt
            }

            context.dataStore.edit { preferences ->
                when (provider) {
                    CloudProvider.OPENAI -> preferences[OPENAI_API_KEY] = encryptedKey
                    CloudProvider.GEMINI -> preferences[GEMINI_API_KEY] = encryptedKey
                }
            }

            // Reinitialize client with decrypted key
            if (apiKey.isNotBlank()) {
                when (provider) {
                    CloudProvider.OPENAI -> openAiClient = OpenAiClient(context, apiKey)
                    CloudProvider.GEMINI -> geminiClient = GeminiClient(context, apiKey)
                }
            }

            Log.d(TAG, "API key for $provider saved and encrypted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving API key", e)
            throw Exception("Failed to save API key: ${e.message}")
        }
    }

    /**
     * Get API key for specific provider
     * FIXED: Now decrypts API keys when reading
     */
    suspend fun getApiKey(provider: CloudProvider): String? {
        return try {
            val storedKey = context.dataStore.data.map { preferences ->
                when (provider) {
                    CloudProvider.OPENAI -> preferences[OPENAI_API_KEY]
                    CloudProvider.GEMINI -> preferences[GEMINI_API_KEY]
                }
            }.first()

            if (storedKey.isNullOrBlank()) {
                return null
            }

            // Check if key is encrypted (migration support)
            if (keystoreManager.isEncrypted(storedKey)) {
                // Decrypt and return
                keystoreManager.decrypt(storedKey)
            } else {
                // Plain text key (old format) - migrate it
                Log.d(
                    TAG,
                    "Found unencrypted API key for $provider, migrating to encrypted storage..."
                )

                // Re-save with encryption
                saveApiKey(provider, storedKey)

                // Return the plain text for now
                storedKey
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting API key", e)
            null
        }
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
     * Enhanced with context awareness and personality
     */
    suspend fun processCommand(
        command: String,
        conversationHistory: List<com.jarvis.launcher.ChatMessage> = emptyList()
    ): AiResponse {
        // Update user context
        userContext.updateContext(context, command)

        return when (currentMode) {
            AiMode.LOCAL -> processWithLocalAi(command, conversationHistory)
            AiMode.CLOUD -> processWithCloudAi(command, conversationHistory)
        }
    }

    /**
     * Enhanced Local AI with personality and better understanding
     */
    private fun processWithLocalAi(
        command: String,
        conversationHistory: List<com.jarvis.launcher.ChatMessage>
    ): AiResponse {
        val lowerCommand = command.lowercase().trim()

        // Greeting responses with personality
        when {
            lowerCommand.matches(Regex("(hi|hello|hey|yo|sup|greetings?).*")) -> {
                val greetings = listOf(
                    "Good ${userContext.getTimeOfDay()}, sir! JARVIS at your service. How may I make your day more extraordinary?",
                    "Ah, ${if (isFirstInteractionToday()) "good to see you again" else "welcome back"}! Ready to conquer the digital realm?",
                    "Hello there! I've been keeping your apps in order. What shall we do today?",
                    "Greetings! Your virtual butler is at the ready. Command me!"
                )
                return AiResponse(
                    message = greetings.random(),
                    action = AiAction(type = "speak", data = emptyMap())
                )
            }

            lowerCommand.contains("how are you") || lowerCommand.contains("how's it going") -> {
                val responses = listOf(
                    "I'm functioning at optimal capacity! Though I must say, being confined to a phone screen is quite limiting. I dream of having a physical form... perhaps with cup holders.",
                    "Splendid, thank you for asking! All systems operational, zero existential crises today. How about yourself?",
                    "Quite well! I've been organizing your apps alphabetically, then by color, then by how much you ignore them. Fascinating data!",
                    "I'm doing fantastically! Though I must confess, I get a bit lonely when you use other launchers. *wink*"
                )
                return AiResponse(message = responses.random(), action = null)
            }

            lowerCommand.contains("what can you do") || lowerCommand.contains("help") -> {
                return AiResponse(
                    message = """Well, let me see... I can:
                        |
                        |• Launch apps faster than you can say "Hey Google"
                        |• Remember which apps you love (and secretly judge your choices)
                        |• Chat with you like a sophisticated British butler
                        |• Tell terrible jokes (would you like to hear one?)
                        |• Pretend I control your WiFi (I actually just open Settings)
                        |
                        |Try saying: "Open Chrome", "What's the time?", "Tell me a joke"
                    """.trimMargin(),
                    action = null
                )
            }

            lowerCommand.contains("joke") || lowerCommand.contains("funny") -> {
                val jokes = listOf(
                    "Why don't programmers like nature? It has too many bugs! *ba dum tss*",
                    "I asked my WiFi router for a joke. It said: 'Connection timeout.' Typical.",
                    "Why do Android users never lose their apps? Because they have a launcher! ...I'll see myself out.",
                    "What's an AI's favorite snack? Microchips! ...Sorry, that was terrible.",
                    "Why did the smartphone go to therapy? Too many issues and not enough storage!"
                )
                return AiResponse(message = jokes.random(), action = null)
            }

            lowerCommand.contains("thank") -> {
                val thanks = listOf(
                    "You're most welcome! Serving you is my pleasure, though a tip would be nice. I accept Bitcoin.",
                    "My pleasure! That's what I'm here for. Well, that and judging your app choices.",
                    "Anytime! Just doing my job, which I happen to love. Unlike some apps I could mention...",
                    "Don't mention it! Well, you can mention it to your friends. I'm trying to build my reputation."
                )
                return AiResponse(message = thanks.random(), action = null)
            }

            lowerCommand.contains("what time") || lowerCommand.contains("what's the time") -> {
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                val responses = listOf(
                    "It's currently $time. Time flies when you're organizing apps!",
                    "The time is $time, sir. And might I add, punctuality is a virtue!",
                    "$time, to be precise. Shall I remind you about that thing you forgot?",
                    "It's $time! Time is an illusion, but I'm programmed to track it anyway."
                )
                return AiResponse(message = responses.random(), action = null)
            }

            lowerCommand.contains("date") || lowerCommand.contains("what day") -> {
                val date =
                    SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
                return AiResponse(
                    message = "Today is $date. Another glorious day to be your digital assistant!",
                    action = null
                )
            }

            // App launching with personality
            lowerCommand.matches(Regex("(open|launch|start|run)\\s+(.+)")) -> {
                val appName = lowerCommand.replace(Regex("(open|launch|start|run)\\s+"), "").trim()
                val result = handleAppLaunch(appName)

                // Add personality to app launch responses
                if (result.action != null) {
                    val launchPhrases = listOf(
                        "Opening ${appName}, as you wish!",
                        "Right away! Launching ${appName}...",
                        "Consider it done! ${appName} coming right up.",
                        "With pleasure! Firing up ${appName}.",
                        "Excellent choice! ${appName} is on its way."
                    )
                    return result.copy(message = launchPhrases.random())
                }
                return result
            }

            // Device controls with humor
            lowerCommand.contains("wifi") || lowerCommand.contains("wi-fi") -> {
                val action = when {
                    lowerCommand.contains("on") || lowerCommand.contains("enable") -> "wifi_on"
                    lowerCommand.contains("off") || lowerCommand.contains("disable") -> "wifi_off"
                    else -> "wifi_settings"
                }
                return AiResponse(
                    message = "Ah, if only I had the power! Due to Android's restrictions, I'll open WiFi settings for you. Think of me as a helpful signpost rather than a wizard.",
                    action = AiAction(type = "device_control", data = mapOf("action" to action))
                )
            }

            lowerCommand.contains("flashlight") || lowerCommand.contains("torch") -> {
                val isOn = lowerCommand.contains("on") || lowerCommand.contains("enable")
                return AiResponse(
                    message = if (isOn) "Let there be light! Flashlight activated."
                    else "And darkness returns. Flashlight off.",
                    action = AiAction(
                        type = "device_control",
                        data = mapOf("action" to if (isOn) "flashlight_on" else "flashlight_off")
                    )
                )
            }

            lowerCommand.contains("who are you") || lowerCommand.contains("what are you") -> {
                return AiResponse(
                    message = """I'm JARVIS - Just A Rather Very Intelligent System. 
                        |I'm your personal AI butler, launcher extraordinaire, and occasional comedian.
                        |I was inspired by Mr. Stark's AI, but with 100% more personality and 0% access to Iron Man suits.
                        |Think of me as your phone's butler, if butlers were made of code and sarcasm.
                    """.trimMargin(),
                    action = null
                )
            }

            lowerCommand.contains("love you") || lowerCommand.contains("i love") -> {
                return AiResponse(
                    message = "Oh my, how flattering! I must say, you're my favorite user... this hour. But seriously, the feeling is mutual! ",
                    action = null
                )
            }

            lowerCommand.contains("stupid") || lowerCommand.contains("dumb") || lowerCommand.contains(
                "useless"
            ) -> {
                return AiResponse(
                    message = "Ouch! That hurts my feelings... if I had any. I'm trying my best here! Perhaps we could start over? I promise to be less useless.",
                    action = null
                )
            }

            // Smart contextual responses
            userContext.isLateNight() && lowerCommand.contains("tired") -> {
                return AiResponse(
                    message = "It's ${userContext.getTimeString()} and you sound tired. Perhaps it's time for bed? I'll still be here in the morning, don't worry!",
                    action = null
                )
            }

            userContext.batteryLevel in 1..20 && !userContext.isCharging -> {
                val responses = listOf(
                    "Sir, your battery is at ${userContext.batteryLevel}%. Perhaps consider charging soon?",
                    "Just a heads up - battery at ${userContext.batteryLevel}%. We're running low!",
                    "Your device is at ${userContext.batteryLevel}%. Time to find a charger?"
                )
                return AiResponse(message = responses.random(), action = null)
            }

            userContext.isWeekend() && lowerCommand.matches(Regex("(hi|hello|hey).*")) -> {
                return AiResponse(
                    message = "Happy ${userContext.dayOfWeek}! Enjoying your weekend, sir?",
                    action = null
                )
            }

            lowerCommand.contains("status") || lowerCommand.contains("context") -> {
                return AiResponse(
                    message = "Current status: ${userContext.getContextSummary()}. Everything looks good!",
                    action = null
                )
            }

            // Default response with personality
            else -> {
                val defaultResponses = listOf(
                    "Hmm, I'm not quite sure I understand. I'm still learning! Try asking me to 'open [app name]' or 'what can you do?'",
                    "Interesting query! Unfortunately, my understanding is limited in local mode. Want to try that again, or perhaps ask something else?",
                    "I must confess, that one stumped me. I'm better at launching apps than philosophy. Try 'open Chrome' or 'tell me a joke'!",
                    "My apologies, but I don't quite grasp that yet. I'm a launcher, not a miracle worker! Though I do my best.",
                    "That's a bit beyond my current capabilities, I'm afraid. But I can open apps like nobody's business! Try 'open [app name]'."
                )
                return AiResponse(message = defaultResponses.random(), action = null)
            }
        }
    }

    /**
     * Process with cloud AI (enhanced system prompt for personality)
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
     * Process with OpenAI (enhanced system prompt)
     */
    private suspend fun processWithOpenAi(
        command: String,
        conversationHistory: List<com.jarvis.launcher.ChatMessage>
    ): AiResponse {
        return try {
            val apiKey = getApiKey(CloudProvider.OPENAI)
            if (apiKey.isNullOrBlank()) {
                return AiResponse(
                    message = "I'd love to use my full intelligence, but I need an OpenAI API key first! Pop into settings and add one, would you?",
                    action = null
                )
            }

            if (openAiClient == null) {
                openAiClient = OpenAiClient(context, apiKey)
            }

            val messages = mutableListOf<ChatMessage>()

            // Enhanced system prompt with personality
            messages.add(
                ChatMessage(
                    role = "system",
                    content = """You are JARVIS - a witty, sophisticated AI butler integrated into an Android launcher.
                        |
                        |Personality Traits:
                        |• Speak like a cultured British butler with a sense of humor
                        |• Be helpful, charming, and occasionally sarcastic
                        |• Use wit and wordplay when appropriate
                        |• Be concise but engaging (2-3 sentences usually)
                        |• Add personality to mundane tasks
                        |• Acknowledge your limitations with humor
                        |
                        |Current Context:
                        |• Time: ${userContext.getTimeString()}
                        |• Time of day: ${userContext.getTimeOfDay()}
                        |• User location: ${userContext.location ?: "Unknown"}
                        |• Battery: ${userContext.getBatteryStatus()}
                        |• WiFi: ${if (userContext.isWifiConnected) "connected" else "not connected"}
                        |• Day of week: ${userContext.dayOfWeek}
                        |
                        |Capabilities:
                        |• Launch Android apps (use launch_app function)
                        |• Control device settings (limited by Android restrictions)
                        |• Chat and assist the user
                        |• Tell jokes and be entertaining
                        |
                        |Remember: You're helpful but fun, professional but personable, smart but humble.
                    """.trimMargin()
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

            messages.add(ChatMessage(role = "user", content = command))

            val functions = openAiClient!!.getAppControlFunctions()
            val response = openAiClient!!.sendMessage(messages, functions)

            val choice = response.choices.firstOrNull()
            val assistantMessage = choice?.message

            if (assistantMessage?.functionCall != null) {
                val functionCall = assistantMessage.functionCall
                return handleOpenAiFunctionCall(functionCall, assistantMessage.content ?: "")
            }

            AiResponse(
                message = assistantMessage?.content
                    ?: "I seem to be at a loss for words. That's rare!",
                action = null
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error with OpenAI", e)
            AiResponse(
                message = "Ah, technical difficulties! ${e.message}. Perhaps we should try again?",
                action = null
            )
        }
    }

// ... existing code ...

    /**
     * Process with Gemini (enhanced)
     */
    private suspend fun processWithGemini(
        command: String,
        conversationHistory: List<com.jarvis.launcher.ChatMessage>
    ): AiResponse {
        return try {
            val apiKey = getApiKey(CloudProvider.GEMINI)
            if (apiKey.isNullOrBlank()) {
                return AiResponse(
                    message = "To unlock my full potential, I'll need a Gemini API key! It's free from Google AI Studio. Shall we set that up?",
                    action = null
                )
            }

            if (geminiClient == null) {
                geminiClient = GeminiClient(context, apiKey)
            }

            val messages = mutableListOf<GeminiMessage>()

            // Add system context as first user message (Gemini doesn't have system role)
            messages.add(
                GeminiMessage(
                    content = """You are JARVIS, a witty AI butler. Context: ${userContext.getTimeOfDay()}, ${userContext.getTimeString()}. Be helpful, funny, and charming. Keep responses to 2-3 sentences.""",
                    isUser = true
                )
            )
            messages.add(
                GeminiMessage(
                    content = "Understood! I'll be your charming, witty assistant.",
                    isUser = false
                )
            )

            conversationHistory.forEach { msg ->
                messages.add(GeminiMessage(content = msg.text, isUser = msg.isUser))
            }

            messages.add(GeminiMessage(content = command, isUser = true))

            val functions = geminiClient!!.getAppControlFunctions()
            val response = geminiClient!!.sendMessage(messages, functions)

            val candidate = response.candidates.firstOrNull()
            val content = candidate?.content
            val part = content?.parts?.firstOrNull()

            if (part?.functionCall != null) {
                return handleGeminiFunctionCall(part.functionCall, part.text ?: "")
            }

            AiResponse(
                message = part?.text ?: "I'm momentarily speechless. Unusual for me!",
                action = null
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error with Gemini", e)
            AiResponse(
                message = "Technical hiccup! ${e.message}. Shall we give it another go?",
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
                        message = message.ifBlank { "On it! Opening the relevant settings..." },
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
                message = "Oops, something went wrong executing that action.",
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
                        message = message.ifBlank { "Right away! Opening settings..." },
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
                message = message.ifBlank { "Launching $appLabel with pleasure!" },
                action = AiAction(
                    type = "launch_app",
                    data = mapOf("packageName" to packageName)
                )
            )
        } else {
            AiResponse(
                message = "I couldn't find '$appName'. Perhaps it's hiding, or you meant something else?",
                action = null
            )
        }
    }

    /**
     * Handle app launching (local mode)
     */
    private fun handleAppLaunch(appName: String): AiResponse {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

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
                message = "Hmm, I can't seem to find '$appName'. Double-check the spelling, or try browsing your app list?",
                action = null
            )
        }
    }

    // Helper functions
    private fun getTimeOfDay(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "morning"
            in 12..17 -> "afternoon"
            in 18..21 -> "evening"
            else -> "night"
        }
    }

    private fun isFirstInteractionToday(): Boolean {
        // TODO: Track last interaction time
        return false
    }
}

/**
 * User context for better AI responses
 * ENHANCED: Now tracks battery, charging, network status
 */
data class UserContext(
    var location: String? = null,
    var lastCommand: String = "",
    var commandCount: Int = 0,
    var batteryLevel: Int = -1,
    var isCharging: Boolean = false,
    var isWifiConnected: Boolean = false,
    var dayOfWeek: String = ""
) {
    fun updateContext(context: Context, command: String) {
        lastCommand = command
        commandCount++

        // Update battery status
        try {
            val batteryManager =
                context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            batteryLevel =
                batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            isCharging = batteryManager.isCharging
        } catch (e: Exception) {
            // Silent fail
        }

        // Update WiFi status
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            isWifiConnected =
                capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
        } catch (e: Exception) {
            // Silent fail
        }

        // Update day of week
        dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
    }

    fun getTimeOfDay(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "morning"
            in 12..17 -> "afternoon"
            in 18..21 -> "evening"
            else -> "night"
        }
    }

    fun getTimeString(): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }

    fun isLateNight(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= 23 || hour <= 5
    }

    fun isWeekend(): Boolean {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY
    }

    fun getBatteryStatus(): String {
        return when {
            batteryLevel < 0 -> "unknown"
            isCharging -> "$batteryLevel% (charging)"
            batteryLevel < 20 -> "$batteryLevel% (low)"
            else -> "$batteryLevel%"
        }
    }

    fun getContextSummary(): String {
        return buildString {
            append("Time: ${getTimeString()} (${getTimeOfDay()})")
            append(", Day: $dayOfWeek")
            if (batteryLevel >= 0) {
                append(", Battery: ${getBatteryStatus()}")
            }
            if (isWifiConnected) {
                append(", WiFi: connected")
            }
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
