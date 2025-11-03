package com.jarvis.launcher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jarvis.launcher.ui.launcher.AppInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore extension
private val Context.categoryDataStore by preferencesDataStore(name = "app_categories")

/**
 * App Category Manager
 *
 * Features:
 * - Automatic app categorization
 * - Custom folder creation
 * - Smart category suggestions
 * - Folder management
 *
 * Day 4: Advanced organization
 */
class AppCategoryManager(private val context: Context) {

    private val gson = Gson()

    companion object {
        private const val CUSTOM_FOLDERS_KEY = "custom_folders"
        private const val APP_CATEGORIES_KEY = "app_categories"
    }

    /**
     * Get category for an app
     */
    fun getCategoryForApp(appInfo: AppInfo): AppCategory {
        val packageName = appInfo.packageName.lowercase()
        val label = appInfo.label.lowercase()

        return when {
            // Social apps
            packageName.contains("whatsapp") || packageName.contains("facebook") ||
                    packageName.contains("instagram") || packageName.contains("twitter") ||
                    packageName.contains("telegram") || packageName.contains("snapchat") ||
                    packageName.contains("tiktok") || label.contains("social") ->
                AppCategory.SOCIAL

            // Communication
            packageName.contains("gmail") || packageName.contains("mail") ||
                    packageName.contains("messages") || packageName.contains("phone") ||
                    packageName.contains("dialer") || packageName.contains("contacts") ->
                AppCategory.COMMUNICATION

            // Entertainment
            packageName.contains("youtube") || packageName.contains("netflix") ||
                    packageName.contains("spotify") || packageName.contains("music") ||
                    packageName.contains("video") || packageName.contains("game") ||
                    packageName.contains("play.games") ->
                AppCategory.ENTERTAINMENT

            // Productivity
            packageName.contains("office") || packageName.contains("docs") ||
                    packageName.contains("sheets") || packageName.contains("slides") ||
                    packageName.contains("drive") || packageName.contains("notes") ||
                    packageName.contains("calendar") || packageName.contains("keep") ->
                AppCategory.PRODUCTIVITY

            // Shopping
            packageName.contains("amazon") || packageName.contains("shop") ||
                    packageName.contains("store") || packageName.contains("cart") ||
                    label.contains("shopping") || label.contains("buy") ->
                AppCategory.SHOPPING

            // Finance
            packageName.contains("bank") || packageName.contains("pay") ||
                    packageName.contains("wallet") || packageName.contains("finance") ||
                    label.contains("payment") || label.contains("banking") ->
                AppCategory.FINANCE

            // Photography
            packageName.contains("camera") || packageName.contains("photo") ||
                    packageName.contains("gallery") || packageName.contains("image") ||
                    packageName.contains("picture") ->
                AppCategory.PHOTOGRAPHY

            // Tools & Utilities
            packageName.contains("settings") || packageName.contains("file") ||
                    packageName.contains("manager") || packageName.contains("clock") ||
                    packageName.contains("calculator") || packageName.contains("flashlight") ->
                AppCategory.TOOLS

            // News & Reading
            packageName.contains("news") || packageName.contains("read") ||
                    packageName.contains("medium") || packageName.contains("kindle") ||
                    label.contains("news") || label.contains("reader") ->
                AppCategory.NEWS

            // Travel
            packageName.contains("maps") || packageName.contains("uber") ||
                    packageName.contains("travel") || packageName.contains("flight") ||
                    packageName.contains("hotel") ->
                AppCategory.TRAVEL

            // Health & Fitness
            packageName.contains("fit") || packageName.contains("health") ||
                    packageName.contains("workout") || packageName.contains("yoga") ||
                    label.contains("fitness") || label.contains("health") ->
                AppCategory.HEALTH

            // System apps
            appInfo.isSystemApp -> AppCategory.SYSTEM

            else -> AppCategory.OTHER
        }
    }

    /**
     * Save custom folders
     */
    suspend fun saveCustomFolders(folders: List<CustomFolder>) {
        val key = stringPreferencesKey(CUSTOM_FOLDERS_KEY)
        context.categoryDataStore.edit { preferences ->
            preferences[key] = gson.toJson(folders)
        }
    }

