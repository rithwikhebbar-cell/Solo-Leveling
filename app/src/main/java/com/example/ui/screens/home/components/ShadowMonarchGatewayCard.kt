package com.example.ui.screens.home.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Castle
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.ShadowMonarchProfile
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors

@Composable
fun ShadowMonarchGatewayCard(
    monarchProfile: ShadowMonarchProfile?,
    shadowEssenceBalance: Int,
    onNavigateToShadowRealm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shadowPurple = SystemColors.ArcaneViolet
    val deepVoid = Color(0xFF140D2B)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SystemSectionTitle(
            title = "Shadow Monarch Gateway",
            subtitle = "Monarch Sovereignty & Shadow Legion"
        )

        HolographicPanel(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToShadowRealm)
                .testTag("shadow_monarch_gateway_card"),
            accentColor = shadowPurple,
            secondaryColor = SystemColors.ElectricCyan,
            contentPadding = 16.dp
        ) {
            if (monarchProfile == null) {
                // Calibrating / Unawakened Sovereign Interface
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = shadowPurple.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, shadowPurple.copy(alpha = 0.6f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Castle,
                                        contentDescription = "Shadow Monarch",
                                        tint = shadowPurple,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "SHADOW SOVEREIGNTY",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SystemColors.PureIce
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "STATUS: SYNCHRONIZING MONARCH REALM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = shadowPurple
                                )
                            }
                        }
                    }

                    Text(
                        text = "Access the Shadow Realm to awaken Sovereign authority and command your shadow army.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MetallicSilver
                    )

                    Button(
                        onClick = onNavigateToShadowRealm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("enter_shadow_realm_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = shadowPurple,
                            contentColor = SystemColors.PureIce
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ENTER THE SHADOW REALM",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                val profile = monarchProfile
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header: Monarch Rank & Level
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = shadowPurple.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, shadowPurple.copy(alpha = 0.6f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Castle,
                                        contentDescription = "Shadow Monarch",
                                        tint = shadowPurple,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "MONARCH LEVEL ${profile.monarchLevel}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SystemColors.PureIce
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "SOVEREIGN RANK: ${profile.monarchRank.label.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = shadowPurple
                                )
                            }
                        }

                        Surface(
                            color = SystemColors.ObsidianBlack.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.75.dp, shadowPurple.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${profile.monarchAuthority} AUTHORITY",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MonarchGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Stats row: Shadow Capacity & Essence Balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Legion Capacity Tile
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = deepVoid.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.75.dp, shadowPurple.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Groups,
                                    contentDescription = "Active Shadows",
                                    tint = shadowPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "SHADOW LEGION",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SystemColors.MutedSilver
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "${profile.activeShadowCount} / ${profile.armyCapacity}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SystemColors.PureIce
                                    )
                                }
                            }
                        }

                        // Essence Balance Tile
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = deepVoid.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.75.dp, SystemColors.ElectricCyan.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = "Shadow Essence",
                                    tint = SystemColors.ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "SHADOW ESSENCE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SystemColors.MutedSilver
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "$shadowEssenceBalance PTS",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SystemColors.ElectricCyan
                                    )
                                }
                            }
                        }
                    }

                    // CTA Button: Enter Shadow Realm
                    Button(
                        onClick = onNavigateToShadowRealm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("enter_shadow_realm_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = shadowPurple,
                            contentColor = SystemColors.PureIce
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ENTER THE SHADOW REALM",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
