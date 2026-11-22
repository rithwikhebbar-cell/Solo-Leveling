package com.example.domain.engine

import com.example.domain.model.DominionAiBriefing
import com.example.domain.model.DominionOverview
import com.example.domain.model.ShadowDominionStage
import com.example.domain.model.ShadowTerritory
import com.example.domain.model.ShadowTerritoryState
import java.util.UUID

// ============================================================================
// PHASE 6.6 — DOMINION AI ADVISOR (Strictly Read-Only & Sanitized)
// ============================================================================

class ShadowDominionAiAdvisor {

    /**
     * Synthesizes a strategic briefing for the Shadow Monarch based on realm state.
     * Guaranteed strictly read-only: no database mutators or progression engines are accessed.
     */
    fun buildDominionBriefing(
        overview: DominionOverview?,
        territories: List<ShadowTerritory>
    ): DominionAiBriefing {
        if (overview == null) {
            return DominionAiBriefing(
                briefingId = UUID.randomUUID().toString(),
                dominionStage = ShadowDominionStage.DORMANT_REALM,
                summary = "The Shadow Dominion lies dormant. Initialize your ancestral citadel to awaken the realm.",
                strategicPriority = "Awaken the Realm",
                recommendedActions = listOf("Initialize the Shadow Monarch Citadel in the Shadow Heartland.")
            )
        }

        val dominion = overview.dominion
        val controlled = territories.filter { it.state == ShadowTerritoryState.CONTROLLED || it.state == ShadowTerritoryState.DEVELOPING || it.state == ShadowTerritoryState.FORTIFIED }
        val discovered = territories.filter { it.state == ShadowTerritoryState.DISCOVERED }
        val unguarded = controlled.filter { it.assignedSquadId == null }

        val sanitizedDominionName = PromptSanitizer.sanitize(dominion.dominionName)
        val recommendations = mutableListOf<String>()
        val strategicPriority: String

        when {
            unguarded.isNotEmpty() -> {
                strategicPriority = "Territorial Garrison Deployment"
                recommendations.add("Assign a defense squad to ${unguarded.first().name} to prevent vulnerabilities.")
                if (discovered.isNotEmpty()) {
                    recommendations.add("Explore adjacent uncharted land: ${discovered.first().name}.")
                }
            }
            overview.totalResourceYieldPerHour < 50 -> {
                strategicPriority = "Resource Infrastructure Expansion"
                recommendations.add("Construct or upgrade Resource Extractors to increase hourly Shadow Essence yield.")
                recommendations.add("Research Energy Conduits in Shadow Logistics to boost extraction efficiency.")
            }
            dominion.citadelLevel < 3 -> {
                strategicPriority = "Citadel Fortification & Authority"
                recommendations.add("Upgrade Citadel wings (Command Hall, Shadow Nexus) to expand realm authority.")
                recommendations.add("Unlock advanced research projects in Dominion Architecture.")
            }
            discovered.isNotEmpty() -> {
                strategicPriority = "Frontier Exploration & Subjugation"
                recommendations.add("Dispatch an expeditionary squad to survey ${discovered.first().name}.")
                recommendations.add("Upgrade the Exploration Gate to reduce expedition scouting times.")
            }
            else -> {
                strategicPriority = "Sovereignty & Empire Consolidation"
                recommendations.add("Fortify all remaining controlled territories with Defense Towers.")
                recommendations.add("Complete apex research projects to advance toward the Shadow Monarch's Empire.")
            }
        }

        val summary = "Dominion [$sanitizedDominionName] stands at Stage ${dominion.developmentStage.stageName} (Citadel Lv ${dominion.citadelLevel}). " +
                "Controlled Territories: ${controlled.size}/${territories.size}. Active Yield: ${overview.totalResourceYieldPerHour}/hr. " +
                "Strategic Focus: $strategicPriority."

        return DominionAiBriefing(
            briefingId = UUID.randomUUID().toString(),
            dominionStage = dominion.developmentStage,
            summary = summary,
            strategicPriority = strategicPriority,
            recommendedActions = recommendations
        )
    }
}
