package com.example.rawal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.rawal.data.local.dao.AchievementDao
import com.example.rawal.data.local.dao.ActivityDao
import com.example.rawal.data.local.dao.CategoryDao
import com.example.rawal.data.local.dao.DailyRecordDao
import com.example.rawal.data.local.dao.ProfileDao
import com.example.rawal.data.local.dao.ProfileSettingsDao
import com.example.rawal.data.local.dao.WeeklyGoalDao
import com.example.rawal.data.local.dao.WeeklyReviewDao
import com.example.rawal.data.local.entity.ActivityItemEntity
import com.example.rawal.data.local.entity.CategoryEntity
import com.example.rawal.data.local.entity.DailyRecordEntity
import com.example.rawal.data.local.entity.ProfileEntity
import com.example.rawal.data.local.entity.ProfileSettingsEntity
import com.example.rawal.data.local.entity.UnlockedAchievementEntity
import com.example.rawal.data.local.entity.WeeklyGoalEntity
import com.example.rawal.data.local.entity.WeeklyReviewEntity

@Database(
    entities = [
        ProfileEntity::class,
        ProfileSettingsEntity::class,
        CategoryEntity::class,
        DailyRecordEntity::class,
        ActivityItemEntity::class,
        WeeklyGoalEntity::class,
        WeeklyReviewEntity::class,
        UnlockedAchievementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RawalDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun profileSettingsDao(): ProfileSettingsDao
    abstract fun categoryDao(): CategoryDao
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun activityDao(): ActivityDao
    abstract fun weeklyGoalDao(): WeeklyGoalDao
    abstract fun weeklyReviewDao(): WeeklyReviewDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: RawalDatabase? = null

        fun getInstance(context: Context): RawalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RawalDatabase::class.java,
                    "rawal_database"
                ).build().also { INSTANCE = it }
            }
        }

        fun defaultCategoriesForProfile(profileId: Long): List<CategoryEntity> = listOf(
            CategoryEntity(
                profileId = profileId,
                nameEn = "Education",
                nameFa = "آموزش",
                colorHex = 0xFF4F46E5,
                iconKey = "school",
                isSystem = true,
                isOptional = false,
                sortOrder = 0
            ),
            CategoryEntity(
                profileId = profileId,
                nameEn = "Reading",
                nameFa = "مطالعه",
                colorHex = 0xFF0F766E,
                iconKey = "book",
                isSystem = true,
                isOptional = false,
                sortOrder = 1
            ),
            CategoryEntity(
                profileId = profileId,
                nameEn = "Exercise",
                nameFa = "ورزش",
                colorHex = 0xFF059669,
                iconKey = "fitness",
                isSystem = true,
                isOptional = false,
                sortOrder = 2
            ),
            CategoryEntity(
                profileId = profileId,
                nameEn = "Personal Activities",
                nameFa = "کارهای شخصی",
                colorHex = 0xFFD97706,
                iconKey = "person",
                isSystem = true,
                isOptional = false,
                sortOrder = 3
            ),
            CategoryEntity(
                profileId = profileId,
                nameEn = "Skill Development",
                nameFa = "توسعه مهارت",
                colorHex = 0xFF0891B2,
                iconKey = "lightbulb",
                isSystem = false,
                isOptional = true,
                sortOrder = 4
            ),
            CategoryEntity(
                profileId = profileId,
                nameEn = "Entertainment",
                nameFa = "سرگرمی",
                colorHex = 0xFFE11D48,
                iconKey = "movie",
                isSystem = false,
                isOptional = true,
                sortOrder = 5
            ),
            CategoryEntity(
                profileId = profileId,
                nameEn = "Gaming",
                nameFa = "بازی",
                colorHex = 0xFF7C3AED,
                iconKey = "sports_esports",
                isSystem = false,
                isOptional = true,
                sortOrder = 6
            ),
            CategoryEntity(
                profileId = profileId,
                nameEn = "Other",
                nameFa = "سایر",
                colorHex = 0xFF475569,
                iconKey = "more_horiz",
                isSystem = true,
                isOptional = false,
                sortOrder = 7
            )
        )
    }
}
