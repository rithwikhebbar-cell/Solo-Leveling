package com.example.domain.engine

import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowDeploymentDao
import com.example.data.local.dao.ShadowFormationDao
import com.example.data.local.dao.ShadowTacticalDao
import com.example.data.local.entity.toDomain
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowTacticalAnalytics
import com.example.domain.model.ShadowTacticalRole
import com.example.domain.model.TacticalAlert
import com.example.domain.model.TacticalAlertSeverity
import java.util.UUID

// ============================================================================
// SHADOW TACTICAL ANALYTICS ENGINE
// Generates performance metrics, utilization trends, and real-time alerts
// derived exclusively from verified persisted deployment and roster data.
// ============================================================================

class ShadowTacticalAnalyticsEngine(
    private val shadowDao: ShadowDao,
    private val deploymentDao: ShadowDeploymentDao,
    private val formationDao: ShadowFormationDao,
    private val shadowTacticalDao: ShadowTacticalDao,
    private val tacticalIntelligenceEngine: ShadowTacticalIntelligenceEngine
) {

    suspend fun generateAnalytics(accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID): ShadowTacticalAnalytics {
        val allDeployments = runCatching {
            deploymentDao.getAllDeploymentsForOwnerOnce(accountOwnerId)
        }.getOrDefault(emptyList())

        val allShadows = runCatching {
            shadowDao.getAllShadowsForOwnerOnce(accountOwnerId).map { it.toDomain() }
        }.getOrDefault(emptyList())

        if (allDeployments.isEmpty() && allShadows.isEmpty()) {
            return ShadowTacticalAnalytics.EMPTY
        }

        val totalDeployments = allDeployments.size
        val successfulDeployments = allDeployments.count { it.statusName == ShadowDeploymentStatus.RECALLED.name || it.recalledAtEpochMs != null }
        val successRate = if (totalDeployments > 0) (successfulDeployments.toFloat() / totalDeployments) else 1.0f

        // Shadow deployment counts
        val shadowDeploymentCounts = allDeployments.groupBy { it.shadowName }
            .mapValues { it.value.size }

        val mostDeployed = shadowDeploymentCounts.toList()
            .sortedByDescending { it.second }
            .take(5)

        // Least utilized shadows from owned army
        val leastDeployed = allShadows.map { shadow ->
            shadow.name to (shadowDeploymentCounts[shadow.name] ?: 0)
        }.sortedBy { it.second }.take(5)

        // Role distribution across all owned shadows
        val roleDist = mutableMapOf<ShadowTacticalRole, Int>()
        ShadowTacticalRole.entries.forEach { roleDist[it] = 0 }
        allShadows.forEach { shadow ->
            val profile = tacticalIntelligenceEngine.evaluateTacticalProfile(shadow)
            roleDist[profile.primaryRole] = (roleDist[profile.primaryRole] ?: 0) + 1
        }

        // Squad utilization from mission assignments
        val assignments = runCatching {
            shadowTacticalDao.getRecentAssignmentsForOwner(accountOwnerId, 50)
        }.getOrDefault(emptyList())

        val squadUtilization = assignments.mapNotNull { it.squadId }
            .groupingBy { it }
            .eachCount()

        // Formation usage trends
        val formations = runCatching {
            formationDao.getAllFormationsForOwnerOnce(accountOwnerId)
        }.getOrDefault(emptyList())

        val formationUsage = formations.associate { it.name to if (it.isActive) 1 else 0 }

        return ShadowTacticalAnalytics(
            totalDeployments = totalDeployments,
            successfulDeployments = successfulDeployments,
            deploymentSuccessRate = successRate,
            mostDeployedShadows = mostDeployed,
            leastUtilizedShadows = leastDeployed,
            squadUtilization = squadUtilization,
            formationUsageTrends = formationUsage,
            roleDistribution = roleDist,
            missionParticipationCount = assignments.size
        )
    }

    suspend fun generateTacticalAlerts(accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID): List<TacticalAlert> {
        val alerts = mutableListOf<TacticalAlert>()

        val allShadows = runCatching {
            shadowDao.getAllShadowsForOwnerOnce(accountOwnerId).map { it.toDomain() }
        }.getOrDefault(emptyList())
        val shadowMap = allShadows.associateBy { it.shadowId }

        val activeSquadEntity = runCatching {
            shadowTacticalDao.getActiveSquadForOwnerOnce(accountOwnerId)
        }.getOrNull()

        if (activeSquadEntity != null) {
            val members = shadowTacticalDao.getMembersForSquadOnce(activeSquadEntity.squadId)
            val assignedShadows = members.mapNotNull { shadowMap[it.shadowId] }

            // Check unavailable shadows in active squad
            assignedShadows.forEach { shadow ->
                if (shadow.deploymentStatus == ShadowDeploymentStatus.DEPLOYED) {
                    alerts.add(
                        TacticalAlert(
                            alertId = UUID.randomUUID().toString(),
                            severity = TacticalAlertSeverity.WARNING,
                            title = "Active Squad Member Deployed",
                            message = "${shadow.name} is currently deployed on another mission. Squad readiness impaired.",
                            relatedSquadId = activeSquadEntity.squadId,
                            relatedShadowId = shadow.shadowId
                        )
                    )
                }
            }

            // Check missing Frontline Anchor
            val hasFrontline = assignedShadows.any {
                tacticalIntelligenceEngine.evaluateTacticalProfile(it).primaryRole == ShadowTacticalRole.FRONTLINE
            }
            if (!hasFrontline && assignedShadows.isNotEmpty()) {
                alerts.add(
                    TacticalAlert(
                        alertId = UUID.randomUUID().toString(),
                        severity = TacticalAlertSeverity.HIGH,
                        title = "No Frontline Defense Anchor",
                        message = "Active squad '${activeSquadEntity.name}' lacks a Frontline Anchor. High backline vulnerability.",
                        relatedSquadId = activeSquadEntity.squadId
                    )
                )
            }
        }

        // Active formation check
        val activeFormation = runCatching {
            formationDao.getActiveFormationForOwnerOnce(accountOwnerId)?.toDomain()
        }.getOrNull()

        if (activeFormation != null) {
            val assessment = tacticalIntelligenceEngine.evaluateFormation(activeFormation, allShadows)
            if (assessment.missingCapabilities.isNotEmpty()) {
                alerts.add(
                    TacticalAlert(
                        alertId = UUID.randomUUID().toString(),
                        severity = TacticalAlertSeverity.MEDIUM,
                        title = "Formation Capability Deficit",
                        message = "Active formation '${activeFormation.name}' missing: ${assessment.missingCapabilities.joinToString()}.",
                        relatedSquadId = null
                    )
                )
            }
        }

        return alerts
    }
}
