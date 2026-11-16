package com.example.domain.engine

import com.example.domain.model.AcademicActivityKind
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.AttributeType
import com.example.domain.model.HabitCategory
import com.example.domain.model.HunterRank
import com.example.domain.model.LifeActivityKind
import com.example.domain.model.PhysicalActivityKind
import com.example.domain.model.PlayerAttributes
import com.example.domain.model.PlayerProfile
import com.example.domain.model.QuestCategory
import com.example.domain.model.RecoveryStatus
import com.example.domain.model.TaskCategory
import com.example.domain.model.VerificationStatus

data class LevelProgressionResult(
    val newLevel: Int,
    val newCurrentLevelXp: Int,
    val newXpToNextLevel: Int,
    val newTotalXp: Long,
    val levelsGained: Int
)

data class ActivityRewardCalculation(
    val xpAwarded: Int,
    val primaryAttribute: AttributeType,
    val primaryAttrXpAwarded: Int,
    val secondaryAttribute: AttributeType,
    val secondaryAttrXpAwarded: Int,
    val wasCapped: Boolean = false,
    val validationNote: String = ""
)

data class NextRankProgress(
    val nextRank: HunterRank?,
    val levelProgress: Float,
    val attributeProgress: Float,
    val verifiedProgress: Float,
    val overallProgress: Float,
    val isEligibleForPromotion: Boolean
)

data class SessionValidationResult(
    val isValid: Boolean,
    val rejectionReason: String? = null
)

/**
 * Deterministic progression engine for XP, level curves, attribute computation,
 * physical & academic reward caps, and rank evaluation. AI recommendations never bypass or mutate this engine directly.
 */
object ProgressionEngine {

    const val VERIFIED_MULTIPLIER = 1.0f
    const val SYSTEM_TIMER_MULTIPLIER = 0.85f
    const val SELF_REPORTED_MULTIPLIER = 0.65f

    const val DAILY_PHYSICAL_XP_CAP = 220
    const val DAILY_SELF_REPORTED_PHYSICAL_LIMIT = 3
    const val DAILY_ACADEMIC_XP_CAP = 240
    const val DAILY_SELF_REPORTED_ACADEMIC_LIMIT = 4
    const val DAILY_LIFE_XP_CAP = 200
    const val DAILY_SELF_REPORTED_LIFE_LIMIT = 5
    const val DAILY_SOCIAL_XP_CAP = 180
    const val DAILY_SELF_REPORTED_SOCIAL_LIMIT = 3
    const val DAILY_RPG_XP_CAP = 300
    const val MAX_SESSION_MINUTES = 360

    fun xpRequiredForLevel(level: Int): Int {
        val safeLevel = level.coerceAtLeast(1)
        val delta = safeLevel - 1
        return 100 + (delta * 50) + (delta * delta * 15)
    }

    fun applyXp(
        currentLevel: Int,
        currentLevelXp: Int,
        totalXp: Long,
        gainedXp: Int
    ): LevelProgressionResult {
        val sanitizedGain = gainedXp.coerceAtLeast(0)
        var level = currentLevel.coerceAtLeast(1)
        var xpPool = currentLevelXp.coerceAtLeast(0) + sanitizedGain
        var levelsGained = 0

        while (xpPool >= xpRequiredForLevel(level)) {
            xpPool -= xpRequiredForLevel(level)
            level += 1
            levelsGained += 1
        }

        return LevelProgressionResult(
            newLevel = level,
            newCurrentLevelXp = xpPool,
            newXpToNextLevel = xpRequiredForLevel(level),
            newTotalXp = totalXp + sanitizedGain,
            levelsGained = levelsGained
        )
    }

