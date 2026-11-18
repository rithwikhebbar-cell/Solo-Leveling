package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AcademicDashboardSummary
import com.example.domain.model.AchievementWithStatus
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.HunterRank
import com.example.domain.model.LifeDashboardSummary
import com.example.domain.model.PlayerTitleDefinition
import com.example.domain.model.RecoveryStatus
import com.example.ui.components.AttributeRadarChart
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.RankBadge
import com.example.ui.components.StatusStatPill
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RealityOsUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusScreen(
    state: RealityOsUiState.Ready,
    modifier: Modifier = Modifier,
    academicSummary: AcademicDashboardSummary = AcademicDashboardSummary(),
    lifeSummary: LifeDashboardSummary = LifeDashboardSummary(),
    unlockedTitles: List<PlayerTitleDefinition> = emptyList(),
    unlockedAchievements: List<AchievementWithStatus> = emptyList(),
    onOpenTitles: () -> Unit = {},
    onOpenAchievements: () -> Unit = {}
) {
    val player = state.player
    val nextRank = state.nextRankProgress
    val attributes = player.attributes.asList()
    val equippedTitle = unlockedTitles.firstOrNull { it.isEquipped }?.name ?: player.title

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("status_screen_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. STATUS WINDOW HERO
            item {
                HolographicPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = SystemColors.rankAccent(player.rank),
                    secondaryColor = SystemColors.ElectricCyan,
                    isHero = true,
                    contentPadding = 18.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "STATUS WINDOW",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SystemColors.ElectricCyan
                                )
                                Text(
                                    text = player.codename,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = SystemColors.PureIce
                                )
                                Text(
                                    text = "${player.rank.displayName} • $equippedTitle",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = SystemColors.rankAccent(player.rank)
                                )
                            }
                            RankBadge(rank = player.rank)
                        }

                        // Stat Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatusStatPill(
                                label = "LEVEL",
                                value = "${player.level}",
                                modifier = Modifier.weight(1f),
                                accentColor = SystemColors.ElectricCyan
                            )
                            StatusStatPill(
                                label = "TOTAL XP",
                                value = "${player.totalXp}",
                                modifier = Modifier.weight(1f),
                                accentColor = SystemColors.MonarchGold
                            )
                            StatusStatPill(
                                label = "TOTAL ATTR",
                                value = "${player.attributes.totalScore}",
                                modifier = Modifier.weight(1f),
                                accentColor = SystemColors.ArcaneViolet
                            )
                            StatusStatPill(
                                label = "RECOVERY",
                                value = "${player.recoveryScore}%",
                                modifier = Modifier.weight(1f),
                                accentColor = SystemColors.VerifiedEmerald
                            )
                        }

                        // Level XP Progress
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "LEVEL ${player.level} -> ${player.level + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemColors.MutedSilver
                                )
                                Text(
                                    text = "${player.currentLevelXp} / ${player.xpToNextLevel} XP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SystemColors.ElectricCyan
                                )
                            }
                            GlowingProgressBar(progress = player.levelProgressFraction, height = 8.dp)
                        }
                    }
                }
            }

            // 2. SEVEN-ATTRIBUTE PROFILE HEADER & RADAR
            item {
                SystemSectionTitle(
                    title = "Seven-Attribute Profile",
                    subtitle = "Deterministic progression only — direct editing locked"
                )
            }

            item {
                HolographicPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = SystemColors.ElectricCyan,
                    secondaryColor = SystemColors.ArcaneViolet,
                    contentPadding = 16.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AttributeRadarChart(attributes = attributes)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Surface(
                                color = SystemColors.VerifiedEmerald.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SystemColors.VerifiedEmerald.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "VERIFIED BONUS: +${player.attributes.totalVerifiedPoints}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SystemColors.VerifiedEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                color = SystemColors.WarningAmber.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SystemColors.WarningAmber.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "SELF-REPORTED: +${player.attributes.totalSelfReportedPoints}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SystemColors.WarningAmber,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. READABLE ATTRIBUTE GRID (2 Columns for balanced scannability)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val chunked = attributes.chunked(2)
                    chunked.forEach { rowAttrs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowAttrs.forEach { attr ->
                                AttributeCard(
                                    attr = attr,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowAttrs.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 4. RANK HIERARCHY & PROMOTION THRESHOLDS
            item {
                SystemSectionTitle(
                    title = "Rank Progression Hierarchy",
                    subtitle = "Configurable deterministic thresholds"
                )
            }

            item {
                HolographicPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = SystemColors.ArcaneViolet,
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        val targetRank = nextRank.nextRank
                        if (targetRank != null) {
                            Text(
                                text = "NEXT PROMOTION: ${targetRank.displayName.uppercase()} (${targetRank.title})",
                                style = MaterialTheme.typography.titleLarge,
                                color = SystemColors.rankAccent(targetRank)
                            )
                            ThresholdRow(
                                label = "Level Requirement",
                                current = "${player.level}",
                                target = "${targetRank.threshold.minLevel}",
                                progress = nextRank.levelProgress
                            )
                            ThresholdRow(
                                label = "Total Attribute Score",
                                current = "${player.attributes.totalScore}",
                                target = "${targetRank.threshold.minTotalAttributes}",
                                progress = nextRank.attributeProgress
                            )
                            ThresholdRow(
                                label = "Verified Real-World Sessions",
                                current = "${player.verifiedActivitiesCount}",
                                target = "${targetRank.threshold.minVerifiedActivities}",
                                progress = nextRank.verifiedProgress
                            )
                        } else {
                            Text(
                                text = "NATIONAL LEVEL ACHIEVED — All System Rank thresholds surpassed.",
                                style = MaterialTheme.typography.titleLarge,
                                color = SystemColors.MonarchGold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HunterRank.entries.forEach { rank ->
                                val unlocked = player.rank.ordinal >= rank.ordinal
                                val accent = SystemColors.rankAccent(rank)
                                Surface(
                                    color = if (unlocked) accent.copy(alpha = 0.16f) else SystemColors.SecondarySurface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (rank == player.rank) accent else SystemColors.SubtleBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (unlocked) Icons.Rounded.MilitaryTech else Icons.Rounded.Lock,
                                            contentDescription = rank.displayName,
                                            tint = if (unlocked) accent else SystemColors.MutedSilver,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = rank.displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (unlocked) SystemColors.PureIce else SystemColors.MutedSilver
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. REAL-WORLD DOMAIN TELEMETRY
            item {
                SystemSectionTitle(
                    title = "Real-World Domain Telemetry",
                    subtitle = "Physical • Academic • Discipline • Recovery readiness"
                )
            }

            item {
                val recColor = when (state.physicalState.recoveryStatus) {
                    RecoveryStatus.OPTIMAL -> SystemColors.VerifiedEmerald
                    RecoveryStatus.MODERATE_LOAD -> SystemColors.WarningAmber
                    RecoveryStatus.RECOVERY_REQUIRED -> SystemColors.DangerCrimson
                }
                HolographicPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("status_domain_telemetry_card"),
                    accentColor = recColor,
                    secondaryColor = SystemColors.ElectricCyan,
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(recColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RECOVERY READINESS: ${state.physicalState.recoveryStatus.label.uppercase()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = recColor
                                )
                            }
                            Text(
                                text = "${player.recoveryScore}/100",
                                style = MaterialTheme.typography.titleLarge,
                                color = SystemColors.PureIce
                            )
                        }

                        Surface(
                            color = SystemColors.SecondarySurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SystemColors.SubtleBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "• Physical: ${state.physicalState.completedJoggingMinutesToday}/${state.physicalState.effectiveJoggingTarget}m jogging • ${state.physicalState.weeklySessionsCompleted} weekly sessions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SystemColors.PureIce
                                )
                                Text(
                                    text = "• Academic: ${academicSummary.todayStudyMinutes}/${academicSummary.dailyStudyGoalMinutes}m study • ${academicSummary.pendingRevisionsCount} revisions due",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SystemColors.PureIce
                                )
                                Text(
                                    text = "• Discipline: Score ${lifeSummary.todayDisciplineScore}/100 • ${lifeSummary.completedHabitsToday}/${lifeSummary.activeHabitsCount} habits completed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SystemColors.PureIce
                                )
                            }
                        }
                    }
                }
            }

            // 6. TITLES & ACHIEVEMENTS HIGHLIGHTS
            item {
                HolographicPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = SystemColors.MonarchGold,
                    secondaryColor = SystemColors.ArcaneViolet,
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TITLES & ACHIEVEMENTS",
                                style = MaterialTheme.typography.titleLarge,
                                color = SystemColors.MonarchGold
                            )
                            Text(
                                text = "${unlockedTitles.size} Titles • ${unlockedAchievements.size} Badges",
                                style = MaterialTheme.typography.labelMedium,
                                color = SystemColors.ElectricCyan
                            )
                        }
                        Text(
                            text = "Equipped Honorific: $equippedTitle",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = SystemColors.PureIce
                        )
                        if (unlockedAchievements.isNotEmpty()) {
                            unlockedAchievements.take(3).forEach { ach ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = ach.definition.title,
                                        tint = SystemColors.MonarchGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${ach.definition.title} (${ach.definition.rarity.label})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SystemColors.MetallicSilver
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Complete verified training and study quests to unlock honorific titles and system achievement badges.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.MutedSilver
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenTitles,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SystemColors.MonarchGold)
                            ) {
                                Text("ALL TITLES", color = SystemColors.MonarchGold, style = MaterialTheme.typography.labelLarge)
                            }
                            OutlinedButton(
                                onClick = onOpenAchievements,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                            ) {
                                Text("ACHIEVEMENTS", color = SystemColors.ElectricCyan, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AttributeCard(
    attr: AttributeBreakdown,
    modifier: Modifier = Modifier
) {
    val accent = SystemColors.attributeAccent(attr.type)
    Surface(
        modifier = modifier,
        color = SystemColors.SecondarySurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = attr.type.code,
                        style = MaterialTheme.typography.titleLarge,
                        color = accent
                    )
                }
                Text(
                    text = "${attr.totalValue}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = SystemColors.PureIce
                )
            }

            Text(
                text = attr.type.displayName.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = SystemColors.PureIce
            )

            Text(
                text = attr.type.description,
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.MutedSilver,
                maxLines = 2
            )

            GlowingProgressBar(
                progress = attr.progressFraction,
                height = 5.dp,
                startColor = accent,
                endColor = SystemColors.ElectricCyan
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+${attr.verifiedPoints} ver / +${attr.selfReportedPoints} self",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MutedSilver
                )
                Text(
                    text = "${attr.progressXp}/${attr.progressXpRequired} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun ThresholdRow(
    label: String,
    current: String,
    target: String,
    progress: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = SystemColors.MetallicSilver
            )
            Text(
                text = "$current / $target",
                style = MaterialTheme.typography.labelLarge,
                color = SystemColors.ElectricCyan
            )
        }
        GlowingProgressBar(progress = progress, height = 6.dp)
    }
}
