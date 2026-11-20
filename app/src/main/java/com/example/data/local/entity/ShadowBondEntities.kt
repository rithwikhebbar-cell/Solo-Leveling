package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.ShadowBondActivityType
import com.example.domain.model.ShadowBondEvent
import com.example.domain.model.ShadowBondMilestone
import com.example.domain.model.ShadowBondMilestoneType
import com.example.domain.model.ShadowBondProfile
import com.example.domain.model.ShadowPersonality
import com.example.domain.model.ShadowRelationshipStage

// ============================================================================
// MILESTONE 12 — PHASE 6.4: SHADOW BOND & LOYALTY ENTITIES
// ============================================================================

@Entity(
    tableName = "shadow_bond_profiles",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShadowEntity::class,
            parentColumns = ["shadowId"],
            childColumns = ["shadowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["shadowId"], unique = true),
        Index(value = ["accountOwnerId"]),
        Index(value = ["accountOwnerId", "relationshipStageName"])
    ]
)
data class ShadowBondProfileEntity(
    @PrimaryKey val shadowId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val bondLevel: Int = 1,
    val bondPoints: Long = 0L,
    val loyaltyRating: Int = 15,
    val trustRating: Int = 10,
    val relationshipStageName: String = ShadowRelationshipStage.UNFAMILIAR.name,
    val personalityCharacteristicName: String = ShadowPersonality.DISCIPLINED.name,
    val totalActivitiesCount: Int = 0,
    val lastBondActivityEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shadow_bond_events",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShadowEntity::class,
            parentColumns = ["shadowId"],
            childColumns = ["shadowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["eventId"], unique = true),
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["accountOwnerId", "shadowId", "timestampEpochMs"]),
        Index(value = ["shadowId"])
    ]
)
data class ShadowBondEventEntity(
    @PrimaryKey val eventId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val shadowId: String,
    val activityTypeName: String,
    val sourceEventId: String,
    val bondPointsAwarded: Int,
    val loyaltyDelta: Int,
    val trustDelta: Int,
    val description: String,
    val timestampEpochMs: Long,
    val idempotencyKey: String
)

@Entity(
    tableName = "shadow_bond_milestones",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShadowEntity::class,
            parentColumns = ["shadowId"],
            childColumns = ["shadowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["milestoneId"], unique = true),
        Index(value = ["accountOwnerId", "shadowId", "milestoneTypeName"], unique = true),
        Index(value = ["accountOwnerId"]),
        Index(value = ["shadowId"])
    ]
)
data class ShadowBondMilestoneEntity(
    @PrimaryKey val milestoneId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val shadowId: String,
    val milestoneTypeName: String,
    val title: String,
    val description: String,
    val sourceEventId: String,
    val timestampEpochMs: Long,
    val verificationStatus: String = "VERIFIED"
)

// ----------------------------------------------------------------------------
// MAPPERS
// ----------------------------------------------------------------------------

fun ShadowBondProfileEntity.toDomain(): ShadowBondProfile {
    return ShadowBondProfile(
        shadowId = shadowId,
        accountOwnerId = accountOwnerId,
        bondLevel = bondLevel,
        bondPoints = bondPoints,
        loyaltyRating = loyaltyRating,
        trustRating = trustRating,
        relationshipStage = runCatching { ShadowRelationshipStage.valueOf(relationshipStageName) }
            .getOrDefault(ShadowRelationshipStage.UNFAMILIAR),
        personality = runCatching { ShadowPersonality.valueOf(personalityCharacteristicName) }
            .getOrDefault(ShadowPersonality.DISCIPLINED),
        totalActivitiesCount = totalActivitiesCount,
        lastBondActivityEpochMs = lastBondActivityEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

fun ShadowBondProfile.toEntity(): ShadowBondProfileEntity {
    return ShadowBondProfileEntity(
        shadowId = shadowId,
        accountOwnerId = accountOwnerId,
        bondLevel = bondLevel,
        bondPoints = bondPoints,
        loyaltyRating = loyaltyRating,
        trustRating = trustRating,
        relationshipStageName = relationshipStage.name,
        personalityCharacteristicName = personality.name,
        totalActivitiesCount = totalActivitiesCount,
        lastBondActivityEpochMs = lastBondActivityEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

fun ShadowBondEventEntity.toDomain(): ShadowBondEvent {
    return ShadowBondEvent(
        eventId = eventId,
        accountOwnerId = accountOwnerId,
        shadowId = shadowId,
        activityType = runCatching { ShadowBondActivityType.valueOf(activityTypeName) }
            .getOrDefault(ShadowBondActivityType.DEPLOYMENT_SUCCESS),
        sourceEventId = sourceEventId,
        bondPointsAwarded = bondPointsAwarded,
        loyaltyDelta = loyaltyDelta,
        trustDelta = trustDelta,
        description = description,
        timestampEpochMs = timestampEpochMs,
        idempotencyKey = idempotencyKey
    )
}

fun ShadowBondEvent.toEntity(): ShadowBondEventEntity {
    return ShadowBondEventEntity(
        eventId = eventId,
        accountOwnerId = accountOwnerId,
        shadowId = shadowId,
        activityTypeName = activityType.name,
        sourceEventId = sourceEventId,
        bondPointsAwarded = bondPointsAwarded,
        loyaltyDelta = loyaltyDelta,
        trustDelta = trustDelta,
        description = description,
        timestampEpochMs = timestampEpochMs,
        idempotencyKey = idempotencyKey
    )
}

fun ShadowBondMilestoneEntity.toDomain(): ShadowBondMilestone {
    return ShadowBondMilestone(
        milestoneId = milestoneId,
        accountOwnerId = accountOwnerId,
        shadowId = shadowId,
        milestoneType = runCatching { ShadowBondMilestoneType.valueOf(milestoneTypeName) }
            .getOrDefault(ShadowBondMilestoneType.FIRST_DEPLOYMENT),
        title = title,
        description = description,
        sourceEventId = sourceEventId,
        timestampEpochMs = timestampEpochMs,
        verificationStatus = verificationStatus
    )
}

fun ShadowBondMilestone.toEntity(): ShadowBondMilestoneEntity {
    return ShadowBondMilestoneEntity(
        milestoneId = milestoneId,
        accountOwnerId = accountOwnerId,
        shadowId = shadowId,
        milestoneTypeName = milestoneType.name,
        title = title,
        description = description,
        sourceEventId = sourceEventId,
        timestampEpochMs = timestampEpochMs,
        verificationStatus = verificationStatus
    )
}
