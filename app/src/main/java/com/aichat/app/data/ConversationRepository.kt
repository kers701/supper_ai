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

private val Context.conversationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_chat_conversations"
)

class ConversationRepository(private val context: Context) {

    private val gson = Gson()
    private val conversationsKey = stringPreferencesKey("conversations")
    private val currentIdKey = stringPreferencesKey("current_conversation_id")

    val conversations: Flow<List<Conversation>> = context.conversationDataStore.data.map { prefs ->
        parseList(prefs[conversationsKey])
    }

    val currentConversationId: Flow<String?> = context.conversationDataStore.data.map { prefs ->
        prefs[currentIdKey]
    }

    private fun parseList(json: String?): List<Conversation> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<Conversation>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun setCurrentId(id: String) {
        context.conversationDataStore.edit { prefs ->
            prefs[currentIdKey] = id
        }
    }

    suspend fun upsert(conversation: Conversation) {
        context.conversationDataStore.edit { prefs ->
            val list = parseList(prefs[conversationsKey]).toMutableList()
            val idx = list.indexOfFirst { it.id == conversation.id }
            val updated = conversation.copy(updatedAt = System.currentTimeMillis())
            if (idx >= 0) list[idx] = updated else list.add(0, updated)
            list.sortByDescending { it.updatedAt }
            prefs[conversationsKey] = gson.toJson(list)
            prefs[currentIdKey] = conversation.id
        }
    }

    suspend fun delete(id: String) {
        context.conversationDataStore.edit { prefs ->
            val list = parseList(prefs[conversationsKey]).toMutableList()
            list.removeAll { it.id == id }
            prefs[conversationsKey] = gson.toJson(list)
            if (prefs[currentIdKey] == id) {
                prefs[currentIdKey] = list.firstOrNull()?.id ?: ""
            }
        }
    }

    suspend fun clearAll() {
        context.conversationDataStore.edit { prefs ->
            prefs.remove(conversationsKey)
            prefs.remove(currentIdKey)
        }
    }
}
