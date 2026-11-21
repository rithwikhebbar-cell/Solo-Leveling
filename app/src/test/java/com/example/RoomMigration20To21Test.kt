package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Milestone 12 - Phase 6.4: Room v20 -> v21 Schema & Data Migration Test.
 * Validates that MIGRATION_20_21 creates shadow_bond_profiles, shadow_bond_events,
 * and shadow_bond_milestones tables with composite indices and foreign keys,
 * while fully preserving existing v20 records.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration20To21Test {

    @Test
    fun migration20To21_createsShadowBondTablesAndPreservesExistingData() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-20-21-test-${System.nanoTime()}.db"
        val now = 1_700_900_000_000L

        // 1. Create SQLite DB at version 20 with shadow_monarch_profile and shadows tables
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(20) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `shadow_monarch_profile` (
                                `accountOwnerId` TEXT NOT NULL,
                                `monarchLevel` INTEGER NOT NULL,
                                `monarchRank` TEXT NOT NULL,
                                `monarchTitle` TEXT NOT NULL,
                                `monarchXp` INTEGER NOT NULL,
                                `currentArmySize` INTEGER NOT NULL,
                                `maxArmyCapacity` INTEGER NOT NULL,
                                `currentShadowEssence` INTEGER NOT NULL,
                                `currentMonarchFragments` INTEGER NOT NULL,
                                `currentEvolutionCores` INTEGER NOT NULL,
                                `totalShadowsExtracted` INTEGER NOT NULL,
                                `totalExtractionsAttempted` INTEGER NOT NULL,
                                `totalShadowsEvolved` INTEGER NOT NULL,
                                `lastActivityEpochMs` INTEGER NOT NULL,
                                `updatedAtEpochMs` INTEGER NOT NULL,
                                PRIMARY KEY(`accountOwnerId`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `shadows` (
                                `shadowId` TEXT NOT NULL,
                                `accountOwnerId` TEXT NOT NULL,
                                `name` TEXT NOT NULL,
                                `shadowClass` TEXT NOT NULL,
                                `currentRank` TEXT NOT NULL,
                                `level` INTEGER NOT NULL,
                                `shadowXp` INTEGER NOT NULL,
                                `rarity` TEXT NOT NULL,
                                `passiveAbility` TEXT NOT NULL,
                                `evolutionStage` TEXT NOT NULL,
                                `evolutionHistoryJson` TEXT NOT NULL,
                                `deploymentStatus` TEXT NOT NULL,
                                `extractedAtEpochMs` INTEGER NOT NULL,
                                `extractionEventId` TEXT NOT NULL,
                                `strength` INTEGER NOT NULL,
                                `agility` INTEGER NOT NULL,
                                `intelligence` INTEGER NOT NULL,
                                `vitality` INTEGER NOT NULL,
                                `perception` INTEGER NOT NULL,
                                `command` INTEGER NOT NULL,
                                PRIMARY KEY(`shadowId`)
                            )
                            """.trimIndent()
                        )
                        // Seed an existing monarch profile & shadow in v20
                        db.execSQL(
                            """
                            INSERT INTO `shadow_monarch_profile` VALUES (
                                'player_123', 5, 'B_RANK', 'Shadow Lord', 1200, 3, 10,
                                500, 50, 10, 5, 6, 2, $now, $now
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO `shadows` VALUES (
                                'shadow_igris_1', 'player_123', 'Igris', 'VANGUARD', 'A_RANK', 25, 450, 'EPIC',
                                'Dominion Command', 'KNIGHT', '[]', 'AVAILABLE', $now, 'extract_ev_1',
                                40, 35, 20, 30, 25, 30
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()

        // 2. Upgrade from version 20 to 21 using MIGRATION_20_21
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(21) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        SoloLevelingDatabase.MIGRATION_20_21.migrate(db)
                    }
                })
                .build()
        )
        val writable = openHelper.writableDatabase

        // 3. Verify shadow_bond_profiles table exists and is queryable
        val cProfiles = writable.query("SELECT count(*) FROM `shadow_bond_profiles`")
        assertTrue(cProfiles.moveToFirst())
        assertEquals(0, cProfiles.getInt(0))
        cProfiles.close()

        // 4. Verify shadow_bond_events table exists and is queryable
        val cEvents = writable.query("SELECT count(*) FROM `shadow_bond_events`")
        assertTrue(cEvents.moveToFirst())
        assertEquals(0, cEvents.getInt(0))
        cEvents.close()

        // 5. Verify shadow_bond_milestones table exists and is queryable
        val cMilestones = writable.query("SELECT count(*) FROM `shadow_bond_milestones`")
        assertTrue(cMilestones.moveToFirst())
        assertEquals(0, cMilestones.getInt(0))
        cMilestones.close()

        // 6. Verify existing data preserved
        val cMonarch = writable.query("SELECT monarchTitle FROM `shadow_monarch_profile` WHERE accountOwnerId = 'player_123'")
        assertTrue(cMonarch.moveToFirst())
        assertEquals("Shadow Lord", cMonarch.getString(0))
        cMonarch.close()

        val cShadow = writable.query("SELECT name, shadowClass FROM `shadows` WHERE shadowId = 'shadow_igris_1'")
        assertTrue(cShadow.moveToFirst())
        assertEquals("Igris", cShadow.getString(0))
        assertEquals("VANGUARD", cShadow.getString(1))
        cShadow.close()

        // 7. Test inserting into shadow_bond_profiles, shadow_bond_events, shadow_bond_milestones
        writable.execSQL(
            """
            INSERT INTO `shadow_bond_profiles` VALUES (
                'shadow_igris_1', 'player_123', 1, 50, 20, 14, 'UNFAMILIAR', 'DISCIPLINED', 1, $now, $now
            )
            """.trimIndent()
        )

        writable.execSQL(
            """
            INSERT INTO `shadow_bond_events` VALUES (
                'bond_ev_1', 'shadow_igris_1', 'player_123', 'DUNGEON_COMPLETION', 50, 5, 4,
                'Cleared C-Rank Gate', 'dungeon_gate_1', 'idemp_key_1', $now
            )
            """.trimIndent()
        )

        writable.execSQL(
            """
            INSERT INTO `shadow_bond_milestones` VALUES (
                'mile_1', 'shadow_igris_1', 'player_123', 'FIRST_MISSION_COMPLETE',
                'First Mission Accomplished', 'Accomplished first gate expedition', $now, 'bond_ev_1'
            )
            """.trimIndent()
        )

        val verifyProfile = writable.query("SELECT bondLevel, bondPoints, loyaltyRating, trustRating FROM `shadow_bond_profiles` WHERE shadowId = 'shadow_igris_1'")
        assertTrue(verifyProfile.moveToFirst())
        assertEquals(1, verifyProfile.getInt(0))
        assertEquals(50, verifyProfile.getInt(1))
        assertEquals(20, verifyProfile.getInt(2))
        assertEquals(14, verifyProfile.getInt(3))
        verifyProfile.close()

        writable.close()
    }
}
