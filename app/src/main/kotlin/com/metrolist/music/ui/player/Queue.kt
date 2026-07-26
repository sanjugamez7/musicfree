/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.QueueEditLockKey
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.move
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.listentogether.RoomRole
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.BottomSheet
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.MediaMetadataListItem
import com.metrolist.music.ui.component.BlinkIconButton
import com.metrolist.music.ui.menu.QueueMenu
import com.metrolist.music.ui.menu.SelectionMediaMetadataMenu
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Queue(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
    background: Color,
    onBackgroundColor: Color,
    TextBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    pureBlack: Boolean,
    showInlineLyrics: Boolean,
    playerBackground: PlayerBackgroundStyle = PlayerBackgroundStyle.DEFAULT,
    onToggleLyrics: () -> Unit = {},
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    // Listen Together state
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()

    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    // Cast state
    val castHandler = remember(playerConnection) {
        try { playerConnection.service.castConnectionHandler } catch (e: Exception) { null }
    }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    var locked by rememberPreference(QueueEditLockKey, defaultValue = true)
    val snackbarHostState = remember { SnackbarHostState() }
    var dismissJob: Job? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(Modifier.fillMaxSize().background(Color.Unspecified))
        },
        collapsedContent = { },
    ) {
        val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
        val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle()
        val automix by playerConnection.service.automixItems.collectAsStateWithLifecycle()
        val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
        val queueLength = remember(queueWindows) {
            queueWindows.sumOf { it.mediaItem.metadata?.duration ?: 0 }
        }

        val headerItems = 1
        val lazyListState = rememberLazyListState()
        var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }

        val currentPlayingUid = remember(currentWindowIndex, queueWindows) {
            if (currentWindowIndex in queueWindows.indices) queueWindows[currentWindowIndex].uid else null
        }

        val reorderableState = rememberReorderableLazyListState(
            lazyListState = lazyListState,
            scrollThresholdPadding = WindowInsets.systemBars.add(WindowInsets(top = ListItemHeight, bottom = ListItemHeight)).asPaddingValues(),
        ) { from, to ->
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) from.index to to.index else currentDragInfo.first to to.index
            val safeFrom = (from.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
            val safeTo = (to.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
            mutableQueueWindows.move(safeFrom, safeTo)
        }

        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (!reorderableState.isAnyItemDragging) {
                dragInfo?.let { (from, to) ->
                    val safeFrom = (from - headerItems).coerceIn(0, queueWindows.lastIndex)
                    val safeTo = (to - headerItems).coerceIn(0, queueWindows.lastIndex)
                    if (!playerConnection.player.shuffleModeEnabled) {
                        playerConnection.player.moveMediaItem(safeFrom, safeTo)
                    } else {
                        playerConnection.player.setShuffleOrder(DefaultShuffleOrder(queueWindows.map { it.firstPeriodIndex }.toMutableList().move(safeFrom, safeTo).toIntArray(), System.currentTimeMillis()))
                    }
                    dragInfo = null
                }
            }
        }

        LaunchedEffect(queueWindows) {
            mutableQueueWindows.apply {
                clear()
                addAll(queueWindows)
            }
        }

        LaunchedEffect(mutableQueueWindows, currentWindowIndex) {
            if (currentWindowIndex != -1) lazyListState.scrollToItem(currentWindowIndex)
        }

        Box(modifier = Modifier.fillMaxSize().background(background)) {
            LazyColumn(
                state = lazyListState,
                contentPadding = WindowInsets.systemBars.add(WindowInsets(top = ListItemHeight + 8.dp, bottom = ListItemHeight + 8.dp)).asPaddingValues(),
                modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
            ) {
                item(key = "queue_top_spacer") {
                    Spacer(modifier = Modifier.animateContentSize().height(if (inSelectMode) 48.dp else 0.dp))
                }

                itemsIndexed(
                    items = mutableQueueWindows,
                    key = { _, item -> item.uid.hashCode() },
                ) { index, window ->
                    ReorderableItem(state = reorderableState, key = window.uid.hashCode()) {
                        val currentItem by rememberUpdatedState(window)
                        val isActive = window.uid == currentPlayingUid
                        val dismissBoxState = rememberSwipeToDismissBoxState(positionalThreshold = { totalDistance -> totalDistance })

                        var processedDismiss by remember { mutableStateOf(false) }
                        val removedSongMsg = stringResource(R.string.removed_song_from_playlist, currentItem.mediaItem.metadata?.title ?: "")
                        val undoStr = stringResource(R.string.undo)
                        
                        LaunchedEffect(dismissBoxState.currentValue) {
                            val dv = dismissBoxState.currentValue
                            if (!processedDismiss && !isListenTogetherGuest && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) {
                                processedDismiss = true
                                playerConnection.player.removeMediaItem(currentItem.firstPeriodIndex)
                                dismissJob?.cancel()
                                dismissJob = coroutineScope.launch {
                                    val snackbarResult = snackbarHostState.showSnackbar(message = removedSongMsg, actionLabel = undoStr, duration = SnackbarDuration.Short)
                                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                                        playerConnection.player.addMediaItem(currentItem.mediaItem)
                                        playerConnection.player.moveMediaItem(mutableQueueWindows.size, currentItem.firstPeriodIndex)
                                    }
                                }
                            }
                            if (dv == SwipeToDismissBoxValue.Settled) processedDismiss = false
                        }

                        val content: @Composable () -> Unit = {
                            Row(modifier = Modifier.animateItem()) {
                                MediaMetadataListItem(
                                    mediaMetadata = window.mediaItem.metadata!!,
                                    isSelected = false,
                                    isActive = isActive,
                                    isPlaying = isPlaying && isActive,
                                    trailingContent = {
                                        if (inSelectMode) {
                                            Checkbox(checked = window.mediaItem.mediaId in selection, onCheckedChange = { if (it) selection.add(window.mediaItem.mediaId) else selection.remove(window.mediaItem.mediaId) })
                                        } else {
                                            if (!isListenTogetherGuest) {
                                                IconButton(onClick = {
                                                    menuState.show {
                                                        QueueMenu(mediaMetadata = window.mediaItem.metadata!!, playerBottomSheetState = playerBottomSheetState, onShowDetailsDialog = { window.mediaItem.mediaId.let { id -> bottomSheetPageState.show { ShowMediaInfo(id) } } }, onDismiss = menuState::dismiss)
                                                    }
                                                }) { Icon(painterResource(R.drawable.more_vert), null) }
                                            }
                                            if (!locked && !isListenTogetherGuest) {
                                                IconButton(onClick = {}, modifier = Modifier.draggableHandle()) { Icon(painterResource(R.drawable.drag_handle), null) }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().background(background).combinedClickable(
                                        onClick = {
                                            if (inSelectMode) { if (window.mediaItem.mediaId in selection) selection.remove(window.mediaItem.mediaId) else selection.add(window.mediaItem.mediaId) }
                                            else if (!isListenTogetherGuest) {
                                                if (index == currentWindowIndex) playerConnection.togglePlayPause()
                                                else { playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex); playerConnection.player.playWhenReady = true }
                                            }
                                        },
                                        onLongClick = { if (!inSelectMode) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); inSelectMode = true; selection.add(window.mediaItem.mediaId) } },
                                    ),
                                )
                            }
                        }

                        if (locked) content() else SwipeToDismissBox(state = dismissBoxState, backgroundContent = {}, content = { content() })
                    }
                }

                if (automix.isNotEmpty()) {
                    item(key = "automix_divider") {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 4.dp).animateItem())
                        Text(stringResource(R.string.similar_content), Modifier.padding(start = 16.dp))
                    }
                    itemsIndexed(items = automix, key = { _, it -> it.mediaId }) { index, item ->
                        MediaMetadataListItem(
                            mediaMetadata = item.metadata!!,
                            trailingContent = {
                                if (!isListenTogetherGuest) {
                                    IconButton(onClick = { playerConnection.service.playNextAutomix(item, index) }) { Icon(painterResource(R.drawable.playlist_play), null) }
                                    IconButton(onClick = { playerConnection.service.addToQueueAutomix(item, index) }) { Icon(painterResource(R.drawable.queue_music), null) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { menuState.show { QueueMenu(mediaMetadata = item.metadata!!, playerBottomSheetState = playerBottomSheetState, onShowDetailsDialog = { item.mediaId.let { id -> bottomSheetPageState.show { ShowMediaInfo(id) } } }, onDismiss = menuState::dismiss) } }).animateItem(),
                        )
                    }
                }
            }
        }

        // Header
        Column(modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}.background(if (pureBlack) Color(0xFF121212) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.90f)).windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(ListItemHeight).padding(horizontal = 12.dp)) {
                Text(text = queueTitle.orEmpty(), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                
                if (!inSelectMode) {
                    IconButton(onClick = { locked = !locked }, modifier = Modifier.padding(horizontal = 6.dp)) {
                        Icon(painterResource(if (locked) R.drawable.lock else R.drawable.lock_open), null)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                    Text(pluralStringResource(R.plurals.n_song, queueWindows.size, queueWindows.size), style = MaterialTheme.typography.bodyMedium)
                    Text(makeTimeString(queueLength * 1000L), style = MaterialTheme.typography.bodyMedium)
                }
            }

            AnimatedVisibility(visible = inSelectMode, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                val selectedSongs = remember(selection.toList(), mutableQueueWindows) { mutableQueueWindows.filter { it.mediaItem.mediaId in selection }.mapNotNull { it.mediaItem.metadata } }
                val selectedItems = remember(selection.toList(), mutableQueueWindows) { mutableQueueWindows.filter { it.mediaItem.mediaId in selection } }
                Row(modifier = Modifier.height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExitSelectionMode) { Icon(painterResource(R.drawable.close), null) }
                    Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size), Modifier.weight(1f))
                    Checkbox(checked = selection.size == mutableQueueWindows.size && selection.size > 0, onCheckedChange = { if (selection.size == mutableQueueWindows.size) selection.clear() else { selection.clear(); mutableQueueWindows.forEach { selection.add(it.mediaItem.mediaId) } } })
                    IconButton(enabled = selection.size > 0, onClick = { menuState.show { SelectionMediaMetadataMenu(songSelection = selectedSongs, onDismiss = menuState::dismiss, clearAction = onExitSelectionMode, currentItems = selectedItems) } }) { Icon(painterResource(R.drawable.more_vert), null) }
                }
            }
            if (pureBlack) HorizontalDivider()
        }

        // Bottom Controls
        val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
        Box(modifier = Modifier.background(if (pureBlack) Color(0xFF121212) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.90f)).fillMaxWidth().height(ListItemHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()).align(Alignment.BottomCenter).clickable { state.collapseSoft() }.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)).padding(12.dp)) {
            IconButton(enabled = !isListenTogetherGuest, modifier = Modifier.align(Alignment.CenterStart), onClick = { coroutineScope.launch { lazyListState.animateScrollToItem(if (playerConnection.player.shuffleModeEnabled) playerConnection.player.currentMediaItemIndex else 0) }.invokeOnCompletion { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled } }) {
                Icon(painterResource(R.drawable.shuffle), null, Modifier.alpha(if (shuffleModeEnabled) 1f else 0.5f))
            }
            Icon(painterResource(R.drawable.expand_more), null, Modifier.align(Alignment.Center))
            IconButton(enabled = !isListenTogetherGuest, modifier = Modifier.align(Alignment.CenterEnd), onClick = playerConnection.player::toggleRepeatMode) {
                Icon(painterResource(when (repeatMode) { Player.REPEAT_MODE_ONE -> R.drawable.repeat_one; else -> R.drawable.repeat }), null, Modifier.alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f))
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(bottom = ListItemHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()).align(Alignment.BottomCenter))
    }
}
