package com.example.domain.engine

import com.example.domain.model.EventProcessingStatus
import com.example.domain.model.SystemEvent
import com.example.domain.model.SystemEventType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.security.MessageDigest

/**
 * In-memory reactive event stream for non-blocking cross-module notifications.
 * Critical reliability events are persisted via SystemEventRepository before emission.
 */
object SystemEventBus {
    private val _events = MutableSharedFlow<SystemEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SystemEvent> = _events.asSharedFlow()

    fun emit(event: SystemEvent): Boolean = _events.tryEmit(event)
}

/**
 * Deterministic event deduplication & infinite-loop guard.
 */
object EventDeduplicationManager {

    const val MAX_CHAIN_DEPTH = 4
    const val SUPPORTED_MAX_SCHEMA_VERSION = 1

    fun buildIdempotencyKey(
        eventType: SystemEventType,
        sourceModule: String,
        relatedEntityId: String,
        bucketKey: String = ""
    ): String {
        val raw = "${eventType.name}|${sourceModule.uppercase()}|$relatedEntityId|$bucketKey"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }

    fun isDuplicate(
        candidate: SystemEvent,
        recentEvents: List<SystemEvent>,
        dedupWindowMs: Long = 10_000L
    ): Boolean {
        if (recentEvents.any { it.eventId == candidate.eventId }) return true
        val semanticKey = candidate.metadata["idempotencyKey"]
        if (!semanticKey.isNullOrBlank()) {
            return recentEvents.any {
                it.metadata["idempotencyKey"] == semanticKey &&
                    it.processingStatus != EventProcessingStatus.FAILED
            }
        }
        return recentEvents.any { existing ->
            existing.eventType == candidate.eventType &&
                existing.sourceModule == candidate.sourceModule &&
                existing.relatedEntityId.isNotBlank() &&
                existing.relatedEntityId == candidate.relatedEntityId &&
                kotlin.math.abs(existing.timestampEpochMs - candidate.timestampEpochMs) <= dedupWindowMs &&
                existing.processingStatus != EventProcessingStatus.FAILED
        }
    }

    fun wouldExceedLoopDepth(event: SystemEvent): Boolean {
        val depth = event.metadata["chainDepth"]?.toIntOrNull() ?: 0
        return depth >= MAX_CHAIN_DEPTH
    }

    fun isSchemaCompatible(event: SystemEvent): Boolean =
        event.schemaVersion in 1..SUPPORTED_MAX_SCHEMA_VERSION
}

fun interface SystemEventHandler {
    suspend fun handle(event: SystemEvent): Result<Unit>
}

/**
 * Typed dispatcher routing persisted events in timestamp order to registered module handlers.
 */
class SystemEventDispatcher {
    private val handlers = mutableMapOf<SystemEventType, MutableList<SystemEventHandler>>()
    private val globalHandlers = mutableListOf<SystemEventHandler>()

    fun register(type: SystemEventType, handler: SystemEventHandler) {
        handlers.getOrPut(type) { mutableListOf() }.add(handler)
    }

    fun registerAll(handler: SystemEventHandler) {
        globalHandlers.add(handler)
    }

    suspend fun dispatch(event: SystemEvent): Result<Unit> {
        if (!EventDeduplicationManager.isSchemaCompatible(event)) {
            return Result.failure(IllegalArgumentException("Unsupported event schema v${event.schemaVersion}"))
        }
        if (EventDeduplicationManager.wouldExceedLoopDepth(event)) {
            return Result.failure(IllegalStateException("Blocked potential event loop (chainDepth >= ${EventDeduplicationManager.MAX_CHAIN_DEPTH})"))
        }
        val targets = handlers[event.eventType].orEmpty() + globalHandlers
        for (handler in targets) {
            val res = handler.handle(event)
            if (res.isFailure) return res
        }
        return Result.success(Unit)
    }
}

/**
 * Coordinates ordered batch execution, retry limits, and dead-letter escalation.
 */
object EventProcessingCoordinator {

    const val MAX_RETRY_ATTEMPTS = 3

    fun orderForProcessing(events: List<SystemEvent>): List<SystemEvent> =
        events.sortedWith(compareBy<SystemEvent> { it.timestampEpochMs }.thenBy { it.eventId })

    fun nextStatusAfterFailure(currentRetryCount: Int): EventProcessingStatus =
        if (currentRetryCount + 1 >= MAX_RETRY_ATTEMPTS) {
            EventProcessingStatus.DEAD_LETTER
        } else {
            EventProcessingStatus.FAILED
        }

    fun canRecoverInterrupted(event: SystemEvent): Boolean =
        event.processingStatus == EventProcessingStatus.PROCESSING ||
            event.processingStatus == EventProcessingStatus.FAILED
}
