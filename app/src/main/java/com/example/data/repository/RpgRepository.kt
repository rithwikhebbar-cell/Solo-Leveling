package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.AchievementUnlockRecordEntity
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.BossBattleInstanceEntity
import com.example.data.local.entity.DungeonCompletionRecordEntity
import com.example.data.local.entity.DungeonInstanceEntity
import com.example.data.local.entity.PlayerInventoryRecordEntity
import com.example.data.local.entity.RpgRewardAuditEntity
import com.example.data.local.entity.ShadowCollectionRecordEntity
import com.example.data.local.entity.SkillTreeDependencyEntity
import com.example.data.local.entity.SkillUnlockRecordEntity
import com.example.data.local.entity.SystemNotificationEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.engine.AchievementEngine
import com.example.domain.engine.BossBattleEngine
import com.example.domain.engine.DungeonEngine
import com.example.domain.engine.PlayerTelemetrySnapshot
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.RpgCatalogs
import com.example.domain.engine.SkillCatalog
import com.example.domain.engine.SkillEffectEngine
import com.example.domain.engine.SkillEngine
import com.example.domain.model.AcademicQuestStatus
import com.example.domain.model.AchievementWithStatus
import com.example.domain.model.ActiveSkillEffects
import com.example.domain.model.AiDungeonProposal
import com.example.domain.model.AttributeType
import com.example.domain.model.BossBattleWithPhases
import com.example.domain.model.BossStatus
import com.example.domain.model.DungeonCompletionRecord
import com.example.domain.model.DungeonCreationSource
import com.example.domain.model.DungeonDefinition
import com.example.domain.model.DungeonObjective
import com.example.domain.model.DungeonStatus
import com.example.domain.model.DungeonWithStatus
import com.example.domain.model.HunterRank
import com.example.domain.model.InventoryCategory
import com.example.domain.model.InventoryItemWithOwnership
import com.example.domain.model.NotificationCategory
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.PlayerProfile
import com.example.domain.model.PlayerTitleDefinition
import com.example.domain.model.ProposalStatus
import com.example.domain.model.QuestCategory
import com.example.domain.model.RecoveryState
import com.example.domain.model.RpgRewardAuditRecord
import com.example.domain.model.ShadowCollectibleWithStatus
import com.example.domain.model.SkillWithStatus
import com.example.domain.model.VerificationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

data class RpgDashboardSnapshot(
    val profile: PlayerProfile = PlayerProfile(),
    val dungeons: List<DungeonWithStatus> = emptyList(),
    val completionHistory: List<DungeonCompletionRecord> = emptyList(),
    val bossBattles: List<BossBattleWithPhases> = emptyList(),
    val skills: List<SkillWithStatus> = emptyList(),
    val activeSkillEffects: ActiveSkillEffects = ActiveSkillEffects(),
    val achievements: List<AchievementWithStatus> = emptyList(),
    val titles: List<PlayerTitleDefinition> = emptyList(),
    val inventoryItems: List<InventoryItemWithOwnership> = emptyList(),
    val shadowCollection: List<ShadowCollectibleWithStatus> = emptyList(),
    val rewardAuditHistory: List<RpgRewardAuditRecord> = emptyList(),
    val aiDungeonProposals: List<AiDungeonProposal> = emptyList()
)

/**
 * Coordinates all Milestone 6 RPG subsystems:
 * - Dungeon Gates & Adaptive Daily/Weekly Challenges
 * - Multi-stage Boss Battles
 * - Skill System, 5 Trees & SkillEffectEngine
 * - Achievements, Titles, Inventory & Shadow Collection
 * - Bounded, duplicate-safe ProgressionEngine XP/Attribute integration
 */
