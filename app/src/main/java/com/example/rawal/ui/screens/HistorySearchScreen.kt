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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rawal.domain.calendar.RawalDate
import com.example.rawal.domain.i18n.AppLanguage
import com.example.rawal.domain.i18n.AppStringsProvider
import com.example.rawal.ui.RawalUiState
import com.example.rawal.ui.components.EmptyStateCard
import com.example.rawal.ui.components.SectionCard
import com.example.rawal.ui.theme.ScoreHighColor
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistorySearchScreen(
    state: RawalUiState,
    initialShowSearch: Boolean = false,
    onOpenRecordDate: (String) -> Unit
) {
    val strings = state.strings
    val lang = state.language
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }

    var modeTab by remember { mutableIntStateOf(if (initialShowSearch) 1 else 0) } // 0 = Activity History, 1 = Global Search
    var expandedActivityTitle by remember { mutableStateOf<String?>(null) }

    // Search filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var statusFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Completed, 2 = Incomplete
    var dayOfWeekFilter by remember { mutableStateOf<Int?>(null) } // 0..6
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }

    val searchResults = remember(
        state.activities, searchQuery, selectedCategoryId, statusFilter, dayOfWeekFilter, dateFrom, dateTo
    ) {
        state.activities.filter { act ->
            val q = searchQuery.trim()
            val matchesText = q.isEmpty() ||
                act.title.contains(q, ignoreCase = true) ||
                act.notes.contains(q, ignoreCase = true)
            val matchesCat = selectedCategoryId == null || act.categoryId == selectedCategoryId
            val matchesStatus = when (statusFilter) {
                1 -> act.isCompleted
                2 -> !act.isCompleted
                else -> true
            }
            val matchesDow = dayOfWeekFilter == null ||
                RawalDate.parseIso(act.date)?.rawalDayOfWeekIndex() == dayOfWeekFilter
            val matchesFrom = dateFrom.isBlank() || act.date >= dateFrom.trim()
            val matchesTo = dateTo.isBlank() || act.date <= dateTo.trim()
            matchesText && matchesCat && matchesStatus && matchesDow && matchesFrom && matchesTo
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = modeTab == 0,
                        onClick = { modeTab = 0 },
                        label = { Text(strings.activityHistoryTitle) },
                        modifier = Modifier.testTag("tab_activity_history")
                    )
                    FilterChip(
                        selected = modeTab == 1,
                        onClick = { modeTab = 1 },
                        label = { Text(strings.globalSearchTitle) },
                        modifier = Modifier.testTag("tab_global_search")
                    )
                }
            }
        }

        if (modeTab == 0) {
            if (state.activityHistoryGroups.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = strings.noRecordsYet,
                        tip = strings.emptyStateTip
                    )
                }
            } else {
                items(state.activityHistoryGroups, key = { it.title }) { group ->
                    val cat = catMap[group.categoryId]
                    val catName = if (lang == AppLanguage.FA) cat?.nameFa ?: "سایر" else cat?.nameEn ?: "Other"
                    val isExpanded = expandedActivityTitle == group.title

                    SectionCard(
                        modifier = Modifier.clickable {
                            expandedActivityTitle = if (isExpanded) null else group.title
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(group.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$catName  •  ${strings.totalSessions}: ${AppStringsProvider.toLocalizedDigits(group.completedSessions.toString(), lang)}/${AppStringsProvider.toLocalizedDigits(group.sessions.size.toString(), lang)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${strings.avgActivityDuration}: ${AppStringsProvider.formatDuration(lang, group.averageMinutes.roundToInt())}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${strings.last7Days}: ${AppStringsProvider.formatDuration(lang, group.lastSevenDaysMinutes)}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${strings.last30Days}: ${AppStringsProvider.formatDuration(lang, group.lastThirtyDaysMinutes)}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        if (isExpanded) {
                            Spacer(Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                group.sessions.forEach { session ->
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenRecordDate(session.date) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (session.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (session.isCompleted) ScoreHighColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(AppStringsProvider.formatDate(lang, session.date), style = MaterialTheme.typography.bodySmall)
                                            }
                                            Text(
                                                text = AppStringsProvider.formatDuration(lang, session.durationMinutes),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Global Search Filters
            item {
                SectionCard {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(strings.searchHint) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("global_search_input")
                    )
                    Spacer(Modifier.height(10.dp))

                    Text(strings.categoryLabel, style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text(strings.all) }
                        )
                        state.categories.forEach { cat ->
                            val name = if (lang == AppLanguage.FA) cat.nameFa else cat.nameEn
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id },
                                label = { Text(name) },
                                leadingIcon = {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(cat.colorHex)))
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = statusFilter == 0, onClick = { statusFilter = 0 }, label = { Text(strings.all) })
                        FilterChip(selected = statusFilter == 1, onClick = { statusFilter = 1 }, label = { Text(strings.completed) })
                        FilterChip(selected = statusFilter == 2, onClick = { statusFilter = 2 }, label = { Text(strings.incomplete) })
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(strings.filterDayOfWeek, style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = dayOfWeekFilter == null,
                            onClick = { dayOfWeekFilter = null },
                            label = { Text(strings.allDays) }
                        )
                        for (dow in 0..6) {
                            FilterChip(
                                selected = dayOfWeekFilter == dow,
                                onClick = { dayOfWeekFilter = if (dayOfWeekFilter == dow) null else dow },
                                label = { Text(AppStringsProvider.dayName(lang, dow).take(3)) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dateFrom,
                            onValueChange = { dateFrom = it },
                            label = { Text(strings.startDateLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dateTo,
                            onValueChange = { dateTo = it },
                            label = { Text(strings.endDateLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    TextButton(
                        onClick = {
                            searchQuery = ""
                            selectedCategoryId = null
                            statusFilter = 0
                            dayOfWeekFilter = null
                            dateFrom = ""
                            dateTo = ""
                        }
                    ) {
                        Text(strings.clearFilters)
                    }
                }
            }

            if (searchResults.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = strings.noDataForPeriod,
                        tip = strings.emptyStateTip
                    )
                }
            } else {
                items(searchResults, key = { it.id }) { act ->
                    val cat = catMap[act.categoryId]
                    val catName = if (lang == AppLanguage.FA) cat?.nameFa ?: "سایر" else cat?.nameEn ?: "Other"
                    SectionCard(
                        modifier = Modifier
                            .clickable { onOpenRecordDate(act.date) }
                            .testTag("search_result_${act.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(act.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${AppStringsProvider.formatDate(lang, act.date)}  •  $catName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (act.notes.isNotBlank()) {
                                    Text(act.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                text = AppStringsProvider.formatDuration(lang, act.durationMinutes),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
