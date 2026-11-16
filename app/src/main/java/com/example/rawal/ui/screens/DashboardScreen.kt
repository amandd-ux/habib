package com.example.rawal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rawal.data.local.entity.ActivityItemEntity
import com.example.rawal.data.local.entity.CategoryEntity
import com.example.rawal.data.local.entity.DailyRecordEntity
import com.example.rawal.domain.analytics.CategoryDurationStat
import com.example.rawal.domain.calendar.RawalDate
import com.example.rawal.domain.calendar.ScheduleHelper
import com.example.rawal.domain.i18n.AppLanguage
import com.example.rawal.domain.i18n.AppStringsProvider
import com.example.rawal.domain.scoring.DailyScoringEngine
import com.example.rawal.ui.RawalUiState
import com.example.rawal.ui.components.EmptyStateCard
import com.example.rawal.ui.components.RawalDonutChart
import com.example.rawal.ui.components.SectionCard
import com.example.rawal.ui.components.categoryIconForKey
import com.example.rawal.ui.theme.ScoreHighColor
import com.example.rawal.ui.theme.ScoreLowColor
import com.example.rawal.ui.theme.ScoreMediumColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    state: RawalUiState,
    onSelectDate: (String) -> Unit,
    onOpenDailySetup: () -> Unit,
    onOpenAddActivity: () -> Unit,
    onEditActivity: (ActivityItemEntity) -> Unit,
    onToggleActivity: (ActivityItemEntity) -> Unit,
    onDeleteActivity: (ActivityItemEntity) -> Unit,
    onOpenEndOfDayReview: () -> Unit,
    onOpenSleepWastedDialog: () -> Unit
) {
    val strings = state.strings
    val lang = state.language
    val selectedDate = state.selectedDashboardDate
    val parsedDate = remember(selectedDate) { RawalDate.parseIso(selectedDate) ?: RawalDate.today() }
    val dayOfWeekIdx = parsedDate.rawalDayOfWeekIndex()
    val dayName = AppStringsProvider.dayName(lang, dayOfWeekIdx)
    val schedule = remember(parsedDate) { ScheduleHelper.getScheduleForDate(parsedDate) }

    val dayActivities = remember(state.activities, selectedDate) {
        state.activities.filter { it.date == selectedDate }
    }
    val dayRecord = remember(state.records, selectedDate) {
        state.records.firstOrNull { it.date == selectedDate }
    }
    val breakdown = remember(dayRecord, dayActivities) {
        DailyScoringEngine.calculate(dayRecord, dayActivities)
    }

    val completedCount = dayActivities.count { it.isCompleted }
    val incompleteCount = dayActivities.count { !it.isCompleted }
    val totalCount = dayActivities.size
    val completionPct = if (totalCount > 0) (completedCount * 100) / totalCount else 0
    val totalDurationMin = dayActivities.filter { it.isCompleted }.sumOf { it.durationMinutes }
    val avgDurationMin = if (completedCount > 0) totalDurationMin / completedCount else 0
    val sleepMinutes = RawalDate.calculateSleepMinutes(
        dayRecord?.wakeUpHour, dayRecord?.wakeUpMinute,
        dayRecord?.sleepHour, dayRecord?.sleepMinute
    )

    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }
    val categoryStats = remember(dayActivities, catMap) {
        dayActivities.filter { it.isCompleted && it.durationMinutes > 0 }
            .groupBy { it.categoryId }
            .map { (cid, list) ->
                val cat = catMap[cid]
                CategoryDurationStat(
                    categoryId = cid,
                    nameEn = cat?.nameEn ?: "Other",
                    nameFa = cat?.nameFa ?: "سایر",
                    colorHex = cat?.colorHex ?: 0xFF475569,
                    totalMinutes = list.sumOf { it.durationMinutes },
                    completedCount = list.size
                )
            }
            .sortedByDescending { it.totalMinutes }
    }

    var filterTab by remember { mutableIntStateOf(0) } // 0=All, 1=Remaining/Incomplete, 2=Completed
    val filteredActivities = remember(dayActivities, filterTab) {
        when (filterTab) {
            1 -> dayActivities.filter { !it.isCompleted }
            2 -> dayActivities.filter { it.isCompleted }
            else -> dayActivities
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date Header + Navigation
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { onSelectDate(parsedDate.minusDays(1).toIso()) },
                        modifier = Modifier.testTag("dashboard_prev_day")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = strings.back)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = AppStringsProvider.formatDate(lang, selectedDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onSelectDate(parsedDate.plusDays(1).toIso()) },
                        modifier = Modifier.testTag("dashboard_next_day")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = strings.next)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenDailySetup,
                        modifier = Modifier.weight(1f).testTag("daily_setup_button")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (dayRecord?.startupCompleted == true) strings.resumeDailySetup else strings.startDailySetup,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenEndOfDayReview,
                        modifier = Modifier.weight(1f).testTag("eod_review_button")
                    ) {
                        Icon(Icons.Rounded.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.endOfDayReview, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Availability Schedule Banner (Section 9)
        if (state.settings.showScheduleBanner) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = strings.scheduleBannerTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val scheduleText = if (schedule.isSchoolDay && schedule.unavailableStartHour != null) {
                                val startStr = AppStringsProvider.formatTime(lang, schedule.unavailableStartHour, schedule.unavailableStartMinute)
                                val endStr = AppStringsProvider.formatTime(lang, schedule.unavailableEndHour, schedule.unavailableEndMinute)
                                String.format(strings.scheduleUnavailableFormat, startStr, endStr)
                            } else {
                                strings.scheduleAvailableAllDay
                            }
                            Text(
                                text = scheduleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Daily Score + Completion Overview Card
        item {
            val scoreColor = when {
                breakdown.totalScore >= 75 -> ScoreHighColor
                breakdown.totalScore >= 50 -> ScoreMediumColor
                else -> ScoreLowColor
            }
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.dailyScore,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = AppStringsProvider.toLocalizedDigits(breakdown.totalScore.toString(), lang),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor,
                                modifier = Modifier.testTag("dashboard_daily_score")
                            )
                            Text(
                                text = " / ${AppStringsProvider.toLocalizedDigits("100", lang)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (completionPct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${strings.completionRate}: ${AppStringsProvider.toLocalizedDigits(completionPct.toString(), lang)}% (${AppStringsProvider.toLocalizedDigits(completedCount.toString(), lang)}/${AppStringsProvider.toLocalizedDigits(totalCount.toString(), lang)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    // Satisfaction badge
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onOpenEndOfDayReview() }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (dayRecord?.satisfaction == "NOT_SATISFIED") {
                                    Icons.Rounded.SentimentDissatisfied
                                } else {
                                    Icons.Rounded.SentimentSatisfied
                                },
                                contentDescription = strings.dailySatisfaction,
                                tint = when (dayRecord?.satisfaction) {
                                    "SATISFIED" -> ScoreHighColor
                                    "NOT_SATISFIED" -> ScoreLowColor
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = when (dayRecord?.satisfaction) {
                                    "SATISFIED" -> strings.satisfied
                                    "NOT_SATISFIED" -> strings.notSatisfied
                                    else -> strings.notReviewedYet
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                if (state.settings.showScoreBreakdown) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScoreChip(strings.scoreCompletionPart, breakdown.completionPoints, 60, lang)
                        ScoreChip(strings.scoreConsistencyPart, breakdown.consistencyPoints, 15, lang)
                        ScoreChip(strings.scoreTimePart, breakdown.timeAwarenessPoints, 15, lang)
                        ScoreChip(strings.scoreSatisfactionPart, breakdown.satisfactionPoints, 10, lang)
                    }
                }
            }
        }

        // Sleep, Wake-Up, Total Activity, Wasted Time Grid
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.editDayMetrics,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButtonLabel(text = strings.edit, onClick = onOpenSleepWastedDialog)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniMetricBox(
                        icon = Icons.Rounded.WbSunny,
                        label = strings.wakeUpTime,
                        value = AppStringsProvider.formatTime(lang, dayRecord?.wakeUpHour, dayRecord?.wakeUpMinute),
                        modifier = Modifier.weight(1f)
                    )
                    MiniMetricBox(
                        icon = Icons.Rounded.Bedtime,
                        label = strings.bedtime,
                        value = AppStringsProvider.formatTime(lang, dayRecord?.sleepHour, dayRecord?.sleepMinute),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniMetricBox(
                        icon = Icons.Rounded.Schedule,
                        label = strings.totalActivityTime,
                        value = AppStringsProvider.formatDuration(lang, totalDurationMin),
                        modifier = Modifier.weight(1f)
                    )
                    MiniMetricBox(
                        icon = Icons.Rounded.HourglassBottom,
                        label = strings.wastedTime,
                        value = AppStringsProvider.formatDuration(lang, dayRecord?.wastedMinutes ?: 0),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (sleepMinutes != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${strings.sleepDuration}: ${AppStringsProvider.formatDuration(lang, sleepMinutes)}  •  ${strings.avgActivityDuration}: ${AppStringsProvider.formatDuration(lang, avgDurationMin)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Digital-Wellbeing-Inspired Manual Time Summary (Section 11)
        if (state.settings.showTimeSummaryCard && categoryStats.isNotEmpty()) {
            item {
                SectionCard {
                    Text(
                        text = strings.digitalWellbeingTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = strings.digitalWellbeingDisclaimer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    RawalDonutChart(
                        items = categoryStats,
                        language = lang,
                        centerLabel = strings.totalActivityTime
                    )
                }
            }
        }

        // Activities Section Header + Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.navActivities,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onOpenAddActivity,
                    modifier = Modifier.testTag("dashboard_add_activity_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.addActivity)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterTab == 0,
                    onClick = { filterTab = 0 },
                    label = { Text("${strings.all} (${AppStringsProvider.toLocalizedDigits(totalCount.toString(), lang)})") }
                )
                FilterChip(
                    selected = filterTab == 1,
                    onClick = { filterTab = 1 },
                    label = { Text("${strings.remaining} (${AppStringsProvider.toLocalizedDigits(incompleteCount.toString(), lang)})") }
                )
                FilterChip(
                    selected = filterTab == 2,
                    onClick = { filterTab = 2 },
                    label = { Text("${strings.completed} (${AppStringsProvider.toLocalizedDigits(completedCount.toString(), lang)})") }
                )
            }
        }

        if (filteredActivities.isEmpty()) {
            item {
                EmptyStateCard(
                    title = strings.noRecordsYet,
                    tip = strings.emptyStateTip,
                    actionLabel = strings.addActivity,
                    onAction = onOpenAddActivity
                )
            }
        } else {
            items(filteredActivities, key = { it.id }) { activity ->
                val cat = catMap[activity.categoryId]
                ActivityCard(
                    activity = activity,
                    category = cat,
                    language = lang,
                    compact = state.settings.compactActivityCards,
                    onToggle = { onToggleActivity(activity) },
                    onEdit = { onEditActivity(activity) },
                    onDelete = { onDeleteActivity(activity) }
                )
            }
        }
    }
}

@Composable
private fun ScoreChip(label: String, points: Int, maxPoints: Int, lang: AppLanguage) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "$label ${AppStringsProvider.toLocalizedDigits("$points/$maxPoints", lang)}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun MiniMetricBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TextButtonLabel(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ActivityCard(
    activity: ActivityItemEntity,
    category: CategoryEntity?,
    language: AppLanguage,
    compact: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val catColor = Color(category?.colorHex ?: 0xFF475569)
    val catName = if (language == AppLanguage.FA) category?.nameFa ?: "سایر" else category?.nameEn ?: "Other"
    SectionCard(
        modifier = Modifier.testTag("activity_card_${activity.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.testTag("toggle_activity_${activity.id}")
            ) {
                Icon(
                    imageVector = if (activity.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = activity.title,
                    tint = if (activity.isCompleted) ScoreHighColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (activity.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (activity.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = catColor.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = categoryIconForKey(category?.iconKey ?: "category"),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(catName, style = MaterialTheme.typography.labelSmall, color = catColor)
                        }
                    }
                    Text(
                        text = AppStringsProvider.formatDuration(language, activity.durationMinutes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!compact && activity.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = activity.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("edit_activity_${activity.id}")
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_activity_${activity.id}")
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