class RpgRepository(
    private val database: SoloLevelingDatabase,
    @Suppress("unused") private val preferencesRepository: UserPreferencesRepository
) {
    private val playerDao = database.playerDao()
    private val dungeonDao = database.dungeonDao()
    private val bossDao = database.bossBattleDao()
    private val skillDao = database.skillDao()
    private val achievementTitleDao = database.achievementTitleDao()
    private val inventoryShadowDao = database.inventoryShadowDao()
    private val auditDao = database.rpgAuditDao()
    private val notificationDao = database.systemNotificationDao()
    private val activityLogDao = database.activityLogDao()

    private fun todayStartEpochMs(zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()

    /**
     * Reactive combined stream of the full Milestone 6 RPG state.
     * Uses nested combine to respect Kotlin Flow's typed arity cleanly.
     */
    val rpgStateFlow: Flow<RpgDashboardSnapshot> = combine(
        observeCoreTelemetryFlow(),
        combine(
            dungeonDao.observeDefinitions(),
            dungeonDao.observeAllObjectives(),
            dungeonDao.observeAllInstances(),
            dungeonDao.observeCompletionHistory(),
            dungeonDao.observeDungeonProposals()
        ) { defs, objs, insts, completions, aiProps ->
            DungeonTablesBundle(defs, objs, insts, completions, aiProps)
        },
        combine(
            bossDao.observeBossDefinitions(),
            bossDao.observeAllPhases(),
            bossDao.observeAllBossInstances()
        ) { defs, phases, insts ->
            BossTablesBundle(defs, phases, insts)
        },
        combine(
            skillDao.observeSkillDefinitions(),
            skillDao.observeSkillEffects(),
            skillDao.observeDependencies(),
            skillDao.observeSkillUnlocks()
        ) { defs, effects, deps, unlocks ->
            SkillTablesBundle(defs, effects, deps, unlocks)
        },
        combine(
            achievementTitleDao.observeAchievementDefinitions(),
            achievementTitleDao.observeAchievementUnlocks(),
            achievementTitleDao.observePlayerTitles(),
            inventoryShadowDao.observeInventoryItems(),
            inventoryShadowDao.observePlayerInventory()
        ) { achDefs, achUnlocks, titles, invItems, invRecs ->
            MetaTablesBundle(achDefs, achUnlocks, titles, invItems, invRecs)
        }
    ) { telemetryBase, dungeonBundle, bossBundle, skillBundle, metaBundle ->
        Triple(telemetryBase, dungeonBundle, Triple(bossBundle, skillBundle, metaBundle))
    }.combine(
        combine(
            inventoryShadowDao.observeShadowCollection(),
            auditDao.observeRewardAudit()
        ) { shadows, audits -> shadows to audits }
    ) { (telemetryBase, dungeonBundle, triple), (shadows, audits) ->
        val (bossBundle, skillBundle, metaBundle) = triple
        val completions = dungeonBundle.completions.map { it.toDomain() }
        val bossInstances = bossBundle.insts.map { it.toDomain() }
        val telemetry = telemetryBase.copy(
            completedDungeonsCount = completions.size,
            completedBossesCount = bossInstances.count { it.status == BossStatus.COMPLETED },
            unlockedSkillsCount = skillBundle.unlocks.size
        )

        val objectivesByDungeon = dungeonBundle.objs.map { it.toDomain() }.groupBy { it.dungeonId }
        val instancesByDungeon = dungeonBundle.insts.map { it.toDomain() }.groupBy { it.dungeonId }
        val completedCountByDungeon = completions.groupBy { it.dungeonId }.mapValues { it.value.size }

        val dungeons = dungeonBundle.defs.map { defEntity ->
            val def = defEntity.toDomain()
            val latestInst = instancesByDungeon[def.id]?.maxByOrNull { it.id }
            DungeonEngine.evaluateDungeonWithStatus(
                definition = def,
                objectives = objectivesByDungeon[def.id].orEmpty(),
                latestInstance = latestInst,
                completedCount = completedCountByDungeon[def.id] ?: 0,
                telemetry = telemetry
            )
        }

        val phasesByBoss = bossBundle.phases.map { it.toDomain() }.groupBy { it.bossId }
        val instancesByBoss = bossInstances.groupBy { it.bossId }
        val bosses = bossBundle.defs.map { bossEntity ->
            val def = bossEntity.toDomain()
            val latestInst = instancesByBoss[def.id]?.maxByOrNull { it.id }
            BossBattleEngine.evaluateBossWithPhases(
                definition = def,
                phases = phasesByBoss[def.id].orEmpty(),
                latestInstance = latestInst,
                telemetry = telemetry
            )
        }

        val unlockMap = skillBundle.unlocks.map { it.toDomain() }.associateBy { it.skillId }
        val effectList = skillBundle.effects.map { it.toDomain() }
        val depList = skillBundle.deps.map { it.toDomain() }
        val skills = skillBundle.defs.map { defEntity ->
            SkillEngine.evaluateSkillWithStatus(
                definition = defEntity.toDomain(),
                dependencies = depList,
                effects = effectList,
                unlockedMap = unlockMap,
                profile = telemetry.profile
            )
        }
        val activeEffects = SkillEffectEngine.evaluateActiveEffects(
            unlockedRecords = unlockMap.values.toList(),
            effectsCatalog = if (effectList.isNotEmpty()) effectList else SkillCatalog.effects
        )

        val achUnlockMap = metaBundle.achUnlocks.map { it.toDomain() }.associateBy { it.achievementId }
        val achievements = AchievementEngine.evaluateAll(telemetry, achUnlockMap)

        val invMap = metaBundle.invRecs.map { it.toDomain() }.associateBy { it.itemId }
        val inventory = metaBundle.invItems.map { item ->
            InventoryItemWithOwnership(definition = item.toDomain(), record = invMap[item.id])
        }

        val shadowMap = shadows.map { it.toDomain() }.associateBy { it.collectibleId }
        val shadowCollection = RpgCatalogs.shadowCollectibles.map { def ->
            ShadowCollectibleWithStatus(definition = def, record = shadowMap[def.id])
        }

        RpgDashboardSnapshot(
            profile = telemetry.profile,
            dungeons = dungeons,
            completionHistory = completions,
            bossBattles = bosses,
            skills = skills,
            activeSkillEffects = activeEffects,
            achievements = achievements,
            titles = metaBundle.titles.map { it.toDomain() },
            inventoryItems = inventory,
            shadowCollection = shadowCollection,
            rewardAuditHistory = audits.map { it.toDomain() },
            aiDungeonProposals = dungeonBundle.aiProps.map { it.toDomain() }
        )
    }

    private fun observeCoreTelemetryFlow(): Flow<PlayerTelemetrySnapshot> {
        val physicalFlow = combine(
            playerDao.observePlayer(),
            database.recoveryAssessmentDao().observeLatest(),
            database.joggingSessionDao().observeAll(),
            database.workoutSessionDao().observeAllWorkouts()
        ) { playerEnt, recEnt, jogs, workouts ->
            PhysicalTelemetryPart(
                profile = playerEnt?.toDomain() ?: PlayerProfile(),
                recovery = recEnt?.toDomain()?.state ?: RecoveryState.READY,
                jogs = jogs.map { it.toDomain() },
                workouts = workouts.map { it.toDomain() }
            )
        }
        val academicFlow = combine(
            database.studySessionDao().observeAllSessions(),
            database.academicSubjectDao().observeAllChapters(),
            database.revisionRecordDao().observeAllRevisions(),
            database.academicAssessmentDao().observeAllAssessments()
        ) { studies, chapters, revisions, assessments ->
            AcademicTelemetryPart(
                studies = studies.map { it.toDomain() },
                chapters = chapters.map { it.toDomain() },
                revisions = revisions.map { it.toDomain() },
                assessments = assessments.map { it.toDomain() }
            )
        }
        val lifeFlow = combine(
            database.habitDao().observeCompletionsSince(LocalDate.now().toEpochDay() - 90L),
            database.routineDao().observeCompletionsSince(LocalDate.now().toEpochDay() - 90L),
            database.personalTaskDao().observeAllTasks(),
            database.goalDao().observeAllMilestones(),
            combine(
                database.questDao().observeAllQuests(),
                database.academicQuestDao().observeAllAcademicQuests()
            ) { gq, aq -> gq to aq }
        ) { habits, routines, tasks, milestones, (genQuests, acadQuests) ->
            LifeTelemetryPart(
                habits = habits.map { it.toDomain() },
                routines = routines.map { it.toDomain() },
                tasks = tasks.map { it.toDomain(null) },
                milestones = milestones.map { it.toDomain() },
                completedQuestsCount = genQuests.count { it.isCompleted } + acadQuests.count { it.statusName == AcademicQuestStatus.COMPLETED.name },
                activeWorkloadCount = genQuests.count { !it.isCompleted } +
                    acadQuests.count { it.statusName == AcademicQuestStatus.ACTIVE.name } +
                    tasks.count { it.statusName == PersonalTaskStatus.PENDING.name || it.statusName == PersonalTaskStatus.IN_PROGRESS.name }
            )
        }

        return combine(physicalFlow, academicFlow, lifeFlow) { p, a, l ->
            PlayerTelemetrySnapshot(
                profile = p.profile,
                recoveryState = p.recovery,
                todayEpochDay = LocalDate.now().toEpochDay(),
                nowEpochMs = System.currentTimeMillis(),
                jogs = p.jogs,
                workouts = p.workouts,
                studySessions = a.studies,
                chapters = a.chapters,
                revisions = a.revisions,
                assessments = a.assessments,
                habitCompletions = l.habits,
                routineCompletions = l.routines,
                completedTasks = l.tasks,
                completedMilestones = l.milestones,
                completedQuestsCount = l.completedQuestsCount,
                activeWorkloadCount = l.activeWorkloadCount
            )
        }
    }

    suspend fun collectTelemetrySnapshotOnce(nowEpochMs: Long = System.currentTimeMillis()): PlayerTelemetrySnapshot {
        val player = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
        val recovery = database.recoveryAssessmentDao().getLatestOnce()?.toDomain()?.state ?: RecoveryState.READY
        val jogs = database.joggingSessionDao().observeAll().first().map { it.toDomain() }
        val workouts = database.workoutSessionDao().observeAllWorkouts().first().map { it.toDomain() }
        val studies = database.studySessionDao().observeAllSessions().first().map { it.toDomain() }
        val chapters = database.academicSubjectDao().observeAllChapters().first().map { it.toDomain() }
        val revisions = database.revisionRecordDao().observeAllRevisions().first().map { it.toDomain() }
        val assessments = database.academicAssessmentDao().observeAllAssessments().first().map { it.toDomain() }
        val todayDay = LocalDate.now().toEpochDay()
        val habits = database.habitDao().getCompletionsSinceOnce(todayDay - 90L).map { it.toDomain() }
        val routines = database.routineDao().getCompletionsSinceOnce(todayDay - 90L).map { it.toDomain() }
        val tasks = database.personalTaskDao().getAllTasksOnce().map { it.toDomain(null) }
        val milestones = database.goalDao().observeAllMilestones().first().map { it.toDomain() }
        val genQuests = database.questDao().getAllQuestsOnce()
        val acadQuests = database.academicQuestDao().getAllAcademicQuestsOnce()
        val completedDungeons = dungeonDao.getTotalClearedCountOnce()
        val completedBosses = bossDao.getAllBossInstancesOnce().count { it.statusName == BossStatus.COMPLETED.name }
        val unlockedSkills = skillDao.getAllUnlocksOnce().size
        val exemptRest = habits.count { it.status == com.example.domain.model.HabitDayStatus.SKIPPED }

        return PlayerTelemetrySnapshot(
            profile = player,
            recoveryState = recovery,
            todayEpochDay = todayDay,
            nowEpochMs = nowEpochMs,
            jogs = jogs,
            workouts = workouts,
            studySessions = studies,
            chapters = chapters,
            revisions = revisions,
            assessments = assessments,
            habitCompletions = habits,
            routineCompletions = routines,
            completedTasks = tasks,
            completedMilestones = milestones,
            completedQuestsCount = genQuests.count { it.isCompleted } + acadQuests.count { it.statusName == AcademicQuestStatus.COMPLETED.name },
            completedDungeonsCount = completedDungeons,
            completedBossesCount = completedBosses,
            unlockedSkillsCount = unlockedSkills,
            activeWorkloadCount = genQuests.count { !it.isCompleted } +
                acadQuests.count { it.statusName == AcademicQuestStatus.ACTIVE.name } +
                tasks.count { it.status == PersonalTaskStatus.PENDING || it.status == PersonalTaskStatus.IN_PROGRESS },
            exemptRestDaysCount = exemptRest
        )
    }

    suspend fun ensureRpgSeededAndSynced() {
        database.withTransaction {
            val now = System.currentTimeMillis()
            // 1. Seed system dungeon definitions & objectives
            val existingCodes = dungeonDao.getAllDefinitionsOnce().map { it.code }.toMutableSet()
            DungeonEngine.defaultCatalog().forEach { spec ->
                if (spec.definition.code !in existingCodes) {
                    val insertedId = dungeonDao.insertDefinition(spec.definition.toEntity())
                    if (insertedId > 0) {
                        dungeonDao.insertObjectives(spec.objectives.map { it.toEntity(insertedId) })
                        existingCodes.add(spec.definition.code)
                    }
                }
            }

            // 2. Seed Boss Battles & Phases
            val existingBossCodes = bossDao.getAllBossDefinitionsOnce().map { it.code }.toSet()
            BossBattleEngine.defaultBossCatalog().forEach { spec ->
                if (spec.definition.code !in existingBossCodes) {
                    val bossId = bossDao.insertBossDefinition(spec.definition.toEntity())
                    if (bossId > 0) {
                        bossDao.insertPhases(spec.phases.map { it.toEntity(bossId) })
                    }
                }
            }

            // 3. Seed 19 Skills, Effects, and Dependencies
            if (skillDao.getAllDefinitionsOnce().size < SkillCatalog.skills.size) {
                skillDao.insertSkillDefinitions(SkillCatalog.skills.map { it.toEntity() })
                skillDao.insertSkillEffects(SkillCatalog.effects.map { it.toEntity() })
                skillDao.insertDependencies(
                    SkillCatalog.dependencies.map {
                        SkillTreeDependencyEntity(
                            skillId = it.skillId,
                            prerequisiteSkillId = it.prerequisiteSkillId,
                            minPrerequisiteMastery = it.minPrerequisiteMastery
                        )
                    }
                )
            }

            // 4. Seed Achievements, Titles, Inventory Catalog & Default Gear
            if (achievementTitleDao.getAllAchievementDefinitionsOnce().size < RpgCatalogs.achievements.size) {
                achievementTitleDao.insertAchievementDefinitions(RpgCatalogs.achievements.map { it.toEntity() })
            }
            if (achievementTitleDao.getAllPlayerTitlesOnce().size < RpgCatalogs.titles.size) {
                val seededTitles = RpgCatalogs.titles.map { t ->
                    if (t.id == "title_system_initiate") {
                        t.copy(isUnlocked = true, isEquipped = true, unlockedAtEpochMs = now).toEntity()
                    } else {
                        t.toEntity()
                    }
                }
                achievementTitleDao.insertTitles(seededTitles)
            }
            if (inventoryShadowDao.getAllInventoryItemsOnce().size < RpgCatalogs.inventoryItems.size) {
                inventoryShadowDao.insertInventoryItems(RpgCatalogs.inventoryItems.map { it.toEntity() })
                listOf("item_frame_obsidian_default", "item_theme_midnight_cyan").forEach { itemId ->
                    inventoryShadowDao.insertInventoryRecord(
                        PlayerInventoryRecordEntity(
                            itemId = itemId,
                            quantity = 1,
                            isEquipped = true,
                            sourceDescription = "System Initialization Gear",
                            acquiredAtEpochMs = now
                        )
                    )
                }
            }

            // 5. Generate Adaptive Daily & Weekly Dungeons (no penalty if missed previously)
            val telemetry = collectTelemetrySnapshotOnce(now)
            val adaptiveSpecs = DungeonEngine.buildAdaptiveDailyAndWeeklyDungeons(telemetry, existingCodes)
            adaptiveSpecs.forEach { spec ->
                val id = dungeonDao.insertDefinition(spec.definition.toEntity())
                if (id > 0) {
                    dungeonDao.insertObjectives(spec.objectives.map { it.toEntity(id) })
                }
            }

            // 6. Evaluate any newly earned achievements/titles/collectibles deterministically
            evaluateAutoUnlocksInternal(now)
        }
    }

    /**
     * Enters a Dungeon Gate. Grants ZERO XP on entry — only real objective validation grants rewards.
     */
    suspend fun enterDungeon(
        dungeonId: Long,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<Long> = database.withTransaction {
        val def = dungeonDao.getDefinitionById(dungeonId)?.toDomain()
            ?: return@withTransaction Result.failure(IllegalArgumentException("Dungeon gate not found."))
        val player = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
        val (eligible, blockers) = DungeonEngine.checkEligibility(def, player)
        if (!eligible) {
            return@withTransaction Result.failure(IllegalStateException("Gate locked: ${blockers.joinToString("; ")}"))
        }

        val activeOrPaused = dungeonDao.getActiveOrPausedInstanceForDungeon(dungeonId)
        if (activeOrPaused != null) {
            return@withTransaction Result.failure(IllegalStateException("This dungeon gate is already ${activeOrPaused.statusName.lowercase()}."))
        }

        if (!def.isRepeatable) {
            val previouslyCleared = dungeonDao.getAllInstancesOnce().any {
                it.dungeonId == dungeonId && it.statusName == DungeonStatus.COMPLETED.name
            }
            if (previouslyCleared) {
                return@withTransaction Result.failure(IllegalStateException("Non-repeatable dungeon gate already cleared."))
            }
        }

        val deadline = nowEpochMs + def.timeLimitHours.coerceAtLeast(1) * 3_600_000L
        val instanceId = dungeonDao.insertInstance(
            DungeonInstanceEntity(
                dungeonId = dungeonId,
                statusName = DungeonStatus.ACTIVE.name,
                creationSourceName = def.creationSource.name,
                cadenceKey = def.code,
                startedAtEpochMs = nowEpochMs,
                pausedAtEpochMs = null,
                accumulatedPausedMs = 0L,
                deadlineEpochMs = deadline,
                completedAtEpochMs = null,
                notes = "Gate entered (${def.difficulty.displayName}). Zero XP until objectives validated."
            )
        )
        notificationDao.insertNotification(
            SystemNotificationEntity(
                title = "Dungeon Entered: ${def.title}",
                message = "Objectives live (${def.timeLimitHours}h window). Complete real-world tasks or verified sessions to clear.",
                categoryName = NotificationCategory.DUNGEON_UPDATE.name,
                isRead = false,
                timestampEpochMs = nowEpochMs
            )
        )
        Result.success(instanceId)
    }

    suspend fun pauseOrResumeDungeon(
        instanceId: Long,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<String> = database.withTransaction {
        val inst = dungeonDao.getInstanceById(instanceId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Dungeon instance not found."))
        val updated = when (inst.statusName) {
            DungeonStatus.ACTIVE.name -> inst.copy(
                statusName = DungeonStatus.PAUSED.name,
                pausedAtEpochMs = nowEpochMs
            )
            DungeonStatus.PAUSED.name -> {
                val pausedDuration = if (inst.pausedAtEpochMs != null) (nowEpochMs - inst.pausedAtEpochMs).coerceAtLeast(0L) else 0L
                inst.copy(
                    statusName = DungeonStatus.ACTIVE.name,
                    pausedAtEpochMs = null,
                    accumulatedPausedMs = inst.accumulatedPausedMs + pausedDuration
                )
            }
            else -> return@withTransaction Result.failure(IllegalStateException("Only active or paused dungeons can be toggled."))
        }
        dungeonDao.updateInstance(updated)
        Result.success(if (updated.statusName == DungeonStatus.PAUSED.name) "Dungeon paused." else "Dungeon resumed.")
    }

    suspend fun abandonDungeon(
        instanceId: Long,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<String> = database.withTransaction {
        val inst = dungeonDao.getInstanceById(instanceId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Dungeon instance not found."))
        if (inst.statusName != DungeonStatus.ACTIVE.name && inst.statusName != DungeonStatus.PAUSED.name) {
            return@withTransaction Result.failure(IllegalStateException("Dungeon is not active."))
        }
        dungeonDao.updateInstance(
            inst.copy(
                statusName = DungeonStatus.ABANDONED.name,
                completedAtEpochMs = nowEpochMs,
                notes = "Abandoned by player without reward."
            )
        )
        Result.success("Dungeon gate abandoned safely with zero penalty.")
    }

    /**
     * Validates real objective records and completes an active dungeon gate,
     * applying bounded XP/attributes via ProgressionEngine and preventing duplicate claims.
     */
    suspend fun completeDungeon(
        instanceId: Long,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<String> = database.withTransaction {
        val instEntity = dungeonDao.getInstanceById(instanceId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Dungeon instance not found."))
        val inst = instEntity.toDomain()
        val def = dungeonDao.getDefinitionById(inst.dungeonId)?.toDomain()
            ?: return@withTransaction Result.failure(IllegalArgumentException("Dungeon definition missing."))
        val objectives = dungeonDao.getObjectivesForDungeonOnce(def.id).map { it.toDomain() }

        val rewardKey = "DUNGEON_CLEAR_${inst.id}"
        if (auditDao.getByRewardKey(rewardKey) != null) {
            return@withTransaction Result.failure(IllegalStateException("Reward already claimed for this dungeon instance."))
        }

        val telemetry = collectTelemetrySnapshotOnce(nowEpochMs)
        val evaluated = DungeonEngine.evaluateDungeonWithStatus(
            definition = def,
            objectives = objectives,
            latestInstance = inst,
            completedCount = telemetry.completedDungeonsCount,
            telemetry = telemetry
        )
        val validation = DungeonEngine.validateCompletion(evaluated, inst, telemetry)
        if (!validation.canComplete) {
            return@withTransaction Result.failure(IllegalStateException(validation.failureReason ?: "Dungeon objectives incomplete."))
        }

        val rpgXpToday = auditDao.sumRpgXpSince(todayStartEpochMs())
        val reward = ProgressionEngine.calculateRpgReward(
            baseXp = def.baseXpReward,
            primaryAttribute = def.primaryAttribute,
            primaryAttrXp = def.primaryAttrXp,
            secondaryAttribute = def.secondaryAttribute,
            secondaryAttrXp = def.secondaryAttrXp,
            hasVerifiedEvidence = validation.hasVerifiedEvidence,
            rpgXpAlreadyEarnedToday = rpgXpToday,
            sourceLabel = def.title
        )

        dungeonDao.updateInstance(
            instEntity.copy(
                statusName = DungeonStatus.COMPLETED.name,
                completedAtEpochMs = nowEpochMs,
                notes = "Cleared: ${validation.evidenceSummary}"
            )
        )
        dungeonDao.insertCompletionRecord(
            DungeonCompletionRecordEntity(
                rewardKey = rewardKey,
                instanceId = inst.id,
                dungeonId = def.id,
                dungeonTitle = def.title,
                difficultyName = def.difficulty.name,
                xpAwarded = reward.xpAwarded,
                primaryAttrName = reward.primaryAttribute.name,
                primaryAttrXp = reward.primaryAttrXpAwarded,
                secondaryAttrName = reward.secondaryAttribute.name,
                secondaryAttrXp = reward.secondaryAttrXpAwarded,
                evidenceSummary = validation.evidenceSummary,
                verificationProvenance = validation.verificationProvenance,
                completedAtEpochMs = nowEpochMs
            )
        )

        applyRpgProgressionReward(
            rewardKey = rewardKey,
            sourceType = "DUNGEON",
            sourceId = def.code,
            title = def.title,
            xpAwarded = reward.xpAwarded,
            primaryAttr = reward.primaryAttribute,
            primaryAttrXp = reward.primaryAttrXpAwarded,
            secondaryAttr = reward.secondaryAttribute,
            secondaryAttrXp = reward.secondaryAttrXpAwarded,
            isVerified = validation.hasVerifiedEvidence,
            provenanceLabel = validation.verificationProvenance,
            wasCapped = reward.wasCapped,
            notes = reward.validationNote,
            nowEpochMs = nowEpochMs
        )

        grantConfiguredUnlocks(
            titleId = def.rewardTitleId,
            itemId = def.rewardItemId,
            shadowId = def.rewardShadowId,
            sourceDesc = "Dungeon Cleared: ${def.title}",
            nowEpochMs = nowEpochMs
        )

        evaluateAutoUnlocksInternal(nowEpochMs)
        Result.success("Dungeon Cleared! +${reward.xpAwarded} XP (${reward.validationNote})")
    }

    suspend fun startBossBattle(
        bossId: Long,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<Long> = database.withTransaction {
        val boss = bossDao.getBossById(bossId)?.toDomain()
            ?: return@withTransaction Result.failure(IllegalArgumentException("Boss campaign not found."))
        val player = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
        val (eligible, blockers) = BossBattleEngine.checkEligibility(boss, player)
        if (!eligible) {
            return@withTransaction Result.failure(IllegalStateException("Boss campaign locked: ${blockers.joinToString("; ")}"))
        }
        val active = bossDao.getActiveInstanceForBoss(bossId)
        if (active != null) {
            return@withTransaction Result.failure(IllegalStateException("Boss battle is already active."))
        }
        val alreadyDefeated = bossDao.getAllBossInstancesOnce().any {
            it.bossId == bossId && it.statusName == BossStatus.COMPLETED.name
        }
        if (alreadyDefeated) {
            return@withTransaction Result.failure(IllegalStateException("Boss campaign already conquered."))
        }

        val instId = bossDao.insertBossInstance(
            BossBattleInstanceEntity(
                bossId = bossId,
                statusName = BossStatus.ACTIVE.name,
                startedAtEpochMs = nowEpochMs,
                completedAtEpochMs = null,
                abandonedAtEpochMs = null
            )
        )
        notificationDao.insertNotification(
            SystemNotificationEntity(
                title = "Boss Campaign Engaged: ${boss.title}",
                message = "Complete phase objectives through real physical, academic, or discipline evidence to deplete boss HP.",
                categoryName = NotificationCategory.BOSS_PHASE.name,
                isRead = false,
                timestampEpochMs = nowEpochMs
            )
        )
        Result.success(instId)
    }

    suspend fun refreshAndClaimBossBattle(
        instanceId: Long,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<String> = database.withTransaction {
        val instEntity = bossDao.getBossInstanceById(instanceId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Boss instance not found."))
        if (instEntity.statusName != BossStatus.ACTIVE.name) {
            return@withTransaction Result.failure(IllegalStateException("Boss battle is not active."))
        }
        val boss = bossDao.getBossById(instEntity.bossId)?.toDomain()
            ?: return@withTransaction Result.failure(IllegalArgumentException("Boss definition missing."))
        val phases = bossDao.getPhasesForBossOnce(boss.id).map { it.toDomain() }
        val telemetry = collectTelemetrySnapshotOnce(nowEpochMs)
        val evaluated = BossBattleEngine.evaluateBossWithPhases(boss, phases, instEntity.toDomain(), telemetry)

        if (evaluated.phaseProgress.any { !it.isCompleted }) {
            return@withTransaction Result.success("Boss HP at ${evaluated.healthRemainingPercent}%. Complete remaining phases to conquer ${boss.title}.")
        }

        val rewardKey = "BOSS_CLEAR_${instEntity.id}"
        if (auditDao.getByRewardKey(rewardKey) != null) {
            return@withTransaction Result.failure(IllegalStateException("Boss reward already claimed."))
        }

        val hasVerified = phases.any { it.requiresVerifiedOnly } || telemetry.profile.verifiedActivitiesCount > 0
        val rpgXpToday = auditDao.sumRpgXpSince(todayStartEpochMs())
        val reward = ProgressionEngine.calculateRpgReward(
            baseXp = boss.xpReward,
            primaryAttribute = boss.primaryAttribute,
            primaryAttrXp = boss.primaryAttrXp,
            secondaryAttribute = boss.secondaryAttribute,
            secondaryAttrXp = boss.secondaryAttrXp,
            hasVerifiedEvidence = hasVerified,
            rpgXpAlreadyEarnedToday = rpgXpToday,
            sourceLabel = "Boss Conquered: ${boss.title}"
        )

        bossDao.updateBossInstance(
            instEntity.copy(
                statusName = BossStatus.COMPLETED.name,
                completedAtEpochMs = nowEpochMs
            )
        )

        applyRpgProgressionReward(
            rewardKey = rewardKey,
            sourceType = "BOSS_BATTLE",
            sourceId = boss.code,
            title = boss.title,
            xpAwarded = reward.xpAwarded,
            primaryAttr = reward.primaryAttribute,
            primaryAttrXp = reward.primaryAttrXpAwarded,
            secondaryAttr = reward.secondaryAttribute,
            secondaryAttrXp = reward.secondaryAttrXpAwarded,
            isVerified = hasVerified,
            provenanceLabel = if (hasVerified) "Verified Campaign Evidence" else "Logged Campaign Evidence",
            wasCapped = reward.wasCapped,
            notes = reward.validationNote,
            nowEpochMs = nowEpochMs
        )

        grantConfiguredUnlocks(
            titleId = boss.rewardTitleId,
            itemId = boss.rewardItemId,
            shadowId = boss.rewardShadowId,
            sourceDesc = "Boss Conquered: ${boss.title}",
            nowEpochMs = nowEpochMs
        )

        evaluateAutoUnlocksInternal(nowEpochMs)
        Result.success("Boss Conquered: ${boss.title}! +${reward.xpAwarded} XP awarded.")
    }

    suspend fun abandonBossBattle(
        instanceId: Long,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<String> = database.withTransaction {
        val inst = bossDao.getBossInstanceById(instanceId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Boss instance not found."))
        if (inst.statusName != BossStatus.ACTIVE.name) {
            return@withTransaction Result.failure(IllegalStateException("Boss battle is not active."))
        }
        bossDao.updateBossInstance(
            inst.copy(
                statusName = BossStatus.ABANDONED.name,
                abandonedAtEpochMs = nowEpochMs
            )
        )
        Result.success("Retreated from boss campaign. No penalty applied.")
    }

    suspend fun unlockOrUpgradeSkill(
        skillId: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<String> = database.withTransaction {
        val def = skillDao.getAllDefinitionsOnce().find { it.id == skillId }?.toDomain()
            ?: return@withTransaction Result.failure(IllegalArgumentException("Skill '$skillId' not found."))
        val deps = skillDao.getAllDependenciesOnce().map { it.toDomain() }
        val effects = skillDao.getAllEffectsOnce().map { it.toDomain() }
        val unlocks = skillDao.getAllUnlocksOnce().map { it.toDomain() }.associateBy { it.skillId }
        val player = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()

        val status = SkillEngine.evaluateSkillWithStatus(def, deps, effects, unlocks, player)
        if (!status.canUnlockOrUpgrade) {
            return@withTransaction Result.failure(
                IllegalStateException("Cannot unlock ${def.name}: ${status.missingRequirements.joinToString("; ")}")
            )
        }

        val existing = skillDao.getUnlockBySkillId(skillId)
        val newMastery = if (existing == null) 1 else existing.masteryLevel + 1
        if (existing == null) {
            skillDao.insertSkillUnlock(
                SkillUnlockRecordEntity(
                    skillId = skillId,
                    masteryLevel = 1,
                    unlockedAtEpochMs = nowEpochMs,
                    lastUpgradedAtEpochMs = nowEpochMs,
                    evidenceNote = "Unlocked at Lv.${player.level} (${def.requiredAttribute.code} ${def.requiredAttributeValue}+)"
                )
            )
        } else {
            skillDao.updateSkillUnlock(
                existing.copy(
                    masteryLevel = newMastery,
                    lastUpgradedAtEpochMs = nowEpochMs,
                    evidenceNote = "Upgraded to Mastery Lv.$newMastery at Lv.${player.level}"
                )
            )
        }

        notificationDao.insertNotification(
            SystemNotificationEntity(
                title = if (existing == null) "Skill Unlocked: ${def.name}" else "Skill Upgraded: ${def.name} Lv.$newMastery",
                message = def.description,
                categoryName = NotificationCategory.SKILL_UNLOCKED.name,
                isRead = false,
                timestampEpochMs = nowEpochMs
            )
        )

        evaluateAutoUnlocksInternal(nowEpochMs)
        Result.success("${def.name} ${if (existing == null) "unlocked" else "upgraded to Lv.$newMastery"}!")
    }

    suspend fun equipTitle(titleId: String): Result<String> = database.withTransaction {
        val title = achievementTitleDao.getTitleById(titleId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Title not found."))
        if (!title.isUnlocked) {
            return@withTransaction Result.failure(IllegalStateException("Title '${title.name}' is locked. Earn it through milestones first."))
        }
        achievementTitleDao.clearEquippedTitle()
        achievementTitleDao.equipUnlockedTitle(titleId)

        val player = playerDao.getPlayerOnce()
        if (player != null) {
            playerDao.upsertPlayer(player.copy(title = title.name))
        }
        Result.success("Equipped title '${title.name}'.")
    }

    suspend fun toggleEquipInventoryItem(itemId: String): Result<String> = database.withTransaction {
        val itemDef = inventoryShadowDao.getAllInventoryItemsOnce().find { it.id == itemId }?.toDomain()
            ?: return@withTransaction Result.failure(IllegalArgumentException("Inventory item not found."))
        val record = inventoryShadowDao.getInventoryRecordByItemId(itemId)
            ?: return@withTransaction Result.failure(IllegalStateException("You do not own '${itemDef.name}' yet."))
        if (!itemDef.canEquip) {
            return@withTransaction Result.failure(IllegalStateException("'${itemDef.name}' is a passive collectible."))
        }

        val targetEquip = !record.isEquipped
        // Single-slot exclusivity for frames & themes
        if (targetEquip && (itemDef.category == InventoryCategory.PROFILE_FRAMES || itemDef.category == InventoryCategory.SYSTEM_THEMES)) {
            val allDefs = inventoryShadowDao.getAllInventoryItemsOnce().associateBy { it.id }
            inventoryShadowDao.getAllPlayerInventoryOnce().forEach { rec ->
                val catName = allDefs[rec.itemId]?.categoryName
                if (catName == itemDef.category.name && rec.isEquipped && rec.itemId != itemId) {
                    inventoryShadowDao.updateInventoryRecord(rec.copy(isEquipped = false))
                }
            }
        }
        inventoryShadowDao.updateInventoryRecord(record.copy(isEquipped = targetEquip))
        Result.success(if (targetEquip) "Equipped '${itemDef.name}'." else "Unequipped '${itemDef.name}'.")
    }

    suspend fun approveAiDungeonProposal(
        proposalId: Long,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<Long> = database.withTransaction {
        val proposalEntity = dungeonDao.getDungeonProposalById(proposalId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("AI dungeon proposal not found."))
        val proposal = proposalEntity.toDomain()
        if (proposal.status != ProposalStatus.PENDING) {
            return@withTransaction Result.failure(IllegalStateException("Proposal was already processed."))
        }

        val recovery = database.recoveryAssessmentDao().getLatestOnce()?.toDomain()?.state ?: RecoveryState.READY
        if (proposal.category == com.example.domain.model.DungeonCategory.PHYSICAL &&
            (recovery == RecoveryState.REST_RECOMMENDED || recovery == RecoveryState.RECOVERY_RECOMMENDED)
        ) {
            return@withTransaction Result.failure(IllegalStateException("Blocked by Recovery Safety Guard: current state requires rest instead of physical dungeon exertion."))
        }

        val code = "ai_gate_${proposal.proposalUuid.take(12)}"
        val existing = dungeonDao.getDefinitionByCode(code)
        val dungeonId = if (existing != null) {
            existing.id
        } else {
            val newId = dungeonDao.insertDefinition(
                DungeonDefinition(
                    code = code,
                    title = proposal.title,
                    description = proposal.description,
                    category = proposal.category,
                    difficulty = proposal.difficulty,
                    requiredRank = HunterRank.E_RANK,
                    requiredLevel = 1,
                    timeLimitHours = proposal.timeLimitHours,
                    baseXpReward = proposal.difficulty.baseXp,
                    primaryAttribute = when (proposal.category) {
                        com.example.domain.model.DungeonCategory.PHYSICAL -> AttributeType.VIT
                        com.example.domain.model.DungeonCategory.ACADEMIC -> AttributeType.INT
                        com.example.domain.model.DungeonCategory.RECOVERY_WELLBEING -> AttributeType.WIS
                        else -> AttributeType.DIS
                    },
                    primaryAttrXp = 28,
                    secondaryAttribute = AttributeType.DIS,
                    secondaryAttrXp = 16,
                    isRepeatable = false,
                    creationSource = DungeonCreationSource.AI_APPROVED_PROPOSAL
                ).toEntity()
            )
            dungeonDao.insertObjectives(
                listOf(
                    DungeonObjective(
                        dungeonId = newId,
                        title = proposal.objectiveTitle,
                        evidenceType = proposal.targetEvidenceType,
                        targetValue = proposal.targetValue,
                        requiresVerifiedOnly = proposal.requiresVerifiedOnly,
                        orderIndex = 0
                    ).toEntity(newId)
                )
            )
            newId
        }

        dungeonDao.updateDungeonProposal(proposalEntity.copy(statusName = ProposalStatus.APPROVED.name))
        Result.success(dungeonId)
    }

    suspend fun rejectAiDungeonProposal(proposalId: Long) {
        val entity = dungeonDao.getDungeonProposalById(proposalId) ?: return
        dungeonDao.updateDungeonProposal(entity.copy(statusName = ProposalStatus.REJECTED.name))
    }

    suspend fun evaluateAutoUnlocksNow(nowEpochMs: Long = System.currentTimeMillis()): Int =
        database.withTransaction { evaluateAutoUnlocksInternal(nowEpochMs) }

    private suspend fun evaluateAutoUnlocksInternal(nowEpochMs: Long): Int {
        val telemetry = collectTelemetrySnapshotOnce(nowEpochMs)
        val existingMap = achievementTitleDao.getAllAchievementUnlocksOnce()
            .map { it.toDomain() }
            .associateBy { it.achievementId }
        val newlyEligible = AchievementEngine.findNewlyEligibleAchievements(telemetry, existingMap)
        if (newlyEligible.isEmpty()) return 0

        var unlockedCount = 0
        for ((ach, evidence) in newlyEligible) {
            val insertedId = achievementTitleDao.insertAchievementUnlock(
                AchievementUnlockRecordEntity(
                    achievementId = ach.id,
                    evidenceReference = evidence,
                    unlockedAtEpochMs = nowEpochMs
                )
            )
            if (insertedId <= 0) continue
            unlockedCount += 1

            val rewardKey = "ACH_${ach.id}"
            if (auditDao.getByRewardKey(rewardKey) == null) {
                val rpgXpToday = auditDao.sumRpgXpSince(todayStartEpochMs())
                val reward = ProgressionEngine.calculateRpgReward(
                    baseXp = ach.xpBonus,
                    primaryAttribute = AttributeType.DIS,
                    primaryAttrXp = 12,
                    secondaryAttribute = AttributeType.WIS,
                    secondaryAttrXp = 8,
                    hasVerifiedEvidence = telemetry.profile.verifiedActivitiesCount > 0,
                    rpgXpAlreadyEarnedToday = rpgXpToday,
                    sourceLabel = "Achievement: ${ach.title}"
                )
                applyRpgProgressionReward(
                    rewardKey = rewardKey,
                    sourceType = "ACHIEVEMENT",
                    sourceId = ach.id,
                    title = ach.title,
                    xpAwarded = reward.xpAwarded,
                    primaryAttr = reward.primaryAttribute,
                    primaryAttrXp = reward.primaryAttrXpAwarded,
                    secondaryAttr = reward.secondaryAttribute,
                    secondaryAttrXp = reward.secondaryAttrXpAwarded,
                    isVerified = telemetry.profile.verifiedActivitiesCount > 0,
                    provenanceLabel = evidence,
                    wasCapped = reward.wasCapped,
                    notes = reward.validationNote,
                    nowEpochMs = nowEpochMs
                )
            }

            grantConfiguredUnlocks(
                titleId = ach.rewardTitleId,
                itemId = ach.rewardItemId,
                shadowId = ach.rewardShadowId,
                sourceDesc = "Achievement: ${ach.title}",
                nowEpochMs = nowEpochMs
            )

            notificationDao.insertNotification(
                SystemNotificationEntity(
                    title = "Achievement Unlocked: ${ach.title}",
                    message = "${ach.description} ($evidence)",
                    categoryName = NotificationCategory.ACHIEVEMENT.name,
                    isRead = false,
                    timestampEpochMs = nowEpochMs
                )
            )
        }
        return unlockedCount
    }

    private suspend fun grantConfiguredUnlocks(
        titleId: String?,
        itemId: String?,
        shadowId: String?,
        sourceDesc: String,
        nowEpochMs: Long
    ) {
        if (!titleId.isNullOrBlank()) {
            achievementTitleDao.unlockTitle(titleId, nowEpochMs)
        }
        if (!itemId.isNullOrBlank()) {
            inventoryShadowDao.insertInventoryRecord(
                PlayerInventoryRecordEntity(
                    itemId = itemId,
                    quantity = 1,
                    isEquipped = false,
                    sourceDescription = sourceDesc,
                    acquiredAtEpochMs = nowEpochMs
                )
            )
        }
        if (!shadowId.isNullOrBlank()) {
            inventoryShadowDao.insertShadowRecord(
                ShadowCollectionRecordEntity(
                    collectibleId = shadowId,
                    rankTier = 1,
                    evidenceSource = sourceDesc,
                    unlockedAtEpochMs = nowEpochMs
                )
            )
        }
    }

    private suspend fun applyRpgProgressionReward(
        rewardKey: String,
        sourceType: String,
        sourceId: String,
        title: String,
        xpAwarded: Int,
        primaryAttr: AttributeType,
        primaryAttrXp: Int,
        secondaryAttr: AttributeType?,
        secondaryAttrXp: Int,
        isVerified: Boolean,
        provenanceLabel: String,
        wasCapped: Boolean,
        notes: String,
        nowEpochMs: Long
    ) {
        auditDao.insertAudit(
            RpgRewardAuditEntity(
                rewardKey = rewardKey,
                sourceType = sourceType,
                sourceId = sourceId,
                title = title,
                xpAwarded = xpAwarded,
                primaryAttrName = primaryAttr.name,
                primaryAttrXp = primaryAttrXp,
                secondaryAttrName = secondaryAttr?.name,
                secondaryAttrXp = secondaryAttrXp,
                verificationProvenance = provenanceLabel,
                wasCapped = wasCapped,
                notes = notes,
                timestampEpochMs = nowEpochMs
            )
        )

        val currentProfile = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
        val levelResult = ProgressionEngine.applyXp(
            currentLevel = currentProfile.level,
            currentLevelXp = currentProfile.currentLevelXp,
            totalXp = currentProfile.totalXp,
            gainedXp = xpAwarded
        )
        val deltas = buildMap {
            put(primaryAttr, primaryAttrXp)
            if (secondaryAttr != null && secondaryAttrXp > 0) {
                put(secondaryAttr, (get(secondaryAttr) ?: 0) + secondaryAttrXp)
            }
        }
        val updatedAttrs = ProgressionEngine.applyAttributeXp(
            attributes = currentProfile.attributes,
            deltas = deltas,
            isVerified = isVerified
        )
        val newRank = ProgressionEngine.evaluateRank(
            level = levelResult.newLevel,
            totalAttributeScore = updatedAttrs.totalScore,
            verifiedActivitiesCount = currentProfile.verifiedActivitiesCount
        )

        val updatedProfile = currentProfile.copy(
            level = levelResult.newLevel,
            currentLevelXp = levelResult.newCurrentLevelXp,
            xpToNextLevel = levelResult.newXpToNextLevel,
            totalXp = levelResult.newTotalXp,
            rank = newRank,
            attributes = updatedAttrs
        )
        playerDao.upsertPlayer(updatedProfile.toEntity())

        if (xpAwarded > 0) {
            activityLogDao.insertLog(
                ActivityLogEntity(
                    title = "[$sourceType] $title",
                    categoryName = QuestCategory.DISCIPLINE.name,
                    durationMinutes = 0,
                    verificationStatusName = if (isVerified) VerificationStatus.VERIFIED_SYSTEM_TIMER.name else VerificationStatus.SELF_REPORTED.name,
                    xpAwarded = xpAwarded,
                    primaryAttrName = primaryAttr.name,
                    primaryAttrXpAwarded = primaryAttrXp,
                    notes = notes,
                    timestampEpochMs = nowEpochMs
                )
            )
        }
    }

    private data class DungeonTablesBundle(
        val defs: List<com.example.data.local.entity.DungeonDefinitionEntity>,
        val objs: List<com.example.data.local.entity.DungeonObjectiveEntity>,
        val insts: List<DungeonInstanceEntity>,
        val completions: List<DungeonCompletionRecordEntity>,
        val aiProps: List<com.example.data.local.entity.AiDungeonProposalEntity>
    )

    private data class BossTablesBundle(
        val defs: List<com.example.data.local.entity.BossDefinitionEntity>,
        val phases: List<com.example.data.local.entity.BossBattlePhaseEntity>,
        val insts: List<BossBattleInstanceEntity>
    )

    private data class SkillTablesBundle(
        val defs: List<com.example.data.local.entity.SkillDefinitionEntity>,
        val effects: List<com.example.data.local.entity.SkillEffectConfigEntity>,
        val deps: List<SkillTreeDependencyEntity>,
        val unlocks: List<SkillUnlockRecordEntity>
    )

    private data class MetaTablesBundle(
        val achDefs: List<com.example.data.local.entity.AchievementDefinitionEntity>,
        val achUnlocks: List<AchievementUnlockRecordEntity>,
        val titles: List<com.example.data.local.entity.PlayerTitleEntity>,
        val invItems: List<com.example.data.local.entity.InventoryItemEntity>,
        val invRecs: List<PlayerInventoryRecordEntity>
    )

    private data class PhysicalTelemetryPart(
        val profile: PlayerProfile,
        val recovery: RecoveryState,
        val jogs: List<com.example.domain.model.JoggingSession>,
        val workouts: List<com.example.domain.model.WorkoutSession>
    )

    private data class AcademicTelemetryPart(
        val studies: List<com.example.domain.model.StudySession>,
        val chapters: List<com.example.domain.model.AcademicChapter>,
        val revisions: List<com.example.domain.model.RevisionRecord>,
        val assessments: List<com.example.domain.model.AcademicAssessment>
    )

    private data class LifeTelemetryPart(
        val habits: List<com.example.domain.model.HabitCompletionRecord>,
        val routines: List<com.example.domain.model.RoutineCompletionRecord>,
        val tasks: List<com.example.domain.model.PersonalTask>,
        val milestones: List<com.example.domain.model.GoalMilestone>,
        val completedQuestsCount: Int,
        val activeWorkloadCount: Int
    )
}
