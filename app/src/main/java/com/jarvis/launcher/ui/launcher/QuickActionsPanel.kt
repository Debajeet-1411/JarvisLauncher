package com.jarvis.launcher.ui.launcher

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.launcher.automation.DeviceController

/**
 * Quick Actions Panel - provides shortcuts for common device controls
 *
 * Features:
 * - WiFi toggle
 * - Bluetooth toggle
 * - Flashlight toggle
 * - Settings access
 * - Volume control
 */
@Composable
fun QuickActionsPanel(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceController = remember { DeviceController(context) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Quick Actions",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(getQuickActions()) { action ->
                QuickActionButton(
                    action = action,
                    onClick = {
                        when (action.id) {
                            "wifi" -> deviceController.toggleWifi(true)
                            "bluetooth" -> deviceController.toggleBluetooth(true)
                            "flashlight" -> {
                                // Toggle flashlight state
                                deviceController.toggleFlashlight(true)
                            }

                            "settings" -> deviceController.openSettings()
                            "battery" -> {
                                // Show battery info
                                val batteryLevel = deviceController.getBatteryLevel()
                                // TODO: Show toast or dialog with battery info
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Quick action button
 */
@Composable
fun QuickActionButton(
    action: QuickAction,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable {
                isPressed = true
                onClick()
            }
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.label,
            tint = action.color,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = action.label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1
        )
    }

    // Reset press state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }
}

/**
 * Quick action data class
 */
data class QuickAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * Get list of quick actions
 */
fun getQuickActions(): List<QuickAction> {
    return listOf(
        QuickAction(
            id = "wifi",
            label = "WiFi",
            icon = Icons.Default.Wifi,
            color = Color(0xFF4FC3F7)
        ),
        QuickAction(
            id = "bluetooth",
            label = "Bluetooth",
            icon = Icons.Default.Bluetooth,
            color = Color(0xFF64B5F6)
        ),
        QuickAction(
            id = "flashlight",
            label = "Flashlight",
            icon = Icons.Default.FlashOn,
            color = Color(0xFFFFF176)
        ),
        QuickAction(
            id = "settings",
            label = "Settings",
            icon = Icons.Default.Settings,
            color = Color(0xFF90A4AE)
        ),
        QuickAction(
            id = "battery",
            label = "Battery",
            icon = Icons.Default.BatteryFull,
            color = Color(0xFF81C784)
        )
    )
}
