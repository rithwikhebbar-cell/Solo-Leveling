package com.example.domain.engine

import com.example.domain.model.Shadow
import com.example.domain.model.ShadowBondProfile
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowFormation
import com.example.domain.model.ShadowFormationType
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowSkillUnlock
import com.example.domain.model.ShadowSquadCategory
import com.example.domain.model.ShadowTacticalProfile
import com.example.domain.model.ShadowTacticalRole
import com.example.domain.model.SquadSynergyReport
import com.example.domain.model.FormationTacticalAssessment
import com.example.domain.model.TacticalCapabilityVector

// ============================================================================
// SHADOW TACTICAL INTELLIGENCE ENGINE
// Deterministic military evaluation of the Shadow Army.
// Derives 7-dimensional capability vectors, tactical role classifications,
// squad synergy analysis, and formation balance assessments strictly from
// existing persisted statistics, classes, ranks, and bond records.
// ============================================================================

class ShadowTacticalIntelligenceEngine {

    // -------------------------------------------------------------------------
    // 1. TACTICAL CAPABILITY ASSESSMENT (DETERMINISTIC & EXPLAINABLE)
    // -------------------------------------------------------------------------

    fun evaluateTacticalProfile(
        shadow: Shadow,
        unlockedSkills: List<ShadowSkillUnlock> = emptyList()
    ): ShadowTacticalProfile {
        val stats = shadow.stats
        val cls = shadow.shadowClass
        val rank = shadow.currentRank
        val stage = shadow.evolutionStage

        // Class-specific weighting multipliers
        val (offClassMod, defClassMod, mobClassMod, rngClassMod, supClassMod, strClassMod) = when (cls) {
            ShadowClass.VANGUARD -> listOf(1.30f, 1.15f, 1.10f, 0.40f, 0.60f, 0.85f)
            ShadowClass.GUARDIAN -> listOf(0.85f, 1.45f, 0.70f, 0.30f, 1.10f, 0.90f)
            ShadowClass.RANGER -> listOf(1.15f, 0.75f, 1.35f, 1.40f, 0.50f, 0.95f)
            ShadowClass.ARCANIST -> listOf(1.25f, 0.65f, 0.80f, 1.45f, 0.80f, 1.10f)
            ShadowClass.TACTICIAN -> listOf(0.90f, 0.90f, 1.05f, 0.75f, 1.25f, 1.45f)
            ShadowClass.SUPPORT -> listOf(0.60f, 1.10f, 0.90f, 0.50f, 1.45f, 1.25f)
        }

        // Rank scale modifier: E=0.85, D=0.95, C=1.05, B=1.15, A=1.25, S=1.40, MONARCH=1.60
        val rankScale = when (rank) {
            ShadowRank.E_RANK -> 0.85f
            ShadowRank.D_RANK -> 0.95f
            ShadowRank.C_RANK -> 1.05f
            ShadowRank.B_RANK -> 1.15f
            ShadowRank.A_RANK -> 1.25f
            ShadowRank.S_RANK -> 1.40f
            ShadowRank.MONARCH_CLASS -> 1.60f
        }

        // Evolution stage scaling bonus
        val stageBonus = when (stage) {
            ShadowEvolutionStage.BASIC -> 0
            ShadowEvolutionStage.ELITE -> 4
            ShadowEvolutionStage.KNIGHT -> 8
            ShadowEvolutionStage.ELITE_KNIGHT -> 12
            ShadowEvolutionStage.COMMANDER -> 16
            ShadowEvolutionStage.MARSHAL -> 20
            ShadowEvolutionStage.GRAND_MARSHAL -> 25
        }

        // Skill count tactical boost
        val skillBonus = (unlockedSkills.size * 2).coerceAtMost(10)

        // Derive 7 dimensions (normalized to 0–100 scale)
        // 1. Offensive Capability: Driven by STR (60%), INT (30%), AGI (10%)
        val rawOff = ((stats.strength * 0.60f + stats.intelligence * 0.30f + stats.agility * 0.10f) * offClassMod * rankScale)
        val offScore = (rawOff.toInt() + stageBonus + skillBonus).coerceIn(10, 100)

        // 2. Defensive Capability: Driven by VIT (70%), STR (30%)
        val rawDef = ((stats.vitality * 0.70f + stats.strength * 0.30f) * defClassMod * rankScale)
        val defScore = (rawDef.toInt() + stageBonus + skillBonus).coerceIn(10, 100)

        // 3. Mobility: Driven by AGI (70%), PER (30%)
        val rawMob = ((stats.agility * 0.70f + stats.perception * 0.30f) * mobClassMod * rankScale)
        val mobScore = (rawMob.toInt() + stageBonus).coerceIn(10, 100)

        // 4. Ranged Capability: Driven by INT (50%), PER (30%), AGI (20%)
        val rawRng = ((stats.intelligence * 0.50f + stats.perception * 0.30f + stats.agility * 0.20f) * rngClassMod * rankScale)
        val rngScore = (rawRng.toInt() + stageBonus).coerceIn(5, 100)

        // 5. Support Capability: Driven by COM (50%), INT (30%), VIT (20%)
        val rawSup = ((stats.command * 0.50f + stats.intelligence * 0.30f + stats.vitality * 0.20f) * supClassMod * rankScale)
        val supScore = (rawSup.toInt() + stageBonus + skillBonus).coerceIn(5, 100)

        // 6. Strategic Utility: Driven by COM (60%), PER (25%), INT (15%)
        val rawStr = ((stats.command * 0.60f + stats.perception * 0.25f + stats.intelligence * 0.15f) * strClassMod * rankScale)
        val strScore = (rawStr.toInt() + stageBonus + skillBonus).coerceIn(10, 100)

        // 7. Survivability: Driven by VIT (60%), AGI (20%), STR (20%)
        val rawSurv = ((stats.vitality * 0.60f + stats.agility * 0.20f + stats.strength * 0.20f) * rankScale)
        val survScore = (rawSurv.toInt() + stageBonus).coerceIn(10, 100)

        val capabilities = TacticalCapabilityVector(
            offensiveCapability = offScore,
            defensiveCapability = defScore,
            mobility = mobScore,
            rangedCapability = rngScore,
            supportCapability = supScore,
            strategicUtility = strScore,
            survivability = survScore
        )

        // Role Classification
        val (primaryRole, secondaryRoles, explanation) = classifyRoles(cls, capabilities)

        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()

        if (offScore >= 75) strengths.add("Devastating offensive damage output")
        if (defScore >= 75) strengths.add("Solid physical and magical damage absorption")
        if (mobScore >= 75) strengths.add("Exceptional tactical flanking mobility")
        if (rngScore >= 75) strengths.add("Superior standoff ranged firepower")
        if (supScore >= 75) strengths.add("High team sustain and buffing capacity")
        if (strScore >= 75) strengths.add("Superb command aura and strategic battlefield coordination")

        if (defScore < 40) weaknesses.add("Vulnerable to focused melee counter-attacks")
        if (rngScore < 30) weaknesses.add("Lacks effective ranged suppression")
        if (mobScore < 40) weaknesses.add("Sluggish repositioning in dynamic engagements")
        if (supScore < 30) weaknesses.add("Zero defensive or restorative squad utility")

        return ShadowTacticalProfile(
            shadowId = shadow.shadowId,
            name = shadow.name,
            shadowClass = cls,
            currentRank = rank,
            evolutionStage = stage,
            level = shadow.level,
            capabilities = capabilities,
            primaryRole = primaryRole,
            secondaryRoles = secondaryRoles,
            roleExplanation = explanation,
            keyStrengths = if (strengths.isEmpty()) listOf("Balanced fundamental combat profile") else strengths,
            tacticalWeaknesses = if (weaknesses.isEmpty()) listOf("No acute tactical vulnerabilities detected") else weaknesses,
            isAvailable = shadow.deploymentStatus == ShadowDeploymentStatus.AVAILABLE && !shadow.isRetired,
            deploymentStatus = shadow.deploymentStatus
        )
    }

