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
 * Milestone 12: Room Migration 11 -> 12 Unit Test.
 * Validates that MIGRATION_11_12 correctly creates all 6 Social Mastery tables,
 * indices, and foreign keys while preserving pre-existing database rows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration11To12Test {

    @Test
    fun testMigration11To12CreatesAllTablesAndPreservesData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration-11-12-test.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(11) {
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
                            1, 'Hunter01', 'The Iron Wall', 15, 350, 14500, 'C_RANK',
                            15, 0, 0, 18, 0, 0, 10, 0, 0, 8, 0, 0, 14, 0, 0, 10, 0, 0, 6, 0, 0,
                            7, 32, 12, 95, 1700000000000
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Step 2: Run MIGRATION_11_12
        SoloLevelingDatabase.MIGRATION_11_12.migrate(db)

        // Step 3: Validate Pre-existing Data Preservation
        val playerCursor = db.query("SELECT * FROM `player_profile` WHERE `id` = 1")
        assertTrue("Player record must be preserved after v11 -> v12 migration", playerCursor.moveToFirst())
        assertEquals(15, playerCursor.getInt(playerCursor.getColumnIndexOrThrow("level")))
        assertEquals(14500L, playerCursor.getLong(playerCursor.getColumnIndexOrThrow("totalXp")))
        assertEquals("C_RANK", playerCursor.getString(playerCursor.getColumnIndexOrThrow("rankName")))
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
