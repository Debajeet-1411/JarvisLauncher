package com.jarvis.launcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.jarvis.launcher.ai.engine.AiEngine
import com.jarvis.launcher.ai.engine.AiMode
import com.jarvis.launcher.data.ConversationRepository
import com.jarvis.launcher.data.CustomFolder
import com.jarvis.launcher.ui.components.*
import com.jarvis.launcher.ui.gestures.GestureCallbacks
import com.jarvis.launcher.ui.gestures.GestureHandler
import com.jarvis.launcher.ui.launcher.*
import com.jarvis.launcher.ui.settings.SettingsDialog
import com.jarvis.launcher.ui.theme.JarvisColors
import com.jarvis.launcher.ui.theme.JarvisLauncherTheme
import com.jarvis.launcher.voice.VoiceService
import com.jarvis.launcher.voice.WakeWordDetector
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * JARVIS Launcher - Main Activity
 * 
 * Futuristic AI-powered launcher with cyberpunk aesthetics
 * 
 * Features:
 * - Futuristic home screen with neon effects
 * - Scroll up to reveal full app drawer
 * - Holographic widgets and glowing cards
 * - AI assistant integration
 * - Voice commands
 * - Smart suggestions
 * - Wake word detection (Day 4)
 * - Gesture controls (Day 4)
 * - App categories (Day 4)
 * - Custom folders (Day 4)
 */
class MainActivity : ComponentActivity() {
    
    private lateinit var voiceService: VoiceService
    private lateinit var wakeWordDetector: WakeWordDetector
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { entry ->
            val permissionName = entry.key
            val isGranted = entry.value
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        voiceService = VoiceService(this)
        wakeWordDetector = WakeWordDetector(this)
        requestPermissions()
        
        setContent {
            JarvisLauncherTheme {
                FuturisticLauncherScreen(
                    voiceService = voiceService,
                    wakeWordDetector = wakeWordDetector
                )
            }
        }
    }
    
    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        voiceService.cleanup()
        wakeWordDetector.cleanup()
    }
}

