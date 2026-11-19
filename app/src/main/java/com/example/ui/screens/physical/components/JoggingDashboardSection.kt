package com.example.ui.screens.physical.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.domain.model.JoggingSession
import com.example.domain.model.JoggingTargetProposal
import com.example.domain.model.PhysicalActivityKind
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date

@Composable
fun JoggingDashboardSection(
    completedMinutesToday: Int,
    effectiveTargetMinutes: Int,
    progressFraction: Float,
    joggingSessions: List<JoggingSession>,
    pendingProposal: JoggingTargetProposal?,
    timeFormatter: SimpleDateFormat,
    onStartLive: () -> Unit,
    onOpenManual: () -> Unit,
    onAcceptProposal: (Int) -> Unit,
    onDismissProposal: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60_000L
    val weeklyJogs = joggingSessions.filter {
        it.startEpochMs >= sevenDaysAgo && it.activityKind != PhysicalActivityKind.WALKING
    }
    val weeklyMinutes = weeklyJogs.sumOf { it.durationMinutes }
    val zone = ZoneId.systemDefault()
    val weeklyDays = weeklyJogs.map { Instant.ofEpochMilli(it.startEpochMs).atZone(zone).toLocalDate() }.toSet().size

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SystemSpacing.sectionSpacing)
    ) {
        // 1. JOGGING TELEMETRY DASHBOARD
        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.ElectricCyan,
            contentPadding = SystemSpacing.cardPadding
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsRun,
                            contentDescription = null,
                            tint = SystemColors.ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "TODAY'S JOGGING TARGET",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.PureIce
                        )
                    }
                    Text(
                        text = "$completedMinutesToday / $effectiveTargetMinutes min (${(progressFraction * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = SystemColors.ElectricCyan
                    )
                }

                GlowingProgressBar(
                    progress = progressFraction,
                    height = 8.dp,
                    fillColor = SystemColors.ElectricCyan
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    JoggingMetricTile("7D MINUTES", "${weeklyMinutes}m", Modifier.weight(1f))
                    JoggingMetricTile("7D SESSIONS", "${weeklyJogs.size}", Modifier.weight(1f))
                    JoggingMetricTile("CONSISTENCY", "$weeklyDays / 7d", Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartLive,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SystemSpacing.minTouchTarget)
                            .testTag("start_live_jog_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        ),
                        shape = RoundedCornerShape(SystemSpacing.cornerMedium)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("START SESSION", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onOpenManual,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SystemSpacing.minTouchTarget)
                            .testTag("open_manual_jog_button"),
                        shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                        border = BorderStroke(SystemSpacing.borderStandard, SystemColors.SubtleBorder)
                    ) {
                        Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("MANUAL LOG", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 2. PENDING PROPOSAL BANNER (IF ACTIVE)
        pendingProposal?.let { proposal ->
            HolographicPanel(
                modifier = Modifier.fillMaxWidth(),
                accentColor = SystemColors.ArcaneViolet,
                contentPadding = SystemSpacing.cardPadding
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = SystemColors.ArcaneViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ADVISORY TARGET PROPOSAL: ${proposal.proposedMinutes} MIN",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.ArcaneViolet
                        )
                    }
                    Text(
                        text = proposal.rationale,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SystemColors.MetallicSilver
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onAcceptProposal(proposal.proposedMinutes) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = SystemSpacing.minTouchTarget)
                                .testTag("accept_jog_proposal_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SystemColors.ArcaneViolet,
                                contentColor = SystemColors.PureIce
                            ),
                            shape = RoundedCornerShape(SystemSpacing.cornerMedium)
                        ) {
                            Text("APPROVE ${proposal.proposedMinutes} MIN", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = { onDismissProposal(proposal.proposedMinutes) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = SystemSpacing.minTouchTarget),
                            shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                            border = BorderStroke(SystemSpacing.borderStandard, SystemColors.SubtleBorder)
                        ) {
                            Text("KEEP ${proposal.currentMinutes} MIN", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 3. JOGGING SESSION HISTORY
        Text(
            text = "RECORDED JOGGING SESSIONS",
            style = MaterialTheme.typography.labelSmall,
            color = SystemColors.MutedSilver
        )

        if (joggingSessions.isEmpty()) {
            SystemEmptyState(
                title = "No Jogging Sessions Recorded",
                description = "Start a live jogging timer, log a session manually, or sync Health Connect workouts."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                joggingSessions.forEach { session ->
                    JoggingSessionCard(
                        session = session,
                        formattedDate = timeFormatter.format(Date(session.startEpochMs))
                    )
                }
            }
        }
    }
}

@Composable
fun JoggingSessionCard(
    session: JoggingSession,
    formattedDate: String,
    modifier: Modifier = Modifier
) {
    val distanceLabel = session.distanceKm?.let { "${"%.2f".format(it)} km" } ?: "Distance —"
    val paceLabel = session.avgPaceSecPerKm?.let { sec -> "${sec / 60}:${(sec % 60).toString().padStart(2, '0')} /km" } ?: "Pace —"
    val hrLabel = session.avgHeartRateBpm?.let { "$it bpm" }

    HolographicPanel(
        modifier = modifier.fillMaxWidth(),
        contentPadding = SystemSpacing.compactCardPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${session.activityKind.label.uppercase()} • $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MutedSilver
                )
                ProvenanceChip(status = session.verificationStatus)
            }

            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium,
                color = SystemColors.PureIce
            )

            Text(
                text = listOfNotNull("${session.durationMinutes} min", distanceLabel, paceLabel, hrLabel, "+${session.xpAwarded} XP").joinToString(" • "),
                style = MaterialTheme.typography.labelMedium,
                color = SystemColors.ElectricCyan
            )

            if (session.notes.isNotBlank()) {
                Text(
                    text = session.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )
            }
        }
    }
}

@Composable
private fun JoggingMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SystemColors.SecondarySurface,
        shape = RoundedCornerShape(SystemSpacing.cornerMedium),
        border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.SubtleBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
        }
    }
}
