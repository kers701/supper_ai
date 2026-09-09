package com.aichat.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_chat_prefs")

class ModelConfigRepository(private val context: Context) {

    private val gson = Gson()
    private val modelsKey = stringPreferencesKey("model_configs")
    private val currentModelIdKey = stringPreferencesKey("current_model_id")

    val models: Flow<List<ModelConfig>> = context.dataStore.data.map { prefs ->
        val json = prefs[modelsKey]
        if (json.isNullOrBlank()) {
            ModelConfig.defaultConfigs()
        } else {
            try {
                val type = object : TypeToken<List<ModelConfig>>() {}.type
                gson.fromJson(json, type) ?: ModelConfig.defaultConfigs()
            } catch (e: Exception) {
                ModelConfig.defaultConfigs()
            }
        }
    }

    val currentModelId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[currentModelIdKey]
    }

    suspend fun saveModels(models: List<ModelConfig>) {
        context.dataStore.edit { prefs ->
            prefs[modelsKey] = gson.toJson(models)
        }
    }

    suspend fun setCurrentModelId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[currentModelIdKey] = id
        }
    }

    suspend fun addOrUpdateModel(config: ModelConfig) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[modelsKey]
            val current = if (currentJson.isNullOrBlank()) {
                ModelConfig.defaultConfigs().toMutableList()
            } else {
                try {
                    val type = object : TypeToken<MutableList<ModelConfig>>() {}.type
                    gson.fromJson<MutableList<ModelConfig>>(currentJson, type) ?: ModelConfig.defaultConfigs().toMutableList()
                } catch (e: Exception) {
                    ModelConfig.defaultConfigs().toMutableList()
                }
            }
            val index = current.indexOfFirst { it.id == config.id }
            if (index >= 0) {
                current[index] = config
            } else {
                current.add(config)
            }
            prefs[modelsKey] = gson.toJson(current)
        }
    }

    suspend fun deleteModel(id: String) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[modelsKey] ?: return@edit
            try {
                val type = object : TypeToken<MutableList<ModelConfig>>() {}.type
                val current = gson.fromJson<MutableList<ModelConfig>>(currentJson, type) ?: return@edit
                current.removeAll { it.id == id }
                prefs[modelsKey] = gson.toJson(current)
            } catch (_: Exception) {
            }
        }
    }
}
