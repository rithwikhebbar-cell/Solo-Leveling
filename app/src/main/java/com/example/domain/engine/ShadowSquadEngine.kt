package com.example.domain.engine

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowTacticalDao
import com.example.data.local.entity.ShadowSquadEntity
import com.example.data.local.entity.ShadowSquadMemberEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowSquad
import com.example.domain.model.ShadowSquadCategory
import com.example.domain.model.ShadowTacticalRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

// ============================================================================
// SHADOW SQUAD ENGINE (Account-Isolated & Transactional)
// Manages creation, updating, membership, leader selection, duplication,
// and activation of tactical squads.
// ============================================================================

class ShadowSquadEngine(
    private val database: SoloLevelingDatabase,
    private val shadowTacticalDao: ShadowTacticalDao,
    private val shadowDao: ShadowDao,
    private val tacticalIntelligenceEngine: ShadowTacticalIntelligenceEngine,
    private val mutex: Mutex = Mutex()
) {

    private suspend fun <T> withLockedTransaction(block: suspend () -> T): T {
        return mutex.withLock {
            database.withTransaction { block() }
        }
    }

    // -------------------------------------------------------------------------
    // 1. CREATE SQUAD
    // -------------------------------------------------------------------------

    suspend fun createSquad(
        name: String,
        category: ShadowSquadCategory,
        memberShadowIds: List<String> = emptyList(),
        leaderShadowId: String? = null,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowSquad> = runCatching {
        val owner = accountOwnerId.trim()
        require(owner.isNotBlank()) { "accountOwnerId must not be blank" }
        require(name.isNotBlank()) { "Squad name must not be blank" }

        withLockedTransaction {
            val validMemberIds = memberShadowIds.distinct().take(5)

            // Validate all members belong to account and are not retired
            for (shadowId in validMemberIds) {
                val member = shadowDao.getShadowByIdForOwner(shadowId, owner)
                    ?: throw IllegalArgumentException("Shadow $shadowId does not belong to account $owner")
                if (member.isRetired) {
                    throw IllegalStateException("Cannot add retired shadow $shadowId to squad")
                }
            }

            // Validate leader
            val effectiveLeaderId = if (leaderShadowId != null && validMemberIds.contains(leaderShadowId)) {
                leaderShadowId
            } else {
                validMemberIds.firstOrNull()
            }

            val now = System.currentTimeMillis()
            val squadId = UUID.randomUUID().toString()

            val squad = ShadowSquad(
                squadId = squadId,
                accountOwnerId = owner,
                name = name.trim().take(50),
                category = category,
                leaderShadowId = effectiveLeaderId,
                memberShadowIds = validMemberIds,
                isActive = false,
                maxMembers = 5,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )

            shadowTacticalDao.insertSquad(squad.toEntity())

            // Insert member records with tactical roles
            validMemberIds.forEachIndexed { index, shadowId ->
                val shadow = shadowDao.getShadowByIdForOwner(shadowId, owner)?.toDomain()
                val profile = shadow?.let { tacticalIntelligenceEngine.evaluateTacticalProfile(it) }
                val role = profile?.primaryRole ?: ShadowTacticalRole.ASSAULT

                shadowTacticalDao.insertMember(
                    ShadowSquadMemberEntity(
                        squadId = squadId,
                        shadowId = shadowId,
                        accountOwnerId = owner,
                        positionIndex = index,
                        assignedTacticalRoleName = role.name
                    )
                )
            }

            squad
        }
    }

    // -------------------------------------------------------------------------
    // 2. UPDATE SQUAD METADATA
    // -------------------------------------------------------------------------

    suspend fun updateSquad(
        squadId: String,
        name: String,
        category: ShadowSquadCategory,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowSquad> = runCatching {
        val owner = accountOwnerId.trim()
        require(name.isNotBlank()) { "Squad name cannot be blank" }

        withLockedTransaction {
            val existing = shadowTacticalDao.getSquadById(squadId, owner)
                ?: throw IllegalArgumentException("Squad not found: $squadId")

            val members = shadowTacticalDao.getMembersForSquadOnce(squadId).map { it.shadowId }

            val updated = existing.copy(
                name = name.trim().take(50),
                categoryName = category.name,
                updatedAtEpochMs = System.currentTimeMillis()
            )
            shadowTacticalDao.updateSquad(updated)
            updated.toDomain(members)
        }
    }

    // -------------------------------------------------------------------------
    // 3. ADD / REMOVE SQUAD MEMBERS
    // -------------------------------------------------------------------------

    suspend fun addMemberToSquad(
        squadId: String,
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowSquad> = runCatching {
        val owner = accountOwnerId.trim()

        withLockedTransaction {
            val squadEntity = shadowTacticalDao.getSquadById(squadId, owner)
                ?: throw IllegalArgumentException("Squad not found: $squadId")

            val currentMembers = shadowTacticalDao.getMembersForSquadOnce(squadId)
            if (currentMembers.size >= squadEntity.maxMembers) {
                throw IllegalStateException("Squad is already full (${squadEntity.maxMembers} members max)")
            }
            if (currentMembers.any { it.shadowId == shadowId }) {
                throw IllegalStateException("Shadow $shadowId is already a member of squad $squadId")
            }

            val shadow = shadowDao.getShadowByIdForOwner(shadowId, owner)?.toDomain()
                ?: throw IllegalArgumentException("Shadow $shadowId does not belong to account $owner")
            if (shadow.isRetired) {
                throw IllegalStateException("Cannot assign retired shadow $shadowId")
            }

            val profile = tacticalIntelligenceEngine.evaluateTacticalProfile(shadow)
            val newPosition = currentMembers.size

            shadowTacticalDao.insertMember(
                ShadowSquadMemberEntity(
                    squadId = squadId,
                    shadowId = shadowId,
                    accountOwnerId = owner,
                    positionIndex = newPosition,
                    assignedTacticalRoleName = profile.primaryRole.name
                )
            )

            val updatedMembers = (currentMembers.map { it.shadowId } + shadowId)
            val leader = if (squadEntity.leaderShadowId == null) shadowId else squadEntity.leaderShadowId

            val updatedSquad = squadEntity.copy(
                leaderShadowId = leader,
                updatedAtEpochMs = System.currentTimeMillis()
            )
            shadowTacticalDao.updateSquad(updatedSquad)

            updatedSquad.toDomain(updatedMembers)
        }
    }

    suspend fun removeMemberFromSquad(
        squadId: String,
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowSquad> = runCatching {
        val owner = accountOwnerId.trim()

        withLockedTransaction {
            val squadEntity = shadowTacticalDao.getSquadById(squadId, owner)
                ?: throw IllegalArgumentException("Squad not found: $squadId")

            shadowTacticalDao.deleteMember(squadId, shadowId)

            val remainingMembers = shadowTacticalDao.getMembersForSquadOnce(squadId).map { it.shadowId }
            val newLeader = if (squadEntity.leaderShadowId == shadowId) {
                remainingMembers.firstOrNull()
            } else {
                squadEntity.leaderShadowId
            }

            val updatedSquad = squadEntity.copy(
                leaderShadowId = newLeader,
                updatedAtEpochMs = System.currentTimeMillis()
            )
            shadowTacticalDao.updateSquad(updatedSquad)

            updatedSquad.toDomain(remainingMembers)
        }
    }

    // -------------------------------------------------------------------------
    // 4. SQUAD LEADER SELECTION
    // -------------------------------------------------------------------------

    suspend fun setSquadLeader(
        squadId: String,
        leaderShadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowSquad> = runCatching {
        val owner = accountOwnerId.trim()

        withLockedTransaction {
            val squadEntity = shadowTacticalDao.getSquadById(squadId, owner)
                ?: throw IllegalArgumentException("Squad not found: $squadId")

            val currentMembers = shadowTacticalDao.getMembersForSquadOnce(squadId).map { it.shadowId }
            if (!currentMembers.contains(leaderShadowId)) {
                throw IllegalStateException("Shadow $leaderShadowId must be an assigned member of squad $squadId to be designated leader")
            }

            val updatedSquad = squadEntity.copy(
                leaderShadowId = leaderShadowId,
                updatedAtEpochMs = System.currentTimeMillis()
            )
            shadowTacticalDao.updateSquad(updatedSquad)

            updatedSquad.toDomain(currentMembers)
        }
    }

    // -------------------------------------------------------------------------
    // 5. ACTIVATE / DEACTIVATE SQUAD
    // -------------------------------------------------------------------------

    suspend fun setActiveSquad(
        squadId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<Unit> = runCatching {
        val owner = accountOwnerId.trim()

        withLockedTransaction {
            val squad = shadowTacticalDao.getSquadById(squadId, owner)
                ?: throw IllegalArgumentException("Squad not found: $squadId")

            shadowTacticalDao.clearActiveSquads(owner)
            shadowTacticalDao.setActiveSquad(squadId, owner, System.currentTimeMillis())
        }
    }

    // -------------------------------------------------------------------------
    // 6. DUPLICATE SQUAD
    // -------------------------------------------------------------------------

    suspend fun duplicateSquad(
        squadId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowSquad> = runCatching {
        val owner = accountOwnerId.trim()

        withLockedTransaction {
            val sourceSquad = shadowTacticalDao.getSquadById(squadId, owner)
                ?: throw IllegalArgumentException("Source squad not found: $squadId")

            val members = shadowTacticalDao.getMembersForSquadOnce(squadId)

            val now = System.currentTimeMillis()
            val newSquadId = UUID.randomUUID().toString()

            val duplicated = ShadowSquadEntity(
                squadId = newSquadId,
                accountOwnerId = owner,
                name = "${sourceSquad.name} (Copy)".take(50),
                categoryName = sourceSquad.categoryName,
                leaderShadowId = sourceSquad.leaderShadowId,
                isActive = false,
                maxMembers = sourceSquad.maxMembers,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            shadowTacticalDao.insertSquad(duplicated)

            members.forEach { m ->
                shadowTacticalDao.insertMember(
                    m.copy(squadId = newSquadId)
                )
            }

            duplicated.toDomain(members.map { it.shadowId })
        }
    }

    // -------------------------------------------------------------------------
    // 7. DELETE SQUAD
    // -------------------------------------------------------------------------

    suspend fun deleteSquad(
        squadId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<Unit> = runCatching {
        val owner = accountOwnerId.trim()

        withLockedTransaction {
            shadowTacticalDao.deleteAllMembersForSquad(squadId)
            shadowTacticalDao.deleteSquad(squadId, owner)
        }
    }
}
