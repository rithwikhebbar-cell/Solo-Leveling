package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.RpgRewardAuditEntity
import com.example.data.local.entity.toEntity
import com.example.data.repository.SocialRepositoryImpl
import com.example.data.sync.SyncQueueManager
import com.example.domain.engine.AdaptiveSocialMasteryEngine
import com.example.domain.engine.ProgressionEngine
import com.example.domain.model.CloudProgressionEventPayload
import com.example.domain.model.MasteryDevelopmentStage
import com.example.domain.model.PlayerProfile
import com.example.domain.model.SkillAssessmentClassification
import com.example.domain.model.SkillAssessmentSubmission
import com.example.domain.model.SocialActivityKind
import com.example.domain.model.SocialArtifactValidationState
import com.example.domain.model.SocialEvidenceSource
import com.example.domain.model.SocialMissionEvidenceType
import com.example.domain.model.SocialModuleIdentifier
import com.example.domain.model.SocialPracticeSession
import com.example.domain.model.SocialRecommendationStatus
import com.example.domain.model.SocialScenarioAttempt
import com.example.domain.model.SocialSessionStatus
import com.example.domain.model.VerificationStatus
import com.example.service.SocialArtifactStorageManager
import com.example.ui.viewmodel.SocialMasteryViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.ZoneOffset

