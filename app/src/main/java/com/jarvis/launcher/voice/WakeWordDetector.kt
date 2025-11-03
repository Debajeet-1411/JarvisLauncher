package com.jarvis.launcher.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Wake Word Detector - "Hey JARVIS"
 *
 * Features:
 * - Continuous audio monitoring
 * - Low-power operation
 * - Pattern matching for wake phrase
 * - Background service capable
 * - Noise filtering
 *
 * Day 4: Advanced voice activation
 */
class WakeWordDetector(private val context: Context) {

    companion object {
        private const val TAG = "WakeWordDetector"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2

        // Energy threshold for voice activity detection
        private const val ENERGY_THRESHOLD = 1000

        // Wake word detection states
        private const val STATE_IDLE = 0
        private const val STATE_LISTENING = 1
        private const val STATE_WAKE_WORD_DETECTED = 2
    }

    private var audioRecord: AudioRecord? = null
    private var isMonitoring = false
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _detectionState = MutableStateFlow(STATE_IDLE)
    val detectionState: StateFlow<Int> = _detectionState.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var onWakeWordDetected: (() -> Unit)? = null

    /**
     * Start monitoring for wake word
     */
    fun startMonitoring(onDetected: () -> Unit) {
        if (isMonitoring) {
            Log.d(TAG, "Already monitoring")
            return
        }

        onWakeWordDetected = onDetected

        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            ) * BUFFER_SIZE_MULTIPLIER

            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size")
                return
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized")
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isMonitoring = true
            _isActive.value = true
            _detectionState.value = STATE_LISTENING

            // Start audio processing in background
            scope.launch {
                processAudioStream(bufferSize)
            }

            Log.d(TAG, "Wake word monitoring started")

        } catch (e: SecurityException) {
            Log.e(TAG, "Audio recording permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting wake word detection", e)
        }
    }

    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        if (!isMonitoring) return

        isMonitoring = false
        _isActive.value = false
        _detectionState.value = STATE_IDLE

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(TAG, "Wake word monitoring stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping wake word detection", e)
        }
    }

    /**
     * Process audio stream for wake word detection
     */
    private suspend fun processAudioStream(bufferSize: Int) = withContext(Dispatchers.Default) {
        val buffer = ShortArray(bufferSize)
        var silenceCounter = 0
        val maxSilenceFrames = 50 // ~1 second of silence

        while (isMonitoring) {
            try {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (readSize > 0) {
                    // Calculate audio energy (simplified Voice Activity Detection)
                    val energy = calculateEnergy(buffer, readSize)

                    if (energy > ENERGY_THRESHOLD) {
                        // Voice detected, reset silence counter
                        silenceCounter = 0

                        // Perform wake word detection
                        if (detectWakeWord(buffer, readSize, energy)) {
                            handleWakeWordDetected()
                        }
                    } else {
                        // Silence detected
                        silenceCounter++

                        // Optional: Enter low-power mode after extended silence
                        if (silenceCounter > maxSilenceFrames) {
                            // Could reduce sampling rate or processing here
                            delay(100) // Small delay to reduce CPU usage
                        }
                    }
                }

                // Small delay to prevent CPU hogging
                delay(10)

            } catch (e: Exception) {
                Log.e(TAG, "Error processing audio", e)
                if (!isMonitoring) break
            }
        }
    }

    /**
     * Calculate audio energy (simple RMS)
     */
    private fun calculateEnergy(buffer: ShortArray, size: Int): Double {
        var sum = 0.0
        for (i in 0 until size) {
            sum += abs(buffer[i].toDouble())
        }
        return sum / size
    }

    /**
     * Detect wake word in audio buffer
     *
     * TODO: Implement actual wake word detection using:
     * - Pocketsphinx (offline)
     * - TensorFlow Lite model
     * - Pattern matching algorithms
     *
     * Current: Simplified energy-based detection
     */
    private fun detectWakeWord(buffer: ShortArray, size: Int, energy: Double): Boolean {
        // Simplified detection logic
        // In production, use a proper wake word detection library

        // For now, detect high energy patterns that might indicate speech
        // This is a placeholder - real implementation would use ML or keyword spotting

        val highEnergyThreshold = ENERGY_THRESHOLD * 3

        if (energy > highEnergyThreshold) {
            // Count high energy peaks (simplified)
            var peakCount = 0
            for (i in 0 until size step 100) {
                if (abs(buffer[i].toInt()) > 5000) {
                    peakCount++
                }
            }

            // "Hey JARVIS" typically has 3 syllables with distinct energy peaks
            // This is very simplified - production would use proper keyword spotting
            if (peakCount in 2..5) {
                Log.d(TAG, "Potential wake word detected (simplified method)")
                return true
            }
        }

        return false
    }

    /**
     * Handle wake word detection
     */
    private fun handleWakeWordDetected() {
        _detectionState.value = STATE_WAKE_WORD_DETECTED

        Log.d(TAG, "Wake word detected!")

        // Invoke callback on main thread
        scope.launch(Dispatchers.Main) {
            onWakeWordDetected?.invoke()
        }

        // Reset state after a delay
        scope.launch {
            delay(2000)
            if (_detectionState.value == STATE_WAKE_WORD_DETECTED) {
                _detectionState.value = STATE_LISTENING
            }
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopMonitoring()
        scope.cancel()
    }
}

/**
 * Wake Word Settings
 */
data class WakeWordSettings(
    val enabled: Boolean = false,
    val wakePhrase: String = "Hey JARVIS",
    val sensitivity: Float = 0.5f,
    val requireUnlock: Boolean = false
)
