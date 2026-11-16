package com.example.rawal.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawal.data.local.RawalDatabase
import com.example.rawal.data.local.entity.ActivityItemEntity
import com.example.rawal.data.local.entity.CategoryEntity
import com.example.rawal.data.local.entity.DailyRecordEntity
import com.example.rawal.data.local.entity.ProfileEntity
import com.example.rawal.data.local.entity.ProfileSettingsEntity
import com.example.rawal.data.local.entity.UnlockedAchievementEntity
import com.example.rawal.data.local.entity.WeeklyGoalEntity
import com.example.rawal.data.local.entity.WeeklyReviewEntity
import com.example.rawal.data.preferences.AppPreferences
import com.example.rawal.data.repository.RawalRepository
import com.example.rawal.domain.analytics.ActivityHistoryGroup
import com.example.rawal.domain.analytics.AnalyticsEngine
import com.example.rawal.domain.analytics.GoalProgress
import com.example.rawal.domain.analytics.PersonalRecords
import com.example.rawal.domain.analytics.StreakSummary
import com.example.rawal.domain.analytics.SuggestedReminders
import com.example.rawal.domain.backup.BackupManager
import com.example.rawal.domain.backup.RawalBackupPayload
import com.example.rawal.domain.calendar.RawalDate
import com.example.rawal.domain.i18n.AppLanguage
import com.example.rawal.domain.i18n.AppStrings
import com.example.rawal.domain.i18n.AppStringsProvider
import com.example.rawal.notifications.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RawalUiState(
    val isReady: Boolean = false,
    val activeProfile: ProfileEntity? = null,
    val profiles: List<ProfileEntity> = emptyList(),
    val settings: ProfileSettingsEntity = ProfileSettingsEntity(profileId = 1L),
    val language: AppLanguage = AppLanguage.EN,
    val strings: AppStrings = AppStringsProvider.forLanguage(AppLanguage.EN),
    val categories: List<CategoryEntity> = emptyList(),
    val records: List<DailyRecordEntity> = emptyList(),
    val activities: List<ActivityItemEntity> = emptyList(),
    val goals: List<WeeklyGoalEntity> = emptyList(),
    val weeklyReviews: List<WeeklyReviewEntity> = emptyList(),
    val unlockedAchievements: List<UnlockedAchievementEntity> = emptyList(),
    val selectedDashboardDate: String = RawalDate.today().toIso(),
    val streakSummary: StreakSummary = StreakSummary(0, 0, 65, emptyList()),
    val personalRecords: PersonalRecords = PersonalRecords(null, 0, null, 0f, 0, null, null, 0, null, 0, emptyList()),
    val goalProgresses: List<GoalProgress> = emptyList(),
    val activityHistoryGroups: List<ActivityHistoryGroup> = emptyList(),
    val suggestedReminders: SuggestedReminders = SuggestedReminders(20, 30, 21, 30, 0),
    val canUndoDelete: Boolean = false
)

sealed class UiEvent {
    data class ShowMessage(val message: String, val showUndo: Boolean = false) : UiEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
class RawalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RawalRepository(
        db = RawalDatabase.getInstance(application),
        prefs = AppPreferences(application)
    )

    private val _selectedDashboardDate = MutableStateFlow(RawalDate.today().toIso())
    private val _undoTrigger = MutableStateFlow(0)
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    private val activeProfileIdFlow = repository.activeProfileIdFlow.filterNotNull()

    private val profileScopedData = activeProfileIdFlow.flatMapLatest { pid ->
        val flowA = combine(
            repository.observeSettings(pid),
            repository.observeCategories(pid),
            repository.observeRecords(pid),
            repository.observeActivities(pid)
        ) { settings, categories, records, activities ->
            QuadData(pid, settings ?: ProfileSettingsEntity(profileId = pid), categories, records, activities)
        }
        val flowB = combine(
            repository.observeGoals(pid),
            repository.observeReviews(pid),
            repository.observeAchievements(pid)
        ) { goals, reviews, achievements ->
            Triple(goals, reviews, achievements)
        }
        combine(flowA, flowB) { a, b -> a to b }
    }

