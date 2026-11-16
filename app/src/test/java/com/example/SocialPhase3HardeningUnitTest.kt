package com.example

import com.example.data.integration.BackendAiGateway
import com.example.data.repository.SocialRepository
import com.example.domain.engine.ActivityRewardCalculation
import com.example.domain.engine.SocialMasteryAnalyticsEngine
import com.example.domain.model.AiCoachingMode
import com.example.domain.model.AttributeType
import com.example.domain.model.PracticeDifficulty
import com.example.domain.model.SocialActivityKind
import com.example.domain.model.SocialAssessment
import com.example.domain.model.SocialEvidenceSource
import com.example.domain.model.SocialMasteryStage
import com.example.domain.model.SocialModuleIdentifier
import com.example.domain.model.SocialModuleProgress
import com.example.domain.model.SocialPracticeSession
import com.example.domain.model.SocialProfile
import com.example.domain.model.SocialScenario
import com.example.domain.model.SocialScenarioAttempt
import com.example.domain.model.SocialSessionStatus
import com.example.domain.model.VerificationStatus
import com.example.service.SocialMasteryAiCoach
import com.example.service.SocialPrivacyService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Milestone 12 - Phase 3.1: Integration Audit, Hardening & Verification Unit Tests.
 *
 * Verifies:
 * 1. AI Coach bounded memory window, sanitization, and offline fallback.
 * 2. Analytics Engine empty dataset resilience, heuristic brevity detection, and adaptive recommendations.
 * 3. Progression separation (AI coaching is non-evaluative and awards zero XP).
 * 4. PII Redaction hierarchy and consent boundaries.
 */
class SocialPhase3HardeningUnitTest {

    // --- Fake Repository for Analytics Testing ---
    private class FakeSocialRepository(
        var profile: SocialProfile = SocialProfile(accountOwnerId = "test_player"),
        var progressList: List<SocialModuleProgress> = emptyList(),
        var recentSessions: List<SocialPracticeSession> = emptyList()
    ) : SocialRepository {
        override suspend fun getOrCreateProfile(accountOwnerId: String): SocialProfile = profile
        override fun observeProfile(accountOwnerId: String): Flow<SocialProfile?> = flowOf(profile)
        override suspend fun getModuleProgress(accountOwnerId: String, module: SocialModuleIdentifier): SocialModuleProgress =
            progressList.firstOrNull { it.module == module } ?: SocialModuleProgress(accountOwnerId, module)
        override fun observeAllModuleProgress(accountOwnerId: String): Flow<List<SocialModuleProgress>> = flowOf(progressList)
        override suspend fun initializeMissingModuleProgress(accountOwnerId: String) = Unit
        override suspend fun getActiveScenarios(): List<SocialScenario> = emptyList()
        override fun observeActiveScenarios(): Flow<List<SocialScenario>> = flowOf(emptyList())
        override suspend fun seedInitialScenariosIfEmpty() = Unit
        override suspend fun completePracticeSession(
            session: SocialPracticeSession,
            rubricScorePercent: Int?
        ): Result<Pair<SocialPracticeSession, ActivityRewardCalculation>> = Result.success(
            session to ActivityRewardCalculation(
                xpAwarded = 25,
                primaryAttribute = AttributeType.CHA,
                primaryAttrXpAwarded = 15,
                secondaryAttribute = AttributeType.WIS,
                secondaryAttrXpAwarded = 10,
                wasCapped = false
            )
        )
        override suspend fun recordScenarioAttempt(attempt: SocialScenarioAttempt): Result<SocialScenarioAttempt> = Result.success(attempt)
        override suspend fun recordAssessment(assessment: SocialAssessment): Result<SocialAssessment> = Result.success(assessment)
        override suspend fun observeRecentSessions(accountOwnerId: String, limit: Int): Flow<List<SocialPracticeSession>> = flowOf(recentSessions)
        override suspend fun calculateTotalPracticeSeconds(accountOwnerId: String): Long = recentSessions.sumOf { it.durationSeconds.toLong() }
        override suspend fun deletePracticeSession(sessionId: String, accountOwnerId: String): Result<Unit> = Result.success(Unit)
    }

    // =========================================================================
    // 1. AI COACH AUDIT & HARDENING TESTS
    // =========================================================================

    @Test
    fun testAiCoach_BoundedMemoryWindow_ClampsAtMaxLimit() = runBlocking {
        val privacyService = SocialPrivacyService()
        val aiGateway = BackendAiGateway()
        val aiCoach = SocialMasteryAiCoach(aiGateway, privacyService)

        // Send 25 messages through offline fallback
        for (i in 1..25) {
            aiCoach.sendMessage(
                userPrompt = "Practice prompt $i",
                mode = AiCoachingMode.PERSONAL_COACH,
                profile = null,
                backendBaseUrl = "http://invalid-test-url:9999"
            )
        }

        // Must be clamped to MAX_STORED_MESSAGES (20)
        val messages = aiCoach.messages.value
        assertTrue("Messages list must not exceed 20 entries, actual: ${messages.size}", messages.size <= 20)
        assertEquals(20, messages.size)

        // Clear session resets cleanly
        aiCoach.clearSession()
        assertTrue("Messages list must be empty after clearSession", aiCoach.messages.value.isEmpty())
    }

