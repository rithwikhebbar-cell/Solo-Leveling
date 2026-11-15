package com.example.domain.model

data class RankThreshold(
    val minLevel: Int,
    val minTotalAttributes: Int,
    val minVerifiedActivities: Int
)

enum class HunterRank(
    val code: String,
    val displayName: String,
    val title: String,
    val threshold: RankThreshold
) {
    E_RANK(
        code = "E",
        displayName = "E-Rank",
        title = "Newly Awakened",
        threshold = RankThreshold(minLevel = 1, minTotalAttributes = 70, minVerifiedActivities = 0)
    ),
    D_RANK(
        code = "D",
        displayName = "D-Rank",
        title = "Field Initiate",
        threshold = RankThreshold(minLevel = 5, minTotalAttributes = 85, minVerifiedActivities = 3)
    ),
    C_RANK(
        code = "C",
        displayName = "C-Rank",
        title = "Vanguard Hunter",
        threshold = RankThreshold(minLevel = 12, minTotalAttributes = 115, minVerifiedActivities = 10)
    ),
    B_RANK(
        code = "B",
        displayName = "B-Rank",
        title = "Strike Specialist",
        threshold = RankThreshold(minLevel = 22, minTotalAttributes = 160, minVerifiedActivities = 25)
    ),
    A_RANK(
        code = "A",
        displayName = "A-Rank",
        title = "Elite Commander",
        threshold = RankThreshold(minLevel = 35, minTotalAttributes = 220, minVerifiedActivities = 50)
    ),
    S_RANK(
        code = "S",
        displayName = "S-Rank",
        title = "Apex Sovereign",
        threshold = RankThreshold(minLevel = 50, minTotalAttributes = 300, minVerifiedActivities = 90)
    ),
    NATIONAL_LEVEL(
        code = "NAT",
        displayName = "National Level",
        title = "Monarch of Reality",
        threshold = RankThreshold(minLevel = 75, minTotalAttributes = 420, minVerifiedActivities = 150)
    );

    fun nextRank(): HunterRank? {
        val all = entries
        val idx = all.indexOf(this)
        return if (idx in 0 until all.lastIndex) all[idx + 1] else null
    }
}
