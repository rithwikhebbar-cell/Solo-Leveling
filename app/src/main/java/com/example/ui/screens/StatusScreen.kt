package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AcademicDashboardSummary
import com.example.domain.model.AchievementWithStatus
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.LifeDashboardSummary
import com.example.domain.model.PlayerTitleDefinition
import com.example.ui.components.TacticalSelector
import com.example.ui.screens.status.components.AchievementShowcaseSection
import com.example.ui.screens.status.components.AttributeDetailBottomSheet
import com.example.ui.screens.status.components.AttributeMatrixSection
import com.example.ui.screens.status.components.DomainTelemetrySection
import com.example.ui.screens.status.components.HunterIdentityHeroCard
import com.example.ui.screens.status.components.PlayerProgressionCard
import com.example.ui.screens.status.components.RankAscensionSection
import com.example.ui.screens.status.components.StatusCinematicHeader
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RealityOsUiState

private enum class StatusSectionFilter(val label: String) {
    ALL("Overview"),
    ATTRIBUTES("Attributes"),
    PROGRESSION("Progression"),
    ASCENSION("Ascension"),
    DOMAINS("Domains"),
    ACHIEVEMENTS("Titles")
}

@Composable
fun StatusScreen(
    state: RealityOsUiState.Ready,
    modifier: Modifier = Modifier,
    academicSummary: AcademicDashboardSummary = AcademicDashboardSummary(),
    lifeSummary: LifeDashboardSummary = LifeDashboardSummary(),
    unlockedTitles: List<PlayerTitleDefinition> = emptyList(),
    unlockedAchievements: List<AchievementWithStatus> = emptyList(),
    onOpenTitles: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onNavigateToPhysical: () -> Unit = {},
    onNavigateToAcademic: () -> Unit = {},
    onNavigateToLife: () -> Unit = {}
) {
    val player = state.player
    val nextRank = state.nextRankProgress
    val equippedTitle = unlockedTitles.firstOrNull { it.isEquipped }?.name ?: player.title

    var selectedFilter by rememberSaveable { mutableStateOf(StatusSectionFilter.ALL) }
    var selectedAttributeForDetail by remember { mutableStateOf<AttributeBreakdown?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("status_screen_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. SECTION A: CINEMATIC STATUS HEADER
            item(key = "status_header") {
                StatusCinematicHeader(
                    level = player.level,
                    rank = player.rank
                )
            }

            // 2. TACTICAL SECTION SELECTOR
            item(key = "status_tactical_selector") {
                TacticalSelector(
                    options = StatusSectionFilter.entries,
                    selectedOption = selectedFilter,
                    onOptionSelected = { selectedFilter = it },
                    labelProvider = { it.label }
                )
            }

            // 3. SECTION B: HUNTER IDENTITY HERO CARD
            if (selectedFilter == StatusSectionFilter.ALL || selectedFilter == StatusSectionFilter.PROGRESSION) {
                item(key = "status_identity_hero") {
                    HunterIdentityHeroCard(
                        player = player,
                        equippedTitle = equippedTitle
                    )
                }
            }

            // 4. SECTION C: PLAYER PROGRESSION CARD
            if (selectedFilter == StatusSectionFilter.ALL || selectedFilter == StatusSectionFilter.PROGRESSION) {
                item(key = "status_progression_card") {
                    PlayerProgressionCard(
                        player = player,
                        nextRankProgress = nextRank
                    )
                }
            }

            // 5. SECTION D: SEVEN CORE ATTRIBUTES & RADAR
            if (selectedFilter == StatusSectionFilter.ALL || selectedFilter == StatusSectionFilter.ATTRIBUTES) {
                item(key = "status_attribute_matrix") {
                    AttributeMatrixSection(
                        attributes = player.attributes,
                        onSelectAttribute = { attr -> selectedAttributeForDetail = attr }
                    )
                }
            }

            // 6. SECTION E: RANK ASCENSION PANEL
            if (selectedFilter == StatusSectionFilter.ALL || selectedFilter == StatusSectionFilter.ASCENSION) {
                item(key = "status_rank_ascension") {
                    RankAscensionSection(
                        player = player,
                        nextRankProgress = nextRank
                    )
                }
            }

            // 7. SECTION F: DOMAIN TELEMETRY
            if (selectedFilter == StatusSectionFilter.ALL || selectedFilter == StatusSectionFilter.DOMAINS) {
                item(key = "status_domain_telemetry") {
                    DomainTelemetrySection(
                        physicalState = state.physicalState,
                        academicSummary = academicSummary,
                        lifeSummary = lifeSummary,
                        onNavigateToPhysical = onNavigateToPhysical,
                        onNavigateToAcademic = onNavigateToAcademic,
                        onNavigateToLife = onNavigateToLife
                    )
                }
            }

            // 8. SECTION G: ACHIEVEMENTS & TITLES SHOWCASE
            if (selectedFilter == StatusSectionFilter.ALL || selectedFilter == StatusSectionFilter.ACHIEVEMENTS) {
                item(key = "status_achievements_showcase") {
                    AchievementShowcaseSection(
                        equippedTitle = equippedTitle,
                        unlockedTitles = unlockedTitles,
                        unlockedAchievements = unlockedAchievements,
                        onOpenTitles = onOpenTitles,
                        onOpenAchievements = onOpenAchievements
                    )
                }
            }

            item(key = "status_bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Attribute Detail Bottom Sheet
        if (selectedAttributeForDetail != null) {
            AttributeDetailBottomSheet(
                attribute = selectedAttributeForDetail,
                onDismiss = { selectedAttributeForDetail = null }
            )
        }
    }
}
