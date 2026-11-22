package com.example.ui.screens.experience

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.domain.model.AwakeningStage
import com.example.domain.model.PlayerProfile
import com.example.ui.screens.onboarding.CinematicOnboardingScreen

/**
 * Milestone 10 / Stage 9.1: System Awakening Sequence & Cinematic Onboarding Experience.
 *
 * Hosted at application startup for unawakened players or replayed on demand from Settings.
 * Forwards to [CinematicOnboardingScreen] to present the canonical 5-stage Reality OS onboarding:
 * 1. THE AWAKENING (Personal development system & reality rulebook)
 * 2. YOUR IDENTITY (Player profile, levels, ranks, XP & 7 canonical attributes)
 * 3. YOUR DOMAINS (Physical, Academic, Life & Habits, Quests, Shadow Army)
 * 4. THE SYSTEM (Daily missions, progress tracking, recovery recommendations, progression rules)
 * 5. ENTER THE SYSTEM ("ARISE. YOUR STORY STARTS HERE." & Initialize System trigger)
 */
@Composable
fun AwakeningScreen(
    player: PlayerProfile,
    animationsEnabled: Boolean,
    reducedMotionEnabled: Boolean,
    onStageShown: (AwakeningStage) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    CinematicOnboardingScreen(
        player = player,
        animationsEnabled = animationsEnabled,
        reducedMotionEnabled = reducedMotionEnabled,
        onStageShown = onStageShown,
        onComplete = onComplete,
        modifier = modifier
    )
}