    fun validateSession(
        title: String,
        startEpochMs: Long,
        endEpochMs: Long,
        durationMinutes: Int,
        distanceMeters: Double? = null,
        nowEpochMs: Long = System.currentTimeMillis()
    ): SessionValidationResult {
        if (title.isBlank()) {
            return SessionValidationResult(false, "Session title cannot be blank.")
        }
        if (endEpochMs <= startEpochMs) {
            return SessionValidationResult(false, "Session end time must be after start time.")
        }
        if (durationMinutes !in 1..MAX_SESSION_MINUTES) {
            return SessionValidationResult(false, "Session duration must be between 1 and $MAX_SESSION_MINUTES minutes.")
        }
        if (endEpochMs > nowEpochMs + 5 * 60_000L) {
            return SessionValidationResult(false, "Future-dated sessions cannot be rewarded.")
        }
        if (distanceMeters != null && distanceMeters < 0.0) {
            return SessionValidationResult(false, "Distance cannot be negative.")
        }
        return SessionValidationResult(true)
    }

    fun validateStudySession(
        title: String,
        subjectId: Long,
        startEpochMs: Long,
        endEpochMs: Long,
        durationMinutes: Int,
        isSystemTimer: Boolean,
        nowEpochMs: Long = System.currentTimeMillis()
    ): SessionValidationResult {
        if (subjectId <= 0L) {
            return SessionValidationResult(false, "Select an active subject before recording study time.")
        }
        val base = validateSession(
            title = title,
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs,
            durationMinutes = durationMinutes,
            distanceMeters = null,
            nowEpochMs = nowEpochMs
        )
        if (!base.isValid) return base
        if (isSystemTimer) {
            val actualElapsedSeconds = ((endEpochMs - startEpochMs) / 1000L).coerceAtLeast(0L)
            if (actualElapsedSeconds < 60L) {
                return SessionValidationResult(
                    false,
                    "Focus timer sessions under 1 minute are discarded without XP (merely starting a timer earns no XP)."
                )
            }
        }
        return SessionValidationResult(true)
    }

    fun validateAssessmentResult(
        title: String,
        subjectId: Long,
        scoredMarks: Double,
        maxMarks: Double
    ): SessionValidationResult {
        if (subjectId <= 0L) {
            return SessionValidationResult(false, "Assessment must be linked to a subject.")
        }
        if (title.isBlank()) {
            return SessionValidationResult(false, "Assessment title cannot be blank.")
        }
        if (maxMarks <= 0.0) {
            return SessionValidationResult(false, "Maximum marks must be greater than zero.")
        }
        if (scoredMarks < 0.0 || scoredMarks > maxMarks) {
            return SessionValidationResult(false, "Scored marks must be between 0 and $maxMarks.")
        }
        return SessionValidationResult(true)
    }

    fun multiplierFor(verificationStatus: VerificationStatus): Float = when (verificationStatus) {
        VerificationStatus.VERIFIED_HEALTH_CONNECT,
        VerificationStatus.VERIFIED_ASSESSMENT -> VERIFIED_MULTIPLIER
        VerificationStatus.VERIFIED_SYSTEM_TIMER -> SYSTEM_TIMER_MULTIPLIER
        VerificationStatus.SELF_REPORTED -> SELF_REPORTED_MULTIPLIER
    }

