/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.utils.YouTubeUrlParser
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.metrolist.music.utils.SearchHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
    @Inject
    constructor(
        @ApplicationContext val context: Context,
        database: MusicDatabase,
        private val historyRepository: SearchHistoryRepository,
    ) : ViewModel() {
        val query = MutableStateFlow("")
        private val _viewState = MutableStateFlow(SearchSuggestionViewState())
        val viewState = _viewState.asStateFlow()

        val richHistory: StateFlow<List<YTItem>> = historyRepository.history
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        private val _featuredPlaylists = MutableStateFlow<List<PlaylistItem>>(emptyList())
        private val _newReleases = MutableStateFlow<List<PlaylistItem>>(emptyList())
        private val _romanticHits = MutableStateFlow<List<PlaylistItem>>(emptyList())
        private val _lofiHits = MutableStateFlow<List<PlaylistItem>>(emptyList())
        private val _dance = MutableStateFlow<List<PlaylistItem>>(emptyList())
        private val _topCharts = MutableStateFlow<List<PlaylistItem>>(emptyList())

        init {
            fetchFeaturedPlaylists()

            viewModelScope.launch {
                query
                    .flatMapLatest { query ->
                        if (query.isEmpty()) {
                            database.searchHistory().map { history ->
                                SearchSuggestionViewState(
                                    history = history,
                                    featuredPlaylists = _featuredPlaylists.value,
                                    newReleases = _newReleases.value,
                                    romanticHits = _romanticHits.value,
                                    lofiHits = _lofiHits.value,
                                    dance = _dance.value,
                                    topCharts = _topCharts.value,
                                )
                            }
                        } else {
                            // Check if query is a YouTube URL
                            val parsedUrl = YouTubeUrlParser.parse(query)
                            if (parsedUrl != null) {
                                // Fetch content from YouTube URL
                                val parsedItem = fetchParsedUrlItem(parsedUrl)
                                database
                                    .searchHistory(query)
                                    .map { it.take(3) }
                                    .map { history ->
                                        SearchSuggestionViewState(
                                            history = history,
                                            suggestions = emptyList(),
                                            items = parsedItem?.let { listOf(it) } ?: emptyList(),
                                            parsedUrlItem = parsedItem,
                                            isUrlQuery = true,
                                        )
                                    }
                            } else {
                                val result = YouTube.searchSuggestions(query).getOrNull()
                                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

                                database
                                    .searchHistory(query)
                                    .map { it.take(3) }
                                    .map { history ->
                                        SearchSuggestionViewState(
                                            history = history,
                                            suggestions =
                                                result
                                                    ?.queries
                                                    ?.filter { suggestionQuery ->
                                                        history.none { it.query == suggestionQuery }
                                                    }.orEmpty(),
                                            items =
                                                result
                                                    ?.recommendedItems
                                                    ?.distinctBy { it.id }
                                                    ?.filterExplicit(hideExplicit)
                                                    ?.filterVideoSongs(hideVideoSongs)
                                                    .orEmpty(),
                                        )
                                    }
                            }
                        }
                    }.collect {
                        _viewState.value = it
                    }
            }
        }

        private suspend fun fetchParsedUrlItem(parsedUrl: YouTubeUrlParser.ParsedUrl): YTItem? =
            when (parsedUrl) {
                is YouTubeUrlParser.ParsedUrl.Video -> {
                    // Use next() to get the song details from a video ID
                    YouTube
                        .next(WatchEndpoint(videoId = parsedUrl.id))
                        .getOrNull()
                        ?.items
                        ?.firstOrNull()
                }

                is YouTubeUrlParser.ParsedUrl.Playlist -> {
                    // Fetch playlist details
                    YouTube
                        .playlist(parsedUrl.id)
                        .getOrNull()
                        ?.playlist
                }

                is YouTubeUrlParser.ParsedUrl.Album -> {
                    // For albums, we need to get the browseId from the playlist
                    // First, try to get the album page
                    val albumResult = YouTube.album("MPREb_${parsedUrl.id}")
                    if (albumResult.isSuccess) {
                        albumResult.getOrNull()?.album
                    } else {
                        // If that fails, treat it as a playlist
                        YouTube
                            .playlist(parsedUrl.id)
                            .getOrNull()
                            ?.playlist
                    }
                }

                is YouTubeUrlParser.ParsedUrl.Artist -> {
                    // Fetch artist details
                    if (parsedUrl.id.startsWith("MPRE")) {
                        // It's a browse ID
                        YouTube
                            .artist(parsedUrl.id)
                            .getOrNull()
                            ?.artist
                    } else {
                        // It's a channel ID, we need to find the browse ID
                        // For now, try using the channel ID as browse ID
                        YouTube
                            .artist(parsedUrl.id)
                            .getOrNull()
                            ?.artist
                    }
                }
            }

        fun onSearchItemClick(item: YTItem) {
            viewModelScope.launch {
                historyRepository.addSearch(item)
            }
        }

        fun removeFromHistory(itemId: String) {
            viewModelScope.launch {
                historyRepository.removeSearch(itemId)
            }
        }

        fun clearHistory() {
            viewModelScope.launch {
                historyRepository.clearHistory()
            }
        }

        private fun fetchFeaturedPlaylists() {
            viewModelScope.launch(Dispatchers.IO) {
                val dailyHitsQueries = listOf(
                    "Tamil Daily Hits",
                    "Malayalam Daily Hits",
                    "Bollywood Daily Hits",
                    "Today's Biggest Hits"
                )

                _featuredPlaylists.value = dailyHitsQueries.mapNotNull { q ->
                    YouTube.search(q, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                        .getOrNull()?.items?.filterIsInstance<PlaylistItem>()?.firstOrNull()
                }

                _newReleases.value = YouTube.search("New Releases", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>()?.take(8) ?: emptyList()

                _romanticHits.value = YouTube.search("Romantic Hits", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>()?.take(8) ?: emptyList()

                _lofiHits.value = YouTube.search("Lofi Hits", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>()?.take(8) ?: emptyList()

                _dance.value = YouTube.search("Dance Hits", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>()?.take(8) ?: emptyList()

                _topCharts.value = YouTube.search("Top Charts", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>()?.take(8) ?: emptyList()

                // If query is currently empty, update viewState immediately with all categories
                if (query.value.isEmpty()) {
                    _viewState.value = _viewState.value.copy(
                        featuredPlaylists = _featuredPlaylists.value,
                        newReleases = _newReleases.value,
                        romanticHits = _romanticHits.value,
                        lofiHits = _lofiHits.value,
                        dance = _dance.value,
                        topCharts = _topCharts.value,
                    )
                }
            }
        }
    }

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
    val featuredPlaylists: List<PlaylistItem> = emptyList(),
    val newReleases: List<PlaylistItem> = emptyList(),
    val romanticHits: List<PlaylistItem> = emptyList(),
    val lofiHits: List<PlaylistItem> = emptyList(),
    val dance: List<PlaylistItem> = emptyList(),
    val topCharts: List<PlaylistItem> = emptyList(),
    val parsedUrlItem: YTItem? = null,
    val isUrlQuery: Boolean = false,
)