    private fun classifyRoles(
        cls: ShadowClass,
        caps: TacticalCapabilityVector
    ): Triple<ShadowTacticalRole, List<ShadowTacticalRole>, String> {
        val scores = mutableMapOf<ShadowTacticalRole, Int>()

        // Score Frontline: Def (50%), Surv (30%), Off (20%)
        scores[ShadowTacticalRole.FRONTLINE] = ((caps.defensiveCapability * 0.50f) + (caps.survivability * 0.30f) + (caps.offensiveCapability * 0.20f)).toInt()
        // Score Assault: Off (60%), Mob (25%), Surv (15%)
        scores[ShadowTacticalRole.ASSAULT] = ((caps.offensiveCapability * 0.60f) + (caps.mobility * 0.25f) + (caps.survivability * 0.15f)).toInt()
        // Score Ranged: Rng (65%), Off (20%), Mob (15%)
        scores[ShadowTacticalRole.RANGED] = ((caps.rangedCapability * 0.65f) + (caps.offensiveCapability * 0.20f) + (caps.mobility * 0.15f)).toInt()
        // Score Support: Sup (65%), Str (20%), Def (15%)
        scores[ShadowTacticalRole.SUPPORT] = ((caps.supportCapability * 0.65f) + (caps.strategicUtility * 0.20f) + (caps.defensiveCapability * 0.15f)).toInt()
        // Score Recon: Mob (60%), Rng (20%), Str (20%)
        scores[ShadowTacticalRole.RECONNAISSANCE] = ((caps.mobility * 0.60f) + (caps.rangedCapability * 0.20f) + (caps.strategicUtility * 0.20f)).toInt()
        // Score Command: Str (55%), Sup (25%), Off (20%)
        scores[ShadowTacticalRole.COMMAND] = ((caps.strategicUtility * 0.55f) + (caps.supportCapability * 0.25f) + (caps.offensiveCapability * 0.20f)).toInt()

        // Class bias boost (+10 points)
        when (cls) {
            ShadowClass.GUARDIAN -> scores[ShadowTacticalRole.FRONTLINE] = (scores[ShadowTacticalRole.FRONTLINE] ?: 0) + 12
            ShadowClass.VANGUARD -> scores[ShadowTacticalRole.ASSAULT] = (scores[ShadowTacticalRole.ASSAULT] ?: 0) + 12
            ShadowClass.RANGER -> scores[ShadowTacticalRole.RANGED] = (scores[ShadowTacticalRole.RANGED] ?: 0) + 12
            ShadowClass.ARCANIST -> scores[ShadowTacticalRole.RANGED] = (scores[ShadowTacticalRole.RANGED] ?: 0) + 12
            ShadowClass.TACTICIAN -> scores[ShadowTacticalRole.COMMAND] = (scores[ShadowTacticalRole.COMMAND] ?: 0) + 14
            ShadowClass.SUPPORT -> scores[ShadowTacticalRole.SUPPORT] = (scores[ShadowTacticalRole.SUPPORT] ?: 0) + 14
        }

        val sorted = scores.toList().sortedByDescending { it.second }
        val primary = sorted.first().first
        val primaryScore = sorted.first().second

        val secondaries = sorted.drop(1)
            .filter { it.second >= 55 && it.second >= (primaryScore * 0.85f) }
            .take(2)
            .map { it.first }

        val explanation = when (primary) {
            ShadowTacticalRole.FRONTLINE -> "Optimal anchor unit with high defensive absorption (${caps.defensiveCapability}/100) and survivability."
            ShadowTacticalRole.ASSAULT -> "Frontline offensive specialist delivering superior assault pressure (${caps.offensiveCapability}/100)."
            ShadowTacticalRole.RANGED -> "Standoff artillery unit equipped with sustained ranged suppression (${caps.rangedCapability}/100)."
            ShadowTacticalRole.SUPPORT -> "Force multiplier unit providing squad utility (${caps.supportCapability}/100) and stabilization."
            ShadowTacticalRole.RECONNAISSANCE -> "High-speed scout with superior tactical mobility (${caps.mobility}/100) and situational awareness."
            ShadowTacticalRole.COMMAND -> "Natural formation leader with commanding tactical presence and strategic utility (${caps.strategicUtility}/100)."
        }

        return Triple(primary, secondaries, explanation)
    }

