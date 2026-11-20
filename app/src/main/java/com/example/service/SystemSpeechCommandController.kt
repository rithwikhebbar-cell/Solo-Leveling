package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.domain.model.SpeechInputUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User-initiated Android SpeechRecognizer controller.
 * Privacy guarantee:
 * - Never listens in background.
 * - Starts only on explicit button tap after RECORD_AUDIO runtime permission is granted.
 * - Never stores raw audio files.
 */
class SystemSpeechCommandController(
    private val context: Context,
    private val onTranscriptReady: (String) -> Unit
) {
    private val _state = MutableStateFlow<SpeechInputUiState>(SpeechInputUiState.Idle)
    val state: StateFlow<SpeechInputUiState> = _state.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    fun isRecognizerSupported(): Boolean = runCatching {
        SpeechRecognizer.isRecognitionAvailable(context)
    }.getOrDefault(false)

    fun startListening(localeTag: String = "en-US") {
        if (!isRecognizerSupported()) {
            _state.value = SpeechInputUiState.Unavailable(
                "On-device or system speech recognition service unavailable. Use typed command input below."
            )
            return
        }

        stopListening()
        runCatching {
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = sr
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = SpeechInputUiState.Listening(0f)
                }

                override fun onBeginningOfSpeech() {
                    _state.value = SpeechInputUiState.Listening(2f)
                }

                override fun onRmsChanged(rmsdB: Float) {
                    if (_state.value is SpeechInputUiState.Listening) {
                        _state.value = SpeechInputUiState.Listening(rmsdB.coerceAtLeast(0f))
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    _state.value = SpeechInputUiState.Processing
                }

                override fun onError(error: Int) {
                    val reason = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio capture error."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                        SpeechRecognizer.ERROR_NO_MATCH -> "Could not recognize speech. Try speaking clearly or use text input."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out before speech was detected."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network recognition unavailable; try offline text fallback."
                        else -> "Speech recognizer stopped (code $error)."
                    }
                    _state.value = SpeechInputUiState.Unavailable(reason)
                    releaseRecognizer()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val top = matches?.firstOrNull()?.trim().orEmpty()
                    _partialTranscript.value = ""
                    _state.value = SpeechInputUiState.Idle
                    releaseRecognizer()
                    if (top.isNotBlank()) {
                        onTranscriptReady(top)
                    } else {
                        _state.value = SpeechInputUiState.Unavailable("No recognized command transcript.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            _partialTranscript.value = ""
            _state.value = SpeechInputUiState.Listening(0f)
            sr.startListening(intent)
        }.onFailure { err ->
            _state.value = SpeechInputUiState.Unavailable(err.message ?: "Unable to initialize speech recognition.")
            releaseRecognizer()
        }
    }

    fun stopListening() {
        runCatching { recognizer?.stopListening() }
        releaseRecognizer()
        _partialTranscript.value = ""
        if (_state.value is SpeechInputUiState.Listening || _state.value is SpeechInputUiState.Processing) {
            _state.value = SpeechInputUiState.Idle
        }
    }

    fun clearError() {
        if (_state.value is SpeechInputUiState.Unavailable) {
            _state.value = SpeechInputUiState.Idle
        }
    }

    fun releaseRecognizer() {
        runCatching { recognizer?.destroy() }
        recognizer = null
    }
}
