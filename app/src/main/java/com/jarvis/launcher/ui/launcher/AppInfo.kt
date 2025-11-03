package com.jarvis.launcher.ui.launcher

import android.graphics.drawable.Drawable

/**
 * Represents an installed application
 *
 * @param label The display name of the app
 * @param packageName The unique package identifier
 * @param icon The app's icon drawable
 * @param isSystemApp Whether this is a system/pre-installed app
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val isSystemApp: Boolean = false
)
