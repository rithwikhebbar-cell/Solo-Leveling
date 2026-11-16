package com.example.domain.engine

import com.example.domain.model.AcademicAssessment
import com.example.domain.model.AcademicChapter
import com.example.domain.model.AcademicExam
import com.example.domain.model.AcademicQuest
import com.example.domain.model.AcademicSubject
import com.example.domain.model.AcademicTopic
import com.example.domain.model.DisciplineAssessment
import com.example.domain.model.DungeonCompletionRecord
import com.example.domain.model.DungeonDefinition
import com.example.domain.model.DungeonInstance
import com.example.domain.model.GoalWithDetails
import com.example.domain.model.Habit
import com.example.domain.model.HabitCompletionRecord
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.JoggingSession
import com.example.domain.model.LifeRoutine
import com.example.domain.model.PersonalTask
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.PlayerIntelligenceProfile
import com.example.domain.model.PlayerProfile
import com.example.domain.model.Quest
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RevisionRecord
import com.example.domain.model.RoutineCompletionRecord
import com.example.domain.model.ScheduledActivityItem
import com.example.domain.model.SkillUnlockRecord
import com.example.domain.model.StudySession
import com.example.domain.model.WorkoutSession
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Complete read-only snapshot of cross-domain Milestone 0-7 telemetry consumed by Milestone 8 engines.
 */
data class IntelligenceTelemetryInput(
    val profile: PlayerProfile,
    val recovery: RecoveryAssessment,
    val joggingSessions: List<JoggingSession> = emptyList(),
    val workoutSessions: List<WorkoutSession> = emptyList(),
    val subjects: List<AcademicSubject> = emptyList(),
    val chapters: List<AcademicChapter> = emptyList(),
    val topics: List<AcademicTopic> = emptyList(),
    val studySessions: List<StudySession> = emptyList(),
    val revisionRecords: List<RevisionRecord> = emptyList(),
    val assessments: List<AcademicAssessment> = emptyList(),
    val exams: List<AcademicExam> = emptyList(),
    val generalQuests: List<Quest> = emptyList(),
    val academicQuests: List<AcademicQuest> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitCompletions: List<HabitCompletionRecord> = emptyList(),
    val routines: List<LifeRoutine> = emptyList(),
    val routineCompletions: List<RoutineCompletionRecord> = emptyList(),
    val tasks: List<PersonalTask> = emptyList(),
    val goalsWithDetails: List<GoalWithDetails> = emptyList(),
    val disciplineAssessments: List<DisciplineAssessment> = emptyList(),
    val scheduledItemsToday: List<ScheduledActivityItem> = emptyList(),
    val dungeonDefinitions: List<DungeonDefinition> = emptyList(),
    val dungeonInstances: List<DungeonInstance> = emptyList(),
    val dungeonCompletions: List<DungeonCompletionRecord> = emptyList(),
    val unlockedSkills: List<SkillUnlockRecord> = emptyList(),
    val unlockedAchievementsCount: Int = 0,
    val dailyAcademicGoalMinutes: Int = 60,
    val dailyJoggingTargetMinutes: Int = 30,
    val nowEpochMs: Long = System.currentTimeMillis(),
    val todayEpochDay: Long = LocalDate.now().toEpochDay(),
    val zoneId: ZoneId = ZoneId.systemDefault()
)

/**
 * Core deterministic analytics & incremental fingerprinting engine.
 * Computes unified player intelligence without fabricating missing data.
 */
object PlayerIntelligenceEngine {

    private const val DAY_MS = 24 * 60 * 60_000L