    val uiState: StateFlow<RawalUiState> = combine(
        repository.observeProfiles(),
        profileScopedData,
        _selectedDashboardDate,
        _undoTrigger
    ) { profiles, scoped, selectedDate, _ ->
        val (base, extras) = scoped
        val (goals, reviews, achievements) = extras
        val activeProfile = profiles.firstOrNull { it.id == base.profileId } ?: profiles.firstOrNull()
        val language = AppLanguage.fromCode(base.settings.languageCode)
        val strings = AppStringsProvider.forLanguage(language)

        val readingIds = base.categories.filter {
            it.nameEn.contains("Read", ignoreCase = true) || it.nameFa.contains("مطالعه")
        }.map { it.id }.toSet()

        val streakSummary = AnalyticsEngine.calculateStreaks(
            records = base.records,
            threshold = base.settings.streakThreshold
        )
        val personalRecords = AnalyticsEngine.calculatePersonalRecords(
            records = base.records,
            activities = base.activities,
            categories = base.categories,
            streakSummary = streakSummary,
            readingCategoryIds = readingIds
        )
        val goalProgresses = AnalyticsEngine.calculateGoalProgresses(goals, base.activities)
        val historyGroups = AnalyticsEngine.buildActivityHistoryGroups(base.activities)
        val suggestedReminders = AnalyticsEngine.suggestReminderTimes(base.records)

        RawalUiState(
            isReady = true,
            activeProfile = activeProfile,
            profiles = profiles,
            settings = base.settings,
            language = language,
            strings = strings,
            categories = base.categories,
            records = base.records,
            activities = base.activities,
            goals = goals,
            weeklyReviews = reviews,
            unlockedAchievements = achievements,
            selectedDashboardDate = selectedDate,
            streakSummary = streakSummary,
            personalRecords = personalRecords,
            goalProgresses = goalProgresses,
            activityHistoryGroups = historyGroups,
            suggestedReminders = suggestedReminders,
            canUndoDelete = repository.hasPendingUndo()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RawalUiState()
    )

    fun selectDashboardDate(date: String) {
        _selectedDashboardDate.value = date
    }

    fun toggleActivityCompleted(activity: ActivityItemEntity) {
        viewModelScope.launch {
            repository.toggleActivityCompleted(activity)
        }
    }

    fun saveActivity(activity: ActivityItemEntity) {
        viewModelScope.launch {
            repository.saveActivity(activity)
        }
    }

    fun deleteActivity(activity: ActivityItemEntity) {
        viewModelScope.launch {
            repository.deleteActivity(activity)
            _undoTrigger.value++
            _events.emit(UiEvent.ShowMessage(uiState.value.strings.deletedUndoMessage, showUndo = true))
        }
    }

    fun deleteDay(date: String) {
        val pid = uiState.value.activeProfile?.id ?: return
        viewModelScope.launch {
            repository.deleteDay(pid, date)
            _undoTrigger.value++
            _events.emit(UiEvent.ShowMessage(uiState.value.strings.deletedUndoMessage, showUndo = true))
        }
    }

    fun undoLastDeletion() {
        viewModelScope.launch {
            repository.undoLastDeletion()
            _undoTrigger.value++
        }
    }

    fun saveDailyRecord(record: DailyRecordEntity) {
        viewModelScope.launch {
            repository.saveDailyRecord(record)
        }
    }

    fun saveDailySetup(
        date: String,
        wakeHour: Int?,
        wakeMinute: Int?,
        sleepHour: Int?,
        sleepMinute: Int?,
        fixedActivities: List<ActivityItemEntity>,
        additionalTasks: List<ActivityItemEntity>
    ) {
        val pid = uiState.value.activeProfile?.id ?: return
        viewModelScope.launch {
            repository.saveDailySetupResult(
                profileId = pid,
                date = date,
                wakeHour = wakeHour,
                wakeMinute = wakeMinute,
                sleepHour = sleepHour,
                sleepMinute = sleepMinute,
                fixedActivities = fixedActivities,
                additionalTasks = additionalTasks
            )
        }
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.saveCategory(category)
        }
    }

    fun deleteCategory(categoryId: Long) {
        val pid = uiState.value.activeProfile?.id ?: return
        viewModelScope.launch {
            repository.deleteCategory(pid, categoryId)
        }
    }

    fun saveGoal(goal: WeeklyGoalEntity) {
        viewModelScope.launch {
            repository.saveGoal(goal)
        }
    }

    fun deleteGoal(goalId: Long) {
        val pid = uiState.value.activeProfile?.id ?: return
        viewModelScope.launch {
            repository.deleteGoal(pid, goalId)
        }
    }

    fun saveWeeklyReview(review: WeeklyReviewEntity) {
        viewModelScope.launch {
            repository.saveWeeklyReview(review)
        }
    }

    fun updateSettings(newSettings: ProfileSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
            ReminderScheduler.syncReminders(
                context = getApplication(),
                enabled = newSettings.remindersEnabled,
                langCode = newSettings.languageCode,
                durationHour = newSettings.missingDurationReminderHour,
                durationMinute = newSettings.missingDurationReminderMinute,
                eodHour = newSettings.endOfDayReminderHour,
                eodMinute = newSettings.endOfDayReminderMinute
            )
        }
    }

    fun switchProfile(profileId: Long) {
        viewModelScope.launch {
            repository.switchProfile(profileId)
            _undoTrigger.value++
        }
    }

    fun createProfile(name: String, avatarKey: String, customAvatarUri: String?) {
        val currentLang = uiState.value.settings.languageCode
        viewModelScope.launch {
            repository.createProfile(name, avatarKey, customAvatarUri, currentLang)
        }
    }

    fun updateProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            repository.updateProfile(profile)
        }
    }

    fun deleteProfile(profileId: Long) {
        viewModelScope.launch {
            val ok = repository.deleteProfile(profileId)
            if (!ok) {
                _events.emit(UiEvent.ShowMessage(uiState.value.strings.cannotDeleteLastProfile))
            }
        }
    }

    suspend fun buildBackupJson(): String {
        val payload = repository.buildFullBackupPayload()
        return BackupManager.exportToJson(payload)
    }

    fun parseBackupJson(jsonString: String): Result<RawalBackupPayload> =
        BackupManager.parseAndValidateJson(jsonString)

    fun applyBackupPayload(payload: RawalBackupPayload) {
        viewModelScope.launch {
            repository.restoreFromBackup(payload)
            _undoTrigger.value++
            _events.emit(UiEvent.ShowMessage(uiState.value.strings.importSuccess))
        }
    }

    fun buildActivitiesCsv(): String {
        val state = uiState.value
        return BackupManager.exportActivitiesCsv(state.activities, state.categories)
    }

    fun buildDailyReportsCsv(): String {
        return BackupManager.exportDailyReportsCsv(uiState.value.records)
    }

    fun emitMessage(message: String) {
        viewModelScope.launch {
            _events.emit(UiEvent.ShowMessage(message))
        }
    }
}

private data class QuadData(
    val profileId: Long,
    val settings: ProfileSettingsEntity,
    val categories: List<CategoryEntity>,
    val records: List<DailyRecordEntity>,
    val activities: List<ActivityItemEntity>
)
