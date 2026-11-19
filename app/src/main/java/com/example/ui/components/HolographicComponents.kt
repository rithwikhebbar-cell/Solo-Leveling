package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.HunterRank
import com.example.domain.model.VerificationStatus
import com.example.ui.theme.LocalHudGlowEnabled
import com.example.ui.theme.SystemColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun HolographicPanel(
    modifier: Modifier = Modifier,
    accentColor: Color = SystemColors.ElectricCyan,
    secondaryColor: Color = SystemColors.ArcaneViolet,
    cornerRadius: Dp = 14.dp,
    contentPadding: Dp = 16.dp,
    isHero: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val glowEnabled = LocalHudGlowEnabled.current
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = if (isHero && glowEnabled) accentColor.copy(alpha = 0.5f) else accentColor.copy(alpha = 0.22f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        SystemColors.DeepNavy.copy(alpha = 0.96f),
                        SystemColors.PanelSurface.copy(alpha = 0.92f),
                        SystemColors.DeepNavy.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = if (isHero) 1.25.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isHero) {
                        listOf(borderColor, secondaryColor.copy(alpha = 0.35f), borderColor)
                    } else {
                        listOf(borderColor, borderColor.copy(alpha = 0.15f), borderColor)
                    }
                ),
                shape = shape
            )
            .drawBehind {
                if (glowEnabled && isHero) {
                    val bracketLen = 14.dp.toPx()
                    val stroke = 2.dp.toPx()
                    val pad = 1.dp.toPx()
                    // Top-left bracket
                    drawLine(accentColor, Offset(pad, pad), Offset(pad + bracketLen, pad), strokeWidth = stroke)
                    drawLine(accentColor, Offset(pad, pad), Offset(pad, pad + bracketLen), strokeWidth = stroke)
                    // Top-right bracket
                    drawLine(accentColor, Offset(size.width - pad - bracketLen, pad), Offset(size.width - pad, pad), strokeWidth = stroke)
                    drawLine(accentColor, Offset(size.width - pad, pad), Offset(size.width - pad, pad + bracketLen), strokeWidth = stroke)
                    // Bottom-left bracket
                    drawLine(secondaryColor, Offset(pad, size.height - pad), Offset(pad + bracketLen, size.height - pad), strokeWidth = stroke)
                    drawLine(secondaryColor, Offset(pad, size.height - pad - bracketLen), Offset(pad, size.height - pad), strokeWidth = stroke)
                    // Bottom-right bracket
                    drawLine(secondaryColor, Offset(size.width - pad - bracketLen, size.height - pad), Offset(size.width - pad, size.height - pad), strokeWidth = stroke)
                    drawLine(secondaryColor, Offset(size.width - pad, size.height - pad - bracketLen), Offset(size.width - pad, size.height - pad), strokeWidth = stroke)
                }
            }
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun SystemSectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(SystemColors.ElectricCyan, SystemColors.ArcaneViolet)
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = SystemColors.PureIce
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun GlowingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    startColor: Color = SystemColors.ElectricCyan,
    endColor: Color = SystemColors.ArcaneViolet,
    trackColor: Color = Color(0xFF1A2642),
    fillColor: Color? = null
) {
    val effectiveStart = fillColor ?: startColor
    val effectiveEnd = fillColor ?: endColor
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "glow_progress"
    )
    val glowEnabled = LocalHudGlowEnabled.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .border(0.5.dp, SystemColors.SubtleGridLine, RoundedCornerShape(50))
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(height)
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(effectiveStart, effectiveEnd)
                        )
                    )
                    .drawBehind {
                        if (glowEnabled) {
                            drawRect(
                                color = effectiveStart.copy(alpha = 0.25f)
                            )
                        }
                    }
            )
        }
    }
}

@Composable
fun RankBadge(
    rank: HunterRank,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val accent = SystemColors.rankAccent(rank)
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rank.code,
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                color = accent
            )
            if (!compact) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RANK",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MetallicSilver
                )
            }
        }
    }
}

