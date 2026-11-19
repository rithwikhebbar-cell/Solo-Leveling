package com.example.ui.screens.status.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.PlayerAttributes
import com.example.ui.components.AttributeRadarChart
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SystemColors

@Composable
fun AttributeMatrixSection(
    attributes: PlayerAttributes,
    onSelectAttribute: (AttributeBreakdown) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRadar by rememberSaveable { mutableStateOf(true) }
    val attrList = attributes.asList()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = "Seven Core Attributes",
            subtitle = "7 Canonical Domains • Score: ${attributes.totalScore}",
            trailing = {
                TextButton(
                    onClick = { showRadar = !showRadar },
                    modifier = Modifier.testTag("toggle_attribute_radar_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Insights,
                        contentDescription = "Toggle radar chart",
                        tint = if (showRadar) SystemColors.ElectricCyan else SystemColors.MutedSilver,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = if (showRadar) "COLLAPSE RADAR" else "EXPAND RADAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (showRadar) SystemColors.ElectricCyan else SystemColors.MutedSilver
                    )
                }
            }
        )

        // Attribute Radar Card
        AnimatedVisibility(
            visible = showRadar,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            HolographicPanel(
                modifier = Modifier.fillMaxWidth(),
                accentColor = SystemColors.ElectricCyan,
                secondaryColor = SystemColors.ArcaneViolet,
                contentPadding = 16.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AttributeRadarChart(
                        attributes = attrList,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Surface(
                            color = SystemColors.VerifiedEmerald.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SystemColors.VerifiedEmerald.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "VERIFIED BONUS: +${attributes.totalVerifiedPoints}",
                                style = MaterialTheme.typography.labelMedium,
                                color = SystemColors.VerifiedEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            color = SystemColors.WarningAmber.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SystemColors.WarningAmber.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "SELF-REPORTED: +${attributes.totalSelfReportedPoints}",
                                style = MaterialTheme.typography.labelMedium,
                                color = SystemColors.WarningAmber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2-Column Responsive Attribute Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val chunked = attrList.chunked(2)
            chunked.forEach { rowAttrs ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowAttrs.forEach { attr ->
                        TacticalAttributeCard(
                            attr = attr,
                            onClick = { onSelectAttribute(attr) },
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
}

@Composable
private fun TacticalAttributeCard(
    attr: AttributeBreakdown,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = SystemColors.attributeAccent(attr.type)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("attribute_card_${attr.type.code.lowercase()}"),
        color = SystemColors.PanelSurface,
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