    // -------------------------------------------------------------------------
    // 2. FORMATION INTELLIGENCE EVALUATION
    // -------------------------------------------------------------------------

    fun evaluateFormation(
        formation: ShadowFormation,
        armyShadows: List<Shadow>
    ): FormationTacticalAssessment {
        val memberMap = armyShadows.associateBy { it.shadowId }
        val members = formation.memberShadowIds.mapNotNull { memberMap[it] }

        val profiles = members.map { evaluateTacticalProfile(it) }

        val roleCoverage = mutableMapOf<ShadowTacticalRole, Int>()
        ShadowTacticalRole.entries.forEach { roleCoverage[it] = 0 }
        profiles.forEach { p ->
            roleCoverage[p.primaryRole] = (roleCoverage[p.primaryRole] ?: 0) + 1
        }

        val missingCaps = mutableListOf<String>()
        val redundantRoles = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        val frontlineCount = roleCoverage[ShadowTacticalRole.FRONTLINE] ?: 0
        val assaultCount = roleCoverage[ShadowTacticalRole.ASSAULT] ?: 0
        val rangedCount = roleCoverage[ShadowTacticalRole.RANGED] ?: 0
        val supportCount = roleCoverage[ShadowTacticalRole.SUPPORT] ?: 0
        val commandCount = roleCoverage[ShadowTacticalRole.COMMAND] ?: 0

        if (frontlineCount == 0) {
            missingCaps.add("No Frontline Anchor")
            weaknesses.add("Formations lacking a dedicated Frontline Anchor are vulnerable to enemy encirclement.")
            improvements.add("Assign at least one Guardian or sturdy Vanguard to absorb initial combat pressure.")
        }
        if (assaultCount == 0 && rangedCount == 0) {
            missingCaps.add("No Direct Offensive Strike Force")
            weaknesses.add("Formation has insufficient offensive firepower to eliminate hostile commanders.")
            improvements.add("Incorporate an Assault Striker or Ranged Specialist to ensure target elimination.")
        }
        if (supportCount == 0 && commandCount == 0) {
            missingCaps.add("No Squad Coordination or Sustain")
            weaknesses.add("Prolonged engagements may cause attrition without tactical sustain.")
            improvements.add("Include a Support unit or Tactician to maintain formation cohesion.")
        }

        // Check redundancies
        if (frontlineCount > 3) {
            redundantRoles.add("Excessive Frontline Anchors ($frontlineCount)")
            weaknesses.add("Formation suffers from diminished offensive output due to defensive over-concentration.")
            improvements.add("Substitute one Frontline Anchor with a Ranged Specialist or Recon unit.")
        }
        if (assaultCount > 3) {
            redundantRoles.add("Excessive Assault Strikers ($assaultCount)")
            weaknesses.add("High offensive burst but lacks sustained defensive buffering.")
            improvements.add("Balance aggressive units with defensive perimeter control.")
        }

        // Calculate strength score
        var strength = 50
        if (members.isNotEmpty()) {
            val avgPower = members.map { it.calculatePowerRating() }.average().toInt()
            val powerBonus = (avgPower / 10).coerceIn(0, 30)
            val coverageBonus = (roleCoverage.values.count { it > 0 } * 4).coerceAtMost(20)
            strength = (strength + powerBonus + coverageBonus - (weaknesses.size * 5)).coerceIn(15, 100)
        } else {
            strength = 0
            weaknesses.add("Formation is completely empty.")
            improvements.add("Deploy recruited shadows to populate formation slots.")
        }

        val tacticalBalance = when {
            members.isEmpty() -> "EMPTY_FORMATION"
            missingCaps.isEmpty() && weaknesses.isEmpty() -> "OPTIMAL_TACTICAL_BALANCE"
            frontlineCount >= 2 && assaultCount >= 2 -> "AGGRESSIVE_VANGUARD_LINE"
            frontlineCount >= 2 && supportCount >= 1 -> "DEFENSIVE_STRONGHOLD"
            rangedCount >= 2 -> "STANDOFF_ARTILLERY_PERIMETER"
            else -> "PARTIALLY_BALANCED_FORMATION"
        }

        return FormationTacticalAssessment(
            formationId = formation.formationId,
            formationName = formation.name,
            formationType = formation.formationType,
            strengthScore = strength,
            tacticalBalance = tacticalBalance,
            roleCoverage = roleCoverage,
            missingCapabilities = missingCaps,
            redundantRoles = redundantRoles,
            weaknessDetection = weaknesses,
            suggestedImprovements = improvements
        )
    }

