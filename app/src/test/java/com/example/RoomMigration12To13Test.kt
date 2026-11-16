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
 * Milestone 12 - Phase 4: Room Migration 12 -> 13 Unit Test.
 * Validates that MIGRATION_12_13 correctly creates the `social_mission_records` table,
 * foreign keys, and indices while preserving all pre-existing player and social data.
 */
@RunWith(AndroidJUnit4::class)
class RoomMigration12To13Test {

    private val testDbName = "migration-12-13-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SoloLevelingDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration12To13CreatesMissionRecordsTableAndPreservesData() {
        // Step 1: Create Database at Version 12 and insert sample player and social data
        var db: SupportSQLiteDatabase = helper.createDatabase(testDbName, 12).apply {
            execSQL(
                """
                INSERT INTO `player_profile` (
                    `id`, `level`, `currentLevelXp`, `xpToNextLevel`, `totalXp`, `rank`, `title`,
                    `str`, `vit`, `agi`, `intAttr`, `dis`, `wis`, `cha`,
                    `verifiedActivitiesCount`, `selfReportedActivitiesCount`, `lastActiveEpochMs`
                ) VALUES (
                    1, 16, 420, 900, 16200, 'B_RANK', 'Shadow Sovereign',
                    28, 30, 22, 20, 26, 22, 18,
                    36, 14, 1700000000000
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO `social_profiles` (
                    `accountOwnerId`, `overallSocialMasteryStage`, `totalPracticeMinutes`,
                    `totalScenariosCleared`, `totalAssessmentsCompleted`, `averageRubricScore`,
                    `lastActiveEpochMs`, `createdAtEpochMs`, `updatedAtEpochMs`, `syncStatus`
                ) VALUES (
                    'player_v12', 'PROFICIENT', 120, 4, 6, 82.5,
                    1700000050000, 1700000000000, 1700000050000, 'SYNCED'
                )
                """.trimIndent()
            )
            close()
        }

        // Step 2: Run MIGRATION_12_13
        db = helper.runMigrationsAndValidate(
            testDbName,
            13,
            true,
            SoloLevelingDatabase.MIGRATION_12_13
        )

        // Step 3: Validate Pre-existing Data Preservation
        val playerCursor = db.query("SELECT * FROM `player_profile` WHERE `id` = 1")
        assertTrue("Player record must be preserved after v12 -> v13 migration", playerCursor.moveToFirst())
        assertEquals(16, playerCursor.getInt(playerCursor.getColumnIndexOrThrow("level")))
        assertEquals(16200L, playerCursor.getLong(playerCursor.getColumnIndexOrThrow("totalXp")))
        playerCursor.close()

        val socialCursor = db.query("SELECT * FROM `social_profiles` WHERE `accountOwnerId` = 'player_v12'")
        assertTrue("Social profile record must be preserved after v12 -> v13 migration", socialCursor.moveToFirst())
        assertEquals("PROFICIENT", socialCursor.getString(socialCursor.getColumnIndexOrThrow("overallSocialMasteryStage")))
        socialCursor.close()

        // Step 4: Validate `social_mission_records` Table Exists
        val tableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='social_mission_records'")
        assertTrue("Table `social_mission_records` must exist in migrated v13 database", tableCursor.moveToFirst())
        tableCursor.close()

        // Step 5: Test Insertion into New v13 Table
        db.execSQL(
            """
            INSERT INTO `social_mission_records` (
                `recordId`, `accountOwnerId`, `missionId`, `moduleName`, `title`,
                `difficultyRank`, `lifecycleStatus`, `startedEpochMs`, `submittedEpochMs`,
                `completedEpochMs`, `evidenceType`, `reflectionText`, `humanConfirmingRole`,
                `artifactNote`, `aiFeedbackNotes`, `evaluationRubricScore`, `xpAwarded`,
                `verificationStatus`, `idempotencyKey`, `updatedAtEpochMs`
            ) VALUES (
                'rec_v13_01', 'player_v12', 'MSN_COMM_E01', 'COMMUNICATION_MASTERY', 'Check-In',
                'E_RANK', 'COMPLETED', 1700000000000, 1700000060000,
                1700000120000, 'SELF_REFLECTION', 'Connected well with peer.', NULL,
                NULL, NULL, 85, 25,
                'SELF_REPORTED', 'idemp_v13_01', 1700000120000
            )
            """.trimIndent()
        )

        val missionCursor = db.query("SELECT * FROM `social_mission_records` WHERE `recordId` = 'rec_v13_01'")
        assertTrue(missionCursor.moveToFirst())
        assertEquals("COMPLETED", missionCursor.getString(missionCursor.getColumnIndexOrThrow("lifecycleStatus")))
        assertEquals(25, missionCursor.getInt(missionCursor.getColumnIndexOrThrow("xpAwarded")))
        missionCursor.close()

        db.close()
    }
}
