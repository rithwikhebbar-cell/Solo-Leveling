package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AttributeType
import com.example.domain.model.DisciplineAssessment
import com.example.domain.model.GoalMilestone
import com.example.domain.model.GoalProgressRecord
import com.example.domain.model.GoalStatus
import com.example.domain.model.GoalTimeframe
import com.example.domain.model.Habit
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitCompletionRecord
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.HabitType
import com.example.domain.model.LifeGoal
import com.example.domain.model.LifeRoutine
import com.example.domain.model.PersonalTask
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.RoutineActivity
import com.example.domain.model.RoutineActivityStatus
import com.example.domain.model.RoutineCompletionRecord
import com.example.domain.model.RoutineTimeSlot
import com.example.domain.model.ScheduledActivityReference
import com.example.domain.model.ScheduledSourceType
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskRecurrenceConfig
import com.example.domain.model.TaskRecurrenceType
import com.example.domain.model.parseDayOfWeekCsv
import com.example.domain.model.toCsvString
import java.time.ZoneId

@Entity(
    tableName = "life_routines",
    indices = [Index(value = ["timeSlotName"])]
)
data class LifeRoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val timeSlotName: String,
    val startMinuteOfDay: Int,
    val scheduledDaysCsv: String,
    val isEnabled: Boolean,
    val targetDurationMinutes: Int,
    val reminderEnabled: Boolean,
    val isRestExemptAllowed: Boolean,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "routine_activities",
    foreignKeys = [
        ForeignKey(
            entity = LifeRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["routineId"])]
)
data class RoutineActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val title: String,
    val durationMinutes: Int,
    val orderIndex: Int,
    val isOptional: Boolean
)

@Entity(
    tableName = "routine_completions",
    foreignKeys = [
        ForeignKey(
            entity = LifeRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routineId"]),
        Index(value = ["dateEpochDay"]),
        Index(value = ["routineId", "activityId", "dateEpochDay"], unique = true)
    ]
)
data class RoutineCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val activityId: Long,
    val dateEpochDay: Long,
    val statusName: String,
    val note: String,
    val completedAtEpochMs: Long
)

@Entity(
    tableName = "habits",
    indices = [Index(value = ["isArchived"])]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val habitTypeName: String,
    val categoryName: String,
    val targetValue: Double,
    val unit: String,
    val scheduledDaysCsv: String,
    val preferredStartMinute: Int?,
    val reminderEnabled: Boolean,
    val isRestDayProtected: Boolean,
    val isArchived: Boolean,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["dateEpochDay"]),
        Index(value = ["habitId", "dateEpochDay"], unique = true)
    ]
)
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val dateEpochDay: Long,
    val statusName: String,
    val achievedValue: Double,
    val skipReason: String,
    val note: String,
    val recordedAtEpochMs: Long
)

@Entity(
    tableName = "personal_tasks",
    indices = [
        Index(value = ["statusName"]),
        Index(value = ["dueDateEpochDay"]),
        Index(value = ["linkedGoalId"])
    ]
)
data class PersonalTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val categoryName: String,
    val priorityName: String,
    val statusName: String,
    val dueDateEpochDay: Long?,
    val scheduledStartMinute: Int?,
    val estimatedMinutes: Int,
    val linkedGoalId: Long?,
    val isTimeSensitive: Boolean,
    val postponeCount: Int,
    val timezoneId: String,
    val completedAtEpochMs: Long?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(
    tableName = "task_recurrences",
    foreignKeys = [
        ForeignKey(
            entity = PersonalTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"], unique = true)]
)
data class TaskRecurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val recurrenceTypeName: String,
    val intervalDays: Int,
    val selectedDaysCsv: String,
    val endDateEpochDay: Long?
)

@Entity(
    tableName = "life_goals",
    indices = [
        Index(value = ["statusName"]),
        Index(value = ["timeframeName"])
    ]
)
data class LifeGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val timeframeName: String,
    val statusName: String,
    val targetAttributeName: String,
    val completionCriteria: String,
    val startDateEpochDay: Long,
    val targetDateEpochDay: Long,
    val linkedModule: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(
    tableName = "goal_milestones",
    foreignKeys = [
        ForeignKey(
            entity = LifeGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId"])]
)
data class GoalMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val title: String,
    val weight: Int,
    val dueDateEpochDay: Long?,
    val isCompleted: Boolean,
    val completedAtEpochMs: Long?,
    val orderIndex: Int
)

@Entity(
    tableName = "goal_progress_records",
    foreignKeys = [
        ForeignKey(
            entity = LifeGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["createdAtEpochMs"])
    ]
)
data class GoalProgressRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val note: String,
    val recordedProgressPercent: Int,
    val adjustmentSummary: String,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "scheduled_activity_references",
    indices = [
        Index(value = ["dateEpochDay"]),
        Index(value = ["sourceTypeName", "sourceRecordId"])
    ]
)
data class ScheduledActivityReferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceTypeName: String,
    val sourceRecordId: Long,
    val title: String,
    val subtitle: String,
    val dateEpochDay: Long,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val isAllDay: Boolean,
    val isRecurringInstance: Boolean,
    val isCompletedSnapshot: Boolean,
    val timezoneId: String,
    val notes: String,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "discipline_assessments",
    indices = [Index(value = ["dateEpochDay"], unique = true)]
)
data class DisciplineAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val disciplineScore: Int,
    val routineConsistencyPercent: Int,
    val habitConsistencyPercent: Int,
    val taskCompletionPercent: Int,
    val scheduleAdherencePercent: Int,
    val goalProgressPercent: Int,
    val strongestArea: String,
    val weakestArea: String,
    val missedCommitmentsCount: Int,
    val exemptRestDaysCount: Int,
    val reflectionSummary: String,
    val calculatedAtEpochMs: Long
)

