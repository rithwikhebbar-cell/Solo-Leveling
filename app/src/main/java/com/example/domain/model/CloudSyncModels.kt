package com.example.domain.model

import kotlinx.serialization.Serializable

enum class AuthStatus(val label: String) {
    SIGNED_OUT("Offline Vault Mode"),
    EMAIL_UNVERIFIED("Awaiting Email Verification"),
    SIGNED_IN("Authenticated & Linked"),
    SESSION_EXPIRED("Session Expired (Re-auth Needed)"),
    RECOVERY_PENDING("Password Recovery Active")
}

enum class AccountStatus(val label: String) {
    ACTIVE("Active"),
    PENDING_VERIFICATION("Pending Verification"),
    SUSPENDED("Suspended"),
    DELETED("Deleted")
}

enum class SyncState(val label: String) {
    LOCAL_ONLY("Local Only"),
    PENDING_UPLOAD("Pending Upload"),
    UPLOADING("Uploading"),
    UPLOADED("Uploaded"),
    PENDING_DOWNLOAD("Pending Download"),
    DOWNLOADING("Downloading"),
    SYNCED("Synced"),
    CONFLICT("Conflict Detected"),
    FAILED("Sync Failed")
}

enum class SyncEntityType(val wireTable: String, val label: String) {
    PLAYER_SETTINGS("player_settings", "Player Settings"),
    PHYSICAL_RECORD("physical_records", "Physical Activity"),
    ACADEMIC_SUBJECT("academic_subjects", "Academic Subject"),
    ACADEMIC_RECORD("academic_records", "Academic Record"),
    LIFE_ROUTINE("life_routines", "Life Routine"),
    ROUTINE_COMPLETION("routine_completions", "Routine Completion"),
    HABIT("habits", "Habit"),
    HABIT_COMPLETION("habit_completions", "Habit Completion"),
    PERSONAL_TASK("personal_tasks", "Personal Task"),
    LIFE_GOAL("life_goals", "Life Goal"),
    RPG_PROGRESS("rpg_progress_records", "RPG Progress"),
    PROGRESSION_EVENT("progression_events", "Progression Event"),
    SHADOW_MONARCH_PROFILE("shadow_monarch_profiles", "Shadow Monarch Profile"),
    SHADOW("shadows", "Recruited Shadow"),
    SHADOW_EXTRACTION("shadow_extractions", "Shadow Extraction Event"),
    SHADOW_SKILL_UNLOCK("shadow_skill_unlocks", "Shadow Skill Unlock"),
    SHADOW_DEPLOYMENT("shadow_deployments", "Shadow Deployment"),
    SHADOW_FORMATION("shadow_formations", "Shadow Formation"),
    MONARCH_SKILL_UNLOCK("monarch_skill_unlocks", "Monarch Skill Unlock"),
    SHADOW_QUEST_PROGRESS("shadow_quest_progress", "Shadow Quest Progress"),
    SHADOW_RESOURCE_TRANSACTION("shadow_resource_transactions", "Shadow Resource Ledger"),
    SHADOW_EVIDENCE_CLAIM("shadow_evidence_claims", "Shadow Evidence Claim"),
    SHADOW_RESONANCE_RECORD("shadow_resonance_records", "Shadow Resonance Record")
}

enum class SyncOperationType {
    UPSERT,
    DELETE
}

enum class ConflictStatus {
    OPEN,
    RESOLVED_LOCAL,
    RESOLVED_CLOUD,
    RESOLVED_MERGED
}

enum class ConflictResolutionChoice {
    KEEP_LOCAL,
    KEEP_CLOUD,
    MERGE_SAFE
}

enum class BackupStatus(val label: String) {
    CREATED("Local Snapshot"),
    UPLOADED("Cloud Archived"),
    VERIFIED("Integrity Verified"),
    RESTORED("Restored"),
    FAILED("Failed")
}

data class PlayerIdentity(
    val playerId: String = "",
    val supabaseUserId: String = "",
    val email: String = "",
    val displayName: String = "Shadow Monarch",
    val activeTitle: String = "Awakened E-Rank",
    val hunterRank: HunterRank = HunterRank.E_RANK,
    val level: Int = 1,
    val totalXp: Long = 0L,
    val accountStatus: AccountStatus = AccountStatus.PENDING_VERIFICATION,
    val emailVerified: Boolean = false,
    val authStatus: AuthStatus = AuthStatus.SIGNED_OUT,
    val createdAtEpochMs: Long = 0L,
    val lastSyncAtEpochMs: Long? = null,
    val expiresAtEpochMs: Long? = null,
    val autoSyncEnabled: Boolean = true,
    val cloudSyncConsentGranted: Boolean = false
)

data class DeviceRegistration(
    val deviceUuid: String,
    val userId: String,
    val deviceName: String,
    val manufacturerModel: String,
    val osVersion: String,
    val appVersion: String,
    val lastActiveEpochMs: Long,
    val lastSyncEpochMs: Long?,
    val syncStatus: SyncState,
    val isCurrentDevice: Boolean,
    val isRevoked: Boolean,
    val createdAtEpochMs: Long
)

