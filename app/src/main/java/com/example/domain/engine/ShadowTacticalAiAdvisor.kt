package com.example.domain.engine

import com.example.data.integration.PromptSanitizer
import com.example.domain.model.FormationTacticalAssessment
import com.example.domain.model.MissionSuitabilityEvaluation
import com.example.domain.model.ShadowSquad
import com.example.domain.model.ShadowTacticalAnalytics
import com.example.domain.model.ShadowTacticalProfile
import com.example.domain.model.SquadSynergyReport

// ============================================================================
// SHADOW TACTICAL AI ADVISOR (READ-ONLY CONTEXT & ADVICE GENERATOR)
// Provides strategic recommendations and tactical situational assessments.
// STRICT ARCHITECTURAL CONSTRAINT:
// This advisor is completely read-only. It possesses zero mutation authority.
// It cannot deploy shadows, mutate squads, grant XP, or alter database records.
// ============================================================================

class ShadowTacticalAiAdvisor {

    fun formatTacticalBriefing(
        squads: List<ShadowSquad>,
        activeFormationAssessment: FormationTacticalAssessment?,
        missionEvaluations: List<MissionSuitabilityEvaluation>,
        analytics: ShadowTacticalAnalytics?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("### [MONARCH TACTICAL COMMAND INTELLIGENCE BRIEFING]")
        sb.appendLine("Status: Real-time Tactical Analysis (Read-Only)")
        sb.appendLine()

        // 1. Squad Overview
        sb.appendLine("Active Tactical Squads: ${squads.size}")
        squads.forEach { squad ->
            val leaderNote = if (squad.leaderShadowId != null) " | Leader Assigned" else " | No Leader"
            sb.appendLine("- ${squad.name} (${squad.category.label}) | Members: ${squad.memberCount}/${squad.maxMembers}$leaderNote")
        }
        sb.appendLine()

        // 2. Formation Assessment
        if (activeFormationAssessment != null) {
            sb.appendLine("Active Formation: ${activeFormationAssessment.formationName} (${activeFormationAssessment.formationType.label})")
            sb.appendLine("Tactical Balance: ${activeFormationAssessment.tacticalBalance} | Strength: ${activeFormationAssessment.strengthScore}/100")
            if (activeFormationAssessment.missingCapabilities.isNotEmpty()) {
                sb.appendLine("Vulnerabilities: ${activeFormationAssessment.missingCapabilities.joinToString()}")
            }
            if (activeFormationAssessment.suggestedImprovements.isNotEmpty()) {
                sb.appendLine("Recommendation: ${activeFormationAssessment.suggestedImprovements.first()}")
            }
            sb.appendLine()
        }

        // 3. Mission Readiness
        if (missionEvaluations.isNotEmpty()) {
            sb.appendLine("Pending Operations Analysis:")
            missionEvaluations.take(3).forEach { eval ->
                val squadName = eval.recommendedSquadName ?: "Unassigned"
                sb.appendLine("- ${eval.mission.missionTitle} (${eval.mission.difficultyRank}): Match Score ${eval.suitabilityScore}/100 with $squadName")
                if (eval.riskFactors.isNotEmpty()) {
                    sb.appendLine("  Risk: ${eval.riskFactors.first()}")
                }
            }
            sb.appendLine()
        }

        // 4. Analytics
        if (analytics != null && analytics.totalDeployments > 0) {
            sb.appendLine("Combat Deployment Record:")
            sb.appendLine("- Total Deployments: ${analytics.totalDeployments} (Success Rate: ${"%.1f".format(analytics.deploymentSuccessRate * 100)}%)")
            val topShadows = analytics.mostDeployedShadows.take(3).map { "${it.first} (${it.second})" }
            if (topShadows.isNotEmpty()) {
                sb.appendLine("- Most Utilized Shadows: ${topShadows.joinToString()}")
            }
        }

        return PromptSanitizer.sanitizeForPrompt(sb.toString())
    }

    fun generateDeterministicAdvice(
        activeSquad: ShadowSquad?,
        synergy: SquadSynergyReport?,
        formation: FormationTacticalAssessment?
    ): String {
        if (activeSquad == null) {
            return "No active squad designated. Form an Assault or Defensive squad in the Command Center to organize your forces."
        }

        val adviceList = mutableListOf<String>()

        if (synergy != null) {
            if (synergy.missingRoles.isNotEmpty()) {
                adviceList.add("Squad '${activeSquad.name}' is missing ${synergy.missingRoles.joinToString { it.label }}. Rebalancing roles is advised.")
            }
            if (synergy.complementaryPairs.isNotEmpty()) {
                adviceList.add("Strong synergy identified: ${synergy.complementaryPairs.first()}.")
            }
        }

        if (formation != null && formation.weaknessDetection.isNotEmpty()) {
            adviceList.add("Formation Advisory: ${formation.weaknessDetection.first()}")
        }

        return if (adviceList.isEmpty()) {
            "Army readiness is optimal. Formation and squad synergy are balanced for upcoming dungeon expeditions."
        } else {
            adviceList.joinToString(" ")
        }
    }
}