/**
 * Futuristic Launcher Screen
 * Main home view + scrollable app drawer
 * Day 4: Added category view and folder support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuturisticLauncherScreen(
    voiceService: VoiceService,
    wakeWordDetector: WakeWordDetector,
    viewModel: LauncherViewModel = viewModel()
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    var showAiSheet by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var wakeWordEnabled by remember { mutableStateOf(false) }
    var showWakeWordWarning by remember { mutableStateOf(false) }

    // Day 4: New states
    val showCategories by viewModel.showCategories.collectAsStateWithLifecycle()
    var selectedFolder by remember { mutableStateOf<CustomFolder?>(null) }

    val aiEngine = remember { AiEngine(context) }
    val conversationRepo = remember { ConversationRepository(context) }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val smartSuggestions by viewModel.smartSuggestions.collectAsStateWithLifecycle()
    val mostUsedApps by viewModel.mostUsedApps.collectAsStateWithLifecycle()

    // FIXED: Initialize AI Engine on startup
    LaunchedEffect(Unit) {
        aiEngine.initialize()
    }

    // FIXED: Gesture handling setup
    val gestureHandler = remember { GestureHandler(context) }
    var showSearchFocus by remember { mutableStateOf(false) }

    val gestureCallbacks = remember {
        object : GestureCallbacks {
            override fun onSwipeDown(velocity: Float) {
                // Open notifications
                try {
                    // Try to expand notifications panel
                    @Suppress("DEPRECATION", "WrongConstant")
                    context.getSystemService("statusbar")?.let { service ->
                        service.javaClass
                            .getMethod("expandNotificationsPanel")
                            .invoke(service)
                    }
                } catch (e: Exception) {
                    // Fallback: open notification settings
                    try {
                        val intent =
                            android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (ex: Exception) {
                        // Silent fail
                    }
                }
            }

            override fun onSwipeUp(velocity: Float) {
                // Scroll to app drawer (if not already there)
                scope.launch {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }

            override fun onDoubleTap(offset: androidx.compose.ui.geometry.Offset) {
                // Focus search bar
                showSearchFocus = true
                viewModel.updateSearchQuery("")
            }

            override fun onLongPress(offset: androidx.compose.ui.geometry.Offset) {
                // Toggle category view
                viewModel.toggleCategoryView()
            }
        }
    }

    LaunchedEffect(Unit) {
        gestureHandler.setCallbacks(gestureCallbacks)
    }

    // Day 4: Wake word detection listener
    LaunchedEffect(wakeWordEnabled) {
        if (wakeWordEnabled) {
            wakeWordDetector.startMonitoring {
                showAiSheet = true
                voiceService.speak("Yes, how can I help?")
            }
        } else {
            wakeWordDetector.stopMonitoring()
        }
    }

    // Main UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        gestureCallbacks.onDoubleTap(offset)
                    },
                    onLongPress = { offset ->
                        gestureCallbacks.onLongPress(offset)
                    }
                )
            }
    ) {
        // Hexagonal tech pattern background
        HexagonalPattern(
            modifier = Modifier.fillMaxSize(),
            color = JarvisColors.neonCyan.copy(alpha = 0.05f)
        )

        // Day 4: Show either category view or normal launcher
        if (showCategories) {
            CategoryView(
                viewModel = viewModel,
                onAppClick = { app ->
                    viewModel.launchApp(context, app)
                },
                onAppLongClick = { app ->
                    viewModel.toggleFavorite(context, app.packageName)
                }
            )
        } else {
            // Main scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                JarvisColors.spaceBlack,
                                JarvisColors.deepNavy,
                                JarvisColors.darkSlate
                            )
                        )
                    )
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // ============ HOME SCREEN START ============

                // "Home" title with neon underline
                Column {
                    Text(
                        text = "Home",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    NeonDivider(
                        modifier = Modifier.width(80.dp),
                        color = JarvisColors.neonCyan,
                        thickness = 2.dp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Futuristic search bar
                FuturisticSearchBar(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = "Looking For Something?"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Floating icon row (Top favorite apps)
                FloatingIconRow(
                    icons = listOf(
                        Icons.Default.Apps to JarvisColors.neonCyan,
                        Icons.Default.Star to JarvisColors.neonBlue,
                        Icons.Default.Favorite to JarvisColors.neonGreen,
                        Icons.Default.MoreHoriz to JarvisColors.neonPurple
                    ),
                    onIconClick = { index ->
                        // Launch respective apps
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Time & Info Widget
                TimeInfoWidget()

                Spacer(modifier = Modifier.height(16.dp))

                // App shortcut cards grid (Phone, Messages, Mail, Calculator)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppShortcutCard(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        notificationCount = 3,
                        color = JarvisColors.neonCyan,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            // Launch phone app
                        }
                    )
                    AppShortcutCard(
                        icon = Icons.Default.Message,
                        label = "Messages",
                        notificationCount = 10,
                        color = JarvisColors.neonPurple,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            // Launch messages app
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppShortcutCard(
                        icon = Icons.Default.Email,
                        label = "Mail",
                        notificationCount = 0,
                        color = JarvisColors.neonBlue,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            // Launch mail app
                        }
                    )
                    AppShortcutCard(
                        icon = Icons.Default.Calculate,
                        label = "Calculator",
                        notificationCount = 0,
                        color = JarvisColors.neonGreen,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            // Launch calculator
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Scroll up indicator
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll up for more",
                        tint = JarvisColors.neonCyan.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Scroll up for app drawer",
                        fontSize = 12.sp,
                        color = JarvisColors.neonCyan.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ============ APP DRAWER START ============

                // System Stats Widget (Holographic RAM display)
                SystemStatsWidget()

                Spacer(modifier = Modifier.height(24.dp))

                // Day 4: Custom Folders
                FolderRow(
                    viewModel = viewModel,
                    onFolderClick = { folder ->
                        selectedFolder = folder
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Actions Panel (existing)
                QuickActionsPanel()

                Spacer(modifier = Modifier.height(24.dp))

                // Smart Suggestions
                if (searchQuery.isEmpty()) {
                    if (smartSuggestions.isNotEmpty()) {
                        SuggestionsRow(
                            title = "For You",
                            apps = smartSuggestions,
                            onAppClick = { app -> viewModel.launchApp(context, app) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (mostUsedApps.isNotEmpty()) {
                        SuggestionsRow(
                            title = "Most Used",
                            apps = mostUsedApps,
                            onAppClick = { app -> viewModel.launchApp(context, app) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // All Apps section header with battery + category toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "All Apps",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            NeonDivider(
                                modifier = Modifier.width(80.dp),
                                color = JarvisColors.neonCyan,
                                thickness = 2.dp
                            )
                        }

                        // Day 4: Category view toggle
                        IconButton(
                            onClick = { viewModel.toggleCategoryView() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Categories",
                                tint = JarvisColors.neonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    BatteryWidget()
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App Grid with glowing effects
                Box(modifier = Modifier.height(800.dp)) {
                    FuturisticAppGrid(viewModel = viewModel)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
        
        // Floating AI Assistant Button with glow
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            // Glow effect behind FAB
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .blur(30.dp)
                    .background(
                        JarvisColors.neonCyan.copy(alpha = 0.4f),
                        CircleShape
                    )
            )
            
            FloatingActionButton(
                onClick = { showAiSheet = true },
                containerColor = JarvisColors.neonCyan,
                contentColor = JarvisColors.spaceBlack,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "JARVIS AI",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Top bar with settings and wake word toggle
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Day 4: Wake word toggle
            IconButton(
                onClick = {
                    if (!wakeWordEnabled) {
                        showWakeWordWarning = true
                    } else {
                        wakeWordEnabled = !wakeWordEnabled
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (wakeWordEnabled) JarvisColors.neonCyan.copy(alpha = 0.3f)
                        else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (wakeWordEnabled) Icons.Default.RecordVoiceOver
                    else Icons.Default.VoiceOverOff,
                    contentDescription = "Wake Word",
                    tint = if (wakeWordEnabled) JarvisColors.neonCyan
                    else JarvisColors.neonCyan.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = { showSettings = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = JarvisColors.neonCyan.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // AI Chat Bottom Sheet
        if (showAiSheet) {
            FuturisticAiChatSheet(
                sheetState = sheetState,
                aiEngine = aiEngine,
                voiceService = voiceService,
                conversationRepo = conversationRepo,
                onDismiss = { showAiSheet = false },
                onLaunchApp = { packageName ->
                    viewModel.launchAppByPackage(context, packageName)
                }
            )
        }

        // Settings Dialog
        if (showSettings) {
            SettingsDialog(
                aiEngine = aiEngine,
                onDismiss = { showSettings = false }
            )
        }

        // Wake Word Warning Dialog
        if (showWakeWordWarning) {
            AlertDialog(
                onDismissRequest = { showWakeWordWarning = false },
                containerColor = JarvisColors.darkSlate,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = JarvisColors.neonPink
                        )
                        Text(
                            text = "⚠️ Experimental Feature",
                            color = Color.White
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Wake Word Detection is currently experimental and has limitations:",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "• NOT real 'Hey JARVIS' keyword recognition",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "• Uses simplified energy-based detection",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "• May trigger on loud noises",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "• Higher battery usage",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "For true wake word detection, future updates will integrate ML-based keyword spotting.",
                            color = JarvisColors.neonCyan.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            wakeWordEnabled = true
                            showWakeWordWarning = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisColors.neonCyan
                        )
                    ) {
                        Text("I Understand, Enable")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showWakeWordWarning = false },
                        border = BorderStroke(1.dp, JarvisColors.neonCyan.copy(alpha = 0.5f))
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }

        // Day 4: Folder content sheet
        selectedFolder?.let { folder ->
            FolderContentSheet(
                folder = folder,
                viewModel = viewModel,
                onDismiss = { selectedFolder = null },
                onAppClick = { app ->
                    viewModel.launchApp(context, app)
                    selectedFolder = null
                }
            )
        }
    }
}

/**
 * Futuristic Search Bar (matches reference UI)
 * FIXED: Now actually accepts input and filters apps!
 */
