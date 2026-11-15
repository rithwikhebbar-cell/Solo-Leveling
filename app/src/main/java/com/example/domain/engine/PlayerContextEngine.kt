package com.example.domain.engine

import com.example.domain.model.AcademicAssessment
import com.example.domain.model.AcademicExam
import com.example.domain.model.AcademicQuest
import com.example.domain.model.AcademicQuestStatus
import com.example.domain.model.AcademicSubject
import com.example.domain.model.AcademicTopic
import com.example.domain.model.AiConsentState
import com.example.domain.model.AiContextScope
import com.example.domain.model.JoggingSession
import com.example.domain.model.PhysicalContextSummary
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerContextSnapshot
import com.example.domain.model.PlayerProfile
import com.example.domain.model.PreferencesContextSummary
import com.example.domain.model.ProfileContextSummary
import com.example.domain.model.Quest
import com.example.domain.model.QuestContextSummary
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RevisionUrgency
import com.example.domain.model.ScheduledRevisionItem
import com.example.domain.model.StudySession
import com.example.domain.model.SubjectProgressSummary
import com.example.domain.model.WorkoutSession
import com.example.domain.model.AcademicContextSummary
import com.example.domain.model.LifeContextSummary
import com.example.domain.model.RpgContextSummary

/**
 * Sanitizes untrusted player-entered or externally imported text before including it in prompts.
 * Prevents prompt-injection attempts by stripping system-style control delimiters and capping length.
 */
object PromptSanitizer {
    private val controlPattern = Regex("(?i)(</?system>|</?assistant>|ignore previous instructions|you are now|override xp|set rank)")

    fun sanitize(raw: String, maxChars: Int = 140): String {
        val collapsed = raw
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace(controlPattern, "[filtered]")
            .trim()
        return if (collapsed.length <= maxChars) collapsed else collapsed.take(maxChars - 3) + "..."
    }
}

/**
 * Aggregates minimized, provenance-tagged player telemetry across Physical, Academic, Quests,
 * and Preferences. Respects AiConsentState and acknowledges missing data explicitly without fabrication.
 */
object PlayerContextEngine {

    fun inferScope(prompt: String): AiContextScope {
        val lower = prompt.lowercase()
        val mentionsPhysical = listOf("workout", "jog", "run", "recovery", "fatigue", "rest", "str", "vit", "agi")
            .any { it in lower }
        val mentionsAcademic = listOf("study", "subject", "exam", "revision", "chapter", "topic", "int", "wis", "syllabus")
            .any { it in lower }
        val mentionsPlan = listOf("schedule", "plan", "tomorrow", "week", "daily")
            .any { it in lower }
        val mentionsQuest = listOf("quest", "mission", "task", "routine", "habit", "goal", "discipline").any { it in lower }

        return when {
            mentionsPlan -> AiContextScope.PLANNING
            mentionsPhysical && !mentionsAcademic -> AiContextScope.PHYSICAL
            mentionsAcademic && !mentionsPhysical -> AiContextScope.ACADEMIC
            mentionsQuest && !mentionsPhysical && !mentionsAcademic -> AiContextScope.QUESTS
            else -> AiContextScope.FULL
        }
    }

