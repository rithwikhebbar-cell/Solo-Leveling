package com.example

import com.example.domain.engine.ActivityRewardCalculation
import com.example.domain.engine.ProgressionCommand
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.ProgressionOrchestrator
import com.example.domain.model.AttributeType
import com.example.domain.model.PlayerProfile
import com.example.domain.model.SocialActivityKind
import com.example.domain.model.SocialEvidenceSource
import com.example.domain.model.SocialMasteryStage
import com.example.domain.model.SocialModuleIdentifier
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Milestone 12: Social Mastery Domain Foundation & Progression Unit Tests.
 */
class SocialMasteryDomainUnitTest {

    @Test
    fun testEightModuleCatalogueCompleteness() {
        val modules = SocialModuleIdentifier.entries
        assertEquals("There must be exactly 8 social modules in the catalogue", 8, modules.size)

        val expectedCodes = setOf(
            "COMMUNICATION_MASTERY",
            "PUBLIC_SPEAKING",
            "NEGOTIATION_PERSUASION",
            "LEADERSHIP_TEAMWORK",
            "NETWORKING_RELATIONSHIPS",
            "EMOTIONAL_INTELLIGENCE",
            "PROFESSIONAL_EXCELLENCE",
            "LANGUAGE_DEVELOPMENT"
        )
        val actualCodes = modules.map { it.code }.toSet()
        assertEquals(expectedCodes, actualCodes)
    }

    @Test
    fun testSocialAttributeRatiosAndValidity() {
        for (module in SocialModuleIdentifier.entries) {
            // Verify attribute validity: only CHA, WIS, DIS, INT are valid social attributes
            assertTrue(
                "Primary attribute must be CHA, WIS, DIS, or INT",
                module.primaryAttribute in listOf(AttributeType.CHA, AttributeType.WIS, AttributeType.DIS, AttributeType.INT)
            )
            assertTrue(
                "Secondary attribute must be CHA, WIS, DIS, or INT",
                module.secondaryAttribute in listOf(AttributeType.CHA, AttributeType.WIS, AttributeType.DIS, AttributeType.INT)
            )
            val sum = module.primaryRatio + module.secondaryRatio
            assertEquals("Attribute ratios must sum to 1.0f", 1.0f, sum, 0.01f)
        }
    }

    @Test
    fun testMasteryStageOrdinalConsistency() {
        assertEquals(0, SocialMasteryStage.NOVICE.ordinalValue)
        assertEquals(1, SocialMasteryStage.COMPETENT.ordinalValue)
        assertEquals(2, SocialMasteryStage.PROFICIENT.ordinalValue)
        assertEquals(3, SocialMasteryStage.ADVANCED.ordinalValue)
        assertEquals(4, SocialMasteryStage.MASTER.ordinalValue)

        assertEquals(SocialMasteryStage.MASTER, SocialMasteryStage.fromOrdinal(4))
        assertEquals(SocialMasteryStage.NOVICE, SocialMasteryStage.fromString("novice"))
    }

    @Test
    fun testCalculateSocialRewardAuthoritative() {
        // In-app speaking rehearsal timer (0.85x multiplier)
        val calc = ProgressionEngine.calculateSocialReward(
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            activityKind = SocialActivityKind.SPEAKING_REHEARSAL,
            durationMinutes = 20,
            verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
            socialXpAlreadyEarnedToday = 0,
            selfReportedSocialCountToday = 0,
            rubricScorePercent = 85
        )

        assertTrue("XP awarded must be > 0", calc.xpAwarded > 0)
        assertEquals(AttributeType.CHA, calc.primaryAttribute)
        assertEquals(AttributeType.WIS, calc.secondaryAttribute)
        assertTrue("Primary attribute XP awarded must be > 0", calc.primaryAttrXpAwarded > 0)
        assertTrue("Secondary attribute XP awarded must be > 0", calc.secondaryAttrXpAwarded > 0)
        assertFalse(calc.wasCapped)
    }

    @Test
    fun testCalculateSocialRewardEnforcesDailyCap() {
        val calc = ProgressionEngine.calculateSocialReward(
            module = SocialModuleIdentifier.PUBLIC_SPEAKING,
            activityKind = SocialActivityKind.PRESENTATION,
            durationMinutes = 60,
            verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
            socialXpAlreadyEarnedToday = ProgressionEngine.DAILY_SOCIAL_XP_CAP,
            selfReportedSocialCountToday = 0
        )

        assertEquals("When daily cap is reached, 0 XP should be awarded", 0, calc.xpAwarded)
        assertEquals(0, calc.primaryAttrXpAwarded)
        assertEquals(0, calc.secondaryAttrXpAwarded)
        assertTrue(calc.wasCapped)
    }

    @Test
    fun testSelfReportedSocialCountLimit() {
        val calc = ProgressionEngine.calculateSocialReward(
            module = SocialModuleIdentifier.NETWORKING_RELATIONSHIPS,
            activityKind = SocialActivityKind.REAL_WORLD_ACTIVITY,
            durationMinutes = 30,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            socialXpAlreadyEarnedToday = 0,
            selfReportedSocialCountToday = ProgressionEngine.DAILY_SELF_REPORTED_SOCIAL_LIMIT
        )

        assertEquals("Self-reported actions beyond daily limit must award 0 XP", 0, calc.xpAwarded)
        assertTrue(calc.wasCapped)
    }

    @Test
    fun testProgressionOrchestratorIntegrityWithSocialCommand() {
        val calc = ProgressionEngine.calculateSocialReward(
            module = SocialModuleIdentifier.LEADERSHIP_TEAMWORK,
            activityKind = SocialActivityKind.SCENARIO,
            durationMinutes = 15,
            verificationStatus = VerificationStatus.VERIFIED_ASSESSMENT,
            socialXpAlreadyEarnedToday = 50,
            selfReportedSocialCountToday = 1,
            rubricScorePercent = 92
        )

        val cmd = ProgressionCommand.fromSocialReward(
            idempotencyKey = "social_test_key_001",
            sourceRecordId = "session_123",
            title = "Leadership Drill",
            calculation = calc,
            verificationStatus = VerificationStatus.VERIFIED_ASSESSMENT
        )

        val profile = PlayerProfile(level = 5, totalXp = 1000L)
        val outcome = ProgressionOrchestrator.applyValidatedReward(
            currentProfile = profile,
            command = cmd,
            alreadyProcessedKeys = emptySet()
        ).getOrThrow()

        assertNotNull(outcome)
        assertTrue(outcome.updatedProfile.totalXp > profile.totalXp)
        assertTrue(outcome.wasVerifiedIncremented)
    }
}
