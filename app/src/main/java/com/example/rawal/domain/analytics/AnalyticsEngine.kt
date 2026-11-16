package com.example.rawal.domain.analytics

import com.example.rawal.data.local.entity.ActivityItemEntity
import com.example.rawal.data.local.entity.CategoryEntity
import com.example.rawal.data.local.entity.DailyRecordEntity
import com.example.rawal.data.local.entity.WeeklyGoalEntity
import com.example.rawal.domain.calendar.RawalDate
import kotlin.math.roundToInt

enum class ReportPeriod { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }

data class CategoryDurationStat(
    val categoryId: Long,
    val nameEn: String,
    val nameFa: String,
    val colorHex: Long,
    val totalMinutes: Int,
    val completedCount: Int
)

data class DailyTrendPoint(
    val date: String,
    val score: Int,
    val completedMinutes: Int,
    val wastedMinutes: Int,
    val sleepMinutes: Int?,
    val satisfactionValue: Float? // 1f = Satisfied, 0f = Not satisfied
)

data class PeriodReport(
    val startDate: String,
    val endDate: String,
    val recordedDaysCount: Int,
    val totalActivities: Int,
    val completedActivities: Int,
    val incompleteActivities: Int,
    val averageScore: Float,
    val satisfactionRate: Float?, // 0f..100f percentage of reviewed days satisfied
    val totalActivityMinutes: Int,
    val averageActivityDurationMinutes: Float,
    val averageSleepMinutes: Float?,
    val averageWastedMinutes: Float,
    val categoryBreakdown: List<CategoryDurationStat>,
    val dailyPoints: List<DailyTrendPoint>,
    val bestDay: DailyRecordEntity?,
    val lowestDay: DailyRecordEntity?
)

data class WeeklyTextAnalysis(
    val hasSufficientData: Boolean,
    val strengthsEn: List<String>,
    val strengthsFa: List<String>,
    val mostCompletedActivity: String?,
    val leastCompletedActivity: String?,
    val scoreDeltaFromPreviousWeek: Float?,
    val trendDirection: String, // "IMPROVING", "STEADY", "DECLINING", "INSUFFICIENT"
    val suggestionsEn: List<String>,
    val suggestionsFa: List<String>
)

data class StreakHistoryItem(
    val startDate: String,
    val endDate: String,
    val lengthDays: Int
)

data class StreakSummary(
    val currentStreak: Int,
    val longestStreak: Int,
    val threshold: Int,
    val history: List<StreakHistoryItem>
)

data class CategoryBestRecord(
    val categoryId: Long,
    val nameEn: String,
    val nameFa: String,
    val colorHex: Long,
    val bestSingleDayMinutes: Int,
    val bestDayDate: String
)

data class PersonalRecords(
    val bestDayDate: String?,
    val bestDayScore: Int,
    val bestWeekStart: String?,
    val bestWeekAvgScore: Float,
    val longestReadingMinutes: Int,
    val longestReadingDate: String?,
    val longestActivityTitle: String?,
    val longestActivityMinutes: Int,
    val longestActivityDate: String?,
    val longestStreakDays: Int,
    val categoryBests: List<CategoryBestRecord>
)

data class GoalProgress(
    val goal: WeeklyGoalEntity,
    val currentValue: Int,
    val progressFraction: Float
)

data class ActivityHistoryGroup(
    val title: String,
    val categoryId: Long,
    val sessions: List<ActivityItemEntity>,
    val completedSessions: Int,
    val totalMinutes: Int,
    val averageMinutes: Float,
    val lastSevenDaysMinutes: Int,
    val lastThirtyDaysMinutes: Int
)

data class SuggestedReminders(
    val missingDurationHour: Int,
    val missingDurationMinute: Int,
    val endOfDayHour: Int,
    val endOfDayMinute: Int,
    val basedOnDaysCount: Int
)

object AnalyticsEngine {

