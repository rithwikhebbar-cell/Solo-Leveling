package com.example.domain.model

// ============================================================================
// MILESTONE 6: DUNGEONS, BOSS BATTLES, SKILLS & RPG SYSTEMS DOMAIN MODELS
// ============================================================================

enum class DungeonCategory(val label: String) {
    PHYSICAL("Physical Dungeon"),
    ACADEMIC("Academic Dungeon"),
    DISCIPLINE("Discipline Dungeon"),
    CONSISTENCY("Consistency Dungeon"),
    RECOVERY_WELLBEING("Recovery & Wellbeing"),
    MULTI_DOMAIN_CHALLENGE("Multi-Domain Challenge")
}

enum class DungeonDifficulty(
    val code: String,
    val displayName: String,
    val minRank: HunterRank,
    val baseXp: Int,
    val rankWeight: Int
) {
    E_RANK("E", "E-Rank Gate", HunterRank.E_RANK, 55, 1),
    D_RANK("D", "D-Rank Gate", HunterRank.D_RANK, 85, 2),
    C_RANK("C", "C-Rank Gate", HunterRank.C_RANK, 120, 3),
    B_RANK("B", "B-Rank Gate", HunterRank.B_RANK, 155, 4),
    A_RANK("A", "A-Rank Gate", HunterRank.A_RANK, 195, 5),
    S_RANK("S", "S-Rank Gate", HunterRank.S_RANK, 240, 6),
    SPECIAL("SP", "Special Event Gate", HunterRank.D_RANK, 110, 3)
}

enum class DungeonStatus(val label: String) {
    LOCKED("Locked"),
    AVAILABLE("Available"),
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Cleared"),
    ABANDONED("Abandoned")
}

enum class DungeonCreationSource(val label: String) {
    SYSTEM("System Gate"),
    DAILY_GENERATED("Daily Gate"),
    WEEKLY_GENERATED("Weekly Gate"),
    AI_APPROVED_PROPOSAL("AI Proposed (Approved)")
}

enum class DungeonEvidenceType(
    val label: String,
    val unit: String,
    val supportsVerifiedOnly: Boolean
) {
    PHYSICAL_JOG_MINUTES("Aerobic / Jogging Minutes", "min", true),
    PHYSICAL_WORKOUT_SESSIONS("Structured Workouts", "sessions", true),
    VERIFIED_PHYSICAL_SESSIONS("Verified Physical Sessions", "sessions", true),
    ACADEMIC_STUDY_MINUTES("Focus & Study Minutes", "min", true),
    ACADEMIC_CHAPTERS_COMPLETED("Syllabus Chapters Cleared", "chapters", false),
    ACADEMIC_REVISIONS_COMPLETED("Spaced Revisions Completed", "revisions", false),
    ACADEMIC_ASSESSMENT_PASS("Assessments Recorded (>=60%)", "assessments", true),
    LIFE_HABIT_COMPLETIONS("Habits Completed", "habits", false),
    LIFE_ROUTINE_COMPLETIONS("Routine Steps Completed", "steps", false),
    LIFE_TASK_COMPLETIONS("Personal Tasks Completed", "tasks", false),
    LIFE_GOAL_MILESTONES("Goal Milestones Cleared", "milestones", false),
    PLAYER_STREAK_DAYS("Active Consistency Streak", "days", false),
    RECOVERY_REST_DAY("Recovery / Protected Rest Actions", "checks", false)
}

data class DungeonObjective(
    val id: Long = 0,
    val dungeonId: Long,
    val title: String,
    val evidenceType: DungeonEvidenceType,
    val targetValue: Int,
    val requiresVerifiedOnly: Boolean = false,
    val orderIndex: Int = 0
)

data class DungeonDefinition(
    val id: Long = 0,
    val code: String,
    val title: String,
    val description: String,
    val category: DungeonCategory,
    val difficulty: DungeonDifficulty,
    val requiredRank: HunterRank = difficulty.minRank,
    val requiredLevel: Int = 1,
    val requiredAttributeCode: String? = null,
    val requiredAttributeValue: Int = 0,
    val timeLimitHours: Int = 24,
    val baseXpReward: Int = difficulty.baseXp,
    val primaryAttribute: AttributeType = AttributeType.DIS,
    val primaryAttrXp: Int = 25,
    val secondaryAttribute: AttributeType = AttributeType.VIT,
    val secondaryAttrXp: Int = 12,
    val rewardTitleId: String? = null,
    val rewardItemId: String? = null,
    val rewardShadowId: String? = null,
    val isRepeatable: Boolean = false,
    val creationSource: DungeonCreationSource = DungeonCreationSource.SYSTEM,
    val isActiveCatalog: Boolean = true
)

