package com.example

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.SoloLevelingDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Milestone 12: Room Migration 11 -> 12 Unit Test.
 * Validates that MIGRATION_11_12 correctly creates all 6 Social Mastery tables,
 * indices, and foreign keys while preserving pre-existing database rows.
 */
@RunWith(AndroidJUnit4::class)
class RoomMigration11To12Test {

    private val testDbName = "migration-11-12-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SoloLevelingDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration11To12CreatesAllTablesAndPreservesData() {
        // Step 1: Create Database at Version 11 and insert sample player data
        var db: SupportSQLiteDatabase = helper.createDatabase(testDbName, 11).apply {
            execSQL(
                """
                INSERT INTO `player_profile` (
                    `id`, `level`, `currentLevelXp`, `xpToNextLevel`, `totalXp`, `rank`, `title`,
                    `str`, `vit`, `agi`, `intAttr`, `dis`, `wis`, `cha`,
                    `verifiedActivitiesCount`, `selfReportedActivitiesCount`, `lastActiveEpochMs`
                ) VALUES (
                    1, 15, 350, 800, 14500, 'C_RANK', 'The Iron Wall',
                    25, 28, 20, 18, 24, 20, 16,
                    32, 12, 1700000000000
                )
                """.trimIndent()
            )
            close()
        }

        // Step 2: Run MIGRATION_11_12
        db = helper.runMigrationsAndValidate(
            testDbName,
            12,
            true,
            SoloLevelingDatabase.MIGRATION_11_12
        )

        // Step 3: Validate Pre-existing Data Preservation
        val playerCursor = db.query("SELECT * FROM `player_profile` WHERE `id` = 1")
        assertTrue("Player record must be preserved after v11 -> v12 migration", playerCursor.moveToFirst())
        assertEquals(15, playerCursor.getInt(playerCursor.getColumnIndexOrThrow("level")))
        assertEquals(14500L, playerCursor.getLong(playerCursor.getColumnIndexOrThrow("totalXp")))
        assertEquals("C_RANK", playerCursor.getString(playerCursor.getColumnIndexOrThrow("rank")))
        playerCursor.close()

        // Step 4: Validate All 6 Social Mastery Tables Exist in sqlite_master
        val expectedTables = listOf(
            "social_profiles",
            "social_module_progress",
            "social_scenarios",
            "social_practice_sessions",
            "social_scenario_attempts",
            "social_assessments"
        )

        for (table in expectedTables) {
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$table'")
            assertTrue("Table `$table` must exist in migrated v12 database", cursor.moveToFirst())
            cursor.close()
        }

        // Step 5: Test Insertion into New v12 Social Tables
        db.execSQL(
            """
            INSERT INTO `social_profiles` (
                `accountOwnerId`, `overallSocialMasteryStage`, `totalPracticeMinutes`,
                `totalScenariosCleared`, `totalAssessmentsCompleted`, `averageRubricScore`,
                `lastActiveEpochMs`, `createdAtEpochMs`, `updatedAtEpochMs`, `syncStatus`
            ) VALUES (
                'test_user_v12', 'COMPETENT', 45, 2, 3, 78.5,
                1700000050000, 1700000000000, 1700000050000, 'SYNCED'
            )
            """.trimIndent()
        )

        val profileCursor = db.query("SELECT * FROM `social_profiles` WHERE `accountOwnerId` = 'test_user_v12'")
        assertTrue(profileCursor.moveToFirst())
        assertEquals("COMPETENT", profileCursor.getString(profileCursor.getColumnIndexOrThrow("overallSocialMasteryStage")))
        assertEquals(45, profileCursor.getInt(profileCursor.getColumnIndexOrThrow("totalPracticeMinutes")))
        profileCursor.close()

        db.close()
    }
}