    fun calculateAcademicReward(
        kind: AcademicActivityKind,
        durationMinutes: Int,
        verificationStatus: VerificationStatus,
        academicXpAlreadyEarnedToday: Int,
        selfReportedAcademicCountToday: Int,
        assessmentRatio: Float? = null,
        difficultyWeight: Float = 1.0f
    ): ActivityRewardCalculation {
        val (primary, secondary) = when (kind) {
            AcademicActivityKind.STUDY_SESSION,
            AcademicActivityKind.FOCUS_SESSION -> AttributeType.INT to AttributeType.DIS
            AcademicActivityKind.REVISION -> AttributeType.WIS to AttributeType.INT
            AcademicActivityKind.CHAPTER_COMPLETION -> AttributeType.INT to AttributeType.WIS
            AcademicActivityKind.PRACTICE_ASSESSMENT -> AttributeType.INT to AttributeType.WIS
            AcademicActivityKind.EXAM_ASSESSMENT -> AttributeType.INT to AttributeType.DIS
            AcademicActivityKind.ACADEMIC_QUEST -> AttributeType.DIS to AttributeType.INT
        }

        if (!verificationStatus.isVerified && selfReportedAcademicCountToday >= DAILY_SELF_REPORTED_ACADEMIC_LIMIT) {
            return ActivityRewardCalculation(
                xpAwarded = 0,
                primaryAttribute = primary,
                primaryAttrXpAwarded = 0,
                secondaryAttribute = secondary,
                secondaryAttrXpAwarded = 0,
                wasCapped = true,
                validationNote = "Daily self-reported academic reward cap (${DAILY_SELF_REPORTED_ACADEMIC_LIMIT}/day) reached. Activity logged without extra XP."
            )
        }

        val multiplier = multiplierFor(verificationStatus)
        val safeMinutes = durationMinutes.coerceIn(1, 180)
        val rawBaseXp = when (kind) {
            AcademicActivityKind.STUDY_SESSION,
            AcademicActivityKind.FOCUS_SESSION -> (safeMinutes * 1.4f + 8f)
            AcademicActivityKind.REVISION -> (safeMinutes * 1.5f + 12f)
            AcademicActivityKind.CHAPTER_COMPLETION -> (32f * difficultyWeight.coerceIn(1.0f, 1.5f))
            AcademicActivityKind.PRACTICE_ASSESSMENT -> 28f + ((assessmentRatio ?: 0.5f).coerceIn(0f, 1f) * 20f)
            AcademicActivityKind.EXAM_ASSESSMENT -> 45f + ((assessmentRatio ?: 0.5f).coerceIn(0f, 1f) * 35f)
            AcademicActivityKind.ACADEMIC_QUEST -> (safeMinutes * 1.2f + 22f)
        }

        val scaledXp = (rawBaseXp * multiplier).toInt().coerceAtLeast(6)
        val remainingCap = (DAILY_ACADEMIC_XP_CAP - academicXpAlreadyEarnedToday).coerceAtLeast(0)
        val awardedXp = scaledXp.coerceAtMost(remainingCap)
        val wasCapped = awardedXp < scaledXp

        val capRatio = if (scaledXp <= 0) 0f else awardedXp.toFloat() / scaledXp
        val rawPrimaryAttrXp = (rawBaseXp * 1.15f * multiplier).toInt().coerceAtLeast(8)
        val rawSecondaryAttrXp = (rawBaseXp * 0.6f * multiplier).toInt().coerceAtLeast(4)

        val finalPrimaryAttrXp = (rawPrimaryAttrXp * capRatio).toInt()
        val finalSecondaryAttrXp = (rawSecondaryAttrXp * capRatio).toInt()

        val provenanceNote = when (verificationStatus) {
            VerificationStatus.VERIFIED_SYSTEM_TIMER ->
                "System timer verified elapsed app duration (not verified learning)."
            VerificationStatus.VERIFIED_ASSESSMENT ->
                "Assessment evidence verified."
            VerificationStatus.VERIFIED_HEALTH_CONNECT ->
                "Health Connect verified."
            VerificationStatus.SELF_REPORTED ->
                "Self-reported study record (0.65x weight)."
        }
        val capNote = if (wasCapped) " Daily academic XP cap ($DAILY_ACADEMIC_XP_CAP XP) applied." else ""

        return ActivityRewardCalculation(
            xpAwarded = awardedXp,
            primaryAttribute = primary,
            primaryAttrXpAwarded = finalPrimaryAttrXp,
            secondaryAttribute = secondary,
            secondaryAttrXpAwarded = finalSecondaryAttrXp,
            wasCapped = wasCapped,
            validationNote = "$provenanceNote$capNote".trim()
        )
    }

