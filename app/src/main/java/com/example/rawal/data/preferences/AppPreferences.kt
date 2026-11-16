package com.example.rawal.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rawal_prefs")

class AppPreferences(private val context: Context) {
    private val activeProfileIdKey = longPreferencesKey("active_profile_id")
    private val firstLaunchDoneKey = booleanPreferencesKey("first_launch_done")

    val activeProfileId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[activeProfileIdKey]
    }

    suspend fun getActiveProfileId(): Long? = context.dataStore.data.first()[activeProfileIdKey]

    suspend fun setActiveProfileId(profileId: Long) {
        context.dataStore.edit { prefs ->
            prefs[activeProfileIdKey] = profileId
        }
    }

    suspend fun isFirstLaunchDone(): Boolean =
        context.dataStore.data.first()[firstLaunchDoneKey] ?: false

    suspend fun markFirstLaunchDone() {
        context.dataStore.edit { prefs ->
            prefs[firstLaunchDoneKey] = true
        }
    }
}