data class DungeonInstance(
    val id: Long = 0,
    val dungeonId: Long,
    val status: DungeonStatus = DungeonStatus.AVAILABLE,
    val creationSource: DungeonCreationSource = DungeonCreationSource.SYSTEM,
    val cadenceKey: String = "",
    val startedAtEpochMs: Long = 0L,
    val pausedAtEpochMs: Long? = null,
    val accumulatedPausedMs: Long = 0L,
    val deadlineEpochMs: Long = 0L,
    val completedAtEpochMs: Long? = null,
    val notes: String = ""
)

data class DungeonObjectiveProgress(
    val objective: DungeonObjective,
    val currentValue: Int,
    val targetValue: Int,
    val isSatisfied: Boolean,
    val verificationNote: String
) {
    val progressFraction: Float
        get() = if (targetValue <= 0) 1f else (currentValue.toFloat() / targetValue).coerceIn(0f, 1f)
}

data class DungeonWithStatus(
    val definition: DungeonDefinition,
    val objectives: List<DungeonObjective>,
    val latestInstance: DungeonInstance? = null,
    val isEligibleToEnter: Boolean = true,
    val eligibilityBlockers: List<String> = emptyList(),
    val objectiveProgress: List<DungeonObjectiveProgress> = emptyList(),
    val overallProgressFraction: Float = 0f,
    val completedCount: Int = 0
) {
    val effectiveStatus: DungeonStatus
        get() = when {
            latestInstance != null && (latestInstance.status == DungeonStatus.ACTIVE || latestInstance.status == DungeonStatus.PAUSED) ->
                latestInstance.status
            latestInstance?.status == DungeonStatus.COMPLETED && !definition.isRepeatable ->
                DungeonStatus.COMPLETED
            !isEligibleToEnter -> DungeonStatus.LOCKED
            else -> DungeonStatus.AVAILABLE
        }
}

data class DungeonCompletionRecord(
    val id: Long = 0,
    val rewardKey: String,
    val instanceId: Long,
    val dungeonId: Long,
    val dungeonTitle: String,
    val difficulty: DungeonDifficulty,
    val xpAwarded: Int,
    val primaryAttribute: AttributeType,
    val primaryAttrXp: Int,
    val secondaryAttribute: AttributeType,
    val secondaryAttrXp: Int,
    val evidenceSummary: String,
    val verificationProvenance: String,
    val completedAtEpochMs: Long
)

// ============================================================================
// BOSS BATTLES
// ============================================================================

enum class BossStatus(val label: String) {
    LOCKED("Locked"),
    AVAILABLE("Available"),
    ACTIVE("In Battle"),
    COMPLETED("Conquered"),
    ABANDONED("Retreated")
}

data class BossPhase(
    val id: Long = 0,
    val bossId: Long,
    val phaseNumber: Int,
    val title: String,
    val description: String,
    val evidenceType: DungeonEvidenceType,
    val targetValue: Int,
    val requiresVerifiedOnly: Boolean = false,
    val weight: Int = 100
)

data class BossBattleDefinition(
    val id: Long = 0,
    val code: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val category: DungeonCategory,
    val difficulty: DungeonDifficulty,
    val requiredRank: HunterRank = HunterRank.E_RANK,
    val requiredLevel: Int = 1,
    val xpReward: Int = 140,
    val primaryAttribute: AttributeType = AttributeType.DIS,
    val primaryAttrXp: Int = 45,
    val secondaryAttribute: AttributeType = AttributeType.WIS,
    val secondaryAttrXp: Int = 25,
    val rewardTitleId: String? = null,
    val rewardItemId: String? = null,
    val rewardShadowId: String? = null,
    val totalEstimatedDays: Int = 14
)

data class BossBattleInstance(
    val id: Long = 0,
    val bossId: Long,
    val status: BossStatus = BossStatus.AVAILABLE,
    val startedAtEpochMs: Long = 0L,
    val completedAtEpochMs: Long? = null,
    val abandonedAtEpochMs: Long? = null
)

data class BossPhaseProgress(
    val phase: BossPhase,
    val currentValue: Int,
    val isCompleted: Boolean,
    val verificationNote: String
) {
    val progressFraction: Float
        get() = if (phase.targetValue <= 0) 1f else (currentValue.toFloat() / phase.targetValue).coerceIn(0f, 1f)
}

data class BossBattleWithPhases(
    val definition: BossBattleDefinition,
    val phases: List<BossPhase>,
    val latestInstance: BossBattleInstance? = null,
    val phaseProgress: List<BossPhaseProgress> = emptyList(),
    val isEligible: Boolean = true,
    val eligibilityBlockers: List<String> = emptyList(),
    val healthRemainingFraction: Float = 1f,
    val healthRemainingPercent: Int = 100
) {
    val effectiveStatus: BossStatus
        get() = when {
            latestInstance?.status == BossStatus.ACTIVE -> BossStatus.ACTIVE
            latestInstance?.status == BossStatus.COMPLETED -> BossStatus.COMPLETED
            !isEligible -> BossStatus.LOCKED
            else -> BossStatus.AVAILABLE
        }
}