data class SyncQueueItem(
    val queueId: Long = 0,
    val entityType: SyncEntityType,
    val localKey: String,
    val cloudUuid: String,
    val operation: SyncOperationType,
    val payloadJson: String,
    val idempotencyKey: String,
    val syncState: SyncState,
    val retryCount: Int = 0,
    val nextRetryAtEpochMs: Long = 0L,
    val lastError: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

data class SyncConflict(
    val conflictId: Long = 0,
    val entityType: SyncEntityType,
    val localKey: String,
    val cloudUuid: String,
    val title: String,
    val conflictReason: String,
    val localSummary: String,
    val cloudSummary: String,
    val localPayloadJson: String,
    val cloudPayloadJson: String,
    val localUpdatedAtEpochMs: Long,
    val cloudUpdatedAtEpochMs: Long,
    val resolutionStrategy: String,
    val status: ConflictStatus,
    val resolvedAtEpochMs: Long? = null,
    val createdAtEpochMs: Long
)

data class SyncDiagnosticsLog(
    val operationUuid: String,
    val triggerSource: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val status: SyncState,
    val recordsUploaded: Int = 0,
    val recordsDownloaded: Int = 0,
    val recordsSkipped: Int = 0,
    val conflictsDetected: Int = 0,
    val failedOperations: Int = 0,
    val retryCount: Int = 0,
    val errorCategory: String? = null,
    val safeMessage: String = ""
)

data class CloudBackupRecord(
    val backupUuid: String,
    val userId: String,
    val schemaVersion: Int,
    val progressionChecksum: String,
    val summaryText: String,
    val totalXpSnapshot: Long,
    val playerLevelSnapshot: Int,
    val recordCount: Int,
    val status: BackupStatus,
    val createdAtEpochMs: Long,
    val uploadedAtEpochMs: Long?
)

data class RestorePreview(
    val backup: CloudBackupRecord,
    val currentLevel: Int,
    val currentTotalXp: Long,
    val isChecksumValid: Boolean,
    val isSchemaCompatible: Boolean,
    val isLocalNewerThanBackup: Boolean,
    val warnings: List<String>,
    val recordBreakdown: Map<String, Int>
)

data class SyncSummaryState(
    val isCloudConfigured: Boolean = false,
    val isOnline: Boolean = false,
    val isSyncing: Boolean = false,
    val overallState: SyncState = SyncState.LOCAL_ONLY,
    val pendingUploadsCount: Int = 0,
    val pendingDownloadsCount: Int = 0,
    val openConflictsCount: Int = 0,
    val failedItemsCount: Int = 0,
    val lastSuccessfulSyncEpochMs: Long? = null,
    val lastErrorSummary: String? = null
)

@Serializable
data class CloudTaskPayload(
    val cloudUuid: String,
    val localId: Long = 0,
    val title: String,
    val description: String = "",
    val categoryName: String,
    val priorityName: String,
    val statusName: String,
    val dueDateEpochDay: Long? = null,
    val estimatedMinutes: Int = 30,
    val postponeCount: Int = 0,
    val completedAtEpochMs: Long? = null,
    val deletedAtEpochMs: Long? = null,
    val updatedAtEpochMs: Long = 0L
)

@Serializable
data class CloudSettingsPayload(
    val hudGlowEnabled: Boolean = true,
    val dailyResetHour: Int = 4,
    val healthConnectSyncEnabled: Boolean = false,
    val supabaseCloudSyncEnabled: Boolean = true,
    val academicDailyGoalMinutes: Int = 60,
    val academicWeeklyGoalMinutes: Int = 360,
    val openRouterModelId: String = "google/gemini-2.5-flash",
    val updatedAtEpochMs: Long = 0L
)

@Serializable
data class CloudProgressionEventPayload(
    val idempotencyKey: String,
    val sourceModule: String,
    val sourceRecordId: String,
    val title: String,
    val verificationStatus: String,
    val xpAwarded: Int,
    val primaryAttribute: String,
    val primaryAttrXp: Int,
    val secondaryAttribute: String? = null,
    val secondaryAttrXp: Int = 0,
    val wasCapped: Boolean = false,
    val occurredAtEpochMs: Long
)

data class ValidationCheck(
    val isValid: Boolean,
    val errorMessage: String? = null
)

object AuthValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateEmail(email: String): ValidationCheck {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> ValidationCheck(false, "Email address is required.")
            !EMAIL_REGEX.matches(trimmed) -> ValidationCheck(false, "Enter a valid email address.")
            else -> ValidationCheck(true)
        }
    }

    fun validatePassword(password: String): ValidationCheck {
        return when {
            password.length < 8 -> ValidationCheck(false, "Password must be at least 8 characters.")
            password.length > 72 -> ValidationCheck(false, "Password must be 72 characters or fewer.")
            !password.any { it.isUpperCase() } -> ValidationCheck(false, "Include at least one uppercase letter.")
            !password.any { it.isLowerCase() } -> ValidationCheck(false, "Include at least one lowercase letter.")
            !password.any { it.isDigit() } -> ValidationCheck(false, "Include at least one number.")
            else -> ValidationCheck(true)
        }
    }

    fun validateDisplayName(name: String): ValidationCheck {
        val trimmed = name.trim()
        return when {
            trimmed.length < 2 -> ValidationCheck(false, "Hunter codename must be at least 2 characters.")
            trimmed.length > 32 -> ValidationCheck(false, "Hunter codename must be 32 characters or fewer.")
            else -> ValidationCheck(true)
        }
    }

    fun sanitizeLogMessage(raw: String): String {
        return raw
            .replace(Regex("Bearer\\s+[A-Za-z0-9._-]+", RegexOption.IGNORE_CASE), "Bearer [REDACTED]")
            .replace(Regex("(password|access_token|refresh_token|apikey|service_role)=[^&\\s]+", RegexOption.IGNORE_CASE), "$1=[REDACTED]")
            .take(280)
    }
}
