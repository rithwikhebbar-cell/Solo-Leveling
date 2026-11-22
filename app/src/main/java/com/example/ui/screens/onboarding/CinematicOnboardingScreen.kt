package com.example.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AttributeType
import com.example.domain.model.AwakeningStage
import com.example.domain.model.PlayerProfile
import com.example.ui.components.HolographicPanel
import com.example.ui.components.RankBadge
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

/**
 * Stage 9.1: Cinematic Onboarding & First-Launch Experience
 * Introduces new players to SOLO LEVELING: REALITY OS across 5 canonical screens.
 */
@Composable
fun CinematicOnboardingScreen(
    player: PlayerProfile,
    animationsEnabled: Boolean,
    reducedMotionEnabled: Boolean,
    onStageShown: (AwakeningStage) -> Unit = {},
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stages = AwakeningStage.entries
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var hasCompletedCalled by rememberSaveable { mutableStateOf(false) }

    val activeStage = stages[currentIndex.coerceIn(0, stages.lastIndex)]
    val allowMotion = animationsEnabled && !reducedMotionEnabled

    val safeComplete = {
        if (!hasCompletedCalled) {
            hasCompletedCalled = true
            onComplete()
        }
    }

    // Android back navigation: go back to previous stage or safely exit onboarding
    BackHandler(enabled = true) {
        if (currentIndex > 0) {
            currentIndex -= 1
        } else {
            safeComplete()
        }
    }

    LaunchedEffect(activeStage) {
        onStageShown(activeStage)
    }

    // Cinematic pulsating energy ring alpha
    val pulseAlpha = if (allowMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "onboarding_pulse")
        val value by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        value
    } else {
        0.55f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SystemColors.ObsidianBlack,
                        SystemColors.DeepNavy,
                        SystemColors.ObsidianBlack
                    )
                )
            )
            .testTag("awakening_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Background Cybernetic Energy Rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.38f)
            val baseRadius = minOf(size.width, size.height) * 0.30f

            drawCircle(
                color = SystemColors.ElectricCyan.copy(alpha = pulseAlpha * 0.18f),
                radius = baseRadius * 1.55f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = SystemColors.ArcaneViolet.copy(alpha = pulseAlpha * 0.22f),
                radius = baseRadius * 1.05f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = SystemColors.ElectricCyan.copy(alpha = 0.08f),
                radius = baseRadius * 0.65f,
                center = center,
                style = Stroke(width = 0.75.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ================================================================
            // TOP BAR: System Telemetry & Skip Button
            // ================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SystemColors.ElectricCyan)
                    )
                    Text(
                        text = "REALITY OS // 0${currentIndex + 1}.0",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        ),
                        color = SystemColors.ElectricCyan
                    )
                }

                TextButton(
                    onClick = safeComplete,
                    modifier = Modifier
                        .heightIn(min = SystemSpacing.minTouchTarget)
                        .testTag("awakening_skip_button")
                ) {
                    Text(
                        text = "SKIP SEQUENCE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            letterSpacing = 1.0.sp
                        ),
                        color = SystemColors.MutedSilver
                    )
                }
            }

            // ================================================================
            // CENTER CONTENT: Animated 5-Screen Sequence
            // ================================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        if (allowMotion) {
                            fadeIn(tween(320)) togetherWith fadeOut(tween(220))
                        } else {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        }
                    },
                    label = "onboarding_screen_content"
                ) { screenIndex ->
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (screenIndex) {
                            0 -> ScreenTheAwakening()
                            1 -> ScreenYourIdentity(player = player)
                            2 -> ScreenYourDomains()
                            3 -> ScreenTheSystem()
                            4 -> ScreenEnterTheSystem()
                            else -> ScreenTheAwakening()
                        }
                    }
                }
            }

            // ================================================================
            // BOTTOM BAR: Step Indicators & Action Navigation
            // ================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step Progress Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until stages.size) {
                        val isCurrent = i == currentIndex
                        val isVisited = i < currentIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    when {
                                        isCurrent -> SystemColors.ElectricCyan
                                        isVisited -> SystemColors.ElectricCyan.copy(alpha = 0.5f)
                                        else -> SystemColors.PanelSurfaceElevated
                                    }
                                )
                        )
                    }
                }

                // Step count telemetry
                Text(
                    text = "PHASE ${currentIndex + 1} OF ${stages.size} // ${activeStage.headline}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        letterSpacing = 1.1.sp
                    ),
                    color = SystemColors.MutedSilver
                )

                // Navigation Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentIndex > 0) {
                        OutlinedButton(
                            onClick = { currentIndex -= 1 },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = SystemSpacing.buttonHeight)
                                .testTag("awakening_prev_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SystemColors.PanelBorderCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Previous",
                                tint = SystemColors.MetallicSilver,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PREVIOUS",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = SystemColors.MetallicSilver
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (currentIndex < stages.lastIndex) {
                                currentIndex += 1
                            } else {
                                safeComplete()
                            }
                        },
                        modifier = Modifier
                            .weight(if (currentIndex > 0) 1.6f else 1f)
                            .heightIn(min = SystemSpacing.buttonHeight)
                            .testTag("awakening_next_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentIndex == stages.lastIndex) {
                                SystemColors.MonarchGold
                            } else {
                                SystemColors.ElectricCyan
                            },
                            contentColor = SystemColors.ObsidianBlack
                        )
                    ) {
                        Text(
                            text = if (currentIndex < stages.lastIndex) "CONTINUE" else "INITIALIZE SYSTEM",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentIndex == stages.lastIndex) Icons.Rounded.Bolt else Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// SCREEN 1 — THE AWAKENING
// ============================================================================
@Composable
private fun ScreenTheAwakening() {
    HolographicPanel(
        modifier = Modifier.fillMaxWidth(),
        accentColor = SystemColors.ElectricCyan,
        secondaryColor = SystemColors.ArcaneViolet,
        cornerRadius = 16.dp,
        contentPadding = 20.dp,
        isHero = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = SystemColors.ElectricCyan.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SystemColors.ElectricCyan.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "SYSTEM // AWAKENING SEQUENCE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        letterSpacing = 1.0.sp
                    ),
                    color = SystemColors.ElectricCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Awakening Core",
                tint = SystemColors.ElectricCyan,
                modifier = Modifier.size(52.dp)
            )

            Text(
                text = "YOUR JOURNEY BEGINS NOW.",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = 1.0.sp
                ),
                color = SystemColors.PureIce,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Welcome to SOLO LEVELING: REALITY OS — a personal development system engineered to transform real-world discipline into deterministic RPG progression. Your daily life is no longer routine; it is the ultimate leveling dungeon.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    lineHeight = 22.sp
                ),
                color = SystemColors.MetallicSilver,
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AwakeningPillarItem(
                    index = "01",
                    title = "ZERO VIRTUAL INFLATION",
                    description = "Attributes and XP cannot be purchased or simulated. Only verified real-world actions advance your Hunter status."
                )
                AwakeningPillarItem(
                    index = "02",
                    title = "DETERMINISTIC REWARDS",
                    description = "Workouts, study blocks, and habits directly increase attributes through transparent mathematical formulas."
                )
                AwakeningPillarItem(
                    index = "03",
                    title = "THE SHADOW MONARCH PATH",
                    description = "Conquer friction, maintain quest streaks, and reanimate overcome challenges into your personal Shadow Army."
                )
            }
        }
    }
}

