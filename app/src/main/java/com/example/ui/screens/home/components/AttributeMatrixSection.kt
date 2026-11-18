package com.example.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.PlayerAttributes
import com.example.ui.components.AttributeRadarChart
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttributeMatrixSection(
    attributes: PlayerAttributes,
    onInspectAttributes: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRadar by rememberSaveable { mutableStateOf(false) }
    val attrList = attributes.asList()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SystemSectionTitle(
            title = "Attribute Matrix",
            subtitle = "7 Canonical Domains • Score: ${attributes.totalScore}",
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            text = if (showRadar) "GRID" else "RADAR",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (showRadar) SystemColors.ElectricCyan else SystemColors.MutedSilver
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = onInspectAttributes,
                        modifier = Modifier.testTag("inspect_attributes_button")
                    ) {
                        Text(
                            text = "DETAILS",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.ElectricCyan
                        )
                    }
                }
            }
        )

        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.SystemBlue,
            secondaryColor = SystemColors.ArcaneViolet,
            contentPadding = 12.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Heptagon Radar Chart (expandable / toggleable)
                AnimatedVisibility(
                    visible = showRadar,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    AttributeRadarChart(
                        attributes = attrList,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }

                // Compact 7-Attribute HUD Chip Grid
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 4
                ) {
                    attrList.forEach { attr ->
                        AttributeMiniCard(
                            attr = attr,
                            onClick = onInspectAttributes,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .widthIn(min = 72.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributeMiniCard(
    attr: AttributeBreakdown,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = SystemColors.attributeAccent(attr.type)

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = SystemColors.ObsidianBlack.copy(alpha = 0.70f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = attr.type.code,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${attr.totalValue}",
                style = MaterialTheme.typography.headlineMedium,
                color = SystemColors.PureIce
            )
            Spacer(modifier = Modifier.height(3.dp))
            GlowingProgressBar(
                progress = attr.progressFraction,
                height = 3.dp,
                startColor = accent,
                endColor = SystemColors.ElectricCyan
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "V:${attr.verifiedPoints} S:${attr.selfReportedPoints}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp
                ),
                color = SystemColors.MutedSilver
            )
        }
    }
}
