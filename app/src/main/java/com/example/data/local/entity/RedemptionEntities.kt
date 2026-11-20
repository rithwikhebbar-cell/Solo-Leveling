package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.CorrectiveCategory
import com.example.domain.model.CorrectiveDifficulty
import com.example.domain.model.RedemptionLifecycleState
import com.example.domain.model.RedemptionPlan
import com.example.domain.model.RedemptionVerificationEvidence
import com.example.domain.model.RedemptionVerificationMethod
import com.example.domain.model.ShadowClass

@Entity(
    tableName = "redemption_activities",
    indices = [
        Index(value = ["penaltyId"]),
        Index(value = ["accountOwnerId"]),
        Index(value = ["lifecycleStateName"]),
        Index(value = ["failureRecordId"])
    ]
)
data class RedemptionEntity(
    @PrimaryKey val redemptionId: String,
    val penaltyId: String,
    val failureRecordId: String,
    val questId: Long,
    val accountOwnerId: String,
    val activityId: String,
    val categoryName: String,
    val activityTitle: String,
    val instructions: String,
    val difficultyName: String,
    val estimatedDurationMinutes: Int,
    val verificationMethodName: String,
    val lifecycleStateName: String,
    val resolutionPoints: Int,
    val assignedShadowClassName: String,
    val shadowGuidanceNote: String,
    val createdAtEpochMs: Long,
    val acceptedAtEpochMs: Long?,
    val completedAtEpochMs: Long?,
    val verifiedAtEpochMs: Long?,
    val verificationNotes: String?
)

@Entity(
    tableName = "redemption_verifications",
    indices = [
        Index(value = ["redemptionId"]),
        Index(value = ["accountOwnerId"])
    ]
)
data class RedemptionVerificationEntity(
    @PrimaryKey val verificationId: String,
    val redemptionId: String,
    val accountOwnerId: String,
    val submittedAtEpochMs: Long,
    val reflectionText: String,
    val checklistItemsJson: String,
    val externalSessionRefId: String?,
    val isValid: Boolean,
    val pointsResolved: Int,
    val verifiedAtEpochMs: Long,
    val notes: String
)

// Extension Mappers
fun RedemptionEntity.toDomain(): RedemptionPlan = RedemptionPlan(
    redemptionId = redemptionId,
    penaltyId = penaltyId,
    failureRecordId = failureRecordId,
    questId = questId,
    accountOwnerId = accountOwnerId,
    activityId = activityId,
    category = CorrectiveCategory.fromName(categoryName),
    activityTitle = activityTitle,
    instructions = instructions,
    difficulty = CorrectiveDifficulty.fromName(difficultyName),
    estimatedDurationMinutes = estimatedDurationMinutes,
    verificationMethod = RedemptionVerificationMethod.fromName(verificationMethodName),
    lifecycleState = RedemptionLifecycleState.fromName(lifecycleStateName),
    resolutionPoints = resolutionPoints,
    assignedShadowClass = ShadowClass.entries.find { it.name.equals(assignedShadowClassName, ignoreCase = true) } ?: ShadowClass.GUARDIAN,
    shadowGuidanceNote = shadowGuidanceNote,
    createdAtEpochMs = createdAtEpochMs,
    acceptedAtEpochMs = acceptedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    verifiedAtEpochMs = verifiedAtEpochMs,
    verificationNotes = verificationNotes
)

fun RedemptionPlan.toEntity(): RedemptionEntity = RedemptionEntity(
    redemptionId = redemptionId,
    penaltyId = penaltyId,
    failureRecordId = failureRecordId,
    questId = questId,
    accountOwnerId = accountOwnerId,
    activityId = activityId,
    categoryName = category.name,
    activityTitle = activityTitle,
    instructions = instructions,
    difficultyName = difficulty.name,
    estimatedDurationMinutes = estimatedDurationMinutes,
    verificationMethodName = verificationMethod.name,
    lifecycleStateName = lifecycleState.name,
    resolutionPoints = resolutionPoints,
    assignedShadowClassName = assignedShadowClass.name,
    shadowGuidanceNote = shadowGuidanceNote,
    createdAtEpochMs = createdAtEpochMs,
    acceptedAtEpochMs = acceptedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    verifiedAtEpochMs = verifiedAtEpochMs,
    verificationNotes = verificationNotes
)
