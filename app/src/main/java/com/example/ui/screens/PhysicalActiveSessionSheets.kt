package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.catalog.ExerciseCatalog
import com.example.domain.model.ActiveJoggingSessionState
import com.example.domain.model.ExerciseDefinition
import com.example.domain.model.ExerciseRecord
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WorkoutCategory
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.theme.SystemColors
import kotlinx.coroutines.delay

@Composable
fun ActiveJoggingSheetContent(
    activeState: ActiveJoggingSessionState,
    onPauseResume: () -> Unit,
    onDiscard: () -> Unit,
    onComplete: (title: String, distanceKm: String, notes: String) -> Unit
) {
    var elapsedSeconds by remember { mutableStateOf(activeState.currentElapsedSeconds()) }
    var title by rememberSaveable { mutableStateOf("Daily Jogging Session") }
    var distanceKm by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(activeState.isActive, activeState.isPaused, activeState.lastResumedAtMs) {
        while (activeState.isActive) {
            elapsedSeconds = activeState.currentElapsedSeconds()
            delay(1000L)
        }
    }

    val mins = elapsedSeconds / 60L
    val secs = elapsedSeconds % 60L
    val formattedTimer = "%02d:%02d".format(mins, secs)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE JOGGING SESSION",
                style = MaterialTheme.typography.titleLarge,
                color = SystemColors.ElectricCyan
            )
            ProvenanceChip(status = VerificationStatus.VERIFIED_SYSTEM_TIMER)
        }

        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.ElectricCyan
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (activeState.isPaused) "PAUSED" else "TIMER ACTIVE",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (activeState.isPaused) SystemColors.WarningAmber else SystemColors.VerifiedEmerald
                )
                Text(
                    text = formattedTimer,
                    style = MaterialTheme.typography.displayLarge,
                    color = SystemColors.PureIce
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onPauseResume,
                        modifier = Modifier.testTag("pause_resume_jog_button"),
                        border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                    ) {
                        Icon(
                            imageVector = if (activeState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = if (activeState.isPaused) "Resume" else "Pause",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (activeState.isPaused) "RESUME" else "PAUSE")
                    }
                    OutlinedButton(
                        onClick = onDiscard,
                        border = BorderStroke(1.dp, SystemColors.DangerCrimson)
                    ) {
                        Text("DISCARD", color = SystemColors.DangerCrimson)
                    }
                }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Session Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        OutlinedTextField(
            value = distanceKm,
            onValueChange = { distanceKm = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("Distance in km (optional — leave blank if unknown)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        Button(
            onClick = { onComplete(title, distanceKm, notes) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("complete_active_jog_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = SystemColors.VerifiedEmerald,
                contentColor = SystemColors.ObsidianBlack
            )
        ) {
            Text("COMPLETE & RECORD SESSION", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ManualJoggingSheetContent(
    onDismiss: () -> Unit,
    onSubmit: (title: String, durationMinutes: Int, distanceKm: String, notes: String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("Jogging Session") }
    var durationText by rememberSaveable { mutableStateOf("30") }
    var distanceKm by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    val duration = durationText.toIntOrNull() ?: 0
    val valid = title.isNotBlank() && duration in 1..360

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MANUAL JOGGING LOG",
                style = MaterialTheme.typography.titleLarge,
                color = SystemColors.ElectricCyan
            )
            ProvenanceChip(status = VerificationStatus.SELF_REPORTED)
        }

        Text(
            text = "Self-reported sessions carry 0.65x progression weight and respect daily physical reward caps.",
            style = MaterialTheme.typography.bodySmall,
            color = SystemColors.MutedSilver
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Session Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )
        OutlinedTextField(
            value = durationText,
            onValueChange = { durationText = it.filter { ch -> ch.isDigit() }.take(3) },
            label = { Text("Duration (minutes, 1–360)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_jog_duration_input"),
            colors = fieldColors()
        )
        OutlinedTextField(
            value = distanceKm,
            onValueChange = { distanceKm = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("Distance in km (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("CANCEL")
            }
            Button(
                onClick = { onSubmit(title, duration, distanceKm, notes) },
                enabled = valid,
                modifier = Modifier
                    .weight(1f)
                    .testTag("submit_manual_jog_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack
                )
            ) {
                Text("SAVE LOG", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private data class ExerciseInputState(
    val selected: Boolean,
    val sets: Int,
    val reps: Int,
    val restSeconds: Int,
    val weightKgText: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutBuilderSheetContent(
    initialCategory: WorkoutCategory,
    onDismiss: () -> Unit,
    onCompleteWorkout: (
        title: String,
        category: WorkoutCategory,
        durationMinutes: Int,
        exercises: List<ExerciseRecord>,
        notes: String
    ) -> Unit
) {
    var selectedCategory by rememberSaveable { mutableStateOf(initialCategory) }
    var workoutTitle by rememberSaveable { mutableStateOf("${initialCategory.label} Session") }
    var durationText by rememberSaveable { mutableStateOf("35") }
    var notes by rememberSaveable { mutableStateOf("") }

    var restCountdown by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(restCountdown) {
        if (restCountdown > 0) {
            delay(1000L)
            restCountdown -= 1
        }
    }

    val exercisesInCategory = remember(selectedCategory) {
        ExerciseCatalog.byCategory(selectedCategory)
    }

    val exerciseStates = remember {
        mutableStateMapOf<String, ExerciseInputState>().apply {
            ExerciseCatalog.allExercises.forEachIndexed { index, ex ->
                put(
                    ex.id,
                    ExerciseInputState(
                        selected = index < 2,
                        sets = ex.defaultSets,
                        reps = ex.defaultReps,
                        restSeconds = ex.defaultRestSeconds,
                        weightKgText = ""
                    )
                )
            }
        }
    }

    val selectedRecords = exercisesInCategory.mapNotNull { def ->
        val st = exerciseStates[def.id] ?: return@mapNotNull null
        if (!st.selected) return@mapNotNull null
        ExerciseRecord(
            exerciseId = def.id,
            exerciseName = def.name,
            category = def.category,
            setsCompleted = st.sets.coerceAtLeast(1),
            repsPerSet = st.reps.coerceAtLeast(1),
            weightKg = st.weightKgText.toDoubleOrNull()?.takeIf { it > 0.0 },
            durationSeconds = if (def.isTimedSeconds) st.reps else null,
            restSeconds = st.restSeconds,
            primaryAttribute = def.primaryAttribute
        )
    }

    val durationMinutes = durationText.toIntOrNull() ?: 0
    val canSave = workoutTitle.isNotBlank() && durationMinutes in 1..360 && selectedRecords.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STRUCTURED WORKOUT",
                style = MaterialTheme.typography.titleLarge,
                color = SystemColors.ElectricCyan
            )
            ProvenanceChip(status = VerificationStatus.SELF_REPORTED)
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WorkoutCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = {
                        selectedCategory = cat
                        workoutTitle = "${cat.label} Session"
                        ExerciseCatalog.byCategory(cat).firstOrNull()?.let { first ->
                            val cur = exerciseStates[first.id]
                            if (cur != null && !cur.selected) {
                                exerciseStates[first.id] = cur.copy(selected = true)
                            }
                        }
                    },
                    label = { Text(cat.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                        selectedLabelColor = SystemColors.ElectricCyan
                    )
                )
            }
        }

        if (restCountdown > 0) {
            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, contentDescription = "Rest timer", tint = SystemColors.ArcaneViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("REST TIMER: ${restCountdown}s", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                    }
                    OutlinedButton(onClick = { restCountdown = 0 }) {
                        Text("SKIP")
                    }
                }
            }
        }

        OutlinedTextField(
            value = workoutTitle,
            onValueChange = { workoutTitle = it },
            label = { Text("Workout Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        OutlinedTextField(
            value = durationText,
            onValueChange = { durationText = it.filter { ch -> ch.isDigit() }.take(3) },
            label = { Text("Total Duration (minutes)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        Text(
            text = "SELECT EXERCISES & SETS",
            style = MaterialTheme.typography.labelMedium,
            color = SystemColors.MetallicSilver
        )

        exercisesInCategory.forEach { def ->
            val st = exerciseStates[def.id] ?: ExerciseInputState(false, def.defaultSets, def.defaultReps, def.defaultRestSeconds, "")
            ExerciseSelectionCard(
                def = def,
                selected = st.selected,
                sets = st.sets,
                reps = st.reps,
                weightKgText = st.weightKgText,
                onToggleSelected = { checked -> exerciseStates[def.id] = st.copy(selected = checked) },
                onSetsChange = { newSets -> exerciseStates[def.id] = st.copy(sets = newSets.coerceIn(1, 12)) },
                onRepsChange = { newReps -> exerciseStates[def.id] = st.copy(reps = newReps.coerceIn(1, 180)) },
                onWeightChange = { w -> exerciseStates[def.id] = st.copy(weightKgText = w.filter { ch -> ch.isDigit() || ch == '.' }) },
                onStartRest = { restCountdown = st.restSeconds }
            )
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Session Notes (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("CANCEL")
            }
            Button(
                onClick = { onCompleteWorkout(workoutTitle, selectedCategory, durationMinutes, selectedRecords, notes) },
                enabled = canSave,
                modifier = Modifier
                    .weight(1f)
                    .testTag("complete_workout_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack
                )
            ) {
                Text("FINISH (${selectedRecords.size})", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ExerciseSelectionCard(
    def: ExerciseDefinition,
    selected: Boolean,
    sets: Int,
    reps: Int,
    weightKgText: String,
    onToggleSelected: (Boolean) -> Unit,
    onSetsChange: (Int) -> Unit,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (String) -> Unit,
    onStartRest: () -> Unit
) {
    Surface(
        color = SystemColors.ObsidianBlack.copy(alpha = 0.65f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) SystemColors.ElectricCyan.copy(alpha = 0.6f) else SystemColors.SubtleGridLine
        )
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = onToggleSelected,
                    colors = CheckboxDefaults.colors(checkedColor = SystemColors.ElectricCyan)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(def.name, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                    Text(
                        text = "${def.primaryAttribute.code} • Rest ${def.defaultRestSeconds}s • ${def.cue}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MutedSilver
                    )
                }
            }

            if (selected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepperChip(label = "Sets", value = sets, onDelta = { onSetsChange(sets + it) })
                    StepperChip(
                        label = if (def.isTimedSeconds) "Sec" else "Reps",
                        value = reps,
                        step = if (def.isTimedSeconds) 5 else 1,
                        onDelta = { onRepsChange(reps + it) }
                    )
                    OutlinedButton(
                        onClick = onStartRest,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("REST ${def.defaultRestSeconds}s", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (!def.isTimedSeconds) {
                    OutlinedTextField(
                        value = weightKgText,
                        onValueChange = onWeightChange,
                        label = { Text("Load kg (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperChip(
    label: String,
    value: Int,
    step: Int = 1,
    onDelta: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onDelta(-step) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Remove, contentDescription = "Decrease $label", tint = SystemColors.ElectricCyan)
        }
        Text("$label: $value", style = MaterialTheme.typography.labelMedium, color = SystemColors.PureIce)
        IconButton(onClick = { onDelta(step) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = "Increase $label", tint = SystemColors.ElectricCyan)
        }
    }
}

@Composable
fun RecoveryCheckInSheetContent(
    initialFatigue: Int,
    initialDiscomfort: Boolean,
    initialNotes: String,
    onDismiss: () -> Unit,
    onSubmit: (fatigueLevel: Int, hasDiscomfort: Boolean, notes: String) -> Unit
) {
    var fatigue by rememberSaveable { mutableIntStateOf(initialFatigue.coerceIn(1, 5)) }
    var discomfort by rememberSaveable { mutableStateOf(initialDiscomfort) }
    var notes by rememberSaveable { mutableStateOf(initialNotes) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "RECOVERY & FATIGUE CHECK-IN",
            style = MaterialTheme.typography.titleLarge,
            color = SystemColors.ElectricCyan
        )
        Text(
            text = "Self-reported signals help the System prevent overtraining. Reporting fatigue or discomfort never penalizes your progression.",
            style = MaterialTheme.typography.bodySmall,
            color = SystemColors.MutedSilver
        )

        Text(
            text = "Fatigue Level: $fatigue / 5 (${fatigueLabel(fatigue)})",
            style = MaterialTheme.typography.titleMedium,
            color = SystemColors.PureIce
        )
        Slider(
            value = fatigue.toFloat(),
            onValueChange = { fatigue = it.toInt().coerceIn(1, 5) },
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(
                thumbColor = SystemColors.ElectricCyan,
                activeTrackColor = SystemColors.ElectricCyan
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Reporting pain, illness, or joint discomfort",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SystemColors.PureIce
                )
                Text(
                    text = "Automatically recommends rest — never trains through pain.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MutedSilver
                )
            }
            Switch(
                checked = discomfort,
                onCheckedChange = { discomfort = it },
                modifier = Modifier.testTag("recovery_discomfort_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SystemColors.ObsidianBlack,
                    checkedTrackColor = SystemColors.WarningAmber
                )
            )
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Recovery notes (sleep quality, soreness, optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("CANCEL")
            }
            Button(
                onClick = { onSubmit(fatigue, discomfort, notes) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("submit_recovery_checkin_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack
                )
            ) {
                Text("UPDATE RECOVERY", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun fatigueLabel(level: Int): String = when (level) {
    1 -> "Fresh"
    2 -> "Lightly Tired"
    3 -> "Moderate"
    4 -> "Heavy Fatigue"
    else -> "Exhausted"
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SystemColors.ElectricCyan,
    unfocusedBorderColor = SystemColors.PanelBorderCyan,
    focusedTextColor = SystemColors.PureIce,
    unfocusedTextColor = SystemColors.PureIce
)
