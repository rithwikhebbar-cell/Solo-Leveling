package com.example.ui.screens.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.StatusStatPill
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@Composable
fun ModuleCommandHeader(
    totalCount: Int,
    activeCount: Int,
    availableCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: ModuleCategory,
    onCategorySelected: (ModuleCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Cinematic System Title Banner
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(SystemColors.ElectricCyan)
                )
                Text(
                    text = "SYSTEM // MODULE DIRECTORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.ElectricCyan
                )
            }

            Text(
                text = "MODULES",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = SystemColors.PureIce
            )

            Text(
                text = "Your systems. Your domains. Your evolution.",
                style = MaterialTheme.typography.bodyMedium,
                color = SystemColors.MutedSilver
            )
        }

        // 2. Operational Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusStatPill(
                label = "TOTAL",
                value = "$totalCount",
                modifier = Modifier.weight(1f),
                accentColor = SystemColors.ElectricCyan
            )
            StatusStatPill(
                label = "ACTIVE",
                value = "$activeCount",
                modifier = Modifier.weight(1f),
                accentColor = SystemColors.VerifiedEmerald
            )
            StatusStatPill(
                label = "AVAILABLE",
                value = "$availableCount",
                modifier = Modifier.weight(1f),
                accentColor = SystemColors.SystemBlue
            )
            StatusStatPill(
                label = "DOMAINS",
                value = "${ModuleCategory.entries.size - 1}",
                modifier = Modifier.weight(1f),
                accentColor = SystemColors.ArcaneViolet
            )
        }

        // 3. Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search subsystems (e.g. Shadow, Academic, Penalty)...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SystemColors.MutedSilver
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = SystemColors.ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear search",
                            tint = SystemColors.MutedSilver,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("module_search_input"),
            shape = RoundedCornerShape(SystemSpacing.cornerMedium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SystemColors.ElectricCyan,
                unfocusedBorderColor = SystemColors.SubtleBorder,
                focusedContainerColor = SystemColors.SecondarySurface,
                unfocusedContainerColor = SystemColors.SecondarySurface,
                focusedTextColor = SystemColors.PureIce,
                unfocusedTextColor = SystemColors.PureIce
            )
        )

        // 4. Category Selector Tabs
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModuleCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category
                Surface(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(SystemSpacing.cornerSmall))
                        .clickable { onCategorySelected(category) }
                        .testTag("module_category_${category.name.lowercase()}"),
                    color = if (isSelected) SystemColors.ElectricCyan.copy(alpha = 0.18f) else SystemColors.SecondarySurface,
                    shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) SystemColors.ElectricCyan else SystemColors.SubtleBorder
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) SystemColors.ElectricCyan else SystemColors.MutedSilver
                        )
                    }
                }
            }
        }
    }
}
