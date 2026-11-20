package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.AchievementDefinitionEntity
import com.example.data.local.entity.AchievementUnlockRecordEntity
import com.example.data.local.entity.BossBattleInstanceEntity
import com.example.data.local.entity.BossDefinitionEntity
import com.example.data.local.entity.DungeonDefinitionEntity
import com.example.data.local.entity.DungeonInstanceEntity
import com.example.data.repository.ShadowArmyRepository
import com.example.domain.engine.MonarchProgressionEngine
import com.example.domain.engine.ShadowDeploymentEngine
import com.example.domain.engine.ShadowEvolutionEngine
import com.example.domain.engine.ShadowExtractionEngine
import com.example.domain.engine.ShadowExtractionEvidenceProvider
import com.example.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShadowPhase62RecruitmentRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: SoloLevelingDatabase
    private val ownerAlpha = "supabase_user_alpha"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun evidenceProvider_detectsEligibleAndClaimedSources() = runTest {
        val now = System.currentTimeMillis()
        db.dungeonDao().insertDefinition(
            DungeonDefinitionEntity(
                id = 101L,
                code = "DNG_101",
                title = "Gate 101",
                description = "Test",
                categoryName = "PHYSICAL",
                difficultyName = "C_RANK",
                requiredRankName = "E_RANK",
                requiredLevel = 1,
                requiredAttributeCode = null,
                requiredAttributeValue = 0,
                timeLimitHours = 24,
                baseXpReward = 100,
                primaryAttrName = "STR",
                primaryAttrXp = 10,
                secondaryAttrName = "VIT",
                secondaryAttrXp = 5,
                rewardTitleId = null,
                rewardItemId = null,
                rewardShadowId = null,
                isRepeatable = true,
                creationSourceName = "TEST",
                isActiveCatalog = true
            )
        )
        db.dungeonDao().insertInstance(
            DungeonInstanceEntity(
                id = 1L,
                dungeonId = 101L,
                statusName = "COMPLETED",
                creationSourceName = "TEST",
                cadenceKey = "DAILY",
                startedAtEpochMs = now - 3600_000,
                pausedAtEpochMs = null,
                accumulatedPausedMs = 0L,
                deadlineEpochMs = now + 3600_000,
                completedAtEpochMs = now,
                notes = "Completed dungeon"
            )
        )
        db.bossBattleDao().insertBossDefinition(
            BossDefinitionEntity(
                id = 501L,
                code = "BOSS_501",
                title = "Blood-Red Commander Igris",
                subtitle = "Knight of the Crimson Throne",
                description = "Test boss definition",
                categoryName = "PHYSICAL",
                difficultyName = "B_RANK",
                requiredRankName = "E_RANK",
                requiredLevel = 1,
                xpReward = 250,
                primaryAttrName = "STR",
                primaryAttrXp = 25,
                secondaryAttrName = "VIT",
                secondaryAttrXp = 15,
                rewardTitleId = null,
                rewardItemId = null,
                rewardShadowId = null,
                totalEstimatedDays = 7
            )
        )
        db.bossBattleDao().insertBossInstance(
            BossBattleInstanceEntity(
                id = 1L,
                bossId = 501L,
                statusName = "DEFEATED",
                startedAtEpochMs = now - 7200_000,
                completedAtEpochMs = now - 1800_000,
                abandonedAtEpochMs = null
            )
        )
        db.achievementTitleDao().insertAchievementDefinitions(
            listOf(
                AchievementDefinitionEntity(
                    id = "ach_first_win",
                    title = "First Victory",
                    description = "Complete your first verified milestone",
                    categoryName = "COMBAT",
                    rarityName = "RARE",
                    criterionDescription = "Complete 1 dungeon or boss encounter",
                    targetValue = 1,
                    xpBonus = 100,
                    rewardTitleId = null,
                    rewardItemId = null,
                    rewardShadowId = null
                )
            )
        )
        db.achievementTitleDao().insertAchievementUnlock(
            AchievementUnlockRecordEntity(
                id = 1L,
                achievementId = "ach_first_win",
                evidenceReference = "First win milestone",
                unlockedAtEpochMs = now
            )
        )

        val provider = ShadowExtractionEvidenceProvider(db)
        val items = provider.getEligibleEvidenceItems(ownerAlpha)

        assertTrue("Should find eligible evidence items", items.isNotEmpty())
        assertEquals(3, items.size)
        assertTrue(items.all { it.eligibility == ShadowEvidenceEligibility.ELIGIBLE })
    }

    @Test
    fun extractionAndDuplicateResonance_awardsResonanceInsteadOfDuplicateShadow() = runTest {
        val now = System.currentTimeMillis()
        val monarchEngine = MonarchProgressionEngine(
            monarchProfileDao = db.shadowMonarchProfileDao(),
            monarchSkillUnlockDao = db.monarchSkillUnlockDao(),
            shadowDao = db.shadowDao(),
            shadowResourceDao = db.shadowResourceDao(),
            shadowQuestProgressDao = db.shadowQuestProgressDao(),
            shadowXpTraceDao = db.shadowXpTraceDao(),
            database = db
        )
        val extractionEngine = ShadowExtractionEngine(
            database = db,
            shadowDao = db.shadowDao(),
            extractionDao = db.shadowExtractionDao(),
            monarchProfileDao = db.shadowMonarchProfileDao(),
            monarchProgressionEngine = monarchEngine
        )
        val evolutionEngine = ShadowEvolutionEngine(
            shadowDao = db.shadowDao(),
            shadowSkillUnlockDao = db.shadowSkillUnlockDao(),
            monarchProgressionEngine = monarchEngine,
            shadowXpTraceDao = db.shadowXpTraceDao(),
            database = db
        )
        val deploymentEngine = ShadowDeploymentEngine(
            shadowDao = db.shadowDao(),
            deploymentDao = db.shadowDeploymentDao(),
            formationDao = db.shadowFormationDao(),
            monarchProgressionEngine = monarchEngine,
            database = db
        )
        val repo = ShadowArmyRepository(
            monarchProfileDao = db.shadowMonarchProfileDao(),
            shadowDao = db.shadowDao(),
            extractionDao = db.shadowExtractionDao(),
            skillUnlockDao = db.shadowSkillUnlockDao(),
            deploymentDao = db.shadowDeploymentDao(),
            formationDao = db.shadowFormationDao(),
            monarchSkillUnlockDao = db.monarchSkillUnlockDao(),
            questProgressDao = db.shadowQuestProgressDao(),
            resourceDao = db.shadowResourceDao(),
            monarchProgressionEngine = monarchEngine,
            extractionEngine = extractionEngine,
            evolutionEngine = evolutionEngine,
            deploymentEngine = deploymentEngine,
            database = db
        )

        db.dungeonDao().insertDefinition(
            DungeonDefinitionEntity(
                id = 102L,
                code = "DNG_102",
                title = "Gate 102",
                description = "Test",
                categoryName = "PHYSICAL",
                difficultyName = "C_RANK",
                requiredRankName = "E_RANK",
                requiredLevel = 1,
                requiredAttributeCode = null,
                requiredAttributeValue = 0,
                timeLimitHours = 24,
                baseXpReward = 100,
                primaryAttrName = "STR",
                primaryAttrXp = 10,
                secondaryAttrName = "VIT",
                secondaryAttrXp = 5,
                rewardTitleId = null,
                rewardItemId = null,
                rewardShadowId = null,
                isRepeatable = true,
                creationSourceName = "TEST",
                isActiveCatalog = true
            )
        )
        db.dungeonDao().insertDefinition(
            DungeonDefinitionEntity(
                id = 103L,
                code = "DNG_103",
                title = "Gate 103",
                description = "Test",
                categoryName = "PHYSICAL",
                difficultyName = "C_RANK",
                requiredRankName = "E_RANK",
                requiredLevel = 1,
                requiredAttributeCode = null,
                requiredAttributeValue = 0,
                timeLimitHours = 24,
                baseXpReward = 100,
                primaryAttrName = "STR",
                primaryAttrXp = 10,
                secondaryAttrName = "VIT",
                secondaryAttrXp = 5,
                rewardTitleId = null,
                rewardItemId = null,
                rewardShadowId = null,
                isRepeatable = true,
                creationSourceName = "TEST",
                isActiveCatalog = true
            )
        )
        db.dungeonDao().insertInstance(
            DungeonInstanceEntity(
                id = 10L,
                dungeonId = 102L,
                statusName = "COMPLETED",
                creationSourceName = "TEST",
                cadenceKey = "DAILY",
                startedAtEpochMs = now,
                pausedAtEpochMs = null,
                accumulatedPausedMs = 0L,
                deadlineEpochMs = now + 3600_000,
                completedAtEpochMs = now,
                notes = "Test dungeon"
            )
        )

        val pending1 = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "10",
            evidenceSummary = "Completed Dungeon 102",
            accountOwnerId = ownerAlpha
        )

        val confirmed1 = extractionEngine.confirmExtraction(pending1, ownerAlpha).getOrThrow()
        assertEquals(ShadowExtractionOutcome.SUCCESS, confirmed1.outcome)
        val initialShadowId = confirmed1.extractedShadowId
        assertNotNull(initialShadowId)

        val shadowsAfterFirst = db.shadowDao().getAllShadowsForOwnerOnce(ownerAlpha)
        assertEquals(1, shadowsAfterFirst.size)

        db.dungeonDao().insertInstance(
            DungeonInstanceEntity(
                id = 11L,
                dungeonId = 103L,
                statusName = "COMPLETED",
                creationSourceName = "TEST",
                cadenceKey = "DAILY",
                startedAtEpochMs = now,
                pausedAtEpochMs = null,
                accumulatedPausedMs = 0L,
                deadlineEpochMs = now + 3600_000,
                completedAtEpochMs = now,
                notes = "Test dungeon 2"
            )
        )

        val pending2 = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "11",
            evidenceSummary = "Completed Dungeon 103",
            accountOwnerId = ownerAlpha
        )

        val forcedPending = pending2.copy(shadowName = shadowsAfterFirst.first().name)
        val confirmed2 = extractionEngine.confirmExtraction(forcedPending, ownerAlpha).getOrThrow()

        assertEquals(ShadowExtractionOutcome.DUPLICATE_RESOLVED, confirmed2.outcome)

        val shadowsAfterSecond = db.shadowDao().getAllShadowsForOwnerOnce(ownerAlpha)
        assertEquals(1, shadowsAfterSecond.size)

        val resonanceRecords = db.shadowResonanceDao().getResonanceRecordsForOwnerOnce(ownerAlpha)
        assertTrue("Should have a resonance record", resonanceRecords.isNotEmpty())
        assertEquals(25, resonanceRecords.first().resonanceShards)
    }
}