@Composable
fun ProvenanceChip(
    status: VerificationStatus,
    modifier: Modifier = Modifier
) {
    val isVerified = status.isVerified
    val color = when (status) {
        VerificationStatus.VERIFIED_HEALTH_CONNECT,
        VerificationStatus.VERIFIED_ASSESSMENT,
        VerificationStatus.VERIFIED_AI_RUBRIC -> SystemColors.VerifiedEmerald
        VerificationStatus.VERIFIED_SYSTEM_TIMER -> SystemColors.ElectricCyan
        VerificationStatus.SELF_REPORTED,
        VerificationStatus.PENDING_REVIEW -> SystemColors.WarningAmber
    }
    val icon = if (isVerified) Icons.Rounded.VerifiedUser else Icons.Rounded.EditNote
    val label = when (status) {
        VerificationStatus.VERIFIED_HEALTH_CONNECT -> "VERIFIED"
        VerificationStatus.VERIFIED_ASSESSMENT -> "ASSESSMENT VERIFIED"
        VerificationStatus.VERIFIED_AI_RUBRIC -> "RUBRIC VERIFIED"
        VerificationStatus.VERIFIED_SYSTEM_TIMER -> "TIMER VERIFIED"
        VerificationStatus.SELF_REPORTED -> "SELF-REPORTED"
        VerificationStatus.PENDING_REVIEW -> "PENDING REVIEW"
    }

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.75.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status.label,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
fun AttributeRadarChart(
    attributes: List<AttributeBreakdown>,
    modifier: Modifier = Modifier
) {
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = SystemColors.MetallicSilver)
    val valueStyle = MaterialTheme.typography.labelSmall.copy(color = SystemColors.ElectricCyan)

    val maxStat = (attributes.maxOfOrNull { it.totalValue } ?: 20).coerceAtLeast(20)
    val scaleMax = ((maxStat / 10) + 1) * 10

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        if (attributes.isEmpty()) return@Canvas
        val count = attributes.size
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) * 0.34f
        val angleStep = (2.0 * PI / count).toFloat()
        val startAngle = (-PI / 2.0).toFloat()

        // Concentric heptagon grid rings
        for (ring in 1..4) {
            val ringRadius = radius * (ring / 4f)
            val ringPath = Path()
            for (i in 0 until count) {
                val angle = startAngle + i * angleStep
                val x = center.x + ringRadius * cos(angle)
                val y = center.y + ringRadius * sin(angle)
                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
            }
            ringPath.close()
            drawPath(
                path = ringPath,
                color = SystemColors.SubtleGridLine,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Spokes and vertex labels
        for (i in 0 until count) {
            val angle = startAngle + i * angleStep
            val outerX = center.x + radius * cos(angle)
            val outerY = center.y + radius * sin(angle)
            drawLine(
                color = SystemColors.SubtleGridLine,
                start = center,
                end = Offset(outerX, outerY),
                strokeWidth = 1.dp.toPx()
            )

            val attr = attributes[i]
            val labelRadius = radius + 24.dp.toPx()
            val lx = center.x + labelRadius * cos(angle)
            val ly = center.y + labelRadius * sin(angle)

            val codeLayout = textMeasurer.measure(attr.type.code, style = labelStyle)
            val valLayout = textMeasurer.measure("${attr.totalValue}", style = valueStyle)

            drawText(
                textLayoutResult = codeLayout,
                topLeft = Offset(
                    lx - codeLayout.size.width / 2f,
                    ly - codeLayout.size.height
                )
            )
            drawText(
                textLayoutResult = valLayout,
                topLeft = Offset(
                    lx - valLayout.size.width / 2f,
                    ly + 1.dp.toPx()
                )
            )
        }

        // Player attribute polygon
        val statPath = Path()
        val vertexOffsets = mutableListOf<Offset>()
        for (i in 0 until count) {
            val attr = attributes[i]
            val ratio = (attr.totalValue.toFloat() / scaleMax).coerceIn(0.15f, 1f)
            val r = radius * ratio
            val angle = startAngle + i * angleStep
            val px = center.x + r * cos(angle)
            val py = center.y + r * sin(angle)
            val point = Offset(px, py)
            vertexOffsets.add(point)
            if (i == 0) statPath.moveTo(px, py) else statPath.lineTo(px, py)
        }
        statPath.close()

        drawPath(
            path = statPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    SystemColors.ElectricCyan.copy(alpha = 0.42f),
                    SystemColors.ArcaneViolet.copy(alpha = 0.25f)
                ),
                center = center,
                radius = radius
            )
        )
        drawPath(
            path = statPath,
            brush = Brush.linearGradient(
                colors = listOf(SystemColors.ElectricCyan, SystemColors.ArcaneViolet)
            ),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        vertexOffsets.forEach { pt ->
            drawCircle(
                color = SystemColors.ElectricCyan,
                radius = 4.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
fun SystemLoadingView(
    message: String = "INITIALIZING REALITY OS...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = SystemColors.ElectricCyan,
                trackColor = SystemColors.PanelSurface,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge,
                color = SystemColors.ElectricCyan
            )
        }
    }
}

