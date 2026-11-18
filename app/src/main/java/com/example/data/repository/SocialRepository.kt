package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.RpgRewardAuditEntity
import com.example.data.local.entity.SocialAssessmentEntity
import com.example.data.local.entity.SocialModuleProgressEntity
import com.example.data.local.entity.SocialPracticeSessionEntity
import com.example.data.local.entity.SocialProfileEntity
import com.example.data.local.entity.SocialScenarioAttemptEntity
import com.example.data.local.entity.SocialScenarioEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.engine.ActivityRewardCalculation
import com.example.domain.engine.ProgressionCommand
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.ProgressionOrchestrator
import com.example.domain.engine.SocialMissionCatalog
import com.example.domain.engine.SocialSkillAssessmentEngine
import com.example.domain.model.AttributeType
import com.example.domain.engine.AdaptiveSocialMasteryEngine
import com.example.domain.model.AdaptiveDifficulty
import com.example.domain.model.PersonalizedSocialRecommendation
import com.example.domain.model.PersonalizedWeeklyTrainingPlan
import com.example.domain.model.SocialRecommendationStatus
import com.example.domain.model.ScheduledTrainingActivity
import com.example.domain.model.SocialCompetencyProgress
import com.example.domain.model.SocialMasteryPathway
import com.example.domain.model.SocialWeeklyDevelopmentReport
import com.example.domain.model.NotificationCategory
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RealWorldSocialMission
import com.example.domain.model.SkillAssessmentSubmission
import com.example.domain.model.SocialAssessment
import com.example.domain.model.SocialDifficultyTier
import com.example.domain.model.SocialEvidenceSource
import com.example.domain.model.SocialMasteryStage
import com.example.domain.model.SocialMissionEvidenceType
import com.example.domain.model.SocialMissionLifecycleStatus
import com.example.domain.model.SocialMissionRecord
import com.example.domain.model.SocialModuleIdentifier
import com.example.domain.model.SocialModuleProgress
import com.example.domain.model.SocialPracticeSession
import com.example.domain.model.SocialProfile
import com.example.domain.model.SocialScenario
import com.example.domain.model.SocialScenarioAttempt
import com.example.domain.model.SocialSessionStatus
import com.example.domain.model.SystemNotification
import com.example.domain.model.VerificationStatus
import com.example.domain.model.SocialArtifactValidationState
import com.example.service.AudioLifecycleManager
import com.example.service.SocialArtifactStorageManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

interface SocialRepository {
    suspend fun getOrCreateProfile(accountOwnerId: String): SocialProfile
    fun observeProfile(accountOwnerId: String): Flow<SocialProfile?>
    suspend fun getModuleProgress(accountOwnerId: String, module: SocialModuleIdentifier): SocialModuleProgress
    fun observeAllModuleProgress(accountOwnerId: String): Flow<List<SocialModuleProgress>>
    suspend fun initializeMissingModuleProgress(accountOwnerId: String)
    suspend fun getActiveScenarios(): List<SocialScenario>
    fun observeActiveScenarios(): Flow<List<SocialScenario>>
    suspend fun seedInitialScenariosIfEmpty()
    
    suspend fun completePracticeSession(
        session: SocialPracticeSession,
        rubricScorePercent: Int? = null
    ): Result<Pair<SocialPracticeSession, ActivityRewardCalculation>>

    suspend fun recordScenarioAttempt(
        attempt: SocialScenarioAttempt
    ): Result<SocialScenarioAttempt>

    suspend fun recordAssessment(
        assessment: SocialAssessment
    ): Result<SocialAssessment>

    suspend fun observeRecentSessions(accountOwnerId: String, limit: Int = 30): Flow<List<SocialPracticeSession>>
    suspend fun calculateTotalPracticeSeconds(accountOwnerId: String): Long
    suspend fun deletePracticeSession(sessionId: String, accountOwnerId: String): Result<Unit>

    // Phase 4: Real-World Social Missions & Skill Assessments
    suspend fun startMission(accountOwnerId: String, missionId: String): Result<SocialMissionRecord>
    suspend fun postponeMission(recordId: String, accountOwnerId: String): Result<SocialMissionRecord>
    suspend fun cancelMission(recordId: String, accountOwnerId: String): Result<SocialMissionRecord>
    suspend fun retryMission(recordId: String, accountOwnerId: String): Result<SocialMissionRecord>
    suspend fun submitMissionReflection(
        recordId: String,
        accountOwnerId: String,
        evidenceType: SocialMissionEvidenceType,
        reflectionText: String,
        humanConfirmingRole: String? = null,
        artifactNote: String? = null,
        aiFeedbackNotes: String? = null,
        rubricScore: Int? = null,
        artifactAttachmentUri: String? = null,
        artifactBytes: ByteArray? = null,
        artifactFileName: String? = null
    ): Result<Pair<SocialMissionRecord, ActivityRewardCalculation>>
    fun observeMissionRecords(accountOwnerId: String): Flow<List<SocialMissionRecord>>
    suspend fun getMissionRecord(recordId: String, accountOwnerId: String): SocialMissionRecord?
    suspend fun recordSkillAssessment(submission: SkillAssessmentSubmission): Result<Pair<SocialAssessment, ActivityRewardCalculation>>

    // Phase 5: Adaptive Mastery Pathways & Personalized Development Engine
    fun observeAdaptiveRecommendations(accountOwnerId: String): Flow<List<PersonalizedSocialRecommendation>>
    suspend fun generateAndSaveRecommendations(accountOwnerId: String): Result<List<PersonalizedSocialRecommendation>>
    suspend fun updateRecommendationStatus(recommendationId: String, accountOwnerId: String, status: SocialRecommendationStatus): Result<Unit>
    fun observeWeeklyTrainingPlan(accountOwnerId: String): Flow<PersonalizedWeeklyTrainingPlan?>
    suspend fun generateWeeklyTrainingPlan(accountOwnerId: String, targetDaysPerWeek: Int, targetMinutesPerDay: Int): Result<PersonalizedWeeklyTrainingPlan>
    suspend fun updateActivityStatus(activityId: String, accountOwnerId: String, isCompleted: Boolean, isSkipped: Boolean): Result<Unit>
    suspend fun rescheduleActivity(activityId: String, accountOwnerId: String, newScheduledDateEpochMs: Long, newDayOfWeek: Int): Result<Unit>
    suspend fun pauseOrResumeTrainingPlan(planId: String, accountOwnerId: String, isPaused: Boolean): Result<Unit>
    fun observeAllPathways(accountOwnerId: String): Flow<List<SocialMasteryPathway>>
    suspend fun getCompetencyProgressList(accountOwnerId: String, module: SocialModuleIdentifier): List<SocialCompetencyProgress>
    suspend fun updateCompetencyDifficultyOverride(competencyId: String, accountOwnerId: String, difficulty: AdaptiveDifficulty): Result<Unit>
    suspend fun generateWeeklyReport(accountOwnerId: String): Result<SocialWeeklyDevelopmentReport>
}

