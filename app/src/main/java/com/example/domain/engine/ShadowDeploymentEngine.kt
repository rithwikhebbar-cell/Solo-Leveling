package com.example.domain.engine

import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowDeploymentDao
import com.example.data.local.dao.ShadowFormationDao
import com.example.data.local.entity.ShadowDeploymentEntity
import com.example.data.local.entity.ShadowFormationEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.local.entity.toEntity
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowDeploymentRecord
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowFormation
import com.example.domain.model.ShadowFormationType
import java.util.UUID

// ============================================================================
// SHADOW DEPLOYMENT ENGINE
//
// SAFETY GUARANTEES:
//  - Shadows cannot perform real-world activities on behalf of the player.
//  - Deployment to Physical Development cannot override recovery restrictions.
//  - Deployment to Academic Development cannot complete study sessions.
//  - Deployment to Recovery Support cannot override REST_RECOMMENDED status.
//  - All deployments are logged with timestamps and are auditable.
//  - Recalls are persistent and tracked.
// ============================================================================

class ShadowDeploymentEngine(
    private val shadowDao: ShadowDao,
    private val deploymentDao: ShadowDeploymentDao,
    private val formationDao: ShadowFormationDao
) {

    companion object {
        // Class compatibility per deployment category
        // Returns preferred classes (not exclusive — any class can deploy, these are primary fits)
        val PREFERRED_CLASSES: Map<ShadowDeploymentCategory, List<ShadowClass>> = mapOf(
            ShadowDeploymentCategory.PHYSICAL_DEVELOPMENT to listOf(ShadowClass.VANGUARD, ShadowClass.GUARDIAN),
            ShadowDeploymentCategory.ACADEMIC_DEVELOPMENT to listOf(ShadowClass.RANGER, ShadowClass.ARCANIST),
            ShadowDeploymentCategory.DISCIPLINE to listOf(ShadowClass.SUPPORT, ShadowClass.GUARDIAN),
            ShadowDeploymentCategory.STRATEGIC_PLANNING to listOf(ShadowClass.TACTICIAN, ShadowClass.ARCANIST),
            ShadowDeploymentCategory.DUNGEON_SUPPORT to listOf(ShadowClass.VANGUARD, ShadowClass.TACTICIAN),
            ShadowDeploymentCategory.RECOVERY_SUPPORT to listOf(ShadowClass.GUARDIAN, ShadowClass.SUPPORT)
        )
    }

    // -------------------------------------------------------------------------
    // DEPLOY A SHADOW
    // -------------------------------------------------------------------------

    /**
     * Deploys a shadow to a strategic support role.
     * Returns the deployment record on success, or a failure with reason.
     *
     * SAFETY: This function only records strategic support assignment.
     * It cannot complete workouts, study sessions, or override recovery.
     */
    suspend fun deployShadow(
        shadowId: String,
        category: ShadowDeploymentCategory,
        role: String
    ): Result<ShadowDeploymentRecord> {
        return runCatching {
            val shadow = shadowDao.getShadowById(shadowId)?.toDomain()
                ?: throw IllegalArgumentException("Shadow not found: $shadowId")

            if (shadow.isRetired) throw IllegalStateException("Cannot deploy retired shadow")

            // Check if shadow is already deployed
            val existingDeployment = deploymentDao.getActiveDeploymentForShadow(shadowId)
            if (existingDeployment != null) {
                throw IllegalStateException("Shadow is already deployed to ${existingDeployment.categoryName}. Recall first.")
            }

            val deploymentId = UUID.randomUUID().toString()
            val record = ShadowDeploymentRecord(
                deploymentId = deploymentId,
                shadowId = shadowId,
                shadowName = shadow.name,
                category = category,
                role = role.take(100),
                assignedAtEpochMs = System.currentTimeMillis(),
                recalledAtEpochMs = null,
                status = ShadowDeploymentStatus.DEPLOYED
            )

            deploymentDao.insertDeployment(record.toEntity())
            shadowDao.updateDeploymentStatus(shadowId, ShadowDeploymentStatus.DEPLOYED.name)

            record
        }
    }

    // -------------------------------------------------------------------------
    // RECALL A SHADOW
    // -------------------------------------------------------------------------

    suspend fun recallShadow(deploymentId: String): Result<Unit> {
        return runCatching {
            val deployment = deploymentDao.getDeploymentById(deploymentId)
                ?: throw IllegalArgumentException("Deployment not found: $deploymentId")

            if (deployment.statusName == ShadowDeploymentStatus.RECALLED.name) {
                throw IllegalStateException("Shadow already recalled")
            }

            val recalledAtMs = System.currentTimeMillis()
            deploymentDao.recallDeployment(deploymentId, recalledAtMs)
            shadowDao.updateDeploymentStatus(deployment.shadowId, ShadowDeploymentStatus.IDLE.name)
        }
    }

    // -------------------------------------------------------------------------
    // FORMATION MANAGEMENT
    // -------------------------------------------------------------------------

    /**
     * Creates a new formation.
     */
    suspend fun createFormation(
        name: String,
        type: ShadowFormationType,
        description: String,
        memberShadowIds: List<String>
    ): Result<ShadowFormation> {
        return runCatching {
            val formationId = UUID.randomUUID().toString()
            val formation = ShadowFormation(
                formationId = formationId,
                name = name.take(50),
                formationType = type,
                description = description.take(200),
                maxSlots = type.maxSlots,
                memberShadowIds = memberShadowIds.take(type.maxSlots),
                isActive = false,
                createdAtEpochMs = System.currentTimeMillis(),
                updatedAtEpochMs = System.currentTimeMillis()
            )
            formationDao.upsertFormation(formation.toEntity())
            formation
        }
    }

    /**
     * Updates an existing formation's members.
     */
    suspend fun updateFormationMembers(
        formationId: String,
        memberShadowIds: List<String>
    ): Result<ShadowFormation> {
        return runCatching {
            val existing = formationDao.getFormationById(formationId)?.toDomain()
                ?: throw IllegalArgumentException("Formation not found: $formationId")

            val validMembers = memberShadowIds.take(existing.maxSlots)
            val updated = existing.copy(
                memberShadowIds = validMembers,
                updatedAtEpochMs = System.currentTimeMillis()
            )
            formationDao.upsertFormation(updated.toEntity())

            // Update each shadow's formation reference
            for (shadowId in validMembers) {
                shadowDao.setFormation(shadowId, formationId)
            }
            // Clear formation from shadows that were removed
            val removed = existing.memberShadowIds - validMembers.toSet()
            for (shadowId in removed) {
                shadowDao.setFormation(shadowId, null)
            }

            updated
        }
    }

    /**
     * Sets a formation as the active formation (only one active at a time).
     */
    suspend fun setActiveFormation(formationId: String): Result<Unit> {
        return runCatching {
            formationDao.getFormationById(formationId)
                ?: throw IllegalArgumentException("Formation not found: $formationId")

            formationDao.clearAllActiveFormations()
            formationDao.setActiveFormation(formationId)
        }
    }

    suspend fun deleteFormation(formationId: String): Result<Unit> {
        return runCatching {
            val formation = formationDao.getFormationById(formationId)?.toDomain()
                ?: throw IllegalArgumentException("Formation not found: $formationId")

            // Clear formation reference from member shadows
            for (shadowId in formation.memberShadowIds) {
                shadowDao.setFormation(shadowId, null)
            }

            formationDao.deleteFormation(formationId)
        }
    }

    // -------------------------------------------------------------------------
    // QUERIES
    // -------------------------------------------------------------------------

    suspend fun getActiveDeployments(): List<ShadowDeploymentRecord> {
        return deploymentDao.getActiveDeploymentsOnce().map { it.toDomain() }
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