    fun calculateActivityReward(
        category: QuestCategory,
        durationMinutes: Int,
        verificationStatus: VerificationStatus
    ): ActivityRewardCalculation {
        val safeMinutes = durationMinutes.coerceIn(1, 240)
        val multiplier = multiplierFor(verificationStatus)

        val rawXp = (safeMinutes * 2) + 10
        val awardedXp = (rawXp * multiplier).toInt().coerceAtLeast(5)

        val (primary, secondary) = when (category) {
            QuestCategory.PHYSICAL -> AttributeType.VIT to AttributeType.STR
            QuestCategory.INTELLECT -> AttributeType.INT to AttributeType.DIS
            QuestCategory.DISCIPLINE -> AttributeType.DIS to AttributeType.WIS
            QuestCategory.RECOVERY -> AttributeType.WIS to AttributeType.VIT
            QuestCategory.SOCIAL -> AttributeType.CHA to AttributeType.WIS
        }

        val primaryAttrXp = ((safeMinutes * 2.2f) * multiplier).toInt().coerceAtLeast(8)
        val secondaryAttrXp = ((safeMinutes * 1.1f) * multiplier).toInt().coerceAtLeast(4)

        return ActivityRewardCalculation(
            xpAwarded = awardedXp,
            primaryAttribute = primary,
            primaryAttrXpAwarded = primaryAttrXp,
            secondaryAttribute = secondary,
            secondaryAttrXpAwarded = secondaryAttrXp
        )
    }

    /**
     * Physical-specific reward engine targeting STR, VIT, AGI, and DIS with daily caps
     * to prevent duplicate/unlimited XP farming.
     */
    fun calculatePhysicalReward(
        kind: PhysicalActivityKind,
        durationMinutes: Int,
        verificationStatus: VerificationStatus,
        xpAlreadyEarnedToday: Int,
        selfReportedPhysicalCountToday: Int,
        totalVolumeSets: Int = 0
    ): ActivityRewardCalculation {
        val safeMinutes = durationMinutes.coerceIn(1, 180)
        val multiplier = multiplierFor(verificationStatus)

        val (primary, secondary) = when (kind) {
            PhysicalActivityKind.JOGGING,
            PhysicalActivityKind.RUNNING -> AttributeType.VIT to AttributeType.AGI
            PhysicalActivityKind.WALKING -> AttributeType.VIT to AttributeType.DIS
            PhysicalActivityKind.STRENGTH_TRAINING,
            PhysicalActivityKind.CALISTHENICS -> AttributeType.STR to AttributeType.DIS
            PhysicalActivityKind.CONDITIONING -> AttributeType.AGI to AttributeType.STR
            PhysicalActivityKind.MOBILITY -> AttributeType.DIS to AttributeType.VIT
        }

        if (!verificationStatus.isVerified && selfReportedPhysicalCountToday >= DAILY_SELF_REPORTED_PHYSICAL_LIMIT) {
            return ActivityRewardCalculation(
                xpAwarded = 0,
                primaryAttribute = primary,
                primaryAttrXpAwarded = 0,
                secondaryAttribute = secondary,
                secondaryAttrXpAwarded = 0,
                wasCapped = true,
                validationNote = "Daily self-reported physical reward cap reached (${DAILY_SELF_REPORTED_PHYSICAL_LIMIT}/day). Session recorded in log."
            )
        }

        val setBonusXp = (totalVolumeSets.coerceIn(0, 25)) * 2
        val rawXp = (safeMinutes * 2) + 12 + setBonusXp
        val scaledXp = (rawXp * multiplier).toInt().coerceAtLeast(6)

        val remainingCap = (DAILY_PHYSICAL_XP_CAP - xpAlreadyEarnedToday).coerceAtLeast(0)
        val awardedXp = scaledXp.coerceAtMost(remainingCap)
        val wasCapped = awardedXp < scaledXp

        val capRatio = if (scaledXp <= 0) 0f else awardedXp.toFloat() / scaledXp
        val rawPrimaryAttrXp = ((safeMinutes * 2.2f + setBonusXp * 1.2f) * multiplier).toInt().coerceAtLeast(8)
        val rawSecondaryAttrXp = ((safeMinutes * 1.2f + setBonusXp * 0.6f) * multiplier).toInt().coerceAtLeast(4)

        val finalPrimaryAttrXp = (rawPrimaryAttrXp * capRatio).toInt()
        val finalSecondaryAttrXp = (rawSecondaryAttrXp * capRatio).toInt()

        val note = if (wasCapped) {
            "Daily physical XP cap ($DAILY_PHYSICAL_XP_CAP XP) reached; partial or zero XP awarded."
        } else {
            "Validated (${verificationStatus.label})."
        }

        return ActivityRewardCalculation(
            xpAwarded = awardedXp,
            primaryAttribute = primary,
            primaryAttrXpAwarded = finalPrimaryAttrXp,
            secondaryAttribute = secondary,
            secondaryAttrXpAwarded = finalSecondaryAttrXp,
            wasCapped = wasCapped,
            validationNote = note
        )
    }

