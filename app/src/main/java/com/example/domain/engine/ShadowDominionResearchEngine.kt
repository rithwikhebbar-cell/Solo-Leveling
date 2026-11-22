package com.example.domain.engine

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.ShadowDominionDao
import com.example.data.local.dao.ShadowResourceDao
import com.example.data.local.entity.ShadowDominionActivityEntity
import com.example.data.local.entity.ShadowDominionResearchEntity
import com.example.data.local.entity.ShadowResourceTransactionEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.DominionActivityType
import com.example.domain.model.ShadowDominionResearch
import com.example.domain.model.ShadowResearchBranch
import com.example.domain.model.ShadowResourceTransactionType
import com.example.domain.model.ShadowResourceType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

// ============================================================================
// PHASE 6.6 — SHADOW DOMINION RESEARCH & DEVELOPMENT ENGINE
// ============================================================================

class ShadowDominionResearchEngine(
    private val database: SoloLevelingDatabase?,
    private val shadowDominionDao: ShadowDominionDao,
    private val shadowResourceDao: ShadowResourceDao,
    private val shadowDominionEngine: ShadowDominionEngine,
    private val mutex: Mutex = Mutex()
) {

    data class ResearchProjectDefinition(
        val projectKey: String,
        val branch: ShadowResearchBranch,
        val title: String,
        val description: String,
        val level: Int,
        val costs: Map<ShadowResourceType, Int>,
        val prerequisiteKey: String?,
        val unlockedEffectsSummary: String
    )

    companion object {
        val CATALOG: List<ResearchProjectDefinition> = listOf(
            // Branch 1: DOMINION ARCHITECTURE
            ResearchProjectDefinition(
                projectKey = "arch_fortified_foundations",
                branch = ShadowResearchBranch.DOMINION_ARCHITECTURE,
                title = "Fortified Foundations",
                description = "Reinforces territory soil and infrastructure stability.",
                level = 1,
                costs = mapOf(ShadowResourceType.SHADOW_ESSENCE to 200),
                prerequisiteKey = null,
                unlockedEffectsSummary = "Increases all structure durability and allows Tier 2 Defense Towers."
            ),
            ResearchProjectDefinition(
                projectKey = "arch_reinforced_masonry",
                branch = ShadowResearchBranch.DOMINION_ARCHITECTURE,
                title = "Reinforced Masonry",
                description = "Shadow stone masonry techniques for Citadel fortification.",
                level = 2,
                costs = mapOf(ShadowResourceType.SHADOW_ESSENCE to 400, ShadowResourceType.SHADOW_CRYSTALS to 20),
                prerequisiteKey = "arch_fortified_foundations",
                unlockedEffectsSummary = "Reduces construction costs by 15% across all territories."
            ),
            // Branch 2: SHADOW LOGISTICS
            ResearchProjectDefinition(
                projectKey = "logistics_energy_conduits",
                branch = ShadowResearchBranch.SHADOW_LOGISTICS,
                title = "Energy Conduits",
                description = "Channels ley-line shadow energy directly into storage extractors.",
                level = 1,
                costs = mapOf(ShadowResourceType.SHADOW_ESSENCE to 250),
                prerequisiteKey = null,
                unlockedEffectsSummary = "Boosts Resource Extractor output by +25%."
            ),
            ResearchProjectDefinition(
                projectKey = "logistics_refined_extraction",
                branch = ShadowResearchBranch.SHADOW_LOGISTICS,
                title = "Refined Extraction",
                description = "Advanced filtration yielding purer crystallized essence.",
                level = 2,
                costs = mapOf(ShadowResourceType.SHADOW_ESSENCE to 450, ShadowResourceType.MONARCH_FRAGMENTS to 15),
                prerequisiteKey = "logistics_energy_conduits",
                unlockedEffectsSummary = "Unlocks intermittent Shadow Crystal yield from standard extractors."
            ),
            // Branch 3: TERRITORIAL DEFENSE
            ResearchProjectDefinition(
                projectKey = "defense_shadow_wards",
                branch = ShadowResearchBranch.TERRITORIAL_DEFENSE,
                title = "Shadow Wards",
                description = "Barrier sigils that slow and deter hostile trespassing.",
                level = 1,
                costs = mapOf(ShadowResourceType.SHADOW_ESSENCE to 180, ShadowResourceType.ABYSSAL_MATERIALS to 10),
                prerequisiteKey = null,
                unlockedEffectsSummary = "Adds +20 Base Defense Rating to all controlled territories."
            ),
            // Branch 4: RESOURCE MANAGEMENT
            ResearchProjectDefinition(
                projectKey = "res_crystal_synthesis",
                branch = ShadowResearchBranch.RESOURCE_MANAGEMENT,
                title = "Crystal Synthesis",
                description = "Synthesizes ambient essence into solid mana crystals.",
                level = 1,
                costs = mapOf(ShadowResourceType.SHADOW_ESSENCE to 300, ShadowResourceType.EVOLUTION_CORES to 1),
                prerequisiteKey = null,
                unlockedEffectsSummary = "Unlocks crystal refinement in Resource Vault."
            ),
            // Branch 5: EXPLORATION TECHNOLOGY
            ResearchProjectDefinition(
                projectKey = "exp_veil_piercer",
                branch = ShadowResearchBranch.EXPLORATION_TECHNOLOGY,
                title = "Veil Piercer",
                description = "Scouting lenses piercing the impenetrable abyssal fog.",
                level = 1,
                costs = mapOf(ShadowResourceType.SHADOW_ESSENCE to 220),
                prerequisiteKey = null,
                unlockedEffectsSummary = "Discovers hidden adjacent territories automatically."
            ),
            // Branch 6: ARMY COORDINATION
            ResearchProjectDefinition(
                projectKey = "army_telepathic_web",
                branch = ShadowResearchBranch.ARMY_COORDINATION,
                title = "Telepathic Web",
                description = "Unites stationed garrisons with the Monarch's personal telepathic consciousness.",
                level = 1,
                costs = mapOf(ShadowResourceType.SHADOW_ESSENCE to 350, ShadowResourceType.MONARCH_FRAGMENTS to 20),
                prerequisiteKey = null,
                unlockedEffectsSummary = "Increases garrison combat effectiveness and response alerts."
            )
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

    /**
     * Completes a research project if prerequisites and resource balances are satisfied.
     * Transactionally deducts costs and records research completion.
     */
    suspend fun conductResearch(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        projectKey: String
    ): Result<ShadowDominionResearch> {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        return runCatching {
            withLockedTransaction {
                val def = CATALOG.find { it.projectKey == projectKey }
                    ?: throw IllegalArgumentException("Unknown research project: $projectKey")

                val existing = shadowDominionDao.getResearchByProjectKey(owner, projectKey)
                if (existing != null && existing.isCompleted) {
                    throw IllegalStateException("Research project already completed: ${def.title}")
                }

                // Check prerequisite
                if (def.prerequisiteKey != null) {
                    val prereq = shadowDominionDao.getResearchByProjectKey(owner, def.prerequisiteKey)
                    if (prereq == null || !prereq.isCompleted) {
                        throw IllegalStateException("Prerequisite research required: ${def.prerequisiteKey}")
                    }
                }

                // Check resource costs
                for ((resType, amount) in def.costs) {
                    val cur = shadowResourceDao.getCurrentBalanceForOwner(owner, resType.name)
                    if (cur < amount) {
                        throw IllegalStateException("Insufficient ${resType.label}. Required: $amount, Available: $cur")
                    }
                }

                val now = System.currentTimeMillis()
                val eventId = "res_${projectKey}_${UUID.randomUUID()}"

                // Deduct resources
                for ((resType, amount) in def.costs) {
                    val cur = shadowResourceDao.getCurrentBalanceForOwner(owner, resType.name)
                    shadowResourceDao.insertTransaction(
                        ShadowResourceTransactionEntity(
                            transactionId = UUID.randomUUID().toString(),
                            accountOwnerId = owner,
                            resourceTypeName = resType.name,
                            amount = -amount,
                            transactionTypeName = ShadowResourceTransactionType.CONSUMED.name,
                            sourceEventId = eventId,
                            sourceDescription = "Researched: ${def.title}",
                            balanceAfter = cur - amount,
                            timestampEpochMs = now
                        )
                    )
                }

                val researchRecord = ShadowDominionResearch(
                    researchId = "res_${projectKey}_$owner",
                    accountOwnerId = owner,
                    branch = def.branch,
                    projectKey = def.projectKey,
                    title = def.title,
                    description = def.description,
                    level = def.level,
                    isCompleted = true,
                    startedAtEpochMs = now,
                    completedAtEpochMs = now,
                    unlockedEffectsSummary = def.unlockedEffectsSummary
                )

                shadowDominionDao.upsertResearch(researchRecord.toEntity())

                shadowDominionDao.insertActivity(
                    ShadowDominionActivityEntity(
                        activityId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        activityTypeName = DominionActivityType.RESEARCH_COMPLETED.name,
                        title = "Research Completed: ${def.title}",
                        description = "Unlocked ${def.title} in the ${def.branch.label} branch.",
                        timestampEpochMs = now,
                        entityReferenceId = researchRecord.researchId
                    )
                )

                // Re-evaluate dominion progression
                shadowDominionEngine.reevaluateDominionStage(owner)

                researchRecord
            }
        }
    }
}
