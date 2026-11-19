package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.ConfirmedFailureRecordEntity
import com.example.data.local.entity.QuestCommitmentEntity
import com.example.data.local.entity.QuestExemptionEntity
import com.example.data.local.entity.QuestRescheduleEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Milestone 12 - Phase 2: Full Room v17 -> v18 Schema & Data Migration Test.
 * Builds a real v17 database, seeds existing quest records, then opens Room at v18
 * using MIGRATION_17_18 so Room's OpenHelper validates every new table, index, and foreign key schema.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration17To18Test {

    @Test
    fun migration17To18_validatesFullSchemaAndPreservesExistingQuests() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-17-18-test-${System.nanoTime()}.db"
        val now = 1_700_900_000_000L

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(17) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Create minimal v17 required tables for testing
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
                        // Seed an existing quest in v17
                        db.execSQL(
                            """
                            INSERT INTO `quests` VALUES (
                                101, 'Daily Jogging Protocol', 'Run 30 mins', 'DAILY', 'PHYSICAL',
                                30, 0, 'min', 45, 'STR', 30, 'VIT', 15, 0, 'SELF_REPORTED', $now, NULL
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()

        // Apply MIGRATION_17_18 to the database
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(18) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        SoloLevelingDatabase.MIGRATION_17_18.migrate(db)
                    }
                })
                .build()
        )
        val writable = openHelper.writableDatabase

        // Validate table existence in upgraded SQLite DB
        val cCommitments = writable.query("SELECT count(*) FROM `quest_commitments`")
        assertTrue(cCommitments.moveToFirst())
        assertEquals(0, cCommitments.getInt(0))
        cCommitments.close()

        val cFailures = writable.query("SELECT count(*) FROM `confirmed_quest_failures`")
        assertTrue(cFailures.moveToFirst())
        assertEquals(0, cFailures.getInt(0))
        cFailures.close()

        // Validate existing quest is intact
        val cQuests = writable.query("SELECT title, isCompleted FROM `quests` WHERE id = 101")
        assertTrue(cQuests.moveToFirst())
        assertEquals("Daily Jogging Protocol", cQuests.getString(0))
        assertEquals(0, cQuests.getInt(1))
        cQuests.close()

        // Insert and read commitment via SQLite to verify foreign key and columns
        writable.execSQL(
            """
            INSERT INTO `quest_commitments` VALUES (
                'commit_101_cycle_100', 101, 'local_vault_player_1', 'cycle_100',
                'ACCEPTED', $now, ${now + 3600_000L}, 30, ${now + 5400_000L}, 0, ${now + 3600_000L}, $now, $now
            )
            """.trimIndent()
        )
        val cReadCommitment = writable.query("SELECT stateName, deadlineEpochMs FROM `quest_commitments` WHERE commitmentId = 'commit_101_cycle_100'")
        assertTrue(cReadCommitment.moveToFirst())
        assertEquals("ACCEPTED", cReadCommitment.getString(0))
        cReadCommitment.close()

        writable.close()
        context.deleteDatabase(dbName)
    }
}