    fun buildPeriodReport(
        startDate: String,
        endDate: String,
        records: List<DailyRecordEntity>,
        activities: List<ActivityItemEntity>,
        categories: List<CategoryEntity>
    ): PeriodReport {
        val periodRecords = records.filter { it.date in startDate..endDate }.sortedBy { it.date }
        val periodActivities = activities.filter { it.date in startDate..endDate }
        val activeRecords = periodRecords.filter { rec ->
            rec.startupCompleted || rec.reviewCompleted || periodActivities.any { it.date == rec.date }
        }

        val completed = periodActivities.filter { it.isCompleted }
        val incomplete = periodActivities.filter { !it.isCompleted }
        val totalMinutes = completed.sumOf { it.durationMinutes }
        val avgActivityDur = if (completed.isNotEmpty()) {
            totalMinutes.toFloat() / completed.size.toFloat()
        } else 0f

        val avgScore = if (activeRecords.isNotEmpty()) {
            activeRecords.map { it.calculatedScore }.average().toFloat()
        } else 0f

        val reviewedDays = activeRecords.filter { it.satisfaction != null }
        val satisfactionRate = if (reviewedDays.isNotEmpty()) {
            (reviewedDays.count { it.satisfaction == "SATISFIED" }.toFloat() / reviewedDays.size.toFloat()) * 100f
        } else null

        val sleepDurations = activeRecords.mapNotNull {
            RawalDate.calculateSleepMinutes(it.wakeUpHour, it.wakeUpMinute, it.sleepHour, it.sleepMinute)
        }
        val avgSleep = if (sleepDurations.isNotEmpty()) sleepDurations.average().toFloat() else null

        val avgWasted = if (activeRecords.isNotEmpty()) {
            activeRecords.map { it.wastedMinutes }.average().toFloat()
        } else 0f

        val catMap = categories.associateBy { it.id }
        val catBreakdown = completed
            .groupBy { it.categoryId }
            .map { (catId, list) ->
                val cat = catMap[catId]
                CategoryDurationStat(
                    categoryId = catId,
                    nameEn = cat?.nameEn ?: "Other",
                    nameFa = cat?.nameFa ?: "سایر",
                    colorHex = cat?.colorHex ?: 0xFF475569,
                    totalMinutes = list.sumOf { it.durationMinutes },
                    completedCount = list.size
                )
            }
            .sortedByDescending { it.totalMinutes }

        val dailyPoints = activeRecords.map { rec ->
            val dayCompletedMin = completed.filter { it.date == rec.date }.sumOf { it.durationMinutes }
            DailyTrendPoint(
                date = rec.date,
                score = rec.calculatedScore,
                completedMinutes = dayCompletedMin,
                wastedMinutes = rec.wastedMinutes,
                sleepMinutes = RawalDate.calculateSleepMinutes(rec.wakeUpHour, rec.wakeUpMinute, rec.sleepHour, rec.sleepMinute),
                satisfactionValue = when (rec.satisfaction) {
                    "SATISFIED" -> 1f
                    "NOT_SATISFIED" -> 0f
                    else -> null
                }
            )
        }

        val bestDay = activeRecords.maxByOrNull { it.calculatedScore }
        val lowestDay = activeRecords.minByOrNull { it.calculatedScore }

        return PeriodReport(
            startDate = startDate,
            endDate = endDate,
            recordedDaysCount = activeRecords.size,
            totalActivities = periodActivities.size,
            completedActivities = completed.size,
            incompleteActivities = incomplete.size,
            averageScore = avgScore,
            satisfactionRate = satisfactionRate,
            totalActivityMinutes = totalMinutes,
            averageActivityDurationMinutes = avgActivityDur,
            averageSleepMinutes = avgSleep,
            averageWastedMinutes = avgWasted,
            categoryBreakdown = catBreakdown,
            dailyPoints = dailyPoints,
            bestDay = bestDay,
            lowestDay = lowestDay
        )
    }

