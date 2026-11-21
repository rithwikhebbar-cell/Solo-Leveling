package com.example.domain.model

import kotlinx.serialization.Serializable

// ============================================================================
// PHASE 6.5 — SHADOW ARMY INTELLIGENCE & TACTICAL COMMAND MODELS
// ============================================================================

/**
 * 6 Core Tactical Roles derived deterministically from shadow attributes and class identity.
 */
@Serializable
enum class ShadowTacticalRole(
    val label: String,
    val description: String,
    val primaryFunction: String
) {
    FRONTLINE(
        label = "Frontline Anchor",
        description = "Absorbs incoming pressure, protects backline units, and establishes a defensive perimeter.",
        primaryFunction = "Damage absorption and perimeter control"
    ),
    ASSAULT(
        label = "Assault Striker",
        description = "High offensive pressure, pierces enemy defensive formations, and eliminates priority targets.",
        primaryFunction = "Burst damage and breakthrough force"
    ),
    RANGED(
        label = "Ranged Specialist",
        description = "Attacks from a safe standoff distance, delivers continuous suppression, and snipes key threats.",
        primaryFunction = "Continuous ranged suppression and artillery"
    ),
    SUPPORT(
        label = "Tactical Support",
        description = "Provides team utility, enhances squad capabilities, and facilitates battlefield sustenance.",
        primaryFunction = "Sustenance, buffing, and squad cohesion"
    ),
    RECONNAISSANCE(
        label = "Recon Scout",
        description = "High mobility scout, flanks enemy positions, gathers intelligence, and disrupts enemy flow.",
        primaryFunction = "Flanking, mobility, and intelligence gathering"
    ),
    COMMAND(
        label = "Squad Commander",
        description = "Tactical coordination, strategic leadership, formation synergy, and command presence.",
        primaryFunction = "Tactical coordination and squad leadership"
    )
}

/**
 * Normalized 7-dimensional tactical capability assessment (0–100 scale).
 * Strictly derived from actual stats, class multipliers, rank, and evolution stage.
 */
@Serializable
data class TacticalCapabilityVector(
    val offensiveCapability: Int,
    val defensiveCapability: Int,
    val mobility: Int,
    val rangedCapability: Int,
    val supportCapability: Int,
    val strategicUtility: Int,
    val survivability: Int
) {
    val overallTacticalScore: Int
        get() = ((offensiveCapability * 0.22f) +
                (defensiveCapability * 0.18f) +
                (mobility * 0.14f) +
                (rangedCapability * 0.14f) +
                (supportCapability * 0.12f) +
                (strategicUtility * 0.10f) +
                (survivability * 0.10f)).toInt().coerceIn(0, 100)

    companion object {
        val ZERO = TacticalCapabilityVector(0, 0, 0, 0, 0, 0, 0)
    }
}

/**
 * Analytical profile of an individual Shadow.
 */
@Serializable
data class ShadowTacticalProfile(
    val shadowId: String,
    val name: String,
    val shadowClass: ShadowClass,
    val currentRank: ShadowRank,
    val evolutionStage: ShadowEvolutionStage,
    val level: Int,
    val capabilities: TacticalCapabilityVector,
    val primaryRole: ShadowTacticalRole,
    val secondaryRoles: List<ShadowTacticalRole> = emptyList(),
    val roleExplanation: String,
    val keyStrengths: List<String>,
    val tacticalWeaknesses: List<String>,
    val isAvailable: Boolean,
    val deploymentStatus: ShadowDeploymentStatus
)

/**
 * 6 Squad Categories as organizational presets for military operations.
 */
@Serializable
enum class ShadowSquadCategory(
    val label: String,
    val description: String,
    val recommendedRoles: List<ShadowTacticalRole>
) {
    ASSAULT_SQUAD(
        label = "Assault Breakthrough Squad",
        description = "High-octane vanguard force designed to pierce enemy lines and destroy elite targets.",
        recommendedRoles = listOf(ShadowTacticalRole.ASSAULT, ShadowTacticalRole.FRONTLINE, ShadowTacticalRole.RECONNAISSANCE)
    ),
    DEFENSE_SQUAD(
        label = "Defensive Bastion Squad",
        description = "Impenetrable shield formation built for high endurance, area lockdown, and attrition.",
        recommendedRoles = listOf(ShadowTacticalRole.FRONTLINE, ShadowTacticalRole.SUPPORT, ShadowTacticalRole.COMMAND)
    ),
    RANGED_SQUAD(
        label = "Ranged Artillery Squad",
        description = "Standoff barrage division that obliterates targets before they breach melee range.",
        recommendedRoles = listOf(ShadowTacticalRole.RANGED, ShadowTacticalRole.RECONNAISSANCE, ShadowTacticalRole.FRONTLINE)
    ),
    SUPPORT_SQUAD(
        label = "Logistics & Support Squad",
        description = "Battlefield stabilization unit providing persistent buffing, field repair, and tactical sustain.",
        recommendedRoles = listOf(ShadowTacticalRole.SUPPORT, ShadowTacticalRole.COMMAND, ShadowTacticalRole.FRONTLINE)
    ),
    RECON_SQUAD(
        label = "Reconnaissance Vanguard",
        description = "Rapid response scouts specializing in terrain scouting, ambushes, and flanking operations.",
        recommendedRoles = listOf(ShadowTacticalRole.RECONNAISSANCE, ShadowTacticalRole.ASSAULT, ShadowTacticalRole.RANGED)
    ),
    ELITE_GUARD(
        label = "Monarch's Elite Guard",
        description = "Premier retinue comprising the Monarch's most formidable knights for high-risk operations.",
        recommendedRoles = listOf(ShadowTacticalRole.COMMAND, ShadowTacticalRole.ASSAULT, ShadowTacticalRole.FRONTLINE, ShadowTacticalRole.SUPPORT)
    )
}

