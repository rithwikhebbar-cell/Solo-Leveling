package com.example.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.SystemAnnouncementCategory
import com.example.domain.model.SystemAnnouncementRecord
import com.example.ui.theme.SystemColors

@Composable
fun SystemAnnouncementBanner(
    announcements: List<SystemAnnouncementRecord>,
    onNavigateRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAnnouncement = announcements.firstOrNull()

    AnimatedVisibility(
        visible = activeAnnouncement != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (activeAnnouncement != null) {
            val accent = when (activeAnnouncement.category) {
                SystemAnnouncementCategory.LEVEL_UP -> SystemColors.MonarchGold
                SystemAnnouncementCategory.RANK_UP -> SystemColors.ArcaneViolet
                SystemAnnouncementCategory.DUNGEON -> SystemColors.ElectricCyan
                SystemAnnouncementCategory.BOSS -> SystemColors.DangerCrimson
                SystemAnnouncementCategory.RECOVERY -> SystemColors.DangerCrimson
                SystemAnnouncementCategory.QUEST_AVAILABLE,
                SystemAnnouncementCategory.QUEST_COMPLETED -> SystemColors.SystemBlue
                SystemAnnouncementCategory.RECOMMENDATION -> SystemColors.ElectricCyan
                SystemAnnouncementCategory.DAILY_SUMMARY -> SystemColors.VerifiedEmerald
                SystemAnnouncementCategory.SYSTEM_ALERT -> SystemColors.WarningAmber
            }

            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable {
                        if (activeAnnouncement.deepLinkRoute.isNotBlank()) {
                            onNavigateRoute(activeAnnouncement.deepLinkRoute)
                        }
                    }
                    .testTag("system_announcement_banner"),
                color = SystemColors.DeepNavy.copy(alpha = 0.92f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (activeAnnouncement.category) {
                            SystemAnnouncementCategory.RECOVERY -> Icons.Rounded.Warning
                            SystemAnnouncementCategory.QUEST_COMPLETED -> Icons.Rounded.CheckCircle
                            SystemAnnouncementCategory.LEVEL_UP,
                            SystemAnnouncementCategory.RANK_UP -> Icons.AutoMirrored.Rounded.VolumeUp
                            else -> Icons.Rounded.Campaign
                        },
                        contentDescription = "System Broadcast",
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "SYSTEM BROADCAST // ${activeAnnouncement.category.label}".uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeAnnouncement.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = SystemColors.PureIce
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = activeAnnouncement.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver,
                            maxLines = 2
                        )
                    }

                    if (activeAnnouncement.actionLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                if (activeAnnouncement.deepLinkRoute.isNotBlank()) {
                                    onNavigateRoute(activeAnnouncement.deepLinkRoute)
                                }
                            },
                            modifier = Modifier.heightIn(min = 40.dp),
                            border = BorderStroke(1.dp, accent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = activeAnnouncement.actionLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
