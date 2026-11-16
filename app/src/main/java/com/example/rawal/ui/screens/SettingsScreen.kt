package com.example.rawal.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.rawal.data.local.entity.CategoryEntity
import com.example.rawal.data.local.entity.ProfileEntity
import com.example.rawal.data.local.entity.ProfileSettingsEntity
import com.example.rawal.domain.backup.RawalBackupPayload
import com.example.rawal.domain.i18n.AppLanguage
import com.example.rawal.domain.i18n.AppStringsProvider
import com.example.rawal.ui.RawalUiState
import com.example.rawal.ui.components.ConfirmationDialog
import com.example.rawal.ui.components.ProfileAvatar
import com.example.rawal.ui.components.SectionCard
import com.example.rawal.ui.theme.AccentPalette
import com.example.rawal.ui.theme.CategoryColorPresets
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: RawalUiState,
    onUpdateSettings: (ProfileSettingsEntity) -> Unit,
    onSwitchProfile: (Long) -> Unit,
    onCreateProfile: (name: String, avatarKey: String, customAvatarUri: String?) -> Unit,
    onUpdateProfile: (ProfileEntity) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onSaveCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onOpenGoals: () -> Unit,
    onBuildBackupJson: suspend () -> String,
    onParseBackupJson: (String) -> Result<RawalBackupPayload>,
    onApplyBackupPayload: (RawalBackupPayload) -> Unit,
    onBuildActivitiesCsv: () -> String,
    onBuildReportsCsv: () -> String,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = state.strings
    val lang = state.language
    val settings = state.settings

    var showProfileDialog by remember { mutableStateOf<ProfileEntity?>(null) }
    var creatingNewProfile by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<ProfileEntity?>(null) }

    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var creatingNewCategory by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    var pendingRestorePayload by remember { mutableStateOf<RawalBackupPayload?>(null) }

    // SAF launchers (zero broad storage permissions)
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = onBuildBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess {
                    onNotify(strings.exportSuccess)
                }.onFailure {
                    onNotify(it.localizedMessage ?: strings.importErrorInvalid)
                }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            }.onSuccess { text ->
                onParseBackupJson(text)
                    .onSuccess { payload -> pendingRestorePayload = payload }
                    .onFailure { onNotify(strings.importErrorInvalid) }
            }.onFailure {
                onNotify(strings.importErrorInvalid)
            }
        }
    }

    val exportActivitiesCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val csv = onBuildActivitiesCsv()
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            }.onSuccess { onNotify(strings.exportSuccess) }
        }
    }

    val exportReportsCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val csv = onBuildReportsCsv()
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            }.onSuccess { onNotify(strings.exportSuccess) }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onUpdateSettings(settings.copy(remindersEnabled = true))
        } else {
            onNotify(strings.notificationPermissionNeeded)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Language Selection
        item {
            SectionCard {
                Text(strings.settingsLanguage, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = lang == AppLanguage.EN,
                        onClick = { onUpdateSettings(settings.copy(languageCode = "en")) },
                        label = { Text("English (LTR)") },
                        modifier = Modifier.weight(1f).testTag("lang_en_chip")
                    )
                    FilterChip(
                        selected = lang == AppLanguage.FA,
                        onClick = { onUpdateSettings(settings.copy(languageCode = "fa")) },
                        label = { Text("فارسی (RTL)") },
                        modifier = Modifier.weight(1f).testTag("lang_fa_chip")
                    )
                }
            }
        }

        // 2. Appearance: Accent Color, Font Size, Theme Mode
        item {
            SectionCard {
                Text(strings.settingsAppearance, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))

                Text(strings.accentColorLabel, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentPalette.entries.forEach { palette ->
                        val isSelected = settings.accentColorKey == palette.key
                        Surface(
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { onUpdateSettings(settings.copy(accentColorKey = palette.key)) }
                                .testTag("accent_${palette.key}"),
                            shape = CircleShape,
                            color = palette.primaryLight,
                            border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(strings.fontSizeLabel, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SMALL" to strings.fontSmall, "MEDIUM" to strings.fontMedium, "LARGE" to strings.fontLarge).forEach { (k, label) ->
                        FilterChip(
                            selected = settings.fontSizeScale == k,
                            onClick = { onUpdateSettings(settings.copy(fontSizeScale = k)) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(strings.themeModeLabel, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SYSTEM" to strings.themeSystem, "LIGHT" to strings.themeLight, "DARK" to strings.themeDark).forEach { (k, label) ->
                        FilterChip(
                            selected = settings.themeMode == k,
                            onClick = { onUpdateSettings(settings.copy(themeMode = k)) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // 3. Dashboard Customization
        item {
            SectionCard {
                Text(strings.dashboardCustomization, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                SettingSwitchRow(strings.toggleScheduleBanner, settings.showScheduleBanner) {
                    onUpdateSettings(settings.copy(showScheduleBanner = it))
                }
                SettingSwitchRow(strings.toggleTimeSummary, settings.showTimeSummaryCard) {
                    onUpdateSettings(settings.copy(showTimeSummaryCard = it))
                }
                SettingSwitchRow(strings.toggleScoreBreakdown, settings.showScoreBreakdown) {
                    onUpdateSettings(settings.copy(showScoreBreakdown = it))
                }
                SettingSwitchRow(strings.toggleCompactCards, settings.compactActivityCards) {
                    onUpdateSettings(settings.copy(compactActivityCards = it))
                }
            }
        }

        // 4. Streak Threshold & Goals Shortcut
        item {
            SectionCard {
                Text(
                    text = "${strings.streakThresholdLabel}: ${AppStringsProvider.toLocalizedDigits(settings.streakThreshold.toString(), lang)}/100",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = settings.streakThreshold.toFloat(),
                    onValueChange = { onUpdateSettings(settings.copy(streakThreshold = it.roundToInt().coerceIn(20, 95))) },
                    valueRange = 20f..95f
                )
                OutlinedButton(onClick = onOpenGoals) {
                    Text(strings.weeklyGoalsTitle)
                }
            }
        }

        // 5. Category Management
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.categoryManagement, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = { creatingNewCategory = true },
                        modifier = Modifier.testTag("add_category_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.addCategory)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.categories.forEach { cat ->
                        val catName = if (lang == AppLanguage.FA) cat.nameFa else cat.nameEn
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(Modifier.size(14.dp).clip(CircleShape).background(Color(cat.colorHex)))
                                Spacer(Modifier.width(10.dp))
                                Text(catName, style = MaterialTheme.typography.bodyMedium)
                            }
                            Row {
                                IconButton(onClick = { editingCategory = cat }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = strings.edit)
                                }
                                if (state.categories.size > 1) {
                                    IconButton(onClick = { categoryToDelete = cat }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Reminders
        item {
            val sug = state.suggestedReminders
            SectionCard {
                Text(strings.reminderSettings, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                SettingSwitchRow(strings.enableReminders, settings.remindersEnabled) { wantEnable ->
                    if (!wantEnable) {
                        onUpdateSettings(settings.copy(remindersEnabled = false))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onUpdateSettings(settings.copy(remindersEnabled = true))
                    }
                }

                Spacer(Modifier.height(6.dp))
                ReminderTimeEditorRow(
                    label = strings.missingDurationReminder,
                    hour = settings.missingDurationReminderHour,
                    minute = settings.missingDurationReminderMinute,
                    language = lang,
                    onChange = { h, m ->
                        onUpdateSettings(settings.copy(missingDurationReminderHour = h, missingDurationReminderMinute = m))
                    }
                )
                Spacer(Modifier.height(6.dp))
                ReminderTimeEditorRow(
                    label = strings.endOfDayReminder,
                    hour = settings.endOfDayReminderHour,
                    minute = settings.endOfDayReminderMinute,
                    language = lang,
                    onChange = { h, m ->
                        onUpdateSettings(settings.copy(endOfDayReminderHour = h, endOfDayReminderMinute = m))
                    }
                )

                if (sug.basedOnDaysCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    val t1 = AppStringsProvider.formatTime(lang, sug.missingDurationHour, sug.missingDurationMinute)
                    val t2 = AppStringsProvider.formatTime(lang, sug.endOfDayHour, sug.endOfDayMinute)
                    Text(
                        text = String.format(strings.reminderSuggestionFormat, sug.basedOnDaysCount, t1, t2),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            onUpdateSettings(
                                settings.copy(
                                    missingDurationReminderHour = sug.missingDurationHour,
                                    missingDurationReminderMinute = sug.missingDurationMinute,
                                    endOfDayReminderHour = sug.endOfDayHour,
                                    endOfDayReminderMinute = sug.endOfDayMinute
                                )
                            )
                        }
                    ) {
                        Text(strings.applySuggestedTimes)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = strings.reminderLimitationNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 7. Backup & Restore + CSV Export
        item {
            SectionCard {
                Text(strings.backupAndRestore, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { exportJsonLauncher.launch("rawal_backup.json") },
                        modifier = Modifier.fillMaxWidth().testTag("export_json_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.exportJsonBackup)
                    }
                    OutlinedButton(
                        onClick = { importJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.fillMaxWidth().testTag("import_json_button")
                    ) {
                        Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.importJsonBackup)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { exportActivitiesCsvLauncher.launch("rawal_activities.csv") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.exportActivitiesCsv)
                        }
                        OutlinedButton(
                            onClick = { exportReportsCsvLauncher.launch("rawal_reports.csv") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.exportReportsCsv)
                        }
                    }
                }
            }
        }

        // 8. Multi-Profile Management
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.profileManagement, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { creatingNewProfile = true },
                        modifier = Modifier.testTag("create_profile_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.createProfile)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.profiles.forEach { profile ->
                        val isActive = profile.id == state.activeProfile?.id
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSwitchProfile(profile.id) }
                                .testTag("profile_row_${profile.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    ProfileAvatar(profile.avatarKey, profile.customAvatarUri, profile.name, size = 38.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(profile.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        if (isActive) {
                                            Text(strings.activeProfileBadge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                Row {
                                    IconButton(onClick = { showProfileDialog = profile }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = strings.editProfile)
                                    }
                                    if (state.profiles.size > 1) {
                                        IconButton(onClick = { profileToDelete = profile }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creatingNewProfile || showProfileDialog != null) {
        ProfileEditorDialog(
            initial = showProfileDialog,
            strings = strings,
            onSave = { name, avatarKey, customUri ->
                val target = showProfileDialog
                if (target == null) {
                    onCreateProfile(name, avatarKey, customUri)
                } else {
                    onUpdateProfile(target.copy(name = name, avatarKey = avatarKey, customAvatarUri = customUri))
                }
                creatingNewProfile = false
                showProfileDialog = null
            },
            onDismiss = {
                creatingNewProfile = false
                showProfileDialog = null
            }
        )
    }

    profileToDelete?.let { p ->
        ConfirmationDialog(
            title = strings.deleteProfileConfirmTitle,
            body = strings.deleteProfileConfirmBody,
            confirmText = strings.delete,
            cancelText = strings.cancel,
            onConfirm = {
                onDeleteProfile(p.id)
                profileToDelete = null
            },
            onDismiss = { profileToDelete = null }
        )
    }

    if (creatingNewCategory || editingCategory != null) {
        CategoryEditorDialog(
            initial = editingCategory,
            profileId = state.activeProfile?.id ?: 1L,
            strings = strings,
            onSave = { cat ->
                onSaveCategory(cat)
                creatingNewCategory = false
                editingCategory = null
            },
            onDismiss = {
                creatingNewCategory = false
                editingCategory = null
            }
        )
    }

    categoryToDelete?.let { cat ->
        ConfirmationDialog(
            title = strings.delete,
            body = strings.deleteCategoryConfirm,
            confirmText = strings.delete,
            cancelText = strings.cancel,
            onConfirm = {
                onDeleteCategory(cat.id)
                categoryToDelete = null
            },
            onDismiss = { categoryToDelete = null }
        )
    }

    pendingRestorePayload?.let { payload ->
        ConfirmationDialog(
            title = strings.importConfirmTitle,
            body = strings.importConfirmBody,
            confirmText = strings.confirm,
            cancelText = strings.cancel,
            isDestructive = false,
            onConfirm = {
                onApplyBackupPayload(payload)
                pendingRestorePayload = null
            },
            onDismiss = { pendingRestorePayload = null }
        )
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ReminderTimeEditorRow(
    label: String,
    hour: Int,
    minute: Int,
    language: AppLanguage,
    onChange: (Int, Int) -> Unit
) {
    var hText by remember(hour) { mutableStateOf(hour.toString()) }
    var mText by remember(minute) { mutableStateOf(minute.toString()) }

    Column {
        Text("$label (${AppStringsProvider.formatTime(language, hour, minute)})", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = hText,
                onValueChange = { v ->
                    if (v.all(Char::isDigit) && v.length <= 2) {
                        hText = v
                        v.toIntOrNull()?.let { onChange(it.coerceIn(0, 23), minute) }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = mText,
                onValueChange = { v ->
                    if (v.all(Char::isDigit) && v.length <= 2) {
                        mText = v
                        v.toIntOrNull()?.let { onChange(hour, it.coerceIn(0, 59)) }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditorDialog(
    initial: ProfileEntity?,
    strings: com.example.rawal.domain.i18n.AppStrings,
    onSave: (name: String, avatarKey: String, customAvatarUri: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var avatarKey by remember { mutableStateOf(initial?.avatarKey ?: "teal_sun") }
    var customUri by remember { mutableStateOf(initial?.customAvatarUri) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            customUri = uri.toString()
        }
    }

    val avatarKeys = listOf("teal_sun", "indigo_compass", "emerald_run", "amber_light", "rose_book", "slate_peak")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 520.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(if (initial == null) strings.createProfile else strings.editProfile) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text(strings.profileNameLabel) },
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { m -> { Text(m) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )
                Text(strings.chooseAvatar, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    avatarKeys.forEach { key ->
                        val selected = customUri == null && avatarKey == key
                        Surface(
                            shape = CircleShape,
                            border = if (selected) BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.clickable {
                                customUri = null
                                avatarKey = key
                            }
                        ) {
                            ProfileAvatar(key, null, key, size = 44.dp)
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Text(strings.pickPhotoFromDevice)
                }
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag("save_profile_button"),
                onClick = {
                    if (name.trim().isEmpty()) {
                        errorMsg = strings.validationRequiredField
                        return@Button
                    }
                    onSave(name.trim(), avatarKey, customUri)
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun CategoryEditorDialog(
    initial: CategoryEntity?,
    profileId: Long,
    strings: com.example.rawal.domain.i18n.AppStrings,
    onSave: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var nameEn by remember { mutableStateOf(initial?.nameEn ?: "") }
    var nameFa by remember { mutableStateOf(initial?.nameFa ?: "") }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: CategoryColorPresets.first()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 520.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(if (initial == null) strings.addCategory else strings.renameCategory) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it; errorMsg = null },
                    label = { Text("English Name") },
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { m -> { Text(m) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("category_name_en_input")
                )
                OutlinedTextField(
                    value = nameFa,
                    onValueChange = { nameFa = it },
                    label = { Text("نام فارسی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(strings.categoryColorLabel, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryColorPresets.forEach { hex ->
                        val selected = colorHex == hex
                        Surface(
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { colorHex = hex },
                            shape = CircleShape,
                            color = Color(hex),
                            border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag("save_category_button"),
                onClick = {
                    if (nameEn.trim().isEmpty() && nameFa.trim().isEmpty()) {
                        errorMsg = strings.validationRequiredField
                        return@Button
                    }
                    val en = nameEn.trim().ifEmpty { nameFa.trim() }
                    val fa = nameFa.trim().ifEmpty { nameEn.trim() }
                    onSave(
                        (initial ?: CategoryEntity(
                            profileId = profileId,
                            nameEn = en,
                            nameFa = fa,
                            colorHex = colorHex,
                            isOptional = true
                        )).copy(
                            nameEn = en,
                            nameFa = fa,
                            colorHex = colorHex
                        )
                    )
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
