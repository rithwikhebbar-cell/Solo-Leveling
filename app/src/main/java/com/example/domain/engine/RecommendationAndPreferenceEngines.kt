package com.example.domain.engine

import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DataProvenanceTag
import com.example.domain.model.PersonalizedRecommendation
import com.example.domain.model.PlayerIntelligencePreferences
import com.example.domain.model.PlayerIntelligenceProfile
import com.example.domain.model.RecommendationCategory
import com.example.domain.model.RecommendationFeedbackRecord
import com.example.domain.model.RecommendationStatus
import com.example.domain.model.RecoveryState
import com.example.domain.model.WorkloadAssessment
import java.util.UUID

/**
 * Generates contextual, deduplicated, cooldown-aware personalized recommendations
 * across Physical, Academic, Discipline, Personal Development, and RPG domains.
 * Adapts priority weighting from historical player acceptance/rejection feedback.
 */
object RecommendationEngine {

    private const val DAY_MS = 24 * 60 * 60_000L

    fun generate(
        input: IntelligenceTelemetryInput,
        profile: PlayerIntelligenceProfile,
        workload: WorkloadAssessment,
        existingRecommendations: List<PersonalizedRecommendation>,
        feedbackHistory: List<RecommendationFeedbackRecord>,
        prefs: PlayerIntelligencePreferences = PlayerIntelligencePreferences()
    ): List<PersonalizedRecommendation> {
        val now = input.nowEpochMs
        val expiresAt = now + 2 * DAY_MS
        val cooldownAt = now + 3 * DAY_MS

        // Preference weights learned strictly from historical feedback (+8 accept/complete, -10 reject)
        val categoryBias = RecommendationCategory.entries.associateWith { cat ->
            val catEvents = feedbackHistory.filter { it.category == cat }
            val pos = catEvents.count { it.action == RecommendationStatus.ACCEPTED || it.action == RecommendationStatus.COMPLETED }
            val neg = catEvents.count { it.action == RecommendationStatus.REJECTED }
            ((pos * 8) - (neg * 10)).coerceIn(-25, 25)
        }

        val candidates = mutableListOf<PersonalizedRecommendation>()
        val recoveryBlock = input.recovery.state == RecoveryState.REST_RECOMMENDED ||
            input.recovery.state == RecoveryState.RECOVERY_RECOMMENDED ||
            input.recovery.selfReportedDiscomfort

        // 1. Physical Recommendation (respects recovery limits)
        if (prefs.sharePhysicalWithIntelligence) {
            if (recoveryBlock) {
                candidates.add(
                    PersonalizedRecommendation(
                        recUuid = UUID.randomUUID().toString(),
                        category = RecommendationCategory.PHYSICAL,
                        title = "Prioritize Penalty-Free Recovery Reset",
                        description = "Readiness is ${input.recovery.readinessScore}/100 (${input.recovery.state.code}). Choose rest or light mobility stretching today.",
                        rationale = "Avoids compounding 48h physical load (${input.recovery.measuredLoadMinutes48h}m) or discomfort.",
                        expectedImpact = "Restores training readiness without losing streak protection",
                        actionLabel = "Open Physical Hub",
                        deepLinkRoute = "physical",
                        priorityScore = (92 + (categoryBias[RecommendationCategory.PHYSICAL] ?: 0)).coerceIn(10, 100),
                        provenance = DataProvenanceTag.MEASURED,
                        confidence = ConfidenceLevel.HIGH,
                        expiresAtEpochMs = expiresAt,
                        cooldownUntilEpochMs = cooldownAt,
                        sourceFingerprint = profile.sourceFingerprint,
                        createdAtEpochMs = now
                    )
                )
            } else {
                candidates.add(
                    PersonalizedRecommendation(
                        recUuid = UUID.randomUUID().toString(),
                        category = RecommendationCategory.PHYSICAL,
                        title = "Complete ${input.dailyJoggingTargetMinutes}m Aerobic Conditioning",
                        description = "Readiness is optimal (${input.recovery.readinessScore}/100). Log a timer or Health Connect session for 1.0x verified progression.",
                        rationale = "7d physical score is ${profile.physicalActivityScore}/100.",
                        expectedImpact = "Advances VIT/AGI and verified rank criteria",
                        actionLabel = "Start Jogging",
                        deepLinkRoute = "physical",
                        priorityScore = (74 + (categoryBias[RecommendationCategory.PHYSICAL] ?: 0)).coerceIn(10, 100),
                        provenance = DataProvenanceTag.MEASURED,
                        confidence = ConfidenceLevel.HIGH,
                        expiresAtEpochMs = expiresAt,
                        cooldownUntilEpochMs = cooldownAt,
                        sourceFingerprint = profile.sourceFingerprint,
                        createdAtEpochMs = now
                    )
                )
            }
        }

        // 2. Academic Recommendation
        if (prefs.shareAcademicWithIntelligence) {
            val overdueRevs = input.topics.count { (it.nextRevisionDueEpochMs ?: Long.MAX_VALUE) < now }
            if (overdueRevs > 0) {
                candidates.add(
                    PersonalizedRecommendation(
                        recUuid = UUID.randomUUID().toString(),
                        category = RecommendationCategory.ACADEMIC,
                        title = "Clear $overdueRevs Overdue Spaced Revision(s)",
                        description = "Estimated topic familiarity is decaying on overdue subjects. Complete a focused recall session.",
                        rationale = "Grounded in Revision Centre due dates.",
                        expectedImpact = "Stabilizes recall & boosts WIS XP",
                        actionLabel = "Open Revisions",
                        deepLinkRoute = "academic_revisions",
                        priorityScore = (86 + (categoryBias[RecommendationCategory.ACADEMIC] ?: 0)).coerceIn(10, 100),
                        provenance = DataProvenanceTag.ESTIMATE,
                        confidence = ConfidenceLevel.MODERATE,
                        expiresAtEpochMs = expiresAt,
                        cooldownUntilEpochMs = cooldownAt,
                        sourceFingerprint = profile.sourceFingerprint,
                        createdAtEpochMs = now
                    )
                )
            } else {
                candidates.add(
                    PersonalizedRecommendation(
                        recUuid = UUID.randomUUID().toString(),
                        category = RecommendationCategory.ACADEMIC,
                        title = "Run 40m Focus Timer Session",
                        description = if (input.subjects.isEmpty()) {
                            "Create your primary study subject and complete a verified focus block."
                        } else {
                            "Advance mastery in ${input.subjects.first().name} with a timer-verified block."
                        },
                        rationale = "Academic activity score=${profile.academicActivityScore}/100.",
                        expectedImpact = "Verified INT XP & syllabus progress",
                        actionLabel = "Open Focus Timer",
                        deepLinkRoute = "academic_revisions",
                        priorityScore = (70 + (categoryBias[RecommendationCategory.ACADEMIC] ?: 0)).coerceIn(10, 100),
                        provenance = DataProvenanceTag.MEASURED,
                        confidence = ConfidenceLevel.MODERATE,
                        expiresAtEpochMs = expiresAt,
                        cooldownUntilEpochMs = cooldownAt,
                        sourceFingerprint = profile.sourceFingerprint,
                        createdAtEpochMs = now
                    )
                )
            }
        }

        // 3. Discipline Recommendation
        if (prefs.shareLifeWithIntelligence) {
            val overdueTasks = input.tasks.count { it.isOverdue(input.todayEpochDay) }
            if (overdueTasks > 0 || workload.scheduleConflictCount > 0) {
                candidates.add(
                    PersonalizedRecommendation(
                        recUuid = UUID.randomUUID().toString(),
                        category = RecommendationCategory.DISCIPLINE,
                        title = "Triage Schedule & Overdue Tasks",
                        description = "$overdueTasks overdue task(s) and ${workload.scheduleConflictCount} schedule conflict(s) detected. Reschedule into open slots.",
                        rationale = "Protects focus clarity without guilt or penalties.",
                        expectedImpact = "Improves Discipline Index & schedule adherence",
                        actionLabel = "Open Schedule",
                        deepLinkRoute = "daily_schedule",
                        priorityScore = (84 + (categoryBias[RecommendationCategory.DISCIPLINE] ?: 0)).coerceIn(10, 100),
                        provenance = DataProvenanceTag.MEASURED,
                        confidence = ConfidenceLevel.HIGH,
                        expiresAtEpochMs = expiresAt,
                        cooldownUntilEpochMs = cooldownAt,
                        sourceFingerprint = profile.sourceFingerprint,
                        createdAtEpochMs = now
                    )
                )
            } else {
                candidates.add(
                    PersonalizedRecommendation(
                        recUuid = UUID.randomUUID().toString(),
                        category = RecommendationCategory.DISCIPLINE,
                        title = "Lock Today's Adaptive Daily Plan",
                        description = "Review and approve today's conflict-free schedule blocks across study, fitness, and routines.",
                        rationale = "Proactive planning prevents end-of-day backlog.",
                        expectedImpact = "Higher daily completion rate",
                        actionLabel = "Open Adaptive Plan",
                        deepLinkRoute = "intelligence_daily_plan",
                        priorityScore = (68 + (categoryBias[RecommendationCategory.DISCIPLINE] ?: 0)).coerceIn(10, 100),
                        provenance = DataProvenanceTag.ESTIMATE,
                        confidence = ConfidenceLevel.MODERATE,
                        expiresAtEpochMs = expiresAt,
                        cooldownUntilEpochMs = cooldownAt,
                        sourceFingerprint = profile.sourceFingerprint,
                        createdAtEpochMs = now
                    )
                )
            }
        }

        // 4. Personal Development Recommendation
        candidates.add(
            PersonalizedRecommendation(
                recUuid = UUID.randomUUID().toString(),
                category = RecommendationCategory.PERSONAL_DEVELOPMENT,
                title = "Align 30-Day Development Roadmap",
                description = "Your peak consistency window is ${profile.behavioralPeakWindow}. Review milestones and risk alerts.",
                rationale = "Connects daily effort to measurable multi-week milestones.",
                expectedImpact = "Clear progression path across domains",
                actionLabel = "View Roadmap",
                deepLinkRoute = "intelligence_roadmap",
                priorityScore = (62 + (categoryBias[RecommendationCategory.PERSONAL_DEVELOPMENT] ?: 0)).coerceIn(10, 100),
                provenance = DataProvenanceTag.ESTIMATE,
                confidence = if (profile.hasMinimumBaseline) ConfidenceLevel.MODERATE else ConfidenceLevel.LOW,
                expiresAtEpochMs = expiresAt,
                cooldownUntilEpochMs = cooldownAt,
                sourceFingerprint = profile.sourceFingerprint,
                createdAtEpochMs = now
            )
        )

        // 5. RPG Recommendation
        if (prefs.shareRpgWithIntelligence) {
            candidates.add(
                PersonalizedRecommendation(
                    recUuid = UUID.randomUUID().toString(),
                    category = RecommendationCategory.RPG,
                    title = if (recoveryBlock) "Enter Recovery Sanctuary Gate" else "Challenge Adaptive Dungeon Gate",
                    description = if (recoveryBlock) {
                        "Recovery Priority limits strenuous gates; the Special Sanctuary Gate aligns with safe restoration."
                    } else {
                        "Select a rank-eligible Dungeon Gate matched to your recent completion history and readiness."
                    },
                    rationale = "Verified real-world effort clears gate objectives automatically.",
                    expectedImpact = "Earns deterministic RPG rewards & titles",
                    actionLabel = "View Adaptive Gates",
                    deepLinkRoute = "intelligence_dungeons",
                    priorityScore = (66 + (categoryBias[RecommendationCategory.RPG] ?: 0)).coerceIn(10, 100),
                    provenance = DataProvenanceTag.MEASURED,
                    confidence = ConfidenceLevel.HIGH,
                    expiresAtEpochMs = expiresAt,
                    cooldownUntilEpochMs = cooldownAt,
                    sourceFingerprint = profile.sourceFingerprint,
                    createdAtEpochMs = now
                )
            )
        }

        // Deduplicate against active or cooldown-protected recommendations
        val blockedTitles = existingRecommendations
            .filter {
                (it.status == RecommendationStatus.NEW && it.expiresAtEpochMs > now) ||
                    it.status == RecommendationStatus.ACCEPTED ||
                    (it.status == RecommendationStatus.REJECTED && it.cooldownUntilEpochMs > now)
            }
            .map { normalize(it.title) }
            .toSet()

        return candidates
            .filter { normalize(it.title) !in blockedTitles }
            .sortedByDescending { it.priorityScore }
            .take(prefs.maxRecommendationsPerDay.coerceIn(1, 8))
    }

    private fun normalize(raw: String): String = raw.lowercase().replace(Regex("[^a-z0-9]"), "")
}