    fun generateWeeklyTextAnalysis(
        currentWeek: PeriodReport,
        previousWeek: PeriodReport,
        activitiesInCurrentWeek: List<ActivityItemEntity>
    ): WeeklyTextAnalysis {
        if (currentWeek.recordedDaysCount < 2 || currentWeek.totalActivities == 0) {
            return WeeklyTextAnalysis(
                hasSufficientData = false,
                strengthsEn = emptyList(),
                strengthsFa = emptyList(),
                mostCompletedActivity = null,
                leastCompletedActivity = null,
                scoreDeltaFromPreviousWeek = null,
                trendDirection = "INSUFFICIENT",
                suggestionsEn = listOf("Record at least 2 days of activities and sleep times this week to unlock data-driven weekly insights."),
                suggestionsFa = listOf("برای دریافت تحلیل دقیق هفتگی، حداقل ۲ روز فعالیت و زمان خواب خود را در این هفته ثبت کنید.")
            )
        }

        val groupedByTitle = activitiesInCurrentWeek.groupBy { it.title.trim() }
        val mostCompleted = groupedByTitle
            .mapValues { (_, list) -> list.count { it.isCompleted } }
            .filterValues { it > 0 }
            .maxByOrNull { it.value }?.key

        val leastCompleted = groupedByTitle
            .mapValues { (_, list) -> list.count { !it.isCompleted } }
            .filterValues { it > 0 }
            .maxByOrNull { it.value }?.key

        val scoreDelta = if (previousWeek.recordedDaysCount > 0) {
            currentWeek.averageScore - previousWeek.averageScore
        } else null

        val trend = when {
            scoreDelta == null -> "STEADY"
            scoreDelta >= 5f -> "IMPROVING"
            scoreDelta <= -5f -> "DECLINING"
            else -> "STEADY"
        }

        val strengthsEn = mutableListOf<String>()
        val strengthsFa = mutableListOf<String>()
        val suggestionsEn = mutableListOf<String>()
        val suggestionsFa = mutableListOf<String>()

        val completionRate = (currentWeek.completedActivities.toFloat() / currentWeek.totalActivities.toFloat()) * 100f
        if (completionRate >= 75f) {
            strengthsEn.add("Strong execution: ${completionRate.roundToInt()}% of planned activities were completed.")
            strengthsFa.add("اجرای قوی: ${completionRate.roundToInt()}٪ از فعالیت‌های برنامه‌ریزی‌شده تکمیل شدند.")
        } else {
            suggestionsEn.add("Completion rate was ${completionRate.roundToInt()}%. Try keeping daily tasks focused and realistic.")
            suggestionsFa.add("نرخ تکمیل فعالیت‌ها ${completionRate.roundToInt()}٪ بود. تعداد کارهای روزانه را متمرکزتر و واقع‌بینانه‌تر انتخاب کنید.")
        }

        val topCat = currentWeek.categoryBreakdown.firstOrNull()
        if (topCat != null && topCat.totalMinutes > 0) {
            strengthsEn.add("Top focus area: ${topCat.nameEn} (${topCat.totalMinutes} min).")
            strengthsFa.add("بیشترین تمرکز روی دسته‌بندی «${topCat.nameFa}» (${topCat.totalMinutes} دقیقه) بوده است.")
        }

        if (currentWeek.averageWastedMinutes <= 45f) {
            strengthsEn.add("Low wasted time averaging ${currentWeek.averageWastedMinutes.roundToInt()} min/day.")
            strengthsFa.add("زمان هدررفته پایین با میانگین ${currentWeek.averageWastedMinutes.roundToInt()} دقیقه در روز.")
        } else {
            suggestionsEn.add("Average wasted time is ${currentWeek.averageWastedMinutes.roundToInt()} min/day. Identify your main afternoon distractions.")
            suggestionsFa.add("میانگین زمان هدررفته ${currentWeek.averageWastedMinutes.roundToInt()} دقیقه در روز است؛ عوامل حواس‌پرتی را شناسایی کنید.")
        }

        val avgSleep = currentWeek.averageSleepMinutes
        if (avgSleep != null && avgSleep < 420f) {
            suggestionsEn.add("Average sleep is ${(avgSleep / 60f).let { String.format(java.util.Locale.US, "%.1f", it) }}h. Aiming for 7–8 hours can lift daily energy.")
            suggestionsFa.add("میانگین خواب شما ${(avgSleep / 60f).let { String.format(java.util.Locale.US, "%.1f", it) }} ساعت است. هدف‌گذاری برای ۷ تا ۸ ساعت خواب انرژی روزانه را افزایش می‌دهد.")
        } else if (avgSleep != null) {
            strengthsEn.add("Healthy sleep routine averaging ${(avgSleep / 60f).let { String.format(java.util.Locale.US, "%.1f", it) }}h per recorded night.")
            strengthsFa.add("الگوی خواب متعادل با میانگین ${(avgSleep / 60f).let { String.format(java.util.Locale.US, "%.1f", it) }} ساعت در شب‌های ثبت‌شده.")
        }

        if (leastCompleted != null) {
            suggestionsEn.add("\"$leastCompleted\" was left incomplete most often—try scheduling it outside school/unavailable hours.")
            suggestionsFa.add("فعالیت «$leastCompleted» بیشتر از بقیه ناتمام مانده است؛ آن را در ساعات آزاد خود برنامه‌ریزی کنید.")
        }

        if (suggestionsEn.isEmpty()) {
            suggestionsEn.add("Keep maintaining your steady daily review and sleep logging rhythm next week.")
            suggestionsFa.add("همین روند منظم مرور پایان روز و ثبت ساعت خواب را در هفته آینده ادامه دهید.")
        }

        return WeeklyTextAnalysis(
            hasSufficientData = true,
            strengthsEn = strengthsEn,
            strengthsFa = strengthsFa,
            mostCompletedActivity = mostCompleted,
            leastCompletedActivity = leastCompleted,
            scoreDeltaFromPreviousWeek = scoreDelta,
            trendDirection = trend,
            suggestionsEn = suggestionsEn,
            suggestionsFa = suggestionsFa
        )
    }

