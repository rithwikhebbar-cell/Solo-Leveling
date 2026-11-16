package com.example.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.domain.model.TtsSpeechPriority
import com.example.domain.model.VoiceEngineAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.PriorityQueue
import java.util.UUID

interface VoiceOutputPort {
    val availability: StateFlow<VoiceEngineAvailability>
    val lastSpokenMessage: StateFlow<String?>

    fun speak(
        text: String,
        priority: TtsSpeechPriority = TtsSpeechPriority.NORMAL,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f,
        localeTag: String = "en-US",
        interruptCurrent: Boolean = false
    )

    fun stop()
    fun updateMuted(muted: Boolean)
    fun shutdown()
}

private data class QueuedSpeechItem(
    val id: String,
    val text: String,
    val priority: TtsSpeechPriority,
    val speechRate: Float,
    val pitch: Float,
    val localeTag: String,
    val sequenceOrder: Long
)

typealias SystemVoiceEngine = AndroidSystemVoiceEngine

/**
 * Native Android TextToSpeech System Voice Engine with priority queue,
 * duplicate suppression, rate/pitch calibration, and offline/unavailable safety.
 */
class AndroidSystemVoiceEngine(context: Context) : VoiceOutputPort {

    private val appContext = context.applicationContext
    private val _availability = MutableStateFlow(VoiceEngineAvailability.INITIALIZING)
    override val availability: StateFlow<VoiceEngineAvailability> = _availability.asStateFlow()

    private val _lastSpokenMessage = MutableStateFlow<String?>(null)
    override val lastSpokenMessage: StateFlow<String?> = _lastSpokenMessage.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isMuted = false
    private var isCurrentlySpeaking = false
    private var seqCounter = 0L
    private var lastQueuedText: String? = null
    private var lastQueuedAtMs: Long = 0L

    private val queueLock = Any()
    private val pendingQueue = PriorityQueue<QueuedSpeechItem>(
        compareByDescending<QueuedSpeechItem> { it.priority.weight }.thenBy { it.sequenceOrder }
    )

    init {
        runCatching {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isCurrentlySpeaking = true
                            _availability.value = if (isMuted) VoiceEngineAvailability.MUTED else VoiceEngineAvailability.SPEAKING
                        }

                        override fun onDone(utteranceId: String?) {
                            isCurrentlySpeaking = false
                            _availability.value = if (isMuted) VoiceEngineAvailability.MUTED else VoiceEngineAvailability.READY
                            drainNext()
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            isCurrentlySpeaking = false
                            _availability.value = if (isMuted) VoiceEngineAvailability.MUTED else VoiceEngineAvailability.READY
                            drainNext()
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            isCurrentlySpeaking = false
                            _availability.value = if (isMuted) VoiceEngineAvailability.MUTED else VoiceEngineAvailability.READY
                            drainNext()
                        }
                    })
                    _availability.value = if (isMuted) VoiceEngineAvailability.MUTED else VoiceEngineAvailability.READY
                    drainNext()
                } else {
                    isInitialized = false
                    _availability.value = VoiceEngineAvailability.UNAVAILABLE
                }
            }
        }.onFailure {
            isInitialized = false
            _availability.value = VoiceEngineAvailability.UNAVAILABLE
        }
    }

    override fun speak(
        text: String,
        priority: TtsSpeechPriority,
        speechRate: Float,
        pitch: Float,
        localeTag: String,
        interruptCurrent: Boolean
    ) {
        val clean = text.trim()
        if (clean.isBlank() || isMuted) return

        val now = System.currentTimeMillis()
        synchronized(queueLock) {
            if (clean == lastQueuedText && now - lastQueuedAtMs < 4_000L && !interruptCurrent) {
                return
            }
            lastQueuedText = clean
            lastQueuedAtMs = now
            if (interruptCurrent || priority == TtsSpeechPriority.CRITICAL) {
                pendingQueue.clear()
                runCatching { tts?.stop() }
                isCurrentlySpeaking = false
            }
            pendingQueue.offer(
                QueuedSpeechItem(
                    id = UUID.randomUUID().toString(),
                    text = clean,
                    priority = priority,
                    speechRate = speechRate.coerceIn(0.6f, 1.8f),
                    pitch = pitch.coerceIn(0.6f, 1.6f),
                    localeTag = localeTag,
                    sequenceOrder = ++seqCounter
                )
            )
        }
        drainNext()
    }

    private fun drainNext() {
        if (!isInitialized || isMuted || isCurrentlySpeaking) return
        val nextItem: QueuedSpeechItem? = synchronized(queueLock) {
            if (isCurrentlySpeaking) null else pendingQueue.poll()
        }
        if (nextItem == null) return

        runCatching {
            val engine = tts ?: return
            val locale = Locale.forLanguageTag(nextItem.localeTag)
            val langStatus = engine.setLanguage(locale)
            if (langStatus == TextToSpeech.LANG_MISSING_DATA || langStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.language = Locale.US
            }
            engine.setSpeechRate(nextItem.speechRate)
            engine.setPitch(nextItem.pitch)
            isCurrentlySpeaking = true
            _lastSpokenMessage.value = nextItem.text
            val res = engine.speak(nextItem.text, TextToSpeech.QUEUE_FLUSH, null, nextItem.id)
            if (res == TextToSpeech.ERROR) {
                isCurrentlySpeaking = false
                _availability.value = VoiceEngineAvailability.UNAVAILABLE
            }
        }.onFailure {
            isCurrentlySpeaking = false
            _availability.value = VoiceEngineAvailability.UNAVAILABLE
        }
    }

    override fun stop() {
        synchronized(queueLock) {
            pendingQueue.clear()
        }
        isCurrentlySpeaking = false
        runCatching { tts?.stop() }
        if (isInitialized && !isMuted) {
            _availability.value = VoiceEngineAvailability.READY
        }
    }

    override fun updateMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            stop()
            _availability.value = VoiceEngineAvailability.MUTED
        } else {
            _availability.value = if (isInitialized) VoiceEngineAvailability.READY else VoiceEngineAvailability.UNAVAILABLE
        }
    }

    override fun shutdown() {
        stop()
        runCatching {
            tts?.shutdown()
            tts = null
        }
        isInitialized = false
    }
}