    // -------------------------------------------------------------------------
    // 3. SQUAD SYNERGY & COMPATIBILITY EVALUATION
    // -------------------------------------------------------------------------

    fun evaluateSquadSynergy(
        members: List<Shadow>,
        squadCategory: ShadowSquadCategory,
        bondProfiles: Map<String, ShadowBondProfile> = emptyMap(),
        sharedEventsCount: Map<String, Int> = emptyMap()
    ): SquadSynergyReport {
        if (members.isEmpty()) {
            return SquadSynergyReport(
                synergyScore = 0,
                roleCoverage = emptyMap(),
                missingRoles = squadCategory.recommendedRoles,
                complementaryPairs = emptyList(),
                tacticalOverlaps = emptyList(),
                coordinationWarnings = listOf("Squad has no members assigned."),
                recommendedAdjustments = listOf("Assign shadows aligned with the ${squadCategory.label} role profile."),
                bondHighlights = emptyList()
            )
        }

        val profiles = members.map { evaluateTacticalProfile(it) }
        val roleCoverage = mutableMapOf<ShadowTacticalRole, Int>()
        ShadowTacticalRole.entries.forEach { roleCoverage[it] = 0 }
        profiles.forEach { p ->
            roleCoverage[p.primaryRole] = (roleCoverage[p.primaryRole] ?: 0) + 1
        }

        val missingRoles = squadCategory.recommendedRoles.filter { (roleCoverage[it] ?: 0) == 0 }
        val complementaryPairs = mutableListOf<String>()
        val overlaps = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val adjustments = mutableListOf<String>()
        val bondHighlights = mutableListOf<String>()

        // Check complementary role pairings
        val hasFrontline = (roleCoverage[ShadowTacticalRole.FRONTLINE] ?: 0) > 0
        val hasAssault = (roleCoverage[ShadowTacticalRole.ASSAULT] ?: 0) > 0
        val hasRanged = (roleCoverage[ShadowTacticalRole.RANGED] ?: 0) > 0
        val hasSupport = (roleCoverage[ShadowTacticalRole.SUPPORT] ?: 0) > 0
        val hasCommand = (roleCoverage[ShadowTacticalRole.COMMAND] ?: 0) > 0
        val hasRecon = (roleCoverage[ShadowTacticalRole.RECONNAISSANCE] ?: 0) > 0

        if (hasFrontline && hasAssault) complementaryPairs.add("Frontline Anchor shields Assault Striker during line penetration")
        if (hasFrontline && hasRanged) complementaryPairs.add("Frontline perimeter secures safe fire lane for Ranged Artillery")
        if (hasSupport && hasCommand) complementaryPairs.add("Tactical Support enhances Squad Command aura propagation")
        if (hasRecon && hasAssault) complementaryPairs.add("Recon Scout identifies flank vulnerabilities for Assault Strikers")

        // Role overlaps
        roleCoverage.forEach { (role, count) ->
            if (count >= 3) {
                overlaps.add("Role redundancy: $count units assigned to ${role.label}")
            }
        }

        // Warnings
        if (missingRoles.isNotEmpty()) {
            warnings.add("Squad lacks recommended category roles: ${missingRoles.joinToString { it.label }}")
            adjustments.add("Recruit or reassign a ${missingRoles.first().label} to maximize squad effectiveness.")
        }

        // Bond highlights (informational & contextual only)
        for (i in members.indices) {
            for (j in i + 1 until members.size) {
                val sA = members[i]
                val sB = members[j]
                val pA = bondProfiles[sA.shadowId]
                val pB = bondProfiles[sB.shadowId]
                if (pA != null && pB != null) {
                    if (pA.relationshipStage.ordinal >= 3 && pB.relationshipStage.ordinal >= 3) {
                        bondHighlights.add("Veteran Bond: ${sA.name} and ${sB.name} display high Monarch loyalty and established unit trust.")
                    }
                }
            }
        }

        // Calculate Synergy Score (0–100)
        var synergy = 50
        synergy += (complementaryPairs.size * 10)
        synergy -= (missingRoles.size * 12)
        synergy -= (overlaps.size * 6)
        if (bondHighlights.isNotEmpty()) synergy += 5

        return SquadSynergyReport(
            synergyScore = synergy.coerceIn(10, 100),
            roleCoverage = roleCoverage,
            missingRoles = missingRoles,
            complementaryPairs = complementaryPairs,
            tacticalOverlaps = overlaps,
            coordinationWarnings = warnings,
            recommendedAdjustments = adjustments,
            bondHighlights = bondHighlights
        )
    }

    // -------------------------------------------------------------------------
    // 4. RECOMMENDATIONS
    // -------------------------------------------------------------------------

    fun recommendSquadComposition(
        category: ShadowSquadCategory,
        availableShadows: List<Shadow>
    ): List<Shadow> {
        val candidates = availableShadows
            .filter { it.deploymentStatus == ShadowDeploymentStatus.AVAILABLE && !it.isRetired }
            .sortedByDescending { it.calculatePowerRating() }

        val chosen = mutableListOf<Shadow>()
        val chosenIds = mutableSetOf<String>()

        // 1. Prioritize recommended roles
        for (targetRole in category.recommendedRoles) {
            val match = candidates.firstOrNull { shadow ->
                !chosenIds.contains(shadow.shadowId) &&
                        evaluateTacticalProfile(shadow).primaryRole == targetRole
            }
            if (match != null) {
                chosen.add(match)
                chosenIds.add(match.shadowId)
            }
        }

        // 2. Fill remaining slots with highest-tier available units
        for (candidate in candidates) {
            if (chosen.size >= 5) break
            if (!chosenIds.contains(candidate.shadowId)) {
                chosen.add(candidate)
                chosenIds.add(candidate.shadowId)
            }
        }

        return chosen
    }
}