    fun calculateStreaks(
        records: List<DailyRecordEntity>,
        threshold: Int,
        todayIso: String = RawalDate.today().toIso()
    ): StreakSummary {
        val passingDates = records
            .filter { it.calculatedScore >= threshold }
            .map { it.date }
            .distinct()
            .sorted()

        if (passingDates.isEmpty()) {
            return StreakSummary(0, 0, threshold, emptyList())
        }

        val runs = mutableListOf<StreakHistoryItem>()
        var runStart = passingDates.first()
        var prev = passingDates.first()
        var count = 1

        for (i in 1 until passingDates.size) {
            val current = passingDates[i]
            val gap = RawalDate.daysBetween(prev, current)
            if (gap == 1) {
                count++
                prev = current
            } else {
                runs.add(StreakHistoryItem(runStart, prev, count))
                runStart = current
                prev = current
                count = 1
            }
        }
        runs.add(StreakHistoryItem(runStart, prev, count))

        val longest = runs.maxOf { it.lengthDays }
        val lastRun = runs.last()
        val gapToToday = RawalDate.daysBetween(lastRun.endDate, todayIso)
        // Current streak stays active if the latest passing day is today or yesterday
        val current = if (gapToToday in 0..1) lastRun.lengthDays else 0

        return StreakSummary(
            currentStreak = current,
            longestStreak = longest,
            threshold = threshold,
            history = runs.reversed()
        )
    }

    fun calculatePersonalRecords(
        records: List<DailyRecordEntity>,
        activities: List<ActivityItemEntity>,
        categories: List<CategoryEntity>,
        streakSummary: StreakSummary,
        readingCategoryIds: Set<Long>
    ): PersonalRecords {
        val activeRecords = records.filter { rec ->
            rec.startupCompleted || rec.reviewCompleted || activities.any { it.date == rec.date }
        }
        val bestDay = activeRecords.maxByOrNull { it.calculatedScore }

        val bestWeekEntry = activeRecords
            .groupBy { rec -> RawalDate.parseIso(rec.date)?.weekStartSaturday()?.toIso() ?: rec.date }
            .mapValues { (_, list) -> list.map { it.calculatedScore }.average().toFloat() }
            .maxByOrNull { it.value }

        val completed = activities.filter { it.isCompleted }
        val longestReading = completed
            .filter { it.fixedKey == "FREE_READING" || it.categoryId in readingCategoryIds }
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
            .maxByOrNull { it.value }

        val longestActivity = completed.maxByOrNull { it.durationMinutes }

        val catBests = categories.mapNotNull { cat ->
            val bestForCat = completed
                .filter { it.categoryId == cat.id }
                .groupBy { it.date }
                .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
                .maxByOrNull { it.value }
            if (bestForCat != null && bestForCat.value > 0) {
                CategoryBestRecord(
                    categoryId = cat.id,
                    nameEn = cat.nameEn,
                    nameFa = cat.nameFa,
                    colorHex = cat.colorHex,
                    bestSingleDayMinutes = bestForCat.value,
                    bestDayDate = bestForCat.key
                )
            } else null
        }

        return PersonalRecords(
            bestDayDate = bestDay?.date,
            bestDayScore = bestDay?.calculatedScore ?: 0,
            bestWeekStart = bestWeekEntry?.key,
            bestWeekAvgScore = bestWeekEntry?.value ?: 0f,
            longestReadingMinutes = longestReading?.value ?: 0,
            longestReadingDate = longestReading?.key,
            longestActivityTitle = longestActivity?.title,
            longestActivityMinutes = longestActivity?.durationMinutes ?: 0,
            longestActivityDate = longestActivity?.date,
            longestStreakDays = streakSummary.longestStreak,
            categoryBests = catBests
        )
    }

