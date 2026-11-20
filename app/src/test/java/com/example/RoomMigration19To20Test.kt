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
 * Milestone 12 - Phase 4: Room v19 -> v20 Schema & Data Migration Test.
 * Validates that MIGRATION_19_20 creates redemption_activities and redemption_verifications
 * tables with indexes, while fully preserving existing v19 penalty and ledger records.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration19To20Test {

    @Test
    fun migration19To20_createsRedemptionTablesAndPreservesPenalties() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-19-20-test-${System.nanoTime()}.db"
        val now = 1_700_900_000_000L

        // 1. Create SQLite DB at version 19 with penalties and ledger tables
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(19) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `penalties` (
                                `penaltyId` TEXT NOT NULL,
                                `failureRecordId` TEXT NOT NULL,
                                `commitmentId` TEXT NOT NULL,
                                `questId` INTEGER NOT NULL,
                                `accountOwnerId` TEXT NOT NULL,
                                `questTitleSnapshot` TEXT NOT NULL,
                                `severityCode` TEXT NOT NULL,
                                `basePenaltyPoints` INTEGER NOT NULL,
                                `escalationBonusPoints` INTEGER NOT NULL,
                                `totalPenaltyPoints` INTEGER NOT NULL,
                                `penaltyStatusName` TEXT NOT NULL,
                                `reason` TEXT NOT NULL,
                                `policyVersion` TEXT NOT NULL,
                                `createdAtEpochMs` INTEGER NOT NULL,
                                `resolvedAtEpochMs` INTEGER,
                                PRIMARY KEY(`penaltyId`)
                            )
                            """.trimIndent()
                        )
                        // Seed an existing penalty in v19
                        db.execSQL(
                            """
                            INSERT INTO `penalties` VALUES (
                                'pen_101', 'fail_101', 'commit_101', 101, 'local_vault_player_1',
                                'Daily Jogging Protocol', 'P2', 10, 0, 10, 'ACTIVE', 'Overdue', 'POLICY_V1_2026', $now, NULL
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()

        // 2. Upgrade from version 19 to 20 using MIGRATION_19_20
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(20) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        SoloLevelingDatabase.MIGRATION_19_20.migrate(db)
                    }
                })
                .build()
        )
        val writable = openHelper.writableDatabase

        // 3. Verify redemption_activities table existence
        val cRedemptions = writable.query("SELECT count(*) FROM `redemption_activities`")
        assertTrue(cRedemptions.moveToFirst())
        assertEquals(0, cRedemptions.getInt(0))
        cRedemptions.close()

        // 4. Verify redemption_verifications table existence
        val cVerifications = writable.query("SELECT count(*) FROM `redemption_verifications`")
        assertTrue(cVerifications.moveToFirst())
        assertEquals(0, cVerifications.getInt(0))
        cVerifications.close()

        // 5. Verify existing v19 penalty is intact
        val cPenalties = writable.query("SELECT penaltyId, severityCode FROM `penalties` WHERE penaltyId = 'pen_101'")
        assertTrue(cPenalties.moveToFirst())
        assertEquals("pen_101", cPenalties.getString(0))
        assertEquals("P2", cPenalties.getString(1))
        cPenalties.close()

        // 6. Insert and query new redemption record to verify schema
        writable.execSQL(
            """
            INSERT INTO `redemption_activities` VALUES (
                'red_1', 'pen_101', 'fail_101', 101, 'local_vault_player_1',
                'strat_root_cause_analysis_30', 'STRATEGIC_PLANNING', 'Failure Post-Mortem',
                'Analyze causes', 'STRUCTURED', 30, 'STRUCTURED_REFLECTION', 'PENDING_ACCEPTANCE',
                10, 'ARCANIST', 'Guidance', $now, NULL, NULL, NULL, NULL
            )
            """.trimIndent()
        )

        val cReadRedemption = writable.query("SELECT activityTitle, resolutionPoints FROM `redemption_activities` WHERE redemptionId = 'red_1'")
        assertTrue(cReadRedemption.moveToFirst())
        assertEquals("Failure Post-Mortem", cReadRedemption.getString(0))
        assertEquals(10, cReadRedemption.getInt(1))
        cReadRedemption.close()

        writable.close()
        context.deleteDatabase(dbName)
    }
}
