package com.example.rawal.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rawal.data.local.entity.WeeklyReviewEntity
import com.example.rawal.domain.analytics.AnalyticsEngine
import com.example.rawal.domain.analytics.ReportPeriod
import com.example.rawal.domain.calendar.RawalDate
import com.example.rawal.domain.i18n.AppLanguage
import com.example.rawal.domain.i18n.AppStringsProvider
import com.example.rawal.ui.RawalUiState
import com.example.rawal.ui.components.BarChartEntry
import com.example.rawal.ui.components.EmptyStateCard
import com.example.rawal.ui.components.LineChartPoint
import com.example.rawal.ui.components.RawalBarChart
import com.example.rawal.ui.components.RawalDonutChart
import com.example.rawal.ui.components.RawalLineChart
import com.example.rawal.ui.components.SectionCard
import com.example.rawal.ui.theme.ScoreLowColor
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    state: RawalUiState,
    onSaveWeeklyReview: (WeeklyReviewEntity) -> Unit
) {
    val strings = state.strings
    val lang = state.language
    val today = remember { RawalDate.today() }
    val currentWeekStart = remember(today) { today.weekStartSaturday() }

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.WEEKLY) }
    var selectedWeekStart by remember { mutableStateOf(currentWeekStart.toIso()) }
    var compareWeekStart by remember { mutableStateOf<String?>(null) }
    var customStart by remember { mutableStateOf(today.minusDays(14).toIso()) }
    var customEnd by remember { mutableStateOf(today.toIso()) }

    val (periodStart, periodEnd) = remember(selectedPeriod, selectedWeekStart, customStart, customEnd, today) {
        when (selectedPeriod) {
            ReportPeriod.DAILY -> state.selectedDashboardDate to state.selectedDashboardDate
            ReportPeriod.WEEKLY -> {
                val ws = RawalDate.parseIso(selectedWeekStart) ?: currentWeekStart
                ws.toIso() to ws.plusDays(6).toIso()
            }
            ReportPeriod.MONTHLY -> {
                val ms = today.monthStart()
                ms.toIso() to RawalDate(ms.year, ms.month, ms.daysInMonth()).toIso()
            }
            ReportPeriod.YEARLY -> "${today.year}-01-01" to "${today.year}-12-31"
            ReportPeriod.CUSTOM -> {
                val s = RawalDate.parseIso(customStart)?.toIso() ?: today.minusDays(14).toIso()
                val e = RawalDate.parseIso(customEnd)?.toIso() ?: today.toIso()
                if (s <= e) s to e else e to s
            }
        }
    }

    val report = remember(periodStart, periodEnd, state.records, state.activities, state.categories) {
        AnalyticsEngine.buildPeriodReport(
            startDate = periodStart,
            endDate = periodEnd,
            records = state.records,
            activities = state.activities,
            categories = state.categories
        )
    }

    val prevWeekStartIso = remember(selectedWeekStart) {
        (RawalDate.parseIso(selectedWeekStart) ?: currentWeekStart).minusDays(7).toIso()
    }
    val prevWeekEndIso = remember(prevWeekStartIso) {
        (RawalDate.parseIso(prevWeekStartIso) ?: currentWeekStart).plusDays(6).toIso()
    }
    val prevWeekReport = remember(prevWeekStartIso, prevWeekEndIso, state.records, state.activities, state.categories) {
        AnalyticsEngine.buildPeriodReport(
            startDate = prevWeekStartIso,
            endDate = prevWeekEndIso,
            records = state.records,
            activities = state.activities,
            categories = state.categories
        )
    }

    val weekActivities = remember(periodStart, periodEnd, state.activities) {
        state.activities.filter { it.date in periodStart..periodEnd }
    }
    val weeklyAnalysis = remember(report, prevWeekReport, weekActivities) {
        AnalyticsEngine.generateWeeklyTextAnalysis(report, prevWeekReport, weekActivities)
    }

    // Actual archived weeks computed strictly from real recorded dates + current week
    val availableWeeks = remember(state.records, state.activities, currentWeekStart) {
        val fromRecords = state.records.mapNotNull { RawalDate.parseIso(it.date)?.weekStartSaturday()?.toIso() }
        val fromActs = state.activities.mapNotNull { RawalDate.parseIso(it.date)?.weekStartSaturday()?.toIso() }
        (fromRecords + fromActs + currentWeekStart.toIso()).distinct().sortedDescending()
    }

    val comparedReport = remember(compareWeekStart, state.records, state.activities, state.categories) {
        val ws = compareWeekStart?.let { RawalDate.parseIso(it) } ?: return@remember null
        AnalyticsEngine.buildPeriodReport(
            startDate = ws.toIso(),
            endDate = ws.plusDays(6).toIso(),
            records = state.records,
            activities = state.activities,
            categories = state.categories
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Period Selector
        item {
            SectionCard {
                Text(strings.navReports, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        ReportPeriod.DAILY to strings.periodDaily,
                        ReportPeriod.WEEKLY to strings.periodWeekly,
                        ReportPeriod.MONTHLY to strings.periodMonthly,
                        ReportPeriod.YEARLY to strings.periodYearly,
                        ReportPeriod.CUSTOM to strings.periodCustom
                    ).forEach { (period, label) ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(label) }
                        )
                    }
                }

                if (selectedPeriod == ReportPeriod.CUSTOM) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customStart,
                            onValueChange = { customStart = it },
                            label = { Text(strings.startDateLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customEnd,
                            onValueChange = { customEnd = it },
                            label = { Text(strings.endDateLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (selectedPeriod == ReportPeriod.WEEKLY) {
                    Spacer(Modifier.height(8.dp))
                    Text(strings.weeklyArchiveTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableWeeks.forEach { ws ->
                            FilterChip(
                                selected = selectedWeekStart == ws,
                                onClick = { selectedWeekStart = ws },
                                label = { Text(AppStringsProvider.formatDate(lang, ws)) }
                            )
                        }
                    }
                }
            }
        }

        if (report.recordedDaysCount == 0 && report.totalActivities == 0) {
            item {
                EmptyStateCard(
                    title = strings.noDataForPeriod,
                    tip = strings.emptyStateTip
                )
            }
        } else {
            // Key Statistics
            item {
                SectionCard {
                    Text(
                        text = "${strings.weeklyStatsTitle} (${AppStringsProvider.formatShortDate(lang, periodStart)} – ${AppStringsProvider.formatShortDate(lang, periodEnd)})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatMini(strings.totalWeeklyActivities, AppStringsProvider.toLocalizedDigits(report.totalActivities.toString(), lang), Modifier.weight(1f))
                        StatMini(strings.completed, AppStringsProvider.toLocalizedDigits(report.completedActivities.toString(), lang), Modifier.weight(1f))
                        StatMini(strings.incomplete, AppStringsProvider.toLocalizedDigits(report.incompleteActivities.toString(), lang), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatMini(strings.avgDailyScore, AppStringsProvider.toLocalizedDigits(report.averageScore.roundToInt().toString(), lang), Modifier.weight(1f))
                        StatMini(
                            strings.avgSatisfaction,
                            report.satisfactionRate?.let { "${AppStringsProvider.toLocalizedDigits(it.roundToInt().toString(), lang)}%" } ?: strings.notRecorded,
                            Modifier.weight(1f)
                        )
                        StatMini(
                            strings.avgSleep,
                            report.averageSleepMinutes?.let { AppStringsProvider.formatDuration(lang, it.roundToInt()) } ?: strings.notRecorded,
                            Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatMini(strings.avgActivityDuration, AppStringsProvider.formatDuration(lang, report.averageActivityDurationMinutes.roundToInt()), Modifier.weight(1f))
                        StatMini(strings.avgWastedTime, AppStringsProvider.formatDuration(lang, report.averageWastedMinutes.roundToInt()), Modifier.weight(1f))
                    }
                    report.bestDay?.let { best ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${strings.bestDay}: ${AppStringsProvider.formatDate(lang, best.date)} (${AppStringsProvider.toLocalizedDigits(best.calculatedScore.toString(), lang)}/100)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    report.lowestDay?.let { low ->
                        Text(
                            text = "${strings.lowestDay}: ${AppStringsProvider.formatDate(lang, low.date)} (${AppStringsProvider.toLocalizedDigits(low.calculatedScore.toString(), lang)}/100)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Line Chart: Score Trend
            if (report.dailyPoints.isNotEmpty()) {
                item {
                    SectionCard {
                        Text(strings.scoreTrendChart, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        RawalLineChart(
                            points = report.dailyPoints.map {
                                LineChartPoint(AppStringsProvider.formatShortDate(lang, it.date), it.score.toFloat())
                            },
                            lineColor = MaterialTheme.colorScheme.primary,
                            maxValue = 100f,
                            language = lang
                        )
                    }
                }

                // Bar Chart: Activity vs Wasted Time
                item {
                    SectionCard {
                        Text(strings.activityDurationChart, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        RawalBarChart(
                            entries = report.dailyPoints.map {
                                BarChartEntry(
                                    label = AppStringsProvider.formatShortDate(lang, it.date),
                                    primaryValue = it.completedMinutes.toFloat(),
                                    secondaryValue = it.wastedMinutes.toFloat()
                                )
                            },
                            primaryColor = MaterialTheme.colorScheme.primary,
                            secondaryColor = ScoreLowColor,
                            primaryLegend = strings.totalActivityTime,
                            secondaryLegend = strings.wastedTime,
                            language = lang
                        )
                    }
                }
            }

            // Donut/Pie Chart: Category Distribution
            if (report.categoryBreakdown.isNotEmpty()) {
                item {
                    SectionCard {
                        Text(strings.categoryDistributionChart, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        RawalDonutChart(
                            items = report.categoryBreakdown,
                            language = lang,
                            centerLabel = strings.totalActivityTime
                        )
                    }
                }
            }
        }

        // Weekly Text Analysis & Week Comparison (Section 15 & 17)
        if (selectedPeriod == ReportPeriod.WEEKLY) {
            item {
                SectionCard {
                    Text(strings.weeklyAnalysisTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    if (!weeklyAnalysis.hasSufficientData) {
                        val msg = if (lang == AppLanguage.FA) weeklyAnalysis.suggestionsFa.first() else weeklyAnalysis.suggestionsEn.first()
                        Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val strengths = if (lang == AppLanguage.FA) weeklyAnalysis.strengthsFa else weeklyAnalysis.strengthsEn
                        if (strengths.isNotEmpty()) {
                            Text(strings.weeklyStrengths, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            strengths.forEach { s ->
                                Text("• $s", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(6.dp))
                        }

                        weeklyAnalysis.mostCompletedActivity?.let {
                            Text("${strings.mostCompleted}: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        weeklyAnalysis.leastCompletedActivity?.let {
                            Text("${strings.leastCompleted}: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        weeklyAnalysis.scoreDeltaFromPreviousWeek?.let { delta ->
                            val sign = if (delta >= 0) "+" else ""
                            Text(
                                text = "${strings.vsPreviousWeek}: $sign${AppStringsProvider.toLocalizedDigits(delta.roundToInt().toString(), lang)} pts",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val suggestions = if (lang == AppLanguage.FA) weeklyAnalysis.suggestionsFa else weeklyAnalysis.suggestionsEn
                        if (suggestions.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(strings.nextWeekSuggestions, style = MaterialTheme.typography.labelLarge)
                            suggestions.forEach { sug ->
                                Text("• $sug", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Week-to-week comparison selector
                    if (availableWeeks.size > 1) {
                        Spacer(Modifier.height(12.dp))
                        Text(strings.compareWeekLabel, style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableWeeks.filter { it != selectedWeekStart }.forEach { otherWs ->
                                FilterChip(
                                    selected = compareWeekStart == otherWs,
                                    onClick = { compareWeekStart = if (compareWeekStart == otherWs) null else otherWs },
                                    label = { Text(AppStringsProvider.formatShortDate(lang, otherWs)) }
                                )
                            }
                        }
                        comparedReport?.let { cmp ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "${AppStringsProvider.formatDate(lang, cmp.startDate)}: ${strings.avgDailyScore} ${AppStringsProvider.toLocalizedDigits(cmp.averageScore.roundToInt().toString(), lang)}/100 • ${AppStringsProvider.formatDuration(lang, cmp.totalActivityMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Weekly Review Editor (Section 18)
            item {
                val existingReview = state.weeklyReviews.firstOrNull { it.weekStartDate == selectedWeekStart }
                WeeklyReviewCard(
                    profileId = state.activeProfile?.id ?: 1L,
                    weekStartDate = selectedWeekStart,
                    existing = existingReview,
                    strings = strings,
                    language = lang,
                    onSave = onSaveWeeklyReview
                )
            }
        }
    }
}

@Composable
private fun StatMini(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeeklyReviewCard(
    profileId: Long,
    weekStartDate: String,
    existing: WeeklyReviewEntity?,
    strings: com.example.rawal.domain.i18n.AppStrings,
    language: AppLanguage,
    onSave: (WeeklyReviewEntity) -> Unit
) {
    var win by remember(weekStartDate, existing) { mutableStateOf(existing?.biggestWin ?: "") }
    var challenge by remember(weekStartDate, existing) { mutableStateOf(existing?.mainChallenge ?: "") }
    var focus by remember(weekStartDate, existing) { mutableStateOf(existing?.nextWeekFocus ?: "") }
    var energy by remember(weekStartDate, existing) { mutableIntStateOf(existing?.energyRating ?: 3) }
    var org by remember(weekStartDate, existing) { mutableIntStateOf(existing?.organizationRating ?: 3) }
    var savedBanner by remember(weekStartDate) { mutableStateOf(false) }

    SectionCard {
        Text(strings.weeklyReviewTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(strings.weeklyReviewPrompt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = win,
            onValueChange = { win = it; savedBanner = false },
            label = { Text(strings.biggestWinLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = challenge,
            onValueChange = { challenge = it; savedBanner = false },
            label = { Text(strings.mainChallengeLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = focus,
            onValueChange = { focus = it; savedBanner = false },
            label = { Text(strings.nextWeekFocusLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        Text("${strings.energyRatingLabel}: ${AppStringsProvider.toLocalizedDigits(energy.toString(), language)}/5", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = energy.toFloat(),
            onValueChange = { energy = it.roundToInt().coerceIn(1, 5); savedBanner = false },
            valueRange = 1f..5f,
            steps = 3
        )
        Text("${strings.organizationRatingLabel}: ${AppStringsProvider.toLocalizedDigits(org.toString(), language)}/5", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = org.toFloat(),
            onValueChange = { org = it.roundToInt().coerceIn(1, 5); savedBanner = false },
            valueRange = 1f..5f,
            steps = 3
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    onSave(
                        (existing ?: WeeklyReviewEntity(profileId = profileId, weekStartDate = weekStartDate)).copy(
                            biggestWin = win.trim(),
                            mainChallenge = challenge.trim(),
                            nextWeekFocus = focus.trim(),
                            energyRating = energy,
                            organizationRating = org
                        )
                    )
                    savedBanner = true
                },
                modifier = Modifier.testTag("save_weekly_review_button")
            ) {
                Text(strings.save)
            }
            if (savedBanner) {
                Text(strings.done, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
