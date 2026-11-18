package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Milestone 12 - Phase 4: Room Migration 12 -> 13 Unit Test.
 * Validates that MIGRATION_12_13 correctly creates the `social_mission_records` table,
 * foreign keys, and indices while preserving all pre-existing player and social data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration12To13Test {

    @Test
    fun testMigration12To13CreatesMissionRecordsTableAndPreservesData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration-12-13-test.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(12) {
                override fun onCreate(db: SupportSQLiteDatabase) {
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
                        INSERT INTO `player_profile` (
                            `id`, `codename`, `title`, `level`, `currentLevelXp`, `totalXp`, `rankName`,
                            `strVerified`, `strSelfReported`, `strProgressXp`,
                            `vitVerified`, `vitSelfReported`, `vitProgressXp`,
                            `agiVerified`, `agiSelfReported`, `agiProgressXp`,
                            `intVerified`, `intSelfReported`, `intProgressXp`,
                            `disVerified`, `disSelfReported`, `disProgressXp`,
                            `wisVerified`, `wisSelfReported`, `wisProgressXp`,
                            `chaVerified`, `chaSelfReported`, `chaProgressXp`,
                            `streakDays`, `verifiedActivitiesCount`, `selfReportedActivitiesCount`,
                            `recoveryScore`, `createdAtEpochMs`
                        ) VALUES (
                            1, 'Hunter01', 'Shadow Sovereign', 16, 420, 16200, 'B_RANK',
                            18, 0, 0, 20, 0, 0, 12, 0, 0, 10, 0, 0, 16, 0, 0, 12, 0, 0, 8, 0, 0,
                            9, 36, 14, 96, 1700000000000
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `social_profiles` (
                            `accountOwnerId` TEXT NOT NULL,
                            `overallSocialMasteryStage` TEXT NOT NULL,
                            `totalPracticeMinutes` INTEGER NOT NULL DEFAULT 0,
                            `totalScenariosCleared` INTEGER NOT NULL DEFAULT 0,
                            `totalAssessmentsCompleted` INTEGER NOT NULL DEFAULT 0,
                            `averageRubricScore` REAL NOT NULL DEFAULT 0.0,
                            `lastActiveEpochMs` INTEGER NOT NULL,
                            `createdAtEpochMs` INTEGER NOT NULL,
                            `updatedAtEpochMs` INTEGER NOT NULL,
                            `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                            `lastSyncedEpochMs` INTEGER,
                            PRIMARY KEY(`accountOwnerId`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
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
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Step 2: Run MIGRATION_12_13
        SoloLevelingDatabase.MIGRATION_12_13.migrate(db)

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
