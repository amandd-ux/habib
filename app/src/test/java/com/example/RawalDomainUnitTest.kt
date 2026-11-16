package com.example

import com.example.rawal.data.local.entity.ActivityItemEntity
import com.example.rawal.data.local.entity.DailyRecordEntity
import com.example.rawal.data.local.entity.WeeklyGoalEntity
import com.example.rawal.domain.analytics.AnalyticsEngine
import com.example.rawal.domain.calendar.RawalDate
import com.example.rawal.domain.calendar.ScheduleHelper
import com.example.rawal.domain.i18n.AppLanguage
import com.example.rawal.domain.i18n.AppStringsProvider
import com.example.rawal.domain.scoring.AchievementCatalog
import com.example.rawal.domain.scoring.DailyScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RawalDomainUnitTest {

    @Test
    fun `daily scoring stays fair and does not reward creating more tasks`() {
        val record = DailyRecordEntity(
            profileId = 1,
            date = "2026-11-16",
            wakeUpHour = 7,
            wakeUpMinute = 0,
            sleepHour = 23,
            sleepMinute = 0,
            wastedMinutes = 15,
            satisfaction = "SATISFIED",
            startupCompleted = true,
            reviewCompleted = true
        )
        val fourDone = (1..4).map {
            ActivityItemEntity(id = it.toLong(), profileId = 1, date = "2026-11-16", categoryId = 1, title = "Task $it", durationMinutes = 20, isCompleted = true, isFixedDaily = true)
        }
        val twelveDone = (1..12).map {
            ActivityItemEntity(id = it.toLong(), profileId = 1, date = "2026-11-16", categoryId = 1, title = "Task $it", durationMinutes = 20, isCompleted = true, isFixedDaily = true)
        }

        val score4 = DailyScoringEngine.calculate(record, fourDone).totalScore
        val score12 = DailyScoringEngine.calculate(record, twelveDone).totalScore
        assertEquals(score4, score12)
        assertTrue(score4 in 90..100)
    }

    @Test
    fun `empty day yields zero score`() {
        val score = DailyScoringEngine.calculate(null, emptyList())
        assertEquals(0, score.totalScore)
    }

    @Test
    fun `schedule helper returns exact unavailable hours per weekday`() {
        val sat = ScheduleHelper.getScheduleForDate(RawalDate(2026, 11, 14))
        assertTrue(sat.isSchoolDay)
        assertEquals(11, sat.unavailableStartHour)
        assertEquals(30, sat.unavailableStartMinute)
        assertEquals(18, sat.unavailableEndHour)

        val tue = ScheduleHelper.getScheduleForDate(RawalDate(2026, 11, 17))
        assertTrue(tue.isSchoolDay)
        assertEquals(6, tue.unavailableStartHour)
        assertEquals(30, tue.unavailableStartMinute)
        assertEquals(13, tue.unavailableEndHour)

        val thu = ScheduleHelper.getScheduleForDate(RawalDate(2026, 11, 19))
        assertFalse(thu.isSchoolDay)
        assertEquals(7, thu.availableFromHour)
    }

    @Test
    fun `sleep duration handles overnight bedtime correctly`() {
        val mins = RawalDate.calculateSleepMinutes(wakeHour = 6, wakeMinute = 30, sleepHour = 23, sleepMinute = 0)
        assertEquals(450, mins)
    }

    @Test
    fun `streaks and weekly analysis reflect real records and report insufficient data honestly`() {
        val emptyReport = AnalyticsEngine.buildPeriodReport("2026-11-14", "2026-11-20", emptyList(), emptyList(), emptyList())
        val emptyAnalysis = AnalyticsEngine.generateWeeklyTextAnalysis(emptyReport, emptyReport, emptyList())
        assertFalse(emptyAnalysis.hasSufficientData)

        val records = listOf(
            DailyRecordEntity(profileId = 1, date = "2026-11-14", calculatedScore = 80, startupCompleted = true),
            DailyRecordEntity(profileId = 1, date = "2026-11-15", calculatedScore = 72, startupCompleted = true),
            DailyRecordEntity(profileId = 1, date = "2026-11-16", calculatedScore = 75, startupCompleted = true)
        )
        val streaks = AnalyticsEngine.calculateStreaks(records, threshold = 70, todayIso = "2026-11-16")
        assertEquals(3, streaks.currentStreak)
        assertEquals(3, streaks.longestStreak)
    }

    @Test
    fun `weekly goal progress and achievements unlock accurately`() {
        val goal = WeeklyGoalEntity(
            id = 1,
            profileId = 1,
            title = "Read 2h",
            targetType = "CATEGORY",
            categoryId = 2,
            metricType = "MINUTES",
            targetValue = 120,
            weekStartDate = "2026-11-14"
        )
        val acts = listOf(
            ActivityItemEntity(id = 1, profileId = 1, date = "2026-11-14", categoryId = 2, title = "Free Reading", durationMinutes = 70, isCompleted = true, fixedKey = "FREE_READING"),
            ActivityItemEntity(id = 2, profileId = 1, date = "2026-11-15", categoryId = 2, title = "Free Reading", durationMinutes = 55, isCompleted = true, fixedKey = "FREE_READING")
        )
        val progresses = AnalyticsEngine.calculateGoalProgresses(listOf(goal), acts)
        assertEquals(125, progresses.first().currentValue)
        assertEquals(1f, progresses.first().progressFraction, 0.001f)

        val unlocked = AchievementCatalog.evaluateUnlockedKeys(
            records = listOf(DailyRecordEntity(profileId = 1, date = "2026-11-14", calculatedScore = 92, startupCompleted = true)),
            activities = acts,
            streakSummary = AnalyticsEngine.calculateStreaks(emptyList(), 65),
            goalProgresses = progresses,
            readingCategoryIds = setOf(2L)
        )
        assertTrue("FIRST_DAY" in unlocked)
        assertTrue("GOAL_ACHIEVER" in unlocked)
        assertTrue("HIGHEST_SCORE_90" in unlocked)
        assertTrue("DEEP_READER_120" in unlocked)
    }

    @Test
    fun `persian digits and jalali date formatting work cleanly`() {
        assertEquals("۱۲:۳۰", AppStringsProvider.toLocalizedDigits("12:30", AppLanguage.FA))
        val formattedFa = AppStringsProvider.formatDate(AppLanguage.FA, "2026-11-16")
        assertNotNull(formattedFa)
        assertTrue(formattedFa.contains("۱۴۰۵"))
    }
}
