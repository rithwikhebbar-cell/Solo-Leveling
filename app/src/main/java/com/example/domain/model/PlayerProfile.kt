package com.example.domain.model

data class PlayerProfile(
    val codename: String = "Player 01",
    val title: String = "Awakened Initiate",
    val level: Int = 1,
    val currentLevelXp: Int = 0,
    val xpToNextLevel: Int = 100,
    val totalXp: Long = 0L,
    val rank: HunterRank = HunterRank.E_RANK,
    val attributes: PlayerAttributes = PlayerAttributes(),
    val streakDays: Int = 0,
    val verifiedActivitiesCount: Int = 0,
    val selfReportedActivitiesCount: Int = 0,
    val recoveryScore: Int = 100,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val levelProgressFraction: Float
        get() = if (xpToNextLevel <= 0) 0f else (currentLevelXp.toFloat() / xpToNextLevel).coerceIn(0f, 1f)

    val totalActivitiesCount: Int
        get() = verifiedActivitiesCount + selfReportedActivitiesCount

    val verifiedRatio: Float
        get() = if (totalActivitiesCount == 0) 0f else verifiedActivitiesCount.toFloat() / totalActivitiesCount
}
