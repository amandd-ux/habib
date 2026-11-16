package com.example.rawal.domain.scoring

import com.example.rawal.data.local.entity.ActivityItemEntity
import com.example.rawal.data.local.entity.DailyRecordEntity
import com.example.rawal.data.local.entity.WeeklyGoalEntity
import com.example.rawal.domain.analytics.GoalProgress
import com.example.rawal.domain.analytics.StreakSummary
import kotlin.math.roundToInt

data class ScoreBreakdown(
    val totalScore: Int,
    val completionPoints: Int,       // max 60
    val consistencyPoints: Int,      // max 15
    val timeAwarenessPoints: Int,    // max 15
    val satisfactionPoints: Int      // max 10
)

object DailyScoringEngine {
    /**
     * Fair, transparent 0-100 scoring formula that does NOT reward creating extra tasks:
     * 1. Completion Ratio (0..60): (completedCount / totalCount) * 60
     * 2. Core Routine & Effort (0..15): up to 10 pts for core fixed habits completed ratio + up to 5 pts for >= 30m logged duration
     * 3. Time Awareness & Low Wasted Time (0..15): +5 pts if wake & sleep times recorded, +0..10 pts inversely proportional to wastedMinutes (10 pts at 0 min, 0 pts at >=180 min)
     * 4. End-of-Day Satisfaction (0..10): +10 if SATISFIED, +3 if NOT_SATISFIED (reflecting honest reflection), 0 if unreviewed
     */
    fun calculate(record: DailyRecordEntity?, activities: List<ActivityItemEntity>): ScoreBreakdown {
        if (record == null && activities.isEmpty()) {
            return ScoreBreakdown(0, 0, 0, 0, 0)
        }

        val totalCount = activities.size
        val completedCount = activities.count { it.isCompleted }

        val completionPts = if (totalCount > 0) {
            ((completedCount.toFloat() / totalCount.toFloat()) * 60f).roundToInt().coerceIn(0, 60)
        } else {
            0
        }

        val fixedActivities = activities.filter { it.isFixedDaily }
        val fixedRatioPts = if (fixedActivities.isNotEmpty()) {
            val fixedCompleted = fixedActivities.count { it.isCompleted }
            ((fixedCompleted.toFloat() / fixedActivities.size.toFloat()) * 10f).roundToInt()
        } else if (completedCount > 0) {
            5
        } else {
            0
        }
        val totalLoggedMinutes = activities.filter { it.isCompleted }.sumOf { it.durationMinutes }
        val durationEffortPts = when {
            totalLoggedMinutes >= 60 -> 5
            totalLoggedMinutes >= 30 -> 4
            totalLoggedMinutes > 0 -> 2
            else -> 0
        }
        val consistencyPts = (fixedRatioPts + durationEffortPts).coerceIn(0, 15)

        val hasSleepAndWake = record?.wakeUpHour != null && record.sleepHour != null
        val sleepRecordPts = if (hasSleepAndWake) 5 else 0
        val wasted = (record?.wastedMinutes ?: 0).coerceAtLeast(0)
        val wastedPts = if (record != null && (activities.isNotEmpty() || record.startupCompleted || record.reviewCompleted)) {
            (10f - (wasted / 18f)).roundToInt().coerceIn(0, 10)
        } else {
            0
        }
        val timeAwarenessPts = (sleepRecordPts + wastedPts).coerceIn(0, 15)

        val satisfactionPts = when (record?.satisfaction) {
            "SATISFIED" -> 10
            "NOT_SATISFIED" -> 3
            else -> 0
        }

        val total = (completionPts + consistencyPts + timeAwarenessPts + satisfactionPts).coerceIn(0, 100)
        return ScoreBreakdown(
            totalScore = total,
            completionPoints = completionPts,
            consistencyPoints = consistencyPts,
            timeAwarenessPoints = timeAwarenessPts,
            satisfactionPoints = satisfactionPts
        )
    }
}

data class AchievementDefinition(
    val key: String,
    val titleEn: String,
    val titleFa: String,
    val descEn: String,
    val descFa: String,
    val iconKey: String
)

