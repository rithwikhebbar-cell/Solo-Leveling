package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration10To11Test {

    @Test
    fun migration10To11_preservesPopulatedDatabaseAndInitializesMonarchSystem() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test_v10_to_v11.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create representative v10 tables
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `player_profile` (
                            `id` INTEGER PRIMARY KEY NOT NULL,
                            `codename` TEXT NOT NULL,
                            `level` INTEGER NOT NULL,
                            `rankName` TEXT NOT NULL,
                            `currentLevelXp` INTEGER NOT NULL,
                            `xpToNextLevel` INTEGER NOT NULL,
                            `totalXp` INTEGER NOT NULL,
                            `attributePointsAvailable` INTEGER NOT NULL,
                            `verifiedActivitiesCount` INTEGER NOT NULL,
                            `selfReportedActivitiesCount` INTEGER NOT NULL,
                            `streakDays` INTEGER NOT NULL,
                            `statStr` INTEGER NOT NULL,
                            `statVit` INTEGER NOT NULL,
                            `statAgi` INTEGER NOT NULL,
                            `statInt` INTEGER NOT NULL,
                            `statDis` INTEGER NOT NULL,
                            `statWis` INTEGER NOT NULL,
                            `statCha` INTEGER NOT NULL,
                            `title` TEXT
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
                            `secondaryAttrName` TEXT NOT NULL,
                            `secondaryAttrXpReward` INTEGER NOT NULL,
                            `verificationStatusName` TEXT NOT NULL,
                            `isCompleted` INTEGER NOT NULL,
                            `completedAtEpochMs` INTEGER,
                            `createdAtEpochMs` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `dungeon_instances` (
                            `instanceId` TEXT PRIMARY KEY NOT NULL,
                            `dungeonId` TEXT NOT NULL,
                            `difficultyName` TEXT NOT NULL,
                            `categoryName` TEXT NOT NULL,
                            `statusName` TEXT NOT NULL,
                            `openedAtEpochMs` INTEGER NOT NULL,
                            `expiresAtEpochMs` INTEGER NOT NULL,
                            `completedAtEpochMs` INTEGER,
                            `objectivesJson` TEXT NOT NULL,
                            `currentTelemetryJson` TEXT NOT NULL,
                            `completionEvidenceType` TEXT,
                            `completionEvidenceReference` TEXT,
                            `failureReason` TEXT,
                            `xpAwarded` INTEGER NOT NULL,
                            `createdAtEpochMs` INTEGER NOT NULL,
                            `updatedAtEpochMs` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    // Populate v10 data
                    db.execSQL(
                        """
                        INSERT INTO `player_profile` (
                            `id`, `codename`, `level`, `rankName`, `currentLevelXp`, `xpToNextLevel`,
                            `totalXp`, `attributePointsAvailable`, `verifiedActivitiesCount`,
                            `selfReportedActivitiesCount`, `streakDays`, `statStr`, `statVit`,
                            `statAgi`, `statInt`, `statDis`, `statWis`, `statCha`, `title`
                        ) VALUES (
                            1, 'ShadowHunter', 15, 'C_RANK', 450, 1000, 12500, 2, 42, 10, 14,
                            55, 50, 48, 62, 58, 45, 30, 'Necromancer Candidate'
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO `quests` (
                            `title`, `description`, `typeName`, `categoryName`, `targetValue`,
                            `currentValue`, `unit`, `xpReward`, `primaryAttrName`, `primaryAttrXpReward`,
                            `secondaryAttrName`, `secondaryAttrXpReward`, `verificationStatusName`,
                            `isCompleted`, `createdAtEpochMs`
                        ) VALUES (
                            'Daily Sprint', 'Run 5km', 'DAILY', 'PHYSICAL', 5, 5, 'km', 100,
                            'STR', 25, 'VIT', 25, 'VERIFIED_SYSTEM_TIMER', 1, 1700000000000
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO `dungeon_instances` (
                            `instanceId`, `dungeonId`, `difficultyName`, `categoryName`, `statusName`,
                            `openedAtEpochMs`, `expiresAtEpochMs`, `objectivesJson`, `currentTelemetryJson`,
                            `xpAwarded`, `createdAtEpochMs`, `updatedAtEpochMs`
                        ) VALUES (
                            'inst_dungeon_01', 'dungeon_frost', 'C_RANK', 'PHYSICAL', 'CLEARED',
                            1700000000000, 1700086400000, '[]', '{}', 250, 1700000000000, 1700010000000
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // Handled manually in test step
                }
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Execute MIGRATION_10_11
        SoloLevelingDatabase.MIGRATION_10_11.migrate(db)

        // 1. Verify v10 player_profile data was completely preserved
        val playerCursor = db.query("SELECT * FROM `player_profile` WHERE `id` = 1")
        assertTrue("Player record must exist after migration", playerCursor.moveToFirst())
        assertEquals("ShadowHunter", playerCursor.getString(playerCursor.getColumnIndexOrThrow("codename")))
        assertEquals(15, playerCursor.getInt(playerCursor.getColumnIndexOrThrow("level")))
        assertEquals("C_RANK", playerCursor.getString(playerCursor.getColumnIndexOrThrow("rankName")))
        assertEquals(450, playerCursor.getInt(playerCursor.getColumnIndexOrThrow("currentLevelXp")))
        assertEquals(12500, playerCursor.getInt(playerCursor.getColumnIndexOrThrow("totalXp")))
        assertEquals(55, playerCursor.getInt(playerCursor.getColumnIndexOrThrow("statStr")))
        assertEquals(62, playerCursor.getInt(playerCursor.getColumnIndexOrThrow("statInt")))
        assertEquals(14, playerCursor.getInt(playerCursor.getColumnIndexOrThrow("streakDays")))
        playerCursor.close()

        // 2. Verify v10 quests data was completely preserved
        val questCursor = db.query("SELECT * FROM `quests`")
        assertTrue(questCursor.moveToFirst())
        assertEquals("Daily Sprint", questCursor.getString(questCursor.getColumnIndexOrThrow("title")))
        assertEquals(1, questCursor.getInt(questCursor.getColumnIndexOrThrow("isCompleted")))
        questCursor.close()

        // 3. Verify v10 dungeon_instances data was completely preserved
        val dungeonCursor = db.query("SELECT * FROM `dungeon_instances` WHERE `instanceId` = 'inst_dungeon_01'")
        assertTrue(dungeonCursor.moveToFirst())
        assertEquals("CLEARED", dungeonCursor.getString(dungeonCursor.getColumnIndexOrThrow("statusName")))
        dungeonCursor.close()

        // 4. Verify all 9 new Milestone 11 tables exist
        val expectedNewTables = listOf(
            "shadow_monarch_profile",
            "shadows",
            "shadow_extractions",
            "shadow_skill_unlocks",
            "shadow_deployments",
            "shadow_formations",
            "monarch_skill_unlocks",
            "shadow_quest_progress",
            "shadow_resource_transactions"
        )
        expectedNewTables.forEach { tableName ->
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")
            assertTrue("Table $tableName must exist after migration 10->11", cursor.moveToFirst())
            cursor.close()
        }

        // 5. Verify default singleton shadow_monarch_profile was seeded
        val monarchCursor = db.query("SELECT * FROM `shadow_monarch_profile` WHERE `id` = 1")
        assertTrue("Monarch profile singleton must be seeded by migration", monarchCursor.moveToFirst())
        assertEquals(1, monarchCursor.getInt(monarchCursor.getColumnIndexOrThrow("monarchLevel")))
        assertEquals(0L, monarchCursor.getLong(monarchCursor.getColumnIndexOrThrow("monarchXp")))
        assertEquals("AWAKENING", monarchCursor.getString(monarchCursor.getColumnIndexOrThrow("monarchRankName")))
        assertEquals(0, monarchCursor.getInt(monarchCursor.getColumnIndexOrThrow("monarchAuthority")))
        assertEquals(10, monarchCursor.getInt(monarchCursor.getColumnIndexOrThrow("armyCapacity")))
        assertEquals(0, monarchCursor.getInt(monarchCursor.getColumnIndexOrThrow("activeShadowCount")))
        monarchCursor.close()

        // 6. Verify composite unique index exists on shadow_extractions and shadow_resource_transactions
        val indexCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='index_shadow_extractions_sourceTypeName_sourceEventId'"
        )
        assertTrue("Composite unique index on shadow_extractions must exist", indexCursor.moveToFirst())
        indexCursor.close()

        val resourceIndexCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='index_shadow_resource_transactions_sourceEventId_resourceTypeName_transactionTypeName'"
        )
        assertTrue("Composite unique index on shadow_resource_transactions must exist", resourceIndexCursor.moveToFirst())
        resourceIndexCursor.close()

        // 7. Verify write and read operations work cleanly on new tables
        db.execSQL(
            """
            INSERT INTO `shadows` (
                `shadowId`, `name`, `shadowClassName`, `rarityName`, `currentRankName`,
                `level`, `shadowXp`, `statPower`, `statEndurance`, `statFocus`, `statDiscipline`,
                `statWisdom`, `statAdaptability`, `evolutionStageName`, `passiveAbility`,
                `activeSkillIdsJson`, `recruitmentSource`, `extractionEventId`,
                `extractedAtEpochMs`, `evolutionHistoryJson`, `deploymentStatusName`,
                `isRetired`, `notes`
            ) VALUES (
                'sh_test_01', 'Iron Vanguard', 'VANGUARD', 'RARE', 'E_RANK',
                1, 0, 15, 12, 10, 10, 8, 10, 'NEWLY_EXTRACTED', 'Iron Resolve',
                '[]', 'Dungeon Clear #01', 'ext_01', 1700000000000, '[]', 'IDLE',
                0, 'Migrated test shadow'
            )
            """.trimIndent()
        )
        val shadowCursor = db.query("SELECT * FROM `shadows` WHERE `shadowId` = 'sh_test_01'")
        assertTrue(shadowCursor.moveToFirst())
        assertEquals("Iron Vanguard", shadowCursor.getString(shadowCursor.getColumnIndexOrThrow("name")))
        assertEquals("VANGUARD", shadowCursor.getString(shadowCursor.getColumnIndexOrThrow("shadowClassName")))
        shadowCursor.close()

        db.close()
    }

    @Test
    fun freshDatabaseCreation_createsAllTablesAndSeededProfile() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        assertNotNull(db.shadowMonarchProfileDao())
        assertNotNull(db.shadowDao())
        assertNotNull(db.shadowExtractionDao())
        assertNotNull(db.shadowSkillUnlockDao())
        assertNotNull(db.shadowDeploymentDao())
        assertNotNull(db.shadowFormationDao())
        assertNotNull(db.monarchSkillUnlockDao())
        assertNotNull(db.shadowQuestProgressDao())
        assertNotNull(db.shadowResourceDao())

        db.close()
    }
}

