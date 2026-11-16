package com.example.rawal.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.rawal.data.local.entity.WeeklyGoalEntity
import com.example.rawal.domain.analytics.AnalyticsEngine
import com.example.rawal.domain.calendar.RawalDate
import com.example.rawal.domain.i18n.AppLanguage
import com.example.rawal.domain.i18n.AppStringsProvider
import com.example.rawal.domain.scoring.AchievementCatalog
import com.example.rawal.ui.RawalUiState
import com.example.rawal.ui.components.EmptyStateCard
import com.example.rawal.ui.components.LineChartPoint
import com.example.rawal.ui.components.RawalLineChart
import com.example.rawal.ui.components.SectionCard
import com.example.rawal.ui.theme.ScoreHighColor
import kotlin.math.roundToInt

enum class ProgressSubTab { GOALS, ACHIEVEMENTS_STREAKS, OVERALL_JOURNEY }

@Composable
fun GoalsAchievementsScreen(
    state: RawalUiState,
    initialTab: ProgressSubTab = ProgressSubTab.GOALS,
    onSaveGoal: (WeeklyGoalEntity) -> Unit,
    onDeleteGoal: (Long) -> Unit
) {
    val strings = state.strings
    val lang = state.language
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    val unlockedKeys = remember(state.unlockedAchievements) {
        state.unlockedAchievements.associateBy { it.achievementKey }
    }

    // Monthly average score trend for Overall Progress
    val monthlyPoints = remember(state.records, state.activities, lang) {
        val activeDates = state.records.filter { rec ->
            rec.startupCompleted || rec.reviewCompleted || state.activities.any { it.date == rec.date }
        }
        activeDates.groupBy { it.date.take(7) } // YYYY-MM
            .toSortedMap()
            .map { (ym, list) ->
                val avg = list.map { it.calculatedScore }.average().toFloat()
                LineChartPoint(ym, avg)
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedTab == ProgressSubTab.GOALS,
                        onClick = { selectedTab = ProgressSubTab.GOALS },
                        label = { Text(strings.navGoals) },
                        modifier = Modifier.testTag("tab_goals")
                    )
                    FilterChip(
                        selected = selectedTab == ProgressSubTab.ACHIEVEMENTS_STREAKS,
                        onClick = { selectedTab = ProgressSubTab.ACHIEVEMENTS_STREAKS },
                        label = { Text(strings.navAchievements) },
                        modifier = Modifier.testTag("tab_achievements")
                    )
                    FilterChip(
                        selected = selectedTab == ProgressSubTab.OVERALL_JOURNEY,
                        onClick = { selectedTab = ProgressSubTab.OVERALL_JOURNEY },
                        label = { Text(strings.navOverallProgress) },
                        modifier = Modifier.testTag("tab_overall")
                    )
                }
            }
        }

        when (selectedTab) {
            ProgressSubTab.GOALS -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.weeklyGoalsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { showAddGoalDialog = true },
                            modifier = Modifier.testTag("add_goal_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.addGoal)
                        }
                    }
                }
                if (state.goalProgresses.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = strings.weeklyGoalsTitle,
                            tip = strings.emptyStateTip,
                            icon = Icons.Rounded.TrackChanges,
                            actionLabel = strings.addGoal,
                            onAction = { showAddGoalDialog = true }
                        )
                    }
                } else {
                    items(state.goalProgresses, key = { it.goal.id }) { gp ->
                        val pct = (gp.progressFraction * 100f).roundToInt()
                        val unitLabel = if (gp.goal.metricType == "SESSIONS") strings.sessions else strings.minutesShort
                        SectionCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(gp.goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${AppStringsProvider.formatDate(lang, gp.goal.weekStartDate)} • ${AppStringsProvider.toLocalizedDigits("${gp.currentValue}/${gp.goal.targetValue}", lang)} $unitLabel (${AppStringsProvider.toLocalizedDigits(pct.toString(), lang)}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteGoal(gp.goal.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { gp.progressFraction },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                            )
                        }
                    }
                }
            }

            ProgressSubTab.ACHIEVEMENTS_STREAKS -> {
                // Streaks Card
                item {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(strings.streakHistory, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(strings.currentStreak, style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "${AppStringsProvider.toLocalizedDigits(state.streakSummary.currentStreak.toString(), lang)} ${strings.days}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(strings.longestStreak, style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "${AppStringsProvider.toLocalizedDigits(state.streakSummary.longestStreak.toString(), lang)} ${strings.days}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${strings.streakThresholdLabel}: ${AppStringsProvider.toLocalizedDigits(state.streakSummary.threshold.toString(), lang)}/100",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.streakSummary.history.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            state.streakSummary.history.take(5).forEach { run ->
                                Text(
                                    text = "• ${AppStringsProvider.formatShortDate(lang, run.startDate)} – ${AppStringsProvider.formatShortDate(lang, run.endDate)}: ${AppStringsProvider.toLocalizedDigits(run.lengthDays.toString(), lang)} ${strings.days}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Personal Records
                item {
                    val rec = state.personalRecords
                    SectionCard {
                        Text(strings.personalRecordsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        RecordRow(strings.bestDay, rec.bestDayDate?.let { "${AppStringsProvider.formatDate(lang, it)} (${AppStringsProvider.toLocalizedDigits(rec.bestDayScore.toString(), lang)}/100)" } ?: strings.notRecorded)
                        RecordRow(strings.bestWeekLabel, rec.bestWeekStart?.let { "${AppStringsProvider.formatDate(lang, it)} (${AppStringsProvider.toLocalizedDigits(rec.bestWeekAvgScore.roundToInt().toString(), lang)}/100)" } ?: strings.notRecorded)
                        RecordRow(strings.longestReadingLabel, if (rec.longestReadingMinutes > 0) AppStringsProvider.formatDuration(lang, rec.longestReadingMinutes) else strings.notRecorded)
                        RecordRow(strings.longestActivityLabel, rec.longestActivityTitle?.let { "$it (${AppStringsProvider.formatDuration(lang, rec.longestActivityMinutes)})" } ?: strings.notRecorded)
                        RecordRow(strings.longestStreak, "${AppStringsProvider.toLocalizedDigits(rec.longestStreakDays.toString(), lang)} ${strings.days}")

                        if (rec.categoryBests.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(strings.categoryBestsLabel, style = MaterialTheme.typography.labelLarge)
                            rec.categoryBests.forEach { cb ->
                                val name = if (lang == AppLanguage.FA) cb.nameFa else cb.nameEn
                                Text(
                                    text = "• $name: ${AppStringsProvider.formatDuration(lang, cb.bestSingleDayMinutes)} (${AppStringsProvider.formatShortDate(lang, cb.bestDayDate)})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Achievements Badges
                item {
                    Text(strings.achievementsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(AchievementCatalog.all, key = { it.key }) { def ->
                    val unlocked = unlockedKeys[def.key]
                    val isUnlocked = unlocked != null
                    SectionCard(
                        containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = if (isUnlocked) ScoreHighColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isUnlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = if (isUnlocked) ScoreHighColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == AppLanguage.FA) def.titleFa else def.titleEn,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (lang == AppLanguage.FA) def.descFa else def.descEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (isUnlocked) strings.unlockedLabel else strings.lockedLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUnlocked) ScoreHighColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            ProgressSubTab.OVERALL_JOURNEY -> {
                item {
                    SectionCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(MaterialTheme.shapes.small)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_journey_header_1794823746880),
                                contentDescription = strings.overallJourneyTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                        )
                                    )
                                    .padding(14.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Text(
                                    text = strings.overallJourneyTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        val activeRecords = state.records.filter { rec ->
                            rec.startupCompleted || rec.reviewCompleted || state.activities.any { it.date == rec.date }
                        }
                        val avgScore = if (activeRecords.isNotEmpty()) activeRecords.map { it.calculatedScore }.average().roundToInt() else 0
                        val reviewed = activeRecords.filter { it.satisfaction != null }
                        val satPct = if (reviewed.isNotEmpty()) (reviewed.count { it.satisfaction == "SATISFIED" } * 100) / reviewed.size else null
                        val totalMinutes = state.activities.filter { it.isCompleted }.sumOf { it.durationMinutes }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            JourneyStat(strings.totalRecordedDays, AppStringsProvider.toLocalizedDigits(activeRecords.size.toString(), lang), Modifier.weight(1f))
                            JourneyStat(strings.avgDailyScore, AppStringsProvider.toLocalizedDigits(avgScore.toString(), lang), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            JourneyStat(strings.avgSatisfaction, satPct?.let { "${AppStringsProvider.toLocalizedDigits(it.toString(), lang)}%" } ?: strings.notRecorded, Modifier.weight(1f))
                            JourneyStat(strings.totalActivityTime, AppStringsProvider.formatDuration(lang, totalMinutes), Modifier.weight(1f))
                        }
                    }
                }

                if (monthlyPoints.isNotEmpty()) {
                    item {
                        SectionCard {
                            Text(strings.monthlyChangesTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            RawalLineChart(
                                points = monthlyPoints,
                                lineColor = MaterialTheme.colorScheme.primary,
                                maxValue = 100f,
                                language = lang
                            )
                        }
                    }
                } else {
                    item {
                        EmptyStateCard(title = strings.noRecordsYet, tip = strings.emptyStateTip)
                    }
                }
            }
        }
    }

    if (showAddGoalDialog) {
        AddWeeklyGoalDialog(
            state = state,
            onSave = {
                onSaveGoal(it)
                showAddGoalDialog = false
            },
            onDismiss = { showAddGoalDialog = false }
        )
    }
}

@Composable
private fun RecordRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun JourneyStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddWeeklyGoalDialog(
    state: RawalUiState,
    onSave: (WeeklyGoalEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = state.strings
    val lang = state.language
    var title by remember { mutableStateOf("") }
    var targetType by remember { mutableStateOf("CATEGORY") }
    var selectedCatId by remember { mutableStateOf(state.categories.firstOrNull()?.id ?: 0L) }
    var activityMatch by remember { mutableStateOf("") }
    var metricType by remember { mutableStateOf("MINUTES") }
    var targetValueText by remember { mutableStateOf("180") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 520.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(strings.addGoal) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text(strings.goalTitleLabel) },
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { m -> { Text(m) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_title_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetType == "CATEGORY",
                        onClick = { targetType = "CATEGORY" },
                        label = { Text(strings.goalTypeCategory) }
                    )
                    FilterChip(
                        selected = targetType == "ACTIVITY",
                        onClick = { targetType = "ACTIVITY" },
                        label = { Text(strings.goalTypeActivity) }
                    )
                }

                if (targetType == "CATEGORY") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.categories.forEach { cat ->
                            val name = if (lang == AppLanguage.FA) cat.nameFa else cat.nameEn
                            FilterChip(
                                selected = selectedCatId == cat.id,
                                onClick = { selectedCatId = cat.id },
                                label = { Text(name) }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = activityMatch,
                        onValueChange = { activityMatch = it },
                        label = { Text(strings.taskNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = metricType == "MINUTES",
                        onClick = { metricType = "MINUTES" },
                        label = { Text(strings.goalMetricMinutes) }
                    )
                    FilterChip(
                        selected = metricType == "SESSIONS",
                        onClick = { metricType = "SESSIONS" },
                        label = { Text(strings.goalMetricSessions) }
                    )
                }

                OutlinedTextField(
                    value = targetValueText,
                    onValueChange = { if (it.all(Char::isDigit) && it.length <= 5) targetValueText = it },
                    label = { Text(strings.targetValueLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_target_input")
                )
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag("save_goal_button"),
                onClick = {
                    if (title.trim().isEmpty()) {
                        errorMsg = strings.validationRequiredField
                        return@Button
                    }
                    onSave(
                        WeeklyGoalEntity(
                            profileId = state.activeProfile?.id ?: 1L,
                            title = title.trim(),
                            targetType = targetType,
                            categoryId = if (targetType == "CATEGORY") selectedCatId else null,
                            activityTitleMatch = if (targetType == "ACTIVITY") activityMatch.trim() else null,
                            metricType = metricType,
                            targetValue = targetValueText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            weekStartDate = RawalDate.today().weekStartSaturday().toIso()
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
