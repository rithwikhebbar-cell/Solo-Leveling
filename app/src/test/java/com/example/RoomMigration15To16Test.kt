package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.ShadowResourceType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Milestone 12 - Phase 6.1: Full Room v15 -> v16 Schema & Data Migration Test.
 * Builds a real v15 database by running v1 -> v15 migrations, seeds Milestone 11 Shadow data
 * (including legacy _monarch_xp_trace_ rows and an orphan deployment row), then opens Room at v16
 * using MIGRATION_15_16 so Room's OpenHelper validates every table, index, and foreign key schema.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration15To16Test {

    @Test
    fun migration15To16_validatesFullSchemaAndIsolatesLegacyDataUnderLocalVault() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-15-16-test-${System.nanoTime()}.db"
        val now = 1_700_800_000_000L

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(15) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Create minimal v1 tables then apply migrations 1..15 to obtain exact v15 schema
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `player_profile` (
                                `id` INTEGER NOT NULL,
                                `codename` TEXT NOT NULL,
                                `title` TEXT NOT NULL,
                                `level` INTEGER NOT NULL,
                                `currentLevelXp` INTEGER NOT NULL,
                                `totalXp` INTEGER NOT NULL,
                                `rankName` TEXT NOT NULL,
                                `strVerified` INTEGER NOT NULL,
                                `strSelfReported` INTEGER NOT NULL,
                                `strProgressXp` INTEGER NOT NULL,
                                `vitVerified` INTEGER NOT NULL,
                                `vitSelfReported` INTEGER NOT NULL,
                                `vitProgressXp` INTEGER NOT NULL,
                                `agiVerified` INTEGER NOT NULL,
                                `agiSelfReported` INTEGER NOT NULL,
                                `agiProgressXp` INTEGER NOT NULL,
                                `intVerified` INTEGER NOT NULL,
                                `intSelfReported` INTEGER NOT NULL,
                                `intProgressXp` INTEGER NOT NULL,
                                `disVerified` INTEGER NOT NULL,
                                `disSelfReported` INTEGER NOT NULL,
                                `disProgressXp` INTEGER NOT NULL,
                                `wisVerified` INTEGER NOT NULL,
                                `wisSelfReported` INTEGER NOT NULL,
                                `wisProgressXp` INTEGER NOT NULL,
                                `chaVerified` INTEGER NOT NULL,
                                `chaSelfReported` INTEGER NOT NULL,
                                `chaProgressXp` INTEGER NOT NULL,
                                `streakDays` INTEGER NOT NULL,
                                `verifiedActivitiesCount` INTEGER NOT NULL,
                                `selfReportedActivitiesCount` INTEGER NOT NULL,
                                `recoveryScore` INTEGER NOT NULL,
                                `createdAtEpochMs` INTEGER NOT NULL,
                                PRIMARY KEY(`id`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `activity_logs` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `title` TEXT NOT NULL,
                                `categoryName` TEXT NOT NULL,
                                `durationMinutes` INTEGER NOT NULL,
                                `verificationStatusName` TEXT NOT NULL,
                                `xpAwarded` INTEGER NOT NULL,
                                `primaryAttrName` TEXT NOT NULL,
                                `primaryAttrXpAwarded` INTEGER NOT NULL,
                                `notes` TEXT NOT NULL,
                                `timestampEpochMs` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `physical_state` (
                                `id` INTEGER NOT NULL,
                                `baseJoggingTargetMinutes` INTEGER NOT NULL,
                                `aiRecommendedJoggingMinutes` INTEGER NOT NULL,
                                `completedJoggingMinutesToday` INTEGER NOT NULL,
                                `workoutStatusName` TEXT NOT NULL,
                                `recoveryStatusName` TEXT NOT NULL,
                                `weeklySessionsCompleted` INTEGER NOT NULL,
                                `lastSessionTimestampMs` INTEGER,
                                `aiTrainingNote` TEXT NOT NULL,
                                PRIMARY KEY(`id`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `system_notifications` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `title` TEXT NOT NULL,
                                `message` TEXT NOT NULL,
                                `categoryName` TEXT NOT NULL,
                                `isRead` INTEGER NOT NULL,
                                `timestampEpochMs` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `quests` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `title` TEXT NOT NULL,
                                `description` TEXT NOT NULL,
                                `typeName` TEXT NOT NULL,
                                `categoryName` TEXT NOT NULL,
                                `targetValue` INTEGER NOT NULL,
                                `currentValue` INTEGER NOT NULL,
                                `unit` TEXT NOT NULL,
                                `xpReward` INTEGER NOT NULL,
                                `primaryAttrName` TEXT NOT NULL,
                                `primaryAttrXpReward` INTEGER NOT NULL,
                                `secondaryAttrName` TEXT,
                                `secondaryAttrXpReward` INTEGER NOT NULL,
                                `isCompleted` INTEGER NOT NULL,
                                `verificationStatusName` TEXT NOT NULL,
                                `createdAtEpochMs` INTEGER NOT NULL,
                                `completedAtEpochMs` INTEGER
                            )
                            """.trimIndent()
                        )

                        listOf(
                            SoloLevelingDatabase.MIGRATION_1_2,
                            SoloLevelingDatabase.MIGRATION_2_3,
                            SoloLevelingDatabase.MIGRATION_3_4,
                            SoloLevelingDatabase.MIGRATION_4_5,
                            SoloLevelingDatabase.MIGRATION_5_6,
                            SoloLevelingDatabase.MIGRATION_6_7,
                            SoloLevelingDatabase.MIGRATION_7_8,
                            SoloLevelingDatabase.MIGRATION_8_9,
                            SoloLevelingDatabase.MIGRATION_9_10,
                            SoloLevelingDatabase.MIGRATION_10_11,
                            SoloLevelingDatabase.MIGRATION_11_12,
                            SoloLevelingDatabase.MIGRATION_12_13,
                            SoloLevelingDatabase.MIGRATION_13_14,
                            SoloLevelingDatabase.MIGRATION_14_15
                        ).forEach { it.migrate(db) }

                        // Seed rich Milestone 11 v15 Shadow data
                        db.execSQL(
                            """
                            INSERT OR REPLACE INTO `shadow_monarch_profile` VALUES (
                                1, 4, 850, 'SHADOW_ADEPT', 9, 10, 1, 1, 'D_RANK', 1, 1400,
                                '["monarch_shadow_extraction"]', $now
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO `shadows` VALUES (
                                'legacy_shadow_igris', 'Igris', 'VANGUARD', 'EPIC', 'D_RANK', 6, 620,
                                20, 18, 16, 15, 14, 15, 'AWAKENED', 'Battle Fervor',
                                '["shadow_tactical_insight"]', 'Boss Battle #1', 'ext_legacy_1',
                                $now, '["AWAKENED:$now"]', 'DEPLOYED', 'legacy_form_1', 0, ''
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO `shadow_extractions` VALUES (
                                'ext_legacy_1', 'BOSS_BATTLE_COMPLETION', 'boss_1', 'SUCCESS',
                                'legacy_shadow_igris', 'Igris', 'VANGUARD', 'EPIC', 12345,
                                'Defeated Blood-Red Commander', $now, 1, 100
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO `shadow_skill_unlocks` (`shadowId`, `skillId`, `unlockedAtEpochMs`, `evidenceNote`)
                            VALUES ('legacy_shadow_igris', 'shadow_tactical_insight', $now, 'Legacy skill')
                            """.trimIndent()
                        )
                        // Orphan skill unlock referencing non-existent shadow -> must be safely filtered
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO `shadow_skill_unlocks` (`shadowId`, `skillId`, `unlockedAtEpochMs`, `evidenceNote`)
                            VALUES ('orphan_shadow_999', 'shadow_tactical_insight', $now, 'Orphan skill')
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO `shadow_deployments` VALUES (
                                'dep_legacy_1', 'legacy_shadow_igris', 'Igris', 'PHYSICAL_DEVELOPMENT',
                                'Vanguard Lead', $now, NULL, 'DEPLOYED', ''
                            )
                            """.trimIndent()
                        )
                        // Orphan deployment referencing non-existent shadow -> must be safely filtered
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO `shadow_deployments` VALUES (
                                'dep_orphan_999', 'orphan_shadow_999', 'Ghost', 'DISCIPLINE',
                                'Orphan', $now, NULL, 'DEPLOYED', ''
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO `shadow_formations` VALUES (
                                'legacy_form_1', 'Monarch Vanguard', 'STRIKE_FORMATION', 'Primary assault',
                                5, '["legacy_shadow_igris"]', 1, $now, $now
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO `monarch_skill_unlocks` (`skillId`, `unlockedAtEpochMs`, `evidenceNote`)
                            VALUES ('monarch_shadow_extraction', $now, 'Initial skill')
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO `shadow_quest_progress` (`questId`, `currentValue`, `isCompleted`, `rewardClaimedAtEpochMs`, `completedAtEpochMs`)
                            VALUES ('sq_first_extraction', 1, 1, NULL, $now)
                            """.trimIndent()
                        )
                        // Genuine resource ledger row + legacy '_monarch_xp_trace_' row
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO `shadow_resource_transactions` VALUES (
                                'tx_res_1', 'SHADOW_ESSENCE', 75, 'EARNED', 'legacy_ext_1', 'Extracted Igris', 75, $now
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO `shadow_resource_transactions` VALUES (
                                'tx_trace_1', '_monarch_xp_trace_', 100, 'EARNED', 'monarch_xp_extraction_legacy_1',
                                'Shadow extracted: Igris', 0, $now
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()

        // Open through Room v16 with MIGRATION_15_16 -> validates exact Room schema!
        val migratedDb = Room.databaseBuilder(context, SoloLevelingDatabase::class.java, dbName)
            .addMigrations(
                SoloLevelingDatabase.MIGRATION_15_16,
                SoloLevelingDatabase.MIGRATION_16_17,
                SoloLevelingDatabase.MIGRATION_17_18
            )
            .allowMainThreadQueries()
            .build()

        // 1. Legacy profile & child records preserved strictly under DEFAULT_SHADOW_ACCOUNT_OWNER_ID
        val legacyProfile = migratedDb.shadowMonarchProfileDao()
            .getProfileForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)
        assertNotNull(legacyProfile)
        assertEquals(4, legacyProfile!!.monarchLevel)
        assertEquals(850L, legacyProfile.monarchXp)

        val legacyShadows = migratedDb.shadowDao().getAllShadowsForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)
        assertEquals(1, legacyShadows.size)
        assertEquals("Igris", legacyShadows.first().name)

        // 2. Orphan child records excluded cleanly without FK violation
        val skillUnlocks = migratedDb.shadowSkillUnlockDao().getAllUnlocksForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)
        assertEquals(1, skillUnlocks.size)
        val deployments = migratedDb.shadowDeploymentDao().getAllDeploymentsForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)
        assertEquals(1, deployments.size)
        assertEquals("dep_legacy_1", deployments.first().deploymentId)

        // 3. Legacy '_monarch_xp_trace_' rows moved into shadow_xp_traces and stripped from resource ledger
        val legacyLedger = migratedDb.shadowResourceDao().getAllTransactionsForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)
        assertEquals(1, legacyLedger.size)
        assertEquals(ShadowResourceType.SHADOW_ESSENCE.name, legacyLedger.first().resourceTypeName)
        assertEquals(75, migratedDb.shadowResourceDao().getCurrentBalanceForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID, ShadowResourceType.SHADOW_ESSENCE.name))

        val xpTraces = migratedDb.shadowXpTraceDao().getTracesForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)
        assertEquals(1, xpTraces.size)
        assertEquals("MONARCH_XP", xpTraces.first().traceType)
        assertEquals("extraction_legacy_1", xpTraces.first().sourceTag)
        assertEquals(100, xpTraces.first().xpAmount)

        // 4. Signed-in accounts start completely isolated from migrated local vault rows
        assertNull(migratedDb.shadowMonarchProfileDao().getProfileForOwnerOnce("supabase_user_alpha"))
        assertTrue(migratedDb.shadowDao().getAllShadowsForOwnerOnce("supabase_user_alpha").isEmpty())

        migratedDb.close()
        context.deleteDatabase(dbName)
    }
}
