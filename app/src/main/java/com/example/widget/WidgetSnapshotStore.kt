package com.example.widget

import android.content.Context
import com.example.domain.model.WidgetSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WidgetSnapshotStore {
    private const val PREFS_NAME = "solo_leveling_widget_snapshots"
    private const val KEY_SNAPSHOT_JSON = "latest_widget_snapshot_json"
    private val json = Json { ignoreUnknownKeys = true }

    fun saveSnapshot(context: Context, snapshot: WidgetSnapshot) {
        runCatching {
            val encoded = json.encodeToString(snapshot)
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SNAPSHOT_JSON, encoded)
                .apply()
        }
    }

    fun loadSnapshot(context: Context): WidgetSnapshot {
        return runCatching {
            val raw = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SNAPSHOT_JSON, null)
            if (raw.isNullOrBlank()) WidgetSnapshot() else json.decodeFromString<WidgetSnapshot>(raw)
        }.getOrElse { WidgetSnapshot() }
    }
}
