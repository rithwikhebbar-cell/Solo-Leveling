package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.SupportedAiModels
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
    val openRouterModelId: String = SupportedAiModels.DEFAULT_MODEL_ID,
    val academicDailyGoalMinutes: Int = 60,
    val academicWeeklyGoalMinutes: Int = 360
)

data class ShadowRealmSettings(
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val extractionAlertsEnabled: Boolean = true,
    val evolutionAlertsEnabled: Boolean = true,
    val autoRecallOnRest: Boolean = true,
    val updatedAtEpochMs: Long = 0L
)

data class VoiceCommandPermissions(
    val allowPlayerInfo: Boolean = true,
    val allowDailyManagement: Boolean = true,
    val allowHabitsAndRoutines: Boolean = true,
    val allowPhysicalDevelopment: Boolean = true,
    val allowAcademicDevelopment: Boolean = true,
    val allowMissionsAndRpg: Boolean = true,
    val allowSystemControl: Boolean = true,
    val allowAiConversation: Boolean = true,
    val requireConfirmationForMutations: Boolean = true,
    val updatedAtEpochMs: Long = 0L
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
        val VOICE_PERM_PLAYER_INFO = booleanPreferencesKey("voice_perm_player_info")
        val VOICE_PERM_DAILY_MGMT = booleanPreferencesKey("voice_perm_daily_mgmt")
        val VOICE_PERM_HABITS = booleanPreferencesKey("voice_perm_habits")
        val VOICE_PERM_PHYSICAL = booleanPreferencesKey("voice_perm_physical")
        val VOICE_PERM_ACADEMIC = booleanPreferencesKey("voice_perm_academic")
        val VOICE_PERM_MISSIONS_RPG = booleanPreferencesKey("voice_perm_missions_rpg")
        val VOICE_PERM_SYSTEM_CONTROL = booleanPreferencesKey("voice_perm_system_control")
        val VOICE_PERM_AI_CONVERSATION = booleanPreferencesKey("voice_perm_ai_conversation")
        val VOICE_PERM_REQUIRE_CONFIRM = booleanPreferencesKey("voice_perm_require_confirm")
        val VOICE_PERM_UPDATED_AT = longPreferencesKey("voice_perm_updated_at")
    }

    private fun normalizeShadowOwner(accountOwnerId: String): String {
        return accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }
    }

    private fun shadowPrefix(accountOwnerId: String): String {
        val normalized = normalizeShadowOwner(accountOwnerId)
        val safe = normalized.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return "shadow_${safe}"
    }

    private fun shadowExtractionAlertsKey(accountOwnerId: String) =
        booleanPreferencesKey("${shadowPrefix(accountOwnerId)}_extraction_alerts")

    private fun shadowEvolutionAlertsKey(accountOwnerId: String) =
        booleanPreferencesKey("${shadowPrefix(accountOwnerId)}_evolution_alerts")

    private fun shadowAutoRecallOnRestKey(accountOwnerId: String) =
        booleanPreferencesKey("${shadowPrefix(accountOwnerId)}_auto_recall_on_rest")

    private fun shadowSettingsUpdatedAtKey(accountOwnerId: String) =
        longPreferencesKey("${shadowPrefix(accountOwnerId)}_settings_updated_at")

    val preferencesFlow: Flow<UserPreferences> = context.realityOsDataStore.data.map { prefs ->
        UserPreferences(
            hudGlowEnabled = prefs[Keys.HUD_GLOW_ENABLED] ?: true,
            dailyResetHour = prefs[Keys.DAILY_RESET_HOUR] ?: 4,
            healthConnectSyncEnabled = prefs[Keys.HEALTH_CONNECT_SYNC_ENABLED] ?: false,
            supabaseCloudSyncEnabled = prefs[Keys.SUPABASE_CLOUD_SYNC_ENABLED] ?: false,
            openRouterModelId = prefs[Keys.OPENROUTER_MODEL_ID] ?: SupportedAiModels.DEFAULT_MODEL_ID,
            academicDailyGoalMinutes = prefs[Keys.ACADEMIC_DAILY_GOAL_MINUTES] ?: 60,
            academicWeeklyGoalMinutes = prefs[Keys.ACADEMIC_WEEKLY_GOAL_MINUTES] ?: 360
        )
    }

    val voiceCommandPermissionsFlow: Flow<VoiceCommandPermissions> = context.realityOsDataStore.data.map { prefs ->
        VoiceCommandPermissions(
            allowPlayerInfo = prefs[Keys.VOICE_PERM_PLAYER_INFO] ?: true,
            allowDailyManagement = prefs[Keys.VOICE_PERM_DAILY_MGMT] ?: true,
            allowHabitsAndRoutines = prefs[Keys.VOICE_PERM_HABITS] ?: true,
            allowPhysicalDevelopment = prefs[Keys.VOICE_PERM_PHYSICAL] ?: true,
            allowAcademicDevelopment = prefs[Keys.VOICE_PERM_ACADEMIC] ?: true,
            allowMissionsAndRpg = prefs[Keys.VOICE_PERM_MISSIONS_RPG] ?: true,
            allowSystemControl = prefs[Keys.VOICE_PERM_SYSTEM_CONTROL] ?: true,
            allowAiConversation = prefs[Keys.VOICE_PERM_AI_CONVERSATION] ?: true,
            requireConfirmationForMutations = prefs[Keys.VOICE_PERM_REQUIRE_CONFIRM] ?: true,
            updatedAtEpochMs = prefs[Keys.VOICE_PERM_UPDATED_AT] ?: 0L
        )
    }

    suspend fun updateVoiceCommandPermissions(
        transform: (VoiceCommandPermissions) -> VoiceCommandPermissions
    ) {
        context.realityOsDataStore.edit { prefs ->
            val current = VoiceCommandPermissions(
                allowPlayerInfo = prefs[Keys.VOICE_PERM_PLAYER_INFO] ?: true,
                allowDailyManagement = prefs[Keys.VOICE_PERM_DAILY_MGMT] ?: true,
                allowHabitsAndRoutines = prefs[Keys.VOICE_PERM_HABITS] ?: true,
                allowPhysicalDevelopment = prefs[Keys.VOICE_PERM_PHYSICAL] ?: true,
                allowAcademicDevelopment = prefs[Keys.VOICE_PERM_ACADEMIC] ?: true,
                allowMissionsAndRpg = prefs[Keys.VOICE_PERM_MISSIONS_RPG] ?: true,
                allowSystemControl = prefs[Keys.VOICE_PERM_SYSTEM_CONTROL] ?: true,
                allowAiConversation = prefs[Keys.VOICE_PERM_AI_CONVERSATION] ?: true,
                requireConfirmationForMutations = prefs[Keys.VOICE_PERM_REQUIRE_CONFIRM] ?: true,
                updatedAtEpochMs = prefs[Keys.VOICE_PERM_UPDATED_AT] ?: 0L
            )
            val updated = transform(current)
            prefs[Keys.VOICE_PERM_PLAYER_INFO] = updated.allowPlayerInfo
            prefs[Keys.VOICE_PERM_DAILY_MGMT] = updated.allowDailyManagement
            prefs[Keys.VOICE_PERM_HABITS] = updated.allowHabitsAndRoutines
            prefs[Keys.VOICE_PERM_PHYSICAL] = updated.allowPhysicalDevelopment
            prefs[Keys.VOICE_PERM_ACADEMIC] = updated.allowAcademicDevelopment
            prefs[Keys.VOICE_PERM_MISSIONS_RPG] = updated.allowMissionsAndRpg
            prefs[Keys.VOICE_PERM_SYSTEM_CONTROL] = updated.allowSystemControl
            prefs[Keys.VOICE_PERM_AI_CONVERSATION] = updated.allowAiConversation
            prefs[Keys.VOICE_PERM_REQUIRE_CONFIRM] = updated.requireConfirmationForMutations
            prefs[Keys.VOICE_PERM_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    fun observeShadowSettings(accountOwnerId: String): Flow<ShadowRealmSettings> {
        val owner = normalizeShadowOwner(accountOwnerId)
        return context.realityOsDataStore.data.map { prefs ->
            ShadowRealmSettings(
                accountOwnerId = owner,
                extractionAlertsEnabled = prefs[shadowExtractionAlertsKey(owner)] ?: true,
                evolutionAlertsEnabled = prefs[shadowEvolutionAlertsKey(owner)] ?: true,
                autoRecallOnRest = prefs[shadowAutoRecallOnRestKey(owner)] ?: true,
                updatedAtEpochMs = prefs[shadowSettingsUpdatedAtKey(owner)] ?: 0L
            )
        }
    }

    suspend fun setShadowExtractionAlerts(accountOwnerId: String, enabled: Boolean) {
        val owner = normalizeShadowOwner(accountOwnerId)
        context.realityOsDataStore.edit { prefs ->
            prefs[shadowExtractionAlertsKey(owner)] = enabled
            prefs[shadowSettingsUpdatedAtKey(owner)] = System.currentTimeMillis()
        }
    }

    suspend fun setShadowEvolutionAlerts(accountOwnerId: String, enabled: Boolean) {
        val owner = normalizeShadowOwner(accountOwnerId)
        context.realityOsDataStore.edit { prefs ->
            prefs[shadowEvolutionAlertsKey(owner)] = enabled
            prefs[shadowSettingsUpdatedAtKey(owner)] = System.currentTimeMillis()
        }
    }

    suspend fun setShadowAutoRecallOnRest(accountOwnerId: String, enabled: Boolean) {
        val owner = normalizeShadowOwner(accountOwnerId)
        context.realityOsDataStore.edit { prefs ->
            prefs[shadowAutoRecallOnRestKey(owner)] = enabled
            prefs[shadowSettingsUpdatedAtKey(owner)] = System.currentTimeMillis()
        }
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
