package com.example.ui.screens.rpg

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.repository.RpgDashboardSnapshot
import com.example.domain.model.DungeonDifficulty
import com.example.domain.model.DungeonStatus
import com.example.domain.model.RpgRarity
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RpgUiState

@Composable
fun RpgStateHost(
    uiState: RpgUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (RpgUiState.Ready) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack)
    ) {
        when (uiState) {
            is RpgUiState.Loading -> SystemLoadingView(message = "SYNCHRONIZING RPG SYSTEMS...")
            is RpgUiState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                SystemErrorState(message = uiState.message, onRetry = onRetry)
            }
            is RpgUiState.Ready -> content(uiState)
        }
    }
}

@Composable
fun RpgFeedbackBanner(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (message.isNullOrBlank()) return
    HolographicPanel(
        modifier = modifier.fillMaxWidth(),
        accentColor = SystemColors.ElectricCyan,
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "System feedback",
                tint = SystemColors.ElectricCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.PureIce,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("rpg_dismiss_feedback")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss feedback",
                    tint = SystemColors.MutedSilver
                )
            }
        }
    }
}

@Composable
fun RpgTagChip(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun difficultyAccent(difficulty: DungeonDifficulty): Color = when (difficulty) {
    DungeonDifficulty.E_RANK -> SystemColors.MetallicSilver
    DungeonDifficulty.D_RANK -> SystemColors.VerifiedEmerald
    DungeonDifficulty.C_RANK -> SystemColors.SystemBlue
    DungeonDifficulty.B_RANK -> SystemColors.ElectricCyan
    DungeonDifficulty.A_RANK -> SystemColors.ArcaneViolet
    DungeonDifficulty.S_RANK -> SystemColors.MonarchGold
    DungeonDifficulty.SPECIAL -> SystemColors.WarningAmber
}

fun statusAccent(status: DungeonStatus): Color = when (status) {
    DungeonStatus.LOCKED -> SystemColors.MutedSilver
    DungeonStatus.AVAILABLE -> SystemColors.ElectricCyan
    DungeonStatus.ACTIVE -> SystemColors.VerifiedEmerald
    DungeonStatus.PAUSED -> SystemColors.WarningAmber
    DungeonStatus.COMPLETED -> SystemColors.MonarchGold
    DungeonStatus.ABANDONED -> SystemColors.DangerCrimson
}

fun rarityAccent(rarity: RpgRarity): Color = when (rarity) {
    RpgRarity.COMMON -> SystemColors.MetallicSilver
    RpgRarity.UNCOMMON -> SystemColors.VerifiedEmerald
    RpgRarity.RARE -> SystemColors.SystemBlue
    RpgRarity.EPIC -> SystemColors.ArcaneViolet
    RpgRarity.LEGENDARY -> SystemColors.MonarchGold
    RpgRarity.MYTHIC -> SystemColors.DangerCrimson
}

@Composable
fun RpgMetricMiniCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    HolographicPanel(
        modifier = modifier,
        accentColor = accent,
        contentPadding = 12.dp
    ) {
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = SystemColors.MutedSilver
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
        }
    }
}

fun RpgDashboardSnapshot.activeDungeonsCount(): Int =
    dungeons.count { it.effectiveStatus == DungeonStatus.ACTIVE || it.effectiveStatus == DungeonStatus.PAUSED }
