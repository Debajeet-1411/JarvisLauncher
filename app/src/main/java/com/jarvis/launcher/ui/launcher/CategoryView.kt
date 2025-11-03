package com.jarvis.launcher.ui.launcher

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarvis.launcher.data.AppCategory
import com.jarvis.launcher.ui.components.GlowingCard
import com.jarvis.launcher.ui.components.NeonDivider
import com.jarvis.launcher.ui.theme.JarvisColors

/**
 * Category View - Shows apps grouped by categories
 * Day 4: Advanced organization
 */
@Composable
fun CategoryView(
    viewModel: LauncherViewModel = viewModel(),
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    val categorizedApps by viewModel.categorizedApps.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Categories",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    NeonDivider(
                        modifier = Modifier.width(100.dp),
                        color = JarvisColors.neonCyan,
                        thickness = 2.dp
                    )
                }

                IconButton(onClick = { viewModel.toggleCategoryView() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = JarvisColors.neonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Display each category
        categorizedApps.forEach { (category, apps) ->
            if (apps.isNotEmpty() && category != AppCategory.SYSTEM) {
                item(key = category) {
                    CategorySection(
                        category = category,
                        apps = apps,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Category Section - Expandable category with apps
 */
@Composable
fun CategorySection(
    category: AppCategory,
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    val categoryColor = getCategoryColor(category)

    GlowingCard(
        glowColor = categoryColor,
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Category header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Emoji icon
                    Text(
                        text = category.icon,
                        fontSize = 32.sp
                    )

                    Column {
                        Text(
                            text = category.displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${apps.size} apps",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = categoryColor
                )
            }

            // Apps grid (expandable)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    NeonDivider(
                        color = categoryColor.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )

                    // Horizontal scrolling app grid
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            CategoryAppIcon(
                                app = app,
                                categoryColor = categoryColor,
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Category App Icon - Smaller icon for category view
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryAppIcon(
    app: AppInfo,
    categoryColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            categoryColor.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .border(
                    width = 1.dp,
                    color = categoryColor.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(8.dp)
        ) {
            DrawableImage(
                drawable = app.icon,
                contentDescription = app.label,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = app.label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.8f),
            maxLines = 1
        )
    }
}

/**
 * Get category color
 */
fun getCategoryColor(category: AppCategory): Color {
    return when (category) {
        AppCategory.SOCIAL -> JarvisColors.neonPurple
        AppCategory.COMMUNICATION -> JarvisColors.neonCyan
        AppCategory.ENTERTAINMENT -> JarvisColors.neonPink
        AppCategory.PRODUCTIVITY -> JarvisColors.neonBlue
        AppCategory.SHOPPING -> JarvisColors.neonGreen
        AppCategory.FINANCE -> Color(0xFF00FF88)
        AppCategory.PHOTOGRAPHY -> Color(0xFFFF00FF)
        AppCategory.TOOLS -> Color(0xFF00DDFF)
        AppCategory.NEWS -> Color(0xFFFFAA00)
        AppCategory.TRAVEL -> Color(0xFF00AAFF)
        AppCategory.HEALTH -> Color(0xFF88FF00)
        AppCategory.SYSTEM -> Color(0xFF888888)
        AppCategory.OTHER -> JarvisColors.neonCyan
    }
}