    /**
     * Deterministic reward calculation for Milestone 5 Life Management & Discipline Engine.
     * Enforces daily cap (DAILY_LIFE_XP_CAP), self-reported count limits, capped streak bonus,
     * and provenance weighting. Never called for creating, editing, browsing, or rescheduling items.
     */
    fun calculateLifeDisciplineReward(
        kind: LifeActivityKind,
        verificationStatus: VerificationStatus,
        lifeXpAlreadyEarnedToday: Int,
        selfReportedLifeCountToday: Int,
        habitCategory: HabitCategory? = null,
        taskCategory: TaskCategory? = null,
        goalAttribute: AttributeType? = null,
        streakDays: Int = 0
    ): ActivityRewardCalculation {
        val (primary, secondary) = when (kind) {
            LifeActivityKind.ROUTINE_COMPLETION -> AttributeType.DIS to AttributeType.WIS
            LifeActivityKind.HABIT_COMPLETION -> (habitCategory?.targetAttribute ?: AttributeType.DIS) to AttributeType.DIS
            LifeActivityKind.PERSONAL_TASK -> (taskCategory?.primaryAttribute ?: AttributeType.DIS) to AttributeType.DIS
            LifeActivityKind.GOAL_MILESTONE,
            LifeActivityKind.GOAL_REVIEW -> (goalAttribute ?: AttributeType.DIS) to AttributeType.WIS
            LifeActivityKind.SOCIAL_COMMITMENT -> AttributeType.CHA to AttributeType.WIS
            LifeActivityKind.REFLECTION -> AttributeType.WIS to AttributeType.DIS
        }

        if (!verificationStatus.isVerified && selfReportedLifeCountToday >= DAILY_SELF_REPORTED_LIFE_LIMIT) {
            return ActivityRewardCalculation(
                xpAwarded = 0,
                primaryAttribute = primary,
                primaryAttrXpAwarded = 0,
                secondaryAttribute = secondary,
                secondaryAttrXpAwarded = 0,
                wasCapped = true,
                validationNote = "Daily self-reported life discipline reward limit ($DAILY_SELF_REPORTED_LIFE_LIMIT/day) reached; recorded without extra XP."
            )
        }

        val multiplier = multiplierFor(verificationStatus)
        val baseXp = when (kind) {
            LifeActivityKind.ROUTINE_COMPLETION -> 20f
            LifeActivityKind.HABIT_COMPLETION -> 22f
            LifeActivityKind.PERSONAL_TASK -> 26f
            LifeActivityKind.GOAL_MILESTONE -> 36f
            LifeActivityKind.GOAL_REVIEW -> 18f
            LifeActivityKind.SOCIAL_COMMITMENT -> 24f
            LifeActivityKind.REFLECTION -> 20f
        }
        val cappedStreakBonus = streakDays.coerceIn(0, 10) * 1f // max +10 raw XP, prevents infinite exponential farming
        val scaledXp = ((baseXp + cappedStreakBonus) * multiplier).toInt().coerceAtLeast(6)

        val remainingCap = (DAILY_LIFE_XP_CAP - lifeXpAlreadyEarnedToday).coerceAtLeast(0)
        val awardedXp = scaledXp.coerceAtMost(remainingCap)
        val wasCapped = awardedXp < scaledXp

        val capRatio = if (scaledXp <= 0) 0f else awardedXp.toFloat() / scaledXp
        val rawPrimaryAttrXp = ((baseXp * 1.1f + cappedStreakBonus * 0.5f) * multiplier).toInt().coerceAtLeast(6)
        val rawSecondaryAttrXp = ((baseXp * 0.55f) * multiplier).toInt().coerceAtLeast(3)

        val finalPrimaryAttrXp = (rawPrimaryAttrXp * capRatio).toInt()
        val finalSecondaryAttrXp = (rawSecondaryAttrXp * capRatio).toInt()

        val note = if (wasCapped) {
            "Daily life & discipline XP cap ($DAILY_LIFE_XP_CAP XP) applied."
        } else {
            "Life action validated (${verificationStatus.label})."
        }

        return ActivityRewardCalculation(
            xpAwarded = awardedXp,
            primaryAttribute = primary,
            primaryAttrXpAwarded = finalPrimaryAttrXp,
            secondaryAttribute = secondary,
            secondaryAttrXpAwarded = finalSecondaryAttrXp,
            wasCapped = wasCapped,
            validationNote = note
        )
    }

