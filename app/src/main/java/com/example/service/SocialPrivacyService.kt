package com.example.service

import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first

/**
 * Milestone 12: Social Privacy and AI Consent Service.
 * Manages explicit opt-in boundaries for AI evaluations,
 * per-record transcript cloud sync, and global consent overrides.
 */
class SocialPrivacyService(
    private val preferencesRepository: UserPreferencesRepository? = null
) {
    /**
     * Regex patterns for deterministic PII redaction.
     */
    private val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}")
    private val phoneRegex = Regex("(\\+\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[-. ]?\\d{3}[-. ]?\\d{4}")
    private val urlRegex = Regex("https?://\\S+\\b")
    private val digitSequenceRegex = Regex("\\b(?:\\d[ -]*?){13,16}\\b")

    fun sanitizeTranscriptForAi(rawText: String, playerName: String? = null): String {
        var text = rawText
            .replace(emailRegex, "[REDACTED_EMAIL]")
            .replace(phoneRegex, "[REDACTED_PHONE]")
            .replace(urlRegex, "[REDACTED_URL]")
            .replace(digitSequenceRegex, "[REDACTED_NUMBER]")

        if (!playerName.isNullOrBlank()) {
            text = text.replace(Regex(Regex.escape(playerName), RegexOption.IGNORE_CASE), "[PLAYER_NAME]")
        }

        return text
    }

    suspend fun isCloudTranscriptSyncAllowedGlobally(): Boolean {
        // Tied to user-level cloud sync consent
        val prefs = preferencesRepository?.preferencesFlow?.first()
        return prefs?.supabaseCloudSyncEnabled ?: false
    }

    fun canSyncSessionTranscript(
        sessionConsentFlag: Boolean,
        globalConsentFlag: Boolean
    ): Boolean {
        // Global preference strictly overrides individual session consent
        return sessionConsentFlag && globalConsentFlag
    }
}
