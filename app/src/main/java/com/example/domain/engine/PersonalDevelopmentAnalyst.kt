package com.example.domain.engine

import com.example.domain.model.DevelopmentAnalysisReport
import com.example.domain.model.PlayerContextSnapshot
import com.example.domain.model.RecoveryState

/**
 * Deterministic Personal Development Analyst & Offline Game Master Responder.
 * Strictly distinguishes measured data, self-reported data, and estimates.
 * Never fabricates missing data and never mutates XP, attributes, or rank.
 */
object PersonalDevelopmentAnalyst {

    fun analyze(snapshot: PlayerContextSnapshot): DevelopmentAnalysisReport {
        val p = snapshot.profileSummary
        val phys = snapshot.physicalSummary
        val acad = snapshot.academicSummary
        val quests = snapshot.questSummary

        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        val bottlenecks = mutableListOf<String>()
        val opportunities = mutableListOf<String>()
        val nextActions = mutableListOf<String>()

        val sortedAttrs = p.attributeValues.entries.sortedByDescending { it.value }
        val topAttr = sortedAttrs.firstOrNull()
        val lowAttr = sortedAttrs.lastOrNull()

        if (topAttr != null) {
            strengths.add("Leading attribute: ${topAttr.key} (${topAttr.value}).")
        }
        if (p.verifiedActivitiesCount > 0) {
            strengths.add("Verified activity baseline: ${p.verifiedActivitiesCount} verified session(s) on record.")
        } else {
            weaknesses.add("Zero verified activities recorded yet (only self-reported logs cannot satisfy higher rank thresholds).")
        }
        if (p.streakDays >= 3) {
            strengths.add("Training consistency streak at ${p.streakDays} days.")
        }

        if (lowAttr != null && topAttr != null && topAttr.value - lowAttr.value >= 3) {
            weaknesses.add("Attribute imbalance: ${lowAttr.key} (${lowAttr.value}) trails ${topAttr.key} (${topAttr.value}).")
        }

        // Rank bottlenecks
        p.nextRankName?.let { nextName ->
            val gaps = buildList {
                if (p.nextRankMissingLevel > 0) add("${p.nextRankMissingLevel} level(s)")
                if (p.nextRankMissingAttributes > 0) add("${p.nextRankMissingAttributes} attribute point(s)")
                if (p.nextRankMissingVerified > 0) add("${p.nextRankMissingVerified} verified session(s)")
            }
            if (gaps.isEmpty()) {
                opportunities.add("All criteria for $nextName are satisfied; complete any verified activity to trigger rank evaluation.")
            } else {
                bottlenecks.add("$nextName promotion requires: ${gaps.joinToString(", ")}.")
            }
        }

        // Physical evaluation
        if (phys != null) {
            when (phys.recoveryState) {
                RecoveryState.REST_RECOMMENDED -> {
                    weaknesses.add("High recovery strain detected (Readiness ${phys.readinessScore}/100${if (phys.selfReportedDiscomfort) ", discomfort reported" else ""}).")
                    nextActions.add("Prioritize rest or light mobility only today (safety rule: do not force intense training).")
                }
                RecoveryState.RECOVERY_RECOMMENDED -> {
                    weaknesses.add("Elevated 48h training load (${phys.measuredLoad48hMinutes}m); recovery priority active.")
                    nextActions.add("Keep movement to low-impact walking or mobility stretching today.")
                }
                RecoveryState.MODERATE -> {
                    opportunities.add("Moderate readiness (${phys.readinessScore}/100) supports controlled endurance or calisthenics.")
                }
                RecoveryState.READY -> {
                    if (phys.completedJoggingTodayMinutes >= phys.joggingTargetMinutes) {
                        strengths.add("Daily jogging target met (${phys.completedJoggingTodayMinutes}/${phys.joggingTargetMinutes} min).")
                    } else {
                        nextActions.add("Complete remaining ${phys.joggingTargetMinutes - phys.completedJoggingTodayMinutes} min toward today's jogging protocol.")
                    }
                }
            }
            if (!phys.hasHealthConnectData) {
                opportunities.add("Connect Health Connect to earn 1.0x verified progression instead of 0.65x self-reported credit.")
            }
        } else {
            weaknesses.add("Physical telemetry sharing is disabled or unavailable for this analysis scope.")
        }

        // Academic evaluation
        if (acad != null) {
            if (acad.activeSubjectsCount == 0) {
                weaknesses.add("No active academic subjects created yet; INT and WIS progression tracks are idle.")
                nextActions.add("Create at least 1 active subject and add syllabus chapters in Academic Engine.")
            } else {
                if (acad.overdueRevisionsCount > 0) {
                    weaknesses.add("${acad.overdueRevisionsCount} topic(s) overdue in Revision Centre (estimated familiarity decaying).")
                    nextActions.add("Clear overdue revisions in Revision Centre to reinforce WIS.")
                }
                if (acad.todayStudyMinutes >= acad.dailyGoalMinutes) {
                    strengths.add("Daily academic study goal reached (${acad.todayStudyMinutes}/${acad.dailyGoalMinutes} min).")
                } else {
                    nextActions.add("Run a Focus Timer block (${(acad.dailyGoalMinutes - acad.todayStudyMinutes).coerceAtLeast(25)}m remaining for today's study goal).")
                }
                if (acad.upcomingExams.isNotEmpty()) {
                    opportunities.add("Upcoming exam window active: ${acad.upcomingExams.first()}.")
                }
            }
        }

        // Quests evaluation
        if (quests != null) {
            if (quests.overdueOrNearDeadlineTitles.isNotEmpty()) {
                bottlenecks.add("Near-deadline quests: ${quests.overdueOrNearDeadlineTitles.joinToString(", ")}.")
            }
        }

        // Life & Discipline evaluation
        snapshot.lifeSummary?.let { life ->
            if (life.disciplineScore >= 75) {
                strengths.add("High daily discipline index (${life.disciplineScore}/100) led by ${life.strongestArea}.")
            } else if (life.activeHabitsCount + life.activeRoutinesCount + life.pendingTasksCount > 0) {
                opportunities.add("Discipline index is ${life.disciplineScore}/100; stabilize ${life.weakestArea} first.")
            }
            if (life.overdueTasksCount > 0) {
                bottlenecks.add("${life.overdueTasksCount} personal task(s) overdue; reschedule or simplify priorities.")
                nextActions.add("Triage overdue personal tasks in Life Management → Tasks.")
            }
            if (life.scheduleConflictsToday > 0) {
                bottlenecks.add("${life.scheduleConflictsToday} overlapping block(s) in Daily Schedule.")
                nextActions.add("Open Daily Schedule to move conflicting blocks into free slots.")
            }
            if (life.behindScheduleGoalsCount > 0) {
                weaknesses.add("${life.behindScheduleGoalsCount} active life goal(s) pacing behind target date.")
            }
        }

        // RPG Dungeons, Boss Battles & Skills evaluation
        snapshot.rpgSummary?.let { rpg ->
            if (rpg.clearedDungeonsCount > 0 || rpg.unlockedSkillsCount > 0) {
                strengths.add("RPG Progression active: ${rpg.clearedDungeonsCount} gate(s) cleared, ${rpg.unlockedSkillsCount}/${rpg.totalSkillsCount} skills unlocked.")
            }
            if (rpg.activeDungeonsCount == 0 && phys?.recoveryState != RecoveryState.REST_RECOMMENDED) {
                opportunities.add("No active Dungeon Gate currently running. Enter an E/D-Rank gate aligned with today's real training/study.")
            }
            rpg.activeBossTitle?.let { boss ->
                nextActions.add("Advance active Boss Battle '$boss' (${rpg.activeBossHpPercent ?: 100}% HP remaining) via verified session evidence.")
            }
        }

        val headline = "Lv.${p.level} ${p.rankName} • ${p.totalXp} XP • ${if (phys?.recoveryState == RecoveryState.REST_RECOMMENDED) "Recovery Priority" else "System Nominal"}"
        val summary = buildString {
            append("You have ${p.verifiedActivitiesCount} verified and ${p.selfReportedActivitiesCount} self-reported activities. ")
            if (p.nextRankName != null) {
                append("Next rank: ${p.nextRankName}. ")
            }
            if (snapshot.dataCompletenessNotes.isNotEmpty()) {
                append("Gaps noted: ${snapshot.dataCompletenessNotes.first()}")
            }
        }.trim()

        return DevelopmentAnalysisReport(
            headline = headline,
            overallSummary = summary,
            strengths = strengths.ifEmpty { listOf("Player profile initialized; begin logging real physical and academic sessions.") },
            weaknesses = weaknesses.ifEmpty { listOf("No critical weaknesses flagged from currently available records.") },
            bottlenecks = bottlenecks.ifEmpty { listOf("No immediate progression bottlenecks detected.") },
            improvementOpportunities = opportunities.ifEmpty { listOf("Maintain consistent daily quests and verified telemetry.") },
            recommendedNextActions = nextActions.ifEmpty { listOf("Complete today's pending Daily Quests and log verified evidence.") },
            provenanceDistinctionNote = "Analysis separates measured records (Health Connect / Timer / Assessments) from self-reported logs and estimated familiarity."
        )
    }