    /**
     * Deterministic reward calculator for Milestone 6 Dungeons, Boss Battles, and Achievements.
     * Strictly bounded by DAILY_RPG_XP_CAP, weights verified evidence higher than self-reported,
     * and never permits AI or browsing/entering actions to grant XP directly.
     */
    fun calculateRpgReward(
        baseXp: Int,
        primaryAttribute: AttributeType,
        primaryAttrXp: Int,
        secondaryAttribute: AttributeType,
        secondaryAttrXp: Int,
        hasVerifiedEvidence: Boolean,
        rpgXpAlreadyEarnedToday: Int,
        sourceLabel: String
    ): ActivityRewardCalculation {
        if (baseXp <= 0) {
            return ActivityRewardCalculation(
                xpAwarded = 0,
                primaryAttribute = primaryAttribute,
                primaryAttrXpAwarded = 0,
                secondaryAttribute = secondaryAttribute,
                secondaryAttrXpAwarded = 0,
                wasCapped = false,
                validationNote = "Non-rewarding RPG action."
            )
        }
        val provenanceMultiplier = if (hasVerifiedEvidence) 1.0f else 0.75f
        val scaledXp = (baseXp * provenanceMultiplier).toInt().coerceAtLeast(1)
        val remainingCap = (DAILY_RPG_XP_CAP - rpgXpAlreadyEarnedToday).coerceAtLeast(0)
        val awardedXp = scaledXp.coerceAtMost(remainingCap)
        val wasCapped = awardedXp < scaledXp
        val ratio = if (scaledXp <= 0) 0f else awardedXp.toFloat() / scaledXp

        val finalPrimary = ((primaryAttrXp * provenanceMultiplier) * ratio).toInt()
        val finalSecondary = ((secondaryAttrXp * provenanceMultiplier) * ratio).toInt()
        val note = if (wasCapped) {
            "$sourceLabel cleared. Daily RPG XP cap ($DAILY_RPG_XP_CAP XP) applied."
        } else {
            "$sourceLabel validated (${if (hasVerifiedEvidence) "Verified Evidence 1.0x" else "Self-Reported 0.75x"})."
        }
        return ActivityRewardCalculation(
            xpAwarded = awardedXp,
            primaryAttribute = primaryAttribute,
            primaryAttrXpAwarded = finalPrimary,
            secondaryAttribute = secondaryAttribute,
            secondaryAttrXpAwarded = finalSecondary,
            wasCapped = wasCapped,
            validationNote = note
        )
    }

