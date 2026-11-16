package com.example.domain.engine

import com.example.domain.model.AttributeType
import com.example.domain.model.PlayerProfile
import com.example.domain.model.VerificationStatus

data class ProgressionCommand(
    val idempotencyKey: String,
    val sourceModule: String,
    val sourceRecordId: String,
    val title: String,
    val xpAwarded: Int,
    val primaryAttribute: AttributeType,
    val primaryAttrXp: Int,
    val secondaryAttribute: AttributeType? = null,
    val secondaryAttrXp: Int = 0,
    val verificationStatus: VerificationStatus,
    val isTrustedSystemAction: Boolean = false
) {
    companion object {
        fun fromSocialReward(
            idempotencyKey: String,
            sourceRecordId: String,
            title: String,
            calculation: ActivityRewardCalculation,
            verificationStatus: VerificationStatus
        ): ProgressionCommand {
            return ProgressionCommand(
                idempotencyKey = idempotencyKey,
                sourceModule = "SOCIAL_MASTERY",
                sourceRecordId = sourceRecordId,
                title = title,
                xpAwarded = calculation.xpAwarded,
                primaryAttribute = calculation.primaryAttribute,
                primaryAttrXp = calculation.primaryAttrXpAwarded,
                secondaryAttribute = calculation.secondaryAttribute,
                secondaryAttrXp = calculation.secondaryAttrXpAwarded,
                verificationStatus = verificationStatus,
                isTrustedSystemAction = true
            )
        }
    }
}

data class ProgressionOrchestrationOutcome(
    val updatedProfile: PlayerProfile,
    val levelsGained: Int,
    val promotedRankChanged: Boolean,
    val wasVerifiedIncremented: Boolean
)

/**
 * Unified, idempotent progression coordinator delegating strictly to ProgressionEngine.
 * Ensures AI/automation cannot bypass validation or duplicate XP awards.
 */
object ProgressionOrchestrator {

    fun validateCommand(command: ProgressionCommand): Result<Unit> {
        if (command.idempotencyKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Progression command requires an idempotencyKey."))
        }
        if (command.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Progression command requires a title."))
        }
        if (command.xpAwarded < 0 || command.primaryAttrXp < 0 || command.secondaryAttrXp < 0) {
            return Result.failure(IllegalArgumentException("Progression XP deltas cannot be negative."))
        }
        if (command.sourceModule.equals("AI", ignoreCase = true) ||
            command.sourceModule.equals("AUTOMATION", ignoreCase = true)
        ) {
            return Result.failure(SecurityException("AI and Automation layers are prohibited from granting XP directly."))
        }
        return Result.success(Unit)
    }

    fun applyValidatedReward(
        currentProfile: PlayerProfile,
        command: ProgressionCommand,
        alreadyProcessedKeys: Set<String>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<ProgressionOrchestrationOutcome> {
        if (command.idempotencyKey in alreadyProcessedKeys) {
            return Result.failure(IllegalStateException("Duplicate progression key rejected: ${command.idempotencyKey}"))
        }
        validateCommand(command).onFailure { return Result.failure(it) }

        val levelRes = ProgressionEngine.applyXp(
            currentLevel = currentProfile.level,
            currentLevelXp = currentProfile.currentLevelXp,
            totalXp = currentProfile.totalXp,
            gainedXp = command.xpAwarded
        )

        val attrDeltas = buildMap {
            if (command.primaryAttrXp > 0) put(command.primaryAttribute, command.primaryAttrXp)
            if (command.secondaryAttribute != null && command.secondaryAttrXp > 0) {
                put(
                    command.secondaryAttribute,
                    (get(command.secondaryAttribute) ?: 0) + command.secondaryAttrXp
                )
            }
        }

        val updatedAttrs = ProgressionEngine.applyAttributeXp(
            attributes = currentProfile.attributes,
            deltas = attrDeltas,
            isVerified = command.verificationStatus.isVerified
        )

        val newVerified = currentProfile.verifiedActivitiesCount + if (command.verificationStatus.isVerified) 1 else 0
        val newSelfReported = currentProfile.selfReportedActivitiesCount + if (!command.verificationStatus.isVerified) 1 else 0
        val evaluatedRank = ProgressionEngine.evaluateRank(
            level = levelRes.newLevel,
            totalAttributeScore = updatedAttrs.totalScore,
            verifiedActivitiesCount = newVerified
        )

        val updatedProfile = currentProfile.copy(
            level = levelRes.newLevel,
            rank = evaluatedRank,
            currentLevelXp = levelRes.newCurrentLevelXp,
            xpToNextLevel = levelRes.newXpToNextLevel,
            totalXp = levelRes.newTotalXp,
            attributes = updatedAttrs,
            verifiedActivitiesCount = newVerified,
            selfReportedActivitiesCount = newSelfReported
        )

        return Result.success(
            ProgressionOrchestrationOutcome(
                updatedProfile = updatedProfile,
                levelsGained = levelRes.levelsGained,
                promotedRankChanged = evaluatedRank != currentProfile.rank,
                wasVerifiedIncremented = command.verificationStatus.isVerified
            )
        )
    }
}
