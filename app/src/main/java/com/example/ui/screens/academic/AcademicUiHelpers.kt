package com.example.ui.screens.academic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.domain.model.SubjectColorToken
import com.example.domain.model.SubjectIconKey
import com.example.ui.theme.SystemColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun SubjectColorToken.toHudColor(): Color = when (this) {
    SubjectColorToken.CYAN -> SystemColors.ElectricCyan
    SubjectColorToken.VIOLET -> SystemColors.ArcaneViolet
    SubjectColorToken.EMERALD -> SystemColors.VerifiedEmerald
    SubjectColorToken.AMBER -> SystemColors.WarningAmber
    SubjectColorToken.BLUE -> SystemColors.SystemBlue
    SubjectColorToken.CRIMSON -> SystemColors.DangerCrimson
    SubjectColorToken.GOLD -> SystemColors.MonarchGold
    SubjectColorToken.SILVER -> SystemColors.MetallicSilver
}

fun SubjectIconKey.toVector(): ImageVector = when (this) {
    SubjectIconKey.BOOK -> Icons.Rounded.AutoStories
    SubjectIconKey.CODE -> Icons.Rounded.Code
    SubjectIconKey.SCIENCE -> Icons.Rounded.Science
    SubjectIconKey.MATH -> Icons.Rounded.Calculate
    SubjectIconKey.LAW -> Icons.Rounded.Gavel
    SubjectIconKey.MEDICAL -> Icons.Rounded.LocalHospital
    SubjectIconKey.LANGUAGE -> Icons.Rounded.Translate
    SubjectIconKey.HISTORY -> Icons.Rounded.HistoryEdu
}

@Composable
fun AcademicMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = SystemColors.PureIce
) {
    Surface(
        modifier = modifier,
        color = SystemColors.ObsidianBlack.copy(alpha = 0.65f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.75.dp, SystemColors.SubtleGridLine)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = accent)
        }
    }
}

@Composable
fun academicFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SystemColors.ElectricCyan,
    unfocusedBorderColor = SystemColors.PanelBorderCyan,
    focusedTextColor = SystemColors.PureIce,
    unfocusedTextColor = SystemColors.PureIce
)

fun formatAcademicDateTime(epochMs: Long): String =
    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(epochMs))

fun formatAcademicDate(epochMs: Long): String =
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(epochMs))
