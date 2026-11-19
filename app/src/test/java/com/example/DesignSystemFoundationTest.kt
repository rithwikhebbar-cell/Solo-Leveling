package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AttributeType
import com.example.domain.model.HunterRank
import com.example.ui.theme.RealityOsShapes
import com.example.ui.theme.RealityOsTypography
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DesignSystemFoundationTest {

    // ========================================================================
    // 1. COLOUR PALETTE TOKENS
    // ========================================================================
    @Test
    fun colorTokens_matchStandardizedSpecification() {
        assertEquals(Color(0xFF070A13), SystemColors.ObsidianBlack)
        assertEquals(Color(0xFF0D1424), SystemColors.DeepNavy)
        assertEquals(Color(0xFF121C32), SystemColors.PanelSurface)
        assertEquals(Color(0xFF172440), SystemColors.PanelSurfaceElevated)
        assertEquals(Color(0xFF00E5FF), SystemColors.ElectricCyan)
        assertEquals(Color(0xFF8B5CF6), SystemColors.ArcaneViolet)
        assertEquals(Color(0xFF287BFF), SystemColors.SystemBlue)
        assertEquals(Color(0xFFF5F7FF), SystemColors.PureIce)
        assertEquals(Color(0xFF94A3B8), SystemColors.MutedSilver)
        assertEquals(Color(0xFF10B981), SystemColors.VerifiedEmerald)
        assertEquals(Color(0xFFF59E0B), SystemColors.WarningAmber)
        assertEquals(Color(0xFFEF4444), SystemColors.DangerCrimson)
        assertEquals(Color(0xFFFBBF24), SystemColors.MonarchGold)
    }

    @Test
    fun rankAccent_mapsAllHunterRanksDeterministically() {
        HunterRank.entries.forEach { rank ->
            val accent = SystemColors.rankAccent(rank)
            assertNotNull(accent)
            assertNotEquals(Color.Unspecified, accent)
        }
        assertEquals(SystemColors.MetallicSilver, SystemColors.rankAccent(HunterRank.E_RANK))
        assertEquals(SystemColors.VerifiedEmerald, SystemColors.rankAccent(HunterRank.D_RANK))
        assertEquals(SystemColors.SystemBlue, SystemColors.rankAccent(HunterRank.C_RANK))
        assertEquals(SystemColors.ElectricCyan, SystemColors.rankAccent(HunterRank.B_RANK))
        assertEquals(SystemColors.ArcaneViolet, SystemColors.rankAccent(HunterRank.A_RANK))
        assertEquals(SystemColors.MonarchGold, SystemColors.rankAccent(HunterRank.S_RANK))
        assertEquals(SystemColors.DangerCrimson, SystemColors.rankAccent(HunterRank.NATIONAL_LEVEL))
    }

    @Test
    fun attributeAccent_mapsAllSevenAttributesDeterministically() {
        AttributeType.entries.forEach { attr ->
            val accent = SystemColors.attributeAccent(attr)
            assertNotNull(accent)
            assertNotEquals(Color.Unspecified, accent)
        }
        assertEquals(SystemColors.DangerCrimson, SystemColors.attributeAccent(AttributeType.STR))
        assertEquals(SystemColors.VerifiedEmerald, SystemColors.attributeAccent(AttributeType.VIT))
        assertEquals(SystemColors.ElectricCyan, SystemColors.attributeAccent(AttributeType.AGI))
        assertEquals(SystemColors.SystemBlue, SystemColors.attributeAccent(AttributeType.INT))
        assertEquals(SystemColors.ArcaneViolet, SystemColors.attributeAccent(AttributeType.DIS))
        assertEquals(SystemColors.MonarchGold, SystemColors.attributeAccent(AttributeType.WIS))
    }

    // ========================================================================
    // 2. TYPOGRAPHY TOKENS
    // ========================================================================
    @Test
    fun typography_matchesStandardizedFontSizes() {
        // Rajdhani display and headlines
        assertEquals(32.sp, RealityOsTypography.displayLarge.fontSize)
        assertEquals(24.sp, RealityOsTypography.headlineLarge.fontSize)
        assertEquals(20.sp, RealityOsTypography.headlineMedium.fontSize)
        assertEquals(17.sp, RealityOsTypography.titleLarge.fontSize)
        assertEquals(15.sp, RealityOsTypography.titleMedium.fontSize)
        assertEquals(13.5.sp, RealityOsTypography.titleSmall.fontSize)

        // Inter body fonts
        assertEquals(15.sp, RealityOsTypography.bodyLarge.fontSize)
        assertEquals(22.sp, RealityOsTypography.bodyLarge.lineHeight)
        assertEquals(14.sp, RealityOsTypography.bodyMedium.fontSize)
        assertEquals(20.sp, RealityOsTypography.bodyMedium.lineHeight)
        assertEquals(12.5.sp, RealityOsTypography.bodySmall.fontSize)
        assertEquals(17.sp, RealityOsTypography.bodySmall.lineHeight)

        // JetBrains Mono labels and telemetry
        assertEquals(13.sp, RealityOsTypography.labelLarge.fontSize)
        assertEquals(11.5.sp, RealityOsTypography.labelMedium.fontSize)
        assertEquals(10.5.sp, RealityOsTypography.labelSmall.fontSize)
    }

    // ========================================================================
    // 3. SPACING, SHAPES & TOUCH TARGET TOKENS
    // ========================================================================
    @Test
    fun spacingTokens_adhereToAccessibilityAndLayoutStandards() {
        assertEquals(16.dp, SystemSpacing.screenHorizontal)
        assertEquals(760.dp, SystemSpacing.maxContentWidth)
        assertEquals(16.dp, SystemSpacing.sectionSpacing)
        assertEquals(48.dp, SystemSpacing.minTouchTarget)
        assertEquals(48.dp, SystemSpacing.buttonHeight)

        assertEquals(6.dp, SystemSpacing.cornerSmall)
        assertEquals(10.dp, SystemSpacing.cornerMedium)
        assertEquals(14.dp, SystemSpacing.cornerLarge)
        assertEquals(18.dp, SystemSpacing.cornerExtraLarge)

        assertEquals(0.75.dp, SystemSpacing.borderSubtle)
        assertEquals(1.dp, SystemSpacing.borderStandard)
        assertEquals(1.25.dp, SystemSpacing.borderHero)
    }

    @Test
    fun shapes_matchDefinedCornerRadii() {
        assertNotNull(RealityOsShapes.small)
        assertNotNull(RealityOsShapes.medium)
        assertNotNull(RealityOsShapes.large)
        assertNotNull(RealityOsShapes.extraLarge)
    }
}
