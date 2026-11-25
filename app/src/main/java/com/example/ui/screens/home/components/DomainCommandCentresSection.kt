package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AcademicDashboardSummary
import com.example.domain.model.LifeDashboardSummary
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.RecoveryState
import com.example.domain.model.ShadowMonarchProfile
import com.example.ui.components.HolographicPanel
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@Composable
fun DomainCommandCentresSection(
    physicalState: PhysicalDevelopmentState,
    academicSummary: AcademicDashboardSummary,
    lifeSummary: LifeDashboardSummary,
    monarchProfile: ShadowMonarchProfile?,
    shadowEssenceBalance: Int,
    onNavigateToPhysical: () -> Unit,
    onNavigateToAcademic: () -> Unit,
    onNavigateToLife: () -> Unit,
    onNavigateToShadowRealm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = SystemColors.ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "DOMAIN COMMAND CENTRES",
                    style = MaterialTheme.typography.titleMedium,
                    color = SystemColors.PureIce
                )
            }

            Text(
                text = "CORE REALMS",
                style = MaterialTheme.typography.labelSmall,
                color = SystemColors.MutedSilver
            )
        }

        // Domain Grid (2 columns on tablets/wide screens or stacked rows)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Physical & Academic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. PHYSICAL COMMAND
                val recovery = physicalState.recoveryAssessment
                val recoveryAlert = recovery.state == RecoveryState.REST_RECOMMENDED
                DomainMiniCard(
                    title = "PHYSICAL",
                    subtitle = "Readiness: ${recovery.readinessScore}%",
                    icon = Icons.Rounded.FitnessCenter,
                    accentColor = if (recoveryAlert) SystemColors.WarningAmber else SystemColors.VerifiedEmerald,
                    metric1Label = "RANK",
                    metric1Value = physicalState.conditioningRank,
                    metric2Label = "RECOVERY",
                    metric2Value = recovery.state.label,
                    onClick = onNavigateToPhysical,
                    testTag = "domain_card_physical",
                    modifier = Modifier.weight(1f)
                )

                // 2. ACADEMIC MASTERY
                DomainMiniCard(
                    title = "ACADEMIC",
                    subtitle = "${academicSummary.todayStudyMinutes}m / ${academicSummary.dailyStudyGoalMinutes}m goal",
                    icon = Icons.Rounded.AutoStories,
                    accentColor = SystemColors.ArcaneViolet,
                    metric1Label = "REVISIONS",
                    metric1Value = if (academicSummary.overdueRevisionsCount > 0) {
                        "${academicSummary.overdueRevisionsCount} OVERDUE"
                    } else {
                        "${academicSummary.pendingRevisionsCount} PENDING"
                    },
                    metric2Label = "SYLLABUS",
                    metric2Value = "${academicSummary.overallSyllabusCompletionPercent}%",
                    onClick = onNavigateToAcademic,
                    testTag = "domain_card_academic",
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Life & Shadow Army
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 3. LIFE ARCHITECTURE
                DomainMiniCard(
                    title = "LIFE & HABITS",
                    subtitle = "Score: ${lifeSummary.todayDisciplineScore}/100",
                    icon = Icons.Rounded.Timer,
                    accentColor = SystemColors.SystemBlue,
                    metric1Label = "HABITS",
                    metric1Value = "${lifeSummary.completedHabitsToday}/${lifeSummary.activeHabitsCount}",
                    metric2Label = "STREAK",
                    metric2Value = "${lifeSummary.activeDisciplineStreakDays}D",
                    onClick = onNavigateToLife,
                    testTag = "domain_card_life",
                    modifier = Modifier.weight(1f)
                )

                // 4. SHADOW ARMY
                DomainMiniCard(
                    title = "SHADOW ARMY",
                    subtitle = monarchProfile?.monarchRank?.label ?: "Awakening",
                    icon = Icons.Rounded.Groups,
                    accentColor = SystemColors.MonarchGold,
                    metric1Label = "SOLDIERS",
                    metric1Value = "${monarchProfile?.activeShadowCount ?: 0}/${monarchProfile?.armyCapacity ?: 10}",
                    metric2Label = "ESSENCE",
                    metric2Value = "$shadowEssenceBalance",
                    onClick = onNavigateToShadowRealm,
                    testTag = "domain_card_shadow",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DomainMiniCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    metric1Label: String,
    metric1Value: String,
    metric2Label: String,
    metric2Value: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    HolographicPanel(
        modifier = modifier
            .clip(RoundedCornerShape(SystemSpacing.cornerMedium))
            .clickable(onClick = onClick)
            .testTag(testTag),
        accentColor = accentColor,
        contentPadding = 12.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top Row: Title, Subtitle, and Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.PureIce
                    )
                }

                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
            }

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = metric1Label,
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    Text(
                        text = metric1Value,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = SystemColors.PureIce
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = metric2Label,
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    Text(
                        text = metric2Value,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = SystemColors.PureIce
                    )
                }
            }

            // Footer Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ENTER",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
