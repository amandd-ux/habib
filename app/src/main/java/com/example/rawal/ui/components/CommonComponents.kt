package com.example.rawal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.rawal.data.local.entity.ActivityItemEntity
import com.example.rawal.data.local.entity.CategoryEntity
import com.example.rawal.data.local.entity.DailyRecordEntity
import com.example.rawal.domain.i18n.AppLanguage
import com.example.rawal.domain.i18n.AppStrings
import com.example.rawal.domain.i18n.AppStringsProvider

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    tip: String,
    icon: ImageVector = Icons.Rounded.AutoAwesome,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SectionCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = tip,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ProfileAvatar(
    avatarKey: String,
    customAvatarUri: String?,
    contentDescription: String,
    size: Dp = 40.dp
) {
    if (!customAvatarUri.isNullOrBlank()) {
        AsyncImage(
            model = customAvatarUri,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val (bgColor, icon) = when (avatarKey) {
            "indigo_compass" -> Color(0xFF4338CA) to Icons.Rounded.Explore
            "emerald_run" -> Color(0xFF059669) to Icons.AutoMirrored.Rounded.DirectionsRun
            "amber_light" -> Color(0xFFD97706) to Icons.Rounded.Lightbulb
            "rose_book" -> Color(0xFFE11D48) to Icons.AutoMirrored.Rounded.MenuBook
            "slate_peak" -> Color(0xFF334155) to Icons.Rounded.Landscape
            else -> Color(0xFF0F766E) to Icons.Rounded.WbSunny
        }
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = bgColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.56f)
                )
            }
        }
    }
}

fun categoryIconForKey(iconKey: String): ImageVector = when (iconKey) {
    "school" -> Icons.Rounded.School
    "book" -> Icons.AutoMirrored.Rounded.MenuBook
    "fitness" -> Icons.Rounded.FitnessCenter
    "person" -> Icons.Rounded.Person
    "lightbulb" -> Icons.Rounded.Lightbulb
    "movie" -> Icons.Rounded.Movie
    "sports_esports" -> Icons.Rounded.SportsEsports
    "more_horiz" -> Icons.Rounded.MoreHoriz
    else -> Icons.Rounded.Category
}

@Composable
fun ConfirmationDialog(
    title: String,
    body: String,
    confirmText: String,
    cancelText: String,
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 460.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else ButtonDefaults.buttonColors()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelText) }
        }
    )
}

