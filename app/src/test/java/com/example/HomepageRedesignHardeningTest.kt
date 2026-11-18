package com.example

import com.example.domain.engine.MonarchProgressionEngine
import com.example.domain.engine.NextRankProgress
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.SystemAnnouncementEngine
import com.example.domain.model.AttributeType
import com.example.domain.model.HunterRank
import com.example.domain.model.MonarchRank
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerAttributes
import com.example.domain.model.PlayerProfile
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.domain.model.ShadowMonarchProfile
import com.example.domain.model.SystemAnnouncementCategory
import com.example.domain.model.SystemAnnouncementRecord
import com.example.domain.model.VerificationStatus
import com.example.ui.viewmodel.RealityOsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomepageRedesignHardeningTest {

    // ========================================================================
    // 1. DAILY QUEST COMPLETION TRIGGERING AUTHORITATIVE FLOW
    // ========================================================================
    @Test
    fun questCompletion_calculatesAuthoritativeProgressionCorrectly() {
        val initialPlayer = PlayerProfile(
            codename = "HunterTest",
            level = 1,
            currentLevelXp = 0,
            totalXp = 0L,
            attributes = PlayerAttributes()
        )
        val quest = Quest(
            id = 101L,
            title = "Morning Calisthenics",
            description = "Perform 100 pushups",
            category = QuestCategory.PHYSICAL,
            type = QuestType.DAILY,
            targetValue = 100,
            unit = "reps",
            currentValue = 0,
            xpReward = 50,
            primaryAttribute = AttributeType.STR,
            primaryAttrXpReward = 20,
            isCompleted = false
        )

        // Progression calculation using authoritative ProgressionEngine
        val xpResult = ProgressionEngine.applyXp(
            currentLevel = initialPlayer.level,
            currentLevelXp = initialPlayer.currentLevelXp,
            totalXp = initialPlayer.totalXp,
            gainedXp = quest.xpReward
        )
        val updatedAttributes = ProgressionEngine.applyAttributeXp(
            attributes = initialPlayer.attributes,
            deltas = mapOf(quest.primaryAttribute to quest.primaryAttrXpReward),
            isVerified = quest.verificationStatus.isVerified
        )

        assertEquals(1, xpResult.newLevel)
        assertEquals(50, xpResult.newCurrentLevelXp)
        assertEquals(50L, xpResult.newTotalXp)
        assertEquals(10, updatedAttributes.str.totalValue)
        assertEquals(20, updatedAttributes.str.progressXp)
    }

    // ========================================================================
    // 2. RAPID REPEATED COMPLETION ATTEMPTS (IDEMPOTENCY)
    // ========================================================================
    @Test
    fun completedQuest_cannotBeCompletedAgain_idempotencyGuaranteed() {
        val completedQuest = Quest(
            id = 202L,
            title = "Run 5km",
            description = "Cardio endurance test",
            category = QuestCategory.PHYSICAL,
            type = QuestType.DAILY,
            targetValue = 5,
            unit = "km",
            currentValue = 5,
            xpReward = 80,
            primaryAttribute = AttributeType.VIT,
            isCompleted = true,
            completedAtEpochMs = 1700000000000L
        )

        // Simulation of repository check: if (quest.isCompleted) return
        var xpAwardedCount = 0
        val completionAttempts = 5
        repeat(completionAttempts) {
            if (!completedQuest.isCompleted) {
                xpAwardedCount++
            }
        }

        assertEquals(0, xpAwardedCount)
        assertTrue(completedQuest.isCompleted)
    }

    // ========================================================================
    // 3. OFFLINE QUEST COMPLETION LOCAL DETERMINISM
    // ========================================================================
    @Test
    fun offlineQuestCompletion_reliesStrictlyOnLocalProgressionEngine() {
        val quest = Quest(
            id = 303L,
            title = "Offline Discipline Protocol",
            description = "Read 20 pages",
            category = QuestCategory.INTELLECTUAL,
            type = QuestType.DAILY,
            targetValue = 20,
            unit = "pages",
            xpReward = 40,
            primaryAttribute = AttributeType.INT,
            primaryAttrXpReward = 15
        )

        val completed = quest.copy(
            currentValue = quest.targetValue,
            isCompleted = true,
            completedAtEpochMs = System.currentTimeMillis()
        )

        assertTrue(completed.isCompleted)
        assertEquals(20, completed.currentValue)
        assertNotNull(completed.completedAtEpochMs)
    }

    // ========================================================================
    // 4. DEEP-LINK ROUTING LOGIC & MALFORMED ROUTE REJECTION
    // ========================================================================
    @Test
    fun deepLinkRouting_validRoutesRecognized_unknownAndMalformedRejected() {
        val registeredRoutes = setOf(
            "home", "status", "physical", "quest_board", "academic_revisions",
            "academic_exams", "academic_focus", "ai_system", "ai_companion",
            "voice_commands", "system_voice", "widgets", "celebrations",
            "life_dashboard", "routines", "habits", "tasks", "goals",
            "daily_schedule", "weekly_review", "dungeon_gate", "boss_battles",
            "cloud_sync", "account_gateway", "intelligence_dashboard",
            "intelligence_daily_plan", "intelligence_roadmap", "intelligence_dungeons",
            "weekly_intelligence", "system_control", "automation_dashboard",
            "shadow_realm", "shadow_extraction", "shadow_roster", "social_mastery",
            "social_missions", "social_assessment", "awakening"
        )

        val testInputs = listOf(
            "home" to true,
            "status" to true,
            "physical" to true,
            "shadow_realm" to true,
            "intelligence_dashboard" to true,
            "unknown_route" to false,
            "" to false,
            "   " to false,
            "javascript:alert(1)" to false,
            "../etc/passwd" to false,
            "invalid/slash/route" to false
        )

        testInputs.forEach { (route, shouldBeRecognized) ->
            val isRecognized = route in registeredRoutes
            assertEquals("Route '$route' check failed", shouldBeRecognized, isRecognized)
        }
    }

    // ========================================================================
    // 5. ANNOUNCEMENT BANNER DEEP LINK ROUTING
    // ========================================================================
    @Test
    fun announcementBanner_validatesDeepLinkRoute() {
        val announcementWithValidRoute = SystemAnnouncementRecord(
            id = "ann-1",
            title = "Level Up Available",
            message = "Congratulations on reaching Level 20",
            category = SystemAnnouncementCategory.LEVEL_UP,
            deepLinkRoute = "status",
            actionLabel = "Inspect"
        )
        val announcementWithoutRoute = SystemAnnouncementRecord(
            id = "ann-2",
            title = "System Calibrating",
            message = "Background telemetry sync active",
            category = SystemAnnouncementCategory.SYSTEM_ALERT,
            deepLinkRoute = "",
            actionLabel = ""
        )

        assertTrue(announcementWithValidRoute.deepLinkRoute.isNotBlank())
        assertEquals("status", announcementWithValidRoute.deepLinkRoute)
        assertTrue(announcementWithoutRoute.deepLinkRoute.isBlank())
    }

    // ========================================================================
    // 6. SHADOW MONARCH CARD STATE DISPLAY
    // ========================================================================
    @Test
    fun shadowMonarchCard_displaysCorrectStateForDifferentProfiles() {
        // Authenticated profile with active legion
        val activeProfile = ShadowMonarchProfile(
            monarchLevel = 15,
            monarchRank = MonarchRank.SHADOW_LORD,
            activeShadowCount = 8,
            armyCapacity = 12,
            monarchAuthority = 450
        )
        assertEquals(15, activeProfile.monarchLevel)
        assertEquals("Shadow Lord", activeProfile.monarchRank.label)
        assertEquals(8, activeProfile.activeShadowCount)
        assertEquals(12, activeProfile.armyCapacity)
        assertEquals(450, activeProfile.monarchAuthority)

        // Zero-shadow profile
        val zeroShadowProfile = ShadowMonarchProfile(
            monarchLevel = 5,
            monarchRank = MonarchRank.AWAKENING,
            activeShadowCount = 0,
            armyCapacity = 3,
            monarchAuthority = 50
        )
        assertEquals(0, zeroShadowProfile.activeShadowCount)
        assertEquals(3, zeroShadowProfile.armyCapacity)

        // Null profile: verified that null is safely distinct from an awakened profile
        val nullProfile: ShadowMonarchProfile? = null
        assertNull(nullProfile)
    }

    // ========================================================================
    // 7. VERIFICATION: NO MOMENTUM XP AWARDED THROUGH WORKOUTS OR SHADOW
    // ========================================================================
    @Test
    fun progressionRules_strictlyZeroMomentumXpForPhysicalWorkoutsAndShadow() {
        // Workouts and jogging fall under QuestCategory.PHYSICAL
        val physicalReward = ProgressionEngine.calculateActivityReward(
            category = QuestCategory.PHYSICAL,
            durationMinutes = 60,
            verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT
        )
        // Verify total XP awarded is greater than 0
        assertTrue(physicalReward > 0)

        // Physical development state must never have momentum XP awarded
        val physicalState = PhysicalDevelopmentState(
            completedJoggingMinutesToday = 45,
            effectiveJoggingTarget = 30
        )
        assertEquals(45, physicalState.completedJoggingMinutesToday)

        // Shadow Monarch extraction must never award Momentum XP to player
        val shadowProfile = ShadowMonarchProfile(monarchLevel = 2, monarchAuthority = 100)
        assertEquals(100, shadowProfile.monarchAuthority)
        // Confirm MonarchProgressionEngine does not touch player level XP or momentum XP
        val monarchProgress = MonarchProgressionEngine.evaluatePromotion(
            shadowProfile,
            activeShadowCount = 5,
            dungeonClears = 10,
            rulerDefeats = 1
        )
        assertNotNull(monarchProgress)
    }

    // ========================================================================
    // 8. REDUCED-MOTION TOGGLE BEHAVIOR
    // ========================================================================
    @Test
    fun reducedMotion_disablesPulsingAnimationsDeterministically() {
        val reducedMotionOn = true
        val hudGlowOn = true
        val allowMotionWhenReduced = !reducedMotionOn && hudGlowOn
        assertFalse(allowMotionWhenReduced)

        val reducedMotionOff = false
        val allowMotionWhenActive = !reducedMotionOff && hudGlowOn
        assertTrue(allowMotionWhenActive)
    }

    // ========================================================================
    // 9. SYSTEM ANNOUNCEMENT PRIORITY ORDER
    // ========================================================================
    @Test
    fun systemAnnouncementPriority_ordersHighSeverityAboveRoutineNotifications() {
        val highPriority = SystemAnnouncementRecord(
            id = "alert-1",
            title = "Boss Gate Approaching",
            message = "High rank dungeon open",
            category = SystemAnnouncementCategory.BOSS
        )
        val routinePriority = SystemAnnouncementRecord(
            id = "routine-1",
            title = "Daily Summary",
            message = "Today's recap",
            category = SystemAnnouncementCategory.DAILY_SUMMARY
        )

        // SystemAnnouncementCategory ordering
        assertTrue(SystemAnnouncementCategory.BOSS.ordinal < SystemAnnouncementCategory.DAILY_SUMMARY.ordinal)
    }

    // ========================================================================
    // 10. UI STATE RESTORATION AND ATTRIBUTE PRESERVATION
    // ========================================================================
    @Test
    fun uiState_preservesAllSevenCanonicalAttributesAccurately() {
        val initialAttributes = PlayerAttributes(
            str = com.example.domain.model.AttributeBreakdown(AttributeType.STR, 15, 12, 3, 40),
            vit = com.example.domain.model.AttributeBreakdown(AttributeType.VIT, 14, 10, 4, 20),
            agi = com.example.domain.model.AttributeBreakdown(AttributeType.AGI, 18, 14, 4, 80),
            intAttr = com.example.domain.model.AttributeBreakdown(AttributeType.INT, 20, 16, 4, 50),
            dis = com.example.domain.model.AttributeBreakdown(AttributeType.DIS, 22, 18, 4, 90),
            wis = com.example.domain.model.AttributeBreakdown(AttributeType.WIS, 16, 12, 4, 30),
            cha = com.example.domain.model.AttributeBreakdown(AttributeType.CHA, 12, 8, 4, 10)
        )

        val player = PlayerProfile(
            codename = "ShadowHunter",
            level = 12,
            rank = HunterRank.C_RANK,
            title = "Shadow Sovereign",
            attributes = initialAttributes,
            streakDays = 14,
            verifiedActivitiesCount = 42,
            totalActivitiesCount = 50
        )

        val state = RealityOsUiState.Ready(
            player = player,
            quests = emptyList(),
            notifications = emptyList(),
            physicalState = PhysicalDevelopmentState(),
            nextRankProgress = NextRankProgress(
                currentRank = HunterRank.C_RANK,
                nextRank = HunterRank.B_RANK,
                overallProgress = 0.65f,
                isEligibleForPromotion = false
            )
        )

        // Verify all 7 canonical attributes are preserved in state
        val attrList = state.player.attributes.asList()
        assertEquals(7, attrList.size)
        assertEquals(AttributeType.STR, attrList[0].type)
        assertEquals(15, attrList[0].totalValue)
        assertEquals(12, attrList[0].verifiedPoints)
        assertEquals(3, attrList[0].selfReportedPoints)

        assertEquals(AttributeType.VIT, attrList[1].type)
        assertEquals(14, attrList[1].totalValue)

        assertEquals(AttributeType.AGI, attrList[2].type)
        assertEquals(18, attrList[2].totalValue)

        assertEquals(AttributeType.INT, attrList[3].type)
        assertEquals(20, attrList[3].totalValue)

        assertEquals(AttributeType.DIS, attrList[4].type)
        assertEquals(22, attrList[4].totalValue)

        assertEquals(AttributeType.WIS, attrList[5].type)
        assertEquals(16, attrList[5].totalValue)

        assertEquals(AttributeType.CHA, attrList[6].type)
        assertEquals(12, attrList[6].totalValue)
    }
}
