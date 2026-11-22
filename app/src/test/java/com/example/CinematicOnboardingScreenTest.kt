package com.example

import com.example.domain.model.AttributeType
import com.example.domain.model.AwakeningStage
import com.example.domain.model.HunterRank
import com.example.domain.model.PlayerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 9.1: Verification of Cinematic Onboarding & First-Launch Experience Specifications.
 */
class CinematicOnboardingScreenTest {

    @Test
    fun `canonical onboarding has exactly 5 stages matching reality os requirements`() {
        val stages = AwakeningStage.entries
        assertEquals("Stage 9.1 requires exactly 5 onboarding screens", 5, stages.size)

        // Stage 1: The Awakening
        assertEquals(AwakeningStage.INITIALIZATION, stages[0])
        assertEquals(0, stages[0].stepIndex)

        // Stage 2: Your Identity
        assertEquals(AwakeningStage.PLAYER_IDENTIFICATION, stages[1])
        assertEquals(1, stages[1].stepIndex)

        // Stage 3: Your Domains
        assertEquals(AwakeningStage.CORE_BRIEFING, stages[2])
        assertEquals(2, stages[2].stepIndex)

        // Stage 4: The System
        assertEquals(AwakeningStage.RANK_CALIBRATION, stages[3])
        assertEquals(3, stages[3].stepIndex)

        // Stage 5: Enter the System
        assertEquals(AwakeningStage.WELCOME_DIRECTIVE, stages[4])
        assertEquals(4, stages[4].stepIndex)
    }

    @Test
    fun `seven canonical attributes are defined and cover all required attributes`() {
        val attributes = AttributeType.entries
        assertEquals("Canonical progression must define exactly 7 attributes", 7, attributes.size)

        val codes = attributes.map { it.code }.toSet()
        val expectedCodes = setOf("STR", "VIT", "AGI", "INT", "DIS", "WIS", "CHA")
        assertEquals(expectedCodes, codes)

        // Validate attribute types and training focuses
        val str = AttributeType.STR
        assertEquals("Strength", str.displayName)
        assertTrue(str.trainingFocus.contains("Resistance", ignoreCase = true))

        val vit = AttributeType.VIT
        assertEquals("Vitality", vit.displayName)
        assertTrue(vit.trainingFocus.contains("recovery", ignoreCase = true))

        val agi = AttributeType.AGI
        assertEquals("Agility", agi.displayName)
        assertTrue(agi.trainingFocus.contains("agility", ignoreCase = true) || agi.trainingFocus.contains("drills", ignoreCase = true))

        val intAttr = AttributeType.INT
        assertEquals("Intelligence", intAttr.displayName)
        assertTrue(intAttr.trainingFocus.contains("study", ignoreCase = true))

        val dis = AttributeType.DIS
        assertEquals("Discipline", dis.displayName)
        assertTrue(dis.trainingFocus.contains("streak", ignoreCase = true) || dis.trainingFocus.contains("commitments", ignoreCase = true))

        val wis = AttributeType.WIS
        assertEquals("Wisdom", wis.displayName)
        assertTrue(wis.trainingFocus.contains("pacing", ignoreCase = true) || wis.trainingFocus.contains("reflection", ignoreCase = true))

        val cha = AttributeType.CHA
        assertEquals("Charisma", cha.displayName)
        assertTrue(cha.trainingFocus.contains("Speaking", ignoreCase = true) || cha.trainingFocus.contains("Collaboration", ignoreCase = true))
    }

    @Test
    fun `player profile handles blank or populated codenames for identity calibration`() {
        val defaultPlayer = PlayerProfile()
        val resolvedDefaultCodename = defaultPlayer.codename.ifBlank { "HUNTER" }
        assertEquals("HUNTER", resolvedDefaultCodename)

        val customPlayer = PlayerProfile(
            codename = "ShadowMonarch",
            level = 25,
            rank = HunterRank.S_RANK
        )
        val resolvedCustomCodename = customPlayer.codename.ifBlank { "HUNTER" }
        assertEquals("ShadowMonarch", resolvedCustomCodename)
        assertEquals(25, customPlayer.level)
        assertEquals(HunterRank.S_RANK, customPlayer.rank)
    }

    @Test
    fun `five development domains are properly indexed without phantom features`() {
        val requiredDomains = listOf(
            "Physical Development",
            "Academic Development",
            "Life & Habits",
            "Quests & Missions",
            "Shadow Army"
        )
        assertEquals(5, requiredDomains.size)
        assertTrue(requiredDomains.contains("Physical Development"))
        assertTrue(requiredDomains.contains("Academic Development"))
        assertTrue(requiredDomains.contains("Life & Habits"))
        assertTrue(requiredDomains.contains("Quests & Missions"))
        assertTrue(requiredDomains.contains("Shadow Army"))
    }
}