// ============================================================================
// SKILLS & 5 SKILL TREES
// ============================================================================

enum class SkillSpecialization(val label: String, val primaryAttr: AttributeType, val description: String) {
    PHYSICAL_DEVELOPMENT("Physical Development", AttributeType.STR, "Endurance, strength capacity, agility, and recovery protection."),
    ACADEMIC_EXCELLENCE("Academic Excellence", AttributeType.INT, "Deep focus presets, accelerated learning, retention, and exam readiness."),
    ABSOLUTE_DISCIPLINE("Absolute Discipline", AttributeType.DIS, "Consistency resilience, routine mastery, time planning, and goal finish."),
    STRATEGIC_WISDOM("Strategic Wisdom", AttributeType.WIS, "Structured reflection, strategic planning, and blind-spot awareness."),
    SOCIAL_MASTERY("Social Mastery", AttributeType.CHA, "Clear communication, confidence, leadership, and team collaboration.")
}

enum class SkillTier(val label: String, val sortOrder: Int) {
    COMMON("Common", 1),
    UNCOMMON("Uncommon", 2),
    RARE("Rare", 3),
    EPIC("Epic", 4),
    LEGENDARY("Legendary", 5),
    MYTHIC("Mythic", 6)
}

data class SkillDefinition(
    val id: String,
    val name: String,
    val specialization: SkillSpecialization,
    val tier: SkillTier,
    val description: String,
    val requiredRank: HunterRank,
    val requiredLevel: Int,
    val requiredAttribute: AttributeType,
    val requiredAttributeValue: Int,
    val requiredVerifiedCount: Int = 0,
    val requiredStreakDays: Int = 0,
    val maxMasteryLevel: Int = 5,
    val treeColumn: Int = 0,
    val treeRow: Int = 0
)

data class SkillTreeDependency(
    val skillId: String,
    val prerequisiteSkillId: String,
    val minPrerequisiteMastery: Int = 1
)

data class SkillEffectSpec(
    val skillId: String,
    val effectCode: String,
    val title: String,
    val description: String,
    val featureTag: String
)

data class SkillUnlockRecord(
    val id: Long = 0,
    val skillId: String,
    val masteryLevel: Int = 1,
    val unlockedAtEpochMs: Long,
    val lastUpgradedAtEpochMs: Long,
    val evidenceNote: String = ""
)

data class SkillWithStatus(
    val definition: SkillDefinition,
    val effects: List<SkillEffectSpec>,
    val prerequisites: List<SkillTreeDependency>,
    val unlockRecord: SkillUnlockRecord? = null,
    val isUnlocked: Boolean = false,
    val currentMastery: Int = 0,
    val canUnlockOrUpgrade: Boolean = false,
    val missingRequirements: List<String> = emptyList()
)

data class ActiveSkillEffects(
    val unlockedSkillCount: Int = 0,
    val unlockedFocusPresetsMinutes: List<Int> = listOf(25, 45),
    val unlockedChallengeTiers: List<String> = listOf("Standard"),
    val unlockedRoutineAnalytics: Boolean = false,
    val unlockedRevisionPlanningOptions: Boolean = false,
    val unlockedGoalPlanningTools: Boolean = false,
    val activeEffectExplanations: List<String> = emptyList(),
    val safetyGuaranteeNote: String = "Skills enhance focus presets, planning views, and eligible dungeon challenges without altering health metrics, bypassing recovery safety rules, or granting infinite XP."
)

// ============================================================================
// ACHIEVEMENTS, TITLES, INVENTORY & SHADOW ARMY COLLECTION
// ============================================================================

enum class RpgRarity(val label: String, val order: Int) {
    COMMON("Common", 1),
    UNCOMMON("Uncommon", 2),
    RARE("Rare", 3),
    EPIC("Epic", 4),
    LEGENDARY("Legendary", 5),
    MYTHIC("Mythic", 6)
}

enum class AchievementCategory(val label: String) {
    PHYSICAL("Physical Achievements"),
    ACADEMIC("Academic Achievements"),
    DISCIPLINE("Discipline Achievements"),
    CONSISTENCY("Consistency Achievements"),
    QUESTS("Quest Achievements"),
    DUNGEONS("Dungeon Achievements"),
    BOSS_BATTLES("Boss Battle Achievements"),
    LEVELS_AND_RANKS("Level & Rank Achievements"),
    PERSONAL_MILESTONES("Personal Milestones"),
    SPECIAL_CHALLENGES("Special Challenges")
}

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val rarity: RpgRarity,
    val criterionDescription: String,
    val targetValue: Int,
    val xpBonus: Int = 30,
    val rewardTitleId: String? = null,
    val rewardItemId: String? = null,
    val rewardShadowId: String? = null
)

