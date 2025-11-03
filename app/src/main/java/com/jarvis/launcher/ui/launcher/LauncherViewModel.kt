package com.jarvis.launcher.ui.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.launcher.data.AppCategoryManager
import com.jarvis.launcher.data.AppCategory
import com.jarvis.launcher.data.AppUsageTracker
import com.jarvis.launcher.data.CustomFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * ViewModel for Launcher Screen
 * 
 * Manages:
 * - Loading installed applications
 * - Filtering apps based on search
 * - Launching apps
 * - Opening app settings
 * - App usage tracking
 * - Favorites management
 * - Smart suggestions
 * - App categories (Day 4)
 * - Folder management (Day 4)
 */
class LauncherViewModel : ViewModel() {
    
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()
    
    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _smartSuggestions = MutableStateFlow<List<AppInfo>>(emptyList())
    val smartSuggestions: StateFlow<List<AppInfo>> = _smartSuggestions.asStateFlow()

    private val _mostUsedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val mostUsedApps: StateFlow<List<AppInfo>> = _mostUsedApps.asStateFlow()

    // Day 4: Categories and folders
    private val _categorizedApps = MutableStateFlow<Map<AppCategory, List<AppInfo>>>(emptyMap())
    val categorizedApps: StateFlow<Map<AppCategory, List<AppInfo>>> = _categorizedApps.asStateFlow()

    private val _customFolders = MutableStateFlow<List<CustomFolder>>(emptyList())
    val customFolders: StateFlow<List<CustomFolder>> = _customFolders.asStateFlow()

    private val _showCategories = MutableStateFlow(false)
    val showCategories: StateFlow<Boolean> = _showCategories.asStateFlow()

    private var usageTracker: AppUsageTracker? = null
    private var categoryManager: AppCategoryManager? = null

    /**
     * Load all installed applications with launcher intent
     */
    fun loadApps(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true

            // Initialize managers
            if (usageTracker == null) {
                usageTracker = AppUsageTracker(context)
            }
            if (categoryManager == null) {
                categoryManager = AppCategoryManager(context)
            }

            val appList = withContext(Dispatchers.IO) {
                val packageManager = context.packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                
                packageManager.queryIntentActivities(mainIntent, 0)
                    .map { resolveInfo ->
                        val appInfo = resolveInfo.activityInfo.applicationInfo
                        AppInfo(
                            label = resolveInfo.loadLabel(packageManager).toString(),
                            packageName = resolveInfo.activityInfo.packageName,
                            icon = resolveInfo.loadIcon(packageManager),
                            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        )
                    }
                    .sortedBy { it.label.lowercase() }
            }
            
            _apps.value = appList
            _filteredApps.value = appList
            _isLoading.value = false

            // Load all additional data
            loadFavorites()
            loadSmartSuggestions(context)
            loadMostUsedApps(context)
            loadCategorizedApps()
            loadCustomFolders()
        }
    }
    
    /**
     * Load favorite apps
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            usageTracker?.getFavorites()?.collect { favorites ->
                _favorites.value = favorites
            }
        }
    }

    /**
     * Load smart suggestions based on time and usage
     */
    private fun loadSmartSuggestions(context: Context) {
        viewModelScope.launch {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val suggestedPackages = usageTracker?.getSmartSuggestions(currentHour, 6) ?: emptyList()

            val suggestedApps = _apps.value.filter { it.packageName in suggestedPackages }
            _smartSuggestions.value = suggestedApps
        }
    }

    /**
     * Load most used apps
     */
    private fun loadMostUsedApps(context: Context) {
        viewModelScope.launch {
            val mostUsed = usageTracker?.getMostUsedApps(6) ?: emptyList()
            val mostUsedPackages = mostUsed.map { it.first }

            val mostUsedApps = _apps.value.filter { it.packageName in mostUsedPackages }
            _mostUsedApps.value = mostUsedApps
        }
    }

    /**
     * Day 4: Load categorized apps
     */
    private fun loadCategorizedApps() {
        viewModelScope.launch {
            val grouped = categoryManager?.groupAppsByCategory(_apps.value) ?: emptyMap()
            _categorizedApps.value = grouped
        }
    }

    /**
     * Day 4: Load custom folders
     */
    private fun loadCustomFolders() {
        viewModelScope.launch {
            categoryManager?.loadCustomFolders()?.collect { folders ->
                _customFolders.value = folders
            }
        }
    }

    /**
     * Day 4: Toggle category view
     */
    fun toggleCategoryView() {
        _showCategories.value = !_showCategories.value
    }

    /**
     * Day 4: Create new folder
     */
    fun createFolder(name: String, color: Int) {
        viewModelScope.launch {
            categoryManager?.createFolder(name, color)
            loadCustomFolders()
        }
    }

    /**
     * Day 4: Delete folder
     */
    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            categoryManager?.deleteFolder(folderId)
            loadCustomFolders()
        }
    }

    /**
     * Day 4: Add app to folder
     */
    fun addAppToFolder(folderId: String, packageName: String) {
        viewModelScope.launch {
            categoryManager?.addAppToFolder(folderId, packageName)
            loadCustomFolders()
        }
    }

    /**
     * Day 4: Remove app from folder
     */
    fun removeAppFromFolder(folderId: String, packageName: String) {
        viewModelScope.launch {
            categoryManager?.removeAppFromFolder(folderId, packageName)
            loadCustomFolders()
        }
    }

    /**
     * Day 4: Get apps in folder
     */
    fun getAppsInFolder(folderId: String): List<AppInfo> {
        val folder = _customFolders.value.find { it.id == folderId } ?: return emptyList()
        return _apps.value.filter { it.packageName in folder.apps }
    }

    /**
     * Update search query and filter apps
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        
        _filteredApps.value = if (query.isBlank()) {
            _apps.value
        } else {
            _apps.value.filter {
                it.label.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
    }
    
    /**
     * Launch an application by AppInfo
     */
    fun launchApp(context: Context, appInfo: AppInfo) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)

                // Record app launch
                viewModelScope.launch {
                    usageTracker?.recordAppLaunch(appInfo.packageName)
                    loadSmartSuggestions(context)
                    loadMostUsedApps(context)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Launch an application by package name (used by AI commands)
     */
    fun launchAppByPackage(context: Context, packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)

                // Record app launch
                viewModelScope.launch {
                    usageTracker?.recordAppLaunch(packageName)
                    loadSmartSuggestions(context)
                    loadMostUsedApps(context)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Toggle app favorite status
     */
    fun toggleFavorite(context: Context, packageName: String) {
        viewModelScope.launch {
            if (packageName in _favorites.value) {
                usageTracker?.removeFromFavorites(packageName)
            } else {
                usageTracker?.addToFavorites(packageName)
            }
            loadFavorites()
        }
    }

    /**
     * Check if app is favorite
     */
    fun isFavorite(packageName: String): Boolean {
        return packageName in _favorites.value
    }

    /**
     * Open app info settings page
     */
    fun openAppInfo(context: Context, appInfo: AppInfo) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${appInfo.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Get app by package name - useful for AI commands
     */
    fun getAppByPackage(packageName: String): AppInfo? {
        return _apps.value.find { it.packageName == packageName }
    }
}
