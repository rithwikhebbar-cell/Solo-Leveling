package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.realityOsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reality_os_preferences"
)

data class UserPreferences(
    val hudGlowEnabled: Boolean = true,
    val dailyResetHour: Int = 4,
    val healthConnectSyncEnabled: Boolean = false,
    val supabaseCloudSyncEnabled: Boolean = false,
    val openRouterModelId: String = "google/gemini-2.5-flash",
    val academicDailyGoalMinutes: Int = 60,
    val academicWeeklyGoalMinutes: Int = 360
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val HUD_GLOW_ENABLED = booleanPreferencesKey("hud_glow_enabled")
        val DAILY_RESET_HOUR = intPreferencesKey("daily_reset_hour")
        val HEALTH_CONNECT_SYNC_ENABLED = booleanPreferencesKey("health_connect_sync_enabled")
        val SUPABASE_CLOUD_SYNC_ENABLED = booleanPreferencesKey("supabase_cloud_sync_enabled")
        val OPENROUTER_MODEL_ID = stringPreferencesKey("openrouter_model_id")
        val ACADEMIC_DAILY_GOAL_MINUTES = intPreferencesKey("academic_daily_goal_minutes")
        val ACADEMIC_WEEKLY_GOAL_MINUTES = intPreferencesKey("academic_weekly_goal_minutes")
    }

    val preferencesFlow: Flow<UserPreferences> = context.realityOsDataStore.data.map { prefs ->
        UserPreferences(
            hudGlowEnabled = prefs[Keys.HUD_GLOW_ENABLED] ?: true,
            dailyResetHour = prefs[Keys.DAILY_RESET_HOUR] ?: 4,
            healthConnectSyncEnabled = prefs[Keys.HEALTH_CONNECT_SYNC_ENABLED] ?: false,
            supabaseCloudSyncEnabled = prefs[Keys.SUPABASE_CLOUD_SYNC_ENABLED] ?: false,
            openRouterModelId = prefs[Keys.OPENROUTER_MODEL_ID] ?: "google/gemini-2.5-flash",
            academicDailyGoalMinutes = prefs[Keys.ACADEMIC_DAILY_GOAL_MINUTES] ?: 60,
            academicWeeklyGoalMinutes = prefs[Keys.ACADEMIC_WEEKLY_GOAL_MINUTES] ?: 360
        )
    }

    suspend fun setHudGlowEnabled(enabled: Boolean) {
        context.realityOsDataStore.edit { it[Keys.HUD_GLOW_ENABLED] = enabled }
    }

    suspend fun setDailyResetHour(hour: Int) {
        context.realityOsDataStore.edit { it[Keys.DAILY_RESET_HOUR] = hour.coerceIn(0, 23) }
    }

    suspend fun setHealthConnectSyncEnabled(enabled: Boolean) {
        context.realityOsDataStore.edit { it[Keys.HEALTH_CONNECT_SYNC_ENABLED] = enabled }
    }

    suspend fun setSupabaseCloudSyncEnabled(enabled: Boolean) {
        context.realityOsDataStore.edit { it[Keys.SUPABASE_CLOUD_SYNC_ENABLED] = enabled }
    }

    suspend fun setOpenRouterModelId(modelId: String) {
        context.realityOsDataStore.edit { it[Keys.OPENROUTER_MODEL_ID] = modelId }
    }

    suspend fun setAcademicGoals(dailyMinutes: Int, weeklyMinutes: Int) {
        context.realityOsDataStore.edit {
            it[Keys.ACADEMIC_DAILY_GOAL_MINUTES] = dailyMinutes.coerceIn(15, 480)
            it[Keys.ACADEMIC_WEEKLY_GOAL_MINUTES] = weeklyMinutes.coerceIn(60, 3000)
        }
    }
}
