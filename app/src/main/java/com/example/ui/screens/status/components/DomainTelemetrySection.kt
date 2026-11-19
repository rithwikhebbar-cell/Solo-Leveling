package com.example.ui.screens.status.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Today
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
import androidx.compose.ui.unit.dp
import com.example.domain.model.AcademicDashboardSummary
import com.example.domain.model.LifeDashboardSummary
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.RecoveryStatus
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SystemColors

@Composable
fun DomainTelemetrySection(
    physicalState: PhysicalDevelopmentState,
    academicSummary: AcademicDashboardSummary,
    lifeSummary: LifeDashboardSummary,
    onNavigateToPhysical: () -> Unit,
    onNavigateToAcademic: () -> Unit,
    onNavigateToLife: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recColor = when (physicalState.recoveryStatus) {
        RecoveryStatus.OPTIMAL -> SystemColors.VerifiedEmerald
        RecoveryStatus.MODERATE_LOAD -> SystemColors.WarningAmber
        RecoveryStatus.RECOVERY_REQUIRED -> SystemColors.DangerCrimson
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = "Real-World Domain Telemetry",
            subtitle = "Physical • Academic • Life & Discipline Readiness"
        )

        HolographicPanel(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("status_domain_telemetry_card"),
            accentColor = recColor,
            secondaryColor = SystemColors.ElectricCyan,
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header Recovery Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(recColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RECOVERY READINESS: ${physicalState.recoveryStatus.label.uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = recColor
                        )
                    }

                    Surface(
                        color = recColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.75.dp, recColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "${physicalState.recoveryStatus.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = recColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // 1. Physical Domain Tile
                DomainCard(
                    title = "PHYSICAL DEVELOPMENT",
                    headline = "${physicalState.completedJoggingMinutesToday} / ${physicalState.effectiveJoggingTarget} MIN JOGGING",
                    detail = "${physicalState.weeklySessionsCompleted} weekly workouts completed • Status: ${physicalState.workoutStatus.label}",
                    progressFraction = physicalState.joggingProgressFraction,
                    icon = Icons.Rounded.DirectionsRun,
                    accentColor = SystemColors.VerifiedEmerald,
                    onClick = onNavigateToPhysical
                )

                // 2. Academic Domain Tile
                val studyGoal = academicSummary.dailyStudyGoalMinutes.coerceAtLeast(1)
                val academicFraction = (academicSummary.todayStudyMinutes.toFloat() / studyGoal).coerceIn(0f, 1f)
                DomainCard(
                    title = "ACADEMIC ENGINE",
                    headline = "${academicSummary.todayStudyMinutes} / ${academicSummary.dailyStudyGoalMinutes} MIN STUDY",
                    detail = "${academicSummary.pendingRevisionsCount} revisions pending • ${academicSummary.upcomingExamsCount} exams scheduled",
                    progressFraction = academicFraction,
                    icon = Icons.Rounded.School,
                    accentColor = SystemColors.SystemBlue,
                    onClick = onNavigateToAcademic
                )

                // 3. Life & Discipline Domain Tile
                val habitCount = lifeSummary.activeHabitsCount.coerceAtLeast(1)
                val disciplineFraction = (lifeSummary.completedHabitsToday.toFloat() / habitCount).coerceIn(0f, 1f)
                DomainCard(
                    title = "LIFE & DISCIPLINE OS",
                    headline = "DISCIPLINE SCORE: ${lifeSummary.todayDisciplineScore} / 100",
                    detail = "${lifeSummary.completedHabitsToday} / ${lifeSummary.activeHabitsCount} habits completed • ${lifeSummary.activeRoutinesCount} active routines",
                    progressFraction = disciplineFraction,
                    icon = Icons.Rounded.Today,
                    accentColor = SystemColors.ArcaneViolet,
                    onClick = onNavigateToLife
                )
            }
        }
    }
}

@Composable
private fun DomainCard(
    title: String,
    headline: String,
    detail: String,
    progressFraction: Float,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = SystemColors.SecondarySurface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.75.dp, SystemColors.SubtleBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Open $title",
                    tint = SystemColors.MutedSilver,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color = SystemColors.PureIce
            )

            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.MutedSilver
            )

            GlowingProgressBar(
                progress = progressFraction,
                height = 4.dp,
                startColor = accentColor,
                endColor = SystemColors.ElectricCyan
            )
        }
    }
}