    fun calculateGoalProgresses(
        goals: List<WeeklyGoalEntity>,
        activities: List<ActivityItemEntity>
    ): List<GoalProgress> {
        return goals.map { goal ->
            val weekEnd = RawalDate.parseIso(goal.weekStartDate)?.plusDays(6)?.toIso() ?: goal.weekStartDate
            val matching = activities.filter { act ->
                act.isCompleted &&
                    act.date in goal.weekStartDate..weekEnd &&
                    when (goal.targetType) {
                        "CATEGORY" -> act.categoryId == goal.categoryId
                        "ACTIVITY" -> !goal.activityTitleMatch.isNullOrBlank() &&
                            act.title.contains(goal.activityTitleMatch, ignoreCase = true)
                        else -> false
                    }
            }
            val currentVal = when (goal.metricType) {
                "SESSIONS" -> matching.size
                else -> matching.sumOf { it.durationMinutes }
            }
            val fraction = if (goal.targetValue > 0) {
                (currentVal.toFloat() / goal.targetValue.toFloat()).coerceIn(0f, 1f)
            } else 0f
            GoalProgress(goal, currentVal, fraction)
        }
    }

    fun buildActivityHistoryGroups(
        activities: List<ActivityItemEntity>,
        todayIso: String = RawalDate.today().toIso()
    ): List<ActivityHistoryGroup> {
        val today = RawalDate.parseIso(todayIso) ?: RawalDate.today()
        val sevenDaysAgo = today.minusDays(6).toIso()
        val thirtyDaysAgo = today.minusDays(29).toIso()

        return activities
            .filter { it.title.isNotBlank() }
            .groupBy { it.title.trim() }
            .map { (title, list) ->
                val completed = list.filter { it.isCompleted }
                val totalMin = completed.sumOf { it.durationMinutes }
                val avgMin = if (completed.isNotEmpty()) totalMin.toFloat() / completed.size.toFloat() else 0f
                val last7 = completed.filter { it.date in sevenDaysAgo..todayIso }.sumOf { it.durationMinutes }
                val last30 = completed.filter { it.date in thirtyDaysAgo..todayIso }.sumOf { it.durationMinutes }
                ActivityHistoryGroup(
                    title = title,
                    categoryId = list.first().categoryId,
                    sessions = list.sortedByDescending { it.date },
                    completedSessions = completed.size,
                    totalMinutes = totalMin,
                    averageMinutes = avgMin,
                    lastSevenDaysMinutes = last7,
                    lastThirtyDaysMinutes = last30
                )
            }
            .sortedByDescending { it.sessions.size }
    }

    fun suggestReminderTimes(records: List<DailyRecordEntity>): SuggestedReminders {
        val sleepTimes = records.mapNotNull { rec ->
            val h = rec.sleepHour ?: return@mapNotNull null
            val m = rec.sleepMinute ?: 0
            // Normalize late-night times (00:00..04:59) as 24+h for averaging
            val adjustedH = if (h < 5) h + 24 else h
            adjustedH * 60 + m
        }
        if (sleepTimes.isEmpty()) {
            return SuggestedReminders(20, 30, 21, 30, 0)
        }
        val avgSleepMin = sleepTimes.average().roundToInt()
        val reviewMin = (avgSleepMin - 45).mod(24 * 60)
        val durationCheckMin = (avgSleepMin - 90).mod(24 * 60)
        return SuggestedReminders(
            missingDurationHour = durationCheckMin / 60,
            missingDurationMinute = durationCheckMin % 60,
            endOfDayHour = reviewMin / 60,
            endOfDayMinute = reviewMin % 60,
            basedOnDaysCount = sleepTimes.size
        )
    }
}
