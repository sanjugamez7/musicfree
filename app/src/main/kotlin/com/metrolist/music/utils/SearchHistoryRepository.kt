/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.innertube.models.YTItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val historyKey = stringPreferencesKey("spotify_search_history")
    private val json = Json { ignoreUnknownKeys = true }

    val history: Flow<List<YTItem>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[historyKey] ?: "[]"
        try {
            json.decodeFromString<List<YTItem>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addSearch(item: YTItem) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[historyKey] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<YTItem>>(currentJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            // Remove if already exists to move to top
            currentList.removeAll { it.id == item.id }
            
            // Add to top
            currentList.add(0, item)

            // Limit to 20
            val limitedList = currentList.take(20)
            
            preferences[historyKey] = json.encodeToString(limitedList)
        }
    }

    suspend fun removeSearch(itemId: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[historyKey] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<YTItem>>(currentJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            currentList.removeAll { it.id == itemId }
            
            preferences[historyKey] = json.encodeToString(currentList)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(historyKey)
        }
    }
}