@Composable
fun FuturisticSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(JarvisColors.glassMedium)
            .border(
                width = 1.dp,
                color = JarvisColors.neonCyan.copy(alpha = 0.4f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 16.sp
            ),
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(JarvisColors.neonCyan),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Clear button when text is entered
        if (value.isNotEmpty()) {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = JarvisColors.neonCyan.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = JarvisColors.neonCyan,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Futuristic App Grid with glowing icons
 */
@Composable
fun FuturisticAppGrid(
    viewModel: LauncherViewModel = viewModel()
) {
    val context = LocalContext.current
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadApps(context)
    }
    
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            HolographicCircle(size = 100.dp) {
                Text("Loading", color = JarvisColors.neonCyan)
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                FuturisticAppIcon(
                    app = app,
                    isFavorite = app.packageName in favorites,
                    onClick = { viewModel.launchApp(context, app) },
                    onLongClick = { viewModel.toggleFavorite(context, app.packageName) }
                )
            }
        }
    }
}

/**
 * Futuristic App Icon with glow
 */
@Composable
fun FuturisticAppIcon(
    app: AppInfo,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing icon container
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isFavorite) JarvisColors.neonCyan.copy(alpha = 0.2f)
                            else Color.Transparent,
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (isFavorite)
                        JarvisColors.neonCyan.copy(alpha = 0.6f)
                    else
                        JarvisColors.glassDark,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(10.dp)
        ) {
            DrawableImage(
                drawable = app.icon,
                contentDescription = app.label,
                modifier = Modifier.fillMaxSize()
            )
            
            // Favorite star
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = JarvisColors.neonCyan,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp)
                        .offset(x = 4.dp, y = (-4).dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // App name
        Text(
            text = app.label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Futuristic AI Chat Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuturisticAiChatSheet(
    sheetState: SheetState,
    aiEngine: AiEngine,
    voiceService: VoiceService,
    conversationRepo: ConversationRepository,
    onDismiss: () -> Unit,
    onLaunchApp: (String) -> Unit
) {
    val context = LocalContext.current
    var userInput by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var currentMode by remember { mutableStateOf(AiMode.LOCAL) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isAnalyzingImage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Image picker launcher
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedImageUri = uri
    }

    val persistedHistory by conversationRepo.loadConversation()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var chatHistory by remember { mutableStateOf(persistedHistory) }
    val chatScrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        currentMode = aiEngine.getMode()
    }

    LaunchedEffect(persistedHistory) {
        if (chatHistory.isEmpty() && persistedHistory.isNotEmpty()) {
            chatHistory = persistedHistory
        }
    }

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            chatScrollState.animateScrollTo(chatScrollState.maxValue)
        }
    }

    LaunchedEffect(isListening) {
        if (isListening) {
            voiceService.startListening { recognizedText ->
                userInput = recognizedText
                isListening = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                Column {
                    Text(
                        text = "JARVIS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisColors.neonCyan
                    )
                    Text(
                        text = "AI Assistant",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                conversationRepo.clearConversation()
                                chatHistory = emptyList()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = JarvisColors.neonPink
                        )
                    }
                    
                    Text(
                        text = if (currentMode == AiMode.LOCAL) "Local" else "Cloud",
                        fontSize = 11.sp,
                        color = JarvisColors.neonCyan
                    )
                    Switch(
                        checked = currentMode == AiMode.CLOUD,
                        onCheckedChange = { isCloud ->
                            scope.launch {
                                currentMode = if (isCloud) AiMode.CLOUD else AiMode.LOCAL
                                aiEngine.setMode(currentMode)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisColors.neonCyan,
                            checkedTrackColor = JarvisColors.neonCyan.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            NeonDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Chat history
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(chatScrollState)
            ) {
                if (chatHistory.isEmpty()) {
                    GlowingCard(
                        glowColor = JarvisColors.neonCyan,
                        cornerRadius = 16.dp
                    ) {
                        Text(
                            text = "Hello! I'm JARVIS, your AI assistant. How can I help you today?",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    chatHistory.takeLast(100).forEach { message ->
                        ChatBubble(message)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Show indicator if messages were trimmed
                    if (chatHistory.size > 100) {
                        Text(
                            text = "${chatHistory.size - 100} older messages hidden",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // FIXED: Image preview if selected
            selectedImageUri?.let { uri ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = JarvisColors.glassMedium
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📷 Image Selected",
                                color = JarvisColors.neonCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = JarvisColors.neonPink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Image preview
                        androidx.compose.foundation.Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = "Selected image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )

                        if (isAnalyzingImage) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = JarvisColors.neonCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Analyzing...",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    placeholder = { Text("Ask JARVIS...", color = Color.White.copy(0.5f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = JarvisColors.glassMedium,
                        unfocusedContainerColor = JarvisColors.glassMedium,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = JarvisColors.neonCyan,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                
                IconButton(
                    onClick = { isListening = !isListening },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) JarvisColors.neonCyan
                            else JarvisColors.glassMedium
                        )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = if (isListening) JarvisColors.spaceBlack else Color.White
                    )
                }
                
                IconButton(
                    onClick = {
                        if (userInput.isNotBlank() || selectedImageUri != null) {
                            val currentInput = userInput
                            val imageUri = selectedImageUri
                            userInput = ""
                            selectedImageUri = null

                            scope.launch {
                                try {
                                    // Handle image analysis if image is selected
                                    if (imageUri != null && currentMode == AiMode.CLOUD) {
                                        isAnalyzingImage = true

                                        val userMessage = ChatMessage(
                                            text = currentInput.ifBlank { "Analyze this image" },
                                            isUser = true,
                                            imageUri = imageUri.toString()
                                        )
                                        chatHistory = (chatHistory + userMessage).takeLast(100)
                                        conversationRepo.addMessage(userMessage)

                                        // Convert URI to Bitmap
                                        val bitmap =
                                            android.provider.MediaStore.Images.Media.getBitmap(
                                                context.contentResolver,
                                                imageUri
                                            )

                                        // Analyze with vision API
                                        val visionClient =
                                            com.jarvis.launcher.ai.engine.VisionClient(context)
                                        val prompt = currentInput.ifBlank {
                                            com.jarvis.launcher.ai.engine.VisionClient.VisionPrompts.DESCRIBE
                                        }

                                        val response = when (aiEngine.getCloudProvider()) {
                                            com.jarvis.launcher.ai.engine.CloudProvider.OPENAI -> {
                                                val apiKey =
                                                    aiEngine.getApiKey(com.jarvis.launcher.ai.engine.CloudProvider.OPENAI)
                                                        ?: ""
                                                visionClient.analyzeWithOpenAI(
                                                    bitmap,
                                                    prompt,
                                                    apiKey
                                                )
                                            }

                                            com.jarvis.launcher.ai.engine.CloudProvider.GEMINI -> {
                                                val apiKey =
                                                    aiEngine.getApiKey(com.jarvis.launcher.ai.engine.CloudProvider.GEMINI)
                                                        ?: ""
                                                visionClient.analyzeWithGemini(
                                                    bitmap,
                                                    prompt,
                                                    apiKey
                                                )
                                            }
                                        }

                                        val aiMessage = ChatMessage("🖼️ $response", false)
                                        chatHistory = (chatHistory + aiMessage).takeLast(100)
                                        conversationRepo.addMessage(aiMessage)

                                        isAnalyzingImage = false
                                    }
                                    // Regular text message
                                    else if (currentInput.isNotBlank()) {
                                        val userMessage = ChatMessage(currentInput, true)
                                        chatHistory = (chatHistory + userMessage).takeLast(100)
                                        conversationRepo.addMessage(userMessage)

                                        val response =
                                            aiEngine.processCommand(currentInput, chatHistory)
                                        val aiMessage = ChatMessage(response.message, false)
                                        chatHistory = (chatHistory + aiMessage).takeLast(100)
                                        conversationRepo.addMessage(aiMessage)

                                        response.action?.let { action ->
                                            when (action.type) {
                                                "launch_app" -> {
                                                    action.data["packageName"]?.let { pkg ->
                                                        onLaunchApp(pkg)
                                                    }
                                                }
                                                "device_control" -> {
                                                    // Handle device control actions
                                                    action.data["action"]?.let { deviceAction ->
                                                        val deviceController =
                                                            com.jarvis.launcher.automation.DeviceController(
                                                                context
                                                            )
                                                        deviceController.executeAction(deviceAction)
                                                    }
                                                }
                                                "speak" -> {
                                                    voiceService.speak(response.message)
                                                }
                                            }
                                        }
                                    }
                                    // Vision not available in local mode
                                    else if (imageUri != null && currentMode == AiMode.LOCAL) {
                                        val errorMsg = ChatMessage(
                                            "📷 Vision analysis requires Cloud mode. Please switch to Cloud mode and ensure you have an API key configured.",
                                            false
                                        )
                                        chatHistory = (chatHistory + errorMsg).takeLast(100)
                                        conversationRepo.addMessage(errorMsg)
                                    }
                                } catch (e: Exception) {
                                    val errorMessage = ChatMessage("Error: ${e.message}", false)
                                    chatHistory = (chatHistory + errorMessage).takeLast(100)
                                    conversationRepo.addMessage(errorMessage)
                                    isAnalyzingImage = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(JarvisColors.neonCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = JarvisColors.spaceBlack
                    )
                }

                IconButton(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(JarvisColors.neonCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Add Photo",
                        tint = JarvisColors.spaceBlack
                    )
                }
            }
        }
    }
}

/**
 * Chat message bubble
 */
@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (message.isUser)
                        JarvisColors.neonCyan
                    else
                        JarvisColors.glassMedium
                )
                .border(
                    width = 1.dp,
                    color = if (message.isUser)
                        JarvisColors.neonCyan
                    else
                        JarvisColors.neonCyan.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show image if present
                message.imageUri?.let { uri ->
                    androidx.compose.foundation.Image(
                        painter = rememberAsyncImagePainter(android.net.Uri.parse(uri)),
                        contentDescription = "Message image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                Text(
                    text = message.text,
                    color = if (message.isUser) JarvisColors.spaceBlack else Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null
)

fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    return sdf.format(Date())
}
