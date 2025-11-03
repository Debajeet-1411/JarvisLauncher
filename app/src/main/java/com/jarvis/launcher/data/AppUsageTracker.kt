package com.jarvis.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

// DataStore extension for app usage
private val Context.appUsageDataStore by preferencesDataStore(name = "app_usage")

/**
 * App Usage Tracker - tracks and analyzes app usage patterns
 *
 * Features:
 * - Launch count tracking
 * - Last launch time
 * - Most used apps
 * - Context-aware suggestions
 */
class AppUsageTracker(private val context: Context) {

    companion object {
        private const val USAGE_PREFIX = "usage_"
        private const val LAST_LAUNCH_PREFIX = "last_launch_"
        private const val FAVORITES_KEY = "favorites"
    }

    /**
     * Record app launch
     */
    suspend fun recordAppLaunch(packageName: String) {
        val usageKey = longPreferencesKey("${USAGE_PREFIX}${packageName}")
        val lastLaunchKey = longPreferencesKey("${LAST_LAUNCH_PREFIX}${packageName}")

        context.appUsageDataStore.edit { preferences ->
            val currentCount = preferences[usageKey] ?: 0L
            preferences[usageKey] = currentCount + 1
            preferences[lastLaunchKey] = System.currentTimeMillis()
        }
    }

    /**
     * Get app launch count
     */
    fun getAppLaunchCount(packageName: String): Flow<Long> {
        val usageKey = longPreferencesKey("${USAGE_PREFIX}${packageName}")
        return context.appUsageDataStore.data.map { preferences ->
            preferences[usageKey] ?: 0L
        }
    }

    /**
     * Get last launch time
     */
    fun getLastLaunchTime(packageName: String): Flow<Long> {
        val lastLaunchKey = longPreferencesKey("${LAST_LAUNCH_PREFIX}${packageName}")
        return context.appUsageDataStore.data.map { preferences ->
            preferences[lastLaunchKey] ?: 0L
        }
    }

    /**
     * Get most used apps (top N)
     */
    suspend fun getMostUsedApps(limit: Int = 10): List<Pair<String, Long>> {
        val allPreferences = context.appUsageDataStore.data.map { it.asMap() }
        val usageData = mutableListOf<Pair<String, Long>>()

        allPreferences.collect { preferences ->
            preferences.forEach { (key, value) ->
                if (key.name.startsWith(USAGE_PREFIX) && value is Long) {
                    val packageName = key.name.removePrefix(USAGE_PREFIX)
                    usageData.add(packageName to value)
                }
            }
        }

        return usageData.sortedByDescending { it.second }.take(limit)
    }

    /**
     * Add app to favorites
     */
    suspend fun addToFavorites(packageName: String) {
        val favoritesKey = stringPreferencesKey(FAVORITES_KEY)
        context.appUsageDataStore.edit { preferences ->
            val currentFavorites =
                preferences[favoritesKey]?.split(",")?.toMutableSet() ?: mutableSetOf()
            currentFavorites.add(packageName)
            preferences[favoritesKey] = currentFavorites.joinToString(",")
        }
    }

    /**
     * Remove app from favorites
     */
    suspend fun removeFromFavorites(packageName: String) {
        val favoritesKey = stringPreferencesKey(FAVORITES_KEY)
        context.appUsageDataStore.edit { preferences ->
            val currentFavorites =
                preferences[favoritesKey]?.split(",")?.toMutableSet() ?: mutableSetOf()
            currentFavorites.remove(packageName)
            preferences[favoritesKey] =
                currentFavorites.filterNot { it.isBlank() }.joinToString(",")
        }
    }

    /**
     * Get favorite apps
     */
    fun getFavorites(): Flow<Set<String>> {
        val favoritesKey = stringPreferencesKey(FAVORITES_KEY)
        return context.appUsageDataStore.data.map { preferences ->
            preferences[favoritesKey]?.split(",")?.filterNot { it.isBlank() }?.toSet() ?: emptySet()
        }
    }

    /**
     * Check if app is favorite
     */
    fun isFavorite(packageName: String): Flow<Boolean> {
        return getFavorites().map { favorites ->
            packageName in favorites
        }
    }

    /**
     * Get context-aware suggestions based on time and usage
     */
    suspend fun getSmartSuggestions(currentHour: Int, limit: Int = 6): List<String> {
        val allPreferences = context.appUsageDataStore.data.map { it.asMap() }
        val suggestions = mutableListOf<Pair<String, Double>>()

        allPreferences.collect { preferences ->
            preferences.forEach { (key, value) ->
                if (key.name.startsWith(USAGE_PREFIX) && value is Long) {
                    val packageName = key.name.removePrefix(USAGE_PREFIX)
                    val usageCount = value

                    // Calculate score based on usage and recency
                    val lastLaunchKey = longPreferencesKey("${LAST_LAUNCH_PREFIX}${packageName}")
                    val lastLaunch = preferences[lastLaunchKey] as? Long ?: 0L
                    val hoursSinceLastLaunch =
                        TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - lastLaunch)

                    // Scoring algorithm
                    val usageScore = usageCount.toDouble()
                    val recencyScore =
                        if (hoursSinceLastLaunch < 1) 10.0 else 1.0 / (hoursSinceLastLaunch + 1)
                    val totalScore = usageScore * 0.7 + recencyScore * 0.3

                    suggestions.add(packageName to totalScore)
                }
            }
        }

        return suggestions.sortedByDescending { it.second }.take(limit).map { it.first }
    }
}
