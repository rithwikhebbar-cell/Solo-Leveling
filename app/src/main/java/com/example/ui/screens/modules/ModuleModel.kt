package com.example.ui.screens.modules

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ModuleCategory(val label: String, val description: String) {
    ALL("All Systems", "Complete inventory of Reality OS subsystems"),
    DEVELOPMENT("Player Development", "Physical, academic, social & habit mastery engines"),
    PROGRESSION("RPG & Monarch", "Shadow Monarch progression, dungeons, skills & achievements"),
    SYSTEM_AI("System & AI", "Intelligence, voice, security, cloud sync & system controls")
}

enum class ModuleOperationalStatus(val label: String, val isOperational: Boolean) {
    ACTIVE("ACTIVE", true),
    AVAILABLE("AVAILABLE", true),
    LOCKED("LOCKED", false),
    COMING_SOON("COMING SOON", false),
    UNAVAILABLE("UNAVAILABLE", false)
}

data class ModuleSpec(
    val id: String,
    val title: String,
    val description: String,
    val capabilitySummary: String,
    val category: ModuleCategory,
    val status: ModuleOperationalStatus,
    val statusRationale: String,
    val icon: ImageVector,
    val accentColor: Color,
    val metricHighlight: String,
    val destinationLabel: String,
    val onClick: () -> Unit
)
