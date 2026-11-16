package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.engine.DailyExecutionOverride
import com.example.domain.model.AutomationExecutionRecord
import com.example.domain.model.AutomationExecutionStatus
import com.example.domain.model.AutomationJobSpec
import com.example.domain.model.AutomationTriggerType
import com.example.domain.model.DailyCycleExecution
import com.example.domain.model.DailyExecutionStatus
import com.example.domain.model.EventProcessingStatus
import com.example.domain.model.IntegrityFindingCategory
import com.example.domain.model.IntegrityFindingStatus
import com.example.domain.model.IntegritySeverity
import com.example.domain.model.RecoveryOperationRecord
import com.example.domain.model.SystemControlPreferences
import com.example.domain.model.SystemEvent
import com.example.domain.model.SystemEventType
import com.example.domain.model.SystemIntegrityFinding
import org.json.JSONObject
import java.time.ZoneId

@Entity(
    tableName = "system_events",
    indices = [
        Index("processingStatusName"),
        Index("timestampEpochMs"),
        Index("eventTypeName")
    ]
)
data class SystemEventEntity(
    @PrimaryKey val eventId: String,
    val eventTypeName: String,
    val timestampEpochMs: Long,
    val sourceModule: String,
    val relatedEntityId: String,
    val metadataJson: String,
    val schemaVersion: Int,
    val processingStatusName: String,
    val retryCount: Int,
    val errorMessage: String?,
    val processedAtEpochMs: Long?,
    val correlationId: String
)

@Entity(tableName = "automation_jobs")
data class AutomationJobEntity(
    @PrimaryKey val automationId: String,
    val name: String,
    val description: String,
    val triggerTypeName: String,
    val frequencyLabel: String,
    val constraintsSummary: String,
    val isEnabled: Boolean,
    val lastExecutionEpochMs: Long?,
    val nextEligibleEpochMs: Long?,
    val currentStatusName: String,
    val retryCount: Int,
    val lastFailureReason: String?,
    val executionCount: Int
)

@Entity(
    tableName = "automation_execution_logs",
    indices = [
        Index("automationId"),
        Index("startedAtEpochMs")
    ]
)
data class AutomationExecutionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val executionUuid: String,
    val automationId: String,
    val triggerSource: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val statusName: String,
    val summaryMessage: String,
    val retryCount: Int,
    val failureReason: String?
)

@Entity(tableName = "daily_cycle_executions")
data class DailyCycleExecutionEntity(
    @PrimaryKey val epochDay: Long,
    val timezoneId: String,
    val executedAtEpochMs: Long,
    val triggerSource: String,
    val dailyQuestsResetCount: Int,
    val missedDaysCaughtUp: Int,
    val status: String,
    val notes: String
)

@Entity(
    tableName = "daily_execution_items",
    indices = [Index("dateEpochDay")]
)
data class DailyExecutionItemEntity(
    @PrimaryKey val executionKey: String,
    val dateEpochDay: Long,
    val activityTypeName: String,
    val sourceRecordId: Long,
    val statusName: String,
    val actualStartEpochMs: Long?,
    val actualCompletionEpochMs: Long?,
    val completedMinutes: Int,
    val plannedStartMinuteOverride: Int?,
    val evidenceReference: String,
    val notes: String,
    val updatedAtEpochMs: Long
)

@Entity(
    tableName = "system_integrity_findings",
    indices = [Index("statusName")]
)
data class SystemIntegrityFindingEntity(
    @PrimaryKey val findingUuid: String,
    val categoryName: String,
    val severityName: String,
    val title: String,
    val description: String,
    val affectedTable: String,
    val affectedRecordKey: String,
    val repairProposal: String,
    val isDestructiveRepair: Boolean,
    val statusName: String,
    val detectedAtEpochMs: Long,
    val resolvedAtEpochMs: Long?
)

@Entity(
    tableName = "system_health_snapshots",
    indices = [Index("evaluatedAtEpochMs")]
)
data class SystemHealthSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val overallStatusName: String,
    val componentsSummaryJson: String,
    val evaluatedAtEpochMs: Long
)

@Entity(
    tableName = "recovery_operation_records",
    indices = [Index("createdAtEpochMs")]
)
data class RecoveryOperationRecordEntity(
    @PrimaryKey val operationUuid: String,
    val operationType: String,
    val reason: String,
    val status: String,
    val details: String,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long?
)

@Entity(tableName = "system_preferences")
data class SystemPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val enableAutoProgressionEvents: Boolean = true,
    val enableAutoIntelligenceRefresh: Boolean = true,
    val enableDailyCycleAutomation: Boolean = true,
    val enableCloudAutoSync: Boolean = true,
    val enablePerformanceCaching: Boolean = true,
    val enableBackgroundNotifications: Boolean = true,
    val preferredTimezoneId: String = ZoneId.systemDefault().id,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

