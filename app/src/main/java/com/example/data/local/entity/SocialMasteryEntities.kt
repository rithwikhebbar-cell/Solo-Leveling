package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.MissionDifficultyRank
import com.example.domain.model.SocialActivityKind
import com.example.domain.model.SocialAssessment
import com.example.domain.model.SocialAssessmentType
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
import com.example.domain.model.VerificationStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Milestone 12: Core Room Database Entities for Social Mastery.
 */

// 1. Root Social Profile Entity
@Entity(tableName = "social_profiles")
data class SocialProfileEntity(
    @PrimaryKey val accountOwnerId: String,
    val overallSocialMasteryStage: String,
    val totalPracticeMinutes: Int = 0,
    val totalScenariosCleared: Int = 0,
    val totalAssessmentsCompleted: Int = 0,
    val averageRubricScore: Float = 0.0f,
    val lastActiveEpochMs: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val syncStatus: String = "SYNCED",
    val lastSyncedEpochMs: Long? = null
)

// 2. Module Progress Entity
@Entity(
    tableName = "social_module_progress",
    primaryKeys = ["accountOwnerId", "moduleName"],
    foreignKeys = [
        ForeignKey(
            entity = SocialProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId"]),
        Index(value = ["moduleName"]),
        Index(value = ["masteryStageOrdinal"])
    ]
)
data class SocialModuleProgressEntity(
    val accountOwnerId: String,
    val moduleName: String,
    val masteryStage: String,
    val masteryStageOrdinal: Int,
    val practiceSeconds: Long = 0L,
    val assessmentsCompleted: Int = 0,
    val rollingAverageScore: Float = 0.0f,
    val lastScoreStandardDeviation: Float = 0.0f,
    val consecutivePassingScores: Int = 0,
    val isStale: Boolean = false,
    val decayedFromStage: String? = null,
    val consecutiveRestorationPassingScores: Int = 0,
    val lastAssessedEpochMs: Long,
    val lastDecayEpochMs: Long? = null,
    val nextDecayEligibleEpochMs: Long? = null,
    val updatedAtEpochMs: Long
)

// 3. Static Scenario Catalogue Entity
@Entity(
    tableName = "social_scenarios",
    indices = [
        Index(value = ["moduleName"]),
        Index(value = ["difficultyTier"])
    ]
)
data class SocialScenarioEntity(
    @PrimaryKey val scenarioId: String,
    val moduleName: String,
    val title: String,
    val description: String,
    val difficultyTier: String,
    val contextPrompt: String,
    val npcRole: String,
    val targetLearningObjective: String,
    val minPassingScore: Int = 70,
    val isDeprecated: Boolean = false,
    val version: Int = 1
)

// 4. Practice Session Entity
@Entity(
    tableName = "social_practice_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SocialProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId"]),
        Index(value = ["moduleName"]),
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["startedEpochMs"])
    ]
)
data class SocialPracticeSessionEntity(
    @PrimaryKey val sessionId: String,
    val accountOwnerId: String,
    val moduleName: String,
    val activityKind: String,
    val sessionStatus: String,
    val durationSeconds: Int,
    val startedEpochMs: Long,
    val completedEpochMs: Long? = null,
    val transcriptText: String? = null,
    val hasAudioRecording: Boolean = false,
    val audioFileName: String? = null,
    val isCloudTranscriptSyncAllowed: Boolean = false,
    val evidenceSourceType: String,
    val verificationStatus: String,
    val idempotencyKey: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

// 5. Scenario Attempt Entity
@Entity(
    tableName = "social_scenario_attempts",
    foreignKeys = [
        ForeignKey(
            entity = SocialProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SocialScenarioEntity::class,
            parentColumns = ["scenarioId"],
            childColumns = ["scenarioId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["accountOwnerId"]),
        Index(value = ["scenarioId"]),
        Index(value = ["moduleName"]),
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["completedEpochMs"])
    ]
)
data class SocialScenarioAttemptEntity(
    @PrimaryKey val attemptId: String,
    val accountOwnerId: String,
    val scenarioId: String,
    val scenarioTitle: String,
    val moduleName: String,
    val userResponseText: String,
    val evaluationScore: Int,
    val isPassed: Boolean,
    val strengthsFeedbackJson: String,
    val improvementAreasJson: String,
    val evidenceSourceType: String,
    val verificationStatus: String,
    val idempotencyKey: String,
    val completedEpochMs: Long
)

// 6. Assessment Entity
@Entity(
    tableName = "social_assessments",
    foreignKeys = [
        ForeignKey(
            entity = SocialProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId"]),
        Index(value = ["moduleName"]),
        Index(value = ["assessedEpochMs"])
    ]
)
data class SocialAssessmentEntity(
    @PrimaryKey val assessmentId: String,
    val accountOwnerId: String,
    val moduleName: String,
    val assessmentType: String,
    val overallScore: Int,
    val clarityScore: Int,
    val empathyScore: Int,
    val assertivenessScore: Int,
    val coherenceScore: Int,
    val qualitativeAnalysis: String,
    val actionableRecommendationsJson: String,
    val assessedEpochMs: Long
)

// Mapping extensions
fun SocialProfileEntity.toDomain() = SocialProfile(
    accountOwnerId = accountOwnerId,
    overallSocialMasteryStage = SocialMasteryStage.fromString(overallSocialMasteryStage),
    totalPracticeMinutes = totalPracticeMinutes,
    totalScenariosCleared = totalScenariosCleared,
    totalAssessmentsCompleted = totalAssessmentsCompleted,
    averageRubricScore = averageRubricScore,
    lastActiveEpochMs = lastActiveEpochMs,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    syncStatus = syncStatus,
    lastSyncedEpochMs = lastSyncedEpochMs
)

fun SocialProfile.toEntity() = SocialProfileEntity(
    accountOwnerId = accountOwnerId,
    overallSocialMasteryStage = overallSocialMasteryStage.name,
    totalPracticeMinutes = totalPracticeMinutes,
    totalScenariosCleared = totalScenariosCleared,
    totalAssessmentsCompleted = totalAssessmentsCompleted,
    averageRubricScore = averageRubricScore,
    lastActiveEpochMs = lastActiveEpochMs,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    syncStatus = syncStatus,
    lastSyncedEpochMs = lastSyncedEpochMs
)

fun SocialModuleProgressEntity.toDomain() = SocialModuleProgress(
    accountOwnerId = accountOwnerId,
    module = SocialModuleIdentifier.fromCode(moduleName),
    masteryStage = SocialMasteryStage.fromString(masteryStage),
    practiceSeconds = practiceSeconds,
    assessmentsCompleted = assessmentsCompleted,
    rollingAverageScore = rollingAverageScore,
    lastScoreStandardDeviation = lastScoreStandardDeviation,
    consecutivePassingScores = consecutivePassingScores,
    isStale = isStale,
    decayedFromStage = decayedFromStage?.let { SocialMasteryStage.fromString(it) },
    consecutiveRestorationPassingScores = consecutiveRestorationPassingScores,
    lastAssessedEpochMs = lastAssessedEpochMs,
    lastDecayEpochMs = lastDecayEpochMs,
    nextDecayEligibleEpochMs = nextDecayEligibleEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun SocialModuleProgress.toEntity() = SocialModuleProgressEntity(
    accountOwnerId = accountOwnerId,
    moduleName = module.code,
    masteryStage = masteryStage.name,
    masteryStageOrdinal = masteryStage.ordinalValue,
    practiceSeconds = practiceSeconds,
    assessmentsCompleted = assessmentsCompleted,
    rollingAverageScore = rollingAverageScore,
    lastScoreStandardDeviation = lastScoreStandardDeviation,
    consecutivePassingScores = consecutivePassingScores,
    isStale = isStale,
    decayedFromStage = decayedFromStage?.name,
    consecutiveRestorationPassingScores = consecutiveRestorationPassingScores,
    lastAssessedEpochMs = lastAssessedEpochMs,
    lastDecayEpochMs = lastDecayEpochMs,
    nextDecayEligibleEpochMs = nextDecayEligibleEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun SocialScenarioEntity.toDomain() = SocialScenario(
    scenarioId = scenarioId,
    module = SocialModuleIdentifier.fromCode(moduleName),
    title = title,
    description = description,
    difficultyTier = SocialDifficultyTier.valueOf(difficultyTier),
    contextPrompt = contextPrompt,
    npcRole = npcRole,
    targetLearningObjective = targetLearningObjective,
    minPassingScore = minPassingScore,
    isDeprecated = isDeprecated,
    version = version
)

fun SocialScenario.toEntity() = SocialScenarioEntity(
    scenarioId = scenarioId,
    moduleName = module.code,
    title = title,
    description = description,
    difficultyTier = difficultyTier.name,
    contextPrompt = contextPrompt,
    npcRole = npcRole,
    targetLearningObjective = targetLearningObjective,
    minPassingScore = minPassingScore,
    isDeprecated = isDeprecated,
    version = version
)

fun SocialPracticeSessionEntity.toDomain() = SocialPracticeSession(
    sessionId = sessionId,
    accountOwnerId = accountOwnerId,
    module = SocialModuleIdentifier.fromCode(moduleName),
    activityKind = SocialActivityKind.valueOf(activityKind),
    sessionStatus = SocialSessionStatus.valueOf(sessionStatus),
    durationSeconds = durationSeconds,
    startedEpochMs = startedEpochMs,
    completedEpochMs = completedEpochMs,
    transcriptText = transcriptText,
    hasAudioRecording = hasAudioRecording,
    audioFileName = audioFileName,
    isCloudTranscriptSyncAllowed = isCloudTranscriptSyncAllowed,
    evidenceSource = SocialEvidenceSource.valueOf(evidenceSourceType),
    verificationStatus = VerificationStatus.valueOf(verificationStatus),
    idempotencyKey = idempotencyKey,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun SocialPracticeSession.toEntity() = SocialPracticeSessionEntity(
    sessionId = sessionId,
    accountOwnerId = accountOwnerId,
    moduleName = module.code,
    activityKind = activityKind.name,
    sessionStatus = sessionStatus.name,
    durationSeconds = durationSeconds,
    startedEpochMs = startedEpochMs,
    completedEpochMs = completedEpochMs,
    transcriptText = transcriptText,
    hasAudioRecording = hasAudioRecording,
    audioFileName = audioFileName,
    isCloudTranscriptSyncAllowed = isCloudTranscriptSyncAllowed,
    evidenceSourceType = evidenceSource.name,
    verificationStatus = verificationStatus.name,
    idempotencyKey = idempotencyKey,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

private val json = Json { ignoreUnknownKeys = true }

fun SocialScenarioAttemptEntity.toDomain() = SocialScenarioAttempt(
    attemptId = attemptId,
    accountOwnerId = accountOwnerId,
    scenarioId = scenarioId,
    scenarioTitle = scenarioTitle,
    module = SocialModuleIdentifier.fromCode(moduleName),
    userResponseText = userResponseText,
    evaluationScore = evaluationScore,
    isPassed = isPassed,
    strengthsFeedback = runCatching { json.decodeFromString<List<String>>(strengthsFeedbackJson) }.getOrDefault(emptyList()),
    improvementAreas = runCatching { json.decodeFromString<List<String>>(improvementAreasJson) }.getOrDefault(emptyList()),
    evidenceSource = SocialEvidenceSource.valueOf(evidenceSourceType),
    verificationStatus = VerificationStatus.valueOf(verificationStatus),
    idempotencyKey = idempotencyKey,
    completedEpochMs = completedEpochMs
)

fun SocialScenarioAttempt.toEntity() = SocialScenarioAttemptEntity(
    attemptId = attemptId,
    accountOwnerId = accountOwnerId,
    scenarioId = scenarioId,
    scenarioTitle = scenarioTitle,
    moduleName = module.code,
    userResponseText = userResponseText,
    evaluationScore = evaluationScore,
    isPassed = isPassed,
    strengthsFeedbackJson = json.encodeToString(strengthsFeedback),
    improvementAreasJson = json.encodeToString(improvementAreas),
    evidenceSourceType = evidenceSource.name,
    verificationStatus = verificationStatus.name,
    idempotencyKey = idempotencyKey,
    completedEpochMs = completedEpochMs
)

fun SocialAssessmentEntity.toDomain() = SocialAssessment(
    assessmentId = assessmentId,
    accountOwnerId = accountOwnerId,
    module = SocialModuleIdentifier.fromCode(moduleName),
    assessmentType = SocialAssessmentType.valueOf(assessmentType),
    overallScore = overallScore,
    clarityScore = clarityScore,
    empathyScore = empathyScore,
    assertivenessScore = assertivenessScore,
    coherenceScore = coherenceScore,
    qualitativeAnalysis = qualitativeAnalysis,
    actionableRecommendations = runCatching { json.decodeFromString<List<String>>(actionableRecommendationsJson) }.getOrDefault(emptyList()),
    assessedEpochMs = assessedEpochMs
)

fun SocialAssessment.toEntity() = SocialAssessmentEntity(
    assessmentId = assessmentId,
    accountOwnerId = accountOwnerId,
    moduleName = module.code,
    assessmentType = assessmentType.name,
    overallScore = overallScore.coerceIn(0, 100),
    clarityScore = clarityScore.coerceIn(0, 100),
    empathyScore = empathyScore.coerceIn(0, 100),
    assertivenessScore = assertivenessScore.coerceIn(0, 100),
    coherenceScore = coherenceScore.coerceIn(0, 100),
    qualitativeAnalysis = qualitativeAnalysis,
    actionableRecommendationsJson = json.encodeToString(actionableRecommendations),
    assessedEpochMs = assessedEpochMs
)

// 7. Real-World Social Mission Record Entity (Milestone 12 - Phase 4)
@Entity(
    tableName = "social_mission_records",
    foreignKeys = [
        ForeignKey(
            entity = SocialProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId"]),
        Index(value = ["missionId"]),
        Index(value = ["moduleName"]),
        Index(value = ["lifecycleStatus"]),
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["startedEpochMs"])
    ]
)
data class SocialMissionRecordEntity(
    @PrimaryKey val recordId: String,
    val accountOwnerId: String,
    val missionId: String,
    val moduleName: String,
    val title: String,
    val difficultyRank: String,
    val lifecycleStatus: String,
    val startedEpochMs: Long,
    val submittedEpochMs: Long? = null,
    val completedEpochMs: Long? = null,
    val evidenceType: String,
    val reflectionText: String? = null,
    val humanConfirmingRole: String? = null,
    val artifactNote: String? = null,
    val aiFeedbackNotes: String? = null,
    val evaluationRubricScore: Int? = null,
    val xpAwarded: Int = 0,
    val verificationStatus: String,
    val idempotencyKey: String,
    val updatedAtEpochMs: Long
)

fun SocialMissionRecordEntity.toDomain() = SocialMissionRecord(
    recordId = recordId,
    accountOwnerId = accountOwnerId,
    missionId = missionId,
    module = SocialModuleIdentifier.fromCode(moduleName),
    title = title,
    difficultyRank = MissionDifficultyRank.fromString(difficultyRank),
    lifecycleStatus = SocialMissionLifecycleStatus.fromString(lifecycleStatus),
    startedEpochMs = startedEpochMs,
    submittedEpochMs = submittedEpochMs,
    completedEpochMs = completedEpochMs,
    evidenceType = SocialMissionEvidenceType.fromString(evidenceType),
    reflectionText = reflectionText,
    humanConfirmingRole = humanConfirmingRole,
    artifactNote = artifactNote,
    aiFeedbackNotes = aiFeedbackNotes,
    evaluationRubricScore = evaluationRubricScore,
    xpAwarded = xpAwarded,
    verificationStatus = runCatching { VerificationStatus.valueOf(verificationStatus) }.getOrDefault(VerificationStatus.SELF_REPORTED),
    idempotencyKey = idempotencyKey,
    updatedAtEpochMs = updatedAtEpochMs
)

fun SocialMissionRecord.toEntity() = SocialMissionRecordEntity(
    recordId = recordId,
    accountOwnerId = accountOwnerId,
    missionId = missionId,
    moduleName = module.code,
    title = title,
    difficultyRank = difficultyRank.name,
    lifecycleStatus = lifecycleStatus.name,
    startedEpochMs = startedEpochMs,
    submittedEpochMs = submittedEpochMs,
    completedEpochMs = completedEpochMs,
    evidenceType = evidenceType.name,
    reflectionText = reflectionText,
    humanConfirmingRole = humanConfirmingRole,
    artifactNote = artifactNote,
    aiFeedbackNotes = aiFeedbackNotes,
    evaluationRubricScore = evaluationRubricScore,
    xpAwarded = xpAwarded,
    verificationStatus = verificationStatus.name,
    idempotencyKey = idempotencyKey,
    updatedAtEpochMs = updatedAtEpochMs
)