@Entity(
    tableName = "life_progression_records",
    indices = [
        Index(value = ["rewardKey"], unique = true),
        Index(value = ["timestampEpochMs"])
    ]
)
data class LifeProgressionRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rewardKey: String,
    val activityKindName: String,
    val title: String,
    val verificationStatusName: String,
    val xpAwarded: Int,
    val disXpAwarded: Int,
    val wisXpAwarded: Int,
    val chaXpAwarded: Int,
    val intXpAwarded: Int,
    val vitXpAwarded: Int,
    val notes: String,
    val timestampEpochMs: Long
)

// Domain <-> Entity Mappers

fun LifeRoutineEntity.toDomain() = LifeRoutine(
    id = id,
    title = title,
    description = description,
    timeSlot = runCatching { RoutineTimeSlot.valueOf(timeSlotName) }.getOrDefault(RoutineTimeSlot.CUSTOM),
    startMinuteOfDay = startMinuteOfDay,
    scheduledDays = scheduledDaysCsv.parseDayOfWeekCsv(),
    isEnabled = isEnabled,
    targetDurationMinutes = targetDurationMinutes,
    reminderEnabled = reminderEnabled,
    isRestExemptAllowed = isRestExemptAllowed,
    createdAtEpochMs = createdAtEpochMs
)

fun LifeRoutine.toEntity() = LifeRoutineEntity(
    id = id,
    title = title,
    description = description,
    timeSlotName = timeSlot.name,
    startMinuteOfDay = startMinuteOfDay,
    scheduledDaysCsv = scheduledDays.toCsvString(),
    isEnabled = isEnabled,
    targetDurationMinutes = targetDurationMinutes,
    reminderEnabled = reminderEnabled,
    isRestExemptAllowed = isRestExemptAllowed,
    createdAtEpochMs = createdAtEpochMs
)

fun RoutineActivityEntity.toDomain() = RoutineActivity(
    id = id,
    routineId = routineId,
    title = title,
    durationMinutes = durationMinutes,
    orderIndex = orderIndex,
    isOptional = isOptional
)

fun RoutineActivity.toEntity() = RoutineActivityEntity(
    id = id,
    routineId = routineId,
    title = title,
    durationMinutes = durationMinutes,
    orderIndex = orderIndex,
    isOptional = isOptional
)

fun RoutineCompletionEntity.toDomain() = RoutineCompletionRecord(
    id = id,
    routineId = routineId,
    activityId = activityId,
    dateEpochDay = dateEpochDay,
    status = runCatching { RoutineActivityStatus.valueOf(statusName) }.getOrDefault(RoutineActivityStatus.COMPLETED),
    note = note,
    completedAtEpochMs = completedAtEpochMs
)

fun RoutineCompletionRecord.toEntity() = RoutineCompletionEntity(
    id = id,
    routineId = routineId,
    activityId = activityId,
    dateEpochDay = dateEpochDay,
    statusName = status.name,
    note = note,
    completedAtEpochMs = completedAtEpochMs
)

fun HabitEntity.toDomain() = Habit(
    id = id,
    title = title,
    description = description,
    habitType = runCatching { HabitType.valueOf(habitTypeName) }.getOrDefault(HabitType.BINARY),
    category = runCatching { HabitCategory.valueOf(categoryName) }.getOrDefault(HabitCategory.DISCIPLINE),
    targetValue = targetValue,
    unit = unit,
    scheduledDays = scheduledDaysCsv.parseDayOfWeekCsv(),
    preferredStartMinute = preferredStartMinute,
    reminderEnabled = reminderEnabled,
    isRestDayProtected = isRestDayProtected,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs
)

