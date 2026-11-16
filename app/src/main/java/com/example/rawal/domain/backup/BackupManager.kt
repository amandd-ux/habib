package com.example.rawal.domain.backup

import com.example.rawal.data.local.entity.ActivityItemEntity
import com.example.rawal.data.local.entity.CategoryEntity
import com.example.rawal.data.local.entity.DailyRecordEntity
import com.example.rawal.data.local.entity.ProfileEntity
import com.example.rawal.data.local.entity.ProfileSettingsEntity
import com.example.rawal.data.local.entity.UnlockedAchievementEntity
import com.example.rawal.data.local.entity.WeeklyGoalEntity
import com.example.rawal.data.local.entity.WeeklyReviewEntity
import org.json.JSONArray
import org.json.JSONObject

data class RawalBackupPayload(
    val version: Int,
    val exportedAt: Long,
    val activeProfileId: Long?,
    val profiles: List<ProfileEntity>,
    val settings: List<ProfileSettingsEntity>,
    val categories: List<CategoryEntity>,
    val dailyRecords: List<DailyRecordEntity>,
    val activities: List<ActivityItemEntity>,
    val weeklyGoals: List<WeeklyGoalEntity>,
    val weeklyReviews: List<WeeklyReviewEntity>,
    val unlockedAchievements: List<UnlockedAchievementEntity>
)

object BackupManager {
    private const val CURRENT_VERSION = 1