/**
 * Tactical Squad Domain Model.
 */
@Serializable
data class ShadowSquad(
    val squadId: String,
    val accountOwnerId: String,
    val name: String,
    val category: ShadowSquadCategory,
    val leaderShadowId: String? = null,
    val memberShadowIds: List<String> = emptyList(),
    val isActive: Boolean = false,
    val maxMembers: Int = 5,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val memberCount: Int get() = memberShadowIds.size
    val availableSlots: Int get() = (maxMembers - memberCount).coerceAtLeast(0)
    val hasLeader: Boolean get() = !leaderShadowId.isNullOrBlank() && memberShadowIds.contains(leaderShadowId)
}

/**
 * Detailed information for a squad member.
 */
data class SquadMemberDetail(
    val shadow: Shadow,
    val tacticalProfile: ShadowTacticalProfile,
    val assignedRole: ShadowTacticalRole,
    val positionIndex: Int,
    val isLeader: Boolean
)

/**
 * Squad Synergy & Compatibility Report.
 */
@Serializable
data class SquadSynergyReport(
    val synergyScore: Int, // 0–100
    val roleCoverage: Map<ShadowTacticalRole, Int>,
    val missingRoles: List<ShadowTacticalRole>,
    val complementaryPairs: List<String>,
    val tacticalOverlaps: List<String>,
    val coordinationWarnings: List<String>,
    val recommendedAdjustments: List<String>,
    val bondHighlights: List<String>
)

/**
 * Full Squad View Presentation Object.
 */
data class SquadDetail(
    val squad: ShadowSquad,
    val leader: Shadow?,
    val members: List<SquadMemberDetail>,
    val capabilityVector: TacticalCapabilityVector,
    val synergyReport: SquadSynergyReport,
    val readinessScore: Int
)

/**
 * Formation Intelligence Assessment.
 */
@Serializable
data class FormationTacticalAssessment(
    val formationId: String,
    val formationName: String,
    val formationType: ShadowFormationType,
    val strengthScore: Int, // 0–100
    val tacticalBalance: String, // e.g. "OPTIMAL_BALANCE", "OFFENSIVE_HEAVY", "LACKS_FRONTLINE"
    val roleCoverage: Map<ShadowTacticalRole, Int>,
    val missingCapabilities: List<String>,
    val redundantRoles: List<String>,
    val weaknessDetection: List<String>,
    val suggestedImprovements: List<String>
)

/**
 * Mission Tactical Requirements.
 */
@Serializable
enum class MissionType(val label: String) {
    DUNGEON("Dungeon Raid"),
    BOSS_BATTLE("Boss Elimination"),
    SHADOW_QUEST("Monarch Quest")
}

@Serializable
data class MissionTacticalRequirement(
    val missionId: String,
    val missionTitle: String,
    val missionType: MissionType,
    val difficultyRank: String,
    val requiredRoles: List<ShadowTacticalRole>,
    val recommendedPower: Int,
    val enemyCharacteristics: List<String> = emptyList(),
    val description: String
)

/**
 * Mission Suitability Evaluation Output.
 */
@Serializable
data class MissionSuitabilityEvaluation(
    val mission: MissionTacticalRequirement,
    val recommendedSquadId: String?,
    val recommendedSquadName: String?,
    val recommendedShadowIds: List<String>,
    val suitabilityScore: Int, // 0–100
    val capabilityCoveragePct: Float,
    val missingRequirements: List<String>,
    val riskFactors: List<String>,
    val tacticalAdvice: String
)

/**
 * Tactical Alert Severity and Alert Model.
 */
@Serializable
enum class TacticalAlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

@Serializable
data class TacticalAlert(
    val alertId: String,
    val severity: TacticalAlertSeverity,
    val title: String,
    val message: String,
    val relatedSquadId: String? = null,
    val relatedShadowId: String? = null,
    val timestampEpochMs: Long = System.currentTimeMillis()
)

/**
 * Performance Analytics derived from verified persisted history.
 */
@Serializable
data class ShadowTacticalAnalytics(
    val totalDeployments: Int,
    val successfulDeployments: Int,
    val deploymentSuccessRate: Float,
    val mostDeployedShadows: List<Pair<String, Int>>, // Name to count
    val leastUtilizedShadows: List<Pair<String, Int>>,
    val squadUtilization: Map<String, Int>, // Squad Name to count
    val formationUsageTrends: Map<String, Int>, // Formation Name to count
    val roleDistribution: Map<ShadowTacticalRole, Int>,
    val missionParticipationCount: Int
) {
    companion object {
        val EMPTY = ShadowTacticalAnalytics(
            totalDeployments = 0,
            successfulDeployments = 0,
            deploymentSuccessRate = 0f,
            mostDeployedShadows = emptyList(),
            leastUtilizedShadows = emptyList(),
            squadUtilization = emptyMap(),
            formationUsageTrends = emptyMap(),
            roleDistribution = emptyMap(),
            missionParticipationCount = 0
        )
    }
}
