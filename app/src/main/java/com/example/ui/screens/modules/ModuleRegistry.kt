package com.example.ui.screens.modules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import com.example.ui.theme.SystemColors

object ModuleRegistry {

    fun buildInventory(
        onOpenAcademicEngine: () -> Unit,
        onOpenDungeonGate: () -> Unit,
        onOpenSkills: () -> Unit,
        onOpenAchievements: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenSettings: () -> Unit,
        onOpenAiSystem: () -> Unit,
        onOpenLifeEngine: () -> Unit = {},
        onOpenBossBattles: () -> Unit = {},
        onOpenTitles: () -> Unit = {},
        onOpenInventory: () -> Unit = {},
        onOpenShadowCollection: () -> Unit = {},
        onOpenRpgRewardHistory: () -> Unit = {},
        onOpenCloudSync: () -> Unit = {},
        onOpenIntelligenceCenter: () -> Unit = {},
        onOpenSystemControl: () -> Unit = {},
        onOpenAiCompanion: () -> Unit = {},
        onOpenVoiceConsole: () -> Unit = {},
        onOpenSystemVoiceSettings: () -> Unit = {},
        onOpenWidgetHub: () -> Unit = {},
        onOpenCelebrationVault: () -> Unit = {},
        onOpenShadowRealm: () -> Unit = {},
        onOpenSocialMastery: () -> Unit = {},
        onOpenPenaltyStatus: () -> Unit = {},
        onOpenPenaltyAudit: () -> Unit = {}
    ): List<ModuleSpec> {
        return listOf(
            // ================================================================
            // 1. PLAYER DEVELOPMENT
            // ================================================================
            ModuleSpec(
                id = "academic_engine",
                title = "Academic Engine",
                description = "Subject syllabus mastery, spaced repetition revision schedules & focused study blocks.",
                capabilitySummary = "Spaced repetition intervals, syllabus breakdown, revision calendar & verified study logs.",
                category = ModuleCategory.DEVELOPMENT,
                status = ModuleOperationalStatus.ACTIVE,
                statusRationale = "Core study engine active with live revision triggers.",
                icon = Icons.Rounded.School,
                accentColor = SystemColors.ElectricCyan,
                metricHighlight = "Syllabus Mastery & Study Timer",
                destinationLabel = "Academic Dashboard",
                onClick = onOpenAcademicEngine
            ),
            ModuleSpec(
                id = "life_engine",
                title = "Life & Discipline OS",
                description = "Habit tracker, morning/evening routines, personal tasks, goal milestones & unified calendar.",
                capabilitySummary = "Deterministic habit completion, streak multiplier rules, routine timelines & personal milestones.",
                category = ModuleCategory.DEVELOPMENT,
                status = ModuleOperationalStatus.ACTIVE,
                statusRationale = "Discipline tracking and routine scheduling running live.",
                icon = Icons.Rounded.CalendarToday,
                accentColor = SystemColors.VerifiedEmerald,
                metricHighlight = "Discipline Score & Habits",
                destinationLabel = "Life Dashboard",
                onClick = onOpenLifeEngine
            ),
            ModuleSpec(
                id = "social_mastery",
                title = "Social Mastery",
                description = "8 communication modules, speech rehearsal, live conversational scenarios & charisma telemetry.",
                capabilitySummary = "Scenario audio evaluator, charisma attribute progression, speech rate analysis & rehearsal vault.",
                category = ModuleCategory.DEVELOPMENT,
                status = ModuleOperationalStatus.ACTIVE,
                statusRationale = "Communication engine and scenario modules ready.",
                icon = Icons.Rounded.RecordVoiceOver,
                accentColor = SystemColors.ElectricCyan,
                metricHighlight = "Speech Rehearsal & Scenarios",
                destinationLabel = "Social Mastery Hub",
                onClick = onOpenSocialMastery
            ),

            // ================================================================
            // 2. PROGRESSION SYSTEMS
            // ================================================================
            ModuleSpec(
                id = "shadow_realm",
                title = "The Shadow Realm",
                description = "Shadow Monarch progression, Shadow Extraction from verified milestones, army formations & deployments.",
                capabilitySummary = "Monarch rank progression, extraction evidence auditing, soldier bond attributes & active deployments.",
                category = ModuleCategory.PROGRESSION,
                status = ModuleOperationalStatus.ACTIVE,
                statusRationale = "Shadow Monarch engine and extraction pipeline operational.",
                icon = Icons.Rounded.Bolt,
                accentColor = SystemColors.ArcaneViolet,
                metricHighlight = "Monarch Rank & Soldier Army",
                destinationLabel = "Shadow Realm Dashboard",
                onClick = onOpenShadowRealm
            ),
            ModuleSpec(
                id = "dungeon_gate",
                title = "Dungeon Gate",
                description = "Rank-gated physical & discipline endurance trials with live verification.",
                capabilitySummary = "Trial progression formulas, rank prerequisites, gate timer verification & clear rewards.",
                category = ModuleCategory.PROGRESSION,
                status = ModuleOperationalStatus.AVAILABLE,
                statusRationale = "Dungeon trials available based on current hunter rank.",
                icon = Icons.Rounded.Security,
                accentColor = SystemColors.DangerCrimson,
                metricHighlight = "Rank-Gated Endurance Trials",
                destinationLabel = "Dungeon Gate Dashboard",
                onClick = onOpenDungeonGate
            ),
            ModuleSpec(
                id = "boss_battles",
                title = "Boss Battles",
                description = "Multi-stage raid campaigns with deterministic boss HP based on real-world milestones.",
                capabilitySummary = "Boss health pools, phase damage calculations, milestone-gated raid mechanics & loot.",
                category = ModuleCategory.PROGRESSION,
                status = ModuleOperationalStatus.AVAILABLE,
                statusRationale = "Raid instances accessible for active milestone challenges.",
                icon = Icons.Rounded.Shield,
                accentColor = SystemColors.DangerCrimson,
                metricHighlight = "Deterministic Raid Campaigns",
                destinationLabel = "Boss Battles Hub",
                onClick = onOpenBossBattles
            ),
            ModuleSpec(
                id = "skills",
                title = "Skills & Trees",
                description = "5 specialization skill trees evaluated from real player attributes.",
                capabilitySummary = "Passive gameplay perks, attribute thresholds, specialization nodes & active skill masteries.",
                category = ModuleCategory.PROGRESSION,
                status = ModuleOperationalStatus.AVAILABLE,
                statusRationale = "Skill trees unlocked based on seven canonical attribute thresholds.",
                icon = Icons.Rounded.Bolt,
                accentColor = SystemColors.SystemBlue,
                metricHighlight = "5 Specialization Attribute Trees",
                destinationLabel = "Skills Screen",
                onClick = onOpenSkills
            ),
            ModuleSpec(
                id = "achievements",
                title = "Achievements",
                description = "10 milestone categories with cryptographic and verified evidence tracking.",
                capabilitySummary = "Verified activity counters, milestone badges, XP reward allocations & unlock ledger.",
                category = ModuleCategory.PROGRESSION,
                status = ModuleOperationalStatus.AVAILABLE,
                statusRationale = "Milestones unlocked dynamically upon reaching verifiable milestones.",
                icon = Icons.Rounded.EmojiEvents,
                accentColor = SystemColors.MonarchGold,
                metricHighlight = "Verified Milestone Evidence",
                destinationLabel = "Achievements Screen",
                onClick = onOpenAchievements
            ),
            ModuleSpec(
                id = "titles",
                title = "Player Titles",
                description = "Equippable RPG honorifics granting visual prestige and status reflection.",
                capabilitySummary = "Title collection inventory, rarity tiering, equip state persistence & Status Window sync.",
                category = ModuleCategory.PROGRESSION,
                status = ModuleOperationalStatus.AVAILABLE,
                statusRationale = "Unlocked honorifics synchronized with Player Profile.",
                icon = Icons.Rounded.MilitaryTech,
                accentColor = SystemColors.MonarchGold,
                metricHighlight = "Equipped Status Honorifics",
                destinationLabel = "Title Collection",
                onClick = onOpenTitles
            ),
            ModuleSpec(
                id = "inventory",
                title = "Player Inventory",
                description = "Cosmetics, holographic HUD frames, system themes, badges & artifacts.",
                capabilitySummary = "Inventory grid, item rarity inspection, cosmetic equipping & item provenance.",
                category = ModuleCategory.PROGRESSION,
                status = ModuleOperationalStatus.AVAILABLE,
                statusRationale = "Local item storage and theme customization active.",
                icon = Icons.Rounded.Inventory2,
                accentColor = SystemColors.ElectricCyan,
                metricHighlight = "Cosmetic Artifacts & Themes",
                destinationLabel = "Inventory Screen",
                onClick = onOpenInventory
            ),
            ModuleSpec(
                id = "shadow_vault",
                title = "Shadow Vault",
                description = "Geometric shadow constructs, shadow soldier archive & milestone guardians.",
                capabilitySummary = "Construct archive, milestone guardian inspect, shadow lore & extraction codex.",
                category = ModuleCategory.PROGRESSION,
                status = ModuleOperationalStatus.AVAILABLE,
                statusRationale = "Collection archive accessible.",
                icon = Icons.Rounded.AutoAwesome,
                accentColor = SystemColors.ArcaneViolet,
                metricHighlight = "Guardian Archive & Constructs",
                destinationLabel = "Shadow Collection Vault",
                onClick = onOpenShadowCollection
            ),
            ModuleSpec(
                id = "celebration_vault",
                title = "Celebration Vault",
                description = "Replayable verified Level-Up and Rank Promotion cinematic milestones.",
                capabilitySummary = "Cinematic ceremony replay, rank transition timeline, historical snapshots & sound effects.",
                category = ModuleCategory.PROGRESSION,
                status = ModuleOperationalStatus.AVAILABLE,
                statusRationale = "Ascension records archived and replayable on demand.",
                icon = Icons.Rounded.EmojiEvents,
                accentColor = SystemColors.MonarchGold,
                metricHighlight = "Milestone Replay Vault",
                destinationLabel = "Celebration Vault",
                onClick = onOpenCelebrationVault
            ),
            ModuleSpec(
                id = "rpg_reward_history",
                title = "RPG Reward Ledger",
                description = "Daily RPG XP cap meter, anti-farming audit log & verified session transactions.",
                capabilitySummary = "Daily XP budget tracking, diminishing returns algorithm, transaction ledger & audit trail.",
                category = ModuleCategory.PROGRESSION,
                status = ModuleOperationalStatus.AVAILABLE,
                statusRationale = "Audit log active ensuring legitimate progression pacing.",
                icon = Icons.Rounded.ReceiptLong,
                accentColor = SystemColors.VerifiedEmerald,
                metricHighlight = "XP Cap & Anti-Farming Audit",
                destinationLabel = "RPG Reward Ledger",
                onClick = onOpenRpgRewardHistory
            ),

            // ================================================================
            // 3. SYSTEM & INTELLIGENCE
            // ================================================================
            ModuleSpec(
                id = "discipline_penalty",
                title = "Discipline & Penalty",
                description = "Accountability ledger, deterministic failure evaluations, penalty tasks & redemption gates.",
                capabilitySummary = "Quest deadline evaluations, penalty severity escalation, redemption tasks & ledger audit.",
                category = ModuleCategory.SYSTEM_AI,
                status = ModuleOperationalStatus.ACTIVE,
                statusRationale = "Failure detection engine and redemption gates running live.",
                icon = Icons.Rounded.Security,
                accentColor = SystemColors.ArcaneViolet,
                metricHighlight = "Ledger & Atonement Protocols",
                destinationLabel = "Penalty Status Hub",
                onClick = onOpenPenaltyStatus
            ),
            ModuleSpec(
                id = "penalty_audit",
                title = "Penalty Audit & Analytics",
                description = "Analytics KPIs, friction pattern detection, ledger history & exemption gateway.",
                capabilitySummary = "Friction patterns, historical compliance curves, exemption logs & mitigation actions.",
                category = ModuleCategory.SYSTEM_AI,
                status = ModuleOperationalStatus.ACTIVE,
                statusRationale = "Penalty analytics and friction models active.",
                icon = Icons.Rounded.ReceiptLong,
                accentColor = SystemColors.ElectricCyan,
                metricHighlight = "Analytics KPIs & Pattern Detection",
                destinationLabel = "Penalty Analytics Hub",
                onClick = onOpenPenaltyAudit
            ),
            ModuleSpec(
                id = "intelligence_center",
                title = "Intelligence Center",
                description = "Adaptive planning, Bayesian predictions, progression roadmaps & intelligence Q&A.",
                capabilitySummary = "Adaptive workload prediction, domain balance heuristics, bottleneck detection & coaching advice.",
                category = ModuleCategory.SYSTEM_AI,
                status = ModuleOperationalStatus.ACTIVE,
                statusRationale = "Player context aggregator and prediction engines active.",
                icon = Icons.Rounded.Psychology,
                accentColor = SystemColors.ElectricCyan,
                metricHighlight = "Adaptive Roadmaps & Analytics",
                destinationLabel = "Intelligence Dashboard",
                onClick = onOpenIntelligenceCenter
            ),
            ModuleSpec(
                id = "ai_companion",
                title = "AI System Companion",
                description = "Holographic interactive guidance, Game Master advisory & privacy-safe Q&A.",
                capabilitySummary = "On-device reasoning, contextual player briefings, role-aware dialogue & coaching suggestions.",
                category = ModuleCategory.SYSTEM_AI,
                status = ModuleOperationalStatus.ACTIVE,
                statusRationale = "Conversational assistant initialized and responsive.",
                icon = Icons.Rounded.AutoAwesome,
                accentColor = SystemColors.ElectricCyan,
                metricHighlight = "Game Master Advisory",
                destinationLabel = "AI Companion Screen",
                onClick = onOpenAiCompanion
            ),
            ModuleSpec(
                id = "voice_console",
                title = "Voice Command Console",
                description = "Tap-to-speak allowlisted system navigation, command confirmation & audio HUD.",
                capabilitySummary = "Deterministic speech grammar parsing, voice-guided screen navigation & confirmation chimes.",
                category = ModuleCategory.SYSTEM_AI,
                status = ModuleOperationalStatus.ACTIVE,
                statusRationale = "Voice recognizer bound to allowlisted command grammar.",
                icon = Icons.Rounded.RecordVoiceOver,
                accentColor = SystemColors.ElectricCyan,
                metricHighlight = "Speech Navigation Console",
                destinationLabel = "Voice Command Console",
                onClick = onOpenVoiceConsole
            ),
            ModuleSpec(
                id = "voice_settings",
                title = "System Voice & Alerts",
                description = "Native Android TTS engine, priority voice broadcasts & custom speech rate controls.",
                capabilitySummary = "Text-to-speech voice selector, speech rate pitch tuning, announcement channels & preview.",
                category = ModuleCategory.SYSTEM_AI,
                status = ModuleOperationalStatus.ACTIVE,
                statusRationale = "Android TTS synthesizer active and configured.",
                icon = Icons.Rounded.Psychology,
                accentColor = SystemColors.ArcaneViolet,
                metricHighlight = "TTS Broadcast Engine",
                destinationLabel = "System Voice Settings",
                onClick = onOpenSystemVoiceSettings
            ),
            ModuleSpec(
                id = "system_control",
                title = "System Control Center",
                description = "Subsystem health diagnostics, WorkManager automation, daily cycle scheduler & integrity checks.",
                capabilitySummary = "Periodic sync workers, background health telemetry, database verification & scheduler controls.",
                category = ModuleCategory.SYSTEM_AI,
                status = ModuleOperationalStatus.ACTIVE,
                statusRationale = "System daemons and diagnostic probes operational.",
                icon = Icons.Rounded.Shield,
                accentColor = SystemColors.ElectricCyan,
                metricHighlight = "Subsystem Health & Daemons",
                destinationLabel = "System Overview",
                onClick = onOpenSystemControl
            ),
            ModuleSpec(
                id = "cloud_sync",
                title = "Cloud Sync & Identity",
                description = "Supabase multi-device synchronization, conflict resolution & cryptographic backup.",
                capabilitySummary = "Real-time sync socket, conflict resolution policies, identity tokens & backup export.",
                category = ModuleCategory.SYSTEM_AI,
                status = ModuleOperationalStatus.ACTIVE,
                statusRationale = "Cloud synchronization layer linked to local Room database.",
                icon = Icons.Rounded.CloudSync,
                accentColor = SystemColors.ElectricCyan,
                metricHighlight = "Multi-Device State Port",
                destinationLabel = "Cloud Sync Dashboard",
                onClick = onOpenCloudSync
            ),
            ModuleSpec(
                id = "widget_hub",
                title = "Home-Screen Widgets",
                description = "4 Glance home-screen widgets providing real-time HUD metrics on launcher.",
                capabilitySummary = "Status glance widget, daily quests widget, streak widget & quick workout glance widget.",
                category = ModuleCategory.SYSTEM_AI,
                status = ModuleOperationalStatus.AVAILABLE,
                statusRationale = "Glance widget providers registered and active.",
                icon = Icons.Rounded.Shield,
                accentColor = SystemColors.VerifiedEmerald,
                metricHighlight = "Glance Launcher HUD",
                destinationLabel = "Widget Configuration Hub",
                onClick = onOpenWidgetHub
            ),
            ModuleSpec(
                id = "ai_system",
                title = "AI System",
                description = "Game Master advisory & OpenRouter generative analysis port.",
                capabilitySummary = "System prompt orchestrator, model parameter tuning & structured JSON schema generation.",
                category = ModuleCategory.SYSTEM_AI,
                status = ModuleOperationalStatus.ACTIVE,
                statusRationale = "Remote LLM inference port operational.",
                icon = Icons.Rounded.AutoAwesome,
                accentColor = SystemColors.ArcaneViolet,
                metricHighlight = "LLM Inference Port",
                destinationLabel = "AI System Screen",
                onClick = onOpenAiSystem
            ),
            ModuleSpec(
                id = "activity_history",
                title = "Activity History",
                description = "Chronological audit log of verified physical sessions, study blocks & quest completions.",
                capabilitySummary = "Paged chronological query, activity categorization, duration filters & verification provenance.",
                category = ModuleCategory.SYSTEM_AI,
                status = ModuleOperationalStatus.AVAILABLE,
                statusRationale = "Historical ledger recorded in local database.",
                icon = Icons.Rounded.History,
                accentColor = SystemColors.VerifiedEmerald,
                metricHighlight = "Activity & XP Ledger",
                destinationLabel = "History Screen",
                onClick = onOpenHistory
            ),
            ModuleSpec(
                id = "settings",
                title = "System Settings",
                description = "Player codename, DataStore preferences, HUD glow toggles & biometric ports.",
                capabilitySummary = "DataStore preferences, theme selection, reduced motion toggle & developer diagnostic tools.",
                category = ModuleCategory.SYSTEM_AI,
                status = ModuleOperationalStatus.AVAILABLE,
                statusRationale = "System preferences accessible.",
                icon = Icons.Rounded.Settings,
                accentColor = SystemColors.MetallicSilver,
                metricHighlight = "Configuration & HUD Switches",
                destinationLabel = "Settings Screen",
                onClick = onOpenSettings
            )
        )
    }
}
