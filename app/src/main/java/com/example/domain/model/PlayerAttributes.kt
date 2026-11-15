package com.example.domain.model

data class AttributeBreakdown(
    val type: AttributeType,
    val baseValue: Int = INITIAL_BASE_ATTRIBUTE,
    val verifiedPoints: Int = 0,
    val selfReportedPoints: Int = 0,
    val progressXp: Int = 0,
    val progressXpRequired: Int = ATTRIBUTE_XP_PER_POINT
) {
    val totalValue: Int
        get() = baseValue + verifiedPoints + selfReportedPoints

    val progressFraction: Float
        get() = (progressXp.toFloat() / progressXpRequired.coerceAtLeast(1)).coerceIn(0f, 1f)

    companion object {
        const val INITIAL_BASE_ATTRIBUTE = 10
        const val ATTRIBUTE_XP_PER_POINT = 100
    }
}

data class PlayerAttributes(
    val str: AttributeBreakdown = AttributeBreakdown(AttributeType.STR),
    val vit: AttributeBreakdown = AttributeBreakdown(AttributeType.VIT),
    val agi: AttributeBreakdown = AttributeBreakdown(AttributeType.AGI),
    val intel: AttributeBreakdown = AttributeBreakdown(AttributeType.INT),
    val dis: AttributeBreakdown = AttributeBreakdown(AttributeType.DIS),
    val wis: AttributeBreakdown = AttributeBreakdown(AttributeType.WIS),
    val cha: AttributeBreakdown = AttributeBreakdown(AttributeType.CHA)
) {
    fun asList(): List<AttributeBreakdown> = listOf(str, vit, agi, intel, dis, wis, cha)

    fun byType(type: AttributeType): AttributeBreakdown = when (type) {
        AttributeType.STR -> str
        AttributeType.VIT -> vit
        AttributeType.AGI -> agi
        AttributeType.INT -> intel
        AttributeType.DIS -> dis
        AttributeType.WIS -> wis
        AttributeType.CHA -> cha
    }

    val totalScore: Int
        get() = asList().sumOf { it.totalValue }

    val totalVerifiedPoints: Int
        get() = asList().sumOf { it.verifiedPoints }

    val totalSelfReportedPoints: Int
        get() = asList().sumOf { it.selfReportedPoints }
}
