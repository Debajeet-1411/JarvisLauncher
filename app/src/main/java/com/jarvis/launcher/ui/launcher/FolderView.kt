package com.jarvis.launcher.ui.launcher

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarvis.launcher.data.CustomFolder
import com.jarvis.launcher.ui.components.GlowingCard
import com.jarvis.launcher.ui.components.NeonDivider
import com.jarvis.launcher.ui.theme.JarvisColors

/**
 * Folder Row - Horizontal row of custom folders
 * Day 4: Folder management
 */
@Composable
fun FolderRow(
    viewModel: LauncherViewModel = viewModel(),
    onFolderClick: (CustomFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    val folders by viewModel.customFolders.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    if (folders.isNotEmpty() || true) { // Always show the row with create button
        Column(modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Folders",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                TextButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Folder",
                        tint = JarvisColors.neonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New",
                        color = JarvisColors.neonCyan,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(folders) { folder ->
                    FolderCard(
                        folder = folder,
                        onClick = { onFolderClick(folder) }
                    )
                }
            }
        }

        if (showCreateDialog) {
            CreateFolderDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, color ->
                    viewModel.createFolder(name, color)
                    showCreateDialog = false
                }
            )
        }
    }
}

/**
 * Folder Card - Individual folder display
 */
@Composable
fun FolderCard(
    folder: CustomFolder,
    onClick: () -> Unit
) {
    val folderColor = Color(folder.color)

    GlowingCard(
        glowColor = folderColor,
        cornerRadius = 16.dp,
        modifier = Modifier
            .width(120.dp)
            .height(100.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = folder.icon,
                    fontSize = 32.sp
                )

                // App count badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(folderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = folder.apps.size.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisColors.spaceBlack
                    )
                }
            }

            Text(
                text = folder.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Create Folder Dialog
 */
@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(JarvisColors.neonCyan) }

    val colorOptions = listOf(
        JarvisColors.neonCyan,
        JarvisColors.neonBlue,
        JarvisColors.neonPurple,
        JarvisColors.neonPink,
        JarvisColors.neonGreen,
        Color(0xFF00FF88),
        Color(0xFFFFAA00),
        Color(0xFF00AAFF)
    )

    Dialog(onDismissRequest = onDismiss) {
        GlowingCard(
            glowColor = JarvisColors.neonCyan,
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Create Folder",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Folder name input
                TextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("Folder name", color = Color.White.copy(0.5f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = JarvisColors.glassMedium,
                        unfocusedContainerColor = JarvisColors.glassMedium,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = JarvisColors.neonCyan,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Color picker
                Text(
                    text = "Choose color",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colorOptions) { color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (color == selectedColor) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White.copy(0.7f))
                    }

                    Button(
                        onClick = {
                            if (folderName.isNotBlank()) {
                                onCreate(folderName, selectedColor.hashCode())
                            }
                        },
                        enabled = folderName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = selectedColor,
                            contentColor = JarvisColors.spaceBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Folder Content View - Shows apps inside a folder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContentSheet(
    folder: CustomFolder,
    viewModel: LauncherViewModel = viewModel(),
    onDismiss: () -> Unit,
    onAppClick: (AppInfo) -> Unit
) {
    val folderApps = viewModel.getAppsInFolder(folder.id)
    val folderColor = Color(folder.color)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = JarvisColors.darkSlate,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = folder.icon,
                        fontSize = 32.sp
                    )

                    Column {
                        Text(
                            text = folder.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = folderColor
                        )
                        Text(
                            text = "${folderApps.size} apps",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        viewModel.deleteFolder(folder.id)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Folder",
                        tint = JarvisColors.neonPink
                    )
                }
            }

            NeonDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = folderColor.copy(alpha = 0.5f)
            )

            // Apps grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(folderApps, key = { it.packageName }) { app ->
                    CategoryAppIcon(
                        app = app,
                        categoryColor = folderColor,
                        onClick = {
                            onAppClick(app)
                            onDismiss()
                        },
                        onLongClick = {
                            viewModel.removeAppFromFolder(folder.id, app.packageName)
                        }
                    )
                }
            }

            if (folderApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📭",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Empty folder",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
