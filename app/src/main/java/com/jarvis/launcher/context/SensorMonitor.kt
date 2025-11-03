package com.jarvis.launcher.context

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 * Sensor Monitor - provides context awareness for JARVIS
 *
 * Monitors:
 * - Location (GPS)
 * - Time of day
 * - Day of week
 * - Battery status
 * - Network connectivity
 *
 * TODO Day 2:
 * - Add accelerometer monitoring for activity detection
 * - Add ambient light sensor for auto-brightness
 * - Add proximity sensor
 * - Integrate with calendar for event awareness
 * - Add weather API integration
 */
class SensorMonitor(private val context: Context) {

    private var locationManager: LocationManager? = null
    private var currentLocation: Location? = null
    private var locationCallback: ((Location) -> Unit)? = null

    init {
        initializeLocationServices()
    }

    /**
     * Initialize location services
     */
    private fun initializeLocationServices() {
        try {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize location services", e)
        }
    }

    /**
     * Start monitoring location
     *
     * @param callback Invoked when location updates
     */
    fun startLocationMonitoring(callback: (Location) -> Unit) {
        this.locationCallback = callback

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted")
            return
        }

        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                UPDATE_INTERVAL,
                UPDATE_DISTANCE,
                locationListener
            )

            // Also request from network provider
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                UPDATE_INTERVAL,
                UPDATE_DISTANCE,
                locationListener
            )

            Log.d(TAG, "Location monitoring started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location monitoring", e)
        }
    }

    /**
     * Stop monitoring location
     */
    fun stopLocationMonitoring() {
        try {
            locationManager?.removeUpdates(locationListener)
            Log.d(TAG, "Location monitoring stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop location monitoring", e)
        }
    }

    /**
     * Get last known location
     */
    fun getLastKnownLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return try {
            locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last known location", e)
            null
        }
    }

    /**
     * Get current time context
     */
    fun getTimeContext(): TimeContext {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)

        val timeOfDay = when (hour) {
            in 0..5 -> TimeOfDay.NIGHT
            in 6..11 -> TimeOfDay.MORNING
            in 12..17 -> TimeOfDay.AFTERNOON
            in 18..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }

        val isWeekend = dayOfWeek == java.util.Calendar.SATURDAY ||
                dayOfWeek == java.util.Calendar.SUNDAY

        return TimeContext(
            hour = hour,
            timeOfDay = timeOfDay,
            dayOfWeek = dayOfWeek,
            isWeekend = isWeekend
        )
    }

    /**
     * Check if user is at a known location
     *
     * TODO: Implement geofencing for home/work detection
     */
    fun isAtKnownLocation(locationType: LocationType): Boolean {
        // TODO: Implement geofencing logic
        Log.d(TAG, "Checking if at location: $locationType")
        return false
    }

    /**
     * Get environmental context
     *
     * Combines location, time, and device state
     */
    fun getEnvironmentalContext(): EnvironmentalContext {
        val timeContext = getTimeContext()
        val location = getLastKnownLocation()

        return EnvironmentalContext(
            timeContext = timeContext,
            location = location,
            isMoving = false,  // TODO: Implement activity detection
            isCharging = isDeviceCharging(),
            batteryLevel = getBatteryLevel()
        )
    }

    /**
     * Check if device is charging
     */
    private fun isDeviceCharging(): Boolean {
        return try {
            val batteryManager =
                context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            batteryManager.isCharging
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get battery level
     */
    private fun getBatteryLevel(): Int {
        return try {
            val batteryManager =
                context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Location listener for updates
     */
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation = location
            locationCallback?.invoke(location)
            Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            // Deprecated but required for older Android versions
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "Provider enabled: $provider")
        }

        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "Provider disabled: $provider")
        }
    }

    companion object {
        private const val TAG = "SensorMonitor"
        private const val UPDATE_INTERVAL = 60000L  // 1 minute
        private const val UPDATE_DISTANCE = 100f     // 100 meters
    }
}

/**
 * Time of day categories
 */
enum class TimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}

/**
 * Known location types
 */
enum class LocationType {
    HOME,
    WORK,
    SCHOOL,
    GYM,
    UNKNOWN
}

/**
 * Time context data
 */
data class TimeContext(
    val hour: Int,
    val timeOfDay: TimeOfDay,
    val dayOfWeek: Int,
    val isWeekend: Boolean
)

/**
 * Environmental context combining multiple sensors
 */
data class EnvironmentalContext(
    val timeContext: TimeContext,
    val location: Location?,
    val isMoving: Boolean,
    val isCharging: Boolean,
    val batteryLevel: Int
)