fun Habit.toEntity() = HabitEntity(
    id = id,
    title = title,
    description = description,
    habitTypeName = habitType.name,
    categoryName = category.name,
    targetValue = targetValue,
    unit = unit,
    scheduledDaysCsv = scheduledDays.toCsvString(),
    preferredStartMinute = preferredStartMinute,
    reminderEnabled = reminderEnabled,
    isRestDayProtected = isRestDayProtected,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs
)

fun HabitCompletionEntity.toDomain() = HabitCompletionRecord(
    id = id,
    habitId = habitId,
    dateEpochDay = dateEpochDay,
    status = runCatching { HabitDayStatus.valueOf(statusName) }.getOrDefault(HabitDayStatus.COMPLETED),
    achievedValue = achievedValue,
    skipReason = skipReason,
    note = note,
    recordedAtEpochMs = recordedAtEpochMs
)

fun HabitCompletionRecord.toEntity() = HabitCompletionEntity(
    id = id,
    habitId = habitId,
    dateEpochDay = dateEpochDay,
    statusName = status.name,
    achievedValue = achievedValue,
    skipReason = skipReason,
    note = note,
    recordedAtEpochMs = recordedAtEpochMs
)

fun TaskRecurrenceEntity.toDomain() = TaskRecurrenceConfig(
    id = id,
    taskId = taskId,
    recurrenceType = runCatching { TaskRecurrenceType.valueOf(recurrenceTypeName) }.getOrDefault(TaskRecurrenceType.NONE),
    intervalDays = intervalDays,
    selectedDays = selectedDaysCsv.parseDayOfWeekCsv(),
    endDateEpochDay = endDateEpochDay
)

fun TaskRecurrenceConfig.toEntity() = TaskRecurrenceEntity(
    id = id,
    taskId = taskId,
    recurrenceTypeName = recurrenceType.name,
    intervalDays = intervalDays,
    selectedDaysCsv = selectedDays.toCsvString(),
    endDateEpochDay = endDateEpochDay
)