object AchievementCatalog {
    val all: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            key = "FIRST_DAY",
            titleEn = "First Recorded Day",
            titleFa = "اولین روز ثبت‌شده",
            descEn = "Log your first day of activities in RAWAL.",
            descFa = "اولین روز فعالیت خود را در روال ثبت کنید.",
            iconKey = "flag"
        ),
        AchievementDefinition(
            key = "THREE_DAY_FLOW",
            titleEn = "Three-Day Rhythm",
            titleFa = "ریتم سه‌روزه",
            descEn = "Maintain an organization streak for 3 consecutive days.",
            descFa = "زنجیره نظم خود را ۳ روز متوالی حفظ کنید.",
            iconKey = "bolt"
        ),
        AchievementDefinition(
            key = "SEVEN_ACTIVE_DAYS",
            titleEn = "Seven Active Days",
            titleFa = "هفت روز فعال",
            descEn = "Record meaningful activity on at least 7 separate days.",
            descFa = "حداقل در ۷ روز مختلف فعالیت ثبت کنید.",
            iconKey = "calendar_month"
        ),
        AchievementDefinition(
            key = "WEEKLY_CONSISTENCY",
            titleEn = "Weekly Consistency",
            titleFa = "پایداری هفتگی",
            descEn = "Reach a 7-day organization streak.",
            descFa = "به زنجیره نظم ۷ روزه دست یابید.",
            iconKey = "local_fire_department"
        ),
        AchievementDefinition(
            key = "GOAL_ACHIEVER",
            titleEn = "Goal Achiever",
            titleFa = "تحقق هدف هفتگی",
            descEn = "Complete 100% of any weekly goal.",
            descFa = "یکی از اهداف هفتگی خود را به ۱۰۰٪ برسانید.",
            iconKey = "track_changes"
        ),
        AchievementDefinition(
            key = "HIGHEST_SCORE_90",
            titleEn = "Peak Focus (90+)",
            titleFa = "اوج تمرکز (+۹۰)",
            descEn = "Achieve a daily score of 90 or higher.",
            descFa = "امتیاز روزانه ۹۰ یا بیشتر کسب کنید.",
            iconKey = "workspace_premium"
        ),
        AchievementDefinition(
            key = "SLEEP_TRACKER_5",
            titleEn = "Sleep Steward",
            titleFa = "منظم در خواب",
            descEn = "Record both wake-up and bedtime on at least 5 days.",
            descFa = "ساعت بیداری و خواب را حداقل در ۵ روز ثبت کنید.",
            iconKey = "bedtime"
        ),
        AchievementDefinition(
            key = "DEEP_READER_120",
            titleEn = "Bookworm",
            titleFa = "همدم کتاب",
            descEn = "Accumulate at least 120 minutes of reading.",
            descFa = "در مجموع حداقل ۱۲۰ دقیقه مطالعه ثبت کنید.",
            iconKey = "menu_book"
        ),
        AchievementDefinition(
            key = "CENTURY_LOGGER",
            titleEn = "1,000 Minutes Logged",
            titleFa = "ثبت ۱۰۰۰ دقیقه",
            descEn = "Log 1,000 total minutes of completed activities.",
            descFa = "در مجموع ۱۰۰۰ دقیقه فعالیت تکمیل‌شده ثبت کنید.",
            iconKey = "timer"
        ),
        AchievementDefinition(
            key = "THIRTY_DAYS_JOURNEY",
            titleEn = "Monthly Pathfinder",
            titleFa = "مسیرساز ماهانه",
            descEn = "Record 30 days of personal organization.",
            descFa = "۳۰ روز نظم و پیگیری شخصی ثبت کنید.",
            iconKey = "emoji_events"
        )
    )

    fun evaluateUnlockedKeys(
        records: List<DailyRecordEntity>,
        activities: List<ActivityItemEntity>,
        streakSummary: StreakSummary,
        goalProgresses: List<GoalProgress>,
        readingCategoryIds: Set<Long>
    ): Set<String> {
        val unlocked = mutableSetOf<String>()
        val activeDays = records.filter { rec ->
            rec.startupCompleted || rec.reviewCompleted || activities.any { it.date == rec.date }
        }

        if (activeDays.isNotEmpty() || activities.isNotEmpty()) unlocked.add("FIRST_DAY")
        if (streakSummary.longestStreak >= 3) unlocked.add("THREE_DAY_FLOW")
        if (activeDays.size >= 7) unlocked.add("SEVEN_ACTIVE_DAYS")
        if (streakSummary.longestStreak >= 7) unlocked.add("WEEKLY_CONSISTENCY")
        if (goalProgresses.any { it.progressFraction >= 1f }) unlocked.add("GOAL_ACHIEVER")
        if (records.any { it.calculatedScore >= 90 }) unlocked.add("HIGHEST_SCORE_90")

        val sleepDays = records.count { it.wakeUpHour != null && it.sleepHour != null }
        if (sleepDays >= 5) unlocked.add("SLEEP_TRACKER_5")

        val readingMinutes = activities.filter {
            it.isCompleted && (it.fixedKey == "FREE_READING" || it.categoryId in readingCategoryIds)
        }.sumOf { it.durationMinutes }
        if (readingMinutes >= 120) unlocked.add("DEEP_READER_120")

        val totalCompletedMinutes = activities.filter { it.isCompleted }.sumOf { it.durationMinutes }
        if (totalCompletedMinutes >= 1000) unlocked.add("CENTURY_LOGGER")

        if (activeDays.size >= 30) unlocked.add("THIRTY_DAYS_JOURNEY")

        return unlocked
    }
}