    fun computeSourceFingerprint(input: IntelligenceTelemetryInput): String {
        val raw = buildString {
            append(input.todayEpochDay).append('|')
            append(input.profile.level).append('|')
            append(input.profile.totalXp).append('|')
            append(input.profile.rank.name).append('|')
            append(input.profile.verifiedActivitiesCount).append('|')
            append(input.profile.selfReportedActivitiesCount).append('|')
            append(input.recovery.state.name).append('|')
            append(input.recovery.readinessScore).append('|')
            append(input.joggingSessions.size).append(':').append(input.joggingSessions.maxOfOrNull { it.endEpochMs } ?: 0L).append('|')
            append(input.workoutSessions.size).append(':').append(input.workoutSessions.maxOfOrNull { it.endEpochMs } ?: 0L).append('|')
            append(input.studySessions.size).append(':').append(input.studySessions.maxOfOrNull { it.endEpochMs } ?: 0L).append('|')
            append(input.revisionRecords.size).append(':').append(input.revisionRecords.maxOfOrNull { it.revisedAtEpochMs } ?: 0L).append('|')
            append(input.assessments.size).append(':').append(input.assessments.maxOfOrNull { it.timestampEpochMs } ?: 0L).append('|')
            append(input.exams.size).append(':').append(input.exams.count { it.isCompleted }).append('|')
            append(input.habits.size).append(':').append(input.habitCompletions.size).append('|')
            append(input.routines.size).append(':').append(input.routineCompletions.size).append('|')
            append(input.tasks.size).append(':').append(input.tasks.count { it.status == PersonalTaskStatus.COMPLETED }).append(':').append(input.tasks.maxOfOrNull { it.updatedAtEpochMs } ?: 0L).append('|')
            append(input.goalsWithDetails.size).append(':').append(input.goalsWithDetails.sumOf { it.computedProgressPercent }).append('|')
            append(input.dungeonCompletions.size).append(':').append(input.dungeonInstances.count { it.status.name == "ACTIVE" }).append('|')
            append(input.unlockedSkills.size).append(':').append(input.unlockedAchievementsCount)
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }

    fun buildProfile(
        input: IntelligenceTelemetryInput,
        sharePhysical: Boolean = true,
        shareAcademic: Boolean = true,
        shareLife: Boolean = true,
        shareRpg: Boolean = true
    ): PlayerIntelligenceProfile {
        val thirtyDaysAgo = input.nowEpochMs - 30 * DAY_MS
        val sevenDaysAgo = input.nowEpochMs - 7 * DAY_MS
        val thirtyDaysAgoDay = input.todayEpochDay - 30L
        val notes = mutableListOf<String>()

        // 1. Physical Score (0-100)
        val recentJogs = if (sharePhysical) input.joggingSessions.filter { it.startEpochMs >= thirtyDaysAgo } else emptyList()
        val recentWorkouts = if (sharePhysical) input.workoutSessions.filter { it.startEpochMs >= thirtyDaysAgo } else emptyList()
        val physicalScore = if (!sharePhysical) {
            0
        } else {
            val mins7d = recentJogs.filter { it.startEpochMs >= sevenDaysAgo }.sumOf { it.durationMinutes } +
                recentWorkouts.filter { it.startEpochMs >= sevenDaysAgo }.sumOf { it.durationMinutes }
            val volumeComponent = ((mins7d.toFloat() / 150f) * 60f).toInt().coerceIn(0, 60)
            val recoveryComponent = (input.recovery.readinessScore * 0.4f).toInt().coerceIn(0, 40)
            if (recentJogs.isEmpty() && recentWorkouts.isEmpty()) {
                notes.add("No jogging or structured workout sessions recorded in the last 30 days.")
                (recoveryComponent / 2).coerceIn(0, 30)
            } else {
                (volumeComponent + recoveryComponent).coerceIn(0, 100)
            }
        }

        // 2. Academic Score (0-100)
        val recentStudy = if (shareAcademic) input.studySessions.filter { it.startEpochMs >= thirtyDaysAgo } else emptyList()
        val recentRevs = if (shareAcademic) input.revisionRecords.filter { it.revisedAtEpochMs >= thirtyDaysAgo } else emptyList()
        val academicScore = if (!shareAcademic) {
            0
        } else {
            val weeklyGoal = (input.dailyAcademicGoalMinutes * 5).coerceAtLeast(150)
            val mins7d = recentStudy.filter { it.startEpochMs >= sevenDaysAgo }.sumOf { it.durationMinutes }
            val studyComp = ((mins7d.toFloat() / weeklyGoal.toFloat()) * 55f).toInt().coerceIn(0, 55)
            val revComp = (recentRevs.filter { it.revisedAtEpochMs >= sevenDaysAgo }.size * 8).coerceIn(0, 25)
            val assessComp = input.assessments.take(5).takeIf { it.isNotEmpty() }
                ?.map { it.normalizedRatio * 20f }?.average()?.toInt()?.coerceIn(0, 20) ?: 0
            if (recentStudy.isEmpty() && input.assessments.isEmpty()) {
                notes.add("No academic study sessions or assessments recorded in the last 30 days.")
            }
            (studyComp + revComp + assessComp).coerceIn(0, 100)
        }

        // 3. Discipline & Life Score (0-100)
        val recentDiscipline = if (shareLife) {
            input.disciplineAssessments.filter { it.dateEpochDay >= thirtyDaysAgoDay }
        } else {
            emptyList()
        }
        val disciplineScore = if (!shareLife) {
            0
        } else if (recentDiscipline.isNotEmpty()) {
            recentDiscipline.map { it.disciplineScore }.average().toInt().coerceIn(0, 100)
        } else {
            val doneHabits = input.habitCompletions.count { it.dateEpochDay >= thirtyDaysAgoDay && it.status == HabitDayStatus.COMPLETED }
            val doneTasks = input.tasks.count { it.status == PersonalTaskStatus.COMPLETED }
            if (doneHabits == 0 && doneTasks == 0) {
                notes.add("No habit or personal task completions recorded yet.")
                0
            } else {
                ((doneHabits * 8) + (doneTasks * 10)).coerceIn(10, 85)
            }
        }

        // 4. RPG Engagement Score (0-100)
        val rpgScore = if (!shareRpg) {
            0
        } else {
            val clears = input.dungeonCompletions.size
            val skills = input.unlockedSkills.size
            val ach = input.unlockedAchievementsCount
            if (clears == 0 && skills == 0 && ach == 0) {
                notes.add("No dungeon clears or skill unlocks recorded yet.")
                0
            } else {
                (clears * 18 + skills * 10 + ach * 8).coerceIn(0, 100)
            }
        }

        // 5. Behavioral Patterns (peak window + average session duration)
        val sessionTimestamps = buildList {
            recentJogs.forEach { add(it.startEpochMs to it.durationMinutes) }
            recentWorkouts.forEach { add(it.startEpochMs to it.durationMinutes) }
            recentStudy.forEach { add(it.startEpochMs to it.durationMinutes) }
        }
        val peakWindow = if (sessionTimestamps.size < 3) {
            "Insufficient timestamp history (<3 sessions)"
        } else {
            val bucketCounts = IntArray(4) // 0=Morning(5-11), 1=Afternoon(12-16), 2=Evening(17-20), 3=Night(21-4)
            sessionTimestamps.forEach { (ts, _) ->
                val hour = Instant.ofEpochMilli(ts).atZone(input.zoneId).hour
                val idx = when (hour) {
                    in 5..11 -> 0
                    in 12..16 -> 1
                    in 17..20 -> 2
                    else -> 3
                }
                bucketCounts[idx]++
            }
            when (bucketCounts.indices.maxByOrNull { bucketCounts[it] } ?: 0) {
                0 -> "Morning Peak (05:00–11:59)"
                1 -> "Afternoon Peak (12:00–16:59)"
                2 -> "Evening Peak (17:00–20:59)"
                else -> "Late Night Peak (21:00–04:59)"
            }
        }
        val avgSessionMinutes = if (sessionTimestamps.isEmpty()) 0 else {
            sessionTimestamps.map { it.second }.average().toInt().coerceAtLeast(1)
        }

        // 6. Provenance Ratio & Active Days
        val totalLogs = input.profile.verifiedActivitiesCount + input.profile.selfReportedActivitiesCount
        val measuredRatio = if (totalLogs == 0) 0 else {
            ((input.profile.verifiedActivitiesCount * 100) / totalLogs).coerceIn(0, 100)
        }
        val activeEpochDays = buildSet {
            recentJogs.forEach { add(Instant.ofEpochMilli(it.startEpochMs).atZone(input.zoneId).toLocalDate().toEpochDay()) }
            recentWorkouts.forEach { add(Instant.ofEpochMilli(it.startEpochMs).atZone(input.zoneId).toLocalDate().toEpochDay()) }
            recentStudy.forEach { add(Instant.ofEpochMilli(it.startEpochMs).atZone(input.zoneId).toLocalDate().toEpochDay()) }
            input.habitCompletions.filter { it.dateEpochDay >= thirtyDaysAgoDay && it.status == HabitDayStatus.COMPLETED }.forEach { add(it.dateEpochDay) }
            input.routineCompletions.filter { it.dateEpochDay >= thirtyDaysAgoDay }.forEach { add(it.dateEpochDay) }
        }

        val domainsWithData = listOf(
            recentJogs.isNotEmpty() || recentWorkouts.isNotEmpty(),
            recentStudy.isNotEmpty() || recentRevs.isNotEmpty() || input.assessments.isNotEmpty(),
            input.habitCompletions.isNotEmpty() || input.tasks.isNotEmpty() || input.routines.isNotEmpty(),
            input.dungeonCompletions.isNotEmpty() || input.unlockedSkills.isNotEmpty()
        ).count { it }
        val completenessPct = ((domainsWithData * 25) + (if (input.recovery.measuredSleepHoursLastNight != null) 0 else 0)).coerceIn(0, 100)

        return PlayerIntelligenceProfile(
            sourceFingerprint = computeSourceFingerprint(input),
            evaluatedAtEpochMs = input.nowEpochMs,
            playerLevel = input.profile.level,
            hunterRank = input.profile.rank,
            physicalActivityScore = physicalScore,
            academicActivityScore = academicScore,
            disciplineConsistencyScore = disciplineScore,
            rpgEngagementScore = rpgScore,
            behavioralPeakWindow = peakWindow,
            averageSessionMinutes = avgSessionMinutes,
            measuredDataRatioPercent = measuredRatio,
            activeDaysLast30 = activeEpochDays.size,
            hasMinimumBaseline = activeEpochDays.size >= 3 || sessionTimestamps.size >= 3,
            dataCoverageNotes = notes.distinct(),
            dataCompletenessPercent = completenessPct
        )
    }
}
