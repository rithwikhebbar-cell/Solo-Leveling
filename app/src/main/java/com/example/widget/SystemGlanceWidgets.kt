package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.local.SoloLevelingDatabase
import com.example.domain.model.WidgetSnapshot
import com.example.worker.SystemNotificationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object WidgetColors {
    val ObsidianBg = ColorProvider(Color(0xFF070A13))
    val PanelSurface = ColorProvider(Color(0xFF0D1424))
    val CyanAccent = ColorProvider(Color(0xFF00E5FF))
    val VioletAccent = ColorProvider(Color(0xFF8B5CF6))
    val EmeraldAccent = ColorProvider(Color(0xFF10B981))
    val GoldAccent = ColorProvider(Color(0xFFFBBF24))
    val PureIce = ColorProvider(Color(0xFFEFF6FF))
    val MutedSilver = ColorProvider(Color(0xFF94A3B8))
}

private fun deepLinkIntent(context: Context, routeKey: String): Intent {
    return Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(SystemNotificationWorker.EXTRA_DEEP_LINK_ROUTE, routeKey)
    }
}

private suspend fun resolveLatestSnapshot(context: Context): WidgetSnapshot {
    val cached = WidgetSnapshotStore.loadSnapshot(context)
    if (cached.updatedAtEpochMs > 0L) return cached
    return withContext(Dispatchers.IO) {
        runCatching {
            val db = SoloLevelingDatabase.getInstance(context)
            WidgetUpdateCoordinator.buildAndStoreSnapshot(context, db)
        }.getOrElse { cached }
    }
}

/**
 * Widget 1: Player Status Widget (Rank, Level, XP bar text, Equipped Title, Recovery status).
 */
class PlayerStatusWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = resolveLatestSnapshot(context)
        provideContent {
            PlayerStatusWidgetContent(context = context, snapshot = snapshot)
        }
    }
}

@Composable
private fun PlayerStatusWidgetContent(context: Context, snapshot: WidgetSnapshot) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.ObsidianBg)
            .cornerRadius(14.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(deepLinkIntent(context, "status"))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.Start,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "SYSTEM // ${snapshot.rankDisplayName.uppercase()}",
                style = TextStyle(color = WidgetColors.CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "LV.${snapshot.level}",
                style = TextStyle(color = WidgetColors.GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "${snapshot.playerCodename} • ${snapshot.equippedTitle}",
            style = TextStyle(color = WidgetColors.PureIce, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "XP ${snapshot.currentLevelXp}/${snapshot.xpToNextLevel} • Total ${snapshot.totalXp}",
            style = TextStyle(color = WidgetColors.MutedSilver, fontSize = 11.sp)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "Recovery: ${snapshot.recoveryLabel}",
            style = TextStyle(color = WidgetColors.EmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        )
    }
}

class PlayerStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlayerStatusWidget()
}

/**
 * Widget 2: Daily Quest Widget (Pending quests count and top daily targets).
 */
class DailyQuestWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = resolveLatestSnapshot(context)
        provideContent {
            DailyQuestWidgetContent(context = context, snapshot = snapshot)
        }
    }
}

@Composable
private fun DailyQuestWidgetContent(context: Context, snapshot: WidgetSnapshot) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.ObsidianBg)
            .cornerRadius(14.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(deepLinkIntent(context, "quest_board")))
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "DAILY QUESTS",
                style = TextStyle(color = WidgetColors.CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "${snapshot.completedDailyQuests}/${snapshot.totalDailyQuests} CLEARED",
                style = TextStyle(color = WidgetColors.EmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        if (snapshot.pendingDailyQuests.isEmpty()) {
            Text(
                text = "All daily objectives completed or open Quest Board.",
                style = TextStyle(color = WidgetColors.PureIce, fontSize = 12.sp)
            )
        } else {
            snapshot.pendingDailyQuests.take(3).forEach { quest ->
                Text(
                    text = "• ${quest.title} (${quest.targetLabel})",
                    style = TextStyle(color = WidgetColors.PureIce, fontSize = 12.sp),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
        }
    }
}

class DailyQuestWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyQuestWidget()
}

/**
 * Widget 3: Daily Progress Widget (Jogging, Academic Study, Quests, Discipline Score).
 */
class DailyProgressWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = resolveLatestSnapshot(context)
        provideContent {
            DailyProgressWidgetContent(context = context, snapshot = snapshot)
        }
    }
}

@Composable
private fun DailyProgressWidgetContent(context: Context, snapshot: WidgetSnapshot) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.ObsidianBg)
            .cornerRadius(14.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(deepLinkIntent(context, "home")))
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "TODAY'S TELEMETRY",
                style = TextStyle(color = WidgetColors.VioletAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "DISC ${snapshot.disciplineScore}/100",
                style = TextStyle(color = WidgetColors.CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = "Jogging: ${snapshot.todayJoggingMinutes}/${snapshot.joggingTargetMinutes} min",
            style = TextStyle(color = WidgetColors.PureIce, fontSize = 12.sp)
        )
        Text(
            text = "Study Focus: ${snapshot.todayStudyMinutes}/${snapshot.academicGoalMinutes} min",
            style = TextStyle(color = WidgetColors.PureIce, fontSize = 12.sp)
        )
        Text(
            text = "Daily Quests: ${snapshot.completedDailyQuests}/${snapshot.totalDailyQuests} completed",
            style = TextStyle(color = WidgetColors.MutedSilver, fontSize = 11.sp)
        )
    }
}

class DailyProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyProgressWidget()
}

/**
 * Widget 4: Quick Action Widget (Direct deep links to Focus Timer, Workout, AI Companion, Status).
 */
class QuickActionWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            QuickActionWidgetContent(context = context)
        }
    }
}

@Composable
private fun QuickActionWidgetContent(context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.ObsidianBg)
            .cornerRadius(14.dp)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "SYSTEM QUICK COMMANDS",
            style = TextStyle(color = WidgetColors.CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            WidgetActionChip(
                label = "FOCUS",
                accent = WidgetColors.VioletAccent,
                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(deepLinkIntent(context, "academic_focus")))
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            WidgetActionChip(
                label = "TRAIN",
                accent = WidgetColors.EmeraldAccent,
                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(deepLinkIntent(context, "physical")))
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            WidgetActionChip(
                label = "SYSTEM AI",
                accent = WidgetColors.CyanAccent,
                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(deepLinkIntent(context, "ai_companion")))
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            WidgetActionChip(
                label = "STATUS",
                accent = WidgetColors.GoldAccent,
                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(deepLinkIntent(context, "status")))
            )
        }
    }
}

@Composable
private fun WidgetActionChip(
    label: String,
    accent: ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier
            .background(WidgetColors.PanelSurface)
            .cornerRadius(8.dp)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = TextStyle(color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

class QuickActionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickActionWidget()
}