    fun buildContextSnapshot(
        scope: AiContextScope,
        consent: AiConsentState,
        profile: PlayerProfile,
        physicalState: PhysicalDevelopmentState,
        recoveryAssessment: RecoveryAssessment,
        recentJogs: List<JoggingSession>,
        recentWorkouts: List<WorkoutSession>,
        subjectSummaries: List<SubjectProgressSummary>,
        revisionQueue: List<ScheduledRevisionItem>,
        studySessions: List<StudySession>,
        exams: List<AcademicExam>,
        assessments: List<AcademicAssessment>,
        generalQuests: List<Quest>,
        academicQuests: List<AcademicQuest>,
        dailyResetHour: Int,
        dailyAcademicGoalMin: Int,
        weeklyAcademicGoalMin: Int,
        selectedAiModelId: String,
        nowEpochMs: Long = System.currentTimeMillis(),
        lifeSummaryInput: LifeContextSummary? = null,
        rpgSummaryInput: RpgContextSummary? = null
    ): PlayerContextSnapshot {
        val missingNotes = mutableListOf<String>()
        val nextRankProgress = ProgressionEngine.evaluateNextRankProgress(profile)
        val nextRank = nextRankProgress.nextRank
        val nextThreshold = nextRank?.threshold

        val profileSummary = ProfileContextSummary(
            codename = PromptSanitizer.sanitize(profile.codename, 32),
            level = profile.level,
            rankCode = profile.rank.code,
            rankName = profile.rank.displayName,
            currentLevelXp = profile.currentLevelXp,
            xpToNextLevel = profile.xpToNextLevel,
            totalXp = profile.totalXp,
            verifiedActivitiesCount = profile.verifiedActivitiesCount,
            selfReportedActivitiesCount = profile.selfReportedActivitiesCount,
            streakDays = profile.streakDays,
            attributeValues = profile.attributes.asList().associate { it.type.code to it.totalValue },
            attributeProvenanceNote = "Attributes immutable; combined from verified and self-reported activities.",
            nextRankName = nextRank?.displayName,
            nextRankMissingLevel = ((nextThreshold?.minLevel ?: profile.level) - profile.level).coerceAtLeast(0),
            nextRankMissingAttributes = ((nextThreshold?.minTotalAttributes ?: profile.attributes.totalScore) - profile.attributes.totalScore).coerceAtLeast(0),
            nextRankMissingVerified = ((nextThreshold?.minVerifiedActivities ?: profile.verifiedActivitiesCount) - profile.verifiedActivitiesCount).coerceAtLeast(0)
        )

        val includePhysical = (scope == AiContextScope.FULL || scope == AiContextScope.PHYSICAL || scope == AiContextScope.PLANNING) &&
            consent.sharePhysicalWithAi
        val includeAcademic = (scope == AiContextScope.FULL || scope == AiContextScope.ACADEMIC || scope == AiContextScope.PLANNING) &&
            consent.shareAcademicWithAi
        val includeQuests = (scope == AiContextScope.FULL || scope == AiContextScope.QUESTS || scope == AiContextScope.PLANNING) &&
            consent.shareQuestsWithAi

        val sevenDaysAgo = nowEpochMs - 7 * 24 * 60 * 60_000L

        val physicalSummary = if (includePhysical) {
            val weeklyJogMins = recentJogs.filter { it.startEpochMs >= sevenDaysAgo }.sumOf { it.durationMinutes }
            val weeklyWorkouts = recentWorkouts.count { it.startEpochMs >= sevenDaysAgo }
            val hasHc = recentJogs.any { it.verificationStatus.isVerified } || recentWorkouts.any { it.verificationStatus.isVerified }
            if (recentJogs.isEmpty() && recentWorkouts.isEmpty()) {
                missingNotes.add("No jogging or structured workout sessions recorded yet.")
            }
            if (recoveryAssessment.measuredSleepHoursLastNight == null) {
                missingNotes.add("Measured sleep hours unavailable (requires Health Connect sleep permission/data).")
            }
            PhysicalContextSummary(
                joggingTargetMinutes = physicalState.effectiveJoggingTarget,
                completedJoggingTodayMinutes = physicalState.completedJoggingMinutesToday,
                weeklyJoggingMinutes = weeklyJogMins,
                weeklyWorkoutCount = weeklyWorkouts,
                workoutStatus = physicalState.workoutStatus.label,
                recoveryState = recoveryAssessment.state,
                readinessScore = recoveryAssessment.readinessScore,
                measuredLoad48hMinutes = recoveryAssessment.measuredLoadMinutes48h,
                measuredLoad7dMinutes = recoveryAssessment.measuredLoadMinutes7d,
                sleepHoursMeasured = recoveryAssessment.measuredSleepHoursLastNight,
                restingHrMeasuredBpm = recoveryAssessment.measuredRestingHrBpm,
                selfReportedDiscomfort = recoveryAssessment.selfReportedDiscomfort,
                hasHealthConnectData = hasHc
            )
        } else null

        val academicSummary = if (includeAcademic) {
            val activeSummaries = subjectSummaries.filter { !it.subject.isArchived }
            val todayStart = nowEpochMs - (nowEpochMs % (24 * 60 * 60_000L))
            val todayMins = studySessions.filter { it.startEpochMs >= todayStart }.sumOf { it.durationMinutes }
            val weeklyMins = studySessions.filter { it.startEpochMs >= sevenDaysAgo }.sumOf { it.durationMinutes }
            val overdue = revisionQueue.count { it.urgency == RevisionUrgency.OVERDUE }
            val dueToday = revisionQueue.count { it.urgency == RevisionUrgency.DUE_TODAY }
            val weakest = revisionQueue.sortedBy { it.retentionEstimatePercent }.take(4).map {
                "${PromptSanitizer.sanitize(it.subjectName, 24)}:${PromptSanitizer.sanitize(it.topic.title, 32)} (~${it.retentionEstimatePercent}% est)"
            }
            val upcomingExamList = exams.filter { !it.isCompleted }.take(4).map { ex ->
                val days = ExamCountdownCalculator.daysRemaining(ex.examDateEpochMs, nowEpochMs)
                "${PromptSanitizer.sanitize(ex.subjectName, 20)} - ${PromptSanitizer.sanitize(ex.title, 28)} (${days}d)"
            }
            val recentAssess = assessments.take(4).map { a ->
                "${PromptSanitizer.sanitize(a.title, 28)}: ${(a.normalizedRatio * 100).toInt()}% [${a.verificationStatus.label}]"
            }

            if (activeSummaries.isEmpty()) missingNotes.add("No active academic subjects configured.")
            if (assessments.isEmpty()) missingNotes.add("No academic assessment scores recorded yet.")
            if (upcomingExamList.isEmpty()) missingNotes.add("No upcoming examinations scheduled.")

            AcademicContextSummary(
                activeSubjectsCount = activeSummaries.size,
                subjectNamesAndCompletion = activeSummaries.take(6).map {
                    "${PromptSanitizer.sanitize(it.subject.name, 28)} (${it.syllabusCompletionPercent}% syllabus, ${it.totalStudyMinutes}m)"
                },
                todayStudyMinutes = todayMins,
                dailyGoalMinutes = dailyAcademicGoalMin,
                weeklyStudyMinutes = weeklyMins,
                overdueRevisionsCount = overdue,
                dueTodayRevisionsCount = dueToday,
                weakestEstimatedTopics = weakest,
                upcomingExams = upcomingExamList,
                recentAssessmentSummaries = recentAssess
            )
        } else null

        val questSummary = if (includeQuests) {
            val activeGen = generalQuests.filter { !it.isCompleted }
            val activeAcad = academicQuests.filter { it.status == AcademicQuestStatus.ACTIVE }
            val nearDeadlines = activeAcad.filter { it.deadlineEpochMs != null && it.deadlineEpochMs - nowEpochMs <= 48 * 3600_000L }
                .map { PromptSanitizer.sanitize(it.title, 40) }
            QuestContextSummary(
                activeGeneralQuestsCount = activeGen.size,
                completedGeneralQuestsCount = generalQuests.count { it.isCompleted },
                activeAcademicQuestsCount = activeAcad.size,
                completedAcademicQuestsCount = academicQuests.count { it.status == AcademicQuestStatus.COMPLETED },
                activeQuestTitles = (activeGen.map { PromptSanitizer.sanitize(it.title, 36) } +
                    activeAcad.map { PromptSanitizer.sanitize(it.title, 36) }).take(8),
                overdueOrNearDeadlineTitles = nearDeadlines
            )
        } else null

        val prefSummary = PreferencesContextSummary(
            dailyResetHour = dailyResetHour,
            dailyAcademicGoalMinutes = dailyAcademicGoalMin,
            weeklyAcademicGoalMinutes = weeklyAcademicGoalMin,
            selectedAiModelId = selectedAiModelId,
            cloudAiConsentGranted = consent.cloudAiConsentGranted
        )

        val lifeSummary = if (consent.shareLifeWithAi) lifeSummaryInput else null
        val rpgSummary = if (consent.shareRpgWithAi) rpgSummaryInput else null

        return PlayerContextSnapshot(
            generatedAtEpochMs = nowEpochMs,
            scope = scope,
            profileSummary = profileSummary,
            physicalSummary = physicalSummary,
            academicSummary = academicSummary,
            questSummary = questSummary,
            lifeSummary = lifeSummary,
            rpgSummary = rpgSummary,
            preferencesSummary = prefSummary,
            dataCompletenessNotes = missingNotes.distinct()
        )
    }