private fun encodeMap(map: Map<String, String>): String {
    val obj = JSONObject()
    map.forEach { (k, v) -> obj.put(k, v) }
    return obj.toString()
}

private fun decodeMap(json: String): Map<String, String> {
    if (json.isBlank()) return emptyMap()
    return runCatching {
        val obj = JSONObject(json)
        buildMap {
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, obj.optString(key, ""))
            }
        }
    }.getOrDefault(emptyMap())
}

fun SystemEventEntity.toDomain(): SystemEvent = SystemEvent(
    eventId = eventId,
    eventType = runCatching { SystemEventType.valueOf(eventTypeName) }.getOrDefault(SystemEventType.PLAYER_PROFILE_UPDATED),
    timestampEpochMs = timestampEpochMs,
    sourceModule = sourceModule,
    relatedEntityId = relatedEntityId,
    metadata = decodeMap(metadataJson),
    schemaVersion = schemaVersion,
    processingStatus = runCatching { EventProcessingStatus.valueOf(processingStatusName) }.getOrDefault(EventProcessingStatus.PENDING),
    retryCount = retryCount,
    errorMessage = errorMessage,
    processedAtEpochMs = processedAtEpochMs,
    correlationId = correlationId
)

fun SystemEvent.toEntity(): SystemEventEntity = SystemEventEntity(
    eventId = eventId,
    eventTypeName = eventType.name,
    timestampEpochMs = timestampEpochMs,
    sourceModule = sourceModule,
    relatedEntityId = relatedEntityId,
    metadataJson = encodeMap(metadata),
    schemaVersion = schemaVersion,
    processingStatusName = processingStatus.name,
    retryCount = retryCount,
    errorMessage = errorMessage,
    processedAtEpochMs = processedAtEpochMs,
    correlationId = correlationId
)

fun AutomationJobEntity.toDomain(): AutomationJobSpec = AutomationJobSpec(
    automationId = automationId,
    name = name,
    description = description,
    triggerType = runCatching { AutomationTriggerType.valueOf(triggerTypeName) }.getOrDefault(AutomationTriggerType.PERIODIC_WORKMANAGER),
    frequencyLabel = frequencyLabel,
    constraintsSummary = constraintsSummary,
    isEnabled = isEnabled,
    lastExecutionEpochMs = lastExecutionEpochMs,
    nextEligibleEpochMs = nextEligibleEpochMs,
    currentStatus = runCatching { AutomationExecutionStatus.valueOf(currentStatusName) }.getOrDefault(AutomationExecutionStatus.IDLE),
    retryCount = retryCount,
    lastFailureReason = lastFailureReason,
    executionCount = executionCount
)

fun AutomationJobSpec.toEntity(): AutomationJobEntity = AutomationJobEntity(
    automationId = automationId,
    name = name,
    description = description,
    triggerTypeName = triggerType.name,
    frequencyLabel = frequencyLabel,
    constraintsSummary = constraintsSummary,
    isEnabled = isEnabled,
    lastExecutionEpochMs = lastExecutionEpochMs,
    nextEligibleEpochMs = nextEligibleEpochMs,
    currentStatusName = currentStatus.name,
    retryCount = retryCount,
    lastFailureReason = lastFailureReason,
    executionCount = executionCount
)

fun AutomationExecutionLogEntity.toDomain(): AutomationExecutionRecord = AutomationExecutionRecord(
    id = id,
    executionUuid = executionUuid,
    automationId = automationId,
    triggerSource = triggerSource,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    status = runCatching { AutomationExecutionStatus.valueOf(statusName) }.getOrDefault(AutomationExecutionStatus.SUCCEEDED),
    summaryMessage = summaryMessage,
    retryCount = retryCount,
    failureReason = failureReason
)

fun AutomationExecutionRecord.toEntity(): AutomationExecutionLogEntity = AutomationExecutionLogEntity(
    id = id,
    executionUuid = executionUuid,
    automationId = automationId,
    triggerSource = triggerSource,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    statusName = status.name,
    summaryMessage = summaryMessage,
    retryCount = retryCount,
    failureReason = failureReason
)

fun DailyCycleExecutionEntity.toDomain(): DailyCycleExecution = DailyCycleExecution(
    epochDay = epochDay,
    timezoneId = timezoneId,
    executedAtEpochMs = executedAtEpochMs,
    triggerSource = triggerSource,
    dailyQuestsResetCount = dailyQuestsResetCount,
    missedDaysCaughtUp = missedDaysCaughtUp,
    status = status,
    notes = notes
)

