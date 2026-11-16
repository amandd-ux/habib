package com.example.rawal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarKey: String = "teal_sun",
    val customAvatarUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)

@Entity(
    tableName = "profile_settings",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProfileSettingsEntity(
    @PrimaryKey val profileId: Long,
    val languageCode: String = "en", // "en" or "fa"
    val accentColorKey: String = "TEAL", // TEAL, INDIGO, EMERALD, AMBER, ROSE, SLATE
    val fontSizeScale: String = "MEDIUM", // SMALL, MEDIUM, LARGE
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val streakThreshold: Int = 65,
    val showScheduleBanner: Boolean = true,
    val showTimeSummaryCard: Boolean = true,
    val showScoreBreakdown: Boolean = true,
    val compactActivityCards: Boolean = false,
    val remindersEnabled: Boolean = false,
    val missingDurationReminderHour: Int = 20,
    val missingDurationReminderMinute: Int = 30,
    val endOfDayReminderHour: Int = 21,
    val endOfDayReminderMinute: Int = 30
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val nameEn: String,
    val nameFa: String,
    val colorHex: Long,
    val iconKey: String = "category",
    val isSystem: Boolean = false,
    val isOptional: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "daily_records",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId", "date"], unique = true)]
)
data class DailyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val date: String, // ISO YYYY-MM-DD
    val wakeUpHour: Int? = null,
    val wakeUpMinute: Int? = null,
    val sleepHour: Int? = null,
    val sleepMinute: Int? = null,
    val wastedMinutes: Int = 0,
    val satisfaction: String? = null, // "SATISFIED", "NOT_SATISFIED", null
    val mood: String? = null, // "GREAT", "GOOD", "NEUTRAL", "TIRED", "STRESSED"
    val unexpectedEvents: String = "",
    val disruptions: String = "",
    val dailyNotes: String = "",
    val startupCompleted: Boolean = false,
    val reviewCompleted: Boolean = false,
    val calculatedScore: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "activities",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId"), Index(value = ["profileId", "date"]), Index("categoryId")]
)
data class ActivityItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val date: String, // ISO YYYY-MM-DD
    val categoryId: Long,
    val title: String,
    val notes: String = "",
    val durationMinutes: Int = 0,
    val isCompleted: Boolean = false,
    val isFixedDaily: Boolean = false,
    val fixedKey: String? = null, // FREE_READING, SCHOOLWORK, SCHOOL_ATTENDANCE, BRUSH_TEETH, MORNING_EXERCISE
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "weekly_goals",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class WeeklyGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val title: String,
    val targetType: String, // "CATEGORY" or "ACTIVITY"
    val categoryId: Long? = null,
    val activityTitleMatch: String? = null,
    val metricType: String, // "MINUTES" or "SESSIONS"
    val targetValue: Int,
    val weekStartDate: String, // ISO YYYY-MM-DD (Saturday)
    val isActive: Boolean = true
)

@Entity(
    tableName = "weekly_reviews",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId", "weekStartDate"], unique = true)]
)
data class WeeklyReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val weekStartDate: String, // ISO YYYY-MM-DD (Saturday)
    val biggestWin: String = "",
    val mainChallenge: String = "",
    val nextWeekFocus: String = "",
    val energyRating: Int = 3,
    val organizationRating: Int = 3,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "unlocked_achievements",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId", "achievementKey"], unique = true)]
)
data class UnlockedAchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val achievementKey: String,
    val unlockedAt: Long = System.currentTimeMillis()
)
