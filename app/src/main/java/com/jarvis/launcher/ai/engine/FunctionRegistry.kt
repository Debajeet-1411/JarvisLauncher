package com.jarvis.launcher.ai.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject

/**
 * Function Registry for AI Function Calling
 *
 * Similar to Python LiveKit project's tools system
 * LLM decides which function to call based on user intent
 *
 * Day 5: Proper function calling architecture
 */
class FunctionRegistry(private val context: Context) {

    /**
     * Get all available functions for OpenAI/Gemini
     */
    fun getOpenAIFunctions(): JSONArray {
        return JSONArray().apply {
            // App Management
            put(
                createFunctionSchema(
                    name = "launch_app",
                    description = "Launch an application by name or package. Use this when user wants to open/launch/start any app.",
                    parameters = mapOf(
                        "app_name" to SchemaParam(
                            "string",
                            "Name of the app to launch (e.g., 'Chrome', 'YouTube', 'Settings')",
                            required = true
                        )
                    )
                )
            )

            put(
                createFunctionSchema(
                    name = "search_apps",
                    description = "Search for apps matching a query. Use when user asks 'what apps do I have' or searches for apps.",
                    parameters = mapOf(
                        "query" to SchemaParam("string", "Search query for apps", required = true)
                    )
                )
            )

            // Device Control
            put(
                createFunctionSchema(
                    name = "toggle_wifi",
                    description = "Open WiFi settings to enable/disable WiFi. Use when user wants to turn on/off WiFi.",
                    parameters = mapOf(
                        "action" to SchemaParam("string", "Either 'on' or 'off'", required = true)
                    )
                )
            )

            put(
                createFunctionSchema(
                    name = "toggle_bluetooth",
                    description = "Open Bluetooth settings. Use when user wants to manage Bluetooth.",
                    parameters = mapOf(
                        "action" to SchemaParam("string", "Either 'on' or 'off'", required = true)
                    )
                )
            )

            put(
                createFunctionSchema(
                    name = "toggle_flashlight",
                    description = "Turn flashlight/torch on or off. Use when user asks for flashlight or torch.",
                    parameters = mapOf(
                        "state" to SchemaParam(
                            "boolean",
                            "True to turn on, false to turn off",
                            required = true
                        )
                    )
                )
            )

            put(
                createFunctionSchema(
                    name = "adjust_volume",
                    description = "Adjust device volume. Use when user wants to change volume level.",
                    parameters = mapOf(
                        "level" to SchemaParam(
                            "string",
                            "Volume level: 'high', 'medium', 'low', or 'mute'",
                            required = true
                        )
                    )
                )
            )

            put(
                createFunctionSchema(
                    name = "adjust_brightness",
                    description = "Adjust screen brightness. Use when user wants to change brightness.",
                    parameters = mapOf(
                        "level" to SchemaParam(
                            "string",
                            "Brightness level: 'high', 'medium', 'low'",
                            required = true
                        )
                    )
                )
            )

            // System Info
            put(
                createFunctionSchema(
                    name = "get_battery_info",
                    description = "Get current battery level and charging status. Use when user asks about battery.",
                    parameters = emptyMap()
                )
            )

            put(
                createFunctionSchema(
                    name = "get_time",
                    description = "Get current time. Use when user asks 'what time is it'.",
                    parameters = emptyMap()
                )
            )

            put(
                createFunctionSchema(
                    name = "get_date",
                    description = "Get current date. Use when user asks 'what's the date'.",
                    parameters = emptyMap()
                )
            )

            // Navigation
            put(
                createFunctionSchema(
                    name = "open_url",
                    description = "Open a URL in browser. Use when user wants to visit a website.",
                    parameters = mapOf(
                        "url" to SchemaParam(
                            "string",
                            "The URL to open (must include http:// or https://)",
                            required = true
                        )
                    )
                )
            )

            put(
                createFunctionSchema(
                    name = "make_call",
                    description = "Open phone dialer with a number. Use when user wants to call someone.",
                    parameters = mapOf(
                        "phone_number" to SchemaParam(
                            "string",
                            "Phone number to call",
                            required = true
                        )
                    )
                )
            )

            put(
                createFunctionSchema(
                    name = "send_message",
                    description = "Open messaging app with a message. Use when user wants to send SMS.",
                    parameters = mapOf(
                        "phone_number" to SchemaParam("string", "Phone number", required = true),
                        "message" to SchemaParam("string", "Message text", required = false)
                    )
                )
            )

            // App Management Advanced
            put(
                createFunctionSchema(
                    name = "add_to_favorites",
                    description = "Add an app to favorites. Use when user wants to favorite/pin an app.",
                    parameters = mapOf(
                        "app_name" to SchemaParam(
                            "string",
                            "Name of app to favorite",
                            required = true
                        )
                    )
                )
            )

            put(
                createFunctionSchema(
                    name = "remove_from_favorites",
                    description = "Remove an app from favorites. Use when user wants to unfavorite/unpin an app.",
                    parameters = mapOf(
                        "app_name" to SchemaParam(
                            "string",
                            "Name of app to remove from favorites",
                            required = true
                        )
                    )
                )
            )

            // Memory/Context
            put(
                createFunctionSchema(
                    name = "remember_information",
                    description = "Store information for later recall. Use when user asks you to remember something.",
                    parameters = mapOf(
                        "key" to SchemaParam(
                            "string",
                            "Short identifier for the information",
                            required = true
                        ),
                        "value" to SchemaParam(
                            "string",
                            "The information to remember",
                            required = true
                        )
                    )
                )
            )

            put(
                createFunctionSchema(
                    name = "recall_information",
                    description = "Retrieve previously stored information. Use when user asks 'what did I tell you about X'.",
                    parameters = mapOf(
                        "key" to SchemaParam(
                            "string",
                            "Identifier of information to recall",
                            required = true
                        )
                    )
                )
            )
        }
    }

