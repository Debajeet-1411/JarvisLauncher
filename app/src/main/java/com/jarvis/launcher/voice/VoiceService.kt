package com.jarvis.launcher.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

/**
 * Voice Service - handles speech recognition and text-to-speech
 *
 * Features:
 * - Speech-to-text using Android SpeechRecognizer
 * - Text-to-speech using Android TTS
 * - Error handling and callbacks
 *
 * Usage:
 * ```
 * val voiceService = VoiceService(context)
 * voiceService.startListening { recognizedText ->
 *     // Handle recognized text
 * }
 * voiceService.speak("Hello, I am JARVIS")
 * ```
 */
class VoiceService(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var onResultCallback: ((String) -> Unit)? = null

    init {
        initializeTTS()
    }

    /**
     * Initialize Text-to-Speech engine
     */
    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    /**
     * Start listening for voice input
     *
     * @param onResult Callback invoked with recognized text
     */
    fun startListening(onResult: (String) -> Unit) {
        this.onResultCallback = onResult

        // Clean up existing recognizer
        speechRecognizer?.destroy()

        // Create new speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(recognitionListener)
        }

        // Create recognition intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        // Start listening
        speechRecognizer?.startListening(intent)
        Log.d(TAG, "Started listening")
    }

    /**
     * Stop listening for voice input
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        Log.d(TAG, "Stopped listening")
    }

    /**
     * Speak text aloud using TTS
     *
     * @param text Text to speak
     */
    fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        Log.d(TAG, "Speaking: $text")
    }

    /**
     * Check if TTS is speaking
     */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }

    /**
     * Stop current speech
     */
    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    /**
     * Clean up resources
     * Call this when done with the service (e.g., in onDestroy)
     */
    fun cleanup() {
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
        speechRecognizer = null
        textToSpeech = null
        Log.d(TAG, "Voice service cleaned up")
    }

    /**
     * Recognition listener for handling speech recognition events
     */
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech started")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Volume level changed - can be used for visual feedback
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "Speech ended")
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                else -> "Unknown error"
            }
            Log.e(TAG, "Recognition error: $errorMessage")

            // Invoke callback with empty string on error
            if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                onResultCallback?.invoke("")
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                Log.d(TAG, "Recognized: $recognizedText")
                onResultCallback?.invoke(recognizedText)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Partial results available during recognition
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                Log.d(TAG, "Partial: ${matches[0]}")
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Reserved for future use
        }
    }

    companion object {
        private const val TAG = "VoiceService"
    }
}