@Composable
private fun AwakeningPillarItem(
    index: String,
    title: String,
    description: String
) {
    Surface(
        color = SystemColors.PanelSurface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.75.dp, SystemColors.SubtleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = index,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = SystemColors.ElectricCyan
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    ),
                    color = SystemColors.PureIce
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = InterFontFamily,
                        lineHeight = 16.sp
                    ),
                    color = SystemColors.SecondaryText
                )
            }
        }
    }
}

// ============================================================================
// SCREEN 2 — YOUR IDENTITY
// ============================================================================
@Composable
private fun ScreenYourIdentity(player: PlayerProfile) {
    HolographicPanel(
        modifier = Modifier.fillMaxWidth(),
        accentColor = SystemColors.ElectricCyan,
        secondaryColor = SystemColors.ArcaneViolet,
        cornerRadius = 16.dp,
        contentPadding = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = SystemColors.ElectricCyan.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SystemColors.ElectricCyan.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "PROFILE // HUNTER CALIBRATION",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        letterSpacing = 1.0.sp
                    ),
                    color = SystemColors.ElectricCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Player identity badge
            Surface(
                color = SystemColors.PanelSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SystemColors.PanelBorderCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = player.codename.ifBlank { "HUNTER" }.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            ),
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = "Level ${player.level} • ${player.rank.displayName}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = SystemColors.ElectricCyan
                        )
                    }
                    RankBadge(rank = player.rank)
                }
            }

            Text(
                text = "THE SEVEN CANONICAL ATTRIBUTES",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = SystemColors.PureIce
            )

            Text(
                text = "Progression is tracked across seven foundational attributes. Each attribute advances only through verified effort in reality:",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = InterFontFamily,
                    lineHeight = 18.sp
                ),
                color = SystemColors.MetallicSilver,
                textAlign = TextAlign.Center
            )

            // Grid of 7 attributes
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AttributeType.entries.forEach { attr ->
                    AttributeExplanationRow(attr = attr)
                }
            }
        }
    }
}

