package com.example.rawal.data.repository

import androidx.room.withTransaction
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
import com.example.rawal.domain.analytics.AnalyticsEngine
import com.example.rawal.domain.backup.RawalBackupPayload
import com.example.rawal.domain.scoring.AchievementCatalog
import com.example.rawal.domain.scoring.DailyScoringEngine
import kotlinx.coroutines.flow.Flow
import java.util.Locale

sealed class UndoAction {
    data class DeletedActivity(val activity: ActivityItemEntity) : UndoAction()
    data class DeletedDay(val record: DailyRecordEntity?, val activities: List<ActivityItemEntity>) : UndoAction()
}

class RawalRepository(
    private val db: RawalDatabase,
    private val prefs: AppPreferences
) {
    private val profileDao = db.profileDao()
    private val settingsDao = db.profileSettingsDao()
    private val categoryDao = db.categoryDao()
    private val recordDao = db.dailyRecordDao()
    private val activityDao = db.activityDao()
    private val goalDao = db.weeklyGoalDao()
    private val reviewDao = db.weeklyReviewDao()
    private val achievementDao = db.achievementDao()

    private var lastUndoAction: UndoAction? = null

    val activeProfileIdFlow: Flow<Long?> = prefs.activeProfileId

    fun observeProfiles(): Flow<List<ProfileEntity>> = profileDao.observeProfiles()
    fun observeSettings(profileId: Long): Flow<ProfileSettingsEntity?> = settingsDao.observeSettings(profileId)
    fun observeCategories(profileId: Long): Flow<List<CategoryEntity>> = categoryDao.observeCategories(profileId)
    fun observeRecords(profileId: Long): Flow<List<DailyRecordEntity>> = recordDao.observeRecords(profileId)
    fun observeActivities(profileId: Long): Flow<List<ActivityItemEntity>> = activityDao.observeActivities(profileId)
    fun observeGoals(profileId: Long): Flow<List<WeeklyGoalEntity>> = goalDao.observeGoals(profileId)
    fun observeReviews(profileId: Long): Flow<List<WeeklyReviewEntity>> = reviewDao.observeReviews(profileId)
    fun observeAchievements(profileId: Long): Flow<List<UnlockedAchievementEntity>> = achievementDao.observeUnlocked(profileId)

    suspend fun ensureInitialized(): Long {
        val existing = profileDao.getAllProfiles()
        val storedActiveId = prefs.getActiveProfileId()
        if (existing.isNotEmpty()) {
            val validId = existing.firstOrNull { it.id == storedActiveId }?.id ?: existing.first().id
            if (storedActiveId != validId) {
                prefs.setActiveProfileId(validId)
            }
            if (settingsDao.getSettings(validId) == null) {
                settingsDao.upsertSettings(ProfileSettingsEntity(profileId = validId, languageCode = detectDeviceLanguage()))
            }
            if (categoryDao.getCategoriesForProfile(validId).isEmpty()) {
                categoryDao.insertCategories(RawalDatabase.defaultCategoriesForProfile(validId))
            }
            return validId
        }

        val detectedLang = detectDeviceLanguage()
        val defaultName = if (detectedLang == "fa") "پروفایل من" else "My Profile"
        val profileId = createProfileInternal(
            name = defaultName,
            avatarKey = "teal_sun",
            customAvatarUri = null,
            languageCode = detectedLang,
            isDefault = true
        )
        prefs.setActiveProfileId(profileId)
        prefs.markFirstLaunchDone()
        return profileId
    }

    private fun detectDeviceLanguage(): String =
        if (Locale.getDefault().language.equals("fa", ignoreCase = true)) "fa" else "en"

    suspend fun switchProfile(profileId: Long) {
        lastUndoAction = null
        prefs.setActiveProfileId(profileId)
    }

    suspend fun createProfile(
        name: String,
        avatarKey: String,
        customAvatarUri: String?,
        languageCode: String
    ): Long {
        val newId = createProfileInternal(name, avatarKey, customAvatarUri, languageCode, isDefault = false)
        prefs.setActiveProfileId(newId)
        return newId
    }

    private suspend fun createProfileInternal(
        name: String,
        avatarKey: String,
        customAvatarUri: String?,
        languageCode: String,
        isDefault: Boolean
    ): Long {
        return db.withTransaction {
            val id = profileDao.insertProfile(
                ProfileEntity(
                    name = name.trim(),
                    avatarKey = avatarKey,
                    customAvatarUri = customAvatarUri,
                    isDefault = isDefault
                )
            )
            settingsDao.upsertSettings(
                ProfileSettingsEntity(
                    profileId = id,
                    languageCode = languageCode
                )
            )
            categoryDao.insertCategories(RawalDatabase.defaultCategoriesForProfile(id))
            id
        }
    }

    suspend fun updateProfile(profile: ProfileEntity) {
        profileDao.updateProfile(profile)
    }

    suspend fun deleteProfile(profileId: Long): Boolean {
        val all = profileDao.getAllProfiles()
        if (all.size <= 1) return false
        db.withTransaction {
            profileDao.deleteProfileById(profileId)
        }
        val remaining = profileDao.getAllProfiles()
        val nextActive = remaining.firstOrNull()?.id
        if (nextActive != null) {
            prefs.setActiveProfileId(nextActive)
        }
        return true
    }

    suspend fun updateSettings(settings: ProfileSettingsEntity) {
        settingsDao.upsertSettings(settings)
        recalculateAllScoresForProfile(settings.profileId, settings.streakThreshold)
    }

    suspend fun saveCategory(category: CategoryEntity): Long {
        return if (category.id == 0L) {
            categoryDao.insertCategory(category)
        } else {
            categoryDao.updateCategory(category)
            category.id
        }
    }

    suspend fun deleteCategory(profileId: Long, categoryId: Long) {
        val categories = categoryDao.getCategoriesForProfile(profileId)
        val fallback = categories.firstOrNull { it.id != categoryId && it.nameEn.equals("Other", ignoreCase = true) }
            ?: categories.firstOrNull { it.id != categoryId }
        if (fallback != null) {
            activityDao.reassignCategory(profileId, categoryId, fallback.id)
        }
        categoryDao.deleteCategoryById(categoryId)
    }

    suspend fun saveActivity(activity: ActivityItemEntity): Long {
        val now = System.currentTimeMillis()
        val id = if (activity.id == 0L) {
            activityDao.insertActivity(activity.copy(createdAt = now, updatedAt = now))
        } else {
            activityDao.updateActivity(activity.copy(updatedAt = now))
            activity.id
        }
        ensureRecordAndRecalculate(activity.profileId, activity.date)
        return id
    }

    suspend fun toggleActivityCompleted(activity: ActivityItemEntity) {
        val updated = activity.copy(
            isCompleted = !activity.isCompleted,
            updatedAt = System.currentTimeMillis()
        )
        activityDao.updateActivity(updated)
        ensureRecordAndRecalculate(activity.profileId, activity.date)
    }

    suspend fun deleteActivity(activity: ActivityItemEntity) {
        lastUndoAction = UndoAction.DeletedActivity(activity)
        activityDao.deleteActivityById(activity.id)
        ensureRecordAndRecalculate(activity.profileId, activity.date)
    }

    suspend fun saveDailyRecord(record: DailyRecordEntity) {
        recordDao.upsertRecord(record.copy(updatedAt = System.currentTimeMillis()))
        ensureRecordAndRecalculate(record.profileId, record.date)
    }

    suspend fun saveDailySetupResult(
        profileId: Long,
        date: String,
        wakeHour: Int?,
        wakeMinute: Int?,
        sleepHour: Int?,
        sleepMinute: Int?,
        fixedActivities: List<ActivityItemEntity>,
        additionalTasks: List<ActivityItemEntity>
    ) {
        db.withTransaction {
            val existingActivities = activityDao.getActivitiesForDate(profileId, date)
            val existingFixedByKey = existingActivities.filter { it.fixedKey != null }.associateBy { it.fixedKey }

            fixedActivities.forEach { item ->
                val prev = existingFixedByKey[item.fixedKey]
                if (prev != null) {
                    activityDao.updateActivity(
                        prev.copy(
                            isCompleted = item.isCompleted,
                            durationMinutes = item.durationMinutes,
                            notes = item.notes,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    activityDao.insertActivity(item)
                }
            }

            additionalTasks.forEach { task ->
                activityDao.insertActivity(task)
            }

            val prevRecord = recordDao.getRecordForDate(profileId, date)
            val updatedRecord = (prevRecord ?: DailyRecordEntity(profileId = profileId, date = date)).copy(
                wakeUpHour = wakeHour,
                wakeUpMinute = wakeMinute,
                sleepHour = sleepHour,
                sleepMinute = sleepMinute,
                startupCompleted = true,
                updatedAt = System.currentTimeMillis()
            )
            recordDao.upsertRecord(updatedRecord)
        }
        ensureRecordAndRecalculate(profileId, date)
    }

    suspend fun deleteDay(profileId: Long, date: String) {
        val prevRecord = recordDao.getRecordForDate(profileId, date)
        val prevActivities = activityDao.getActivitiesForDate(profileId, date)
        lastUndoAction = UndoAction.DeletedDay(prevRecord, prevActivities)
        db.withTransaction {
            activityDao.deleteActivitiesForDate(profileId, date)
            recordDao.deleteRecordForDate(profileId, date)
        }
        evaluateAchievements(profileId)
    }

    suspend fun undoLastDeletion(): Boolean {
        val action = lastUndoAction ?: return false
        lastUndoAction = null
        when (action) {
            is UndoAction.DeletedActivity -> {
                activityDao.insertActivity(action.activity)
                ensureRecordAndRecalculate(action.activity.profileId, action.activity.date)
            }
            is UndoAction.DeletedDay -> {
                db.withTransaction {
                    action.record?.let { recordDao.upsertRecord(it) }
                    if (action.activities.isNotEmpty()) {
                        activityDao.insertActivities(action.activities)
                    }
                }
                val profileId = action.record?.profileId ?: action.activities.firstOrNull()?.profileId
                val date = action.record?.date ?: action.activities.firstOrNull()?.date
                if (profileId != null && date != null) {
                    ensureRecordAndRecalculate(profileId, date)
                }
            }
        }
        return true
    }

    fun hasPendingUndo(): Boolean = lastUndoAction != null

    suspend fun saveGoal(goal: WeeklyGoalEntity): Long {
        val id = goalDao.upsertGoal(goal)
        evaluateAchievements(goal.profileId)
        return id
    }

    suspend fun deleteGoal(profileId: Long, goalId: Long) {
        goalDao.deleteGoalById(goalId)
        evaluateAchievements(profileId)
    }

    suspend fun saveWeeklyReview(review: WeeklyReviewEntity): Long {
        return reviewDao.upsertReview(review.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun ensureRecordAndRecalculate(profileId: Long, date: String) {
        val activities = activityDao.getActivitiesForDate(profileId, date)
        val existing = recordDao.getRecordForDate(profileId, date)
        val base = existing ?: DailyRecordEntity(profileId = profileId, date = date)
        val breakdown = DailyScoringEngine.calculate(base, activities)
        recordDao.upsertRecord(
            base.copy(
                calculatedScore = breakdown.totalScore,
                updatedAt = System.currentTimeMillis()
            )
        )
        evaluateAchievements(profileId)
    }

    private suspend fun recalculateAllScoresForProfile(profileId: Long, threshold: Int) {
        val records = recordDao.getRecordsForProfile(profileId)
        val activities = activityDao.getActivitiesForProfile(profileId)
        val byDate = activities.groupBy { it.date }
        records.forEach { rec ->
            val breakdown = DailyScoringEngine.calculate(rec, byDate[rec.date].orEmpty())
            if (breakdown.totalScore != rec.calculatedScore) {
                recordDao.upsertRecord(rec.copy(calculatedScore = breakdown.totalScore))
            }
        }
        evaluateAchievements(profileId, threshold)
    }

    private suspend fun evaluateAchievements(profileId: Long, customThreshold: Int? = null) {
        val settings = settingsDao.getSettings(profileId)
        val threshold = customThreshold ?: settings?.streakThreshold ?: 65
        val records = recordDao.getRecordsForProfile(profileId)
        val activities = activityDao.getActivitiesForProfile(profileId)
        val categories = categoryDao.getCategoriesForProfile(profileId)
        val goals = goalDao.getGoalsForProfile(profileId)

        val readingIds = categories.filter {
            it.nameEn.contains("Read", ignoreCase = true) || it.nameFa.contains("مطالعه")
        }.map { it.id }.toSet()

        val streakSummary = AnalyticsEngine.calculateStreaks(records, threshold)
        val goalProgresses = AnalyticsEngine.calculateGoalProgresses(goals, activities)
        val shouldBeUnlocked = AchievementCatalog.evaluateUnlockedKeys(
            records = records,
            activities = activities,
            streakSummary = streakSummary,
            goalProgresses = goalProgresses,
            readingCategoryIds = readingIds
        )
        val existing = achievementDao.getUnlockedForProfile(profileId).map { it.achievementKey }.toSet()
        val toAdd = shouldBeUnlocked - existing
        toAdd.forEach { key ->
            achievementDao.insertAchievement(
                UnlockedAchievementEntity(
                    profileId = profileId,
                    achievementKey = key
                )
            )
        }
    }

    suspend fun buildFullBackupPayload(): RawalBackupPayload {
        return RawalBackupPayload(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            activeProfileId = prefs.getActiveProfileId(),
            profiles = profileDao.getAllProfiles(),
            settings = settingsDao.getAllSettings(),
            categories = categoryDao.getAllCategories(),
            dailyRecords = recordDao.getAllRecords(),
            activities = activityDao.getAllActivities(),
            weeklyGoals = goalDao.getAllGoals(),
            weeklyReviews = reviewDao.getAllReviews(),
            unlockedAchievements = achievementDao.getAllUnlocked()
        )
    }

    suspend fun restoreFromBackup(payload: RawalBackupPayload): Long {
        lastUndoAction = null
        db.withTransaction {
            profileDao.deleteAllProfiles() // CASCADE clears all profile-scoped tables
            payload.profiles.forEach { profileDao.insertProfile(it) }
            payload.settings.forEach { settingsDao.upsertSettings(it) }
            payload.categories.forEach { categoryDao.insertCategory(it) }
            payload.dailyRecords.forEach { recordDao.upsertRecord(it) }
            payload.activities.forEach { activityDao.insertActivity(it) }
            payload.weeklyGoals.forEach { goalDao.upsertGoal(it) }
            payload.weeklyReviews.forEach { reviewDao.upsertReview(it) }
            payload.unlockedAchievements.forEach { achievementDao.insertAchievement(it) }
        }
        val nextActive = payload.activeProfileId?.takeIf { id -> payload.profiles.any { it.id == id } }
            ?: payload.profiles.first().id
        prefs.setActiveProfileId(nextActive)
        // Ensure settings & categories exist for all restored profiles
        payload.profiles.forEach { p ->
            if (settingsDao.getSettings(p.id) == null) {
                settingsDao.upsertSettings(ProfileSettingsEntity(profileId = p.id))
            }
            if (categoryDao.getCategoriesForProfile(p.id).isEmpty()) {
                categoryDao.insertCategories(RawalDatabase.defaultCategoriesForProfile(p.id))
            }
        }
        return nextActive
    }
}