fun DailyCycleExecution.toEntity(): DailyCycleExecutionEntity = DailyCycleExecutionEntity(
    epochDay = epochDay,
    timezoneId = timezoneId,
    executedAtEpochMs = executedAtEpochMs,
    triggerSource = triggerSource,
    dailyQuestsResetCount = dailyQuestsResetCount,
    missedDaysCaughtUp = missedDaysCaughtUp,
    status = status,
    notes = notes
)

fun DailyExecutionItemEntity.toOverride(): DailyExecutionOverride = DailyExecutionOverride(
    executionKey = executionKey,
    status = runCatching { DailyExecutionStatus.valueOf(statusName) }.getOrDefault(DailyExecutionStatus.SCHEDULED),
    actualStartEpochMs = actualStartEpochMs,
    actualCompletionEpochMs = actualCompletionEpochMs,
    completedMinutes = completedMinutes,
    plannedStartMinuteOverride = plannedStartMinuteOverride,
    evidenceReference = evidenceReference,
    notes = notes
)

fun SystemIntegrityFindingEntity.toDomain(): SystemIntegrityFinding = SystemIntegrityFinding(
    findingUuid = findingUuid,
    category = runCatching { IntegrityFindingCategory.valueOf(categoryName) }.getOrDefault(IntegrityFindingCategory.PLAYER_PROGRESSION),
    severity = runCatching { IntegritySeverity.valueOf(severityName) }.getOrDefault(IntegritySeverity.INFO),
    title = title,
    description = description,
    affectedTable = affectedTable,
    affectedRecordKey = affectedRecordKey,
    repairProposal = repairProposal,
    isDestructiveRepair = isDestructiveRepair,
    status = runCatching { IntegrityFindingStatus.valueOf(statusName) }.getOrDefault(IntegrityFindingStatus.OPEN),
    detectedAtEpochMs = detectedAtEpochMs,
    resolvedAtEpochMs = resolvedAtEpochMs
)

fun SystemIntegrityFinding.toEntity(): SystemIntegrityFindingEntity = SystemIntegrityFindingEntity(
    findingUuid = findingUuid,
    categoryName = category.name,
    severityName = severity.name,
    title = title,
    description = description,
    affectedTable = affectedTable,
    affectedRecordKey = affectedRecordKey,
    repairProposal = repairProposal,
    isDestructiveRepair = isDestructiveRepair,
    statusName = status.name,
    detectedAtEpochMs = detectedAtEpochMs,
    resolvedAtEpochMs = resolvedAtEpochMs
)

fun RecoveryOperationRecordEntity.toDomain(): RecoveryOperationRecord = RecoveryOperationRecord(
    operationUuid = operationUuid,
    operationType = operationType,
    reason = reason,
    status = status,
    details = details,
    createdAtEpochMs = createdAtEpochMs,
    completedAtEpochMs = completedAtEpochMs
)

fun RecoveryOperationRecord.toEntity(): RecoveryOperationRecordEntity = RecoveryOperationRecordEntity(
    operationUuid = operationUuid,
    operationType = operationType,
    reason = reason,
    status = status,
    details = details,
    createdAtEpochMs = createdAtEpochMs,
    completedAtEpochMs = completedAtEpochMs
)

fun SystemPreferencesEntity.toDomain(): SystemControlPreferences = SystemControlPreferences(
    enableAutoProgressionEvents = enableAutoProgressionEvents,
    enableAutoIntelligenceRefresh = enableAutoIntelligenceRefresh,
    enableDailyCycleAutomation = enableDailyCycleAutomation,
    enableCloudAutoSync = enableCloudAutoSync,
    enablePerformanceCaching = enablePerformanceCaching,
    enableBackgroundNotifications = enableBackgroundNotifications,
    preferredTimezoneId = preferredTimezoneId,
    updatedAtEpochMs = updatedAtEpochMs
)

fun SystemControlPreferences.toEntity(): SystemPreferencesEntity = SystemPreferencesEntity(
    id = 1,
    enableAutoProgressionEvents = enableAutoProgressionEvents,
    enableAutoIntelligenceRefresh = enableAutoIntelligenceRefresh,
    enableDailyCycleAutomation = enableDailyCycleAutomation,
    enableCloudAutoSync = enableCloudAutoSync,
    enablePerformanceCaching = enablePerformanceCaching,
    enableBackgroundNotifications = enableBackgroundNotifications,
    preferredTimezoneId = preferredTimezoneId,
    updatedAtEpochMs = updatedAtEpochMs
)