    /**
     * Get functions for Gemini format
     */
    fun getGeminiFunctions(): JSONArray {
        return JSONArray().apply {
            // Convert OpenAI format to Gemini format
            val openAIFunctions = getOpenAIFunctions()
            for (i in 0 until openAIFunctions.length()) {
                val func = openAIFunctions.getJSONObject(i)
                put(convertToGeminiFormat(func))
            }
        }
    }

    /**
     * Execute a function by name
     */
    suspend fun executeFunction(name: String, arguments: Map<String, Any>): FunctionResult {
        return try {
            when (name) {
                "launch_app" -> launchApp(arguments["app_name"] as? String ?: "")
                "search_apps" -> searchApps(arguments["query"] as? String ?: "")
                "toggle_wifi" -> toggleWifi(arguments["action"] as? String ?: "")
                "toggle_bluetooth" -> toggleBluetooth(arguments["action"] as? String ?: "")
                "toggle_flashlight" -> toggleFlashlight(arguments["state"] as? Boolean ?: false)
                "adjust_volume" -> adjustVolume(arguments["level"] as? String ?: "")
                "adjust_brightness" -> adjustBrightness(arguments["level"] as? String ?: "")
                "get_battery_info" -> getBatteryInfo()
                "get_time" -> getTime()
                "get_date" -> getDate()
                "open_url" -> openUrl(arguments["url"] as? String ?: "")
                "make_call" -> makeCall(arguments["phone_number"] as? String ?: "")
                "send_message" -> sendMessage(
                    arguments["phone_number"] as? String ?: "",
                    arguments["message"] as? String ?: ""
                )

                "add_to_favorites" -> addToFavorites(arguments["app_name"] as? String ?: "")
                "remove_from_favorites" -> removeFromFavorites(
                    arguments["app_name"] as? String ?: ""
                )

                "remember_information" -> rememberInformation(
                    arguments["key"] as? String ?: "",
                    arguments["value"] as? String ?: ""
                )

                "recall_information" -> recallInformation(arguments["key"] as? String ?: "")
                else -> FunctionResult.error("Unknown function: $name")
            }
        } catch (e: Exception) {
            FunctionResult.error("Function '$name' failed: ${e.message}")
        }
    }

    // ============ Function Implementations ============

    private suspend fun launchApp(appName: String): FunctionResult {
        if (appName.isBlank()) {
            return FunctionResult.error("App name is required")
        }

        // Search for app by name
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = packageManager.queryIntentActivities(intent, 0)
        val matchingApp = apps.find {
            it.loadLabel(packageManager).toString().equals(appName, ignoreCase = true)
        }

        if (matchingApp != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(
                matchingApp.activityInfo.packageName
            )
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)

            return FunctionResult.success(
                "Launched ${matchingApp.loadLabel(packageManager)}",
                data = mapOf("package_name" to matchingApp.activityInfo.packageName)
            )
        }