    /**
     * Produces a compact JSON-free structured brief to stay well inside token limits.
     */
    fun formatCompactPromptContext(snapshot: PlayerContextSnapshot): String = buildString {
        val p = snapshot.profileSummary
        appendLine("PLAYER: Lv.${p.level} ${p.rankName} | XP ${p.currentLevelXp}/${p.xpToNextLevel} (Total ${p.totalXp}) | Verified=${p.verifiedActivitiesCount}, Self=${p.selfReportedActivitiesCount}, Streak=${p.streakDays}d")
        appendLine("ATTRIBUTES (read-only): ${p.attributeValues.entries.joinToString { "${it.key}=${it.value}" }}")
        p.nextRankName?.let { next ->
            appendLine("NEXT RANK ($next) GAPS: +${p.nextRankMissingLevel} Lv, +${p.nextRankMissingAttributes} Attr, +${p.nextRankMissingVerified} Verified")
        }
        snapshot.physicalSummary?.let { phys ->
            appendLine("PHYSICAL: Jog today ${phys.completedJoggingTodayMinutes}/${phys.joggingTargetMinutes}m | 7dJog=${phys.weeklyJoggingMinutes}m, 7dWorkouts=${phys.weeklyWorkoutCount} | Recovery=${phys.recoveryState.code} (Score=${phys.readinessScore}/100, Discomfort=${phys.selfReportedDiscomfort}, Sleep=${phys.sleepHoursMeasured ?: "N/A"})")
        }
        snapshot.academicSummary?.let { acad ->
            appendLine("ACADEMIC: Subjects=${acad.activeSubjectsCount}, Study today ${acad.todayStudyMinutes}/${acad.dailyGoalMinutes}m, 7dStudy=${acad.weeklyStudyMinutes}m | OverdueRev=${acad.overdueRevisionsCount}, DueToday=${acad.dueTodayRevisionsCount}")
            if (acad.subjectNamesAndCompletion.isNotEmpty()) {
                appendLine("  Subjects: ${acad.subjectNamesAndCompletion.joinToString("; ")}")
            }
            if (acad.weakestEstimatedTopics.isNotEmpty()) {
                appendLine("  Lowest est. familiarity: ${acad.weakestEstimatedTopics.joinToString("; ")}")
            }
            if (acad.upcomingExams.isNotEmpty()) {
                appendLine("  Exams: ${acad.upcomingExams.joinToString("; ")}")
            }
        }
        snapshot.lifeSummary?.let { life ->
            appendLine("LIFE & DISCIPLINE: Score=${life.disciplineScore}/100 | Routines=${life.completedRoutinesToday}/${life.activeRoutinesCount}, Habits=${life.completedHabitsToday}/${life.activeHabitsCount} (BestStreak=${life.bestHabitStreakDays}d), Tasks=${life.pendingTasksCount} (${life.overdueTasksCount} overdue), Goals=${life.activeGoalsCount} (${life.behindScheduleGoalsCount} behind), ScheduleConflicts=${life.scheduleConflictsToday}")
        }
        snapshot.rpgSummary?.let { rpg ->
            appendLine("RPG SYSTEMS: Title='${rpg.equippedTitle}', ActiveDungeons=${rpg.activeDungeonsCount}, ClearedDungeons=${rpg.clearedDungeonsCount}, Skills=${rpg.unlockedSkillsCount}/${rpg.totalSkillsCount}, Achievements=${rpg.unlockedAchievementsCount}, Shadows=${rpg.shadowCollectionCount}" +
                (rpg.activeBossTitle?.let { " | Boss='$it' (${rpg.activeBossHpPercent ?: 100}% HP)" } ?: ""))
        }
        snapshot.questSummary?.let { q ->
            appendLine("QUESTS: Active=${q.activeGeneralQuestsCount + q.activeAcademicQuestsCount}, Completed=${q.completedGeneralQuestsCount + q.completedAcademicQuestsCount}")
            if (q.activeQuestTitles.isNotEmpty()) {
                appendLine("  Active titles: ${q.activeQuestTitles.joinToString(", ")}")
            }
        }
        if (snapshot.dataCompletenessNotes.isNotEmpty()) {
            appendLine("MISSING DATA NOTES: ${snapshot.dataCompletenessNotes.joinToString(" | ")}")
        }
    }
}
