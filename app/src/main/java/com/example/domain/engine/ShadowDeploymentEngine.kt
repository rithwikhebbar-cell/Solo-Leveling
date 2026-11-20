package com.example.domain.engine

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowDeploymentDao
import com.example.data.local.dao.ShadowFormationDao
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowDeploymentRecord
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowFormation
import com.example.domain.model.ShadowFormationType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

// ============================================================================
// SHADOW DEPLOYMENT ENGINE (Account-Isolated & Transactionally Hardened)
// ============================================================================

class ShadowDeploymentEngine(
    private val shadowDao: ShadowDao,
    private val deploymentDao: ShadowDeploymentDao,
    private val formationDao: ShadowFormationDao,
    private val monarchProgressionEngine: MonarchProgressionEngine? = null,
    private val shadowBondEngine: ShadowBondEngine? = null,
    private val database: SoloLevelingDatabase? = null,
    private val mutex: Mutex = Mutex()
) {

    companion object {
        val PREFERRED_CLASSES: Map<ShadowDeploymentCategory, List<ShadowClass>> = mapOf(
            ShadowDeploymentCategory.PHYSICAL_DEVELOPMENT to listOf(ShadowClass.VANGUARD, ShadowClass.GUARDIAN),
            ShadowDeploymentCategory.ACADEMIC_DEVELOPMENT to listOf(ShadowClass.RANGER, ShadowClass.ARCANIST),
            ShadowDeploymentCategory.DISCIPLINE to listOf(ShadowClass.SUPPORT, ShadowClass.GUARDIAN),
            ShadowDeploymentCategory.STRATEGIC_PLANNING to listOf(ShadowClass.TACTICIAN, ShadowClass.ARCANIST),
            ShadowDeploymentCategory.DUNGEON_SUPPORT to listOf(ShadowClass.VANGUARD, ShadowClass.TACTICIAN),
            ShadowDeploymentCategory.RECOVERY_SUPPORT to listOf(ShadowClass.GUARDIAN, ShadowClass.SUPPORT)
        )
    }

    private suspend fun <T> withLockedTransaction(block: suspend () -> T): T {
        return mutex.withLock {
            val db = database
            if (db != null) {
                db.withTransaction { block() }
            } else {
                block()
            }
        }
    }

    // -------------------------------------------------------------------------
    // DEPLOY A SHADOW
    // -------------------------------------------------------------------------

    suspend fun deployShadow(
        shadowId: String,
        category: ShadowDeploymentCategory,
        role: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowDeploymentRecord> {
        return runCatching {
            val owner = accountOwnerId.trim()
            require(owner.isNotBlank()) { "accountOwnerId must not be blank" }
            require(shadowId.isNotBlank()) { "shadowId must not be blank" }

            withLockedTransaction {
                val shadow = shadowDao.getShadowByIdForOwner(shadowId, owner)?.toDomain()
                    ?: throw IllegalArgumentException("Shadow not found for account: $shadowId")

                if (shadow.isRetired) throw IllegalStateException("Cannot deploy retired shadow")

                val existingDeployment = deploymentDao.getActiveDeploymentForShadow(owner, shadowId)
                if (existingDeployment != null) {
                    throw IllegalStateException("Shadow is already deployed to ${existingDeployment.categoryName}. Recall first.")
                }

                val deploymentId = UUID.randomUUID().toString()
                val record = ShadowDeploymentRecord(
                    deploymentId = deploymentId,
                    accountOwnerId = owner,
                    shadowId = shadowId,
                    shadowName = shadow.name,
                    category = category,
                    role = role.take(100),
                    assignedAtEpochMs = System.currentTimeMillis(),
                    recalledAtEpochMs = null,
                    status = ShadowDeploymentStatus.DEPLOYED
                )

                val inserted = deploymentDao.insertDeployment(record.toEntity())
                if (inserted <= 0L) {
                    throw IllegalStateException("Failed to record deployment $deploymentId")
                }
                val updated = shadowDao.updateDeploymentStatusForOwner(shadowId, owner, ShadowDeploymentStatus.DEPLOYED.name)
                if (updated <= 0) {
                    throw IllegalStateException("Shadow ownership check failed during deploy: $shadowId")
                }

                shadowBondEngine?.recordBondActivity(
                    shadowId = shadowId,
                    accountOwnerId = owner,
                    activityType = com.example.domain.model.ShadowBondActivityType.DEPLOYMENT_SUCCESS,
                    sourceEventId = deploymentId
                )

                record
            }
        }
    }

    // -------------------------------------------------------------------------
    // RECALL A SHADOW
    // -------------------------------------------------------------------------

    suspend fun recallShadow(
        deploymentId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<Unit> {
        return runCatching {
            val owner = accountOwnerId.trim()
            require(owner.isNotBlank()) { "accountOwnerId must not be blank" }
            withLockedTransaction {
                val deployment = deploymentDao.getDeploymentByIdForOwner(deploymentId, owner)
                    ?: throw IllegalArgumentException("Deployment not found for account: $deploymentId")

                if (deployment.statusName == ShadowDeploymentStatus.RECALLED.name) {
                    throw IllegalStateException("Shadow already recalled")
                }

                val recalledAtMs = System.currentTimeMillis()
                val updatedRows = deploymentDao.recallDeploymentForOwner(deploymentId, owner, recalledAtMs)
                if (updatedRows <= 0) {
                    throw IllegalStateException("Deployment recall conflicted or unauthorized: $deploymentId")
                }
                shadowDao.updateDeploymentStatusForOwner(
                    deployment.shadowId,
                    owner,
                    ShadowDeploymentStatus.IDLE.name
                )

                shadowBondEngine?.recordBondActivity(
                    shadowId = deployment.shadowId,
                    accountOwnerId = owner,
                    activityType = com.example.domain.model.ShadowBondActivityType.DEPLOYMENT_RECALL,
                    sourceEventId = deploymentId
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // FORMATION MANAGEMENT
    // -------------------------------------------------------------------------

    suspend fun createFormation(
        name: String,
        type: ShadowFormationType,
        description: String,
        memberShadowIds: List<String>,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowFormation> {
        return runCatching {
            val owner = accountOwnerId.trim()
            require(owner.isNotBlank()) { "accountOwnerId must not be blank" }
            require(name.isNotBlank()) { "Formation name must not be blank" }

            withLockedTransaction {
                monarchProgressionEngine?.getOrCreateProfileLocked(owner)

                val requestedMembers = memberShadowIds.distinct().take(type.maxSlots)
                for (shadowId in requestedMembers) {
                    val member = shadowDao.getShadowByIdForOwner(shadowId, owner)
                        ?: throw IllegalArgumentException("Member shadow $shadowId does not belong to account $owner")
                    if (member.isRetired) {
                        throw IllegalStateException("Cannot add retired shadow $shadowId to formation")
                    }
                }

                val now = System.currentTimeMillis()
                val formationId = UUID.randomUUID().toString()
                val formation = ShadowFormation(
                    formationId = formationId,
                    accountOwnerId = owner,
                    name = name.trim().take(50),
                    formationType = type,
                    description = description.take(200),
                    maxSlots = type.maxSlots,
                    memberShadowIds = requestedMembers,
                    isActive = false,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
                val inserted = formationDao.insertFormationIgnore(formation.toEntity())
                if (inserted <= 0L) {
                    throw IllegalStateException("Formation creation conflict for $formationId")
                }
                requestedMembers.forEach { shadowId ->
                    shadowDao.setFormationForOwner(shadowId, owner, formationId)
                }
                formation
            }
        }
    }

    suspend fun updateFormationMembers(
        formationId: String,
        memberShadowIds: List<String>,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowFormation> {
        return runCatching {
            val owner = accountOwnerId.trim()
            require(owner.isNotBlank()) { "accountOwnerId must not be blank" }

            withLockedTransaction {
                val existing = formationDao.getFormationByIdForOwner(formationId, owner)?.toDomain()
                    ?: throw IllegalArgumentException("Formation not found for account: $formationId")

                val validMembers = memberShadowIds.distinct().take(existing.maxSlots)
                for (shadowId in validMembers) {
                    val member = shadowDao.getShadowByIdForOwner(shadowId, owner)
                        ?: throw IllegalArgumentException("Member shadow $shadowId does not belong to account $owner")
                    if (member.isRetired) {
                        throw IllegalStateException("Cannot add retired shadow $shadowId to formation")
                    }
                }

                val updated = existing.copy(
                    accountOwnerId = owner,
                    memberShadowIds = validMembers,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
                val rowsUpdated = formationDao.updateFormation(updated.toEntity())
                if (rowsUpdated <= 0) {
                    throw IllegalStateException("Formation update rejected for $formationId")
                }

                for (shadowId in validMembers) {
                    shadowDao.setFormationForOwner(shadowId, owner, formationId)
                }
                val removed = existing.memberShadowIds - validMembers.toSet()
                for (shadowId in removed) {
                    shadowDao.setFormationForOwner(shadowId, owner, null)
                }

                updated
            }
        }
    }

    suspend fun setActiveFormation(
        formationId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<Unit> {
        return runCatching {
            val owner = accountOwnerId.trim()
            require(owner.isNotBlank()) { "accountOwnerId must not be blank" }

            withLockedTransaction {
                formationDao.getFormationByIdForOwner(formationId, owner)
                    ?: throw IllegalArgumentException("Formation not found for account: $formationId")

                formationDao.clearAllActiveFormationsForOwner(owner)
                val updated = formationDao.setActiveFormationForOwner(formationId, owner, System.currentTimeMillis())
                if (updated <= 0) {
                    throw IllegalStateException("Failed to activate formation $formationId")
                }
            }
        }
    }

    suspend fun deleteFormation(
        formationId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<Unit> {
        return runCatching {
            val owner = accountOwnerId.trim()
            require(owner.isNotBlank()) { "accountOwnerId must not be blank" }

            withLockedTransaction {
                val formation = formationDao.getFormationByIdForOwner(formationId, owner)?.toDomain()
                    ?: throw IllegalArgumentException("Formation not found for account: $formationId")

                for (shadowId in formation.memberShadowIds) {
                    shadowDao.setFormationForOwner(shadowId, owner, null)
                }

                formationDao.deleteFormationForOwner(formationId, owner)
            }
        }
    }

    // -------------------------------------------------------------------------
    // QUERIES
    // -------------------------------------------------------------------------

    suspend fun getActiveDeployments(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): List<ShadowDeploymentRecord> {
        return deploymentDao.getActiveDeploymentsForOwnerOnce(accountOwnerId).map { it.toDomain() }
    }

    fun getCompatibilityNote(shadowClass: ShadowClass, category: ShadowDeploymentCategory): String {
        val preferred = PREFERRED_CLASSES[category] ?: emptyList()
        return if (shadowClass in preferred) {
            "${shadowClass.label} has high compatibility with ${category.label}."
        } else {
            "${shadowClass.label} can support ${category.label} with standard effectiveness."
        }
    }
}
