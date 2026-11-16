package com.example.rawal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.rawal.data.local.entity.ActivityItemEntity
import com.example.rawal.data.local.entity.CategoryEntity
import com.example.rawal.data.local.entity.DailyRecordEntity
import com.example.rawal.domain.i18n.AppLanguage
import com.example.rawal.domain.i18n.AppStrings
import com.example.rawal.domain.i18n.AppStringsProvider

private data class FixedQuestionDraft(
    val fixedKey: String,
    val question: String,
    val defaultTitle: String,
    val defaultCategoryMatch: String,
    var answeredYes: Boolean?,
    var durationText: String,
    var details: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyStartupDialog(
    profileId: Long,
    date: String,
    existingRecord: DailyRecordEntity?,
    existingActivities: List<ActivityItemEntity>,
    categories: List<CategoryEntity>,
    strings: AppStrings,
    language: AppLanguage,
    onFinish: (
        wakeHour: Int?,
        wakeMinute: Int?,
        sleepHour: Int?,
        sleepMinute: Int?,
        fixedActivities: List<ActivityItemEntity>,
        additionalTasks: List<ActivityItemEntity>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val existingByKey = remember(existingActivities) {
        existingActivities.filter { it.fixedKey != null }.associateBy { it.fixedKey!! }
    }

    val drafts = remember {
        mutableStateListOf(
            FixedQuestionDraft(
                fixedKey = "FREE_READING",
                question = strings.qFreeReading,
                defaultTitle = strings.fixedTitleFreeReading,
                defaultCategoryMatch = "Read",
                answeredYes = existingByKey["FREE_READING"]?.isCompleted,
                durationText = (existingByKey["FREE_READING"]?.durationMinutes ?: 20).toString(),
                details = existingByKey["FREE_READING"]?.notes ?: ""
            ),
            FixedQuestionDraft(
                fixedKey = "SCHOOLWORK",
                question = strings.qDailySchoolwork,
                defaultTitle = strings.fixedTitleSchoolwork,
                defaultCategoryMatch = "Education",
                answeredYes = existingByKey["SCHOOLWORK"]?.isCompleted,
                durationText = (existingByKey["SCHOOLWORK"]?.durationMinutes ?: 45).toString(),
                details = existingByKey["SCHOOLWORK"]?.notes ?: ""
            ),
            FixedQuestionDraft(
                fixedKey = "SCHOOL_ATTENDANCE",
                question = strings.qAttendSchool,
                defaultTitle = strings.fixedTitleSchoolAttendance,
                defaultCategoryMatch = "Education",
                answeredYes = existingByKey["SCHOOL_ATTENDANCE"]?.isCompleted,
                durationText = (existingByKey["SCHOOL_ATTENDANCE"]?.durationMinutes ?: 360).toString(),
                details = existingByKey["SCHOOL_ATTENDANCE"]?.notes ?: ""
            ),
            FixedQuestionDraft(
                fixedKey = "BRUSH_TEETH",
                question = strings.qBrushTeeth,
                defaultTitle = strings.fixedTitleBrushTeeth,
                defaultCategoryMatch = "Personal",
                answeredYes = existingByKey["BRUSH_TEETH"]?.isCompleted,
                durationText = (existingByKey["BRUSH_TEETH"]?.durationMinutes ?: 5).toString(),
                details = existingByKey["BRUSH_TEETH"]?.notes ?: ""
            ),
            FixedQuestionDraft(
                fixedKey = "MORNING_EXERCISE",
                question = strings.qMorningExercise,
                defaultTitle = strings.fixedTitleMorningExercise,
                defaultCategoryMatch = "Exercise",
                answeredYes = existingByKey["MORNING_EXERCISE"]?.isCompleted,
                durationText = (existingByKey["MORNING_EXERCISE"]?.durationMinutes ?: 20).toString(),
                details = existingByKey["MORNING_EXERCISE"]?.notes ?: ""
            )
        )
    }

    var stepIndex by remember { mutableIntStateOf(0) } // 0..4 fixed, 5 wake, 6 sleep, 7 additional tasks
    var wakeH by remember { mutableStateOf((existingRecord?.wakeUpHour ?: 7).toString()) }
    var wakeM by remember { mutableStateOf((existingRecord?.wakeUpMinute ?: 0).toString()) }
    var sleepH by remember { mutableStateOf((existingRecord?.sleepHour ?: 23).toString()) }
    var sleepM by remember { mutableStateOf((existingRecord?.sleepMinute ?: 0).toString()) }

    val addedTasks = remember { mutableStateListOf<ActivityItemEntity>() }
    var showTaskForm by remember { mutableStateOf(false) }
    var extraTaskTitle by remember { mutableStateOf("") }
    var extraTaskNotes by remember { mutableStateOf("") }
    var extraTaskDuration by remember { mutableStateOf("30") }
    var extraTaskCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 0L) }
    var extraTaskError by remember { mutableStateOf<String?>(null) }

    fun findCategoryId(match: String): Long {
        return categories.firstOrNull { it.nameEn.contains(match, ignoreCase = true) }?.id
            ?: categories.firstOrNull()?.id ?: 0L
    }

    fun finishAll() {
        val fixedList = drafts.map { d ->
            val yes = d.answeredYes == true
            ActivityItemEntity(
                profileId = profileId,
                date = date,
                categoryId = findCategoryId(d.defaultCategoryMatch),
                title = d.defaultTitle,
                notes = d.details.trim(),
                durationMinutes = if (yes) (d.durationText.toIntOrNull()?.coerceIn(0, 1440) ?: 0) else 0,
                isCompleted = yes,
                isFixedDaily = true,
                fixedKey = d.fixedKey
            )
        }
        onFinish(
            wakeH.toIntOrNull()?.coerceIn(0, 23),
            wakeM.toIntOrNull()?.coerceIn(0, 59) ?: 0,
            sleepH.toIntOrNull()?.coerceIn(0, 23),
            sleepM.toIntOrNull()?.coerceIn(0, 59) ?: 0,
            fixedList,
            addedTasks.toList()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 520.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Column {
                Text(strings.dailySetupTitle, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { ((stepIndex + 1) / 8f).coerceIn(0.1f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (stepIndex) {
                    in 0..4 -> {
                        val current = drafts[stepIndex]
                        var yesState by remember(stepIndex) { mutableStateOf(current.answeredYes) }
                        var durState by remember(stepIndex) { mutableStateOf(current.durationText) }
                        var detState by remember(stepIndex) { mutableStateOf(current.details) }

                        Text(current.question, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = yesState == true,
                                onClick = {
                                    yesState = true
                                    current.answeredYes = true
                                },
                                label = { Text(strings.yes) },
                                modifier = Modifier.weight(1f).testTag("setup_yes_chip")
                            )
                            FilterChip(
                                selected = yesState == false,
                                onClick = {
                                    yesState = false
                                    current.answeredYes = false
                                    if (stepIndex < 7) stepIndex++
                                },
                                label = { Text(strings.no) },
                                modifier = Modifier.weight(1f).testTag("setup_no_chip")
                            )
                        }
                        if (yesState == true) {
                            OutlinedTextField(
                                value = durState,
                                onValueChange = { v ->
                                    if (v.all(Char::isDigit) && v.length <= 4) {
                                        durState = v
                                        current.durationText = v
                                    }
                                },
                                label = { Text(strings.durationMinutesLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setup_duration_input")
                            )
                            OutlinedTextField(
                                value = detState,
                                onValueChange = {
                                    detState = it
                                    current.details = it
                                },
                                label = { Text(strings.activityDetailsHint) },
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    5 -> {
                        Text(strings.qWakeUpTime, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = wakeH,
                                onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) wakeH = it },
                                label = { Text(strings.hourLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("setup_wake_hour")
                            )
                            OutlinedTextField(
                                value = wakeM,
                                onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) wakeM = it },
                                label = { Text(strings.minuteLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    6 -> {
                        Text(strings.qSleepTime, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = sleepH,
                                onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) sleepH = it },
                                label = { Text(strings.hourLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("setup_sleep_hour")
                            )
                            OutlinedTextField(
                                value = sleepM,
                                onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) sleepM = it },
                                label = { Text(strings.minuteLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    7 -> {
                        val prompt = if (addedTasks.isEmpty()) strings.qAnythingElse else strings.qAnotherTask
                        Text(prompt, style = MaterialTheme.typography.titleMedium)
                        if (addedTasks.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                addedTasks.forEach { t ->
                                    Text(
                                        text = "• ${t.title} (${AppStringsProvider.formatDuration(language, t.durationMinutes)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (!showTaskForm) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { showTaskForm = true },
                                    modifier = Modifier.weight(1f).testTag("setup_add_extra_yes")
                                ) {
                                    Text(strings.yes)
                                }
                                Button(
                                    onClick = { finishAll() },
                                    modifier = Modifier.weight(1f).testTag("setup_add_extra_no")
                                ) {
                                    Text(strings.no)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.forEach { cat ->
                                    val name = if (language == AppLanguage.FA) cat.nameFa else cat.nameEn
                                    FilterChip(
                                        selected = extraTaskCatId == cat.id,
                                        onClick = { extraTaskCatId = cat.id },
                                        label = { Text(name) },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(cat.colorHex))
                                            )
                                        }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = extraTaskTitle,
                                onValueChange = {
                                    extraTaskTitle = it
                                    extraTaskError = null
                                },
                                label = { Text(strings.taskNameLabel) },
                                isError = extraTaskError != null,
                                supportingText = extraTaskError?.let { msg -> { Text(msg) } },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setup_extra_task_name")
                            )
                            OutlinedTextField(
                                value = extraTaskDuration,
                                onValueChange = { if (it.all(Char::isDigit) && it.length <= 4) extraTaskDuration = it },
                                label = { Text(strings.durationMinutesLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = extraTaskNotes,
                                onValueChange = { extraTaskNotes = it },
                                label = { Text(strings.notesOptionalLabel) },
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { showTaskForm = false }) { Text(strings.cancel) }
                                Button(
                                    onClick = {
                                        if (extraTaskTitle.trim().isEmpty()) {
                                            extraTaskError = strings.validationRequiredField
                                            return@Button
                                        }
                                        addedTasks.add(
                                            ActivityItemEntity(
                                                profileId = profileId,
                                                date = date,
                                                categoryId = extraTaskCatId,
                                                title = extraTaskTitle.trim(),
                                                notes = extraTaskNotes.trim(),
                                                durationMinutes = extraTaskDuration.toIntOrNull()?.coerceIn(0, 1440) ?: 0,
                                                isCompleted = false,
                                                isFixedDaily = false
                                            )
                                        )
                                        extraTaskTitle = ""
                                        extraTaskNotes = ""
                                        extraTaskDuration = "30"
                                        showTaskForm = false
                                    }
                                ) {
                                    Text(strings.add)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (stepIndex < 7) {
                Button(
                    modifier = Modifier.testTag("setup_next_button"),
                    onClick = { stepIndex++ }
                ) {
                    Text(strings.next)
                }
            } else if (!showTaskForm) {
                Button(
                    modifier = Modifier.testTag("setup_finish_button"),
                    onClick = { finishAll() }
                ) {
                    Text(strings.done)
                }
            }
        },
        dismissButton = {
            Row {
                if (stepIndex > 0) {
                    TextButton(onClick = { stepIndex-- }) { Text(strings.back) }
                }
                TextButton(onClick = onDismiss) { Text(strings.cancel) }
            }
        }
    )
}