    fun applyAttributeXp(
        attributes: PlayerAttributes,
        deltas: Map<AttributeType, Int>,
        isVerified: Boolean
    ): PlayerAttributes {
        fun updateOne(breakdown: AttributeBreakdown): AttributeBreakdown {
            val gain = deltas[breakdown.type]?.coerceAtLeast(0) ?: 0
            if (gain == 0) return breakdown

            val combinedProgress = breakdown.progressXp + gain
            val pointsGained = combinedProgress / AttributeBreakdown.ATTRIBUTE_XP_PER_POINT
            val remainderXp = combinedProgress % AttributeBreakdown.ATTRIBUTE_XP_PER_POINT

            return breakdown.copy(
                verifiedPoints = if (isVerified) breakdown.verifiedPoints + pointsGained else breakdown.verifiedPoints,
                selfReportedPoints = if (!isVerified) breakdown.selfReportedPoints + pointsGained else breakdown.selfReportedPoints,
                progressXp = remainderXp
            )
        }

        return PlayerAttributes(
            str = updateOne(attributes.str),
            vit = updateOne(attributes.vit),
            agi = updateOne(attributes.agi),
            intel = updateOne(attributes.intel),
            dis = updateOne(attributes.dis),
            wis = updateOne(attributes.wis),
            cha = updateOne(attributes.cha)
        )
    }

    fun evaluateRank(
        level: Int,
        totalAttributeScore: Int,
        verifiedActivitiesCount: Int
    ): HunterRank {
        var achieved = HunterRank.E_RANK
        for (rank in HunterRank.entries) {
            val t = rank.threshold
            if (level >= t.minLevel &&
                totalAttributeScore >= t.minTotalAttributes &&
                verifiedActivitiesCount >= t.minVerifiedActivities
            ) {
                achieved = rank
            }
        }
        return achieved
    }

    fun evaluateNextRankProgress(profile: PlayerProfile): NextRankProgress {
        val next = profile.rank.nextRank()
            ?: return NextRankProgress(
                nextRank = null,
                levelProgress = 1f,
                attributeProgress = 1f,
                verifiedProgress = 1f,
                overallProgress = 1f,
                isEligibleForPromotion = false
            )

        val t = next.threshold
        val levelPct = (profile.level.toFloat() / t.minLevel.coerceAtLeast(1)).coerceIn(0f, 1f)
        val attrPct = (profile.attributes.totalScore.toFloat() / t.minTotalAttributes.coerceAtLeast(1)).coerceIn(0f, 1f)
        val verifiedPct = if (t.minVerifiedActivities == 0) 1f else {
            (profile.verifiedActivitiesCount.toFloat() / t.minVerifiedActivities).coerceIn(0f, 1f)
        }
        val overall = (levelPct + attrPct + verifiedPct) / 3f
        val eligible = levelPct >= 1f && attrPct >= 1f && verifiedPct >= 1f

        return NextRankProgress(
            nextRank = next,
            levelProgress = levelPct,
            attributeProgress = attrPct,
            verifiedProgress = verifiedPct,
            overallProgress = overall,
            isEligibleForPromotion = eligible
        )
    }

    fun evaluateRecoveryStatus(todayTrainingMinutes: Int, weeklySessions: Int): RecoveryStatus {
        return when {
            todayTrainingMinutes >= 90 || weeklySessions >= 7 -> RecoveryStatus.RECOVERY_REQUIRED
            todayTrainingMinutes >= 45 || weeklySessions >= 5 -> RecoveryStatus.MODERATE_LOAD
            else -> RecoveryStatus.OPTIMAL
        }
    }