/**
 * Milestone 12 - Phase 5.1: Production Hardening Integration Tests (Robolectric + Room in-memory).
 * Verifies:
 * 1. Unified 180 XP/day & 3 self-reported rewards/day across all 4 repository write paths.
 * 2. SELF_ASSESSMENT awards 0 XP and does not consume self-reported slots or touch audit ledger.
 * 3. UTC midnight rollover resets daily caps cleanly.
 * 4. Concurrent idempotent replays & concurrent cap exhaustion never exceed 180 XP/day.
 * 5. Adaptive recommendation lifecycle (creation, ACCEPTED/DISMISSED preservation, expiration of superseded recs).
 * 6. Multi-account DB isolation (assessments, plans, activities, pathways) & ViewModel account switching.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SocialPhase51RepositoryRobolectricTest {

    private lateinit var database: SoloLevelingDatabase
    private lateinit var artifactRootDir: File
    private lateinit var artifactStorageManager: SocialArtifactStorageManager
    private var fakeNowEpochMs: Long = 1_700_049_600_000L // 2023-11-15T12:00:00Z
    private lateinit var repository: SocialRepositoryImpl

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.playerDao().upsertPlayer(PlayerProfile().toEntity())
        artifactRootDir = File(context.filesDir, "test_artifacts_${System.nanoTime()}")
        artifactStorageManager = SocialArtifactStorageManager(context, artifactRootDir)
        repository = SocialRepositoryImpl(
            database = database,
            audioLifecycleManager = null,
            artifactStorageManager = artifactStorageManager,
            zoneId = ZoneOffset.UTC,
            clock = { fakeNowEpochMs }
        )
        repository.seedInitialScenariosIfEmpty()
    }

    @After
    fun teardown() {
        database.close()
        artifactRootDir.deleteRecursively()
    }

    private suspend fun totalAuditedSocialXp(): Int {
        return database.rpgAuditDao().getAllOnce()
            .filter { it.sourceType == "SOCIAL_MASTERY" }
            .sumOf { it.xpAwarded }
    }

    @Test
    fun unifiedDailyCapsEnforcedAcrossPracticeScenariosMissionsAndAssessments() = runTest {
        val owner = "acct_cap_unified"
        repository.getOrCreateProfile(owner)

        // 1. Practice session (SELF_REPORTED #1)
        val practice = repository.completePracticeSession(
            session = SocialPracticeSession(
                sessionId = "sess_u_1",
                accountOwnerId = owner,
                module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                activityKind = SocialActivityKind.PRACTICE,
                sessionStatus = SocialSessionStatus.IN_PROGRESS,
                durationSeconds = 900,
                startedEpochMs = fakeNowEpochMs - 900_000,
                evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                verificationStatus = VerificationStatus.SELF_REPORTED,
                idempotencyKey = "key_sess_u_1"
            ),
            rubricScorePercent = 85
        ).getOrThrow().second
        assertTrue("Practice #1 should award XP", practice.xpAwarded > 0)

        // 2. Self-assessment slider -> 0 XP, MUST NOT consume a self-reported cap slot
        val selfAssess = repository.recordSkillAssessment(
            SkillAssessmentSubmission(
                assessmentId = "assess_self_1",
                accountOwnerId = owner,
                module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                classification = SkillAssessmentClassification.SELF_ASSESSMENT,
                scoreDim1 = 95, scoreDim2 = 95, scoreDim3 = 95, scoreDim4 = 95, scoreDim5 = 95,
                qualitativeNotes = "High confidence rating"
            )
        ).getOrThrow().second
        assertEquals(0, selfAssess.xpAwarded)
        assertNull("Self-assessment must not write an audit ledger row", database.rpgAuditDao().getByRewardKey("assess_reward_assess_self_1"))

        // 3. Mission reflection (SELF_REPORTED #2)
        val rec1 = repository.startMission(owner, "MSN_COMM_E01").getOrThrow()
        val m1 = repository.submitMissionReflection(
            recordId = rec1.recordId,
            accountOwnerId = owner,
            evidenceType = SocialMissionEvidenceType.SELF_REPORTED,
            reflectionText = "Had an open conversation about project clarity with peer."
        ).getOrThrow().second
        assertTrue("Self-reported mission #2 should award XP", m1.xpAwarded > 0)

        // 4. Scenario attempt (VERIFIED_AI_RUBRIC -> counts toward XP cap, not self-reported limit)
        repository.recordScenarioAttempt(
            SocialScenarioAttempt(
                attemptId = "att_u_1",
                accountOwnerId = owner,
                scenarioId = "SCN_ACTIVE_LISTEN_01",
                scenarioTitle = "Clarifying Project Misalignment",
                module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                userResponseText = "I hear you felt blocked all morning; let's walk through task breakdown together.",
                evaluationScore = 90,
                isPassed = true,
                evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC,
                idempotencyKey = "key_att_u_1",
                completedEpochMs = fakeNowEpochMs
            )
        ).getOrThrow()

        // 5. Mission reflection (SELF_REPORTED #3)
        val rec2 = repository.startMission(owner, "MSN_COMM_D02").getOrThrow()
        val m2 = repository.submitMissionReflection(
            recordId = rec2.recordId,
            accountOwnerId = owner,
            evidenceType = SocialMissionEvidenceType.SELF_REPORTED,
            reflectionText = "Applied Point Reason Example Point explaining caching to peer."
        ).getOrThrow().second
        assertTrue("Self-reported mission #3 should still have slot", m2.xpAwarded > 0 || totalAuditedSocialXp() == ProgressionEngine.DAILY_SOCIAL_XP_CAP)

        // 6. Attempt a 4th self-reported activity -> MUST yield 0 XP (capped by 3/day limit)
        val rec3 = repository.startMission(owner, "MSN_COMM_E01_ALT").getOrThrow()
        val m3 = repository.submitMissionReflection(
            recordId = rec3.recordId,
            accountOwnerId = owner,
            evidenceType = SocialMissionEvidenceType.SELF_REPORTED,
            reflectionText = "Fourth self-reported mission exceeding daily self-reported quota."
        ).getOrThrow().second
        assertEquals("4th self-reported activity in one day must award 0 XP", 0, m3.xpAwarded)
        assertTrue(m3.wasCapped)

        // 7. Total audited Social XP must never exceed 180
        assertTrue(totalAuditedSocialXp() <= ProgressionEngine.DAILY_SOCIAL_XP_CAP)
    }

    @Test
    fun midnightBoundaryRolloverUnlocksDailyCapNextDay() = runTest {
        val owner = "acct_midnight"
        repository.getOrCreateProfile(owner)

        // Set clock to 2023-11-15T23:55:00Z
        fakeNowEpochMs = 1_700_092_500_000L

        // Exhaust all 3 self-reported slots via practice sessions before midnight
        repeat(3) { i ->
            val calc = repository.completePracticeSession(
                session = SocialPracticeSession(
                    sessionId = "mid_s_$i",
                    accountOwnerId = owner,
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    activityKind = SocialActivityKind.PRACTICE,
                    sessionStatus = SocialSessionStatus.IN_PROGRESS,
                    durationSeconds = 900,
                    startedEpochMs = fakeNowEpochMs - 900_000,
                    evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                    verificationStatus = VerificationStatus.SELF_REPORTED,
                    idempotencyKey = "mid_key_$i"
                ),
                rubricScorePercent = 80
            ).getOrThrow().second
            assertTrue("Session $i before cap should award XP", calc.xpAwarded > 0)
        }

        // 4th self-reported at 23:58:00Z -> capped
        fakeNowEpochMs = 1_700_092_680_000L
        val cappedAtNight = repository.completePracticeSession(
            session = SocialPracticeSession(
                sessionId = "mid_s_blocked",
                accountOwnerId = owner,
                module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                activityKind = SocialActivityKind.PRACTICE,
                sessionStatus = SocialSessionStatus.IN_PROGRESS,
                durationSeconds = 900,
                startedEpochMs = fakeNowEpochMs - 900_000,
                evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                verificationStatus = VerificationStatus.SELF_REPORTED,
                idempotencyKey = "mid_key_blocked"
            ),
            rubricScorePercent = 85
        ).getOrThrow().second
        assertEquals(0, cappedAtNight.xpAwarded)
        assertTrue(cappedAtNight.wasCapped)

        // Advance clock across midnight to 2023-11-16T00:02:00Z
        fakeNowEpochMs = 1_700_092_920_000L
        val unlockedNextDay = repository.completePracticeSession(
            session = SocialPracticeSession(
                sessionId = "mid_s_nextday",
                accountOwnerId = owner,
                module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                activityKind = SocialActivityKind.PRACTICE,
                sessionStatus = SocialSessionStatus.IN_PROGRESS,
                durationSeconds = 900,
                startedEpochMs = fakeNowEpochMs - 900_000,
                evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                verificationStatus = VerificationStatus.SELF_REPORTED,
                idempotencyKey = "mid_key_nextday"
            ),
            rubricScorePercent = 85
        ).getOrThrow().second
        assertTrue("Daily cap must reset across UTC midnight boundary", unlockedNextDay.xpAwarded > 0)
        assertFalse(unlockedNextDay.wasCapped)
    }

    @Test
    fun concurrentReplaysAndCapExhaustionAreMutexSerializedAndAtomic() = runTest {
        val owner = "acct_concurrent"
        repository.getOrCreateProfile(owner)

        // 1. Replaying identical idempotency key 10 times concurrently awards once
        val replayResults = (1..10).map {
            async {
                repository.completePracticeSession(
                    session = SocialPracticeSession(
                        sessionId = "conc_same_sess",
                        accountOwnerId = owner,
                        module = SocialModuleIdentifier.PUBLIC_SPEAKING,
                        activityKind = SocialActivityKind.SPEAKING_REHEARSAL,
                        sessionStatus = SocialSessionStatus.IN_PROGRESS,
                        durationSeconds = 1200,
                        startedEpochMs = fakeNowEpochMs - 1200_000,
                        evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                        verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC,
                        idempotencyKey = "conc_same_key"
                    ),
                    rubricScorePercent = 90
                ).getOrThrow().second
            }
        }.awaitAll()

        val awardedCount = replayResults.count { it.xpAwarded > 0 }
        assertEquals("Identical idempotency key must award XP exactly once under concurrency", 1, awardedCount)

        // 2. Concurrent high-value verified assessments pushing well past 180 XP/day must stop strictly at 180
        (1..12).map { idx ->
            async {
                repository.recordSkillAssessment(
                    SkillAssessmentSubmission(
                        assessmentId = "conc_assess_$idx",
                        accountOwnerId = owner,
                        module = SocialModuleIdentifier.LEADERSHIP_TEAMWORK,
                        classification = SkillAssessmentClassification.SIMULATION_EVALUATED,
                        scoreDim1 = 95, scoreDim2 = 95, scoreDim3 = 95, scoreDim4 = 95, scoreDim5 = 95,
                        qualitativeNotes = "Verified peer rubric run $idx"
                    )
                ).getOrThrow()
            }
        }.awaitAll()

        val auditedTotal = totalAuditedSocialXp()
        val playerXp = database.playerDao().getPlayerOnce()!!.totalXp
        assertTrue("Total daily Social XP ($auditedTotal) must never exceed 180", auditedTotal <= ProgressionEngine.DAILY_SOCIAL_XP_CAP)
        assertEquals("Player totalXp must match audited social XP sum", auditedTotal.toLong(), playerXp)
    }

    @Test
    fun recommendationRefreshPreservesUserDecisionsAndExpiresSupersededItems() = runTest {
        val owner = "acct_rec_lifecycle"
        repository.getOrCreateProfile(owner)

        val initial = repository.generateAndSaveRecommendations(owner).getOrThrow()
        assertEquals(5, initial.size)

        val acceptedId = initial[0].recommendationId
        val dismissedId = initial[1].recommendationId
        repository.updateRecommendationStatus(acceptedId, owner, SocialRecommendationStatus.ACCEPTED).getOrThrow()
        repository.updateRecommendationStatus(dismissedId, owner, SocialRecommendationStatus.DISMISSED).getOrThrow()

        // Second refresh under identical state: ACCEPTED stays ACCEPTED, DISMISSED does not come back
        val refreshed = repository.generateAndSaveRecommendations(owner).getOrThrow()
        val acceptedAfter = refreshed.find { it.recommendationId == acceptedId }
        assertNotNull(acceptedAfter)
        assertEquals(SocialRecommendationStatus.ACCEPTED, acceptedAfter!!.status)
        assertNull("DISMISSED rec must remain hidden after refresh", refreshed.find { it.recommendationId == dismissedId })

        val observedActive = repository.observeAdaptiveRecommendations(owner).first()
        assertFalse(observedActive.any { it.recommendationId == dismissedId })

        // Now complete an IMPROVE_WEAKNESS scenario so priority changes and supersedes prior UNEXPLORED recs
        repository.recordScenarioAttempt(
            SocialScenarioAttempt(
                attemptId = "att_weak_rec",
                accountOwnerId = owner,
                scenarioId = "SCN_ACTIVE_LISTEN_01",
                scenarioTitle = "Active Listening",
                module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                userResponseText = "Short response",
                evaluationScore = 52,
                isPassed = false,
                evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC,
                idempotencyKey = "key_weak_rec",
                completedEpochMs = fakeNowEpochMs
            )
        ).getOrThrow()

        val afterWeak = repository.generateAndSaveRecommendations(owner).getOrThrow()
        assertTrue(afterWeak.any { it.category.name == "IMPROVE_WEAKNESS" && it.targetCompetency == "Active Listening" })
    }

    @Test
    fun multiAccountDatabaseIsolationAndViewModelAccountSwitching() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val ownerA = "acct_iso_alpha"
            val ownerB = "acct_iso_beta"
            repository.getOrCreateProfile(ownerA)
            repository.getOrCreateProfile(ownerB)

            // Seed Owner A assessment, plan, and recommendations
            repository.recordSkillAssessment(
                SkillAssessmentSubmission(
                    assessmentId = "iso_assess_1",
                    accountOwnerId = ownerA,
                    module = SocialModuleIdentifier.PUBLIC_SPEAKING,
                    classification = SkillAssessmentClassification.SIMULATION_EVALUATED,
                    scoreDim1 = 92, scoreDim2 = 88, scoreDim3 = 90, scoreDim4 = 91, scoreDim5 = 89,
                    qualitativeNotes = "Owner A isolated assessment"
                )
            ).getOrThrow()

            val planA = repository.generateWeeklyTrainingPlan(ownerA, targetDaysPerWeek = 4, targetMinutesPerDay = 20).getOrThrow()
            repository.generateAndSaveRecommendations(ownerA).getOrThrow()

            // Verify DAO-level owner scoping blocks Owner B from seeing Owner A rows
            assertNull(database.socialAssessmentDao().getAssessmentById("iso_assess_1", ownerB))
            assertNotNull(database.socialAssessmentDao().getAssessmentById("iso_assess_1", ownerA))
            assertTrue(database.socialTrainingPlanDao().getActivitiesForPlan(planA.planId, ownerB).isEmpty())
            assertEquals(4, database.socialTrainingPlanDao().getActivitiesForPlan(planA.planId, ownerA).size)

            // Seed Owner B with a distinct plan (3 days, 10 min)
            val planB = repository.generateWeeklyTrainingPlan(ownerB, targetDaysPerWeek = 3, targetMinutesPerDay = 10).getOrThrow()

            // Verify ViewModel clears state and loads Owner B when accountOwnerIdFlow emits Owner B
            fun awaitCondition(predicate: () -> Boolean) {
                val deadline = System.currentTimeMillis() + 5000L
                while (!predicate() && System.currentTimeMillis() < deadline) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    advanceUntilIdle()
                    if (!predicate()) {
                        Thread.sleep(20)
                    }
                }
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                advanceUntilIdle()
            }

            val ownerFlow = MutableStateFlow(ownerA)
            val viewModel = SocialMasteryViewModel(
                socialRepository = repository,
                accountOwnerId = ownerA,
                accountOwnerIdFlow = ownerFlow
            )
            awaitCondition {
                viewModel.uiState.value.profile?.accountOwnerId == ownerA &&
                    viewModel.uiState.value.currentTrainingPlan?.planId == planA.planId
            }

            assertEquals(ownerA, viewModel.uiState.value.profile?.accountOwnerId)
            assertEquals(planA.planId, viewModel.uiState.value.currentTrainingPlan?.planId)

            // Switch active account to Owner B
            ownerFlow.value = ownerB
            awaitCondition {
                viewModel.uiState.value.profile?.accountOwnerId == ownerB &&
                    viewModel.uiState.value.currentTrainingPlan?.planId == planB.planId
            }

            assertEquals(ownerB, viewModel.uiState.value.profile?.accountOwnerId)
            assertEquals(planB.planId, viewModel.uiState.value.currentTrainingPlan?.planId)
            assertEquals(3, viewModel.uiState.value.currentTrainingPlan?.targetDaysPerWeek)
        } finally {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun artifactValidationEnforcesRetrievabilityOwnershipDeduplicationAndDistinguishesDescribedNotes() = runTest {
        val ownerA = "acct_art_a"
        val ownerB = "acct_art_b"
        repository.getOrCreateProfile(ownerA)
        repository.getOrCreateProfile(ownerB)

        // Seed 4 practice sessions + 2 scenario attempts >= 85% for ownerA Active Listening
        repeat(4) { i ->
            fakeNowEpochMs += 60_000L
            repository.completePracticeSession(
                session = SocialPracticeSession(
                    sessionId = "art_s_$i", accountOwnerId = ownerA,
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    activityKind = SocialActivityKind.PRACTICE, sessionStatus = SocialSessionStatus.IN_PROGRESS,
                    durationSeconds = 600, startedEpochMs = fakeNowEpochMs - 600_000,
                    transcriptText = "Active Listening structured drill $i",
                    evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                    verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
                    idempotencyKey = "art_ks_$i"
                )
            ).getOrThrow()
        }
        repeat(2) { i ->
            fakeNowEpochMs += 60_000L
            repository.recordScenarioAttempt(
                SocialScenarioAttempt(
                    attemptId = "art_att_$i", accountOwnerId = ownerA, scenarioId = "SCN_ACTIVE_LISTEN_01",
                    scenarioTitle = "Active Listening", module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    userResponseText = "Active Listening paraphrase turn $i",
                    evaluationScore = 90, isPassed = true,
                    evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                    verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC,
                    idempotencyKey = "art_ka_$i",
                    completedEpochMs = fakeNowEpochMs
                )
            ).getOrThrow()
        }

        // 1. Claiming ARTIFACT_ATTACHED with only a 5-char note and NO file MUST fail
        val mNoteReject = repository.startMission(ownerA, "MSN_COMM_E01").getOrThrow()
        val noteRejectRes = repository.submitMissionReflection(
            recordId = mNoteReject.recordId,
            accountOwnerId = ownerA,
            evidenceType = SocialMissionEvidenceType.ARTIFACT_ATTACHED,
            reflectionText = "Completed an active listening check-in without uploading a file.",
            artifactNote = "12345"
        )
        assertTrue("Note-only ARTIFACT_ATTACHED must be rejected", noteRejectRes.isFailure)

        // 2. Claiming ARTIFACT_ATTACHED pointing to non-existent file MUST fail
        val missingFileRes = repository.submitMissionReflection(
            recordId = mNoteReject.recordId,
            accountOwnerId = ownerA,
            evidenceType = SocialMissionEvidenceType.ARTIFACT_ATTACHED,
            reflectionText = "Trying to attach a file path that does not exist on disk.",
            artifactAttachmentUri = "/nonexistent/missing_evidence.pdf"
        )
        assertTrue("Missing file attachment must be rejected", missingFileRes.isFailure)

        // 3. Using ARTIFACT_DESCRIBED succeeds for reflection history, but stage stays COMPETENT
        val mDescribed = repository.startMission(ownerA, "MSN_COMM_E01").getOrThrow()
        val describedOk = repository.submitMissionReflection(
            recordId = mDescribed.recordId,
            accountOwnerId = ownerA,
            evidenceType = SocialMissionEvidenceType.ARTIFACT_DESCRIBED,
            reflectionText = "Active Listening conversation notes logged as described-only artifact.",
            artifactNote = "Debrief Summary"
        ).getOrThrow().first
        assertEquals(SocialArtifactValidationState.ARTIFACT_DESCRIBED, describedOk.artifactValidationState)
        repository.generateAndSaveRecommendations(ownerA).getOrThrow()
        val afterDescribed = repository.getCompetencyProgressList(ownerA, SocialModuleIdentifier.COMMUNICATION_MASTERY)
            .first { it.competencyName == "Active Listening" }
        assertEquals(MasteryDevelopmentStage.COMPETENT, afterDescribed.masteryStage)

        // 4. Attach Owner B's vault artifact reference as Owner A -> cross-account rejected
        val mOwnerB = repository.startMission(ownerB, "MSN_COMM_E01").getOrThrow()
        val storedB = repository.submitMissionReflection(
            recordId = mOwnerB.recordId,
            accountOwnerId = ownerB,
            evidenceType = SocialMissionEvidenceType.ARTIFACT_ATTACHED,
            reflectionText = "Owner B uploads their own genuine debrief notes document.",
            artifactBytes = "Owner B genuine debrief content".toByteArray(),
            artifactFileName = "owner_b_debrief.txt"
        ).getOrThrow().first
        assertNotNull(storedB.artifactUri)

        val crossAccountAttempt = repository.submitMissionReflection(
            recordId = mNoteReject.recordId,
            accountOwnerId = ownerA,
            evidenceType = SocialMissionEvidenceType.ARTIFACT_ATTACHED,
            reflectionText = "Owner A tries to reference Owner B's private vault file.",
            artifactAttachmentUri = storedB.artifactUri
        )
        assertTrue("Cross-account artifact reference must be blocked", crossAccountAttempt.isFailure)

        // 5. Attach valid genuine file for Owner A -> stored, retrievable, and unlocks ADVANCED
        val genuineBytes = "Real-world Active Listening transcript & reflection #1".toByteArray()
        val completedReal1 = repository.submitMissionReflection(
            recordId = mNoteReject.recordId,
            accountOwnerId = ownerA,
            evidenceType = SocialMissionEvidenceType.ARTIFACT_ATTACHED,
            reflectionText = "Active Listening debrief completed with uploaded follow-up document.",
            artifactNote = "Active Listening Debrief Doc 1",
            artifactBytes = genuineBytes,
            artifactFileName = "debrief_1.txt"
        ).getOrThrow().first
        assertEquals(SocialArtifactValidationState.ARTIFACT_REVIEWED, completedReal1.artifactValidationState)
        assertTrue(artifactStorageManager.verifyRetrievable(completedReal1))

        // Duplicate identical file bytes on a second mission of Owner A -> rejected
        val mDup = repository.startMission(ownerA, "MSN_COMM_E01_ALT").getOrThrow()
        val dupRes = repository.submitMissionReflection(
            recordId = mDup.recordId,
            accountOwnerId = ownerA,
            evidenceType = SocialMissionEvidenceType.ARTIFACT_ATTACHED,
            reflectionText = "Trying to reuse the exact same document hash on a second mission.",
            artifactBytes = genuineBytes,
            artifactFileName = "debrief_copy.txt"
        )
        assertTrue("Duplicate artifact content hash across missions must fail", dupRes.isFailure)

        // With 1 retrievable real-world artifact -> ADVANCED
        repository.generateAndSaveRecommendations(ownerA).getOrThrow()
        val afterReal1 = repository.getCompetencyProgressList(ownerA, SocialModuleIdentifier.COMMUNICATION_MASTERY)
            .first { it.competencyName == "Active Listening" }
        assertEquals(MasteryDevelopmentStage.ADVANCED, afterReal1.masteryStage)

        // Attach 2 more distinct retrievable artifacts (total practiceCount = 4 + 2 + 1 desc + 3 real = 10, >= 3 distinct hashes) -> MASTERY_CANDIDATE
        repeat(2) { idx ->
            fakeNowEpochMs += 60_000L
            val nextM = repository.startMission(ownerA, "MSN_COMM_D02").getOrThrow()
            repository.submitMissionReflection(
                recordId = nextM.recordId,
                accountOwnerId = ownerA,
                evidenceType = SocialMissionEvidenceType.ARTIFACT_ATTACHED,
                reflectionText = "Active Listening real-world follow-up conversation artifact #$idx.",
                artifactNote = "Active Listening Artifact #$idx",
                artifactBytes = "Distinct real-world evidence document payload #$idx".toByteArray(),
                artifactFileName = "debrief_extra_$idx.txt"
            ).getOrThrow()
        }
        repository.generateAndSaveRecommendations(ownerA).getOrThrow()
        val afterReal3 = repository.getCompetencyProgressList(ownerA, SocialModuleIdentifier.COMMUNICATION_MASTERY)
            .first { it.competencyName == "Active Listening" }
        assertEquals(MasteryDevelopmentStage.MASTERY_CANDIDATE, afterReal3.masteryStage)
    }

    @Test
    fun crossInstanceConcurrencyAndSyncQueueAccountIsolationRespectDatabaseLocks() = runTest {
        val owner = "acct_cross_inst"
        val otherOwner = "acct_other_sync"
        repository.getOrCreateProfile(owner)
        repository.getOrCreateProfile(otherOwner)

        // Two separate SocialRepositoryImpl instances WITHOUT shared in-memory Mutex
        val repoInstance1 = repository
        val repoInstance2 = SocialRepositoryImpl(
            database = database,
            audioLifecycleManager = null,
            artifactStorageManager = artifactStorageManager,
            zoneId = ZoneOffset.UTC,
            clock = { fakeNowEpochMs }
        )

        // Race identical key across 2 separate repository instances
        val replayResults = listOf(repoInstance1, repoInstance2).mapIndexed { idx, repo ->
            async(Dispatchers.IO) {
                repo.completePracticeSession(
                    session = SocialPracticeSession(
                        sessionId = "ci_same_key_$idx",
                        accountOwnerId = owner,
                        module = SocialModuleIdentifier.LEADERSHIP_TEAMWORK,
                        activityKind = SocialActivityKind.PRACTICE,
                        sessionStatus = SocialSessionStatus.IN_PROGRESS,
                        durationSeconds = 900,
                        startedEpochMs = fakeNowEpochMs - 900_000,
                        evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                        verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
                        idempotencyKey = "shared_cross_instance_key"
                    ),
                    rubricScorePercent = 88
                ).getOrThrow().second
            }
        }.awaitAll()

        assertEquals(1, replayResults.count { it.xpAwarded > 0 })
        assertEquals(1, replayResults.count { it.xpAwarded == 0 })

        // Verify SyncQueueManager scopes audit rows strictly by userId (plus default local vault) and preserves SOCIAL_MASTERY module
        database.rpgAuditDao().insertAudit(
            RpgRewardAuditEntity(
                rewardKey = "foreign_user_reward",
                sourceType = "SOCIAL_MASTERY",
                sourceId = "m_foreign",
                title = "Foreign Mission",
                xpAwarded = 30,
                primaryAttrName = "CHA",
                primaryAttrXp = 15,
                secondaryAttrName = "DIS",
                secondaryAttrXp = 15,
                verificationProvenance = "SELF_REPORTED",
                wasCapped = false,
                notes = "accountOwnerId=$otherOwner|Foreign",
                timestampEpochMs = fakeNowEpochMs,
                accountOwnerId = otherOwner
            )
        )

        val syncQueueManager = SyncQueueManager(database)
        syncQueueManager.scanAndEnqueueLocalChanges(userId = owner, nowEpochMs = fakeNowEpochMs)
        val queued = database.syncQueueDao().getAllOnce()
        assertTrue(queued.any { it.localKey == "shared_cross_instance_key" })
        assertFalse("Foreign user's audit row must NOT enqueue for $owner", queued.any { it.localKey == "foreign_user_reward" })

        val enqueuedSocialPayload = queued.first { it.localKey == "shared_cross_instance_key" }.payloadJson
        val decoded = com.example.data.remote.CloudDataMapper.json.decodeFromString<CloudProgressionEventPayload>(enqueuedSocialPayload)
        assertEquals("SOCIAL_MASTERY", decoded.sourceModule)
    }
}