data class AchievementUnlockRecord(
    val id: Long = 0,
    val achievementId: String,
    val evidenceReference: String,
    val unlockedAtEpochMs: Long
)

data class AchievementWithStatus(
    val definition: AchievementDefinition,
    val currentValue: Int,
    val isUnlocked: Boolean,
    val unlockRecord: AchievementUnlockRecord? = null
) {
    val progressFraction: Float
        get() = if (definition.targetValue <= 0) 1f else (currentValue.toFloat() / definition.targetValue).coerceIn(0f, 1f)
}

data class PlayerTitleDefinition(
    val id: String,
    val name: String,
    val description: String,
    val rarity: RpgRarity,
    val unlockRequirement: String,
    val isUnlocked: Boolean = false,
    val isEquipped: Boolean = false,
    val unlockedAtEpochMs: Long? = null
)

enum class InventoryCategory(val label: String) {
    COSMETIC_ITEMS("Cosmetic Items"),
    PROFILE_FRAMES("Profile Frames"),
    TITLES("Titles"),
    SYSTEM_THEMES("System Themes"),
    PLAYER_BADGES("Player Badges"),
    COLLECTIBLES("Collectibles"),
    SPECIAL_REWARDS("Special Rewards")
}

data class InventoryItemDefinition(
    val id: String,
    val name: String,
    val category: InventoryCategory,
    val rarity: RpgRarity,
    val description: String,
    val acquisitionSource: String,
    val canEquip: Boolean = true
)

data class PlayerInventoryRecord(
    val id: Long = 0,
    val itemId: String,
    val quantity: Int = 1,
    val isEquipped: Boolean = false,
    val sourceDescription: String = "",
    val acquiredAtEpochMs: Long
)

data class InventoryItemWithOwnership(
    val definition: InventoryItemDefinition,
    val record: PlayerInventoryRecord? = null
) {
    val isOwned: Boolean get() = record != null && record.quantity > 0
    val isEquipped: Boolean get() = record?.isEquipped == true
}

enum class ShadowCollectionCategory(val label: String) {
    SHADOW_COLLECTIBLES("Shadow Collectibles"),
    ACHIEVEMENT_COMPANIONS("Achievement Companions"),
    MILESTONE_GUARDIANS("Milestone Guardians"),
    SPECIAL_EVENT_COLLECTIBLES("Special Event Collectibles")
}

data class ShadowCollectibleDefinition(
    val id: String,
    val codename: String,
    val category: ShadowCollectionCategory,
    val rarity: RpgRarity,
    val visualMotif: String,
    val loreDescription: String,
    val unlockSource: String
)

data class ShadowCollectionRecord(
    val id: Long = 0,
    val collectibleId: String,
    val rankTier: Int = 1,
    val evidenceSource: String,
    val unlockedAtEpochMs: Long
)

data class ShadowCollectibleWithStatus(
    val definition: ShadowCollectibleDefinition,
    val record: ShadowCollectionRecord? = null
) {
    val isUnlocked: Boolean get() = record != null
}

// ============================================================================
// REWARD AUDIT & AI INTEGRATION SUMMARY
// ============================================================================

data class RpgRewardAuditRecord(
    val id: Long = 0,
    val rewardKey: String,
    val sourceType: String,
    val sourceId: String,
    val title: String,
    val xpAwarded: Int,
    val primaryAttribute: AttributeType,
    val primaryAttrXp: Int,
    val secondaryAttribute: AttributeType?,
    val secondaryAttrXp: Int,
    val verificationProvenance: String,
    val wasCapped: Boolean = false,
    val notes: String = "",
    val timestampEpochMs: Long = System.currentTimeMillis()
)

data class RpgContextSummary(
    val activeDungeonsCount: Int = 0,
    val activeDungeonTitles: List<String> = emptyList(),
    val clearedDungeonsCount: Int = 0,
    val activeBossTitle: String? = null,
    val activeBossHpPercent: Int? = null,
    val unlockedSkillsCount: Int = 0,
    val totalSkillsCount: Int = 19,
    val unlockedAchievementsCount: Int = 0,
    val equippedTitle: String = "System Initiate",
    val shadowCollectionCount: Int = 0
)

data class AiDungeonProposal(
    val id: Long = 0,
    val proposalUuid: String,
    val title: String,
    val description: String,
    val category: DungeonCategory,
    val difficulty: DungeonDifficulty,
    val objectiveTitle: String,
    val targetEvidenceType: DungeonEvidenceType,
    val targetValue: Int,
    val timeLimitHours: Int = 24,
    val requiresVerifiedOnly: Boolean = false,
    val reason: String,
    val isBossCampaign: Boolean = false,
    val status: ProposalStatus = ProposalStatus.PENDING,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
