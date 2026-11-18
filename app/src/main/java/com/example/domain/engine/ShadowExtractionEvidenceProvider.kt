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
        val claimedEventIds = database.shadowExtractionDao().getAllExtractionsForOwnerOnce(owner)
            .map { it.sourceEventId }.toSet()

        // 1. Dungeons
        val dungeons = runCatching { database.dungeonDao().getAllInstancesOnce() }.getOrNull() ?: emptyList()
        for (dungeon in dungeons) {
            val isCompleted = dungeon.statusName == "COMPLETED" || dungeon.completedAtEpochMs != null
            val sourceId = dungeon.id.toString()
            val isClaimed = claimedEventIds.contains(sourceId)
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
            items.add(
                ShadowExtractionEvidenceItem(
                    sourceRecordId = sourceId,
                    accountOwnerId = owner,
                    sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
                    title = "Dungeon Gate #${dungeon.dungeonId}",
                    description = "Completed dungeon gate trial",
                    timestampEpochMs = dungeon.startedAtEpochMs,
                    verificationStatus = if (isCompleted) "VERIFIED_SYSTEM_COMPLETION" else "PENDING",
                    eligibility = eligibility,
                    eligibilityReason = reason,
                    rewardSummary = "Dungeon Tier Reward & Potential Shadow Recruit"
                )
            )
        }

        // 2. Boss Battles
        val bosses = runCatching { database.bossBattleDao().getAllBossInstancesOnce() }.getOrNull() ?: emptyList()
        for (boss in bosses) {
            val isDefeated = boss.statusName == "DEFEATED" || boss.completedAtEpochMs != null
            val sourceId = boss.id.toString()
            val isClaimed = claimedEventIds.contains(sourceId)
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
            items.add(
                ShadowExtractionEvidenceItem(
                    sourceRecordId = sourceId,
                    accountOwnerId = owner,
                    sourceType = ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION,
                    title = "Boss Instance #${boss.bossId}",
                    description = "Subjugated boss combat entity",
                    timestampEpochMs = boss.startedAtEpochMs,
                    verificationStatus = if (isDefeated) "VERIFIED_COMBAT_LOG" else "PENDING",
                    eligibility = eligibility,
                    eligibilityReason = reason,
                    rewardSummary = "Boss Subjugation & High Rarity Shadow Potential"
                )
            )
        }

        // 3. Achievements
        val achievements = runCatching { database.achievementTitleDao().getAllAchievementUnlocksOnce() }.getOrNull() ?: emptyList()
        for (ach in achievements) {
            val sourceId = ach.achievementId
            val isClaimed = claimedEventIds.contains(sourceId)
            val eligibility = when {
                isClaimed -> ShadowEvidenceEligibility.ALREADY_EXTRACTED
                else -> ShadowEvidenceEligibility.ELIGIBLE
            }
            val reason = when (eligibility) {
                ShadowEvidenceEligibility.ELIGIBLE -> "Achievement unlocked and verified"
                ShadowEvidenceEligibility.ALREADY_EXTRACTED -> "Achievement source already claimed"
                else -> "Unavailable"
            }
            items.add(
                ShadowExtractionEvidenceItem(
                    sourceRecordId = sourceId,
                    accountOwnerId = owner,
                    sourceType = ShadowExtractionSourceType.ACHIEVEMENT_UNLOCK,
                    title = "Achievement: ${ach.achievementId}",
                    description = ach.evidenceReference,
                    timestampEpochMs = ach.unlockedAtEpochMs,
                    verificationStatus = "VERIFIED_MILESTONE",
                    eligibility = eligibility,
                    eligibilityReason = reason,
                    rewardSummary = "Milestone Recognition & Elite Resonance"
                )
            )
        }

        return items.sortedByDescending { it.timestampEpochMs }
    }
}
