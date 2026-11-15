package com.example

import com.example.domain.engine.ProgressionEngine
import com.example.domain.model.AttributeType
import com.example.domain.model.HunterRank
import com.example.domain.model.PlayerAttributes
import com.example.domain.model.QuestCategory
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {

    @Test
    fun applyXp_advancesMultipleLevelsDeterministically() {
        // Level 1 requires 100 XP, Level 2 requires 165 XP (total 265 XP for Lv 3)
        val result = ProgressionEngine.applyXp(
            currentLevel = 1,
            currentLevelXp = 0,
            totalXp = 0L,
            gainedXp = 280
        )

        assertEquals(3, result.newLevel)
        assertEquals(15, result.newCurrentLevelXp)
        assertEquals(280L, result.newTotalXp)
        assertEquals(2, result.levelsGained)
    }

    @Test
    fun applyAttributeXp_separatesVerifiedAndSelfReportedPoints() {
        val initial = PlayerAttributes()
        val afterVerified = ProgressionEngine.applyAttributeXp(
            attributes = initial,
            deltas = mapOf(AttributeType.VIT to 120),
            isVerified = true
        )
        assertEquals(11, afterVerified.vit.totalValue)
        assertEquals(1, afterVerified.vit.verifiedPoints)
        assertEquals(0, afterVerified.vit.selfReportedPoints)
        assertEquals(20, afterVerified.vit.progressXp)

        val afterSelf = ProgressionEngine.applyAttributeXp(
            attributes = afterVerified,
            deltas = mapOf(AttributeType.VIT to 90),
            isVerified = false
        )
        assertEquals(12, afterSelf.vit.totalValue)
        assertEquals(1, afterSelf.vit.verifiedPoints)
        assertEquals(1, afterSelf.vit.selfReportedPoints)
        assertEquals(10, afterSelf.vit.progressXp)
    }

    @Test
    fun calculateActivityReward_weightsVerifiedHigherThanSelfReported() {
        val verified = ProgressionEngine.calculateActivityReward(
            category = QuestCategory.PHYSICAL,
            durationMinutes = 30,
            verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT
        )
        val selfReported = ProgressionEngine.calculateActivityReward(
            category = QuestCategory.PHYSICAL,
            durationMinutes = 30,
            verificationStatus = VerificationStatus.SELF_REPORTED
        )

        assertTrue(verified.xpAwarded > selfReported.xpAwarded)
        assertTrue(verified.primaryAttrXpAwarded > selfReported.primaryAttrXpAwarded)
    }

    @Test
    fun evaluateRank_enforcesAllThreeThresholds() {
        // High level & attributes, but 0 verified activities -> still E-Rank
        assertEquals(
            HunterRank.E_RANK,
            ProgressionEngine.evaluateRank(level = 10, totalAttributeScore = 100, verifiedActivitiesCount = 0)
        )
        // Meets all D-Rank thresholds (Lv 5, 85 total attrs, 3 verified)
        assertEquals(
            HunterRank.D_RANK,
            ProgressionEngine.evaluateRank(level = 5, totalAttributeScore = 85, verifiedActivitiesCount = 3)
        )
    }
}