@Composable
fun ActivityEditorDialog(
    initial: ActivityItemEntity?,
    profileId: Long,
    date: String,
    categories: List<CategoryEntity>,
    strings: AppStrings,
    language: AppLanguage,
    onSave: (ActivityItemEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var selectedCatId by remember {
        mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id ?: 0L)
    }
    var durationText by remember {
        mutableStateOf((initial?.durationMinutes ?: 30).let { if (it == 0 && initial == null) "30" else it.toString() })
    }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var isCompleted by remember { mutableStateOf(initial?.isCompleted ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 520.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(if (initial == null) strings.addActivity else strings.editActivity) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMsg = null
                    },
                    label = { Text(strings.taskNameLabel) },
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activity_title_input")
                )

                Text(strings.categoryLabel, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val catName = if (language == AppLanguage.FA) cat.nameFa else cat.nameEn
                        FilterChip(
                            selected = selectedCatId == cat.id,
                            onClick = { selectedCatId = cat.id },
                            label = { Text(catName) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.colorHex))
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { v ->
                        if (v.all { it.isDigit() } && v.length <= 4) durationText = v
                    },
                    label = { Text(strings.durationMinutesLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activity_duration_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15, 30, 45, 60, 90).forEach { preset ->
                        FilterChip(
                            selected = durationText == preset.toString(),
                            onClick = { durationText = preset.toString() },
                            label = { Text("${AppStringsProvider.toLocalizedDigits(preset.toString(), language)}${strings.minutesShort}") }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesOptionalLabel) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isCompleted = !isCompleted }
                ) {
                    Checkbox(checked = isCompleted, onCheckedChange = { isCompleted = it })
                    Text(strings.completed, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag("save_activity_button"),
                onClick = {
                    if (title.trim().isEmpty()) {
                        errorMsg = strings.validationRequiredField
                        return@Button
                    }
                    val minutes = durationText.toIntOrNull()?.coerceIn(0, 1440) ?: 0
                    val item = (initial ?: ActivityItemEntity(
                        profileId = profileId,
                        date = date,
                        categoryId = selectedCatId,
                        title = title.trim()
                    )).copy(
                        categoryId = selectedCatId,
                        title = title.trim(),
                        notes = notes.trim(),
                        durationMinutes = minutes,
                        isCompleted = isCompleted
                    )
                    onSave(item)
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun EndOfDayReviewDialog(
    record: DailyRecordEntity,
    strings: AppStrings,
    onSave: (DailyRecordEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var satisfaction by remember { mutableStateOf(record.satisfaction ?: "SATISFIED") }
    var mood by remember { mutableStateOf(record.mood ?: "GOOD") }
    var unexpected by remember { mutableStateOf(record.unexpectedEvents) }
    var disruptions by remember { mutableStateOf(record.disruptions) }
    var notes by remember { mutableStateOf(record.dailyNotes) }
    var wastedText by remember { mutableStateOf(record.wastedMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 520.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(strings.endOfDayReview) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(strings.eodQuestion, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = satisfaction == "SATISFIED",
                        onClick = { satisfaction = "SATISFIED" },
                        label = { Text(strings.satisfied) },
                        modifier = Modifier.weight(1f).testTag("eod_satisfied_chip")
                    )
                    FilterChip(
                        selected = satisfaction == "NOT_SATISFIED",
                        onClick = { satisfaction = "NOT_SATISFIED" },
                        label = { Text(strings.notSatisfied) },
                        modifier = Modifier.weight(1f).testTag("eod_not_satisfied_chip")
                    )
                }

                Text(strings.dailyMoodLabel, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val moods = listOf(
                        "GREAT" to strings.moodGreat,
                        "GOOD" to strings.moodGood,
                        "NEUTRAL" to strings.moodNeutral,
                        "TIRED" to strings.moodTired,
                        "STRESSED" to strings.moodStressed
                    )
                    moods.forEach { (key, label) ->
                        FilterChip(
                            selected = mood == key,
                            onClick = { mood = key },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = wastedText,
                    onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 4) wastedText = it },
                    label = { Text("${strings.wastedTime} (${strings.minutesShort})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unexpected,
                    onValueChange = { unexpected = it },
                    label = { Text(strings.unexpectedEventsLabel) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = disruptions,
                    onValueChange = { disruptions = it },
                    label = { Text(strings.disruptionsLabel) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.dailyNotesLabel) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag("save_eod_review_button"),
                onClick = {
                    onSave(
                        record.copy(
                            satisfaction = satisfaction,
                            mood = mood,
                            wastedMinutes = wastedText.toIntOrNull()?.coerceIn(0, 1440) ?: 0,
                            unexpectedEvents = unexpected.trim(),
                            disruptions = disruptions.trim(),
                            dailyNotes = notes.trim(),
                            reviewCompleted = true
                        )
                    )
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun DaySleepWastedDialog(
    record: DailyRecordEntity,
    strings: AppStrings,
    onSave: (DailyRecordEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var wakeH by remember { mutableStateOf((record.wakeUpHour ?: 7).toString()) }
    var wakeM by remember { mutableStateOf((record.wakeUpMinute ?: 0).toString()) }
    var sleepH by remember { mutableStateOf((record.sleepHour ?: 23).toString()) }
    var sleepM by remember { mutableStateOf((record.sleepMinute ?: 0).toString()) }
    var wasted by remember { mutableStateOf(record.wastedMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 480.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(strings.editDayMetrics) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.wakeUpTime, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wakeH,
                        onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) wakeH = it },
                        label = { Text(strings.hourLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = wakeM,
                        onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) wakeM = it },
                        label = { Text(strings.minuteLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(strings.bedtime, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sleepH,
                        onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) sleepH = it },
                        label = { Text(strings.hourLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sleepM,
                        onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) sleepM = it },
                        label = { Text(strings.minuteLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = wasted,
                    onValueChange = { if (it.all(Char::isDigit) && it.length <= 4) wasted = it },
                    label = { Text("${strings.wastedTime} (${strings.minutesShort})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        record.copy(
                            wakeUpHour = wakeH.toIntOrNull()?.coerceIn(0, 23),
                            wakeUpMinute = wakeM.toIntOrNull()?.coerceIn(0, 59) ?: 0,
                            sleepHour = sleepH.toIntOrNull()?.coerceIn(0, 23),
                            sleepMinute = sleepM.toIntOrNull()?.coerceIn(0, 59) ?: 0,
                            wastedMinutes = wasted.toIntOrNull()?.coerceIn(0, 1440) ?: 0
                        )
                    )
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