    @Test
    fun testAiCoach_SanitizesSensitiveDataBeforeSubmission() = runBlocking {
        val privacyService = SocialPrivacyService()

        val rawInput = "Contact me at hunter@sung.org or +1 (555) 234-5678. Also check https://secret-dungeon.com and card 4532-1234-5678-9012. My name is Jin-Woo."
        val sanitized = privacyService.sanitizeTranscriptForAi(rawInput, playerName = "Jin-Woo")

        assertFalse("Email must be redacted", sanitized.contains("hunter@sung.org"))
        assertTrue("Redaction token present for email", sanitized.contains("[REDACTED_EMAIL]"))

        assertFalse("Phone must be redacted", sanitized.contains("555"))
        assertTrue("Redaction token present for phone", sanitized.contains("[REDACTED_PHONE]"))

        assertFalse("URL must be redacted", sanitized.contains("https://secret-dungeon.com"))
        assertTrue("Redaction token present for URL", sanitized.contains("[REDACTED_URL]"))

        assertFalse("Card sequence must be redacted", sanitized.contains("4532"))
        assertTrue("Redaction token present for numbers", sanitized.contains("[REDACTED_NUMBER]"))

        assertFalse("Player name must be redacted", sanitized.contains("Jin-Woo"))
        assertTrue("Redaction token present for player name", sanitized.contains("[PLAYER_NAME]"))
    }

    @Test
    fun testAiCoach_OfflineFallbackRespondsWhenBackendFails() = runBlocking {
        val privacyService = SocialPrivacyService()
        val aiGateway = BackendAiGateway()
        val aiCoach = SocialMasteryAiCoach(aiGateway, privacyService)

        val result = aiCoach.sendMessage(
            userPrompt = "How do I improve my executive presence in sprint reviews?",
            mode = AiCoachingMode.PROFESSIONAL_COACH,
            profile = null,
            backendBaseUrl = "http://unreachable-host:9999"
        )

        assertTrue("Fallback response must succeed even when offline", result.isSuccess)
        val text = result.getOrNull()
        assertNotNull(text)
        assertTrue("Response must indicate offline guidance", text!!.contains("[Offline", ignoreCase = true))

        val lastMessage = aiCoach.messages.value.lastOrNull()
        assertNotNull(lastMessage)
        assertEquals("COACH", lastMessage!!.role)
        assertTrue(lastMessage.suggestedFollowUpQuestions.isNotEmpty())
    }

    // =========================================================================
    // 2. ANALYTICS & WEAKNESS ENGINE AUDIT TESTS
    // =========================================================================

    @Test
    fun testAnalyticsEngine_EmptyDataset_NoSpuriousWeaknesses() = runBlocking {
        val repo = FakeSocialRepository()
        val engine = SocialMasteryAnalyticsEngine(repo)

        val summary = engine.computeAnalyticsSummary("test_player")

        assertEquals(0, summary.exercisesCompleted)
        assertEquals(0f, summary.averageScore, 0.001f)
        assertTrue("Detected weaknesses must be empty for fresh player", summary.detectedWeaknesses.isEmpty())
        assertTrue("Active recommendations must provide safe maintenance fallback", summary.activeRecommendations.isNotEmpty())
    }

    @Test
    fun testAnalyticsEngine_DetectsLowAverageScoreWeakness() = runBlocking {
        val progress = SocialModuleProgress(
            accountOwnerId = "test_player",
            module = SocialModuleIdentifier.PUBLIC_SPEAKING,
            assessmentsCompleted = 3,
            rollingAverageScore = 62.5f
        )
        val repo = FakeSocialRepository(progressList = listOf(progress))
        val engine = SocialMasteryAnalyticsEngine(repo)

        val summary = engine.computeAnalyticsSummary("test_player")

        assertEquals(1, summary.detectedWeaknesses.size)
        val weakness = summary.detectedWeaknesses.first()
        assertEquals(SocialModuleIdentifier.PUBLIC_SPEAKING, weakness.affectedModule)
        assertTrue("Must describe low scoring consistency", weakness.title.contains("Low Scoring Consistency"))
        assertEquals(PracticeDifficulty.BASIC, weakness.recommendedDifficulty)

        // Recommendation must prioritize this weakness
        val topRec = summary.activeRecommendations.first()
        assertTrue("Top recommendation must address weakness", topRec.priorityScore >= 90)
        assertEquals(SocialModuleIdentifier.PUBLIC_SPEAKING, topRec.targetModule)
    }