    /**
     * Milestone 12: Canonical Social Mastery reward calculation.
     * Sole authorized formula engine for social activities, rehearsal, scenarios and assessments.
     * Maps strictly to verified attributes: CHA, WIS, DIS, INT.
     */
    fun calculateSocialReward(
        module: com.example.domain.model.SocialModuleIdentifier,
        activityKind: com.example.domain.model.SocialActivityKind,
        durationMinutes: Int,
        verificationStatus: VerificationStatus,
        socialXpAlreadyEarnedToday: Int,
        selfReportedSocialCountToday: Int,
        rubricScorePercent: Int? = null
    ): ActivityRewardCalculation {
        val primary = module.primaryAttribute
        val secondary = module.secondaryAttribute

        if (!verificationStatus.isVerified && selfReportedSocialCountToday >= DAILY_SELF_REPORTED_SOCIAL_LIMIT) {
            return ActivityRewardCalculation(
                xpAwarded = 0,
                primaryAttribute = primary,
                primaryAttrXpAwarded = 0,
                secondaryAttribute = secondary,
                secondaryAttrXpAwarded = 0,
                wasCapped = true,
                validationNote = "Daily self-reported social limit ($DAILY_SELF_REPORTED_SOCIAL_LIMIT/day) reached. Activity logged without extra XP."
            )
        }

        val multiplier = multiplierFor(verificationStatus)
        val safeMinutes = durationMinutes.coerceIn(1, 180)
        val rawBaseXp = when (activityKind) {
            com.example.domain.model.SocialActivityKind.PRACTICE,
            com.example.domain.model.SocialActivityKind.LISTENING_EXERCISE,
            com.example.domain.model.SocialActivityKind.LANGUAGE_PRACTICE -> (safeMinutes * 1.2f + 6f)
            com.example.domain.model.SocialActivityKind.SPEAKING_REHEARSAL,
            com.example.domain.model.SocialActivityKind.PRESENTATION -> (safeMinutes * 1.5f + 10f)
            com.example.domain.model.SocialActivityKind.SCENARIO,
            com.example.domain.model.SocialActivityKind.NEGOTIATION_EXERCISE -> 30f
            com.example.domain.model.SocialActivityKind.REFLECTION -> 15f
            com.example.domain.model.SocialActivityKind.REAL_WORLD_ACTIVITY -> (safeMinutes * 0.8f + 8f)
        }

        val qualityFactor = rubricScorePercent?.let { score ->
            when {
                score >= 90 -> 1.20f
                score >= 75 -> 1.00f
                score >= 60 -> 0.80f
                else -> 0.50f
            }
        } ?: 1.0f

        val scaledXp = (rawBaseXp * qualityFactor * multiplier).toInt().coerceAtLeast(4)
        val remainingCap = (DAILY_SOCIAL_XP_CAP - socialXpAlreadyEarnedToday).coerceAtLeast(0)
        val awardedXp = scaledXp.coerceAtMost(remainingCap)
        val wasCapped = awardedXp < scaledXp

        val capRatio = if (scaledXp <= 0) 0f else awardedXp.toFloat() / scaledXp
        val rawPrimaryAttrXp = (rawBaseXp * 1.2f * multiplier * module.primaryRatio).toInt().coerceAtLeast(4)
        val rawSecondaryAttrXp = (rawBaseXp * 0.8f * multiplier * module.secondaryRatio).toInt().coerceAtLeast(2)

        val finalPrimaryAttrXp = (rawPrimaryAttrXp * capRatio).toInt()
        val finalSecondaryAttrXp = (rawSecondaryAttrXp * capRatio).toInt()

        val provenanceNote = when (verificationStatus) {
            VerificationStatus.VERIFIED_SYSTEM_TIMER -> "System timer verified in-app rehearsal."
            VerificationStatus.VERIFIED_ASSESSMENT -> "Assessment evidence evaluated."
            VerificationStatus.VERIFIED_HEALTH_CONNECT -> "Health Connect verified."
            VerificationStatus.SELF_REPORTED -> "Self-reported social action (0.65x weight)."
        }
        val capNote = if (wasCapped) " Daily social XP cap ($DAILY_SOCIAL_XP_CAP XP) reached." else ""

        return ActivityRewardCalculation(
            xpAwarded = awardedXp,
            primaryAttribute = primary,
            primaryAttrXpAwarded = finalPrimaryAttrXp,
            secondaryAttribute = secondary,
            secondaryAttrXpAwarded = finalSecondaryAttrXp,
            wasCapped = wasCapped,
            validationNote = "$provenanceNote$capNote".trim()
        )
    }
}