    fun answerNaturalPrompt(
        userMessage: String,
        snapshot: PlayerContextSnapshot,
        report: DevelopmentAnalysisReport
    ): String {
        val lower = userMessage.lowercase()
        val p = snapshot.profileSummary
        val phys = snapshot.physicalSummary
        val acad = snapshot.academicSummary

        return buildString {
            when {
                "rank" in lower || "level" in lower -> {
                    appendLine("RANK PROGRESSION DIAGNOSTIC (${p.rankName}, Lv.${p.level})")
                    if (p.nextRankName == null) {
                        appendLine("• You have achieved the highest rank.")
                    } else {
                        appendLine("• Next rank (${p.nextRankName}) gaps: ${p.nextRankMissingLevel} level(s), ${p.nextRankMissingAttributes} attribute point(s), ${p.nextRankMissingVerified} verified activity(ies).")
                        appendLine("• Reminder: Only verified activities (Health Connect, System Timer, or Assessment Verified) satisfy verified-session thresholds.")
                    }
                }
                "workout" in lower || "jog" in lower || "ready" in lower || "recovery" in lower -> {
                    if (phys == null) {
                        appendLine("PHYSICAL STATUS UNAVAILABLE: Physical telemetry consent is disabled or not loaded.")
                    } else {
                        appendLine("PHYSICAL READINESS REPORT (${phys.recoveryState.code}, ${phys.readinessScore}/100)")
                        appendLine("• 48h Load: ${phys.measuredLoad48hMinutes}m | 7d Jogging: ${phys.weeklyJoggingMinutes}m | Today: ${phys.completedJoggingTodayMinutes}/${phys.joggingTargetMinutes}m")
                        appendLine("• Sleep (measured): ${phys.sleepHoursMeasured?.let { "${it}h" } ?: "Not available"} | Discomfort flagged: ${if (phys.selfReportedDiscomfort) "YES" else "No"}")
                        if (phys.recoveryState == RecoveryState.REST_RECOMMENDED || phys.selfReportedDiscomfort) {
                            appendLine("• SAFETY DIRECTIVE: Do NOT train through fatigue or discomfort. Choose rest or gentle mobility only.")
                        } else if (phys.recoveryState == RecoveryState.RECOVERY_RECOMMENDED) {
                            appendLine("• ADVISORY: Recovery prioritized. Light walking or mobility recommended instead of heavy sets.")
                        } else {
                            appendLine("• ADVISORY: Cleared for structured training within your ${phys.joggingTargetMinutes}m aerobic baseline.")
                        }
                    }
                }
                "subject" in lower || "study" in lower || "exam" in lower || "revision" in lower || "int" in lower -> {
                    if (acad == null) {
                        appendLine("ACADEMIC STATUS UNAVAILABLE: Academic telemetry consent is disabled or not loaded.")
                    } else if (acad.activeSubjectsCount == 0) {
                        appendLine("ACADEMIC DIAGNOSTIC: No active subjects exist yet.")
                        appendLine("• Create subjects in Academic Engine → Subjects before scheduling focus blocks or revision quests.")
                    } else {
                        appendLine("ACADEMIC DIAGNOSTIC (INT=${p.attributeValues["INT"] ?: 10}, WIS=${p.attributeValues["WIS"] ?: 10})")
                        appendLine("• Study today: ${acad.todayStudyMinutes}/${acad.dailyGoalMinutes}m | 7d total: ${acad.weeklyStudyMinutes}m")
                        appendLine("• Revisions: ${acad.overdueRevisionsCount} overdue, ${acad.dueTodayRevisionsCount} due today.")
                        if (acad.weakestEstimatedTopics.isNotEmpty()) {
                            appendLine("• Lowest familiarity estimates (heuristic, not measured memory): ${acad.weakestEstimatedTopics.joinToString(", ")}")
                        }
                        if (acad.upcomingExams.isNotEmpty()) {
                            appendLine("• Upcoming exams: ${acad.upcomingExams.joinToString(", ")}")
                        } else {
                            appendLine("• No upcoming exams scheduled.")
                        }
                    }
                }
                "habit" in lower || "routine" in lower || "task" in lower || "goal" in lower || "discipline" in lower || "schedule" in lower -> {
                    val life = snapshot.lifeSummary
                    if (life == null) {
                        appendLine("LIFE TELEMETRY UNAVAILABLE: Life discipline sharing is disabled or uninitialized.")
                    } else {
                        appendLine("LIFE & DISCIPLINE DIAGNOSTIC (Score ${life.disciplineScore}/100)")
                        appendLine("• Routines today: ${life.completedRoutinesToday}/${life.activeRoutinesCount} | Habits today: ${life.completedHabitsToday}/${life.activeHabitsCount} (Best streak: ${life.bestHabitStreakDays}d)")
                        appendLine("• Tasks pending: ${life.pendingTasksCount} (${life.overdueTasksCount} overdue) | Active goals: ${life.activeGoalsCount} (${life.behindScheduleGoalsCount} behind pace)")
                        appendLine("• Calendar blocks today: ${life.scheduledBlocksToday} | Schedule conflicts: ${life.scheduleConflictsToday}")
                        if (life.scheduleConflictsToday > 0) {
                            appendLine("• ACTION: Resolve ${life.scheduleConflictsToday} overlapping block(s) via Daily Schedule conflict resolver.")
                        } else if (life.overdueTasksCount > 0) {
                            appendLine("• ACTION: Reschedule or complete ${life.overdueTasksCount} overdue task(s) without overloading today's buffer.")
                        } else {
                            appendLine("• ADVISORY: Strongest area is ${life.strongestArea}. Protect rest days and maintain routine consistency.")
                        }
                    }
                }
                "dungeon" in lower || "boss" in lower || "skill" in lower || "achievement" in lower || "shadow" in lower || "title" in lower -> {
                    val rpg = snapshot.rpgSummary
                    if (rpg == null) {
                        appendLine("RPG SYSTEMS TELEMETRY UNAVAILABLE: RPG context sharing is disabled or uninitialized.")
                    } else {
                        appendLine("RPG SYSTEMS ADVISORY (Title: '${rpg.equippedTitle}')")
                        appendLine("• Dungeons: ${rpg.activeDungeonsCount} active (${rpg.activeDungeonTitles.ifEmpty { listOf("None") }.joinToString(", ")}) | ${rpg.clearedDungeonsCount} cleared.")
                        appendLine("• Boss Battle: ${rpg.activeBossTitle?.let { "$it (${rpg.activeBossHpPercent ?: 100}% HP remaining)" } ?: "No active Boss Campaign"}")
                        appendLine("• Skills: ${rpg.unlockedSkillsCount}/${rpg.totalSkillsCount} unlocked | Achievements: ${rpg.unlockedAchievementsCount} | Shadows: ${rpg.shadowCollectionCount}")
                        appendLine("• SYSTEM RULE: Only verified physical, academic, or life records advance dungeon & boss objectives—AI never grants XP or bypasses gates.")
                    }
                }
                else -> {
                    appendLine("SYSTEM ANALYSIS — ${report.headline}")
                    appendLine("Strengths: ${report.strengths.joinToString(" ")}")
                    appendLine("Weaknesses: ${report.weaknesses.joinToString(" ")}")
                    appendLine("Next Actions: ${report.recommendedNextActions.joinToString(" ")}")
                }
            }
            if (snapshot.dataCompletenessNotes.isNotEmpty()) {
                appendLine()
                append("Unrecorded Data Notice: ${snapshot.dataCompletenessNotes.joinToString("; ")}")
            }
        }.trim()
    }
}
