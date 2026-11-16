package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rawal.data.local.entity.ActivityItemEntity
import com.example.rawal.data.local.entity.DailyRecordEntity
import com.example.rawal.ui.RawalUiState
import com.example.rawal.ui.RawalViewModel
import com.example.rawal.ui.UiEvent
import com.example.rawal.ui.components.ActivityEditorDialog
import com.example.rawal.ui.components.DailyStartupDialog
import com.example.rawal.ui.components.DaySleepWastedDialog
import com.example.rawal.ui.components.EndOfDayReviewDialog
import com.example.rawal.ui.components.ProfileAvatar
import com.example.rawal.ui.screens.CalendarScreen
import com.example.rawal.ui.screens.DashboardScreen
import com.example.rawal.ui.screens.GoalsAchievementsScreen
import com.example.rawal.ui.screens.HistorySearchScreen
import com.example.rawal.ui.screens.ProgressSubTab
import com.example.rawal.ui.screens.ReportsScreen
import com.example.rawal.ui.screens.SettingsScreen
import com.example.rawal.ui.theme.RawalTheme
import kotlinx.coroutines.launch

enum class RawalRoute {
    DASHBOARD,
    CALENDAR,
    REPORTS,
    HISTORY,
    SEARCH,
    GOALS,
    ACHIEVEMENTS,
    OVERALL_PROGRESS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: RawalViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            RawalTheme(
                language = state.language,
                accentKey = state.settings.accentColorKey,
                fontSizeScale = state.settings.fontSizeScale,
                themeMode = state.settings.themeMode
            ) {
                if (!state.isReady) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    RawalAppShell(state = state, viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RawalAppShell(
    state: RawalUiState,
    viewModel: RawalViewModel
) {
    val strings = state.strings
    var currentRoute by remember { mutableStateOf(RawalRoute.DASHBOARD) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Dialog states
    var showDailySetup by remember { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<ActivityItemEntity?>(null) }
    var creatingActivity by remember { mutableStateOf(false) }
    var showEndOfDayReview by remember { mutableStateOf(false) }
    var showSleepWastedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = if (event.showUndo) strings.undo else null,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed && event.showUndo) {
                        viewModel.undoLastDeletion()
                    }
                }
            }
        }
    }

    if (currentRoute != RawalRoute.DASHBOARD) {
        BackHandler {
            currentRoute = RawalRoute.DASHBOARD
        }
    }

    val drawerItems = listOf(
        Triple(RawalRoute.DASHBOARD, strings.navDashboard, Icons.Rounded.Dashboard),
        Triple(RawalRoute.CALENDAR, strings.navCalendar, Icons.Rounded.CalendarMonth),
        Triple(RawalRoute.REPORTS, strings.navReports, Icons.Rounded.Assessment),
        Triple(RawalRoute.HISTORY, strings.navHistory, Icons.Rounded.History),
        Triple(RawalRoute.SEARCH, strings.globalSearchTitle, Icons.Rounded.Search),
        Triple(RawalRoute.GOALS, strings.navGoals, Icons.Rounded.TrackChanges),
        Triple(RawalRoute.ACHIEVEMENTS, strings.navAchievements, Icons.Rounded.EmojiEvents),
        Triple(RawalRoute.OVERALL_PROGRESS, strings.navOverallProgress, Icons.Rounded.Timeline),
        Triple(RawalRoute.SETTINGS, strings.navSettings, Icons.Rounded.Settings)
    )

    val bottomNavItems = listOf(
        Triple(RawalRoute.DASHBOARD, strings.navDashboard, Icons.Rounded.Dashboard),
        Triple(RawalRoute.CALENDAR, strings.navCalendar, Icons.Rounded.CalendarMonth),
        Triple(RawalRoute.REPORTS, strings.navReports, Icons.Rounded.Assessment),
        Triple(RawalRoute.GOALS, strings.navExplore, Icons.Rounded.EmojiEvents),
        Triple(RawalRoute.SETTINGS, strings.navSettings, Icons.Rounded.Settings)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.widthIn(max = 300.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.activeProfile?.let { prof ->
                            ProfileAvatar(prof.avatarKey, prof.customAvatarUri, prof.name, size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(strings.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(prof.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    drawerItems.forEach { (route, label, icon) ->
                        NavigationDrawerItem(
                            selected = currentRoute == route,
                            onClick = {
                                currentRoute = route
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("drawer_${route.name}")
                        )
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            val titleText = drawerItems.firstOrNull { it.first == currentRoute }?.second ?: strings.appName
                            Text(titleText, fontWeight = FontWeight.Bold)
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("open_drawer_button")
                            ) {
                                Icon(Icons.Rounded.Menu, contentDescription = strings.appName)
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { currentRoute = RawalRoute.SEARCH },
                                modifier = Modifier.testTag("top_search_button")
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = strings.globalSearchTitle)
                            }
                            state.activeProfile?.let { prof ->
                                IconButton(
                                    onClick = { currentRoute = RawalRoute.SETTINGS },
                                    modifier = Modifier.testTag("top_profile_button")
                                ) {
                                    ProfileAvatar(prof.avatarKey, prof.customAvatarUri, prof.name, size = 30.dp)
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar {
                            bottomNavItems.forEach { (route, label, icon) ->
                                val isProgressSelected = route == RawalRoute.GOALS &&
                                    currentRoute in listOf(RawalRoute.GOALS, RawalRoute.ACHIEVEMENTS, RawalRoute.OVERALL_PROGRESS)
                                NavigationBarItem(
                                    selected = currentRoute == route || isProgressSelected,
                                    onClick = { currentRoute = route },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label, maxLines = 1) },
                                    modifier = Modifier.testTag("bottom_nav_${route.name}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        NavigationRail(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            drawerItems.forEach { (route, label, icon) ->
                                NavigationRailItem(
                                    selected = currentRoute == route,
                                    onClick = { currentRoute = route },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 760.dp)
                        ) {
                            when (currentRoute) {
                                RawalRoute.DASHBOARD -> DashboardScreen(
                                    state = state,
                                    onSelectDate = viewModel::selectDashboardDate,
                                    onOpenDailySetup = { showDailySetup = true },
                                    onOpenAddActivity = { creatingActivity = true },
                                    onEditActivity = { editingActivity = it },
                                    onToggleActivity = viewModel::toggleActivityCompleted,
                                    onDeleteActivity = viewModel::deleteActivity,
                                    onOpenEndOfDayReview = { showEndOfDayReview = true },
                                    onOpenSleepWastedDialog = { showSleepWastedDialog = true }
                                )

                                RawalRoute.CALENDAR -> CalendarScreen(
                                    state = state,
                                    onOpenDayInDashboard = { dateIso ->
                                        viewModel.selectDashboardDate(dateIso)
                                        currentRoute = RawalRoute.DASHBOARD
                                    },
                                    onEditActivity = { editingActivity = it },
                                    onToggleActivity = viewModel::toggleActivityCompleted,
                                    onDeleteActivity = viewModel::deleteActivity,
                                    onDeleteDay = viewModel::deleteDay
                                )

                                RawalRoute.REPORTS -> ReportsScreen(
                                    state = state,
                                    onSaveWeeklyReview = viewModel::saveWeeklyReview
                                )

                                RawalRoute.HISTORY -> HistorySearchScreen(
                                    state = state,
                                    initialShowSearch = false,
                                    onOpenRecordDate = { dateIso ->
                                        viewModel.selectDashboardDate(dateIso)
                                        currentRoute = RawalRoute.DASHBOARD
                                    }
                                )

                                RawalRoute.SEARCH -> HistorySearchScreen(
                                    state = state,
                                    initialShowSearch = true,
                                    onOpenRecordDate = { dateIso ->
                                        viewModel.selectDashboardDate(dateIso)
                                        currentRoute = RawalRoute.DASHBOARD
                                    }
                                )

                                RawalRoute.GOALS -> GoalsAchievementsScreen(
                                    state = state,
                                    initialTab = ProgressSubTab.GOALS,
                                    onSaveGoal = viewModel::saveGoal,
                                    onDeleteGoal = viewModel::deleteGoal
                                )

                                RawalRoute.ACHIEVEMENTS -> GoalsAchievementsScreen(
                                    state = state,
                                    initialTab = ProgressSubTab.ACHIEVEMENTS_STREAKS,
                                    onSaveGoal = viewModel::saveGoal,
                                    onDeleteGoal = viewModel::deleteGoal
                                )

                                RawalRoute.OVERALL_PROGRESS -> GoalsAchievementsScreen(
                                    state = state,
                                    initialTab = ProgressSubTab.OVERALL_JOURNEY,
                                    onSaveGoal = viewModel::saveGoal,
                                    onDeleteGoal = viewModel::deleteGoal
                                )

                                RawalRoute.SETTINGS -> SettingsScreen(
                                    state = state,
                                    onUpdateSettings = viewModel::updateSettings,
                                    onSwitchProfile = viewModel::switchProfile,
                                    onCreateProfile = viewModel::createProfile,
                                    onUpdateProfile = viewModel::updateProfile,
                                    onDeleteProfile = viewModel::deleteProfile,
                                    onSaveCategory = viewModel::saveCategory,
                                    onDeleteCategory = viewModel::deleteCategory,
                                    onOpenGoals = { currentRoute = RawalRoute.GOALS },
                                    onBuildBackupJson = viewModel::buildBackupJson,
                                    onParseBackupJson = viewModel::parseBackupJson,
                                    onApplyBackupPayload = viewModel::applyBackupPayload,
                                    onBuildActivitiesCsv = viewModel::buildActivitiesCsv,
                                    onBuildReportsCsv = viewModel::buildDailyReportsCsv,
                                    onNotify = viewModel::emitMessage
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Active Modals
    val activeProfileId = state.activeProfile?.id ?: 1L
    val selectedDate = state.selectedDashboardDate
    val currentRecord = state.records.firstOrNull { it.date == selectedDate }
        ?: DailyRecordEntity(profileId = activeProfileId, date = selectedDate)
    val currentActivities = state.activities.filter { it.date == selectedDate }

    if (showDailySetup) {
        DailyStartupDialog(
            profileId = activeProfileId,
            date = selectedDate,
            existingRecord = state.records.firstOrNull { it.date == selectedDate },
            existingActivities = currentActivities,
            categories = state.categories,
            strings = strings,
            language = state.language,
            onFinish = { wh, wm, sh, sm, fixed, extra ->
                viewModel.saveDailySetup(selectedDate, wh, wm, sh, sm, fixed, extra)
                showDailySetup = false
            },
            onDismiss = { showDailySetup = false }
        )
    }

    if (creatingActivity || editingActivity != null) {
        ActivityEditorDialog(
            initial = editingActivity,
            profileId = activeProfileId,
            date = selectedDate,
            categories = state.categories,
            strings = strings,
            language = state.language,
            onSave = { item ->
                viewModel.saveActivity(item)
                creatingActivity = false
                editingActivity = null
            },
            onDismiss = {
                creatingActivity = false
                editingActivity = null
            }
        )
    }

    if (showEndOfDayReview) {
        EndOfDayReviewDialog(
            record = currentRecord,
            strings = strings,
            onSave = { updated ->
                viewModel.saveDailyRecord(updated)
                showEndOfDayReview = false
            },
            onDismiss = { showEndOfDayReview = false }
        )
    }

    if (showSleepWastedDialog) {
        DaySleepWastedDialog(
            record = currentRecord,
            strings = strings,
            onSave = { updated ->
                viewModel.saveDailyRecord(updated)
                showSleepWastedDialog = false
            },
            onDismiss = { showSleepWastedDialog = false }
        )
    }
}
