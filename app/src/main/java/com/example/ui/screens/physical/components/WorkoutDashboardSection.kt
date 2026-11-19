package com.example.ui.screens.physical.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.RecoveryState
import com.example.domain.model.WorkoutCategory
import com.example.domain.model.WorkoutSession
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutDashboardSection(
    advisoryCategory: Pair<WorkoutCategory, String>,
    recoveryState: RecoveryState,
    workoutSessions: List<WorkoutSession>,
    timeFormatter: SimpleDateFormat,
    onSelectCategoryToStart: (WorkoutCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRest = recoveryState == RecoveryState.REST_RECOMMENDED

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SystemSpacing.sectionSpacing)
    ) {
        // 1. WORKOUT PROTOCOL & CATEGORY SELECTOR
        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = if (isRest) SystemColors.DangerCrimson else SystemColors.ElectricCyan,
            contentPadding = SystemSpacing.cardPadding
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        tint = if (isRest) SystemColors.DangerCrimson else SystemColors.ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "WORKOUT PROTOCOL & CATEGORIES",
                        style = MaterialTheme.typography.titleMedium,
                        color = SystemColors.PureIce
                    )
                }

                if (isRest) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = SystemColors.DangerCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "REST_RECOMMENDED is active: High-intensity workouts should be deferred. Focus on mobility, gentle stretching, or active rest.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SystemColors.DangerCrimson
                        )
                    }
                } else {
                    Text(
                        text = "System Suggestion (${advisoryCategory.first.label}): ${advisoryCategory.second}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SystemColors.MetallicSilver
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkoutCategory.entries.forEach { cat ->
                        val isSuggested = cat == advisoryCategory.first && !isRest
                        OutlinedButton(
                            onClick = { onSelectCategoryToStart(cat) },
                            modifier = Modifier
                                .heightIn(min = SystemSpacing.minTouchTarget)
                                .testTag("start_workout_${cat.name.lowercase()}"),
                            shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                            border = BorderStroke(
                                width = if (isSuggested) 1.5.dp else 1.dp,
                                color = if (isSuggested) SystemColors.ElectricCyan else SystemColors.SubtleBorder
                            )
                        ) {
                            Text(
                                text = "START ${cat.label.uppercase()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSuggested) SystemColors.ElectricCyan else SystemColors.PureIce
                            )
                        }
                    }
                }
            }
        }

        // 2. WORKOUT SESSION HISTORY
        Text(
            text = "RECORDED WORKOUT SESSIONS",
            style = MaterialTheme.typography.labelSmall,
            color = SystemColors.MutedSilver
        )

        if (workoutSessions.isEmpty()) {
            SystemEmptyState(
                title = "No Workouts Recorded",
                description = "Select a category above to run a structured sets/reps/rest workout or sync from Health Connect."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                workoutSessions.forEach { workout ->
                    WorkoutSessionCard(
                        workout = workout,
                        formattedDate = timeFormatter.format(Date(workout.startEpochMs))
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutSessionCard(
    workout: WorkoutSession,
    formattedDate: String,
    modifier: Modifier = Modifier
) {
    HolographicPanel(
        modifier = modifier.fillMaxWidth(),
        accentColor = SystemColors.ArcaneViolet,
        contentPadding = SystemSpacing.compactCardPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${workout.category.label.uppercase()} • $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MutedSilver
                )
                ProvenanceChip(status = workout.verificationStatus)
            }

            Text(
                text = workout.title,
                style = MaterialTheme.typography.titleMedium,
                color = SystemColors.PureIce
            )

            Text(
                text = "${workout.durationMinutes} min • ${workout.totalSets} sets / ${workout.totalReps} reps • +${workout.xpAwarded} XP",
                style = MaterialTheme.typography.labelMedium,
                color = SystemColors.ElectricCyan
            )

            if (workout.exercises.isNotEmpty()) {
                Surface(
                    color = SystemColors.SecondarySurface,
                    shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                    border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = workout.exercises.joinToString(", ") { "${it.exerciseName} (${it.setsCompleted}x${it.repsPerSet})" },
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                    }
                }
            }

            if (workout.notes.isNotBlank()) {
                Text(
                    text = workout.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MutedSilver
                )
            }
        }
    }
}
