package com.example.domain.model

enum class QuestType(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    LONG_TERM("Long-Term")
}

enum class QuestCategory(val label: String) {
    PHYSICAL("Physical"),
    INTELLECT("Intellect"),
    DISCIPLINE("Discipline"),
    RECOVERY("Recovery"),
    SOCIAL("Social")
}

enum class VerificationStatus(val label: String, val isVerified: Boolean) {
    VERIFIED_HEALTH_CONNECT("Health Connect Verified", true),
    VERIFIED_ASSESSMENT("Assessment Verified", true),
    VERIFIED_SYSTEM_TIMER("System Timer Verified", true),
    SELF_REPORTED("Self-Reported", false)
}

data class Quest(
    val id: Long = 0,
    val title: String,
    val description: String,
    val type: QuestType,
    val category: QuestCategory,
    val targetValue: Int,
    val currentValue: Int,
    val unit: String,
    val xpReward: Int,
    val primaryAttribute: AttributeType,
    val primaryAttrXpReward: Int,
    val secondaryAttribute: AttributeType? = null,
    val secondaryAttrXpReward: Int = 0,
    val isCompleted: Boolean = false,
    val verificationStatus: VerificationStatus = VerificationStatus.SELF_REPORTED,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val completedAtEpochMs: Long? = null
) {
    val progressFraction: Float
        get() = if (targetValue <= 0) 0f else (currentValue.toFloat() / targetValue).coerceIn(0f, 1f)
}
