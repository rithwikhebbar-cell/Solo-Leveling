package com.example.domain.model

enum class AttributeType(
    val code: String,
    val displayName: String,
    val description: String,
    val trainingFocus: String
) {
    STR(
        code = "STR",
        displayName = "Strength",
        description = "Physical force, resistance capacity, and anaerobic power.",
        trainingFocus = "Resistance & strength workouts"
    ),
    VIT(
        code = "VIT",
        displayName = "Vitality",
        description = "Endurance, cardiovascular stamina, recovery, and physical resilience.",
        trainingFocus = "Jogging, sleep hygiene & recovery"
    ),
    AGI(
        code = "AGI",
        displayName = "Agility",
        description = "Speed, cadence, mobility, and responsive coordination.",
        trainingFocus = "Interval runs, mobility & agility drills"
    ),
    INT(
        code = "INT",
        displayName = "Intelligence",
        description = "Analytical depth, problem-solving, and academic mastery.",
        trainingFocus = "Focused study, reading & technical work"
    ),
    DIS(
        code = "DIS",
        displayName = "Discipline",
        description = "Consistency under friction, habit adherence, and execution follow-through.",
        trainingFocus = "Quest streaks & scheduled commitments"
    ),
    WIS(
        code = "WIS",
        displayName = "Wisdom",
        description = "Strategic judgement, reflection, mindfulness, and energy pacing.",
        trainingFocus = "Journaling, reflection & recovery pacing"
    ),
    CHA(
        code = "CHA",
        displayName = "Charisma",
        description = "Communication clarity, leadership presence, and constructive influence.",
        trainingFocus = "Collaboration, speaking & community impact"
    )
}
