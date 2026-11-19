package com.example.domain.engine

import com.example.data.local.SoloLevelingDatabase
import com.example.domain.model.ShadowEvidenceEligibility
import com.example.domain.model.ShadowExtractionEvidenceItem
import com.example.domain.model.ShadowExtractionSourceType

class ShadowExtractionEvidenceProvider(
    private val database: SoloLevelingDatabase
) {
    suspend fun getEligibleEvidenceItems(accountOwnerId: String): List<ShadowExtractionEvidenceItem> {
        val owner = accountOwnerId.trim()
        if (owner.isBlank()) return emptyList()

        val items = mutableListOf<ShadowExtractionEvidenceItem>()

        // Collect claimed IDs from both extractions and shadow_evidence_claims
        val claimedEventIds = runCatching {
            database.shadowExtractionDao().getAllExtractionsForOwnerOnce(owner).map { it.sourceEventId }.toSet()
        }.getOrDefault(emptySet())

        val explicitClaims = runCatching {
            database.shadowEvidenceClaimDao().getClaimsForOwnerOnce(owner).map { it.sourceRecordId }.toSet()
        }.getOrDefault(emptySet())

        val allClaimedIds = claimedEventIds + explicitClaims

        // 1. Dungeons
        val dungeons = runCatching { database.dungeonDao().getAllInstancesOnce() }.getOrNull() ?: emptyList()
        val dungeonDefs = runCatching { database.dungeonDao().getAllDefinitionsOnce() }.getOrNull()?.associateBy { it.id } ?: emptyMap()

        for (dungeon in dungeons) {
            val isCompleted = dungeon.statusName == "COMPLETED" || dungeon.statusName == "CLEARED" || dungeon.completedAtEpochMs != null
            val sourceId = dungeon.id.toString()
            val isClaimed = allClaimedIds.contains(sourceId)
            val eligibility = when {
                isClaimed -> ShadowEvidenceEligibility.ALREADY_EXTRACTED
                !isCompleted -> ShadowEvidenceEligibility.INCOMPLETE
                else -> ShadowEvidenceEligibility.ELIGIBLE
            }
            val reason = when (eligibility) {
                ShadowEvidenceEligibility.ELIGIBLE -> "Ready for Shadow Extraction"
                ShadowEvidenceEligibility.ALREADY_EXTRACTED -> "Source already claimed for extraction"
                ShadowEvidenceEligibility.INCOMPLETE -> "Dungeon challenge not yet completed"
                else -> "Unavailable"
            }
            val defTitle = dungeonDefs[dungeon.dungeonId]?.title ?: "Gate #${dungeon.dungeonId}"
            items.add(
                ShadowExtractionEvidenceItem(
                    sourceRecordId = sourceId,
                    accountOwnerId = owner,
                    sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
                    title = "Dungeon: $defTitle",
                    description = "Completed dungeon gate trial (#${dungeon.dungeonId})",
                    timestampEpochMs = dungeon.completedAtEpochMs ?: dungeon.startedAtEpochMs,
                    verificationStatus = if (isCompleted) "VERIFIED_SYSTEM_COMPLETION" else "PENDING",
                    eligibility = eligibility,
                    eligibilityReason = reason,
                    rewardSummary = "Dungeon Tier Reward & Potential Shadow Recruit"
                )
            )
        }

        // 2. Boss Battles
        val bosses = runCatching { database.bossBattleDao().getAllBossInstancesOnce() }.getOrNull() ?: emptyList()
        val bossDefs = runCatching { database.bossBattleDao().getAllBossDefinitionsOnce() }.getOrNull()?.associateBy { it.id } ?: emptyMap()

        for (boss in bosses) {
            val isDefeated = boss.statusName == "DEFEATED" || boss.statusName == "COMPLETED" || boss.completedAtEpochMs != null
            val sourceId = boss.id.toString()
            val isClaimed = allClaimedIds.contains(sourceId)
            val eligibility = when {
                isClaimed -> ShadowEvidenceEligibility.ALREADY_EXTRACTED
                !isDefeated -> ShadowEvidenceEligibility.INCOMPLETE
                else -> ShadowEvidenceEligibility.ELIGIBLE
            }
            val reason = when (eligibility) {
                ShadowEvidenceEligibility.ELIGIBLE -> "Boss defeated; extraction vector stable"
                ShadowEvidenceEligibility.ALREADY_EXTRACTED -> "Boss source already claimed"
                ShadowEvidenceEligibility.INCOMPLETE -> "Boss battle not yet won"
                else -> "Unavailable"
            }
            val bossTitle = bossDefs[boss.bossId]?.title ?: "Boss #${boss.bossId}"
            items.add(
                ShadowExtractionEvidenceItem(
                    sourceRecordId = sourceId,
                    accountOwnerId = owner,
                    sourceType = ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION,
                    title = "Boss Defeated: $bossTitle",
                    description = "Subjugated boss combat entity (${boss.statusName})",
                    timestampEpochMs = boss.completedAtEpochMs ?: boss.startedAtEpochMs,
                    verificationStatus = if (isDefeated) "VERIFIED_COMBAT_LOG" else "PENDING",
                    eligibility = eligibility,
                    eligibilityReason = reason,
                    rewardSummary = "Boss Subjugation & High Rarity Shadow Potential"
                )
            )
        }

        // 3. Achievements
        val achievements = runCatching { database.achievementTitleDao().getAllAchievementUnlocksOnce() }.getOrNull() ?: emptyList()
        val achDefs = runCatching { database.achievementTitleDao().getAllAchievementDefinitionsOnce() }.getOrNull()?.associateBy { it.id } ?: emptyMap()

        for (ach in achievements) {
            val sourceId = ach.achievementId
            val isClaimed = allClaimedIds.contains(sourceId)
            val eligibility = when {
                isClaimed -> ShadowEvidenceEligibility.ALREADY_EXTRACTED
                else -> ShadowEvidenceEligibility.ELIGIBLE
            }
            val reason = when (eligibility) {
                ShadowEvidenceEligibility.ELIGIBLE -> "Achievement unlocked and verified"
                ShadowEvidenceEligibility.ALREADY_EXTRACTED -> "Achievement source already claimed"
                else -> "Unavailable"
            }
            val achTitle = achDefs[ach.achievementId]?.title ?: "Achievement: ${ach.achievementId}"
            items.add(
                ShadowExtractionEvidenceItem(
                    sourceRecordId = sourceId,
                    accountOwnerId = owner,
                    sourceType = ShadowExtractionSourceType.ACHIEVEMENT_UNLOCK,
                    title = achTitle,
                    description = ach.evidenceReference.ifBlank { "Verified achievement unlock" },
                    timestampEpochMs = ach.unlockedAtEpochMs,
                    verificationStatus = "VERIFIED_MILESTONE",
                    eligibility = eligibility,
                    eligibilityReason = reason,
                    rewardSummary = "Milestone Recognition & Elite Resonance"
                )
            )
        }

        // 4. Verified Social Missions
        val completedMissions = runCatching {
            database.socialMissionDao().getRecordsByStatus(owner, "COMPLETED")
        }.getOrNull() ?: emptyList()

        for (mission in completedMissions) {
            val sourceId = mission.recordId
            val isClaimed = allClaimedIds.contains(sourceId)
            val isVerified = mission.verificationStatus == "VERIFIED" || mission.verificationStatus == "VERIFIED_SYSTEM"
            val isOwnerMatch = mission.accountOwnerId == owner

            val eligibility = when {
                !isOwnerMatch -> ShadowEvidenceEligibility.WRONG_ACCOUNT
                isClaimed -> ShadowEvidenceEligibility.ALREADY_EXTRACTED
                !isVerified -> ShadowEvidenceEligibility.NOT_VERIFIED
                else -> ShadowEvidenceEligibility.ELIGIBLE
            }

            val reason = when (eligibility) {
                ShadowEvidenceEligibility.ELIGIBLE -> "Verified social mastery mission completed"
                ShadowEvidenceEligibility.ALREADY_EXTRACTED -> "Social mission source already claimed"
                ShadowEvidenceEligibility.NOT_VERIFIED -> "Social mission not independently verified"
                ShadowEvidenceEligibility.WRONG_ACCOUNT -> "Record belongs to another account"
                else -> "Unavailable"
            }

            items.add(
                ShadowExtractionEvidenceItem(
                    sourceRecordId = sourceId,
                    accountOwnerId = owner,
                    sourceType = ShadowExtractionSourceType.CONSISTENCY_MILESTONE,
                    title = "Social Mission: ${mission.title}",
                    description = mission.reflectionText ?: "Verified social competency challenge completed",
                    timestampEpochMs = mission.completedEpochMs ?: mission.startedEpochMs,
                    verificationStatus = mission.verificationStatus,
                    eligibility = eligibility,
                    eligibilityReason = reason,
                    rewardSummary = "Social Mastery Resonance & Potential Shadow Recruit"
                )
            )
        }

        return items.sortedByDescending { it.timestampEpochMs }
    }
}
