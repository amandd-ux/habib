package com.example.rawal.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rawal.data.local.entity.ActivityItemEntity
import com.example.rawal.domain.calendar.RawalDate
import com.example.rawal.domain.i18n.AppLanguage
import com.example.rawal.domain.i18n.AppStringsProvider
import com.example.rawal.ui.RawalUiState
import com.example.rawal.ui.components.ConfirmationDialog
import com.example.rawal.ui.components.EmptyStateCard
import com.example.rawal.ui.components.SectionCard
import com.example.rawal.ui.theme.ScoreHighColor
import com.example.rawal.ui.theme.ScoreLowColor
import com.example.rawal.ui.theme.ScoreMediumColor

@Composable
fun CalendarScreen(
    state: RawalUiState,
    onOpenDayInDashboard: (String) -> Unit,
    onEditActivity: (ActivityItemEntity) -> Unit,
    onToggleActivity: (ActivityItemEntity) -> Unit,
    onDeleteActivity: (ActivityItemEntity) -> Unit,
    onDeleteDay: (String) -> Unit
) {
    val strings = state.strings
    val lang = state.language
    var monthAnchor by remember { mutableStateOf(RawalDate.today().monthStart()) }
    var selectedDateIso by remember { mutableStateOf(state.selectedDashboardDate) }
    var showDeleteDayConfirm by remember { mutableStateOf(false) }

    val recordsByDate = remember(state.records) { state.records.associateBy { it.date } }
    val activitiesByDate = remember(state.activities) { state.activities.groupBy { it.date } }
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }

    val firstDayOfMonth = monthAnchor.monthStart()
    val leadingEmptySlots = firstDayOfMonth.rawalDayOfWeekIndex() // Saturday = 0 .. Friday = 6
    val totalDaysInMonth = firstDayOfMonth.daysInMonth()

    val selectedRecord = recordsByDate[selectedDateIso]
    val selectedActivities = activitiesByDate[selectedDateIso].orEmpty()
    val hasRecordedData = selectedActivities.isNotEmpty() ||
        selectedRecord?.startupCompleted == true ||
        selectedRecord?.reviewCompleted == true

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { monthAnchor = monthAnchor.plusMonths(-1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = strings.back)
                    }
                    Text(
                        text = AppStringsProvider.monthHeader(lang, monthAnchor),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { monthAnchor = monthAnchor.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = strings.next)
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Day-of-week header (Sat..Fri)
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dow in 0..6) {
                        Text(
                            text = AppStringsProvider.dayName(lang, dow).take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                // Month Grid
                val totalCells = leadingEmptySlots + totalDaysInMonth
                val rows = (totalCells + 6) / 7
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (c in 0 until 7) {
                                val cellIndex = r * 7 + c
                                val dayNum = cellIndex - leadingEmptySlots + 1
                                if (dayNum in 1..totalDaysInMonth) {
                                    val cellDate = RawalDate(monthAnchor.year, monthAnchor.month, dayNum)
                                    val iso = cellDate.toIso()
                                    val rec = recordsByDate[iso]
                                    val acts = activitiesByDate[iso].orEmpty()
                                    val hasData = acts.isNotEmpty() || rec?.startupCompleted == true || rec?.reviewCompleted == true
                                    val isSelected = iso == selectedDateIso

                                    val cellColor = when {
                                        !hasData -> Color.Transparent
                                        (rec?.calculatedScore ?: 0) >= 75 -> ScoreHighColor.copy(alpha = 0.22f)
                                        (rec?.calculatedScore ?: 0) >= 50 -> ScoreMediumColor.copy(alpha = 0.22f)
                                        else -> ScoreLowColor.copy(alpha = 0.20f)
                                    }
                                    val border = if (isSelected) {
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    } else null

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clickable { selectedDateIso = iso },
                                        shape = RoundedCornerShape(10.dp),
                                        color = cellColor,
                                        border = border
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = AppStringsProvider.formatDayNumber(lang, cellDate),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            if (hasData) {
                                                Text(
                                                    text = AppStringsProvider.toLocalizedDigits((rec?.calculatedScore ?: 0).toString(), lang),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                // Color Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendDot(Color.Transparent, strings.calendarLegendEmpty, bordered = true)
                    LegendDot(ScoreLowColor.copy(alpha = 0.4f), AppStringsProvider.toLocalizedDigits(strings.calendarLegendLow, lang))
                    LegendDot(ScoreMediumColor.copy(alpha = 0.4f), AppStringsProvider.toLocalizedDigits(strings.calendarLegendMedium, lang))
                    LegendDot(ScoreHighColor.copy(alpha = 0.4f), AppStringsProvider.toLocalizedDigits(strings.calendarLegendHigh, lang))
                }
            }
        }

        // Selected Day Details
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(strings.selectedDayDetails, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = AppStringsProvider.formatDate(lang, selectedDateIso),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onOpenDayInDashboard(selectedDateIso) }) {
                            Icon(Icons.Rounded.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.edit)
                        }
                        if (hasRecordedData) {
                            IconButton(
                                onClick = { showDeleteDayConfirm = true },
                                modifier = Modifier.testTag("calendar_delete_day")
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.deleteDayData, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (!hasRecordedData) {
                    EmptyStateCard(
                        title = strings.calendarLegendEmpty,
                        tip = strings.noRecordsForSelectedDay,
                        actionLabel = strings.edit,
                        onAction = { onOpenDayInDashboard(selectedDateIso) }
                    )
                } else {
                    val rec = selectedRecord
                    val scoreStr = AppStringsProvider.toLocalizedDigits((rec?.calculatedScore ?: 0).toString(), lang)
                    val satStr = when (rec?.satisfaction) {
                        "SATISFIED" -> strings.satisfied
                        "NOT_SATISFIED" -> strings.notSatisfied
                        else -> strings.notReviewedYet
                    }
                    val completedList = selectedActivities.filter { it.isCompleted }
                    val incompleteList = selectedActivities.filter { !it.isCompleted }

                    Text(
                        text = "${strings.dailyScore}: $scoreStr/100  •  ${strings.dailySatisfaction}: $satStr",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${strings.wakeUpTime}: ${AppStringsProvider.formatTime(lang, rec?.wakeUpHour, rec?.wakeUpMinute)}  •  ${strings.bedtime}: ${AppStringsProvider.formatTime(lang, rec?.sleepHour, rec?.sleepMinute)}  •  ${strings.wastedTime}: ${AppStringsProvider.formatDuration(lang, rec?.wastedMinutes ?: 0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${strings.completed}: ${AppStringsProvider.toLocalizedDigits(completedList.size.toString(), lang)}  •  ${strings.incomplete}: ${AppStringsProvider.toLocalizedDigits(incompleteList.size.toString(), lang)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!rec?.unexpectedEvents.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("${strings.unexpectedEventsLabel}: ${rec?.unexpectedEvents}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (!rec?.disruptions.isNullOrBlank()) {
                        Text("${strings.disruptionsLabel}: ${rec?.disruptions}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (!rec?.dailyNotes.isNullOrBlank()) {
                        Text("${strings.dailyNotesLabel}: ${rec?.dailyNotes}", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectedActivities.forEach { act ->
                            ActivityCard(
                                activity = act,
                                category = catMap[act.categoryId],
                                language = lang,
                                compact = true,
                                onToggle = { onToggleActivity(act) },
                                onEdit = { onEditActivity(act) },
                                onDelete = { onDeleteActivity(act) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDayConfirm) {
        ConfirmationDialog(
            title = strings.deleteDayConfirmTitle,
            body = strings.deleteDayConfirmBody,
            confirmText = strings.delete,
            cancelText = strings.cancel,
            onConfirm = {
                onDeleteDay(selectedDateIso)
                showDeleteDayConfirm = false
            },
            onDismiss = { showDeleteDayConfirm = false }
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String, bordered: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = CircleShape,
            color = color,
            border = if (bordered) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
        ) {}
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
