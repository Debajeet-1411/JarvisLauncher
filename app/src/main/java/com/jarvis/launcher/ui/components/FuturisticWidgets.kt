package com.jarvis.launcher.ui.components

import android.os.BatteryManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.launcher.ui.theme.JarvisColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * System stats widget with holographic circle (RAM/Storage display)
 */
@Composable
fun SystemStatsWidget(
    modifier: Modifier = Modifier,
    usedRam: Long = 2564,
    totalRam: Long = 8192
) {
    val percentage = (usedRam.toFloat() / totalRam.toFloat())

    GlowingCard(
        modifier = modifier,
        glowColor = JarvisColors.neonCyan,
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Holographic RAM display
            HolographicCircle(
                size = 100.dp,
                color = JarvisColors.neonCyan
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${usedRam}MB",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisColors.neonCyan
                    )
                    Text(
                        text = "RAM",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Quick action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Speed,
                    label = "SPEED TEST",
                    color = JarvisColors.neonBlue
                )
                QuickActionButton(
                    icon = Icons.Default.Bolt,
                    label = "BOOST",
                    color = JarvisColors.neonGreen
                )
                QuickActionButton(
                    icon = Icons.Default.Edit,
                    label = "EDIT MODE",
                    color = JarvisColors.neonPurple
                )
            }
        }
    }
}

/**
 * Quick action button with neon border
 */
@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(JarvisColors.glassDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Time & Info Widget (like the reference's time card)
 */
@Composable
fun TimeInfoWidget(
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(getCurrentTimeFormatted()) }
    var currentDate by remember { mutableStateOf(getCurrentDateInfo()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = getCurrentTimeFormatted()
            currentDate = getCurrentDateInfo()
        }
    }

    GlowingCard(
        modifier = modifier,
        glowColor = JarvisColors.neonBlue,
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "It's $currentTime",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentDate.dayOfWeek,
                fontSize = 14.sp,
                color = JarvisColors.neonCyan
            )
            Text(
                text = currentDate.fullDate,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = currentDate.weather,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            NeonDivider(color = JarvisColors.neonBlue.copy(alpha = 0.3f))
        }
    }
}

/**
 * App shortcut card with notification badge (like reference's Phone/Messages cards)
 */
@Composable
fun AppShortcutCard(
    icon: ImageVector,
    label: String,
    notificationCount: Int = 0,
    color: Color = JarvisColors.neonCyan,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        JarvisColors.glassMedium,
                        JarvisColors.glassDark
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Notification badge
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    PulsingBadge(
                        count = notificationCount,
                        color = JarvisColors.neonPink
                    )
                }
            }

            // Icon and label
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Battery Widget with circular progress
 */
@Composable
fun BatteryWidget(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val batteryManager =
        context.getSystemService(android.content.Context.BATTERY_SERVICE) as? BatteryManager
    val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
    val isCharging = batteryManager?.isCharging ?: false

    val infiniteTransition = rememberInfiniteTransition(label = "battery_anim")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = batteryLevel / 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .drawBehind {
                // Background circle
                drawCircle(
                    color = JarvisColors.glassMedium,
                    style = Stroke(width = 8.dp.toPx())
                )

                // Progress arc
                val sweepAngle = animatedProgress * 360f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            if (isCharging) JarvisColors.neonGreen else JarvisColors.neonBlue,
                            if (isCharging) JarvisColors.neonCyan else JarvisColors.neonPurple
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$batteryLevel%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (isCharging) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = "Charging",
                    tint = JarvisColors.neonGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Floating icon row (like the reference's top icon row)
 */
@Composable
fun FloatingIconRow(
    modifier: Modifier = Modifier,
    icons: List<Pair<ImageVector, Color>> = emptyList(),
    onIconClick: (Int) -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        icons.forEachIndexed { index, (icon, color) ->
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.3f),
                                JarvisColors.glassDark
                            )
                        )
                    )
                    .clickable { onIconClick(index) }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// Helper data class for date info
data class DateInfo(
    val dayOfWeek: String,
    val fullDate: String,
    val weather: String
)

// Helper functions
private fun getCurrentTimeFormatted(): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date())
}

private fun getCurrentDateInfo(): DateInfo {
    val calendar = Calendar.getInstance()
    val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
    val fullDate = "Date is ${calendar.get(Calendar.DAY_OF_MONTH)} of ${
        SimpleDateFormat("MMMM, yyyy", Locale.getDefault()).format(calendar.time)
    }"
    return DateInfo(
        dayOfWeek = "Today is $dayOfWeek.",
        fullDate = fullDate,
        weather = "It seems to be Clear outside!"
    )
}