    /**
     * Load custom folders
     */
    fun loadCustomFolders(): Flow<List<CustomFolder>> {
        val key = stringPreferencesKey(CUSTOM_FOLDERS_KEY)
        return context.categoryDataStore.data.map { preferences ->
            val json = preferences[key]
            if (json != null) {
                try {
                    val type = object : TypeToken<List<CustomFolder>>() {}.type
                    gson.fromJson(json, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    /**
     * Add app to folder
     */
    suspend fun addAppToFolder(folderId: String, packageName: String) {
        val folders = loadCustomFolders().map { it.toMutableList() }
        folders.collect { currentFolders ->
            val folder = currentFolders.find { it.id == folderId }
            if (folder != null) {
                val updatedApps = folder.apps.toMutableList()
                if (!updatedApps.contains(packageName)) {
                    updatedApps.add(packageName)
                    val updatedFolder = folder.copy(apps = updatedApps)
                    val updatedFolders = currentFolders.map {
                        if (it.id == folderId) updatedFolder else it
                    }
                    saveCustomFolders(updatedFolders)
                }
            }
        }
    }

    /**
     * Remove app from folder
     */
    suspend fun removeAppFromFolder(folderId: String, packageName: String) {
        val folders = loadCustomFolders().map { it.toMutableList() }
        folders.collect { currentFolders ->
            val folder = currentFolders.find { it.id == folderId }
            if (folder != null) {
                val updatedApps = folder.apps.toMutableList()
                updatedApps.remove(packageName)
                val updatedFolder = folder.copy(apps = updatedApps)
                val updatedFolders = currentFolders.map {
                    if (it.id == folderId) updatedFolder else it
                }
                saveCustomFolders(updatedFolders)
            }
        }
    }

    /**
     * Create new folder
     */
    suspend fun createFolder(name: String, color: Int): CustomFolder {
        val newFolder = CustomFolder(
            id = System.currentTimeMillis().toString(),
            name = name,
            color = color,
            apps = emptyList()
        )

        val folders = loadCustomFolders().map { it.toMutableList() }
        folders.collect { currentFolders ->
            val updatedFolders = currentFolders + newFolder
            saveCustomFolders(updatedFolders)
        }

        return newFolder
    }

    /**
     * Delete folder
     */
    suspend fun deleteFolder(folderId: String) {
        val folders = loadCustomFolders().map { it.toMutableList() }
        folders.collect { currentFolders ->
            val updatedFolders = currentFolders.filterNot { it.id == folderId }
            saveCustomFolders(updatedFolders)
        }
    }

    /**
     * Get apps grouped by category
     */
    fun groupAppsByCategory(apps: List<AppInfo>): Map<AppCategory, List<AppInfo>> {
        return apps.groupBy { getCategoryForApp(it) }
    }
}

/**
 * App categories
 */
enum class AppCategory(val displayName: String, val icon: String) {
    SOCIAL("Social", "👥"),
    COMMUNICATION("Communication", "💬"),
    ENTERTAINMENT("Entertainment", "🎬"),
    PRODUCTIVITY("Productivity", "📊"),
    SHOPPING("Shopping", "🛒"),
    FINANCE("Finance", "💰"),
    PHOTOGRAPHY("Photography", "📷"),
    TOOLS("Tools", "🔧"),
    NEWS("News", "📰"),
    TRAVEL("Travel", "✈️"),
    HEALTH("Health & Fitness", "💪"),
    SYSTEM("System", "⚙️"),
    OTHER("Other", "📱")
}

/**
 * Custom folder
 */
data class CustomFolder(
    val id: String,
    val name: String,
    val color: Int,
    val apps: List<String> = emptyList(),
    val icon: String = "📁"
)

/**
 * Folder view mode
 */
enum class FolderViewMode {
    GRID,      // Show apps in grid
    LIST,      // Show apps in list
    EXPANDED   // Show folder contents inline
}