@Composable
private fun AttributeExplanationRow(attr: AttributeType) {
    val accent = SystemColors.attributeAccent(attr)
    Surface(
        color = SystemColors.PanelSurface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
            ) {
                Text(
                    text = attr.code,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = accent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = attr.displayName,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SystemColors.PureIce
                    )
                    Text(
                        text = "• ${attr.trainingFocus}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily
                        ),
                        color = SystemColors.SecondaryText
                    )
                }
                Text(
                    text = attr.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    ),
                    color = SystemColors.MetallicSilver
                )
            }
        }
    }
}

// ============================================================================
// SCREEN 3 — YOUR DOMAINS
// ============================================================================
@Composable
private fun ScreenYourDomains() {
    HolographicPanel(
        modifier = Modifier.fillMaxWidth(),
        accentColor = SystemColors.SystemBlue,
        secondaryColor = SystemColors.ArcaneViolet,
        cornerRadius = 16.dp,
        contentPadding = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = SystemColors.SystemBlue.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SystemColors.SystemBlue.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "DOMAINS // FIVE REALMS OF MASTERY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        letterSpacing = 1.0.sp
                    ),
                    color = SystemColors.SystemBlue,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Text(
                text = "YOUR ACTIVE DOMAINS",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = 0.8.sp
                ),
                color = SystemColors.PureIce
            )

            Text(
                text = "REALITY OS coordinates your personal development across five operational command centres:",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    lineHeight = 20.sp
                ),
                color = SystemColors.MetallicSilver,
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DomainOverviewCard(
                    title = "Physical Development",
                    subtitle = "Workouts, Jogging & Health Connect",
                    description = "Strength training logs, distance running, heart rate telemetry, and recovery scores with automated biometric sync.",
                    icon = Icons.Rounded.FitnessCenter,
                    accentColor = SystemColors.ElectricCyan
                )
                DomainOverviewCard(
                    title = "Academic Development",
                    subtitle = "Focus Blocks & Syllabus Mastery",
                    description = "Subject syllabus breakdown, Pomodoro deep-work timers, and spaced repetition revision intervals.",
                    icon = Icons.Rounded.School,
                    accentColor = SystemColors.SystemBlue
                )
                DomainOverviewCard(
                    title = "Life & Habits",
                    subtitle = "Routines & Discipline Engine",
                    description = "Morning and evening routine sequencing, habit streak multipliers, personal milestones, and unified scheduling.",
                    icon = Icons.Rounded.CalendarToday,
                    accentColor = SystemColors.VerifiedEmerald
                )
                DomainOverviewCard(
                    title = "Quests & Missions",
                    subtitle = "Daily Tasks & Rank Trials",
                    description = "Mandatory daily quest objectives, urgent side missions, dungeon encounters, and penalty zone avoidance.",
                    icon = Icons.Rounded.Assignment,
                    accentColor = SystemColors.WarningAmber
                )
                DomainOverviewCard(
                    title = "Shadow Army",
                    subtitle = "Conquered Hurdles & Legion Roster",
                    description = "Reanimate overcome challenges into loyal shadows. Level up shadow ranks and deploy them on passive developmental duties.",
                    icon = Icons.Rounded.Shield,
                    accentColor = SystemColors.ArcaneViolet
                )
            }
        }
    }
}

@Composable
private fun DomainOverviewCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    accentColor: Color
) {
    Surface(
        color = SystemColors.PanelSurface.copy(alpha = 0.75f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.75.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    ),
                    color = SystemColors.PureIce
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp
                    ),
                    color = accentColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = InterFontFamily,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    ),
                    color = SystemColors.SecondaryText
                )
            }
        }
    }
}

