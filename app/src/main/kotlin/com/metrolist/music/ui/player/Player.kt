/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player
import androidx.compose.foundation.layout.offset
import androidx.activity.compose.BackHandler
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import com.metrolist.music.LocalNavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.HidePlayerThumbnailKey
import com.metrolist.music.constants.HideStatusBarOnFullscreenKey
import com.metrolist.music.constants.KeepScreenOn
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerButtonsStyle
import com.metrolist.music.constants.PlayerButtonsStyleKey
import com.metrolist.music.constants.PlayerHorizontalPadding
import com.metrolist.music.constants.QueuePeekHeight
import com.metrolist.music.constants.SleepTimerDefaultKey
import com.metrolist.music.constants.SleepTimerFadeOutKey
import com.metrolist.music.constants.SleepTimerStopAfterCurrentSongKey
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.constants.UseNewPlayerDesignKey
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.listentogether.RoomRole
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.BottomSheet
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.BlinkIconButton
import com.metrolist.music.ui.component.blinkClickable
import com.metrolist.music.ui.theme.FluxTheme
import com.metrolist.music.ui.theme.extractThemeColor
import coil3.toBitmap
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.component.ResizableIconButton
import com.metrolist.music.ui.component.SpotifySeekBar
import com.metrolist.music.ui.component.rememberBottomSheetState
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.theme.PlayerColorExtractor
import com.metrolist.music.ui.theme.PlayerSliderColors
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.ui.utils.ShowOffsetDialog
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.safeDataStoreEdit
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import com.metrolist.music.ui.component.Icon as MIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val sleepTimerDefaultSetTemplate = stringResource(R.string.sleep_timer_default_set)
    val copiedTitleStr = stringResource(R.string.copied_title)
    val copiedArtistStr = stringResource(R.string.copied_artist)
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var dynamicGradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val paletteColorCache = remember { mutableMapOf<String, List<Color>>() }

    LaunchedEffect(mediaMetadata?.id) {
        val currentId = mediaMetadata?.id
        val url = mediaMetadata?.thumbnailUrl
        
        if (currentId == null || url == null) {
            dynamicGradientColors = emptyList()
            return@LaunchedEffect
        }

        val cachedColors = paletteColorCache[currentId]
        if (cachedColors != null) {
            dynamicGradientColors = cachedColors
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(200, 200)
                .allowHardware(false)
                .build()
            
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            val bitmap = result?.image?.toBitmap()
            if (bitmap != null) {
                val palette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap)
                        .maximumColorCount(32)
                        .generate()
                }
                val extracted = PlayerColorExtractor.extractGradientColors(
                    palette = palette,
                    fallbackColor = 0xFF121212.toInt()
                )
                paletteColorCache[currentId] = extracted
                withContext(Dispatchers.Main) {
                    dynamicGradientColors = extracted
                }
            }
        }
    }

    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, false)
    val (hideStatusBarOnFullscreen) = rememberPreference(HideStatusBarOnFullscreenKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)

    var showInlineLyrics by rememberSaveable {
        mutableStateOf(false)
    }

    var isFullScreen by rememberSaveable {
        mutableStateOf(false)
    }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT,
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme =
        remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val isKeepScreenOn by rememberPreference(KeepScreenOn, false)
    val keepScreenOn = isPlaying && isKeepScreenOn

    DisposableEffect(playerBackground, state.isExpanded, useDarkTheme, keepScreenOn, isFullScreen, hideStatusBarOnFullscreen) {
        val window = (context as? android.app.Activity)?.window
        if (window != null && state.isExpanded) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = false

            if (isFullScreen && hideStatusBarOnFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }

            if (keepScreenOn && state.isExpanded) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !useDarkTheme
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    BackHandler(enabled = state.isExpanded) {
        state.collapseSoft()
    }

    val useBlackBackground = true // Force dark theme for player

    val playbackState by playerConnection.playbackState.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val automix by playerConnection.service.automixItems.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    // Listen Together state
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

    // Cast state
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castPosition by castHandler?.castPosition?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castDuration by castHandler?.castDuration?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isExpanded) {
        if (state.isExpanded) {
            delay(100)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) { }
        }
    }

    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    val positionState = remember { mutableLongStateOf(runCatching { playerConnection.player.currentPosition }.getOrDefault(0L)) }
    val durationState = remember {
        mutableLongStateOf(
            (mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L))
                ?: runCatching { playerConnection.player.duration }.getOrDefault(0L).coerceAtLeast(0L),
        )
    }

    var position by positionState
    var duration by durationState

    val effectivePosition by remember {
        derivedStateOf {
            if (isCasting) castPosition else position
        }
    }

    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    var lastManualSeekTime by remember { mutableLongStateOf(0L) }

    val scope = rememberCoroutineScope()

    // Position update
    LaunchedEffect(isPlaying, isCasting) {
        if (!isCasting && isPlaying) {
            while (isActive) {
                delay(100)
                if (sliderPosition == null) {
                    position = playerConnection.player.currentPosition
                    playerConnection.player.duration.takeIf { it > 0 }?.let { duration = it }
                }
            }
        }
    }

    LaunchedEffect(playbackState, mediaMetadata?.id) {
        if (!isCasting) {
            position = playerConnection.player.currentPosition
            duration = (mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L))
                ?: playerConnection.player.duration
        }
    }

    val dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    val queueSheetState =
        rememberBottomSheetState(
            dismissedBound = dismissedBound,
            expandedBound = state.expandedBound,
            collapsedBound = dismissedBound + 1.dp,
            initialAnchor = 1,
        )

    val backgroundAlpha = state.progress.coerceIn(0f, 1f)

    val color1 by animateColorAsState(
        targetValue = dynamicGradientColors.getOrNull(0) ?: Color(0xFF121212),
        animationSpec = tween(durationMillis = 500),
        label = "gradient1"
    )
    val color2 by animateColorAsState(
        targetValue = dynamicGradientColors.getOrNull(1) ?: Color(0xFF121212),
        animationSpec = tween(durationMillis = 500),
        label = "gradient2"
    )
    val color3 by animateColorAsState(
        targetValue = dynamicGradientColors.getOrNull(2) ?: Color(0xFF0D0D0D),
        animationSpec = tween(durationMillis = 500),
        label = "gradient3"
    )

    FluxTheme(
        darkTheme = true,
        pureBlack = true,
        themeColor = Color.White,
    ) {
        BottomSheet(
            state = state,
            modifier = modifier,
            background = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(color1, color2, color3)
                                )
                            )
                            .background(Color.Black.copy(alpha = 0.2f)),
                )
            },
            onDismiss =
                if (!isListenTogetherGuest) {
                    {
                        playerConnection.service.clearAutomix()
                        playerConnection.player.stop()
                        playerConnection.player.clearMediaItems()
                    }
                } else null,
            collapsedContent = {
                MiniPlayer(
                    positionState = positionState,
                    durationState = durationState,
                    onClick = { state.expandSoft() },
                )
            },
        ) {
            val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
                // Song Info Top Margin: 32dp from artwork
                Spacer(Modifier.height(32.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = mediaMetadata.title,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "title",
                        ) { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.W700
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White,
                                modifier = Modifier
                                    .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                    .combinedClickable(
                                        enabled = true,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = {
                                            val albumId = mediaMetadata.album?.id ?: currentSong?.album?.id ?: currentSong?.song?.albumId
                                            if (albumId != null) {
                                                navController.navigate("album/$albumId")
                                                state.collapseSoft()
                                            }
                                        },
                                        onLongClick = {
                                            val clip = ClipData.newPlainText(copiedTitleStr, title)
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast.makeText(context, copiedTitleStr, Toast.LENGTH_SHORT).show()
                                        },
                                    ),
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                            val annotatedString = buildAnnotatedString {
                                mediaMetadata.artists.forEachIndexed { index, artist ->
                                    val tag = "artist_${artist.id.orEmpty()}"
                                    pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                                    withStyle(SpanStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, fontWeight = FontWeight.W500)) {
                                        append(artist.name)
                                    }
                                    pop()
                                    if (index != mediaMetadata.artists.lastIndex) append(", ")
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                            ) {
                                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                var clickOffset by remember { mutableStateOf<Offset?>(null) }
                                Text(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    onTextLayout = { layoutResult = it },
                                    modifier = Modifier
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val tapPosition = event.changes.firstOrNull()?.position
                                                    if (tapPosition != null) clickOffset = tapPosition
                                                }
                                            }
                                        }
                                        .combinedClickable(
                                            enabled = true,
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = {
                                                val tapPosition = clickOffset
                                                val layout = layoutResult
                                                if (tapPosition != null && layout != null) {
                                                    val offset = layout.getOffsetForPosition(tapPosition)
                                                    annotatedString.getStringAnnotations(offset, offset).firstOrNull()?.let { ann ->
                                                        val artistId = ann.item
                                                        if (artistId.isNotBlank()) {
                                                            navController.navigate("artist/$artistId")
                                                            state.collapseSoft()
                                                        }
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                val clip = ClipData.newPlainText(copiedArtistStr, annotatedString)
                                                clipboardManager.setPrimaryClip(clip)
                                                Toast.makeText(context, copiedArtistStr, Toast.LENGTH_SHORT).show()
                                            },
                                        ),
                                )
                            }
                        }
                    }

                    // Like button next to title/author row
                    val isEpisode = currentSong?.song?.isEpisode == true
                    val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                    BlinkIconButton(
                        onClick = playerConnection::toggleLike,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border),
                            contentDescription = null,
                            tint = if (isFavorite) Color(0xFFED5564) else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Progress Bar Margin from Artist: 34dp
                Spacer(Modifier.height(34.dp))

                SpotifySeekBar(
                    value = (sliderPosition ?: effectivePosition).toFloat(),
                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                    onValueChange = { if (!isListenTogetherGuest) sliderPosition = it.toLong() },
                    onValueChangeFinished = {
                        if (!isListenTogetherGuest) {
                            sliderPosition?.let {
                                if (isCasting) {
                                    castHandler?.seekTo(it)
                                    lastManualSeekTime = System.currentTimeMillis()
                                } else {
                                    playerConnection.player.seekTo(it)
                                }
                                position = it
                            }
                            sliderPosition = null
                        }
                    },
                    enabled = !isListenTogetherGuest,
                    colors = PlayerSliderColors.getSliderColors(Color.White, playerBackground, true),
                    modifier = Modifier.padding(horizontal = 32.dp).height(3.dp),
                )

                // Time labels top margin: 10dp
                Spacer(Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                ) {
                    Text(
                        text = makeTimeString(sliderPosition ?: effectivePosition),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                    Text(
                        text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }

                // Playback controls top margin from timestamps: 44dp
                Spacer(Modifier.height(44.dp))

                AnimatedVisibility(
                    visible = !isFullScreen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
                        BlinkIconButton(
                            onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
                            enabled = !isListenTogetherGuest,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(if (shuffleModeEnabled) R.drawable.ic_player_shuffle_on else R.drawable.ic_player_shuffle),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.width(30.dp))

                        BlinkIconButton(
                            onClick = playerConnection::seekToPrevious,
                            enabled = canSkipPrevious && !isListenTogetherGuest,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_player_previous),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        Spacer(Modifier.width(28.dp))

                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .blinkClickable(
                                    enabled = true,
                                    onClick = {
                                        if (isListenTogetherGuest) {
                                            playerConnection.toggleMute()
                                        } else if (isCasting) {
                                            if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                                        } else if (playbackState == Player.STATE_ENDED) {
                                            playerConnection.player.seekTo(0, 0)
                                            playerConnection.player.playWhenReady = true
                                        } else {
                                            playerConnection.togglePlayPause()
                                        }
                                    }
                                )
                                .focusRequester(focusRequester),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (effectiveIsPlaying) R.drawable.ic_player_pause_circle else R.drawable.ic_player_play_circle
                                ),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        Spacer(Modifier.width(28.dp))

                        BlinkIconButton(
                            onClick = playerConnection::seekToNext,
                            enabled = canSkipNext && !isListenTogetherGuest,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_player_next),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        Spacer(Modifier.width(30.dp))

                        BlinkIconButton(
                            onClick = { playerConnection.player.toggleRepeatMode() },
                            enabled = !isListenTogetherGuest,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    when (repeatMode) {
                                        Player.REPEAT_MODE_ONE -> R.drawable.ic_player_repeat_once
                                        Player.REPEAT_MODE_ALL -> R.drawable.ic_player_repeat_on
                                        else -> R.drawable.ic_player_repeat_off
                                    }
                                ),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .offset(y = 2.dp)

                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (LocalConfiguration.current.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        val density = LocalDensity.current
                        val verticalPadding = max(WindowInsets.systemBars.getTop(density), WindowInsets.systemBars.getBottom(density))
                        val verticalPaddingDp = with(density) { verticalPadding.toDp() }
                        val verticalWindowInsets = WindowInsets(left = 0.dp, top = verticalPaddingDp, right = 0.dp, bottom = verticalPaddingDp)

                        Row(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).add(verticalWindowInsets))
                                .padding(bottom = 24.dp)
                                .fillMaxSize(),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.weight(1f).nestedScroll(state.preUpPostDownNestedScrollConnection),
                            ) {
                                val currentSliderPosition by rememberUpdatedState(sliderPosition)
                                val sliderPositionProvider = remember { { currentSliderPosition } }
                                val isExpandedProvider = remember(state) { { state.isExpanded } }
                                AnimatedContent(
                                    targetState = showInlineLyrics,
                                    label = "Lyrics",
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                ) { showLyrics ->
                                    if (showLyrics) {
                                        InlineLyricsView(
                                            mediaMetadata = mediaMetadata,
                                            showLyrics = showLyrics,
                                            positionProvider = { effectivePosition },
                                        )
                                    } else {
                                        Thumbnail(
                                            sliderPositionProvider = sliderPositionProvider,
                                            state = state,
                                            modifier = Modifier.animateContentSize(),
                                            isPlayerExpanded = isExpandedProvider,
                                            isLandscape = true,
                                            isListenTogetherGuest = isListenTogetherGuest,
                                        )
                                    }
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(if (showInlineLyrics) 0.65f else 1f, false)
                                    .animateContentSize()
                                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
                            ) {
                                Spacer(Modifier.weight(1f))
                                mediaMetadata?.let { controlsContent(it) }
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    else -> {
                        val bottomPadding by animateDpAsState(
                            targetValue = if (isFullScreen) 0.dp else queueSheetState.collapsedBound,
                            label = "bottomPadding",
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                .padding(bottom = bottomPadding)
                                .animateContentSize(),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.weight(1f),
                            ) {
                                val currentSliderPosition by rememberUpdatedState(sliderPosition)
                                val sliderPositionProvider = remember { { currentSliderPosition } }
                                val isExpandedProvider = remember(state) { { state.isExpanded } }
                                AnimatedContent(
                                    targetState = showInlineLyrics,
                                    label = "Lyrics",
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                ) { showLyrics ->
                                    if (showLyrics) {
                                        InlineLyricsView(
                                            mediaMetadata = mediaMetadata,
                                            showLyrics = showLyrics,
                                            positionProvider = { effectivePosition },
                                        )
                                    } else {
                                        Thumbnail(
                                            sliderPositionProvider = sliderPositionProvider,
                                            state = state,
                                            modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                                            isPlayerExpanded = isExpandedProvider,
                                            isListenTogetherGuest = isListenTogetherGuest,
                                        )
                                    }
                                }
                            }
                            mediaMetadata?.let { controlsContent(it) }
                            Spacer(Modifier.height(30.dp))
                        }
                    }
                }

                // Bottom right Queue icon: 40dp above bottom safe area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                        .padding(bottom = 40.dp)
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BlinkIconButton(
                        onClick = { queueSheetState.expandSoft() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_player_queue),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isFullScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Queue(
                    state = queueSheetState,
                    playerBottomSheetState = state,
                    background = if (useBlackBackground) Color(0xFF121212) else MaterialTheme.colorScheme.surfaceContainer,
                    onBackgroundColor = Color.White,
                    TextBackgroundColor = Color.White,
                    textButtonColor = Color.White,
                    iconButtonColor = Color.Black,
                    pureBlack = true,
                    showInlineLyrics = showInlineLyrics,
                    playerBackground = playerBackground,
                    onToggleLyrics = { showInlineLyrics = !showInlineLyrics },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InlineLyricsView(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    positionProvider: () -> Long,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle(initialValue = -1)
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    var appInForeground by remember {
        mutableStateOf(ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(Unit) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, _ ->
            appInForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val nextMetadata = remember(queueWindows, currentWindowIndex) {
        if (currentWindowIndex >= 0 && currentWindowIndex + 1 < queueWindows.size) {
            queueWindows[currentWindowIndex + 1].mediaItem.metadata
        } else null
    }

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, com.metrolist.music.di.LyricsHelperEntryPoint::class.java)
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                    database.query {
                        upsert(LyricsEntity(mediaMetadata.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider))
                    }
                } catch (e: Exception) { }
            }
        }
    }

    LaunchedEffect(nextMetadata?.id, showLyrics, appInForeground, mediaMetadata?.id, currentLyrics) {
        if (!showLyrics || !appInForeground || nextMetadata == null) return@LaunchedEffect
        val loadedForCurrent = currentLyrics?.let { lyrics -> mediaMetadata == null || lyrics.id == mediaMetadata.id } == true
        if (mediaMetadata != null && !loadedForCurrent) return@LaunchedEffect
        val nextId = nextMetadata.id
        delay(400)
        if (!showLyrics || !appInForeground || !isActive) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val existing = database.lyrics(nextId).first()
                if (existing != null) return@withContext
                val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, com.metrolist.music.di.LyricsHelperEntryPoint::class.java)
                val lyricsHelper = entryPoint.lyricsHelper()
                val fetched = lyricsHelper.getLyrics(nextMetadata)
                database.query {
                    upsert(LyricsEntity(nextId, fetched.lyrics, fetched.provider))
                }
            } catch (_: Exception) { }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            lyrics == null -> ContainedLoadingIndicator()
            lyrics == LyricsEntity.LYRICS_NOT_FOUND -> {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
            else -> {
                val lyricsContent: @Composable () -> Unit = {
                    Lyrics(
                        sliderPositionProvider = positionProvider,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        showLyrics = showLyrics,
                    )
                }
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, textAlign = TextAlign.Center),
                    content = lyricsContent
                )
            }
        }
    }
}

