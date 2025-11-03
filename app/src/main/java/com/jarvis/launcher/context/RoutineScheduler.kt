package com.jarvis.launcher.context

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Routine Scheduler - schedules automated tasks and routines
 *
 * Uses WorkManager for background task scheduling
 *
 * Features:
 * - Time-based routines (e.g., "Good morning" at 7 AM)
 * - Location-based triggers (e.g., arriving home)
 * - Condition-based automation
 *
 * TODO Day 2:
 * - Implement actual routine execution
 * - Add persistent routine storage (Room database)
 * - Create routine builder UI
 * - Add conditional logic (if/then rules)
 */
class RoutineScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule a daily routine
     *
     * @param routineName Name of the routine
     * @param hour Hour to execute (0-23)
     * @param minute Minute to execute (0-59)
     */
    fun scheduleDailyRoutine(routineName: String, hour: Int, minute: Int) {
        // Calculate initial delay until next occurrence
        val currentTime = java.util.Calendar.getInstance()
        val scheduledTime = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)

            // If scheduled time has passed today, schedule for tomorrow
            if (before(currentTime)) {
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelay = scheduledTime.timeInMillis - currentTime.timeInMillis

        // Create work request
        val workRequest = PeriodicWorkRequestBuilder<RoutineWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    "routine_name" to routineName,
                    "hour" to hour,
                    "minute" to minute
                )
            )
            .addTag("routine_$routineName")
            .build()

        // Enqueue work
        workManager.enqueueUniquePeriodicWork(
            "routine_$routineName",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "Scheduled daily routine: $routineName at $hour:$minute")
    }

    /**
     * Schedule a one-time task
     *
     * @param taskName Name of the task
     * @param delayMinutes Delay in minutes before execution
     */
    fun scheduleOneTimeTask(taskName: String, delayMinutes: Long) {
        val workRequest = OneTimeWorkRequestBuilder<RoutineWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    "task_name" to taskName
                )
            )
            .addTag("task_$taskName")
            .build()

        workManager.enqueue(workRequest)

        Log.d(TAG, "Scheduled one-time task: $taskName in $delayMinutes minutes")
    }

    /**
     * Cancel a scheduled routine
     *
     * @param routineName Name of the routine to cancel
     */
    fun cancelRoutine(routineName: String) {
        workManager.cancelUniqueWork("routine_$routineName")
        Log.d(TAG, "Cancelled routine: $routineName")
    }

    /**
     * Cancel all scheduled routines
     */
    fun cancelAllRoutines() {
        workManager.cancelAllWorkByTag("routine")
        Log.d(TAG, "Cancelled all routines")
    }

    /**
     * Get list of scheduled routines
     *
     * TODO: Implement persistent storage and retrieval
     */
    fun getScheduledRoutines(): List<RoutineInfo> {
        // TODO: Retrieve from database
        return emptyList()
    }

    companion object {
        private const val TAG = "RoutineScheduler"
    }
}

/**
 * WorkManager worker for executing routines
 */
class RoutineWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val routineName = inputData.getString("routine_name")
        val taskName = inputData.getString("task_name")

        return try {
            when {
                routineName != null -> executeRoutine(routineName)
                taskName != null -> executeTask(taskName)
                else -> {
                    Log.w(TAG, "No routine or task name provided")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing work", e)
            Result.failure()
        }
    }

    /**
     * Execute a routine
     */
    private fun executeRoutine(routineName: String): Result {
        Log.d(TAG, "Executing routine: $routineName")

        // TODO: Implement actual routine execution
        // This would integrate with:
        // - DeviceController for device automation
        // - HomeController for smart home actions
        // - AiEngine for natural language processing

        // Stub implementation
        when (routineName.lowercase()) {
            "good morning" -> {
                Log.d(TAG, "- Turning on lights")
                Log.d(TAG, "- Reading news headlines")
                Log.d(TAG, "- Checking calendar")
            }

            "leaving home" -> {
                Log.d(TAG, "- Turning off lights")
                Log.d(TAG, "- Setting thermostat to away mode")
                Log.d(TAG, "- Arming security system")
            }

            "bedtime" -> {
                Log.d(TAG, "- Dimming lights")
                Log.d(TAG, "- Setting phone to silent")
                Log.d(TAG, "- Locking doors")
            }
        }

        return Result.success()
    }

    /**
     * Execute a one-time task
     */
    private fun executeTask(taskName: String): Result {
        Log.d(TAG, "Executing task: $taskName")

        // TODO: Implement task execution

        return Result.success()
    }

    companion object {
        private const val TAG = "RoutineWorker"
    }
}

/**
 * Routine information
 */
data class RoutineInfo(
    val name: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val daysOfWeek: List<Int>
)