@Composable
fun SystemEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    HolographicPanel(
        modifier = modifier.fillMaxWidth(),
        accentColor = SystemColors.MutedSilver
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = title,
                tint = SystemColors.MutedSilver,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = SystemColors.PureIce,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = SystemColors.MutedSilver,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SystemErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    HolographicPanel(
        modifier = modifier.fillMaxWidth(),
        accentColor = SystemColors.DangerCrimson
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = "System error",
                tint = SystemColors.DangerCrimson,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SystemColors.PureIce,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.DangerCrimson,
                    contentColor = SystemColors.PureIce
                )
            ) {
                Text("RETRY SYNC", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = SystemColors.ElectricCyan,
    contentColor: Color = SystemColors.ObsidianBlack
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.3f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    borderColor: Color = SystemColors.ElectricCyan,
    contentColor: Color = SystemColors.ElectricCyan
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (enabled) borderColor.copy(alpha = 0.65f) else borderColor.copy(alpha = 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) contentColor else contentColor.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.4f),
            maxLines = 1
        )
    }
}

@Composable
fun AttributeMiniCard(
    attribute: AttributeBreakdown,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = SystemColors.attributeAccent(attribute.type)
    Surface(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = SystemColors.SecondarySurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = attribute.type.code,
                        style = MaterialTheme.typography.titleMedium,
                        color = accent
                    )
                }
                Text(
                    text = "${attribute.totalValue}",
                    style = MaterialTheme.typography.titleLarge,
                    color = SystemColors.PureIce
                )
            }
            Text(
                text = attribute.type.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = SystemColors.MutedSilver,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            GlowingProgressBar(
                progress = attribute.progressFraction,
                height = 4.dp,
                startColor = accent,
                endColor = SystemColors.ElectricCyan
            )
        }
    }
}

@Composable
fun AttributeOverviewGrid(
    attributes: List<AttributeBreakdown>,
    onInspectStatus: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val chunked = attributes.chunked(2)
        chunked.forEach { rowAttrs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowAttrs.forEach { attr ->
                    AttributeMiniCard(
                        attribute = attr,
                        onClick = onInspectStatus,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowAttrs.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun StatusStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = SystemColors.ElectricCyan
) {
    Surface(
        modifier = modifier,
        color = SystemColors.SecondarySurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SystemColors.SubtleBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SystemColors.MutedSilver,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SystemPageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    tag: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (tag != null) {
                Text(
                    text = tag.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.ElectricCyan
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.headlineLarge,
                color = SystemColors.PureIce
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MutedSilver
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    SystemSectionTitle(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        trailing = trailing
    )
}

@Composable
fun ProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    startColor: Color = SystemColors.ElectricCyan,
    endColor: Color = SystemColors.ArcaneViolet,
    trackColor: Color = Color(0xFF1A2642)
) {
    GlowingProgressBar(
        progress = progress,
        modifier = modifier,
        height = height,
        startColor = startColor,
        endColor = endColor,
        trackColor = trackColor
    )
}

@Composable
fun StatusBadge(
    rank: HunterRank,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    RankBadge(
        rank = rank,
        modifier = modifier,
        compact = compact
    )
}

@Composable
fun <T> TacticalSelector(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    activeColor: Color = SystemColors.ElectricCyan,
    inactiveColor: Color = SystemColors.MutedSilver
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val label = labelProvider(option)
            Surface(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOptionSelected(option) },
                color = if (isSelected) activeColor.copy(alpha = 0.16f) else SystemColors.SecondarySurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.dp else 0.75.dp,
                    color = if (isSelected) activeColor.copy(alpha = 0.8f) else SystemColors.SubtleBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(activeColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) SystemColors.PureIce else inactiveColor
                    )
                }
            }
        }
    }
}