@Composable
fun MoreActionsButton(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    state: BottomSheetState,
    textButtonColor: Color,
    iconButtonColor: Color,
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(textButtonColor)
            .clickable {
                menuState.show {
                    PlayerMenu(
                        mediaMetadata = mediaMetadata,
                        playerBottomSheetState = state,
                        onShowDetailsDialog = {
                            mediaMetadata.id.let {
                                bottomSheetPageState.show { ShowMediaInfo(it) }
                            }
                        },
                        onDismiss = menuState::dismiss,
                    )
                }
            },
    ) {
        Image(
            painter = painterResource(R.drawable.more_horiz),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconButtonColor),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun PlayerMoreMenuButton(
    mediaMetadata: MediaMetadata,
    state: BottomSheetState,
    textButtonColor: Color,
    iconButtonColor: Color,
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(textButtonColor)
            .clickable {
                menuState.show {
                    PlayerMenu(
                        mediaMetadata = mediaMetadata,
                        playerBottomSheetState = state,
                        onShowDetailsDialog = {
                            mediaMetadata.id.let {
                                bottomSheetPageState.show { ShowMediaInfo(it) }
                            }
                        },
                        onDismiss = menuState::dismiss,
                    )
                }
            },
    ) {
        Image(
            painter = painterResource(R.drawable.more_horiz),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconButtonColor),
        )
    }
}
