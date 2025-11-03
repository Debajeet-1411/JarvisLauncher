package com.jarvis.launcher.ui.gestures

import android.content.Context
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Gesture Handler for Launcher
 *
 * Supports:
 * - Swipe up: Open app drawer
 * - Swipe down: Open notifications
 * - Swipe left/right: Switch pages
 * - Double tap: Quick search
 * - Two-finger swipe down: Quick settings
 * - Long press: Widget mode
 *
 * Day 4: Advanced gesture controls
 */
class GestureHandler(private val context: Context) {

    companion object {
        private const val SWIPE_THRESHOLD = 100f
        private const val VELOCITY_THRESHOLD = 500f
        private const val DOUBLE_TAP_TIMEOUT = 300L

        // Gesture directions
        const val DIRECTION_UP = 0
        const val DIRECTION_DOWN = 1
        const val DIRECTION_LEFT = 2
        const val DIRECTION_RIGHT = 3
    }

    private var lastTapTime = 0L
    private var gestureCallbacks: GestureCallbacks? = null

    /**
     * Set gesture callbacks
     */
    fun setCallbacks(callbacks: GestureCallbacks) {
        gestureCallbacks = callbacks
    }

    /**
     * Detect swipe gestures
     */
    suspend fun PointerInputScope.detectSwipeGestures() = coroutineScope {
        detectDragGestures(
            onDragStart = { offset ->
                // Optionally handle drag start
            },
            onDragEnd = {
                // Drag ended
            },
            onDrag = { change, dragAmount ->
                change.consume()

                // Calculate total drag distance
                val totalDrag = Offset(dragAmount.x, dragAmount.y)

                // Determine direction and trigger callback if threshold met
                if (abs(totalDrag.x) > SWIPE_THRESHOLD || abs(totalDrag.y) > SWIPE_THRESHOLD) {
                    val direction = getSwipeDirection(totalDrag)
                    val velocity = calculateVelocity(totalDrag)

                    if (velocity > VELOCITY_THRESHOLD) {
                        handleSwipe(direction, velocity)
                    }
                }
            }
        )
    }

    /**
     * Detect tap gestures
     */
    suspend fun PointerInputScope.detectTapGesture() = coroutineScope {
        detectTapGestures(
            onTap = { offset ->
                handleTap(offset)
            },
            onDoubleTap = { offset ->
                handleDoubleTap(offset)
            },
            onLongPress = { offset ->
                handleLongPress(offset)
            }
        )
    }

    /**
     * Get swipe direction from offset
     */
    private fun getSwipeDirection(offset: Offset): Int {
        val angle = Math.toDegrees(atan2(offset.y.toDouble(), offset.x.toDouble()))

        return when {
            angle in -45.0..45.0 -> DIRECTION_RIGHT
            angle in 45.0..135.0 -> DIRECTION_DOWN
            angle in -135.0..-45.0 -> DIRECTION_UP
            else -> DIRECTION_LEFT
        }
    }

    /**
     * Calculate swipe velocity
     */
    private fun calculateVelocity(offset: Offset): Float {
        return sqrt(offset.x * offset.x + offset.y * offset.y)
    }

    /**
     * Handle swipe gesture
     */
    private fun handleSwipe(direction: Int, velocity: Float) {
        when (direction) {
            DIRECTION_UP -> gestureCallbacks?.onSwipeUp(velocity)
            DIRECTION_DOWN -> gestureCallbacks?.onSwipeDown(velocity)
            DIRECTION_LEFT -> gestureCallbacks?.onSwipeLeft(velocity)
            DIRECTION_RIGHT -> gestureCallbacks?.onSwipeRight(velocity)
        }
    }

    /**
     * Handle tap
     */
    private fun handleTap(offset: Offset) {
        val currentTime = System.currentTimeMillis()

        // Check for double tap
        if (currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT) {
            handleDoubleTap(offset)
        }

        lastTapTime = currentTime
        gestureCallbacks?.onSingleTap(offset)
    }

    /**
     * Handle double tap
     */
    private fun handleDoubleTap(offset: Offset) {
        gestureCallbacks?.onDoubleTap(offset)
    }

    /**
     * Handle long press
     */
    private fun handleLongPress(offset: Offset) {
        gestureCallbacks?.onLongPress(offset)
    }
}

/**
 * Gesture callbacks interface
 */
interface GestureCallbacks {
    fun onSwipeUp(velocity: Float) {}
    fun onSwipeDown(velocity: Float) {}
    fun onSwipeLeft(velocity: Float) {}
    fun onSwipeRight(velocity: Float) {}
    fun onSingleTap(offset: Offset) {}
    fun onDoubleTap(offset: Offset) {}
    fun onLongPress(offset: Offset) {}
    fun onTwoFingerSwipeDown() {}
    fun onPinchZoom(scale: Float) {}
}

/**
 * Gesture action configuration
 */
data class GestureAction(
    val gesture: GestureType,
    val action: ActionType,
    val enabled: Boolean = true
)

/**
 * Gesture types
 */
enum class GestureType {
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    DOUBLE_TAP,
    LONG_PRESS,
    TWO_FINGER_SWIPE,
    PINCH_ZOOM
}

/**
 * Action types
 */
enum class ActionType {
    OPEN_APP_DRAWER,
    OPEN_NOTIFICATIONS,
    OPEN_QUICK_SETTINGS,
    OPEN_SEARCH,
    PREVIOUS_PAGE,
    NEXT_PAGE,
    SHOW_WIDGETS,
    LAUNCH_APP,
    VOICE_SEARCH,
    TAKE_SCREENSHOT,
    NONE
}

/**
 * Gesture configuration storage
 */
data class GestureConfig(
    val swipeUpAction: ActionType = ActionType.OPEN_APP_DRAWER,
    val swipeDownAction: ActionType = ActionType.OPEN_NOTIFICATIONS,
    val swipeLeftAction: ActionType = ActionType.NEXT_PAGE,
    val swipeRightAction: ActionType = ActionType.PREVIOUS_PAGE,
    val doubleTapAction: ActionType = ActionType.OPEN_SEARCH,
    val longPressAction: ActionType = ActionType.SHOW_WIDGETS,
    val twoFingerSwipeAction: ActionType = ActionType.OPEN_QUICK_SETTINGS
)
