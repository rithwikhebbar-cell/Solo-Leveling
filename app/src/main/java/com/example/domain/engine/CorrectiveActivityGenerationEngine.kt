package com.example.domain.engine

import com.example.domain.model.AttributeType
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.Quest
import com.example.domain.model.RecoveryStatus
import com.example.domain.model.RedemptionPlan
import com.example.domain.model.RedemptionLifecycleState
import com.example.domain.model.CorrectiveCategory
import com.example.domain.model.CorrectiveDifficulty
import com.example.domain.model.RedemptionVerificationMethod
import com.example.domain.model.ShadowClass
import java.util.UUID

object CorrectiveActivityGenerationEngine {

    fun generatePlan(
        penalty: PenaltyRecord,
        quest: Quest?,
        recoveryStatus: RecoveryStatus,
        activeRedemptions: List<RedemptionPlan>,
        recentActivityIds: Set<String>
    ): RedemptionPlan {
        val redemptionId = "redemption_${UUID.randomUUID().toString().take(8)}"
        val activityId = "corrective_${UUID.randomUUID().toString().take(8)}"

        val title = quest?.title?.let { "Corrective Protocol: $it" } ?: "System Disciplinary Correction"
        val description = "Complete restorative physical or cognitive corrective activity to clear penalty status."

        return RedemptionPlan(
            redemptionId = redemptionId,
            penaltyId = penalty.penaltyId,
            failureRecordId = penalty.penaltyId,
            questId = quest?.id ?: 0L,
            accountOwnerId = penalty.accountOwnerId,
            activityId = activityId,
            category = CorrectiveCategory.DISCIPLINE,
            activityTitle = title,
            instructions = description,
            difficulty = CorrectiveDifficulty.MANAGEABLE,
            estimatedDurationMinutes = 25,
            verificationMethod = RedemptionVerificationMethod.USER_ATTESTATION,
            lifecycleState = RedemptionLifecycleState.PENDING_ACCEPTANCE,
            resolutionPoints = 50,
            assignedShadowClass = ShadowClass.VANGUARD,
            shadowGuidanceNote = "Complete this corrective protocol to restore system alignment.",
            createdAtEpochMs = System.currentTimeMillis()
        )
    }
}