class SocialRepositoryImpl(
    private val database: SoloLevelingDatabase,
    private val audioLifecycleManager: AudioLifecycleManager? = null,
    private val artifactStorageManager: SocialArtifactStorageManager? = null,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: () -> Long = { System.currentTimeMillis() }
) : SocialRepository {

    private val profileDao = database.socialProfileDao()
    private val progressDao = database.socialModuleProgressDao()
    private val scenarioDao = database.socialScenarioDao()
    private val sessionDao = database.socialPracticeSessionDao()
    private val attemptDao = database.socialScenarioAttemptDao()
    private val assessmentDao = database.socialAssessmentDao()
    private val missionDao = database.socialMissionDao()
    private val playerDao = database.playerDao()
    private val notificationDao = database.systemNotificationDao()
    private val recommendationDao = database.socialRecommendationDao()
    private val trainingPlanDao = database.socialTrainingPlanDao()
    private val competencyProgressDao = database.socialCompetencyProgressDao()
    private val auditDao = database.rpgAuditDao()
    private val rewardMutex = Mutex()

    override suspend fun getOrCreateProfile(accountOwnerId: String): SocialProfile {
        val existing = profileDao.getProfile(accountOwnerId)
        if (existing != null) return existing.toDomain()

        val now = clock()
        val newProfile = SocialProfile(
            accountOwnerId = accountOwnerId,
            overallSocialMasteryStage = SocialMasteryStage.NOVICE,
            lastActiveEpochMs = now,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        profileDao.insertOrUpdateProfile(newProfile.toEntity())
        initializeMissingModuleProgress(accountOwnerId)
        return newProfile
    }

    override fun observeProfile(accountOwnerId: String): Flow<SocialProfile?> {
        return profileDao.observeProfile(accountOwnerId).map { it?.toDomain() }
    }

    override suspend fun getModuleProgress(accountOwnerId: String, module: SocialModuleIdentifier): SocialModuleProgress {
        val existing = progressDao.getProgress(accountOwnerId, module.code)
        if (existing != null) return existing.toDomain()

        val now = clock()
        val initialized = SocialModuleProgress(
            accountOwnerId = accountOwnerId,
            module = module,
            masteryStage = SocialMasteryStage.NOVICE,
            lastAssessedEpochMs = now
        )
        progressDao.insertOrUpdateProgress(initialized.toEntity())
        return initialized
    }

    override fun observeAllModuleProgress(accountOwnerId: String): Flow<List<SocialModuleProgress>> {
        return progressDao.observeAllModuleProgress(accountOwnerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun initializeMissingModuleProgress(accountOwnerId: String) {
        val now = clock()
        val allModules = SocialModuleIdentifier.entries
        val existing = progressDao.getAllForOwner(accountOwnerId).map { it.moduleName }.toSet()

        val missing = allModules.filter { it.code !in existing }.map { mod ->
            SocialModuleProgressEntity(
                accountOwnerId = accountOwnerId,
                moduleName = mod.code,
                masteryStage = SocialMasteryStage.NOVICE.name,
                masteryStageOrdinal = SocialMasteryStage.NOVICE.ordinalValue,
                lastAssessedEpochMs = now,
                updatedAtEpochMs = now
            )
        }
        if (missing.isNotEmpty()) {
            progressDao.insertAllIgnore(missing)
        }
    }

    override suspend fun getActiveScenarios(): List<SocialScenario> {
        return scenarioDao.getScenariosByDifficulty(SocialDifficultyTier.BASIC.name).map { it.toDomain() }
    }

    override fun observeActiveScenarios(): Flow<List<SocialScenario>> {
        return scenarioDao.observeActiveScenarios().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun seedInitialScenariosIfEmpty() {
        if (scenarioDao.getScenarioCount() > 0) return

        val defaultScenarios = listOf(
            SocialScenarioEntity(
                scenarioId = "SCN_ACTIVE_LISTEN_01",
                moduleName = SocialModuleIdentifier.COMMUNICATION_MASTERY.code,
                title = "Clarifying Project Misalignment",
                description = "A colleague brings vague complaints regarding a recent delivery. Practice reflective listening and non-defensive clarification.",
                difficultyTier = SocialDifficultyTier.BASIC.name,
                contextPrompt = "Your team member says: 'The task breakdown wasn't clear, and I felt blocked all morning.'",
                npcRole = "Blocked Colleague",
                targetLearningObjective = "Validate frustration, mirror key concerns, and ask open clarifying questions.",
                minPassingScore = 70
            ),
            SocialScenarioEntity(
                scenarioId = "SCN_PUBLIC_SPEAK_01",
                moduleName = SocialModuleIdentifier.PUBLIC_SPEAKING.code,
                title = "2-Minute Technical Standup",
                description = "Deliver a crisp, structured standup summary without filler words.",
                difficultyTier = SocialDifficultyTier.BASIC.name,
                contextPrompt = "Deliver yesterday's progress, today's priority, and key blockers to leadership in under 2 minutes.",
                npcRole = "Sprint Facilitator",
                targetLearningObjective = "Eliminate filler words and maintain steady rhetorical cadence.",
                minPassingScore = 75
            ),
            SocialScenarioEntity(
                scenarioId = "SCN_NEGOTIATION_01",
                moduleName = SocialModuleIdentifier.NEGOTIATION_PERSUASION.code,
                title = "Scope Creep Defense",
                description = "A stakeholder asks for emergency features 2 days before deployment. Protect sprint integrity while offering a constructive tradeoff.",
                difficultyTier = SocialDifficultyTier.INTERMEDIATE.name,
                contextPrompt = "Stakeholder: 'We have to include the new reporting dashboard this Friday, no exceptions.'",
                npcRole = "Urgent Stakeholder",
                targetLearningObjective = "Establish boundary, offer trade-off choices, avoid defensive hostility.",
                minPassingScore = 75
            ),
            SocialScenarioEntity(
                scenarioId = "SCN_LEADERSHIP_01",
                moduleName = SocialModuleIdentifier.LEADERSHIP_TEAMWORK.code,
                title = "Constructive Corrective Feedback",
                description = "Provide private feedback to a peer whose late commits broke the staging pipeline.",
                difficultyTier = SocialDifficultyTier.INTERMEDIATE.name,
                contextPrompt = "Peer committed untested changes directly to main, causing a broken build.",
                npcRole = "Junior Developer",
                targetLearningObjective = "Focus on behavior impact rather than character; establish supportive accountability.",
                minPassingScore = 75
            ),
            SocialScenarioEntity(
                scenarioId = "SCN_EMOTIONAL_INTEL_01",
                moduleName = SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE.code,
                title = "High-Tension Disagreement",
                description = "A partner or colleague is angry after an unexpected scheduling conflict.",
                difficultyTier = SocialDifficultyTier.BASIC.name,
                contextPrompt = "Other person: 'You always change plans at the last second!'",
                npcRole = "Agitated Partner",
                targetLearningObjective = "De-escalate emotional reactivity, label the emotion, acknowledge impact before explaining logistics.",
                minPassingScore = 70
            )
        )
        scenarioDao.insertAllIgnore(defaultScenarios)
    }

    override suspend fun completePracticeSession(
        session: SocialPracticeSession,
        rubricScorePercent: Int?
    ): Result<Pair<SocialPracticeSession, ActivityRewardCalculation>> {
        val now = clock()
        val durationMinutes = (session.durationSeconds / 60).coerceIn(1, 180)

        return runCatching {
            rewardMutex.withLock {
                database.withTransaction {
                    val existingOwner = sessionDao.getOwnerBySessionId(session.sessionId)
                    if (existingOwner != null && existingOwner != session.accountOwnerId) {
                        throw SecurityException("Session ownership mismatch for ${session.sessionId}")
                    }
                    val existingAudit = auditDao.getByRewardKey(session.idempotencyKey)
                    val existingCount = sessionDao.countByIdempotencyKey(session.idempotencyKey)
                    if (existingAudit != null || existingCount > 0) {
                        val existing = sessionDao.getSessionById(session.sessionId, session.accountOwnerId)?.toDomain()
                            ?: session
                        return@withTransaction existing to ActivityRewardCalculation(
                            xpAwarded = 0,
                            primaryAttribute = session.module.primaryAttribute,
                            primaryAttrXpAwarded = 0,
                            secondaryAttribute = session.module.secondaryAttribute,
                            secondaryAttrXpAwarded = 0,
                            wasCapped = true,
                            validationNote = "Idempotent replay: session already recorded."
                        )
                    }

                    val usage = computeDailySocialUsageLocked(session.accountOwnerId, now)
                    val rewardCalc = ProgressionEngine.calculateSocialReward(
                        module = session.module,
                        activityKind = session.activityKind,
                        durationMinutes = durationMinutes,
                        verificationStatus = session.verificationStatus,
                        socialXpAlreadyEarnedToday = usage.xpEarnedToday,
                        selfReportedSocialCountToday = usage.selfReportedCountToday,
                        rubricScorePercent = rubricScorePercent
                    )

                    val currentProfile = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
                    val progressionCmd = ProgressionCommand.fromSocialReward(
                        idempotencyKey = session.idempotencyKey,
                        sourceRecordId = session.sessionId,
                        title = "Social Practice: ${session.module.displayName}",
                        calculation = rewardCalc,
                        verificationStatus = session.verificationStatus
                    )

                    val outcome = ProgressionOrchestrator.applyValidatedReward(
                        currentProfile = currentProfile,
                        command = progressionCmd,
                        alreadyProcessedKeys = emptySet(),
                        nowEpochMs = now
                    ).getOrThrow()

                    // Database-enforced unique lock across repository instances before mutating player XP
                    val auditInserted = tryInsertSocialAuditEntry(
                        rewardKey = session.idempotencyKey,
                        sourceId = session.sessionId,
                        title = progressionCmd.title,
                        accountOwnerId = session.accountOwnerId,
                        calc = rewardCalc,
                        verificationStatus = session.verificationStatus,
                        nowEpochMs = now
                    )
                    if (!auditInserted) {
                        val existing = sessionDao.getSessionById(session.sessionId, session.accountOwnerId)?.toDomain()
                            ?: session
                        return@withTransaction existing to ActivityRewardCalculation(
                            xpAwarded = 0,
                            primaryAttribute = session.module.primaryAttribute,
                            primaryAttrXpAwarded = 0,
                            secondaryAttribute = session.module.secondaryAttribute,
                            secondaryAttrXpAwarded = 0,
                            wasCapped = true,
                            validationNote = "Idempotent replay: session already recorded."
                        )
                    }

                    playerDao.upsertPlayer(outcome.updatedProfile.toEntity())

                    val completedSession = session.copy(
                        sessionStatus = SocialSessionStatus.COMPLETED,
                        completedEpochMs = now,
                        updatedAtEpochMs = now
                    )
                    sessionDao.insertSession(completedSession.toEntity())

                    progressDao.addPracticeSeconds(
                        accountOwnerId = session.accountOwnerId,
                        moduleName = session.module.code,
                        deltaSeconds = session.durationSeconds.toLong(),
                        nowEpochMs = now
                    )

                    refreshProfileSummary(session.accountOwnerId, now)

                    if (rewardCalc.xpAwarded > 0) {
                        notificationDao.insertNotification(
                            SystemNotification(
                                title = "Social Mastery Progression (+${rewardCalc.xpAwarded} XP)",
                                message = "${session.module.displayName}: ${session.activityKind.label}. ${rewardCalc.primaryAttribute.code} +${rewardCalc.primaryAttrXpAwarded} XP.",
                                category = NotificationCategory.QUEST_UPDATE,
                                timestampEpochMs = now
                            ).toEntity()
                        )
                    }

                    completedSession to rewardCalc
                }
            }
        }
    }

    override suspend fun recordScenarioAttempt(attempt: SocialScenarioAttempt): Result<SocialScenarioAttempt> {
        val now = clock()
        return runCatching {
            rewardMutex.withLock {
                database.withTransaction {
                    val existingOwner = attemptDao.getOwnerByAttemptId(attempt.attemptId)
                    if (existingOwner != null && existingOwner != attempt.accountOwnerId) {
                        throw SecurityException("Scenario attempt ownership mismatch for ${attempt.attemptId}")
                    }
                    val existingAudit = auditDao.getByRewardKey(attempt.idempotencyKey)
                    if (existingAudit != null || attemptDao.countByIdempotencyKey(attempt.idempotencyKey) > 0) {
                        return@withTransaction attempt
                    }

                    if (attempt.isPassed) {
                        val usage = computeDailySocialUsageLocked(attempt.accountOwnerId, now)
                        val rewardCalc = ProgressionEngine.calculateSocialReward(
                            module = attempt.module,
                            activityKind = com.example.domain.model.SocialActivityKind.SCENARIO,
                            durationMinutes = 15,
                            verificationStatus = attempt.verificationStatus,
                            socialXpAlreadyEarnedToday = usage.xpEarnedToday,
                            selfReportedSocialCountToday = usage.selfReportedCountToday,
                            rubricScorePercent = attempt.evaluationScore
                        )

                        val currentProfile = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
                        val command = ProgressionCommand.fromSocialReward(
                            idempotencyKey = attempt.idempotencyKey,
                            sourceRecordId = attempt.attemptId,
                            title = "Scenario Cleared: ${attempt.scenarioTitle}",
                            calculation = rewardCalc,
                            verificationStatus = attempt.verificationStatus
                        )

                        val outcome = ProgressionOrchestrator.applyValidatedReward(
                            currentProfile = currentProfile,
                            command = command,
                            alreadyProcessedKeys = emptySet(),
                            nowEpochMs = now
                        ).getOrThrow()

                        val auditInserted = tryInsertSocialAuditEntry(
                            rewardKey = attempt.idempotencyKey,
                            sourceId = attempt.attemptId,
                            title = command.title,
                            accountOwnerId = attempt.accountOwnerId,
                            calc = rewardCalc,
                            verificationStatus = attempt.verificationStatus,
                            nowEpochMs = now
                        )
                        if (!auditInserted) {
                            return@withTransaction attempt
                        }

                        playerDao.upsertPlayer(outcome.updatedProfile.toEntity())
                    }

                    attemptDao.insertAttempt(attempt.toEntity())
                    refreshProfileSummary(attempt.accountOwnerId, now)
                    attempt
                }
            }
        }
    }

    override suspend fun recordAssessment(assessment: SocialAssessment): Result<SocialAssessment> {
        val now = clock()
        return runCatching {
            database.withTransaction {
                val existingOwner = assessmentDao.getOwnerByAssessmentId(assessment.assessmentId)
                if (existingOwner != null && existingOwner != assessment.accountOwnerId) {
                    throw SecurityException("Assessment ownership mismatch for ${assessment.assessmentId}")
                }
                assessmentDao.insertAssessment(assessment.toEntity())
                
                // Update module progress assessment counters
                val progress = progressDao.getProgress(assessment.accountOwnerId, assessment.module.code)
                if (progress != null) {
                    val updatedCount = progress.assessmentsCompleted + 1
                    val newAvg = ((progress.rollingAverageScore * progress.assessmentsCompleted) + assessment.overallScore) / updatedCount
                    progressDao.insertOrUpdateProgress(
                        progress.copy(
                            assessmentsCompleted = updatedCount,
                            rollingAverageScore = newAvg,
                            lastAssessedEpochMs = now,
                            updatedAtEpochMs = now
                        )
                    )
                }

                refreshProfileSummary(assessment.accountOwnerId, now)
                assessment
            }
        }
    }

    override suspend fun observeRecentSessions(accountOwnerId: String, limit: Int): Flow<List<SocialPracticeSession>> {
        return sessionDao.observeRecentSessions(accountOwnerId, limit).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun calculateTotalPracticeSeconds(accountOwnerId: String): Long {
        return sessionDao.getTotalCompletedPracticeSeconds(accountOwnerId)
    }

    override suspend fun deletePracticeSession(sessionId: String, accountOwnerId: String): Result<Unit> {
        return runCatching {
            val session = sessionDao.getSessionById(sessionId, accountOwnerId)
            sessionDao.deleteSession(sessionId, accountOwnerId)
            if (session?.hasAudioRecording == true && session.audioFileName != null) {
                audioLifecycleManager?.deleteRecording(accountOwnerId, session.audioFileName)
            }
            refreshProfileSummary(accountOwnerId, clock())
        }
    }

    private suspend fun refreshProfileSummary(accountOwnerId: String, nowEpochMs: Long) {
        val totalSecs = sessionDao.getTotalCompletedPracticeSeconds(accountOwnerId)
        val clearedScenarios = attemptDao.getUniquePassedScenariosCount(accountOwnerId)
        val assessmentsCount = assessmentDao.getAssessmentCount(accountOwnerId)
        val avgScore = assessmentDao.getAverageOverallScore(accountOwnerId)

        // Evaluate overall social mastery stage based on practice and cleared scenarios
        val overallStage = when {
            totalSecs >= 360 * 60 && clearedScenarios >= 8 -> SocialMasteryStage.MASTER
            totalSecs >= 180 * 60 && clearedScenarios >= 5 -> SocialMasteryStage.ADVANCED
            totalSecs >= 90 * 60 && clearedScenarios >= 2 -> SocialMasteryStage.PROFICIENT
            totalSecs >= 30 * 60 -> SocialMasteryStage.COMPETENT
            else -> SocialMasteryStage.NOVICE
        }

        profileDao.updateSummaryFields(
            accountOwnerId = accountOwnerId,
            practiceMinutes = (totalSecs / 60).toInt(),
            scenariosCleared = clearedScenarios,
            assessmentsCompleted = assessmentsCount,
            avgScore = avgScore,
            overallStage = overallStage.name,
            nowEpochMs = nowEpochMs
        )
    }

    private fun startOfDayEpochMs(epochMs: Long): Long {
        val date: LocalDate = Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun startOfNextDayEpochMs(epochMs: Long): Long {
        val date: LocalDate = Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()
        return date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private data class DailySocialUsage(
        val xpEarnedToday: Int,
        val selfReportedCountToday: Int
    )

    private suspend fun computeDailySocialUsageLocked(
        accountOwnerId: String,
        nowEpochMs: Long
    ): DailySocialUsage {
        val dayStart = startOfDayEpochMs(nowEpochMs)
        val dayEnd = startOfNextDayEpochMs(nowEpochMs)
        val auditedXp = auditDao.sumSocialXpInWindow(
            sourceType = "SOCIAL_MASTERY",
            accountOwnerId = accountOwnerId,
            dayStartEpochMs = dayStart,
            dayEndEpochMs = dayEnd
        )
        val unauditedMissionXp = missionDao.getUnauditedSocialXpInWindow(
            accountOwnerId = accountOwnerId,
            dayStartEpochMs = dayStart,
            dayEndEpochMs = dayEnd
        )
        val auditedSelfReported = auditDao.countSocialSelfReportedInWindow(
            sourceType = "SOCIAL_MASTERY",
            accountOwnerId = accountOwnerId,
            dayStartEpochMs = dayStart,
            dayEndEpochMs = dayEnd
        )
        val unauditedSelfReported = missionDao.countUnauditedSelfReportedInWindow(
            accountOwnerId = accountOwnerId,
            dayStartEpochMs = dayStart,
            dayEndEpochMs = dayEnd
        )
        return DailySocialUsage(
            xpEarnedToday = auditedXp + unauditedMissionXp,
            selfReportedCountToday = auditedSelfReported + unauditedSelfReported
        )
    }

    private suspend fun tryInsertSocialAuditEntry(
        rewardKey: String,
        sourceId: String,
        title: String,
        accountOwnerId: String,
        calc: ActivityRewardCalculation,
        verificationStatus: VerificationStatus,
        nowEpochMs: Long
    ): Boolean {
        val insertedRowId = auditDao.insertAudit(
            RpgRewardAuditEntity(
                rewardKey = rewardKey,
                sourceType = "SOCIAL_MASTERY",
                sourceId = sourceId,
                title = title,
                xpAwarded = calc.xpAwarded,
                primaryAttrName = calc.primaryAttribute.name,
                primaryAttrXp = calc.primaryAttrXpAwarded,
                secondaryAttrName = calc.secondaryAttribute.name,
                secondaryAttrXp = calc.secondaryAttrXpAwarded,
                verificationProvenance = verificationStatus.name,
                wasCapped = calc.wasCapped,
                notes = "accountOwnerId=$accountOwnerId|${calc.validationNote}",
                timestampEpochMs = nowEpochMs,
                accountOwnerId = accountOwnerId
            )
        )
        return insertedRowId != -1L
    }

    // Phase 4: Real-World Social Missions & Skill Assessments Implementations
    override suspend fun startMission(accountOwnerId: String, missionId: String): Result<SocialMissionRecord> {
        val now = clock()
        val mission = SocialMissionCatalog.getMissionById(missionId)
            ?: return Result.failure(IllegalArgumentException("Mission definition not found: $missionId"))

        return runCatching {
            getOrCreateProfile(accountOwnerId)
            val recordId = UUID.randomUUID().toString()
            val record = SocialMissionRecord(
                recordId = recordId,
                accountOwnerId = accountOwnerId,
                missionId = mission.missionId,
                module = mission.module,
                title = mission.title,
                difficultyRank = mission.difficulty,
                lifecycleStatus = SocialMissionLifecycleStatus.IN_PROGRESS,
                startedEpochMs = now,
                evidenceType = SocialMissionEvidenceType.SELF_REPORTED,
                verificationStatus = VerificationStatus.SELF_REPORTED,
                idempotencyKey = "msn_${accountOwnerId}_${missionId}_${recordId}",
                updatedAtEpochMs = now
            )
            missionDao.insertRecord(record.toEntity())
            record
        }
    }

    override suspend fun postponeMission(recordId: String, accountOwnerId: String): Result<SocialMissionRecord> {
        val now = clock()
        return runCatching {
            val entity = missionDao.getRecordById(recordId, accountOwnerId)
                ?: throw IllegalArgumentException("Mission record not found: $recordId")
            val currentStatus = SocialMissionLifecycleStatus.fromString(entity.lifecycleStatus)
            if (!currentStatus.canTransitionTo(SocialMissionLifecycleStatus.POSTPONED)) {
                throw IllegalStateException("Invalid mission transition: cannot move from $currentStatus to POSTPONED")
            }
            val updated = entity.copy(
                lifecycleStatus = SocialMissionLifecycleStatus.POSTPONED.name,
                updatedAtEpochMs = now
            )
            missionDao.updateRecord(updated)
            updated.toDomain()
        }
    }

    override suspend fun cancelMission(recordId: String, accountOwnerId: String): Result<SocialMissionRecord> {
        val now = clock()
        return runCatching {
            val entity = missionDao.getRecordById(recordId, accountOwnerId)
                ?: throw IllegalArgumentException("Mission record not found: $recordId")
            val currentStatus = SocialMissionLifecycleStatus.fromString(entity.lifecycleStatus)
            if (!currentStatus.canTransitionTo(SocialMissionLifecycleStatus.CANCELLED)) {
                throw IllegalStateException("Invalid mission transition: cannot move from $currentStatus to CANCELLED")
            }
            val updated = entity.copy(
                lifecycleStatus = SocialMissionLifecycleStatus.CANCELLED.name,
                updatedAtEpochMs = now
            )
            missionDao.updateRecord(updated)
            updated.toDomain()
        }
    }

    override suspend fun retryMission(recordId: String, accountOwnerId: String): Result<SocialMissionRecord> {
        val now = clock()
        return runCatching {
            val entity = missionDao.getRecordById(recordId, accountOwnerId)
                ?: throw IllegalArgumentException("Mission record not found: $recordId")
            val currentStatus = SocialMissionLifecycleStatus.fromString(entity.lifecycleStatus)
            if (!currentStatus.canTransitionTo(SocialMissionLifecycleStatus.RETRY)) {
                throw IllegalStateException("Invalid mission transition: cannot retry mission from status $currentStatus")
            }
            val updated = entity.copy(
                lifecycleStatus = SocialMissionLifecycleStatus.IN_PROGRESS.name,
                aiFeedbackNotes = "Mission retry initiated. Ready for revised execution and reflection.",
                updatedAtEpochMs = now
            )
            missionDao.updateRecord(updated)
            updated.toDomain()
        }
    }

    override suspend fun submitMissionReflection(
        recordId: String,
        accountOwnerId: String,
        evidenceType: SocialMissionEvidenceType,
        reflectionText: String,
        humanConfirmingRole: String?,
        artifactNote: String?,
        aiFeedbackNotes: String?,
        rubricScore: Int?,
        artifactAttachmentUri: String?,
        artifactBytes: ByteArray?,
        artifactFileName: String?
    ): Result<Pair<SocialMissionRecord, ActivityRewardCalculation>> {
        val now = clock()
        return runCatching {
            rewardMutex.withLock {
                var persistedArtifactPathToRollback: String? = null
                try {
                    database.withTransaction {
                        val existingOwner = missionDao.getOwnerByRecordId(recordId)
                        if (existingOwner != null && existingOwner != accountOwnerId) {
                            throw SecurityException("Mission ownership mismatch for $recordId")
                        }
                        val entity = missionDao.getRecordById(recordId, accountOwnerId)
                            ?: throw IllegalArgumentException("Mission record not found: $recordId")

                        val mission = SocialMissionCatalog.getMissionById(entity.missionId)
                            ?: throw IllegalArgumentException("Mission definition not found: ${entity.missionId}")

                        val currentStatus = SocialMissionLifecycleStatus.fromString(entity.lifecycleStatus)
                        val stableRewardKey = "msn_reward_${entity.recordId}"

                        // Idempotency: check both domain table and progression audit ledger
                        if ((currentStatus == SocialMissionLifecycleStatus.COMPLETED && entity.xpAwarded > 0) ||
                            missionDao.countByIdempotencyKey(stableRewardKey) > 0 ||
                            auditDao.getByRewardKey(stableRewardKey) != null
                        ) {
                            return@withTransaction entity.toDomain() to ActivityRewardCalculation(
                                xpAwarded = 0,
                                primaryAttribute = mission.module.primaryAttribute,
                                primaryAttrXpAwarded = 0,
                                secondaryAttribute = mission.module.secondaryAttribute,
                                secondaryAttrXpAwarded = 0,
                                wasCapped = true,
                                validationNote = "Idempotent replay: mission reflection already evaluated."
                            )
                        }

                        // Validate state machine transition to SUBMITTED
                        if (!currentStatus.canTransitionTo(SocialMissionLifecycleStatus.SUBMITTED)) {
                            throw IllegalStateException("Invalid mission lifecycle transition: cannot submit reflection from status $currentStatus")
                        }

                        // Validate reflection text brevity (minimum 20 characters required)
                        if (reflectionText.trim().length < 20) {
                            val needsImprovementRecord = entity.copy(
                                lifecycleStatus = SocialMissionLifecycleStatus.NEEDS_IMPROVEMENT.name,
                                reflectionText = reflectionText,
                                evidenceType = evidenceType.name,
                                humanConfirmingRole = humanConfirmingRole,
                                artifactNote = artifactNote,
                                aiFeedbackNotes = "Reflection rejected: minimum 20 characters required for meaningful social reflection.",
                                xpAwarded = 0,
                                artifactStateName = if (!artifactNote.isNullOrBlank()) SocialArtifactValidationState.ARTIFACT_DESCRIBED.name else SocialArtifactValidationState.NONE.name,
                                updatedAtEpochMs = now
                            )
                            missionDao.updateRecord(needsImprovementRecord)
                            return@withTransaction needsImprovementRecord.toDomain() to ActivityRewardCalculation(
                                xpAwarded = 0,
                                primaryAttribute = mission.module.primaryAttribute,
                                primaryAttrXpAwarded = 0,
                                secondaryAttribute = mission.module.secondaryAttribute,
                                secondaryAttrXpAwarded = 0,
                                wasCapped = false,
                                validationNote = "Reflection text too brief (minimum 20 characters required). Mission marked as NEEDS_IMPROVEMENT."
                            )
                        }

                        // Validate & ingest real-world artifact attachment when ARTIFACT_ATTACHED is claimed.
                        // Note-only submissions MUST use ARTIFACT_DESCRIBED and never qualify as attached files.
                        var resolvedArtifactUri: String? = null
                        var resolvedArtifactOwnerId: String? = null
                        var resolvedArtifactHash: String? = null
                        var resolvedArtifactState = SocialArtifactValidationState.NONE

                        when (evidenceType) {
                            SocialMissionEvidenceType.ARTIFACT_ATTACHED -> {
                                val manager = artifactStorageManager
                                    ?: throw IllegalStateException("Artifact storage manager is unavailable to verify attachment retrievability.")

                                val hasBytes = artifactBytes != null
                                val cleanUri = artifactAttachmentUri?.trim()?.takeIf { it.isNotEmpty() }
                                if (!hasBytes && cleanUri == null) {
                                    throw IllegalArgumentException(
                                        "ARTIFACT_ATTACHED requires an attached, retrievable file. Use ARTIFACT_DESCRIBED for note-only submissions."
                                    )
                                }

                                val stored = if (artifactBytes != null) {
                                    manager.ingestFromBytes(
                                        accountOwnerId = accountOwnerId,
                                        recordId = entity.recordId,
                                        fileName = artifactFileName ?: "evidence.bin",
                                        bytes = artifactBytes
                                    ).getOrThrow()
                                } else {
                                    val uriString = cleanUri!!
                                    if (uriString.startsWith("content://", ignoreCase = true)) {
                                        manager.ingestFromUri(
                                            accountOwnerId = accountOwnerId,
                                            recordId = entity.recordId,
                                            uri = android.net.Uri.parse(uriString),
                                            declaredFileName = artifactFileName
                                        ).getOrThrow()
                                    } else {
                                        val sourceFile = File(uriString)
                                        // Guard against reading another account's vault artifact directory
                                        manager.validateSourcePathAllowedForAccount(accountOwnerId, sourceFile)
                                        if (!sourceFile.exists() || !sourceFile.isFile || !sourceFile.canRead() || sourceFile.length() <= 0L) {
                                            throw IllegalArgumentException("Attached artifact reference does not exist or cannot be read.")
                                        }
                                        val bytes = sourceFile.readBytes()
                                        manager.ingestFromBytes(
                                            accountOwnerId = accountOwnerId,
                                            recordId = entity.recordId,
                                            fileName = artifactFileName ?: sourceFile.name,
                                            bytes = bytes
                                        ).getOrThrow()
                                    }
                                }
                                persistedArtifactPathToRollback = stored.artifactUri

                                // Prevent reusing identical artifact file hashes across multiple missions for the same account
                                val dupCount = missionDao.countByArtifactHash(
                                    accountOwnerId = accountOwnerId,
                                    artifactHash = stored.artifactContentHash,
                                    excludeRecordId = entity.recordId
                                )
                                if (dupCount > 0) {
                                    throw IllegalArgumentException("Duplicate artifact rejected: this file has already been attached to another mission.")
                                }

                                resolvedArtifactUri = stored.artifactUri
                                resolvedArtifactOwnerId = stored.artifactOwnerId
                                resolvedArtifactHash = stored.artifactContentHash
                                resolvedArtifactState = SocialArtifactValidationState.ARTIFACT_REVIEWED
                            }
                            SocialMissionEvidenceType.ARTIFACT_DESCRIBED -> {
                                if (artifactNote.isNullOrBlank()) {
                                    throw IllegalArgumentException("Please enter an artifact title or summary note for ARTIFACT_DESCRIBED.")
                                }
                                resolvedArtifactState = SocialArtifactValidationState.ARTIFACT_DESCRIBED
                            }
                            else -> {
                                resolvedArtifactState = if (!artifactNote.isNullOrBlank()) {
                                    SocialArtifactValidationState.ARTIFACT_DESCRIBED
                                } else {
                                    SocialArtifactValidationState.NONE
                                }
                            }
                        }

                        val verificationStatus = evidenceType.verificationTier

                        // Check if mission requires review (e.g. HUMAN_CONFIRMED with PENDING_REVIEW tier)
                        if (verificationStatus == VerificationStatus.PENDING_REVIEW) {
                            val underReviewRecord = entity.copy(
                                lifecycleStatus = SocialMissionLifecycleStatus.UNDER_REVIEW.name,
                                submittedEpochMs = now,
                                evidenceType = evidenceType.name,
                                reflectionText = reflectionText,
                                humanConfirmingRole = humanConfirmingRole,
                                artifactNote = artifactNote,
                                aiFeedbackNotes = "Mission submitted and pending review. Independent confirmation is unsupported in offline local vault.",
                                xpAwarded = 0,
                                verificationStatus = verificationStatus.name,
                                artifactUri = resolvedArtifactUri,
                                artifactOwnerId = resolvedArtifactOwnerId,
                                artifactContentHash = resolvedArtifactHash,
                                artifactStateName = resolvedArtifactState.name,
                                updatedAtEpochMs = now
                            )
                            missionDao.updateRecord(underReviewRecord)
                            return@withTransaction underReviewRecord.toDomain() to ActivityRewardCalculation(
                                xpAwarded = 0,
                                primaryAttribute = mission.module.primaryAttribute,
                                primaryAttrXpAwarded = 0,
                                secondaryAttribute = mission.module.secondaryAttribute,
                                secondaryAttrXpAwarded = 0,
                                wasCapped = false,
                                validationNote = "Mission submitted and under review. XP pending verification."
                            )
                        }

                        // 1. Authoritative Progression calculation with unified daily caps
                        val usage = computeDailySocialUsageLocked(accountOwnerId, now)
                        val durationMinutes = mission.estimatedDurationMinutes.coerceIn(5, 120)

                        val rewardCalc = ProgressionEngine.calculateSocialReward(
                            module = mission.module,
                            activityKind = com.example.domain.model.SocialActivityKind.REAL_WORLD_ACTIVITY,
                            durationMinutes = durationMinutes,
                            verificationStatus = verificationStatus,
                            socialXpAlreadyEarnedToday = usage.xpEarnedToday,
                            selfReportedSocialCountToday = usage.selfReportedCountToday,
                            rubricScorePercent = rubricScore ?: 80
                        )

                        // 2. Stable reward event idempotency key (no timestamp leak)
                        val currentProfile = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
                        val progressionCmd = ProgressionCommand.fromSocialReward(
                            idempotencyKey = stableRewardKey,
                            sourceRecordId = entity.recordId,
                            title = "Real-World Mission: ${mission.title}",
                            calculation = rewardCalc,
                            verificationStatus = verificationStatus
                        )

                        val outcome = ProgressionOrchestrator.applyValidatedReward(
                            currentProfile = currentProfile,
                            command = progressionCmd,
                            alreadyProcessedKeys = emptySet(),
                            nowEpochMs = now
                        ).getOrThrow()

                        // Insert unique DB audit lock FIRST before crediting player_profile
                        val auditInserted = tryInsertSocialAuditEntry(
                            rewardKey = stableRewardKey,
                            sourceId = entity.recordId,
                            title = progressionCmd.title,
                            accountOwnerId = accountOwnerId,
                            calc = rewardCalc,
                            verificationStatus = verificationStatus,
                            nowEpochMs = now
                        )
                        if (!auditInserted) {
                            return@withTransaction entity.toDomain() to ActivityRewardCalculation(
                                xpAwarded = 0,
                                primaryAttribute = mission.module.primaryAttribute,
                                primaryAttrXpAwarded = 0,
                                secondaryAttribute = mission.module.secondaryAttribute,
                                secondaryAttrXpAwarded = 0,
                                wasCapped = true,
                                validationNote = "Idempotent replay: mission reflection already evaluated."
                            )
                        }

                        playerDao.upsertPlayer(outcome.updatedProfile.toEntity())

                        // 3. Update Mission Record Entity
                        val completedRecord = entity.copy(
                            lifecycleStatus = SocialMissionLifecycleStatus.COMPLETED.name,
                            submittedEpochMs = now,
                            completedEpochMs = now,
                            evidenceType = evidenceType.name,
                            reflectionText = reflectionText,
                            humanConfirmingRole = humanConfirmingRole,
                            artifactNote = artifactNote,
                            aiFeedbackNotes = aiFeedbackNotes,
                            evaluationRubricScore = rubricScore ?: 80,
                            xpAwarded = rewardCalc.xpAwarded,
                            verificationStatus = verificationStatus.name,
                            idempotencyKey = stableRewardKey,
                            artifactUri = resolvedArtifactUri,
                            artifactOwnerId = resolvedArtifactOwnerId,
                            artifactContentHash = resolvedArtifactHash,
                            artifactStateName = resolvedArtifactState.name,
                            updatedAtEpochMs = now
                        )
                        missionDao.updateRecord(completedRecord)

                        // 4. Update module progress
                        progressDao.addPracticeSeconds(
                            accountOwnerId = accountOwnerId,
                            moduleName = mission.module.code,
                            deltaSeconds = durationMinutes * 60L,
                            nowEpochMs = now
                        )

                        refreshProfileSummary(accountOwnerId, now)

                        // 5. System Notification
                        if (rewardCalc.xpAwarded > 0) {
                            notificationDao.insertNotification(
                                SystemNotification(
                                    title = "Social Mission Cleared (+${rewardCalc.xpAwarded} XP)",
                                    message = "${mission.title}: ${mission.difficulty.label}. ${rewardCalc.primaryAttribute.code} +${rewardCalc.primaryAttrXpAwarded} XP.",
                                    category = NotificationCategory.QUEST_UPDATE,
                                    timestampEpochMs = now
                                ).toEntity()
                            )
                        }

                        persistedArtifactPathToRollback = null
                        completedRecord.toDomain() to rewardCalc
                    }
                } catch (t: Throwable) {
                    if (persistedArtifactPathToRollback != null) {
                        artifactStorageManager?.deleteArtifact(accountOwnerId, persistedArtifactPathToRollback)
                    }
                    throw t
                }
            }
        }
    }

    override fun observeMissionRecords(accountOwnerId: String): Flow<List<SocialMissionRecord>> {
        return missionDao.observeRecords(accountOwnerId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getMissionRecord(recordId: String, accountOwnerId: String): SocialMissionRecord? {
        return missionDao.getRecordById(recordId, accountOwnerId)?.toDomain()
    }

    override suspend fun recordSkillAssessment(
        submission: SkillAssessmentSubmission
    ): Result<Pair<SocialAssessment, ActivityRewardCalculation>> {
        val now = clock()
        val eval = SocialSkillAssessmentEngine.evaluateSubmission(submission)
        val isSelfAssessment = submission.classification == com.example.domain.model.SkillAssessmentClassification.SELF_ASSESSMENT
        val stableRewardKey = "assess_reward_${submission.assessmentId}"

        return runCatching {
            rewardMutex.withLock {
                database.withTransaction {
                    val existingOwner = assessmentDao.getOwnerByAssessmentId(submission.assessmentId)
                    if (existingOwner != null && existingOwner != submission.accountOwnerId) {
                        throw SecurityException("Assessment ownership mismatch for ${submission.assessmentId}")
                    }

                    val domainAssessment = SocialAssessment(
                        assessmentId = submission.assessmentId,
                        accountOwnerId = submission.accountOwnerId,
                        module = submission.module,
                        assessmentType = com.example.domain.model.SocialAssessmentType.COMPREHENSIVE_RUBRIC,
                        overallScore = eval.overallScore,
                        clarityScore = submission.scoreDim1,
                        empathyScore = submission.scoreDim2,
                        assertivenessScore = submission.scoreDim3,
                        coherenceScore = submission.scoreDim4,
                        qualitativeAnalysis = eval.qualitativeSummary,
                        actionableRecommendations = eval.actionableRecommendations,
                        assessedEpochMs = now
                    )

                    // 0. Idempotency Check: check assessment table (account-scoped) AND audit ledger
                    val existingAssessment = assessmentDao.getAssessmentById(submission.assessmentId, submission.accountOwnerId)
                    val existingAudit = if (!isSelfAssessment) auditDao.getByRewardKey(stableRewardKey) else null
                    if (existingAssessment != null || existingAudit != null) {
                        val returnedAssessment = existingAssessment?.toDomain() ?: domainAssessment
                        return@withTransaction returnedAssessment to ActivityRewardCalculation(
                            xpAwarded = 0,
                            primaryAttribute = submission.module.primaryAttribute,
                            primaryAttrXpAwarded = 0,
                            secondaryAttribute = submission.module.secondaryAttribute,
                            secondaryAttrXpAwarded = 0,
                            wasCapped = true,
                            validationNote = "Idempotent replay: skill assessment already recorded."
                        )
                    }

                    // 1. Progression Reward (Strict Guard: Self-assessment sliders carry 0 XP and do NOT touch ProgressionOrchestrator or daily cap)
                    val rewardCalc: ActivityRewardCalculation

                    if (isSelfAssessment) {
                        rewardCalc = ActivityRewardCalculation(
                            xpAwarded = 0,
                            primaryAttribute = submission.module.primaryAttribute,
                            primaryAttrXpAwarded = 0,
                            secondaryAttribute = submission.module.secondaryAttribute,
                            secondaryAttrXpAwarded = 0,
                            wasCapped = false,
                            validationNote = "Subjective self-assessment: 0 XP awarded. Perceived confidence rating only."
                        )
                    } else {
                        val usage = computeDailySocialUsageLocked(submission.accountOwnerId, now)

                        val calc = ProgressionEngine.calculateSocialReward(
                            module = submission.module,
                            activityKind = com.example.domain.model.SocialActivityKind.PRACTICE,
                            durationMinutes = 15,
                            verificationStatus = eval.verificationStatus,
                            socialXpAlreadyEarnedToday = usage.xpEarnedToday,
                            selfReportedSocialCountToday = usage.selfReportedCountToday,
                            rubricScorePercent = eval.overallScore
                        )

                        val currentProfile = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
                        val progressionCmd = ProgressionCommand.fromSocialReward(
                            idempotencyKey = stableRewardKey,
                            sourceRecordId = submission.assessmentId,
                            title = "Skill Assessment: ${submission.module.displayName}",
                            calculation = calc,
                            verificationStatus = eval.verificationStatus
                        )

                        val outcome = ProgressionOrchestrator.applyValidatedReward(
                            currentProfile = currentProfile,
                            command = progressionCmd,
                            alreadyProcessedKeys = emptySet(),
                            nowEpochMs = now
                        ).getOrThrow()

                        val auditInserted = tryInsertSocialAuditEntry(
                            rewardKey = stableRewardKey,
                            sourceId = submission.assessmentId,
                            title = progressionCmd.title,
                            accountOwnerId = submission.accountOwnerId,
                            calc = calc,
                            verificationStatus = eval.verificationStatus,
                            nowEpochMs = now
                        )
                        if (!auditInserted) {
                            return@withTransaction domainAssessment to ActivityRewardCalculation(
                                xpAwarded = 0,
                                primaryAttribute = submission.module.primaryAttribute,
                                primaryAttrXpAwarded = 0,
                                secondaryAttribute = submission.module.secondaryAttribute,
                                secondaryAttrXpAwarded = 0,
                                wasCapped = true,
                                validationNote = "Idempotent replay: skill assessment already recorded."
                            )
                        }

                        playerDao.upsertPlayer(outcome.updatedProfile.toEntity())
                        rewardCalc = calc
                    }

                    // 2. Persist Assessment
                    assessmentDao.insertAssessment(domainAssessment.toEntity())

                    // 3. Update Module Progress (Mastery Progression Guard: Self-assessment cannot elevate stage)
                    val progress = progressDao.getProgress(submission.accountOwnerId, submission.module.code)
                    if (progress != null) {
                        val updatedCount = progress.assessmentsCompleted + 1
                        val newAvg = ((progress.rollingAverageScore * progress.assessmentsCompleted) + eval.overallScore) / updatedCount
                        val currentStage = SocialMasteryStage.fromString(progress.masteryStage)

                        val updatedStage = if (isSelfAssessment) {
                            currentStage
                        } else {
                            eval.masteryStage
                        }

                        progressDao.insertOrUpdateProgress(
                            progress.copy(
                                assessmentsCompleted = updatedCount,
                                rollingAverageScore = newAvg,
                                masteryStage = updatedStage.name,
                                masteryStageOrdinal = updatedStage.ordinalValue,
                                lastAssessedEpochMs = now,
                                updatedAtEpochMs = now
                            )
                        )
                    }

                    refreshProfileSummary(submission.accountOwnerId, now)

                    domainAssessment to rewardCalc
                }
            }
        }
    }

    // =========================================================================
    // PHASE 5: ADAPTIVE MASTERY PATHWAYS & PERSONALIZED DEVELOPMENT ENGINE
    // =========================================================================

    override fun observeAdaptiveRecommendations(accountOwnerId: String): Flow<List<PersonalizedSocialRecommendation>> {
        return recommendationDao.observeActiveRecommendations(accountOwnerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    private fun verifyMissionArtifact(mission: SocialMissionRecord): Boolean {
        val manager = artifactStorageManager
        return if (manager != null) {
            manager.verifyRetrievable(mission)
        } else {
            AdaptiveSocialMasteryEngine.defaultArtifactMetadataVerifier(mission)
        }
    }

    private suspend fun evaluateAndPersistCompetencies(
        accountOwnerId: String,
        now: Long,
        missionsOverride: List<SocialMissionRecord>? = null
    ): List<SocialCompetencyProgress> {
        val sessions = sessionDao.observeRecentSessions(accountOwnerId, 200).firstOrNull()?.map { it.toDomain() } ?: emptyList()
        val attempts = attemptDao.observeAttemptsByAccount(accountOwnerId).firstOrNull()?.map { it.toDomain() } ?: emptyList()
        val missions = missionsOverride ?: missionDao.observeRecords(accountOwnerId).firstOrNull()?.map { it.toDomain() } ?: emptyList()
        val assessments = assessmentDao.getAssessmentHistory(accountOwnerId).map { it.toDomain() }
        val priorProgress = competencyProgressDao.getAllProgress(accountOwnerId)
            .map { it.toDomain() }
            .associateBy { it.competencyId }

        val evaluatedCompetencies = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = sessions,
            attempts = attempts,
            missions = missions,
            assessments = assessments,
            priorProgress = priorProgress,
            nowEpochMs = now,
            artifactVerifier = { verifyMissionArtifact(it) }
        )

        competencyProgressDao.insertOrUpdateAll(evaluatedCompetencies.map { it.toEntity() })
        return evaluatedCompetencies
    }

    override suspend fun generateAndSaveRecommendations(accountOwnerId: String): Result<List<PersonalizedSocialRecommendation>> {
        val now = clock()
        return runCatching {
            database.withTransaction {
                getOrCreateProfile(accountOwnerId)
                val missions = missionDao.observeRecords(accountOwnerId).firstOrNull()?.map { it.toDomain() } ?: emptyList()
                val evaluatedCompetencies = evaluateAndPersistCompetencies(accountOwnerId, now, missions)

                val generatedRecs = AdaptiveSocialMasteryEngine.generateRecommendations(
                    accountOwnerId = accountOwnerId,
                    competencies = evaluatedCompetencies,
                    missions = missions,
                    nowEpochMs = now
                )

                val existingRecs = recommendationDao.getAllForOwner(accountOwnerId)
                    .associateBy { it.recommendationId }

                val mergedActive = mutableListOf<PersonalizedSocialRecommendation>()
                for (rec in generatedRecs) {
                    val prev = existingRecs[rec.recommendationId]
                    when (prev?.status) {
                        SocialRecommendationStatus.DISMISSED.name,
                        SocialRecommendationStatus.COMPLETED.name -> {
                            // Preserve user dismissal or completed status across refreshes
                        }
                        SocialRecommendationStatus.ACCEPTED.name -> {
                            mergedActive.add(
                                rec.copy(
                                    status = SocialRecommendationStatus.ACCEPTED,
                                    createdAtEpochMs = prev.createdAtEpochMs,
                                    updatedAtEpochMs = now
                                )
                            )
                        }
                        else -> {
                            mergedActive.add(
                                rec.copy(
                                    createdAtEpochMs = prev?.createdAtEpochMs ?: now,
                                    updatedAtEpochMs = now
                                )
                            )
                        }
                    }
                }

                if (mergedActive.isNotEmpty()) {
                    recommendationDao.insertRecommendations(mergedActive.map { it.toEntity() })
                    recommendationDao.expireSupersededRecommendations(
                        accountOwnerId = accountOwnerId,
                        activeIds = mergedActive.map { it.recommendationId },
                        nowEpochMs = now
                    )
                } else {
                    recommendationDao.expireAllActiveRecommendations(accountOwnerId, now)
                }

                mergedActive
            }
        }
    }

    override suspend fun updateRecommendationStatus(
        recommendationId: String,
        accountOwnerId: String,
        status: SocialRecommendationStatus
    ): Result<Unit> {
        return runCatching {
            recommendationDao.updateStatus(recommendationId, accountOwnerId, status.name, clock())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeWeeklyTrainingPlan(accountOwnerId: String): Flow<PersonalizedWeeklyTrainingPlan?> {
        return trainingPlanDao.observeLatestPlan(accountOwnerId).flatMapLatest { planEntity ->
            if (planEntity == null) {
                flowOf(null)
            } else {
                trainingPlanDao.observeActivitiesForPlan(planEntity.planId, accountOwnerId).map { acts ->
                    planEntity.toDomain(acts.map { it.toDomain() })
                }
            }
        }
    }

    override suspend fun generateWeeklyTrainingPlan(
        accountOwnerId: String,
        targetDaysPerWeek: Int,
        targetMinutesPerDay: Int
    ): Result<PersonalizedWeeklyTrainingPlan> {
        val now = clock()
        return runCatching {
            getOrCreateProfile(accountOwnerId)
            var comps = competencyProgressDao.getAllProgress(accountOwnerId)
                .map { AdaptiveSocialMasteryEngine.enrichWithMaintenanceState(it.toDomain(), now) }
            val needsRecovery = comps.isEmpty() || comps.any {
                it.maintenanceRequired && it.masteryStage == com.example.domain.model.MasteryDevelopmentStage.UNEXPLORED && it.practiceCount > 0
            }
            if (needsRecovery) {
                comps = database.withTransaction {
                    evaluateAndPersistCompetencies(accountOwnerId, now)
                }
            }

            val plan = AdaptiveSocialMasteryEngine.generateWeeklyTrainingPlan(
                accountOwnerId = accountOwnerId,
                targetDaysPerWeek = targetDaysPerWeek,
                targetMinutesPerDay = targetMinutesPerDay,
                competencies = comps,
                nowEpochMs = now
            )

            database.withTransaction {
                trainingPlanDao.insertPlan(plan.toEntity())
                trainingPlanDao.insertActivities(plan.scheduledActivities.map { it.toEntity() })
            }
            plan
        }
    }

    override suspend fun updateActivityStatus(
        activityId: String,
        accountOwnerId: String,
        isCompleted: Boolean,
        isSkipped: Boolean
    ): Result<Unit> {
        return runCatching {
            // Note: marking activity complete or skipping carries 0 XP and causes no penalty.
            val completedTime = if (isCompleted) clock() else null
            trainingPlanDao.updateActivityCompletion(activityId, accountOwnerId, isCompleted, isSkipped, completedTime)
        }
    }

    override suspend fun rescheduleActivity(
        activityId: String,
        accountOwnerId: String,
        newScheduledDateEpochMs: Long,
        newDayOfWeek: Int
    ): Result<Unit> {
        return runCatching {
            trainingPlanDao.rescheduleActivity(activityId, accountOwnerId, newScheduledDateEpochMs, newDayOfWeek)
        }
    }

    override suspend fun pauseOrResumeTrainingPlan(
        planId: String,
        accountOwnerId: String,
        isPaused: Boolean
    ): Result<Unit> {
        return runCatching {
            trainingPlanDao.setPlanPaused(planId, accountOwnerId, isPaused, clock())
        }
    }

    override fun observeAllPathways(accountOwnerId: String): Flow<List<SocialMasteryPathway>> {
        return combine(
            competencyProgressDao.observeAllProgress(accountOwnerId),
            recommendationDao.observeActiveRecommendations(accountOwnerId)
        ) { compEntities, recEntities ->
            val now = clock()
            val comps = compEntities.map {
                AdaptiveSocialMasteryEngine.enrichWithMaintenanceState(it.toDomain(), now)
            }
            val activeRecs = recEntities.map { it.toDomain() }
            AdaptiveSocialMasteryEngine.buildMasteryPathways(comps, activeRecs)
        }
    }

    override suspend fun getCompetencyProgressList(
        accountOwnerId: String,
        module: SocialModuleIdentifier
    ): List<SocialCompetencyProgress> {
        val now = clock()
        val existing = competencyProgressDao.getProgressForModule(accountOwnerId, module.code)
        val hasLegacyRecoveryCandidate = existing.any {
            com.example.domain.model.MasteryDevelopmentStage.isLegacyMaintenance(it.masteryStage) ||
                (it.maintenanceRequired &&
                    it.masteryStage == com.example.domain.model.MasteryDevelopmentStage.UNEXPLORED.name &&
                    it.practiceCount > 0)
        }
        if (hasLegacyRecoveryCandidate) {
            database.withTransaction {
                evaluateAndPersistCompetencies(accountOwnerId, now)
            }
            return competencyProgressDao.getProgressForModule(accountOwnerId, module.code)
                .map { AdaptiveSocialMasteryEngine.enrichWithMaintenanceState(it.toDomain(), now) }
        }
        return existing.map { AdaptiveSocialMasteryEngine.enrichWithMaintenanceState(it.toDomain(), now) }
    }

    override suspend fun updateCompetencyDifficultyOverride(
        competencyId: String,
        accountOwnerId: String,
        difficulty: AdaptiveDifficulty
    ): Result<Unit> {
        return runCatching {
            competencyProgressDao.updateDifficultyOverride(competencyId, accountOwnerId, difficulty.name, clock())
        }
    }

    override suspend fun generateWeeklyReport(accountOwnerId: String): Result<SocialWeeklyDevelopmentReport> {
        val now = clock()
        return runCatching {
            val planEntity = trainingPlanDao.getLatestPlan(accountOwnerId)
            val activities = planEntity?.let {
                trainingPlanDao.getActivitiesForPlan(it.planId, accountOwnerId).map { a -> a.toDomain() }
            } ?: emptyList()
            val plan = planEntity?.toDomain(activities)
            val missions = missionDao.observeRecords(accountOwnerId).firstOrNull()?.map { it.toDomain() } ?: emptyList()
            val sessions = sessionDao.observeRecentSessions(accountOwnerId, 50).firstOrNull()?.map { it.toDomain() } ?: emptyList()

            AdaptiveSocialMasteryEngine.generateWeeklyDevelopmentReport(
                plan = plan,
                missions = missions,
                sessions = sessions,
                nowEpochMs = now
            )
        }
    }
}