// ============================================================================
// SCREEN 4 — THE SYSTEM
// ============================================================================
@Composable
private fun ScreenTheSystem() {
    HolographicPanel(
        modifier = Modifier.fillMaxWidth(),
        accentColor = SystemColors.ArcaneViolet,
        secondaryColor = SystemColors.ElectricCyan,
        cornerRadius = 16.dp,
        contentPadding = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = SystemColors.ArcaneViolet.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SystemColors.ArcaneViolet.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "OPERATING SYSTEM // CORE MECHANICS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        letterSpacing = 1.0.sp
                    ),
                    color = SystemColors.ArcaneViolet,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Text(
                text = "HOW THE SYSTEM OPERATES",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = 0.8.sp
                ),
                color = SystemColors.PureIce
            )

            Text(
                text = "Deterministic rules govern every cycle to ensure uncompromised real-world progression:",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    lineHeight = 20.sp
                ),
                color = SystemColors.MetallicSilver,
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SystemMechanicCard(
                    title = "Daily Missions & 04:00 AM Reset",
                    description = "Mandatory quests generate daily. Complete all objectives before the 04:00 AM reset to protect your streak and avoid the Penalty Zone."
                )
                SystemMechanicCard(
                    title = "Deterministic Progress Tracking",
                    description = "Logged activities produce mathematical XP across your 7 attributes. All telemetry runs locally on-device without synthetic inflation."
                )
                SystemMechanicCard(
                    title = "Recovery Recommendations & Sleep",
                    description = "Cardiovascular strain and exertion are continuously balanced against rest protocols to prevent overtraining and burnout."
                )
                SystemMechanicCard(
                    title = "Hunter Rank Ascension",
                    description = "Accumulate XP to trigger Rank Gate trials. Advance from E-Rank through National-Level Hunter as your real capabilities ascend."
                )
                SystemMechanicCard(
                    title = "Shadow Monarch Capabilities",
                    description = "Reanimate overcome challenges into loyal shadows. Deploy your legion on passive duties and strengthen shadow synergy."
                )
            }
        }
    }
}

@Composable
private fun SystemMechanicCard(
    title: String,
    description: String
) {
    Surface(
        color = SystemColors.PanelSurface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.75.dp, SystemColors.SubtleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = SystemColors.ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    ),
                    color = SystemColors.PureIce
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = InterFontFamily,
                    lineHeight = 16.sp
                ),
                color = SystemColors.SecondaryText
            )
        }
    }
}

// ============================================================================
// SCREEN 5 — ENTER THE SYSTEM
// ============================================================================
@Composable
private fun ScreenEnterTheSystem() {
    HolographicPanel(
        modifier = Modifier.fillMaxWidth(),
        accentColor = SystemColors.MonarchGold,
        secondaryColor = SystemColors.ElectricCyan,
        cornerRadius = 16.dp,
        contentPadding = 24.dp,
        isHero = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = SystemColors.MonarchGold.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SystemColors.MonarchGold.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "INITIALIZATION // FINAL PROTOCOL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        letterSpacing = 1.0.sp
                    ),
                    color = SystemColors.MonarchGold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Glowing Monarch Emblem
            Surface(
                color = SystemColors.MonarchGold.copy(alpha = 0.12f),
                shape = CircleShape,
                border = BorderStroke(1.5.dp, SystemColors.MonarchGold.copy(alpha = 0.7f)),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = "Monarch Emblem",
                        tint = SystemColors.MonarchGold,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "ARISE.\nYOUR STORY STARTS HERE.",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    letterSpacing = 1.2.sp
                ),
                color = SystemColors.PureIce,
                textAlign = TextAlign.Center
            )

            Text(
                text = "The contract is forged. The Reality Operating System is calibrated to your baseline. Step into your realm, execute your daily quests, and arise.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    lineHeight = 22.sp
                ),
                color = SystemColors.MetallicSilver,
                textAlign = TextAlign.Center
            )

            // Readiness checklist
            Surface(
                color = SystemColors.PanelSurface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SystemColors.SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReadinessItem(label = "Neural Interface & Local DB", isOnline = true)
                    ReadinessItem(label = "Daily Quest Matrix & Penalty Shield", isOnline = true)
                    ReadinessItem(label = "Physical, Academic & Life Engines", isOnline = true)
                    ReadinessItem(label = "Shadow Realm Gateway & Army Roster", isOnline = true)
                    ReadinessItem(label = "Biometric Telemetry & Recovery Monitor", isOnline = true)
                }
            }

            Text(
                text = "TAP 'INITIALIZE SYSTEM' TO ENTER HOME DASHBOARD",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    letterSpacing = 1.0.sp
                ),
                color = SystemColors.ElectricCyan
            )
        }
    }
}

@Composable
private fun ReadinessItem(label: String, isOnline: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) SystemColors.VerifiedEmerald else SystemColors.WarningAmber)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = InterFontFamily
                ),
                color = SystemColors.PureIce
            )
        }
        Text(
            text = if (isOnline) "ONLINE" else "STANDBY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = if (isOnline) SystemColors.VerifiedEmerald else SystemColors.WarningAmber
        )
    }
}