        return FunctionResult.error("App '$appName' not found. Try a different name.")
    }

    private suspend fun searchApps(query: String): FunctionResult {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = packageManager.queryIntentActivities(intent, 0)
        val matches = apps.filter {
            it.loadLabel(packageManager).toString().contains(query, ignoreCase = true)
        }.take(10).map { it.loadLabel(packageManager).toString() }

        return if (matches.isNotEmpty()) {
            FunctionResult.success(
                "Found ${matches.size} apps: ${matches.joinToString(", ")}",
                data = mapOf("apps" to matches)
            )
        } else {
            FunctionResult.error("No apps found matching '$query'")
        }
    }

    private suspend fun toggleWifi(action: String): FunctionResult {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return FunctionResult.success("Opening WiFi settings to $action WiFi")
    }

    private suspend fun toggleBluetooth(action: String): FunctionResult {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return FunctionResult.success("Opening Bluetooth settings to $action Bluetooth")
    }

    private suspend fun toggleFlashlight(state: Boolean): FunctionResult {
        // This requires CameraManager - implement in DeviceController
        return FunctionResult.success(
            if (state) "Turning flashlight on" else "Turning flashlight off",
            data = mapOf("action" to "toggle_flashlight", "state" to state)
        )
    }

    private suspend fun adjustVolume(level: String): FunctionResult {
        return FunctionResult.success(
            "Adjusting volume to $level",
            data = mapOf("action" to "adjust_volume", "level" to level)
        )
    }

    private suspend fun adjustBrightness(level: String): FunctionResult {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return FunctionResult.success("Opening display settings to adjust brightness")
    }

    private suspend fun getBatteryInfo(): FunctionResult {
        val batteryIntent = context.registerReceiver(
            null,
            android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct =
            if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else -1

        val status = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL

        return FunctionResult.success(
            "Battery is at $batteryPct%${if (isCharging) " (charging)" else ""}",
            data = mapOf("level" to batteryPct, "charging" to isCharging)
        )
    }

    private suspend fun getTime(): FunctionResult {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        return FunctionResult.success("Current time is $time")
    }

    private suspend fun getDate(): FunctionResult {
        val date = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
        return FunctionResult.success("Today is $date")
    }

    private suspend fun openUrl(url: String): FunctionResult {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return FunctionResult.error("URL must start with http:// or https://")
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return FunctionResult.success("Opening $url")
    }

    private suspend fun makeCall(phoneNumber: String): FunctionResult {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return FunctionResult.success("Opening dialer with $phoneNumber")
    }

    private suspend fun sendMessage(phoneNumber: String, message: String): FunctionResult {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
        if (message.isNotBlank()) {
            intent.putExtra("sms_body", message)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return FunctionResult.success("Opening messaging app")
    }

    private suspend fun addToFavorites(appName: String): FunctionResult {
        return FunctionResult.success(
            "Added $appName to favorites",
            data = mapOf("action" to "add_favorite", "app_name" to appName)
        )
    }

    private suspend fun removeFromFavorites(appName: String): FunctionResult {
        return FunctionResult.success(
            "Removed $appName from favorites",
            data = mapOf("action" to "remove_favorite", "app_name" to appName)
        )
    }

    private suspend fun rememberInformation(key: String, value: String): FunctionResult {
        // Store in DataStore or shared preferences
        return FunctionResult.success(
            "I'll remember that $key is $value",
            data = mapOf("action" to "remember", "key" to key, "value" to value)
        )
    }

    private suspend fun recallInformation(key: String): FunctionResult {
        // Retrieve from storage
        return FunctionResult.success(
            "I don't have any information stored about $key yet",
            data = mapOf("action" to "recall", "key" to key)
        )
    }

    // ============ Helper Functions ============

    private fun createFunctionSchema(
        name: String,
        description: String,
        parameters: Map<String, SchemaParam>
    ): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("description", description)
            put("parameters", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    parameters.forEach { (paramName, param) ->
                        put(paramName, JSONObject().apply {
                            put("type", param.type)
                            put("description", param.description)
                        })
                    }
                })
                val required = parameters.filter { it.value.required }.keys.toList()
                if (required.isNotEmpty()) {
                    put("required", JSONArray(required))
                }
            })
        }
    }

    private fun convertToGeminiFormat(openAIFunc: JSONObject): JSONObject {
        // Convert OpenAI function schema to Gemini function declaration
        return JSONObject().apply {
            put("name", openAIFunc.getString("name"))
            put("description", openAIFunc.getString("description"))
            put("parameters", openAIFunc.getJSONObject("parameters"))
        }
    }
}

/**
 * Schema parameter definition
 */
data class SchemaParam(
    val type: String,
    val description: String,
    val required: Boolean = true
)

/**
 * Function execution result
 */
sealed class FunctionResult {
    data class Success(
        val message: String,
        val data: Map<String, Any> = emptyMap()
    ) : FunctionResult()

    data class Error(val message: String) : FunctionResult()

    companion object {
        fun success(message: String, data: Map<String, Any> = emptyMap()) =
            Success(message, data)

        fun error(message: String) = Error(message)
    }
}
