package com.example.domain.engine

import com.example.domain.model.Shadow
import com.example.domain.model.ShadowBondConfig
import com.example.domain.model.ShadowBondMilestone
import com.example.domain.model.ShadowBondProfile

// ============================================================================
// MILESTONE 12 — PHASE 6.4: SHADOW BOND AI ADVISOR
// Read-only advisory context provider for the AI companion.
//
// STRICT SAFETY RESTRICTIONS:
// - AI cannot grant Bond Points, XP, or resources.
// - AI cannot mutate loyalty, trust, or relationship stage.
// - AI cannot fabricate historical events or milestones.
// - All context is strictly derived from persisted, verified repository state.
// ============================================================================

object ShadowBondAiAdvisor {

    /**
     * Formats verified bond telemetry into a concise, sanitized context block
     * for prompt generation, respecting token limits and preventing prompt injection.
     */
    fun formatShadowBondPromptContext(
        profile: ShadowBondProfile,
        shadow: Shadow,
        milestones: List<ShadowBondMilestone> = emptyList()
    ): String = buildString {
        val sanitizedName = PromptSanitizer.sanitize(shadow.name, 40)
        val stage = profile.relationshipStage
        val personality = profile.personality
        val loyaltyDesc = ShadowBondConfig.getLoyaltyDescription(profile.loyaltyRating)
        val trustDesc = ShadowBondConfig.getTrustDescription(profile.trustRating)
        val dialogue = ShadowBondConfig.getEvolvingDialogue(personality, stage)

        appendLine("SHADOW BOND TELEMETRY (READ-ONLY):")
        appendLine("  Unit: '$sanitizedName' (${shadow.shadowClass.label}, ${shadow.rarity.label}, Lv.${shadow.level})")
        appendLine("  Relationship Stage: ${stage.stageName} ('${stage.title}') [Order ${stage.order}/6]")
        appendLine("  Bond Level: ${profile.bondLevel}/10 (${profile.bondPoints} pts)")
        appendLine("  Loyalty Rating: ${profile.loyaltyRating}/100 ($loyaltyDesc)")
        appendLine("  Trust Rating: ${profile.trustRating}/100 ($trustDesc)")
        appendLine("  Personality: ${personality.traitName} - ${personality.summary}")
        appendLine("  Monarch Reflection: \"$dialogue\"")

        if (milestones.isNotEmpty()) {
            val milestoneTitles = milestones.take(4).map { PromptSanitizer.sanitize(it.title, 30) }
            appendLine("  Verified Milestones (${milestones.size}): ${milestoneTitles.joinToString(", ")}")
        }

        stage.nextStage()?.let { next ->
            val ptsNeeded = (next.minBondPoints - profile.bondPoints).coerceAtLeast(0L)
            val loyaltyNeeded = (next.minLoyalty - profile.loyaltyRating).coerceAtLeast(0)
            val trustNeeded = (next.minTrust - profile.trustRating).coerceAtLeast(0)
            appendLine("  Next Stage Requirements (${next.stageName}): Bond Lv.${next.minBondLevel}, +$ptsNeeded pts, +$loyaltyNeeded Loyalty, +$trustNeeded Trust")
        } ?: appendLine("  Next Stage: MAX RELATIONSHIP STAGE REACHED (Unbreakable)")
    }

    /**
     * Generates a deterministic offline relationship summary without calling any external LLM.
     */
    fun generateDeterministicSummary(
        profile: ShadowBondProfile,
        shadow: Shadow
    ): String {
        val stage = profile.relationshipStage
        val personality = profile.personality
        val dialogue = ShadowBondConfig.getEvolvingDialogue(personality, stage)
        return "Shadow ${shadow.name} stands at the ${stage.stageName} relationship tier ('${stage.title}') with Loyalty ${profile.loyaltyRating}/100 and Trust ${profile.trustRating}/100. Personality trait: ${personality.traitName}. \"$dialogue\""
    }
}
