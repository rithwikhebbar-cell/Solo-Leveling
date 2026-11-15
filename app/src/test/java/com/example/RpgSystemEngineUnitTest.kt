package com.example

import com.example.domain.engine.AchievementEngine
import com.example.domain.engine.BossBattleEngine
import com.example.domain.engine.DungeonEngine
import com.example.domain.engine.PlayerTelemetrySnapshot
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.SkillCatalog
import com.example.domain.engine.SkillEffectEngine
import com.example.domain.engine.SkillEngine
import com.example.domain.model.AcademicActivityKind
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.AttributeType
import com.example.domain.model.BossBattleInstance
import com.example.domain.model.BossStatus
import com.example.domain.model.DungeonCategory
import com.example.domain.model.DungeonInstance
import com.example.domain.model.DungeonStatus
import com.example.domain.model.HunterRank
import com.example.domain.model.JoggingSession
import com.example.domain.model.PlayerAttributes
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RecoveryState
import com.example.domain.model.SkillUnlockRecord
import com.example.domain.model.StudySession
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RpgSystemEngineUnitTest {

    private fun baseProfile(
        level: Int = 1,
        rank: HunterRank = HunterRank.E_RANK,
        str: Int = 10,
        vit: Int = 10,
        intel: Int = 10,
        dis: Int = 10,
        wis: Int = 10,
        verifiedCount: Int = 0,
        streakDays: Int = 0
    ): PlayerProfile = PlayerProfile(
        codename = "TestHunter",
        level = level,
        rank = rank,
        verifiedActivitiesCount = verifiedCount,
        streakDays = streakDays,
        attributes = PlayerAttributes(
            str = AttributeBreakdown(AttributeType.STR, baseValue = str),
            vit = AttributeBreakdown(AttributeType.VIT, baseValue = vit),
            intel = AttributeBreakdown(AttributeType.INT, baseValue = intel),
            dis = AttributeBreakdown(AttributeType.DIS, baseValue = dis),
            wis = AttributeBreakdown(AttributeType.WIS, baseValue = wis)
        )
    )

    @Test
    fun dungeonEligibility_blocksHigherRankAndMissingAttributes() {
        val dRankGate = DungeonEngine.defaultCatalog()
            .first { it.definition.code == "sys_d_verified_gauntlet" }
        val starter = baseProfile(level = 2, rank = HunterRank.E_RANK, vit = 10)
        val (eligibleStarter, blockersStarter) = DungeonEngine.checkEligibility(dRankGate.definition, starter)
        assertFalse(eligibleStarter)
        assertTrue(blockersStarter.any { it.contains("D-Rank") })
        assertTrue(blockersStarter.any { it.contains("VIT") })

        val qualified = baseProfile(level = 6, rank = HunterRank.D_RANK, vit = 14)
        val (eligibleQualified, blockersQualified) = DungeonEngine.checkEligibility(dRankGate.definition, qualified)
        assertTrue(eligibleQualified)
        assertTrue(blockersQualified.isEmpty())
    }

    @Test
    fun dungeonCompletionValidator_ignoresPreEntryLogs_andValidatesRealRecordsAfterEntry() {
        val gate = DungeonEngine.defaultCatalog().first { it.definition.code == "sys_e_first_stride" }
        val entryTime = 1_700_000_000_000L
        val instance = DungeonInstance(
            id = 10L,
            dungeonId = 1L,
            status = DungeonStatus.ACTIVE,
            startedAtEpochMs = entryTime,
            deadlineEpochMs = entryTime + 48 * 3_600_000L
        )

        // Pre-entry jog & study must NOT satisfy objectives
        val preEntryTelemetry = PlayerTelemetrySnapshot(
            profile = baseProfile(),
            nowEpochMs = entryTime + 1_000L,
            jogs = listOf(
                JoggingSession(
                    id = 1L,
                    sourceId = "pre_jog",
                    originApp = "HealthConnect",
                    title = "Pre-entry Run",
                    startEpochMs = entryTime - 40 * 60_000L,
                    endEpochMs = entryTime - 10 * 60_000L,
                    durationMinutes = 30,
                    distanceMeters = 4000.0,
                    verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
                    xpAwarded = 45
                )
            ),
            studySessions = listOf(
                StudySession(
                    id = 1L,
                    sourceId = "pre_study",
                    subjectId = 1L,
                    title = "Pre-entry Study",
                    activityKind = AcademicActivityKind.FOCUS_SESSION,
                    startEpochMs = entryTime - 35 * 60_000L,
                    endEpochMs = entryTime - 5 * 60_000L,
                    durationMinutes = 30,
                    verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
                    xpAwarded = 30
                )
            )
        )
        val preStatus = DungeonEngine.evaluateDungeonWithStatus(
            definition = gate.definition,
            objectives = gate.objectives,
            latestInstance = instance,
            completedCount = 0,
            telemetry = preEntryTelemetry
        )
        assertEquals(0f, preStatus.overallProgressFraction, 0.001f)
        val preValidation = DungeonEngine.validateCompletion(preStatus, instance, preEntryTelemetry)
        assertFalse(preValidation.canComplete)
        assertTrue(preValidation.failureReason!!.contains("incomplete", ignoreCase = true))

        // Post-entry real logs satisfy all objectives
        val postEntryTelemetry = preEntryTelemetry.copy(
            nowEpochMs = entryTime + 90 * 60_000L,
            jogs = preEntryTelemetry.jogs + JoggingSession(
                id = 2L,
                sourceId = "post_jog",
                originApp = "HealthConnect",
                title = "Verified Run",
                startEpochMs = entryTime + 60_000L,
                endEpochMs = entryTime + 35 * 60_000L,
                durationMinutes = 34,
                distanceMeters = 4500.0,
                verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
                xpAwarded = 50
            ),
            studySessions = preEntryTelemetry.studySessions + StudySession(
                id = 2L,
                sourceId = "post_study",
                subjectId = 1L,
                title = "Post-entry Study",
                activityKind = AcademicActivityKind.FOCUS_SESSION,
                startEpochMs = entryTime + 40 * 60_000L,
                endEpochMs = entryTime + 70 * 60_000L,
                durationMinutes = 30,
                verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
                xpAwarded = 30
            )
        )
        val postStatus = DungeonEngine.evaluateDungeonWithStatus(
            definition = gate.definition,
            objectives = gate.objectives,
            latestInstance = instance,
            completedCount = 0,
            telemetry = postEntryTelemetry
        )
        assertEquals(1f, postStatus.overallProgressFraction, 0.001f)
        val postValidation = DungeonEngine.validateCompletion(postStatus, instance, postEntryTelemetry)
        assertTrue(postValidation.canComplete)
        assertTrue(postValidation.hasVerifiedEvidence)

        // Expired gate must fail validation even if objectives met
        val expiredTelemetry = postEntryTelemetry.copy(nowEpochMs = entryTime + 60 * 3_600_000L)
        val expiredValidation = DungeonEngine.validateCompletion(postStatus, instance, expiredTelemetry)
        assertFalse(expiredValidation.canComplete)
        assertTrue(expiredValidation.failureReason!!.contains("expired", ignoreCase = true))
    }

    @Test
    fun adaptiveDailyGeneration_respectsRestRecommendedAndMissedDays() {
        val fatiguedSnapshot = PlayerTelemetrySnapshot(
            profile = baseProfile(level = 4, rank = HunterRank.D_RANK),
            recoveryState = RecoveryState.REST_RECOMMENDED,
            todayEpochDay = 20_000L
        )
        val generated = DungeonEngine.buildAdaptiveDailyAndWeeklyDungeons(
            telemetry = fatiguedSnapshot,
            existingCodes = emptySet()
        )
        val daily = generated.first { it.definition.code == "daily_gate_20000" }
        assertEquals(DungeonCategory.RECOVERY_WELLBEING, daily.definition.category)
        assertFalse(daily.objectives.any { it.evidenceType.name.contains("PHYSICAL") })

        // Missing prior day creates only today's gate cleanly with no failure/penalty gates
        assertEquals(2, generated.size)
    }

    @Test
    fun bossBattleEngine_reducesHealthDeterministicallyFromPhaseProgress() {
        val monolith = BossBattleEngine.defaultBossCatalog()
            .first { it.definition.code == "boss_academic_sentinel" }
        val start = 1_700_000_000_000L
        val activeInstance = BossBattleInstance(
            id = 5L,
            bossId = 1L,
            status = BossStatus.ACTIVE,
            startedAtEpochMs = start
        )
        val fullHp = BossBattleEngine.evaluateBossWithPhases(
            definition = monolith.definition,
            phases = monolith.phases,
            latestInstance = activeInstance,
            telemetry = PlayerTelemetrySnapshot(profile = baseProfile(), nowEpochMs = start + 1_000L)
        )
        assertEquals(100, fullHp.healthRemainingPercent)

        // Completing Phase 1 (90 min study, 25% weight) drops HP deterministically to 75%
        val afterPhase1 = BossBattleEngine.evaluateBossWithPhases(
            definition = monolith.definition,
            phases = monolith.phases,
            latestInstance = activeInstance,
            telemetry = PlayerTelemetrySnapshot(
                profile = baseProfile(),
                nowEpochMs = start + 100 * 60_000L,
                studySessions = listOf(
                    StudySession(
                        id = 1L,
                        sourceId = "boss_study_1",
                        subjectId = 1L,
                        title = "Deep Focus Block",
                        activityKind = AcademicActivityKind.FOCUS_SESSION,
                        startEpochMs = start + 60_000L,
                        endEpochMs = start + 91 * 60_000L,
                        durationMinutes = 90,
                        verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
                        xpAwarded = 80
                    )
                )
            )
        )
        assertEquals(75, afterPhase1.healthRemainingPercent)
        assertTrue(afterPhase1.phaseProgress.first().isCompleted)
    }

    @Test
    fun skillEngineAndEffectEngine_enforcePrerequisitesAndProvideSafePerks() {
        val tier2Skill = SkillCatalog.skills.first { it.id == "acad_retention" }
        val qualifiedProfile = baseProfile(level = 8, rank = HunterRank.D_RANK, intel = 18, wis = 15, verifiedCount = 4)

        // Blocked when prerequisite acad_deep_focus is missing
        val missingPrereq = SkillEngine.evaluateSkillWithStatus(
            definition = tier2Skill,
            dependencies = SkillCatalog.dependencies,
            effects = SkillCatalog.effects,
            unlockedMap = emptyMap(),
            profile = qualifiedProfile
        )
        assertFalse(missingPrereq.canUnlockOrUpgrade)
        assertTrue(missingPrereq.missingRequirements.any { it.contains("Deep Focus") })

        // Allowed once acad_deep_focus Lv.1 is unlocked
        val unlockedMap = mapOf(
            "acad_deep_focus" to SkillUnlockRecord(
                id = 1L,
                skillId = "acad_deep_focus",
                masteryLevel = 1,
                unlockedAtEpochMs = 1L,
                lastUpgradedAtEpochMs = 1L
            )
        )
        val readyTier2 = SkillEngine.evaluateSkillWithStatus(
            definition = tier2Skill,
            dependencies = SkillCatalog.dependencies,
            effects = SkillCatalog.effects,
            unlockedMap = unlockedMap,
            profile = qualifiedProfile
        )
        assertTrue(readyTier2.canUnlockOrUpgrade)

        // SkillEffectEngine reflects real focus presets & safety guarantee
        val effects = SkillEffectEngine.evaluateActiveEffects(unlockedMap.values.toList())
        assertTrue(60 in effects.unlockedFocusPresetsMinutes)
        assertTrue(90 in effects.unlockedFocusPresetsMinutes)
        assertTrue(effects.safetyGuaranteeNote.contains("recovery safety", ignoreCase = true))
    }

    @Test
    fun progressionEngineRpgReward_weightsProvenanceAndCapsDailyXp() {
        val verified = ProgressionEngine.calculateRpgReward(
            baseXp = 100,
            primaryAttribute = AttributeType.VIT,
            primaryAttrXp = 40,
            secondaryAttribute = AttributeType.DIS,
            secondaryAttrXp = 20,
            hasVerifiedEvidence = true,
            rpgXpAlreadyEarnedToday = 0,
            sourceLabel = "Dungeon"
        )
        val manual = ProgressionEngine.calculateRpgReward(
            baseXp = 100,
            primaryAttribute = AttributeType.VIT,
            primaryAttrXp = 40,
            secondaryAttribute = AttributeType.DIS,
            secondaryAttrXp = 20,
            hasVerifiedEvidence = false,
            rpgXpAlreadyEarnedToday = 0,
            sourceLabel = "Dungeon"
        )
        assertEquals(100, verified.xpAwarded)
        assertEquals(75, manual.xpAwarded)
        assertFalse(verified.wasCapped)

        val capped = ProgressionEngine.calculateRpgReward(
            baseXp = 100,
            primaryAttribute = AttributeType.VIT,
            primaryAttrXp = 40,
            secondaryAttribute = AttributeType.DIS,
            secondaryAttrXp = 20,
            hasVerifiedEvidence = true,
            rpgXpAlreadyEarnedToday = ProgressionEngine.DAILY_RPG_XP_CAP - 30,
            sourceLabel = "Dungeon"
        )
        assertEquals(30, capped.xpAwarded)
        assertTrue(capped.wasCapped)
    }

    @Test
    fun achievementEngine_detectsNewlyEligibleMilestones() {
        val telemetry = PlayerTelemetrySnapshot(
            profile = baseProfile(level = 5, rank = HunterRank.D_RANK, verifiedCount = 5, streakDays = 7),
            completedDungeonsCount = 1,
            completedBossesCount = 1,
            unlockedSkillsCount = 5
        )
        val eligible = AchievementEngine.findNewlyEligibleAchievements(telemetry, emptyMap())
        val ids = eligible.map { it.first.id }.toSet()
        assertTrue("ach_dungeon_first" in ids)
        assertTrue("ach_boss_first" in ids)
        assertTrue("ach_rank_d" in ids)
        assertTrue("ach_skills_5" in ids)
        assertTrue("ach_consistency_7" in ids)
    }
}
