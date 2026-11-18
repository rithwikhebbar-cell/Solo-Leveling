package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Milestone 12 - Phase 5.1: Room Migration 13 -> 14 Verification Test.
 * Validates forward migration from a real v13 schema:
 * - Pre-existing v13 rows in player_profile, social_profiles, and social_mission_records are preserved.
 * - All 4 Phase 5 tables and all 16 indexes are created accurately.
 * - Foreign key cascades delete child Phase 5 rows for acct_a while keeping acct_b completely isolated.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration13To14Test {

    @Test
    fun testMigration13To14PreservesDataCreatesTablesIndexesAndEnforcesCascades() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-13-14-test-${System.nanoTime()}.db"
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(13) {
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
                            1, 'Monarch01', 'Shadow Sovereign', 22, 340, 28500, 'A_RANK',
                            20, 0, 0, 22, 0, 0, 14, 0, 0, 16, 0, 0, 19, 0, 0, 15, 0, 0, 11, 0, 0,
                            14, 52, 8, 94, 1700000000000
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
                        CREATE TABLE IF NOT EXISTS `social_module_progress` (
                            `accountOwnerId` TEXT NOT NULL,
                            `moduleName` TEXT NOT NULL,
                            `masteryStage` TEXT NOT NULL,
                            `masteryStageOrdinal` INTEGER NOT NULL DEFAULT 0,
                            `practiceSeconds` INTEGER NOT NULL DEFAULT 0,
                            `assessmentsCompleted` INTEGER NOT NULL DEFAULT 0,
                            `rollingAverageScore` REAL NOT NULL DEFAULT 0.0,
                            `lastScoreStandardDeviation` REAL NOT NULL DEFAULT 0.0,
                            `consecutivePassingScores` INTEGER NOT NULL DEFAULT 0,
                            `isStale` INTEGER NOT NULL DEFAULT 0,
                            `decayedFromStage` TEXT,
                            `consecutiveRestorationPassingScores` INTEGER NOT NULL DEFAULT 0,
                            `lastAssessedEpochMs` INTEGER NOT NULL,
                            `lastDecayEpochMs` INTEGER,
                            `nextDecayEligibleEpochMs` INTEGER,
                            `updatedAtEpochMs` INTEGER NOT NULL,
                            PRIMARY KEY(`accountOwnerId`, `moduleName`),
                            FOREIGN KEY(`accountOwnerId`) REFERENCES `social_profiles`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `social_scenarios` (
                            `scenarioId` TEXT NOT NULL,
                            `moduleName` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `difficultyTier` TEXT NOT NULL,
                            `contextPrompt` TEXT NOT NULL,
                            `npcRole` TEXT NOT NULL,
                            `targetLearningObjective` TEXT NOT NULL,
                            `minPassingScore` INTEGER NOT NULL DEFAULT 70,
                            `isDeprecated` INTEGER NOT NULL DEFAULT 0,
                            `version` INTEGER NOT NULL DEFAULT 1,
                            PRIMARY KEY(`scenarioId`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `social_practice_sessions` (
                            `sessionId` TEXT NOT NULL,
                            `accountOwnerId` TEXT NOT NULL,
                            `moduleName` TEXT NOT NULL,
                            `activityKind` TEXT NOT NULL,
                            `sessionStatus` TEXT NOT NULL,
                            `durationSeconds` INTEGER NOT NULL,
                            `startedEpochMs` INTEGER NOT NULL,
                            `completedEpochMs` INTEGER,
                            `transcriptText` TEXT,
                            `hasAudioRecording` INTEGER NOT NULL DEFAULT 0,
                            `audioFileName` TEXT,
                            `isCloudTranscriptSyncAllowed` INTEGER NOT NULL DEFAULT 0,
                            `evidenceSourceType` TEXT NOT NULL,
                            `verificationStatus` TEXT NOT NULL,
                            `idempotencyKey` TEXT NOT NULL,
                            `createdAtEpochMs` INTEGER NOT NULL,
                            `updatedAtEpochMs` INTEGER NOT NULL,
                            PRIMARY KEY(`sessionId`),
                            FOREIGN KEY(`accountOwnerId`) REFERENCES `social_profiles`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `social_scenario_attempts` (
                            `attemptId` TEXT NOT NULL,
                            `accountOwnerId` TEXT NOT NULL,
                            `scenarioId` TEXT NOT NULL,
                            `scenarioTitle` TEXT NOT NULL,
                            `moduleName` TEXT NOT NULL,
                            `userResponseText` TEXT NOT NULL,
                            `evaluationScore` INTEGER NOT NULL,
                            `isPassed` INTEGER NOT NULL,
                            `strengthsFeedbackJson` TEXT NOT NULL,
                            `improvementAreasJson` TEXT NOT NULL,
                            `evidenceSourceType` TEXT NOT NULL,
                            `verificationStatus` TEXT NOT NULL,
                            `idempotencyKey` TEXT NOT NULL,
                            `completedEpochMs` INTEGER NOT NULL,
                            PRIMARY KEY(`attemptId`),
                            FOREIGN KEY(`accountOwnerId`) REFERENCES `social_profiles`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                            FOREIGN KEY(`scenarioId`) REFERENCES `social_scenarios`(`scenarioId`) ON UPDATE NO ACTION ON DELETE RESTRICT
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `social_assessments` (
                            `assessmentId` TEXT NOT NULL,
                            `accountOwnerId` TEXT NOT NULL,
                            `moduleName` TEXT NOT NULL,
                            `assessmentType` TEXT NOT NULL,
                            `overallScore` INTEGER NOT NULL,
                            `clarityScore` INTEGER NOT NULL,
                            `empathyScore` INTEGER NOT NULL,
                            `assertivenessScore` INTEGER NOT NULL,
                            `coherenceScore` INTEGER NOT NULL,
                            `qualitativeAnalysis` TEXT NOT NULL,
                            `actionableRecommendationsJson` TEXT NOT NULL,
                            `assessedEpochMs` INTEGER NOT NULL,
                            PRIMARY KEY(`assessmentId`),
                            FOREIGN KEY(`accountOwnerId`) REFERENCES `social_profiles`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `social_mission_records` (
                            `recordId` TEXT NOT NULL,
                            `accountOwnerId` TEXT NOT NULL,
                            `missionId` TEXT NOT NULL,
                            `moduleName` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `difficultyRank` TEXT NOT NULL,
                            `lifecycleStatus` TEXT NOT NULL,
                            `startedEpochMs` INTEGER NOT NULL,
                            `submittedEpochMs` INTEGER,
                            `completedEpochMs` INTEGER,
                            `evidenceType` TEXT NOT NULL,
                            `reflectionText` TEXT,
                            `humanConfirmingRole` TEXT,
                            `artifactNote` TEXT,
                            `aiFeedbackNotes` TEXT,
                            `evaluationRubricScore` INTEGER,
                            `xpAwarded` INTEGER NOT NULL DEFAULT 0,
                            `verificationStatus` TEXT NOT NULL,
                            `idempotencyKey` TEXT NOT NULL,
                            `updatedAtEpochMs` INTEGER NOT NULL,
                            PRIMARY KEY(`recordId`),
                            FOREIGN KEY(`accountOwnerId`) REFERENCES `social_profiles`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    // Insert v13 multi-account records for acct_a and acct_b
                    db.execSQL(
                        """
                        INSERT INTO `social_profiles` (
                            `accountOwnerId`, `overallSocialMasteryStage`, `totalPracticeMinutes`,
                            `totalScenariosCleared`, `totalAssessmentsCompleted`, `averageRubricScore`,
                            `lastActiveEpochMs`, `createdAtEpochMs`, `updatedAtEpochMs`, `syncStatus`
                        ) VALUES
                        ('acct_a', 'COMPETENT', 95, 3, 2, 84.0, 1700000050000, 1700000000000, 1700000050000, 'SYNCED'),
                        ('acct_b', 'NOVICE', 20, 1, 0, 0.0, 1700000060000, 1700000000000, 1700000060000, 'SYNCED')
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO `social_mission_records` (
                            `recordId`, `accountOwnerId`, `missionId`, `moduleName`, `title`,
                            `difficultyRank`, `lifecycleStatus`, `startedEpochMs`, `submittedEpochMs`,
                            `completedEpochMs`, `evidenceType`, `reflectionText`, `humanConfirmingRole`,
                            `artifactNote`, `aiFeedbackNotes`, `evaluationRubricScore`, `xpAwarded`,
                            `verificationStatus`, `idempotencyKey`, `updatedAtEpochMs`
                        ) VALUES (
                            'rec_a_01', 'acct_a', 'MSN_COMM_E01', 'COMMUNICATION_MASTERY', 'Check-In',
                            'E_RANK', 'COMPLETED', 1700000000000, 1700000060000,
                            1700000120000, 'ARTIFACT_ATTACHED', 'Detailed check-in reflection.', NULL,
                            'Meeting notes summary #1', NULL, 86, 25,
                            'SELF_REPORTED', 'msn_reward_rec_a_01', 1700000120000
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase
        try {
            // Execute MIGRATION_13_14 with FK enforcement active
            db.execSQL("PRAGMA foreign_keys = ON")
            SoloLevelingDatabase.MIGRATION_13_14.migrate(db)

            // 1. Validate pre-existing v13 records remain intact
            db.query("SELECT level, totalXp FROM `player_profile` WHERE `id` = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(22, c.getInt(0))
                assertEquals(28500L, c.getLong(1))
            }
            db.query("SELECT overallSocialMasteryStage FROM `social_profiles` WHERE `accountOwnerId` = 'acct_a'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("COMPETENT", c.getString(0))
            }
            db.query("SELECT xpAwarded FROM `social_mission_records` WHERE `recordId` = 'rec_a_01'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(25, c.getInt(0))
            }

            // 2. Verify all 4 new Phase 5 tables exist
            val expectedTables = listOf(
                "social_adaptive_recommendations",
                "social_weekly_training_plans",
                "social_planned_activities",
                "social_competency_progress"
            )
            for (table in expectedTables) {
                db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$table'").use { c ->
                    assertTrue("Table $table must exist after MIGRATION_13_14", c.moveToFirst())
                }
            }

            // 3. Verify all 16 indexes exist
            val expectedIndexes = listOf(
                "index_social_adaptive_recommendations_accountOwnerId",
                "index_social_adaptive_recommendations_targetModule",
                "index_social_adaptive_recommendations_category",
                "index_social_adaptive_recommendations_status",
                "index_social_adaptive_recommendations_createdAtEpochMs",
                "index_social_weekly_training_plans_accountOwnerId",
                "index_social_weekly_training_plans_startDateEpochMs",
                "index_social_weekly_training_plans_isPaused",
                "index_social_planned_activities_planId",
                "index_social_planned_activities_accountOwnerId",
                "index_social_planned_activities_scheduledDateEpochMs",
                "index_social_planned_activities_isCompleted",
                "index_social_competency_progress_accountOwnerId",
                "index_social_competency_progress_moduleName",
                "index_social_competency_progress_competencyName",
                "index_social_competency_progress_masteryStage"
            )
            assertEquals(16, expectedIndexes.size)
            for (idx in expectedIndexes) {
                db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='$idx'").use { c ->
                    assertTrue("Index $idx must exist after MIGRATION_13_14", c.moveToFirst())
                }
            }

            // 4. Populate all 4 Phase 5 tables for both acct_a and acct_b
            for (acct in listOf("acct_a", "acct_b")) {
                db.execSQL(
                    """
                    INSERT INTO `social_adaptive_recommendations` VALUES (
                        'rec_$acct', '$acct', 'Practice Active Listening', 'COMMUNICATION_MASTERY',
                        'Active Listening', 'PRACTICE_NEW_SKILL', 'Start foundational practice.',
                        '["Unexplored skill"]', 'EX_COMM_01', 'EXERCISE', 10, 'NORMAL',
                        'Build listening habits', 'NEW', 1700000200000, 1700000200000
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `social_weekly_training_plans` VALUES (
                        'plan_$acct', '$acct', 'Weekly Comm Plan', 15, 5,
                        1700000200000, 1700605000000, 0, 1700000200000, 1700000200000
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `social_planned_activities` VALUES (
                        'act_$acct', 'plan_$acct', '$acct', 1, 1700000200000,
                        'COMMUNICATION_MASTERY', 'Active Listening', 'EXERCISE',
                        'Practice Drill', 'EX_COMM_01', 15, 'NORMAL', 0, 0, NULL, NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `social_competency_progress` VALUES (
                        'comp_$acct', '$acct', 'COMMUNICATION_MASTERY', 'Active Listening',
                        'DEVELOPING', 'NORMAL', 3, 1700000100000, 78, 'SIMULATION_EVALUATED', 1700000200000
                    )
                    """.trimIndent()
                )
            }

            // 5. Deleting acct_a's social_profile must cascade-delete acct_a Phase 5 rows and leave acct_b untouched
            db.execSQL("DELETE FROM `social_profiles` WHERE `accountOwnerId` = 'acct_a'")

            for (table in expectedTables) {
                db.query("SELECT COUNT(*) FROM `$table` WHERE `accountOwnerId` = 'acct_a'").use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals("Cascade delete failed for acct_a in $table", 0, c.getInt(0))
                }
                db.query("SELECT COUNT(*) FROM `$table` WHERE `accountOwnerId` = 'acct_b'").use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals("acct_b isolation violated in $table", 1, c.getInt(0))
                }
            }
            // Verify mission FK cascade as well
            db.query("SELECT COUNT(*) FROM `social_mission_records` WHERE `accountOwnerId` = 'acct_a'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
        } finally {
            db.close()
        }
    }
}
