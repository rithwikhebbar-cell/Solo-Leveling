package com.example.data.repository

import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.RecoveryOperationRecordEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.engine.EventDeduplicationManager
import com.example.domain.engine.EventProcessingCoordinator
import com.example.domain.engine.SystemEventBus
import com.example.domain.engine.SystemEventDispatcher
import com.example.domain.engine.SystemEventHandler
import com.example.domain.model.EventDiagnosticsSummary
import com.example.domain.model.EventProcessingStatus
import com.example.domain.model.RecoveryOperationRecord
import com.example.domain.model.SystemEvent
import com.example.domain.model.SystemEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Room-backed repository for unified SystemEvents:
 * - Persists events reliably before dispatch
 * - Deduplicates rapid identical semantic keys
 * - Processes events in timestamp order via SystemEventDispatcher
 * - Escalates repeated failures to DEAD_LETTER without blocking primary user flows
 * - Recovers interrupted PROCESSING events cleanly on startup/crash recovery
 */
class SystemEventRepository(
    private val database: SoloLevelingDatabase
) {
    val dispatcher: SystemEventDispatcher = SystemEventDispatcher()
    private val processMutex = Mutex()

    fun observeRecentEvents(limit: Int = 100): Flow<List<SystemEvent>> =
        database.systemEventDao().observeRecentEvents(limit).map { list -> list.map { it.toDomain() } }

    fun registerHandler(type: SystemEventType, handler: SystemEventHandler) {
        dispatcher.register(type, handler)
    }

    fun registerGlobalHandler(handler: SystemEventHandler) {
        dispatcher.registerAll(handler)
    }

    suspend fun publishEvent(
        eventType: SystemEventType,
        relatedEntityId: String = "",
        sourceModule: String = eventType.defaultModule,
        metadata: Map<String, String> = emptyMap(),
        correlationId: String? = null,
        processImmediately: Boolean = true,
        nowEpochMs: Long = System.currentTimeMillis()
    ): SystemEvent {
        val prefs = database.systemIntegrityHealthDao().getPreferencesOnce()?.toDomain()
        val idempotencyKey = metadata["idempotencyKey"] ?: EventDeduplicationManager.buildIdempotencyKey(
            eventType = eventType,
            sourceModule = sourceModule,
            relatedEntityId = relatedEntityId,
            bucketKey = (nowEpochMs / 10_000L).toString()
        )
        val mergedMeta = metadata.toMutableMap().apply {
            putIfAbsent("idempotencyKey", idempotencyKey)
        }
        val candidate = SystemEvent(
            eventType = eventType,
            timestampEpochMs = nowEpochMs,
            sourceModule = sourceModule,
            relatedEntityId = relatedEntityId,
            metadata = mergedMeta,
            processingStatus = EventProcessingStatus.PENDING,
            correlationId = correlationId ?: relatedEntityId.ifBlank { eventType.name }
        )

        val recent = database.systemEventDao().getRecentEventsOnce(50).map { it.toDomain() }
        val stored = if (EventDeduplicationManager.isDuplicate(candidate, recent)) {
            candidate.copy(
                processingStatus = EventProcessingStatus.DEDUPLICATED,
                processedAtEpochMs = nowEpochMs
            )
        } else {
            candidate
        }

        database.systemEventDao().upsertEvent(stored.toEntity())
        SystemEventBus.emit(stored)

        if (processImmediately &&
            stored.processingStatus == EventProcessingStatus.PENDING &&
            (prefs == null || prefs.enableAutoProgressionEvents)
        ) {
            processPendingQueue()
        }
        return stored
    }

    suspend fun processPendingQueue(nowEpochMs: Long = System.currentTimeMillis()): Int {
        return processMutex.withLock {
            val rawPending = database.systemEventDao().getPendingOrFailedOnce(50).map { it.toDomain() }
            val ordered = EventProcessingCoordinator.orderForProcessing(rawPending)
            var processedCount = 0

            for (event in ordered) {
                if (event.processingStatus == EventProcessingStatus.FAILED &&
                    event.retryCount >= EventProcessingCoordinator.MAX_RETRY_ATTEMPTS
                ) {
                    database.systemEventDao().upsertEvent(
                        event.copy(processingStatus = EventProcessingStatus.DEAD_LETTER).toEntity()
                    )
                    continue
                }

                database.systemEventDao().upsertEvent(
                    event.copy(processingStatus = EventProcessingStatus.PROCESSING).toEntity()
                )

                val dispatchRes = runCatching { dispatcher.dispatch(event) }
                    .getOrElse { Result.failure(it) }

                if (dispatchRes.isSuccess) {
                    database.systemEventDao().upsertEvent(
                        event.copy(
                            processingStatus = EventProcessingStatus.PROCESSED,
                            errorMessage = null,
                            processedAtEpochMs = nowEpochMs
                        ).toEntity()
                    )
                    processedCount++
                } else {
                    val nextStatus = EventProcessingCoordinator.nextStatusAfterFailure(event.retryCount)
                    val errMsg = dispatchRes.exceptionOrNull()?.message ?: "Dispatch handler failure"
                    database.systemEventDao().upsertEvent(
                        event.copy(
                            processingStatus = nextStatus,
                            retryCount = event.retryCount + 1,
                            errorMessage = errMsg.take(240)
                        ).toEntity()
                    )
                }
            }
            processedCount
        }
    }

    suspend fun retryEvent(eventId: String): Boolean {
        val current = database.systemEventDao().getRecentEventsOnce(200)
            .firstOrNull { it.eventId == eventId }?.toDomain() ?: return false
        database.systemEventDao().upsertEvent(
            current.copy(
                processingStatus = EventProcessingStatus.PENDING,
                retryCount = 0,
                errorMessage = null
            ).toEntity()
        )
        processPendingQueue()
        return true
    }

    suspend fun retryAllFailedAndDeadLetter(): Int {
        val recent = database.systemEventDao().getRecentEventsOnce(200).map { it.toDomain() }
        val candidates = recent.filter {
            it.processingStatus == EventProcessingStatus.FAILED ||
                it.processingStatus == EventProcessingStatus.DEAD_LETTER
        }
        if (candidates.isEmpty()) return 0
        candidates.forEach { ev ->
            database.systemEventDao().upsertEvent(
                ev.copy(
                    processingStatus = EventProcessingStatus.PENDING,
                    retryCount = 0,
                    errorMessage = null
                ).toEntity()
            )
        }
        processPendingQueue()
        return candidates.size
    }

    suspend fun recoverInterruptedEvents(nowEpochMs: Long = System.currentTimeMillis()): Int {
        val stuck = database.systemEventDao().getInterruptedProcessingOnce().map { it.toDomain() }
        if (stuck.isEmpty()) return 0
        stuck.forEach { event ->
            database.systemEventDao().upsertEvent(
                event.copy(
                    processingStatus = EventProcessingStatus.PENDING,
                    errorMessage = "Recovered interrupted PROCESSING state on startup"
                ).toEntity()
            )
        }
        database.systemIntegrityHealthDao().upsertRecoveryOperation(
            RecoveryOperationRecord(
                operationType = "EVENT_QUEUE_CRASH_RECOVERY",
                reason = "Detected ${stuck.size} interrupted PROCESSING event(s) from prior run.",
                status = "COMPLETED",
                details = "Re-queued ${stuck.size} interrupted event(s) cleanly without duplicate progression rewards.",
                createdAtEpochMs = nowEpochMs,
                completedAtEpochMs = nowEpochMs
            ).toEntity()
        )
        processPendingQueue(nowEpochMs)
        return stuck.size
    }

    suspend fun getDiagnosticsSummary(): EventDiagnosticsSummary {
        val dao = database.systemEventDao()
        return EventDiagnosticsSummary(
            totalCount = dao.getTotalCount(),
            pendingCount = dao.countByStatus(EventProcessingStatus.PENDING.name) +
                dao.countByStatus(EventProcessingStatus.PROCESSING.name),
            processedCount = dao.countByStatus(EventProcessingStatus.PROCESSED.name),
            failedCount = dao.countByStatus(EventProcessingStatus.FAILED.name),
            deadLetterCount = dao.countByStatus(EventProcessingStatus.DEAD_LETTER.name),
            deduplicatedCount = dao.countByStatus(EventProcessingStatus.DEDUPLICATED.name)
        )
    }

    suspend fun purgeOldCompleted(retentionDays: Int = 30, nowEpochMs: Long = System.currentTimeMillis()): Int {
        val cutoff = nowEpochMs - retentionDays.coerceAtLeast(1) * 24 * 3_600_000L
        return database.systemEventDao().purgeOldCompleted(cutoff)
    }
}
