package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.rawal.data.local.RawalDatabase
import com.example.rawal.data.local.entity.ActivityItemEntity
import com.example.rawal.data.local.entity.CategoryEntity
import com.example.rawal.data.local.entity.DailyRecordEntity
import com.example.rawal.data.local.entity.ProfileEntity
import com.example.rawal.data.local.entity.ProfileSettingsEntity
import com.example.rawal.data.preferences.AppPreferences
import com.example.rawal.data.repository.RawalRepository
import com.example.rawal.domain.backup.BackupManager
import com.example.rawal.domain.backup.RawalBackupPayload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RAWAL", appName)
    }

    @Test
    fun `json backup round-trip validation and csv export`() {
        val payload = RawalBackupPayload(
            version = 1,
            exportedAt = 1700000000L,
            activeProfileId = 1L,
            profiles = listOf(ProfileEntity(id = 1L, name = "Sara", isDefault = true)),
            settings = listOf(ProfileSettingsEntity(profileId = 1L, languageCode = "fa", streakThreshold = 70)),
            categories = listOf(CategoryEntity(id = 10L, profileId = 1L, nameEn = "Reading", nameFa = "مطالعه", colorHex = 0xFF0F766E)),
            dailyRecords = listOf(DailyRecordEntity(id = 1L, profileId = 1L, date = "2026-11-16", calculatedScore = 88)),
            activities = listOf(ActivityItemEntity(id = 1L, profileId = 1L, date = "2026-11-16", categoryId = 10L, title = "Book", durationMinutes = 45, isCompleted = true)),
            weeklyGoals = emptyList(),
            weeklyReviews = emptyList(),
            unlockedAchievements = emptyList()
        )
        val json = BackupManager.exportToJson(payload)
        val parsed = BackupManager.parseAndValidateJson(json)
        assertTrue(parsed.isSuccess)
        val restored = parsed.getOrThrow()
        assertEquals("Sara", restored.profiles.first().name)
        assertEquals("fa", restored.settings.first().languageCode)
        assertEquals(88, restored.dailyRecords.first().calculatedScore)

        val invalid = BackupManager.parseAndValidateJson("{\"app\":\"OTHER\"}")
        assertTrue(invalid.isFailure)

        val csv = BackupManager.exportActivitiesCsv(payload.activities, payload.categories)
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("Book"))
    }

    @Test
    fun `repository ensures multi profile isolation and undo deletion`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, RawalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = RawalRepository(db, AppPreferences(context))

        val p1 = repo.ensureInitialized()
        val p1Cats = repo.observeCategories(p1).first()
        assertTrue(p1Cats.isNotEmpty())

        val act1Id = repo.saveActivity(
            ActivityItemEntity(
                profileId = p1,
                date = "2026-11-16",
                categoryId = p1Cats.first().id,
                title = "Profile 1 Study",
                durationMinutes = 40,
                isCompleted = true
            )
        )
        assertTrue(act1Id > 0)

        val p2 = repo.createProfile("Guest", "indigo_compass", null, "fa")
        val p2Activities = repo.observeActivities(p2).first()
        assertTrue(p2Activities.isEmpty())

        assertTrue(repo.deleteProfile(p2))
        assertFalse(repo.deleteProfile(p1))

        val p1Acts = repo.observeActivities(p1).first()
        assertEquals(1, p1Acts.size)
        repo.deleteActivity(p1Acts.first())
        assertEquals(0, repo.observeActivities(p1).first().size)
        assertTrue(repo.undoLastDeletion())
        assertEquals(1, repo.observeActivities(p1).first().size)
        db.close()
    }

    @Test
    fun `compose dashboard opens add activity and switches language without losing data`() {
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("dashboard_daily_score").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("dashboard_daily_score").assertIsDisplayed()

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("dashboard_add_activity_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("dashboard_add_activity_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("activity_title_input").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("activity_title_input").performTextInput("Evening Physics")
        composeRule.onNodeWithTag("save_activity_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("0").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("dashboard_lazy_column")
            .performScrollToNode(hasTestTag("activity_card_1"))
        composeRule.onNodeWithTag("activity_card_1").assertIsDisplayed()
        composeRule.onNodeWithText("Evening Physics").assertIsDisplayed()

        // Open Daily Setup dialog and step through to verify no dialog freeze
        composeRule.onNodeWithTag("dashboard_lazy_column").performScrollToNode(hasTestTag("daily_setup_button"))
        composeRule.onNodeWithTag("daily_setup_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("setup_no_chip").fetchSemanticsNodes().isNotEmpty()
        }
        repeat(5) {
            composeRule.onNodeWithTag("setup_no_chip").performClick()
        }
        composeRule.onNodeWithTag("setup_next_button").performClick() // Wake up -> Sleep
        composeRule.onNodeWithTag("setup_next_button").performClick() // Sleep -> Extra tasks
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("setup_add_extra_no").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("setup_add_extra_no").performClick()

        // Switch to Settings -> Persian (RTL)
        composeRule.onNodeWithTag("bottom_nav_SETTINGS").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("lang_fa_chip").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("lang_fa_chip").performClick()

        // Switch to Search, Calendar, Reports, Goals tabs to verify zero crashes
        composeRule.onNodeWithTag("top_search_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("global_search_input").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("global_search_input").performTextInput("Physics")
        composeRule.onNodeWithText("Evening Physics").assertIsDisplayed()

        composeRule.onNodeWithTag("bottom_nav_CALENDAR").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_nav_REPORTS").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_nav_GOALS").performClick()
        composeRule.waitForIdle()

        // Switch back to Dashboard and verify activity & score still exist in Persian RTL
        composeRule.onNodeWithTag("bottom_nav_DASHBOARD").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("dashboard_daily_score").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("dashboard_daily_score").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_lazy_column")
            .performScrollToNode(hasText("Evening Physics"))
        composeRule.onNodeWithText("Evening Physics").assertIsDisplayed()
    }
}