    fun exportToJson(payload: RawalBackupPayload): String {
        val root = JSONObject()
        root.put("app", "RAWAL")
        root.put("version", payload.version)
        root.put("exportedAt", payload.exportedAt)
        if (payload.activeProfileId != null) {
            root.put("activeProfileId", payload.activeProfileId)
        }

        root.put("profiles", JSONArray().apply {
            payload.profiles.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("avatarKey", p.avatarKey)
                    put("customAvatarUri", p.customAvatarUri ?: JSONObject.NULL)
                    put("createdAt", p.createdAt)
                    put("isDefault", p.isDefault)
                })
            }
        })

        root.put("settings", JSONArray().apply {
            payload.settings.forEach { s ->
                put(JSONObject().apply {
                    put("profileId", s.profileId)
                    put("languageCode", s.languageCode)
                    put("accentColorKey", s.accentColorKey)
                    put("fontSizeScale", s.fontSizeScale)
                    put("themeMode", s.themeMode)
                    put("streakThreshold", s.streakThreshold)
                    put("showScheduleBanner", s.showScheduleBanner)
                    put("showTimeSummaryCard", s.showTimeSummaryCard)
                    put("showScoreBreakdown", s.showScoreBreakdown)
                    put("compactActivityCards", s.compactActivityCards)
                    put("remindersEnabled", s.remindersEnabled)
                    put("missingDurationReminderHour", s.missingDurationReminderHour)
                    put("missingDurationReminderMinute", s.missingDurationReminderMinute)
                    put("endOfDayReminderHour", s.endOfDayReminderHour)
                    put("endOfDayReminderMinute", s.endOfDayReminderMinute)
                })
            }
        })

        root.put("categories", JSONArray().apply {
            payload.categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("profileId", c.profileId)
                    put("nameEn", c.nameEn)
                    put("nameFa", c.nameFa)
                    put("colorHex", c.colorHex)
                    put("iconKey", c.iconKey)
                    put("isSystem", c.isSystem)
                    put("isOptional", c.isOptional)
                    put("sortOrder", c.sortOrder)
                })
            }
        })

        root.put("dailyRecords", JSONArray().apply {
            payload.dailyRecords.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("profileId", r.profileId)
                    put("date", r.date)
                    put("wakeUpHour", r.wakeUpHour ?: JSONObject.NULL)
                    put("wakeUpMinute", r.wakeUpMinute ?: JSONObject.NULL)
                    put("sleepHour", r.sleepHour ?: JSONObject.NULL)
                    put("sleepMinute", r.sleepMinute ?: JSONObject.NULL)
                    put("wastedMinutes", r.wastedMinutes)
                    put("satisfaction", r.satisfaction ?: JSONObject.NULL)
                    put("mood", r.mood ?: JSONObject.NULL)
                    put("unexpectedEvents", r.unexpectedEvents)
                    put("disruptions", r.disruptions)
                    put("dailyNotes", r.dailyNotes)
                    put("startupCompleted", r.startupCompleted)
                    put("reviewCompleted", r.reviewCompleted)
                    put("calculatedScore", r.calculatedScore)
                    put("updatedAt", r.updatedAt)
                })
            }
        })

        root.put("activities", JSONArray().apply {
            payload.activities.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("profileId", a.profileId)
                    put("date", a.date)
                    put("categoryId", a.categoryId)
                    put("title", a.title)
                    put("notes", a.notes)
                    put("durationMinutes", a.durationMinutes)
                    put("isCompleted", a.isCompleted)
                    put("isFixedDaily", a.isFixedDaily)
                    put("fixedKey", a.fixedKey ?: JSONObject.NULL)
                    put("createdAt", a.createdAt)
                    put("updatedAt", a.updatedAt)
                })
            }
        })

        root.put("weeklyGoals", JSONArray().apply {
            payload.weeklyGoals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id)
                    put("profileId", g.profileId)
                    put("title", g.title)
                    put("targetType", g.targetType)
                    put("categoryId", g.categoryId ?: JSONObject.NULL)
                    put("activityTitleMatch", g.activityTitleMatch ?: JSONObject.NULL)
                    put("metricType", g.metricType)
                    put("targetValue", g.targetValue)
                    put("weekStartDate", g.weekStartDate)
                    put("isActive", g.isActive)
                })
            }
        })

        root.put("weeklyReviews", JSONArray().apply {
            payload.weeklyReviews.forEach { w ->
                put(JSONObject().apply {
                    put("id", w.id)
                    put("profileId", w.profileId)
                    put("weekStartDate", w.weekStartDate)
                    put("biggestWin", w.biggestWin)
                    put("mainChallenge", w.mainChallenge)
                    put("nextWeekFocus", w.nextWeekFocus)
                    put("energyRating", w.energyRating)
                    put("organizationRating", w.organizationRating)
                    put("updatedAt", w.updatedAt)
                })
            }
        })

        root.put("unlockedAchievements", JSONArray().apply {
            payload.unlockedAchievements.forEach { u ->
                put(JSONObject().apply {
                    put("id", u.id)
                    put("profileId", u.profileId)
                    put("achievementKey", u.achievementKey)
                    put("unlockedAt", u.unlockedAt)
                })
            }
        })

        return root.toString(2)
    }

    fun parseAndValidateJson(jsonString: String): Result<RawalBackupPayload> {
        return runCatching {
            val root = JSONObject(jsonString)
            require(root.optString("app") == "RAWAL") { "Invalid backup file: missing RAWAL signature." }
            val version = root.optInt("version", -1)
            require(version in 1..CURRENT_VERSION) { "Unsupported backup version: $version" }

            val profilesArr = root.getJSONArray("profiles")
            require(profilesArr.length() > 0) { "Backup must contain at least one profile." }

            val profiles = (0 until profilesArr.length()).map { i ->
                val o = profilesArr.getJSONObject(i)
                val name = o.getString("name").trim()
                require(name.isNotEmpty()) { "Profile name cannot be empty." }
                ProfileEntity(
                    id = o.getLong("id"),
                    name = name,
                    avatarKey = o.optString("avatarKey", "teal_sun"),
                    customAvatarUri = o.optNullableString("customAvatarUri"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    isDefault = o.optBoolean("isDefault", false)
                )
            }
            val validProfileIds = profiles.map { it.id }.toSet()

            val settingsArr = root.optJSONArray("settings") ?: JSONArray()
            val settings = (0 until settingsArr.length()).mapNotNull { i ->
                val o = settingsArr.getJSONObject(i)
                val pid = o.getLong("profileId")
                if (pid !in validProfileIds) return@mapNotNull null
                ProfileSettingsEntity(
                    profileId = pid,
                    languageCode = o.optString("languageCode", "en").let { if (it == "fa") "fa" else "en" },
                    accentColorKey = o.optString("accentColorKey", "TEAL"),
                    fontSizeScale = o.optString("fontSizeScale", "MEDIUM"),
                    themeMode = o.optString("themeMode", "SYSTEM"),
                    streakThreshold = o.optInt("streakThreshold", 65).coerceIn(10, 100),
                    showScheduleBanner = o.optBoolean("showScheduleBanner", true),
                    showTimeSummaryCard = o.optBoolean("showTimeSummaryCard", true),
                    showScoreBreakdown = o.optBoolean("showScoreBreakdown", true),
                    compactActivityCards = o.optBoolean("compactActivityCards", false),
                    remindersEnabled = o.optBoolean("remindersEnabled", false),
                    missingDurationReminderHour = o.optInt("missingDurationReminderHour", 20).coerceIn(0, 23),
                    missingDurationReminderMinute = o.optInt("missingDurationReminderMinute", 30).coerceIn(0, 59),
                    endOfDayReminderHour = o.optInt("endOfDayReminderHour", 21).coerceIn(0, 23),
                    endOfDayReminderMinute = o.optInt("endOfDayReminderMinute", 30).coerceIn(0, 59)
                )
            }

            val categoriesArr = root.optJSONArray("categories") ?: JSONArray()
            val categories = (0 until categoriesArr.length()).mapNotNull { i ->
                val o = categoriesArr.getJSONObject(i)
                val pid = o.getLong("profileId")
                if (pid !in validProfileIds) return@mapNotNull null
                CategoryEntity(
                    id = o.getLong("id"),
                    profileId = pid,
                    nameEn = o.optString("nameEn", "Category"),
                    nameFa = o.optString("nameFa", "دسته‌بندی"),
                    colorHex = o.optLong("colorHex", 0xFF0F766E),
                    iconKey = o.optString("iconKey", "category"),
                    isSystem = o.optBoolean("isSystem", false),
                    isOptional = o.optBoolean("isOptional", false),
                    sortOrder = o.optInt("sortOrder", 0)
                )
            }

            val recordsArr = root.optJSONArray("dailyRecords") ?: JSONArray()
            val records = (0 until recordsArr.length()).mapNotNull { i ->
                val o = recordsArr.getJSONObject(i)
                val pid = o.getLong("profileId")
                if (pid !in validProfileIds) return@mapNotNull null
                DailyRecordEntity(
                    id = o.getLong("id"),
                    profileId = pid,
                    date = o.getString("date"),
                    wakeUpHour = o.optNullableInt("wakeUpHour"),
                    wakeUpMinute = o.optNullableInt("wakeUpMinute"),
                    sleepHour = o.optNullableInt("sleepHour"),
                    sleepMinute = o.optNullableInt("sleepMinute"),
                    wastedMinutes = o.optInt("wastedMinutes", 0).coerceAtLeast(0),
                    satisfaction = o.optNullableString("satisfaction"),
                    mood = o.optNullableString("mood"),
                    unexpectedEvents = o.optString("unexpectedEvents", ""),
                    disruptions = o.optString("disruptions", ""),
                    dailyNotes = o.optString("dailyNotes", ""),
                    startupCompleted = o.optBoolean("startupCompleted", false),
                    reviewCompleted = o.optBoolean("reviewCompleted", false),
                    calculatedScore = o.optInt("calculatedScore", 0).coerceIn(0, 100),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            }

            val activitiesArr = root.optJSONArray("activities") ?: JSONArray()
            val activities = (0 until activitiesArr.length()).mapNotNull { i ->
                val o = activitiesArr.getJSONObject(i)
                val pid = o.getLong("profileId")
                if (pid !in validProfileIds) return@mapNotNull null
                ActivityItemEntity(
                    id = o.getLong("id"),
                    profileId = pid,
                    date = o.getString("date"),
                    categoryId = o.getLong("categoryId"),
                    title = o.getString("title"),
                    notes = o.optString("notes", ""),
                    durationMinutes = o.optInt("durationMinutes", 0).coerceAtLeast(0),
                    isCompleted = o.optBoolean("isCompleted", false),
                    isFixedDaily = o.optBoolean("isFixedDaily", false),
                    fixedKey = o.optNullableString("fixedKey"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            }

            val goalsArr = root.optJSONArray("weeklyGoals") ?: JSONArray()
            val goals = (0 until goalsArr.length()).mapNotNull { i ->
                val o = goalsArr.getJSONObject(i)
                val pid = o.getLong("profileId")
                if (pid !in validProfileIds) return@mapNotNull null
                WeeklyGoalEntity(
                    id = o.getLong("id"),
                    profileId = pid,
                    title = o.getString("title"),
                    targetType = o.optString("targetType", "CATEGORY"),
                    categoryId = o.optNullableLong("categoryId"),
                    activityTitleMatch = o.optNullableString("activityTitleMatch"),
                    metricType = o.optString("metricType", "MINUTES"),
                    targetValue = o.optInt("targetValue", 60).coerceAtLeast(1),
                    weekStartDate = o.getString("weekStartDate"),
                    isActive = o.optBoolean("isActive", true)
                )
            }

            val reviewsArr = root.optJSONArray("weeklyReviews") ?: JSONArray()
            val reviews = (0 until reviewsArr.length()).mapNotNull { i ->
                val o = reviewsArr.getJSONObject(i)
                val pid = o.getLong("profileId")
                if (pid !in validProfileIds) return@mapNotNull null
                WeeklyReviewEntity(
                    id = o.getLong("id"),
                    profileId = pid,
                    weekStartDate = o.getString("weekStartDate"),
                    biggestWin = o.optString("biggestWin", ""),
                    mainChallenge = o.optString("mainChallenge", ""),
                    nextWeekFocus = o.optString("nextWeekFocus", ""),
                    energyRating = o.optInt("energyRating", 3).coerceIn(1, 5),
                    organizationRating = o.optInt("organizationRating", 3).coerceIn(1, 5),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            }

            val achievementsArr = root.optJSONArray("unlockedAchievements") ?: JSONArray()
            val achievements = (0 until achievementsArr.length()).mapNotNull { i ->
                val o = achievementsArr.getJSONObject(i)
                val pid = o.getLong("profileId")
                if (pid !in validProfileIds) return@mapNotNull null
                UnlockedAchievementEntity(
                    id = o.getLong("id"),
                    profileId = pid,
                    achievementKey = o.getString("achievementKey"),
                    unlockedAt = o.optLong("unlockedAt", System.currentTimeMillis())
                )
            }

            RawalBackupPayload(
                version = version,
                exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
                activeProfileId = root.optNullableLong("activeProfileId"),
                profiles = profiles,
                settings = settings,
                categories = categories,
                dailyRecords = records,
                activities = activities,
                weeklyGoals = goals,
                weeklyReviews = reviews,
                unlockedAchievements = achievements
            )
        }
    }

    fun exportActivitiesCsv(
        activities: List<ActivityItemEntity>,
        categories: List<CategoryEntity>
    ): String {
        val catMap = categories.associateBy { it.id }
        val sb = StringBuilder("\uFEFF") // UTF-8 BOM for Persian Excel compatibility
        sb.appendLine("Date,Activity,CategoryEn,CategoryFa,DurationMinutes,Completed,FixedDaily,Notes")
        activities.forEach { a ->
            val cat = catMap[a.categoryId]
            sb.appendLine(
                listOf(
                    csvEscape(a.date),
                    csvEscape(a.title),
                    csvEscape(cat?.nameEn ?: "Other"),
                    csvEscape(cat?.nameFa ?: "سایر"),
                    a.durationMinutes.toString(),
                    a.isCompleted.toString(),
                    a.isFixedDaily.toString(),
                    csvEscape(a.notes)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun exportDailyReportsCsv(records: List<DailyRecordEntity>): String {
        val sb = StringBuilder("\uFEFF")
        sb.appendLine("Date,Score,WakeUp,Sleep,WastedMinutes,Satisfaction,Mood,UnexpectedEvents,Disruptions,Notes")
        records.forEach { r ->
            val wake = if (r.wakeUpHour != null && r.wakeUpMinute != null) {
                String.format(java.util.Locale.US, "%02d:%02d", r.wakeUpHour, r.wakeUpMinute)
            } else ""
            val sleep = if (r.sleepHour != null && r.sleepMinute != null) {
                String.format(java.util.Locale.US, "%02d:%02d", r.sleepHour, r.sleepMinute)
            } else ""
            sb.appendLine(
                listOf(
                    csvEscape(r.date),
                    r.calculatedScore.toString(),
                    csvEscape(wake),
                    csvEscape(sleep),
                    r.wastedMinutes.toString(),
                    csvEscape(r.satisfaction ?: ""),
                    csvEscape(r.mood ?: ""),
                    csvEscape(r.unexpectedEvents),
                    csvEscape(r.disruptions),
                    csvEscape(r.dailyNotes)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String {
        val clean = value.replace("\r\n", " ").replace("\n", " ")
        return if (clean.contains(",") || clean.contains("\"")) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (isNull(key)) null else optInt(key)

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (isNull(key)) null else optLong(key)
}