fun PersonalTaskEntity.toDomain(recurrence: TaskRecurrenceConfig? = null) = PersonalTask(
    id = id,
    title = title,
    description = description,
    category = runCatching { TaskCategory.valueOf(categoryName) }.getOrDefault(TaskCategory.PERSONAL),
    priority = runCatching { TaskPriority.valueOf(priorityName) }.getOrDefault(TaskPriority.MEDIUM),
    status = runCatching { PersonalTaskStatus.valueOf(statusName) }.getOrDefault(PersonalTaskStatus.PENDING),
    dueDateEpochDay = dueDateEpochDay,
    scheduledStartMinute = scheduledStartMinute,
    estimatedMinutes = estimatedMinutes,
    linkedGoalId = linkedGoalId,
    isTimeSensitive = isTimeSensitive,
    postponeCount = postponeCount,
    timezoneId = timezoneId.ifBlank { ZoneId.systemDefault().id },
    recurrence = recurrence,
    completedAtEpochMs = completedAtEpochMs,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun PersonalTask.toEntity() = PersonalTaskEntity(
    id = id,
    title = title,
    description = description,
    categoryName = category.name,
    priorityName = priority.name,
    statusName = status.name,
    dueDateEpochDay = dueDateEpochDay,
    scheduledStartMinute = scheduledStartMinute,
    estimatedMinutes = estimatedMinutes,
    linkedGoalId = linkedGoalId,
    isTimeSensitive = isTimeSensitive,
    postponeCount = postponeCount,
    timezoneId = timezoneId,
    completedAtEpochMs = completedAtEpochMs,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun LifeGoalEntity.toDomain() = LifeGoal(
    id = id,
    title = title,
    description = description,
    timeframe = runCatching { GoalTimeframe.valueOf(timeframeName) }.getOrDefault(GoalTimeframe.MONTHLY),
    status = runCatching { GoalStatus.valueOf(statusName) }.getOrDefault(GoalStatus.ACTIVE),
    targetAttribute = runCatching { AttributeType.valueOf(targetAttributeName) }.getOrDefault(AttributeType.DIS),
    completionCriteria = completionCriteria,
    startDateEpochDay = startDateEpochDay,
    targetDateEpochDay = targetDateEpochDay,
    linkedModule = linkedModule,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun LifeGoal.toEntity() = LifeGoalEntity(
    id = id,
    title = title,
    description = description,
    timeframeName = timeframe.name,
    statusName = status.name,
    targetAttributeName = targetAttribute.name,
    completionCriteria = completionCriteria,
    startDateEpochDay = startDateEpochDay,
    targetDateEpochDay = targetDateEpochDay,
    linkedModule = linkedModule,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun GoalMilestoneEntity.toDomain() = GoalMilestone(
    id = id,
    goalId = goalId,
    title = title,
    weight = weight,
    dueDateEpochDay = dueDateEpochDay,
    isCompleted = isCompleted,
    completedAtEpochMs = completedAtEpochMs,
    orderIndex = orderIndex
)

fun GoalMilestone.toEntity() = GoalMilestoneEntity(
    id = id,
    goalId = goalId,
    title = title,
    weight = weight,
    dueDateEpochDay = dueDateEpochDay,
    isCompleted = isCompleted,
    completedAtEpochMs = completedAtEpochMs,
    orderIndex = orderIndex
)

fun GoalProgressRecordEntity.toDomain() = GoalProgressRecord(
    id = id,
    goalId = goalId,
    note = note,
    recordedProgressPercent = recordedProgressPercent,
    adjustmentSummary = adjustmentSummary,
    createdAtEpochMs = createdAtEpochMs
)

fun GoalProgressRecord.toEntity() = GoalProgressRecordEntity(
    id = id,
    goalId = goalId,
    note = note,
    recordedProgressPercent = recordedProgressPercent,
    adjustmentSummary = adjustmentSummary,
    createdAtEpochMs = createdAtEpochMs
)

fun ScheduledActivityReferenceEntity.toDomain() = ScheduledActivityReference(
    id = id,
    sourceType = runCatching { ScheduledSourceType.valueOf(sourceTypeName) }.getOrDefault(ScheduledSourceType.CUSTOM_EVENT),
    sourceRecordId = sourceRecordId,
    title = title,
    subtitle = subtitle,
    dateEpochDay = dateEpochDay,
    startMinuteOfDay = startMinuteOfDay,
    durationMinutes = durationMinutes,
    isAllDay = isAllDay,
    isRecurringInstance = isRecurringInstance,
    isCompletedSnapshot = isCompletedSnapshot,
    timezoneId = timezoneId.ifBlank { ZoneId.systemDefault().id },
    notes = notes,
    createdAtEpochMs = createdAtEpochMs
)

fun ScheduledActivityReference.toEntity() = ScheduledActivityReferenceEntity(
    id = id,
    sourceTypeName = sourceType.name,
    sourceRecordId = sourceRecordId,
    title = title,
    subtitle = subtitle,
    dateEpochDay = dateEpochDay,
    startMinuteOfDay = startMinuteOfDay,
    durationMinutes = durationMinutes,
    isAllDay = isAllDay,
    isRecurringInstance = isRecurringInstance,
    isCompletedSnapshot = isCompletedSnapshot,
    timezoneId = timezoneId,
    notes = notes,
    createdAtEpochMs = createdAtEpochMs
)

fun DisciplineAssessmentEntity.toDomain() = DisciplineAssessment(
    id = id,
    dateEpochDay = dateEpochDay,
    disciplineScore = disciplineScore,
    routineConsistencyPercent = routineConsistencyPercent,
    habitConsistencyPercent = habitConsistencyPercent,
    taskCompletionPercent = taskCompletionPercent,
    scheduleAdherencePercent = scheduleAdherencePercent,
    goalProgressPercent = goalProgressPercent,
    strongestArea = strongestArea,
    weakestArea = weakestArea,
    missedCommitmentsCount = missedCommitmentsCount,
    exemptRestDaysCount = exemptRestDaysCount,
    reflectionSummary = reflectionSummary,
    calculatedAtEpochMs = calculatedAtEpochMs
)

fun DisciplineAssessment.toEntity() = DisciplineAssessmentEntity(
    id = id,
    dateEpochDay = dateEpochDay,
    disciplineScore = disciplineScore,
    routineConsistencyPercent = routineConsistencyPercent,
    habitConsistencyPercent = habitConsistencyPercent,
    taskCompletionPercent = taskCompletionPercent,
    scheduleAdherencePercent = scheduleAdherencePercent,
    goalProgressPercent = goalProgressPercent,
    strongestArea = strongestArea,
    weakestArea = weakestArea,
    missedCommitmentsCount = missedCommitmentsCount,
    exemptRestDaysCount = exemptRestDaysCount,
    reflectionSummary = reflectionSummary,
    calculatedAtEpochMs = calculatedAtEpochMs
)
