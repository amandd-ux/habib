package com.example.rawal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.rawal.data.local.entity.ActivityItemEntity
import com.example.rawal.data.local.entity.CategoryEntity
import com.example.rawal.data.local.entity.DailyRecordEntity
import com.example.rawal.data.local.entity.ProfileEntity
import com.example.rawal.data.local.entity.ProfileSettingsEntity
import com.example.rawal.data.local.entity.UnlockedAchievementEntity
import com.example.rawal.data.local.entity.WeeklyGoalEntity
import com.example.rawal.data.local.entity.WeeklyReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun observeProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    suspend fun getAllProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    @Query("DELETE FROM profiles")
    suspend fun deleteAllProfiles()
}

@Dao
interface ProfileSettingsDao {
    @Query("SELECT * FROM profile_settings WHERE profileId = :profileId")
    fun observeSettings(profileId: Long): Flow<ProfileSettingsEntity?>

    @Query("SELECT * FROM profile_settings WHERE profileId = :profileId")
    suspend fun getSettings(profileId: Long): ProfileSettingsEntity?

    @Query("SELECT * FROM profile_settings")
    suspend fun getAllSettings(): List<ProfileSettingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: ProfileSettingsEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    fun observeCategories(profileId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    suspend fun getCategoriesForProfile(profileId: Long): List<CategoryEntity>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)
}

@Dao
interface DailyRecordDao {
    @Query("SELECT * FROM daily_records WHERE profileId = :profileId ORDER BY date DESC")
    fun observeRecords(profileId: Long): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_records WHERE profileId = :profileId AND date = :date LIMIT 1")
    fun observeRecordForDate(profileId: Long, date: String): Flow<DailyRecordEntity?>

    @Query("SELECT * FROM daily_records WHERE profileId = :profileId AND date = :date LIMIT 1")
    suspend fun getRecordForDate(profileId: Long, date: String): DailyRecordEntity?

    @Query("SELECT * FROM daily_records WHERE profileId = :profileId ORDER BY date ASC")
    suspend fun getRecordsForProfile(profileId: Long): List<DailyRecordEntity>

    @Query("SELECT * FROM daily_records")
    suspend fun getAllRecords(): List<DailyRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: DailyRecordEntity): Long

    @Query("DELETE FROM daily_records WHERE profileId = :profileId AND date = :date")
    suspend fun deleteRecordForDate(profileId: Long, date: String)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE profileId = :profileId ORDER BY date DESC, createdAt ASC")
    fun observeActivities(profileId: Long): Flow<List<ActivityItemEntity>>

    @Query("SELECT * FROM activities WHERE profileId = :profileId AND date = :date ORDER BY isFixedDaily DESC, createdAt ASC")
    fun observeActivitiesForDate(profileId: Long, date: String): Flow<List<ActivityItemEntity>>

    @Query("SELECT * FROM activities WHERE profileId = :profileId AND date = :date ORDER BY isFixedDaily DESC, createdAt ASC")
    suspend fun getActivitiesForDate(profileId: Long, date: String): List<ActivityItemEntity>

    @Query("SELECT * FROM activities WHERE profileId = :profileId ORDER BY date DESC, createdAt ASC")
    suspend fun getActivitiesForProfile(profileId: Long): List<ActivityItemEntity>

    @Query("SELECT * FROM activities")
    suspend fun getAllActivities(): List<ActivityItemEntity>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: Long): ActivityItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityItemEntity>)

    @Update
    suspend fun updateActivity(activity: ActivityItemEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: Long)

    @Query("DELETE FROM activities WHERE profileId = :profileId AND date = :date")
    suspend fun deleteActivitiesForDate(profileId: Long, date: String)

    @Query("UPDATE activities SET categoryId = :newCategoryId WHERE profileId = :profileId AND categoryId = :oldCategoryId")
    suspend fun reassignCategory(profileId: Long, oldCategoryId: Long, newCategoryId: Long)
}

@Dao
interface WeeklyGoalDao {
    @Query("SELECT * FROM weekly_goals WHERE profileId = :profileId ORDER BY weekStartDate DESC, id DESC")
    fun observeGoals(profileId: Long): Flow<List<WeeklyGoalEntity>>

    @Query("SELECT * FROM weekly_goals WHERE profileId = :profileId")
    suspend fun getGoalsForProfile(profileId: Long): List<WeeklyGoalEntity>

    @Query("SELECT * FROM weekly_goals")
    suspend fun getAllGoals(): List<WeeklyGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: WeeklyGoalEntity): Long

    @Query("DELETE FROM weekly_goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)
}

@Dao
interface WeeklyReviewDao {
    @Query("SELECT * FROM weekly_reviews WHERE profileId = :profileId ORDER BY weekStartDate DESC")
    fun observeReviews(profileId: Long): Flow<List<WeeklyReviewEntity>>

    @Query("SELECT * FROM weekly_reviews WHERE profileId = :profileId AND weekStartDate = :weekStartDate LIMIT 1")
    suspend fun getReviewForWeek(profileId: Long, weekStartDate: String): WeeklyReviewEntity?

    @Query("SELECT * FROM weekly_reviews WHERE profileId = :profileId")
    suspend fun getReviewsForProfile(profileId: Long): List<WeeklyReviewEntity>

    @Query("SELECT * FROM weekly_reviews")
    suspend fun getAllReviews(): List<WeeklyReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(review: WeeklyReviewEntity): Long
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM unlocked_achievements WHERE profileId = :profileId ORDER BY unlockedAt DESC")
    fun observeUnlocked(profileId: Long): Flow<List<UnlockedAchievementEntity>>

    @Query("SELECT * FROM unlocked_achievements WHERE profileId = :profileId")
    suspend fun getUnlockedForProfile(profileId: Long): List<UnlockedAchievementEntity>

    @Query("SELECT * FROM unlocked_achievements")
    suspend fun getAllUnlocked(): List<UnlockedAchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: UnlockedAchievementEntity): Long

    @Query("DELETE FROM unlocked_achievements WHERE profileId = :profileId AND achievementKey = :key")
    suspend fun deleteAchievement(profileId: Long, key: String)
}