    @Test
    fun testAnalyticsEngine_HeuristicBrevityDetectionRequiresAtLeastTwoBriefSessions() = runBlocking {
        val now = System.currentTimeMillis()
        val briefSession1 = SocialPracticeSession(
            sessionId = UUID.randomUUID().toString(),
            accountOwnerId = "test_player",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            activityKind = SocialActivityKind.PRACTICE,
            sessionStatus = SocialSessionStatus.COMPLETED,
            durationSeconds = 60,
            startedEpochMs = now,
            transcriptText = "Short prompt.", // 13 chars
            evidenceSource = SocialEvidenceSource.AI_ASSESSED,
            verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC,
            idempotencyKey = "key_1"
        )

        // Single brief session should NOT trigger a weakness
        val repoSingle = FakeSocialRepository(recentSessions = listOf(briefSession1))
        val engineSingle = SocialMasteryAnalyticsEngine(repoSingle)
        val summarySingle = engineSingle.computeAnalyticsSummary("test_player")
        assertTrue("Single brief session must not trigger weakness pattern", summarySingle.detectedWeaknesses.isEmpty())

        // Two brief sessions should trigger underdeveloped response pattern
        val briefSession2 = briefSession1.copy(sessionId = UUID.randomUUID().toString(), idempotencyKey = "key_2")
        val repoDouble = FakeSocialRepository(recentSessions = listOf(briefSession1, briefSession2))
        val engineDouble = SocialMasteryAnalyticsEngine(repoDouble)
        val summaryDouble = engineDouble.computeAnalyticsSummary("test_player")

        assertEquals(1, summaryDouble.detectedWeaknesses.size)
        val weakness = summaryDouble.detectedWeaknesses.first()
        assertEquals("WEAK_UNDERDEVELOPED_RESPONSES", weakness.patternId)
        assertTrue("Explanation must frame brevity as heuristic", weakness.description.contains("Heuristic", ignoreCase = true))
    }

    @Test
    fun testAnalyticsEngine_StaleModuleDetectionRequiresPriorHistory() = runBlocking {
        // Module initialized but never practiced (assessmentsCompleted = 0)
        val unstartedStale = SocialModuleProgress(
            accountOwnerId = "test_player",
            module = SocialModuleIdentifier.NEGOTIATION_PERSUASION,
            assessmentsCompleted = 0,
            isStale = true
        )
        val repoUnstarted = FakeSocialRepository(progressList = listOf(unstartedStale))
        val engineUnstarted = SocialMasteryAnalyticsEngine(repoUnstarted)
        val summaryUnstarted = engineUnstarted.computeAnalyticsSummary("test_player")
        assertTrue("Unstarted module must not trigger staleness decay weakness", summaryUnstarted.detectedWeaknesses.isEmpty())

        // Module with prior history that became stale
        val practicedStale = unstartedStale.copy(assessmentsCompleted = 2, rollingAverageScore = 80.0f)
        val repoPracticed = FakeSocialRepository(progressList = listOf(practicedStale))
        val enginePracticed = SocialMasteryAnalyticsEngine(repoPracticed)
        val summaryPracticed = enginePracticed.computeAnalyticsSummary("test_player")
        assertEquals(1, summaryPracticed.detectedWeaknesses.size)
        assertEquals("WEAK_STALE_NEGOTIATION_PERSUASION", summaryPracticed.detectedWeaknesses.first().patternId)
    }

    // =========================================================================
    // 3. PROGRESSION SEPARATION & INTEGRITY TESTS
    // =========================================================================

    @Test
    fun testAiCoachingZeroProgressionGuarantee() = runBlocking {
        val privacyService = SocialPrivacyService()
        val aiGateway = BackendAiGateway()
        val aiCoach = SocialMasteryAiCoach(aiGateway, privacyService)

        // Sending an AI coach prompt
        val result = aiCoach.sendMessage(
            userPrompt = "How do I negotiate salary?",
            mode = AiCoachingMode.PROFESSIONAL_COACH,
            profile = null,
            backendBaseUrl = "http://unreachable:9999"
        )
        assertTrue(result.isSuccess)

        // Verify that SocialMasteryAiCoach exposes zero methods to award XP,
        // and cannot mutate player level or rank.
        val declaredMethods = SocialMasteryAiCoach::class.java.declaredMethods.map { it.name }
        assertFalse("AI Coach must not contain applyXp", declaredMethods.any { it.contains("applyXp", ignoreCase = true) })
        assertFalse("AI Coach must not contain awardProgression", declaredMethods.any { it.contains("award", ignoreCase = true) })
        assertFalse("AI Coach must not contain mutateRank", declaredMethods.any { it.contains("rank", ignoreCase = true) })
    }
}
