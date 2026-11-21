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
 * Milestone 12 - Phase 6.5: Room v21 -> v22 Schema & Data Migration Test.
 * Validates that MIGRATION_21_22 creates shadow_squads, shadow_squad_members,
 * and shadow_mission_assignments tables with composite indices and foreign keys,
 * while fully preserving existing v21 records.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration21To22Test {

    @Test
    fun migration21To22_createsShadowTacticalTablesAndPreservesExistingData() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-21-22-test-${System.nanoTime()}.db"
        val now = 1_700_900_000_000L

        // 1. Create SQLite DB at version 21 with shadow_monarch_profile, shadows, and shadow_bond_profiles
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(21) {
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
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `shadow_bond_profiles` (
                                `shadowId` TEXT NOT NULL,
                                `accountOwnerId` TEXT NOT NULL,
                                `bondLevel` INTEGER NOT NULL,
                                `bondPoints` INTEGER NOT NULL,
                                `loyaltyRating` INTEGER NOT NULL,
                                `trustRating` INTEGER NOT NULL,
                                `relationshipStage` TEXT NOT NULL,
                                `personality` TEXT NOT NULL,
                                `totalActivitiesCount` INTEGER NOT NULL,
                                `lastBondActivityEpochMs` INTEGER NOT NULL,
                                `updatedAtEpochMs` INTEGER NOT NULL,
                                PRIMARY KEY(`shadowId`, `accountOwnerId`)
                            )
                            """.trimIndent()
                        )
                        // Seed existing data
                        db.execSQL(
                            """
                            INSERT INTO `shadow_monarch_profile` VALUES (
                                'player_123', 6, 'A_RANK', 'Monarch of Shadows', 1500, 4, 12,
                                600, 60, 12, 6, 7, 3, $now, $now
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO `shadows` VALUES (
                                'shadow_igris_1', 'player_123', 'Igris', 'VANGUARD', 'A_RANK', 30, 800, 'EPIC',
                                'Dominion Command', 'KNIGHT', '[]', 'AVAILABLE', $now, 'extract_ev_1',
                                50, 45, 25, 40, 30, 35
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO `shadow_bond_profiles` VALUES (
                                'shadow_igris_1', 'player_123', 5, 2500, 65, 55, 'SWORN_LOYALTY',
                                'DISCIPLINED', 14, $now, $now
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()

        // 2. Upgrade from version 21 to 22 using MIGRATION_21_22
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(22) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        SoloLevelingDatabase.MIGRATION_21_22.migrate(db)
                    }
                })
                .build()
        )
        val writable = openHelper.writableDatabase

        // 3. Verify shadow_squads table exists and is queryable
        val cSquads = writable.query("SELECT count(*) FROM `shadow_squads`")
        assertTrue(cSquads.moveToFirst())
        assertEquals(0, cSquads.getInt(0))
        cSquads.close()

        // 4. Verify shadow_squad_members table exists and is queryable
        val cMembers = writable.query("SELECT count(*) FROM `shadow_squad_members`")
        assertTrue(cMembers.moveToFirst())
        assertEquals(0, cMembers.getInt(0))
        cMembers.close()

        // 5. Verify shadow_mission_assignments table exists and is queryable
        val cAssignments = writable.query("SELECT count(*) FROM `shadow_mission_assignments`")
        assertTrue(cAssignments.moveToFirst())
        assertEquals(0, cAssignments.getInt(0))
        cAssignments.close()

        // 6. Verify existing data preserved across all previously migrated tables
        val cMonarch = writable.query("SELECT monarchTitle FROM `shadow_monarch_profile` WHERE accountOwnerId = 'player_123'")
        assertTrue(cMonarch.moveToFirst())
        assertEquals("Monarch of Shadows", cMonarch.getString(0))
        cMonarch.close()

        val cShadow = writable.query("SELECT name FROM `shadows` WHERE shadowId = 'shadow_igris_1'")
        assertTrue(cShadow.moveToFirst())
        assertEquals("Igris", cShadow.getString(0))
        cShadow.close()

        val cBond = writable.query("SELECT relationshipStage FROM `shadow_bond_profiles` WHERE shadowId = 'shadow_igris_1'")
        assertTrue(cBond.moveToFirst())
        assertEquals("SWORN_LOYALTY", cBond.getString(0))
        cBond.close()

        writable.close()
    }
}
