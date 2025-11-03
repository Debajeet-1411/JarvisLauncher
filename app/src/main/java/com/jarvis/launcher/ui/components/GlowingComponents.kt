package com.jarvis.launcher.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.launcher.ui.theme.JarvisColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Glowing Card with neon border and glass morphism effect
 */
@Composable
fun GlowingCard(
    modifier: Modifier = Modifier,
    glowColor: Color = JarvisColors.neonCyan,
    glowIntensity: Float = 0.5f,
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = glowIntensity * 0.3f,
        targetValue = glowIntensity,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .drawBehind {
                // Outer glow effect
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.maxDimension * 0.8f
                    ),
                    size = size
                )
            }
            .background(
                color = JarvisColors.glassDark,
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.8f),
                        glowColor.copy(alpha = 0.3f),
                        glowColor.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(16.dp)
    ) {
        content()
    }
}

/**
 * Holographic circle with rotating border (like RAM display in reference)
 */
@Composable
fun HolographicCircle(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    color: Color = JarvisColors.neonCyan,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                // Outer rotating ring
                rotate(rotation) {
                    val ringCount = 3
                    for (i in 0 until ringCount) {
                        val radius = size.toPx() / 2 - (i * 8.dp.toPx())
                        drawCircle(
                            color = color.copy(alpha = 0.3f - i * 0.1f),
                            radius = radius,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Glowing core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    radius = size.toPx() / 2
                )
            }
            .background(
                color = JarvisColors.glassDark,
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        color,
                        Color.Transparent,
                        color
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Neon Search Bar with glow effect
 */
@Composable
fun NeonSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val focusGlow by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = JarvisColors.glassMedium,
                shape = RoundedCornerShape(28.dp)
            )
            .border(
                width = 1.dp,
                color = JarvisColors.neonCyan.copy(alpha = if (focusGlow) 0.8f else 0.3f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.4f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Animated pulse effect for notifications/badges
 */
@Composable
fun PulsingBadge(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = JarvisColors.neonPink
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(20.dp)
            .scale(scale)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0.8f))
                ),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.5f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 9) "9+" else count.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Glowing divider line
 */
@Composable
fun NeonDivider(
    modifier: Modifier = Modifier,
    color: Color = JarvisColors.neonCyan,
    thickness: Dp = 2.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        color.copy(alpha = 0.6f),
                        color,
                        color.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * Shimmer effect for loading states
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val offset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        JarvisColors.neonCyan.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    start = Offset(offset * 1000f, 0f),
                    end = Offset((offset + 1f) * 1000f, 0f)
                )
            )
    )
}

/**
 * Hexagonal border pattern (tech aesthetic)
 */
@Composable
fun HexagonalPattern(
    modifier: Modifier = Modifier,
    color: Color = JarvisColors.neonCyan.copy(alpha = 0.1f)
) {
    Box(
        modifier = modifier.drawBehind {
            val hexSize = 40.dp.toPx()
            val cols = (size.width / hexSize).toInt() + 2
            val rows = (size.height / hexSize).toInt() + 2

            for (row in 0..rows) {
                for (col in 0..cols) {
                    val x = col * hexSize + (if (row % 2 == 0) 0f else hexSize / 2)
                    val y = row * hexSize * 0.866f

                    drawHexagon(
                        center = Offset(x, y),
                        radius = hexSize / 3,
                        color = color
                    )
                }
            }
        }
    )
}

/**
 * Helper function to draw hexagon
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHexagon(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path()
    for (i in 0..6) {
        val angle = Math.toRadians((60 * i).toDouble())
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color = color, style = Stroke(width = 1.dp.toPx()))
}
