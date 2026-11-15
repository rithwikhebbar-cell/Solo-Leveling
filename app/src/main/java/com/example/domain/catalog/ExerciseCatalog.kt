package com.example.domain.catalog

import com.example.domain.model.AttributeType
import com.example.domain.model.ExerciseDefinition
import com.example.domain.model.WorkoutCategory

/**
 * Reusable structured exercise catalog used for workout construction
 * and future AI workout program generation. Does not generate arbitrary fake logs.
 */
object ExerciseCatalog {

    val allExercises: List<ExerciseDefinition> = listOf(
        ExerciseDefinition(
            id = "barbell_squat",
            name = "Goblet / Barbell Squat",
            category = WorkoutCategory.STRENGTH,
            primaryAttribute = AttributeType.STR,
            secondaryAttribute = AttributeType.VIT,
            defaultSets = 4,
            defaultReps = 8,
            defaultRestSeconds = 90,
            cue = "Brace core, full depth with controlled eccentric."
        ),
        ExerciseDefinition(
            id = "romanian_deadlift",
            name = "Romanian Deadlift",
            category = WorkoutCategory.STRENGTH,
            primaryAttribute = AttributeType.STR,
            secondaryAttribute = AttributeType.DIS,
            defaultSets = 3,
            defaultReps = 10,
            defaultRestSeconds = 90,
            cue = "Hinge at hips, keep neutral spine."
        ),
        ExerciseDefinition(
            id = "overhead_press",
            name = "Overhead Press",
            category = WorkoutCategory.STRENGTH,
            primaryAttribute = AttributeType.STR,
            secondaryAttribute = AttributeType.DIS,
            defaultSets = 3,
            defaultReps = 8,
            defaultRestSeconds = 75,
            cue = "Lock ribcage down, press straight overhead."
        ),
        ExerciseDefinition(
            id = "dumbbell_row",
            name = "Single-Arm Row",
            category = WorkoutCategory.HYPERTROPHY,
            primaryAttribute = AttributeType.STR,
            secondaryAttribute = AttributeType.VIT,
            defaultSets = 3,
            defaultReps = 12,
            defaultRestSeconds = 60,
            cue = "Pull elbow toward hip, pause at contraction."
        ),
        ExerciseDefinition(
            id = "walking_lunge",
            name = "Split / Walking Lunges",
            category = WorkoutCategory.HYPERTROPHY,
            primaryAttribute = AttributeType.AGI,
            secondaryAttribute = AttributeType.STR,
            defaultSets = 3,
            defaultReps = 12,
            defaultRestSeconds = 60,
            cue = "Controlled knee tracking and upright torso."
        ),
        ExerciseDefinition(
            id = "push_ups",
            name = "Strict Push-Ups",
            category = WorkoutCategory.CALISTHENICS,
            primaryAttribute = AttributeType.STR,
            secondaryAttribute = AttributeType.DIS,
            defaultSets = 4,
            defaultReps = 15,
            defaultRestSeconds = 60,
            cue = "Full lockout, elbows 45 degrees."
        ),
        ExerciseDefinition(
            id = "pull_ups",
            name = "Pull-Ups / Inverted Rows",
            category = WorkoutCategory.CALISTHENICS,
            primaryAttribute = AttributeType.STR,
            secondaryAttribute = AttributeType.AGI,
            defaultSets = 3,
            defaultReps = 8,
            defaultRestSeconds = 90,
            cue = "Full dead hang to chest clearance."
        ),
        ExerciseDefinition(
            id = "plank_hold",
            name = "Forearm Plank Hold",
            category = WorkoutCategory.CALISTHENICS,
            primaryAttribute = AttributeType.DIS,
            secondaryAttribute = AttributeType.VIT,
            defaultSets = 3,
            defaultReps = 45,
            defaultRestSeconds = 45,
            isTimedSeconds = true,
            cue = "Seconds hold — glutes and ribs engaged."
        ),
        ExerciseDefinition(
            id = "burpee_intervals",
            name = "Conditioning Intervals",
            category = WorkoutCategory.CONDITIONING,
            primaryAttribute = AttributeType.VIT,
            secondaryAttribute = AttributeType.AGI,
            defaultSets = 4,
            defaultReps = 10,
            defaultRestSeconds = 60,
            cue = "Steady aerobic rhythm, stop if form degrades."
        ),
        ExerciseDefinition(
            id = "shuttle_agility",
            name = "Agility Footwork Drills",
            category = WorkoutCategory.CONDITIONING,
            primaryAttribute = AttributeType.AGI,
            secondaryAttribute = AttributeType.VIT,
            defaultSets = 4,
            defaultReps = 30,
            defaultRestSeconds = 45,
            isTimedSeconds = true,
            cue = "Seconds crisp lateral & acceleration footwork."
        ),
        ExerciseDefinition(
            id = "hip_thoracic_mobility",
            name = "Hip & Thoracic Mobility Flow",
            category = WorkoutCategory.MOBILITY,
            primaryAttribute = AttributeType.AGI,
            secondaryAttribute = AttributeType.WIS,
            defaultSets = 2,
            defaultReps = 60,
            defaultRestSeconds = 30,
            isTimedSeconds = true,
            cue = "Seconds slow nasal-breathing joint mobility."
        ),
        ExerciseDefinition(
            id = "cooldown_stretch",
            name = "Lower-Body Recovery Stretch",
            category = WorkoutCategory.MOBILITY,
            primaryAttribute = AttributeType.VIT,
            secondaryAttribute = AttributeType.WIS,
            defaultSets = 2,
            defaultReps = 60,
            defaultRestSeconds = 20,
            isTimedSeconds = true,
            cue = "Seconds gentle calves, hamstrings, and hip flexors."
        )
    )

    fun byCategory(category: WorkoutCategory): List<ExerciseDefinition> =
        allExercises.filter { it.category == category }
}
