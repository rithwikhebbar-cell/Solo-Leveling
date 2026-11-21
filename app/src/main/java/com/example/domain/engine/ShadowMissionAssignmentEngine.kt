package com.example.domain.engine

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.DungeonDefinitionDao
import com.example.data.local.dao.DungeonInstanceDao
import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowTacticalDao
import com.example.data.local.entity.ShadowMissionAssignmentEntity
import com.example.data.local.entity.toDomain
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.MissionSuitabilityEvaluation
import com.example.domain.model.MissionTacticalRequirement
import com.example.domain.model.MissionType
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowSquad
import com.example.domain.model.ShadowTacticalRole
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

// ============================================================================
// SHADOW MISSION ASSIGNMENT ENGINE
// Evaluates mission suitability for squads and individual shadows, calculates
// role coverage and risk factors, and executes deployments strictly via
// the authoritative ShadowDeploymentEngine upon explicit player confirmation.
// ============================================================================

class ShadowMissionAssignmentEngine(
    private val database: SoloLevelingDatabase,
    private val shadowTacticalDao: ShadowTacticalDao,
    private val shadowDao: ShadowDao,
    private val dungeonDefinitionDao: DungeonDefinitionDao,
    private val dungeonInstanceDao: DungeonInstanceDao,
    private val tacticalIntelligenceEngine: ShadowTacticalIntelligenceEngine,
    private val deploymentEngine: ShadowDeploymentEngine,
    private val mutex: Mutex = Mutex()
) {

    // -------------------------------------------------------------------------
    // 1. GET AVAILABLE MISSIONS FOR TACTICAL EVALUATION
    // -------------------------------------------------------------------------

    suspend fun getAvailableMissions(accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID): List<MissionTacticalRequirement> {
        val missions = mutableListOf<MissionTacticalRequirement>()

        // 1. Pull active or available dungeons from database
        val dungeons = runCatching { dungeonDefinitionDao.getAllDungeonsOnce() }.getOrDefault(emptyList())
        dungeons.take(5).forEach { d ->
            val requiredRoles = when (d.rank) {
                "S_RANK" -> listOf(ShadowTacticalRole.FRONTLINE, ShadowTacticalRole.ASSAULT, ShadowTacticalRole.RANGED, ShadowTacticalRole.SUPPORT)
                "A_RANK" -> listOf(ShadowTacticalRole.FRONTLINE, ShadowTacticalRole.ASSAULT, ShadowTacticalRole.SUPPORT)
                "B_RANK" -> listOf(ShadowTacticalRole.FRONTLINE, ShadowTacticalRole.ASSAULT)
                else -> listOf(ShadowTacticalRole.ASSAULT)
            }
            missions.add(
                MissionTacticalRequirement(
                    missionId = "dungeon_${d.id}",
                    missionTitle = d.name,
                    missionType = MissionType.DUNGEON,
                    difficultyRank = d.rank,
                    requiredRoles = requiredRoles,
                    recommendedPower = d.recommendedLevel * 25,
                    enemyCharacteristics = listOf("Dungeon Grade: ${d.rank}", "${d.totalRooms} Encounter Chambers"),
                    description = d.description
                )
            )
        }

        // 2. Add standard tactical operations if database lists are sparse
        if (missions.isEmpty()) {
            missions.add(
                MissionTacticalRequirement(
                    missionId = "mission_gate_recon",
                    missionTitle = "Red Gate Reconnaissance",
                    missionType = MissionType.DUNGEON,
                    difficultyRank = "A_RANK",
                    requiredRoles = listOf(ShadowTacticalRole.RECONNAISSANCE, ShadowTacticalRole.FRONTLINE, ShadowTacticalRole.ASSAULT),
                    recommendedPower = 350,
                    enemyCharacteristics = listOf("High Environmental Hostility", "Ambush Hazards"),
                    description = "Infiltrate and stabilize an unstable Red Gate perimeter before full spatial breach."
                )
            )
            missions.add(
                MissionTacticalRequirement(
                    missionId = "mission_boss_suppression",
                    missionTitle = "Monarch Domain Suppression",
                    missionType = MissionType.BOSS_BATTLE,
                    difficultyRank = "S_RANK",
                    requiredRoles = listOf(ShadowTacticalRole.FRONTLINE, ShadowTacticalRole.COMMAND, ShadowTacticalRole.RANGED, ShadowTacticalRole.SUPPORT),
                    recommendedPower = 600,
                    enemyCharacteristics = listOf("Massive Single-Target Damage", "Area Shockwaves"),
                    description = "Eliminate an elite realm colossus threatening reality synchronization."
                )
            )
        }

        return missions
    }

    // -------------------------------------------------------------------------
    // 2. EVALUATE MISSION SUITABILITY
    // -------------------------------------------------------------------------

    fun evaluateMissionSuitability(
        mission: MissionTacticalRequirement,
        squad: ShadowSquad?,
        roster: List<Shadow>
    ): MissionSuitabilityEvaluation {
        val rosterMap = roster.associateBy { it.shadowId }
        val assignedShadows = squad?.memberShadowIds?.mapNotNull { rosterMap[it] } ?: emptyList()

        val profiles = assignedShadows.map { tacticalIntelligenceEngine.evaluateTacticalProfile(it) }
        val rolesPresent = profiles.map { it.primaryRole }.toSet()

        val missingRoles = mission.requiredRoles.filter { !rolesPresent.contains(it) }
        val totalRequired = mission.requiredRoles.size.coerceAtLeast(1)
        val coverageCount = (mission.requiredRoles.size - missingRoles.size).coerceAtLeast(0)
        val coveragePct = (coverageCount.toFloat() / totalRequired).coerceIn(0f, 1f)

        val totalSquadPower = assignedShadows.sumOf { it.calculatePowerRating() }
        val powerRatio = if (mission.recommendedPower > 0) {
            (totalSquadPower.toFloat() / mission.recommendedPower).coerceAtMost(1.5f)
        } else 1.0f

        val risks = mutableListOf<String>()
        val missingReqs = mutableListOf<String>()

        if (assignedShadows.isEmpty()) {
            risks.add("No shadows currently assigned to this operation.")
            missingReqs.add("Squad has zero active members.")
        }

        missingRoles.forEach { role ->
            missingReqs.add("Missing required tactical role: ${role.label}")
            when (role) {
                ShadowTacticalRole.FRONTLINE -> risks.add("Squad lacks damage absorption; backline units may suffer fatal damage.")
                ShadowTacticalRole.RANGED -> risks.add("Hostile standoff units cannot be neutralized effectively.")
                ShadowTacticalRole.SUPPORT -> risks.add("Attrition risk: No squad restoration during extended encounter.")
                ShadowTacticalRole.COMMAND -> risks.add("Coordination delay: Squad lacks tactical command aura.")
                else -> risks.add("Sub-optimal role coverage for ${role.label}.")
            }
        }

        if (powerRatio < 0.8f && assignedShadows.isNotEmpty()) {
            risks.add("Squad total power ($totalSquadPower) is below recommended threshold (${mission.recommendedPower}).")
        }

        val suitability = if (assignedShadows.isEmpty()) 0 else {
            ((coveragePct * 60f) + (powerRatio.coerceAtMost(1f) * 40f) - (risks.size * 5)).toInt().coerceIn(10, 100)
        }

        val advice = when {
            suitability >= 80 -> "Optimal tactical match. Squad possesses requisite role coverage and combat rating."
            suitability >= 50 -> "Viable deployment with moderate operational risk. Address missing roles for safer execution."
            else -> "High casualty probability. Restructure squad to fulfill required tactical roles before deployment."
        }

        return MissionSuitabilityEvaluation(
            mission = mission,
            recommendedSquadId = squad?.squadId,
            recommendedSquadName = squad?.name,
            recommendedShadowIds = assignedShadows.map { it.shadowId },
            suitabilityScore = suitability,
            capabilityCoveragePct = coveragePct,
            missingRequirements = missingReqs,
            riskFactors = risks,
            tacticalAdvice = advice
        )
    }

    // -------------------------------------------------------------------------
    // 3. RECOMMEND BEST SQUAD FOR MISSION
    // -------------------------------------------------------------------------

    fun recommendSquadForMission(
        mission: MissionTacticalRequirement,
        squads: List<ShadowSquad>,
        roster: List<Shadow>
    ): MissionSuitabilityEvaluation {
        if (squads.isEmpty()) {
            return evaluateMissionSuitability(mission, null, roster)
        }

        val evaluations = squads.map { squad ->
            evaluateMissionSuitability(mission, squad, roster)
        }

        return evaluations.maxByOrNull { it.suitabilityScore }
            ?: evaluateMissionSuitability(mission, squads.first(), roster)
    }

    // -------------------------------------------------------------------------
    // 4. CONFIRM & DEPLOY MISSION ASSIGNMENT (AUTHORITATIVE HOOK)
    // -------------------------------------------------------------------------

    suspend fun confirmAndDeployMissionAssignment(
        missionId: String,
        missionType: MissionType,
        squadId: String?,
        shadowIdsToDeploy: List<String>,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowMissionAssignmentEntity> = runCatching {
        val owner = accountOwnerId.trim()
        require(shadowIdsToDeploy.isNotEmpty()) { "Must assign at least one shadow to mission" }

        mutex.withLock {
            database.withTransaction {
                val deployedRecords = mutableListOf<String>()

                // Authoritative deployment through ShadowDeploymentEngine
                for (shadowId in shadowIdsToDeploy) {
                    val shadow = shadowDao.getShadowByIdForOwner(shadowId, owner)
                        ?: throw IllegalArgumentException("Shadow $shadowId does not belong to $owner")

                    if (shadow.deploymentStatus != ShadowDeploymentStatus.DEPLOYED.name) {
                        val deployRes = deploymentEngine.deployShadow(
                            shadowId = shadowId,
                            category = ShadowDeploymentCategory.DUNGEON_SUPPORT,
                            role = "Mission Tactical Specialist",
                            accountOwnerId = owner
                        )
                        if (deployRes.isFailure) {
                            throw IllegalStateException("Failed to deploy shadow $shadowId: ${deployRes.exceptionOrNull()?.message}")
                        }
                    }
                    deployedRecords.add(shadowId)
                }

                val now = System.currentTimeMillis()
                val assignmentId = UUID.randomUUID().toString()
                val assignment = ShadowMissionAssignmentEntity(
                    assignmentId = assignmentId,
                    accountOwnerId = owner,
                    missionId = missionId,
                    missionTypeName = missionType.name,
                    squadId = squadId,
                    assignedShadowIdsJson = Json.encodeToString(deployedRecords),
                    statusName = "DEPLOYED",
                    assignedAtEpochMs = now,
                    completedAtEpochMs = null
                )

                shadowTacticalDao.insertAssignment(assignment)
                assignment
            }
        }
    }
}
