package com.jarvis.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jarvis.launcher.ai.engine.AiEngine
import com.jarvis.launcher.ai.engine.CloudProvider
import kotlinx.coroutines.launch

/**
 * Settings Dialog - Configure JARVIS settings
 *
 * Features:
 * - OpenAI API key configuration
 * - Gemini API key configuration
 * - Cloud provider selection
 *
 * Future:
 * - Model selection
 * - Voice settings
 * - Theme customization
 */
@Composable
fun SettingsDialog(
    aiEngine: AiEngine,
    onDismiss: () -> Unit
) {
    var openAiKey by remember { mutableStateOf("") }
    var geminiKey by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf(CloudProvider.OPENAI) }
    var showOpenAiKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Load existing settings
    LaunchedEffect(Unit) {
        val existingOpenAiKey = aiEngine.getApiKey(CloudProvider.OPENAI)
        if (!existingOpenAiKey.isNullOrBlank()) {
            openAiKey = existingOpenAiKey
        }

        val existingGeminiKey = aiEngine.getApiKey(CloudProvider.GEMINI)
        if (!existingGeminiKey.isNullOrBlank()) {
            geminiKey = existingGeminiKey
        }

        selectedProvider = aiEngine.getCloudProvider()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cloud AI Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Provider Selection
                    Text(
                        text = "AI Provider",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Provider tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // OpenAI button
                        Button(
                            onClick = { selectedProvider = CloudProvider.OPENAI },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedProvider == CloudProvider.OPENAI)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "OpenAI",
                                color = Color.White
                            )
                        }

                        // Gemini button
                        Button(
                            onClick = { selectedProvider = CloudProvider.GEMINI },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedProvider == CloudProvider.GEMINI)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Gemini",
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // OpenAI Section
                    if (selectedProvider == CloudProvider.OPENAI) {
                        Text(
                            text = "OpenAI Configuration",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Get your API key from platform.openai.com",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = openAiKey,
                            onValueChange = { openAiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("OpenAI API Key", color = Color.White.copy(0.7f)) },
                            placeholder = { Text("sk-...", color = Color.White.copy(0.5f)) },
                            visualTransformation = if (showOpenAiKey)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showOpenAiKey = !showOpenAiKey }) {
                                    Icon(
                                        imageVector = if (showOpenAiKey)
                                            Icons.Default.Visibility
                                        else
                                            Icons.Default.VisibilityOff,
                                        contentDescription = if (showOpenAiKey) "Hide" else "Show",
                                        tint = Color.White.copy(0.7f)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(0.3f),
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // OpenAI Info card
                        InfoCard(
                            title = "💡 OpenAI GPT-4o-mini",
                            content = """Fast and cost-effective model.
                                |• ~$0.00007 per command
                                |• Excellent natural language understanding
                                |• Function calling support""".trimMargin()
                        )
                    }

                    // Gemini Section
                    if (selectedProvider == CloudProvider.GEMINI) {
                        Text(
                            text = "Google Gemini Configuration",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Get your API key from aistudio.google.com/apikey",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = geminiKey,
                            onValueChange = { geminiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Gemini API Key", color = Color.White.copy(0.7f)) },
                            placeholder = { Text("AIza...", color = Color.White.copy(0.5f)) },
                            visualTransformation = if (showGeminiKey)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                    Icon(
                                        imageVector = if (showGeminiKey)
                                            Icons.Default.Visibility
                                        else
                                            Icons.Default.VisibilityOff,
                                        contentDescription = if (showGeminiKey) "Hide" else "Show",
                                        tint = Color.White.copy(0.7f)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(0.3f),
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Gemini Info card
                        InfoCard(
                            title = "💡 Gemini 2.0 Flash",
                            content = """Fast, powerful, and FREE (generous quota)!
                                |• Free tier: 1500 requests/day
                                |• Latest Google AI technology
                                |• Excellent for app control""".trimMargin()
                        )
                    }

                    // Saved message
                    if (savedMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = savedMessage!!,
                            fontSize = 12.sp,
                            color = if (savedMessage!!.contains("Error"))
                                Color.Red
                            else
                                Color.Green
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isSaving = true
                            try {
                                // Save API key for selected provider
                                when (selectedProvider) {
                                    CloudProvider.OPENAI -> {
                                        if (openAiKey.isNotBlank()) {
                                            aiEngine.saveApiKey(CloudProvider.OPENAI, openAiKey)
                                        }
                                    }

                                    CloudProvider.GEMINI -> {
                                        if (geminiKey.isNotBlank()) {
                                            aiEngine.saveApiKey(CloudProvider.GEMINI, geminiKey)
                                        }
                                    }
                                }

                                // Set cloud provider
                                aiEngine.setCloudProvider(selectedProvider)

                                savedMessage = "✓ Settings saved successfully!"
                            } catch (e: Exception) {
                                savedMessage = "Error saving: ${e.message}"
                            }
                            isSaving = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isSaving && when (selectedProvider) {
                        CloudProvider.OPENAI -> openAiKey.isNotBlank()
                        CloudProvider.GEMINI -> geminiKey.isNotBlank()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Settings", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(title: String, content: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = 16.sp
            )
        }
    }
}
