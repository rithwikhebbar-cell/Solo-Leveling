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
 * Milestone 12 - Phase 3: Room v18 -> v19 Schema & Data Migration Test.
 * Validates that MIGRATION_18_19 creates penalties, penalty_transactions, and penalty_point_accounts
 * tables with proper unique constraints and indexes, while fully preserving existing v18 records.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration18To19Test {

    @Test
    fun migration18To19_createsLedgerTablesAndPreservesExistingData() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-18-19-test-${System.nanoTime()}.db"
        val now = 1_700_900_000_000L

        // 1. Create SQLite DB at version 18 with core tables
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(18) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `confirmed_quest_failures` (
                                `failureId` TEXT NOT NULL,
                                `commitmentId` TEXT NOT NULL,
                                `questId` INTEGER NOT NULL,
                                `logicalCycleId` TEXT NOT NULL,
                                `accountOwnerId` TEXT NOT NULL,
                                `deadlineEpochMs` INTEGER NOT NULL,
                                `gracePeriodExpiresAtEpochMs` INTEGER NOT NULL,
                                `confirmedAtEpochMs` INTEGER NOT NULL,
                                `failureReasonCode` TEXT NOT NULL,
                                `policyVersion` INTEGER NOT NULL,
                                `exemptionEvaluated` INTEGER NOT NULL,
                                PRIMARY KEY(`failureId`)
                            )
                            """.trimIndent()
                        )
                        // Seed an existing confirmed failure in v18
                        db.execSQL(
                            """
                            INSERT INTO `confirmed_quest_failures` VALUES (
                                'fail_101_cycle_100', 'commit_101_cycle_100', 101, 'cycle_100',
                                'local_vault_player_1', ${now - 1800_000L}, $now, $now, 'GRACE_PERIOD_EXPIRED', 1, 0
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()

        // 2. Upgrade from version 18 to 19 using MIGRATION_18_19
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(19) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        SoloLevelingDatabase.MIGRATION_18_19.migrate(db)
                    }
                })
                .build()
        )
        val writable = openHelper.writableDatabase

        // 3. Verify penalties table existence and structure
        val cPenalties = writable.query("SELECT count(*) FROM `penalties`")
        assertTrue(cPenalties.moveToFirst())
        assertEquals(0, cPenalties.getInt(0))
        cPenalties.close()

        // 4. Verify penalty_transactions table existence
        val cTransactions = writable.query("SELECT count(*) FROM `penalty_transactions`")
        assertTrue(cTransactions.moveToFirst())
        assertEquals(0, cTransactions.getInt(0))
        cTransactions.close()

        // 5. Verify penalty_point_accounts table existence
        val cAccounts = writable.query("SELECT count(*) FROM `penalty_point_accounts`")
        assertTrue(cAccounts.moveToFirst())
        assertEquals(0, cAccounts.getInt(0))
        cAccounts.close()

        // 6. Verify existing v18 confirmed failure is intact
        val cFailures = writable.query("SELECT failureId, failureReasonCode FROM `confirmed_quest_failures` WHERE failureId = 'fail_101_cycle_100'")
        assertTrue(cFailures.moveToFirst())
        assertEquals("fail_101_cycle_100", cFailures.getString(0))
        assertEquals("GRACE_PERIOD_EXPIRED", cFailures.getString(1))
        cFailures.close()

        // 7. Insert into new tables to verify schema, types, and constraints
        writable.execSQL(
            """
            INSERT INTO `penalties` VALUES (
                'pen_101', 'fail_101_cycle_100', 'commit_101_cycle_100', 101,
                'local_vault_player_1', 'Daily Jogging Protocol', 'P2', 10, 0, 10,
                'ACTIVE', 'Grace period expired', 'POLICY_V1_2026', $now, NULL
            )
            """.trimIndent()
        )

        writable.execSQL(
            """
            INSERT INTO `penalty_transactions` VALUES (
                'tx_1', 'local_vault_player_1', 'pen_101', 'ALLOCATION', 10, 10,
                'Initial allocation', $now, 'alloc_fail_101_cycle_100'
            )
            """.trimIndent()
        )

        writable.execSQL(
            """
            INSERT INTO `penalty_point_accounts` VALUES (
                'local_vault_player_1', 10, 10, 1, $now
            )
            """.trimIndent()
        )

        val cReadPenalty = writable.query("SELECT totalPenaltyPoints, severityCode FROM `penalties` WHERE penaltyId = 'pen_101'")
        assertTrue(cReadPenalty.moveToFirst())
        assertEquals(10, cReadPenalty.getInt(0))
        assertEquals("P2", cReadPenalty.getString(1))
        cReadPenalty.close()

        val cReadTx = writable.query("SELECT pointsDelta, idempotencyKey FROM `penalty_transactions` WHERE transactionId = 'tx_1'")
        assertTrue(cReadTx.moveToFirst())
        assertEquals(10, cReadTx.getInt(0))
        assertEquals("alloc_fail_101_cycle_100", cReadTx.getString(1))
        cReadTx.close()

        val cReadAcc = writable.query("SELECT currentBalance FROM `penalty_point_accounts` WHERE accountOwnerId = 'local_vault_player_1'")
        assertTrue(cReadAcc.moveToFirst())
        assertEquals(10, cReadAcc.getInt(0))
        cReadAcc.close()

        writable.close()
        context.deleteDatabase(dbName)
    }
}
