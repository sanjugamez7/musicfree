/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AutoRadioQueueKey
import com.metrolist.music.constants.SuggestionItemHeight
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun OnlineSearchScreen(
    query: String,
    isSearchActive: Boolean,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    pureBlack: Boolean,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val coroutineScope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val richHistory by viewModel.richHistory.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()

    val autoRadioQueue by rememberPreference(AutoRadioQueueKey, defaultValue = true)

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        snapshotFlow { query }.debounce(300L).collectLatest {
            viewModel.query.value = it
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
        modifier =
            Modifier
                .fillMaxSize()
                .background(if (isSearchActive) Color.Black else if (pureBlack) Color(0xFF121212) else MaterialTheme.colorScheme.background),
    ) {
        // Discovery View (Idle)
        if (query.isEmpty() && !isSearchActive) {
            // Daily Hits
            if (viewState.featuredPlaylists.isNotEmpty()) {
                item(key = "featured_playlists_title") { NavigationTitle(title = stringResource(R.string.daily_hits)) }
                item(key = "featured_playlists_list") {
                    PlaylistLazyRow(viewState.featuredPlaylists, coroutineScope, navController, viewModel::onSearchItemClick, onDismiss)
                }
            }

            // New Releases
            if (viewState.newReleases.isNotEmpty()) {
                item(key = "new_releases_title") { NavigationTitle(title = stringResource(R.string.new_releases)) }
                item(key = "new_releases_list") {
                    PlaylistLazyRow(viewState.newReleases, coroutineScope, navController, viewModel::onSearchItemClick, onDismiss)
                }
            }

            // Romantic Hits
            if (viewState.romanticHits.isNotEmpty()) {
                item(key = "romantic_hits_title") { NavigationTitle(title = stringResource(R.string.romantic_hits)) }
                item(key = "romantic_hits_list") {
                    PlaylistLazyRow(viewState.romanticHits, coroutineScope, navController, viewModel::onSearchItemClick, onDismiss)
                }
            }

            // Lofi Hits
            if (viewState.lofiHits.isNotEmpty()) {
                item(key = "lofi_hits_title") { NavigationTitle(title = stringResource(R.string.lofi_hits)) }
                item(key = "lofi_hits_list") {
                    PlaylistLazyRow(viewState.lofiHits, coroutineScope, navController, viewModel::onSearchItemClick, onDismiss)
                }
            }

            // Dance
            if (viewState.dance.isNotEmpty()) {
                item(key = "dance_title") { NavigationTitle(title = stringResource(R.string.dance)) }
                item(key = "dance_list") {
                    PlaylistLazyRow(viewState.dance, coroutineScope, navController, viewModel::onSearchItemClick, onDismiss)
                }
            }

            // Top Charts
            if (viewState.topCharts.isNotEmpty()) {
                item(key = "top_charts_title") { NavigationTitle(title = stringResource(R.string.top_charts)) }
                item(key = "top_charts_list") {
                    PlaylistLazyRow(viewState.topCharts, coroutineScope, navController, viewModel::onSearchItemClick, onDismiss)
                }
            }
        }

        // Search View (Active)
        if (isSearchActive || query.isNotEmpty()) {
            // Show parsed URL item if present
            if (viewState.isUrlQuery && viewState.parsedUrlItem != null) {
                item(key = "parsed_url_header") {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.parsed_from_link),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }

                item(key = "parsed_url_item") {
                    val item = viewState.parsedUrlItem!!
                    YouTubeListItem(
                        item = item,
                        isActive =
                            when (item) {
                                is SongItem -> mediaMetadata?.id == item.id
                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                is EpisodeItem -> mediaMetadata?.id == item.id
                                else -> false
                            },
                        isPlaying = isPlaying,
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        when (item) {
                                            is SongItem -> YouTubeSongMenu(item, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is AlbumItem -> YouTubeAlbumMenu(item, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is ArtistItem -> YouTubeArtistMenu(item, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is EpisodeItem -> YouTubeSongMenu(item.asSongItem(), onDismiss = { menuState.dismiss(); onDismiss() })
                                        }
                                    }
                                },
                            ) {
                                Icon(painterResource(R.drawable.more_vert), null)
                            }
                        },
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {
                                    viewModel.onSearchItemClick(item)
                                    when (item) {
                                        is SongItem -> {
                                            if (item.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                            else { playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue.radio(item.toMediaMetadata()) else ListQueue(item.title, listOf(item.toMediaItem()))); onDismiss() }
                                        }
                                        is AlbumItem -> { navController.navigate("album/${item.id}"); onDismiss() }
                                        is ArtistItem -> { navController.navigate("artist/${item.id}"); onDismiss() }
                                        is PlaylistItem -> { navController.navigate("online_playlist/${item.id}"); onDismiss() }
                                        is PodcastItem -> { navController.navigate("online_podcast/${item.id}"); onDismiss() }
                                        is EpisodeItem -> {
                                            if (item.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                            else { playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata())); onDismiss() }
                                        }
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        when (item) {
                                            is SongItem -> YouTubeSongMenu(item, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is AlbumItem -> YouTubeAlbumMenu(item, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is ArtistItem -> YouTubeArtistMenu(item, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is EpisodeItem -> YouTubeSongMenu(item.asSongItem(), onDismiss = { menuState.dismiss(); onDismiss() })
                                        }
                                    }
                                }
                            ).background(if (pureBlack) Color(0xFF121212) else MaterialTheme.colorScheme.surface)
                    )
                }

                item(key = "parsed_url_divider") {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }
            }

            // History (Spotify Style)
            if (query.isEmpty() && richHistory.isNotEmpty()) {
                items(richHistory, key = { "rich_history_${it.id}" }) { item ->
                    RecentSearchRow(
                        item = item,
                        onItemClick = {
                            viewModel.onSearchItemClick(item)
                            when (item) {
                                is SongItem -> {
                                    if (item.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                    else {
                                        playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue.radio(item.toMediaMetadata()) else ListQueue(item.title, listOf(item.toMediaItem())))
                                    }
                                }
                                is AlbumItem -> navController.navigate("album/${item.id}")
                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                                is EpisodeItem -> {
                                    if (item.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                    else playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                                }
                            }
                            onDismiss()
                        },
                        onRemoveClick = { viewModel.removeFromHistory(item.id) }
                    )
                }

                item(key = "clear_all_history") {
                    var showClearConfirm by remember { mutableStateOf(false) }

                    if (showClearConfirm) {
                        AlertDialog(
                            onDismissRequest = { showClearConfirm = false },
                            title = { Text("Clear all recent searches?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.clearHistory()
                                    showClearConfirm = false
                                }) {
                                    Text("Clear", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearConfirm = false }) {
                                    Text("Cancel")
                                }
                            },
                            containerColor = Color(0xFF1C1C1C),
                            titleContentColor = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFB3B3B3),
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .clickable { showClearConfirm = true }
                                .padding(16.dp)
                        )
                    }
                }
            }

            // Suggestions and Results (only if query is not empty)
            if (query.isNotEmpty()) {
                items(viewState.suggestions, key = { "suggestion_$it" }) { query ->
                    SuggestionItem(
                        query = query,
                        online = true,
                        onClick = { onSearch(query); onDismiss() },
                        onFillTextField = { onQueryChange(TextFieldValue(query, TextRange(query.length))) },
                        pureBlack = pureBlack,
                    )
                }

                if (viewState.items.isNotEmpty()) {
                    item(key = "search_divider") { HorizontalDivider() }
                    item(key = "search_divider_spacer") { Spacer(Modifier.height(8.dp)) }
                }

                items(viewState.items, key = { "item_${it.id}" }) { item ->
                    YouTubeListItem(
                        item = item,
                        isActive = when (item) {
                            is SongItem -> mediaMetadata?.id == item.id
                            is AlbumItem -> mediaMetadata?.album?.id == item.id
                            is EpisodeItem -> mediaMetadata?.id == item.id
                            else -> false
                        },
                        isPlaying = isPlaying,
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        when (item) {
                                            is SongItem -> YouTubeSongMenu(item, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is AlbumItem -> YouTubeAlbumMenu(item, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is ArtistItem -> YouTubeArtistMenu(item, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is EpisodeItem -> YouTubeSongMenu(item.asSongItem(), onDismiss = { menuState.dismiss(); onDismiss() })
                                        }
                                    }
                                }
                            ) { Icon(painterResource(R.drawable.more_vert), null) }
                        },
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {
                                    viewModel.onSearchItemClick(item)
                                    when (item) {
                                        is SongItem -> {
                                            if (item.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                            else { playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue.radio(item.toMediaMetadata()) else ListQueue(item.title, listOf(item.toMediaItem()))); onDismiss() }
                                        }
                                        is AlbumItem -> { navController.navigate("album/${item.id}"); onDismiss() }
                                        is ArtistItem -> { navController.navigate("artist/${item.id}"); onDismiss() }
                                        is PlaylistItem -> { navController.navigate("online_playlist/${item.id}"); onDismiss() }
                                        is PodcastItem -> { navController.navigate("online_podcast/${item.id}"); onDismiss() }
                                        is EpisodeItem -> {
                                            if (item.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                            else { playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata())); onDismiss() }
                                        }
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        when (item) {
                                            is SongItem -> YouTubeSongMenu(item, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is AlbumItem -> YouTubeAlbumMenu(item, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is ArtistItem -> YouTubeArtistMenu(item, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                            is EpisodeItem -> YouTubeSongMenu(item.asSongItem(), onDismiss = { menuState.dismiss(); onDismiss() })
                                        }
                                    }
                                }
                            ).background(if (pureBlack) Color(0xFF121212) else MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistLazyRow(
    items: List<PlaylistItem>,
    coroutineScope: CoroutineScope,
    navController: NavController,
    onSearchItemClick: (YTItem) -> Unit,
    onDismiss: () -> Unit
) {
    LazyRow(
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        items(items, key = { "playlist_${it.id}" }) { item ->
            YouTubeGridItem(
                item = item,
                coroutineScope = coroutineScope,
                isActive = false,
                isPlaying = false,
                modifier = Modifier.combinedClickable(
                    onClick = {
                        onSearchItemClick(item)
                        navController.navigate("online_playlist/${item.id}")
                        onDismiss()
                    }
                )
            )
        }
    }
}

@Composable
fun RecentSearchRow(
    item: YTItem,
    onItemClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val title = item.title
    val subtitle = when (item) {
        is SongItem -> item.artists.joinToString { it.name }
        is AlbumItem -> item.artists?.joinToString { it.name } ?: ""
        is ArtistItem -> "Artist"
        is PlaylistItem -> item.author?.name ?: ""
        is PodcastItem -> item.author?.name ?: ""
        is EpisodeItem -> item.author?.name ?: ""
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onItemClick)
            .padding(horizontal = 16.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.thumbnail)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.DarkGray)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFB3B3B3)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onRemoveClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = "Remove",
                tint = Color(0xFFB3B3B3),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
    pureBlack: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .height(SuggestionItemHeight)
                .background(if (pureBlack) Color(0xFF121212) else MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Icon(
            painterResource(if (online) R.drawable.search else R.drawable.history),
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 16.dp).alpha(0.5f),
        )

        Text(
            text = query,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (!online) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.alpha(0.5f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                )
            }
        }

        IconButton(
            onClick = onFillTextField,
            modifier = Modifier.alpha(0.5f),
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_top_left),
                contentDescription = null,
            )
        }
    }
}
