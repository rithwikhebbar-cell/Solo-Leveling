package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.domain.model.AttributeType
import com.example.domain.model.HunterRank

object SystemColors {
    val ObsidianBlack = Color(0xFF070A13)
    val DeepNavy = Color(0xFF0D1424)
    val SecondarySurface = Color(0xFF111B2D)
    val PanelSurface = Color(0xFF121C32)
    val PanelSurfaceElevated = Color(0xFF172440)

    val ElectricCyan = Color(0xFF00E5FF)
    val ElectricCyanDim = Color(0xFF0891B2)
    val ArcaneViolet = Color(0xFF8B5CF6)
    val MysticPurple = Color(0xFF8B5CF6)
    val SystemBlue = Color(0xFF287BFF)
    val NeonBlue = Color(0xFF287BFF)
    val MetallicSilver = Color(0xFFCBD5E1)
    val MutedSilver = Color(0xFF94A3B8)
    val SecondaryText = Color(0xFF94A3B8)
    val PureIce = Color(0xFFF5F7FF)
    val PrimaryText = Color(0xFFF5F7FF)

    val VerifiedEmerald = Color(0xFF10B981)
    val WarningAmber = Color(0xFFF59E0B)
    val DangerCrimson = Color(0xFFEF4444)
    val MonarchGold = Color(0xFFFBBF24)

    val PanelBorderCyan = Color(0x4D00E5FF)
    val PanelBorderViolet = Color(0x4D8B5CF6)
    val SubtleGridLine = Color(0x1F38BDF8)
    val SubtleBorder = Color(0x2E38BDF8)
    val CardGlowLow = Color(0x1A00E5FF)

    fun rankAccent(rank: HunterRank): Color = when (rank) {
        HunterRank.E_RANK -> MetallicSilver
        HunterRank.D_RANK -> VerifiedEmerald
        HunterRank.C_RANK -> SystemBlue
        HunterRank.B_RANK -> ElectricCyan
        HunterRank.A_RANK -> ArcaneViolet
        HunterRank.S_RANK -> MonarchGold
        HunterRank.NATIONAL_LEVEL -> DangerCrimson
    }

    fun attributeAccent(type: AttributeType): Color = when (type) {
        AttributeType.STR -> DangerCrimson
        AttributeType.VIT -> VerifiedEmerald
        AttributeType.AGI -> ElectricCyan
        AttributeType.INT -> SystemBlue
        AttributeType.DIS -> ArcaneViolet
        AttributeType.WIS -> MonarchGold
        AttributeType.CHA -> Color(0xFFEC4899)
    }
}
