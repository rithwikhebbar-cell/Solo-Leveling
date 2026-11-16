package com.example.service

import android.content.Context
import com.example.domain.model.AiCoachingMode
import com.example.domain.model.SocialProfile
import com.example.domain.model.SpeechInputUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Milestone 12 - Phase 3: AI Voice Coach Controller.
 * Bridges speech-to-text input (SystemSpeechCommandController),
 * AI response generation (SocialMasteryAiCoach), and text-to-speech output (SystemVoiceEngine).
 *
 * Privacy Guarantees:
 * - No background recording.
 * - Explicit user button-press to record.
 * - Handles microphone denial, network errors, and speech service absence cleanly.
 * - Audio is not stored permanently or synchronized to cloud.
 */
class SocialVoiceCoachController(
    private val context: Context,
    private val aiCoach: SocialMasteryAiCoach,
    private val voiceEngine: SystemVoiceEngine?,
    private val coroutineScope: CoroutineScope
) {
    private val _isVoiceActive = MutableStateFlow(false)
    val isVoiceActive: StateFlow<Boolean> = _isVoiceActive.asStateFlow()

    private val _transcriptText = MutableStateFlow("")
    val transcriptText: StateFlow<String> = _transcriptText.asStateFlow()

    private val _voiceStatusMessage = MutableStateFlow("Tap microphone to start speaking")
    val voiceStatusMessage: StateFlow<String> = _voiceStatusMessage.asStateFlow()

    private var speechController: SystemSpeechCommandController? = null

    init {
        val sc = SystemSpeechCommandController(context) { recognizedText ->
            _transcriptText.value = recognizedText
            _voiceStatusMessage.value = "Speech captured. Formulating coaching response..."
        }
        speechController = sc
        coroutineScope.launch {
            sc.state.collect { state ->
                when (state) {
                    is SpeechInputUiState.Unavailable -> {
                        _isVoiceActive.value = false
                        _voiceStatusMessage.value = state.reason
                    }
                    is SpeechInputUiState.Listening -> {
                        _isVoiceActive.value = true
                        _voiceStatusMessage.value = "Listening... Speak your prompt clearly."
                    }
                    is SpeechInputUiState.Processing -> {
                        _voiceStatusMessage.value = "Processing captured speech..."
                    }
                    is SpeechInputUiState.Idle -> {
                        // Retain existing state
                    }
                }
            }
        }
    }

    fun startListening() {
        val sc = speechController
        if (sc == null || !sc.isRecognizerSupported()) {
            _voiceStatusMessage.value = "Speech recognition unavailable on this device. Type your prompt below."
            return
        }

        _isVoiceActive.value = true
        _voiceStatusMessage.value = "Listening... Speak your prompt clearly."
        sc.startListening()
    }

    fun stopListeningAndSubmit(
        mode: AiCoachingMode,
        profile: SocialProfile?,
        speakReplyAloud: Boolean = true
    ) {
        speechController?.stopListening()
        _isVoiceActive.value = false

        val prompt = _transcriptText.value.trim()
        if (prompt.isBlank()) {
            _voiceStatusMessage.value = "No speech detected. Tap microphone and speak."
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            _voiceStatusMessage.value = "Coach is analyzing your speech..."
            val result = aiCoach.sendMessage(prompt, mode, profile)
            result.onSuccess { replyText ->
                _voiceStatusMessage.value = "Coach response received."
                if (speakReplyAloud && voiceEngine != null) {
                    val cleanForTts = replyText.replace(Regex("\\[.*?\\]"), "").take(300)
                    voiceEngine.speak(cleanForTts, interruptCurrent = true)
                }
            }.onFailure { err ->
                _voiceStatusMessage.value = "Coach error: ${err.localizedMessage}. Using offline text guidance."
            }
        }
    }

    fun cancelSession() {
        speechController?.stopListening()
        voiceEngine?.stop()
        _isVoiceActive.value = false
        _transcriptText.value = ""
        _voiceStatusMessage.value = "Voice session cancelled."
    }

    fun release() {
        cancelSession()
        speechController?.releaseRecognizer()
        speechController = null
    }
}
