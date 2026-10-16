@file:OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.CaptionItem
import com.example.data.model.StreamSource
import com.example.ui.theme.*
import com.example.util.CaptionPreferences
import com.example.util.StreamTokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import java.util.Locale
import kotlin.math.roundToInt

enum class ResizeMode(val mode: Int, val label: String) {
    FIT(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fit"),
    ZOOM(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Crop/Fill"),
    FILL(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Stretch")
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer,
    title: String,
    subtitleInfo: String?,
    sources: List<StreamSource>,
    initialSourceIndex: Int = 0,
    captions: List<CaptionItem> = emptyList(),
    initialPositionMs: Long = 0L,
    isSeries: Boolean = false,
    onProgressUpdate: (positionMs: Long, durationMs: Long) -> Unit,
    onBackClick: () -> Unit,
    onRefreshStream: (resumePos: Long) -> Unit = {},
    hasPreviousEpisode: Boolean = false,
    hasNextEpisode: Boolean = false,
    onPreviousEpisode: () -> Unit = {},
    onNextEpisode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val isInPip = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        activity?.isInPictureInPictureMode == true
    } else false

    var selectedSourceIndex by remember(sources) {
        val preferred1080 = sources.indexOfFirst { it.resolution.contains("1080", ignoreCase = true) }
        val preferred720 = sources.indexOfFirst { it.resolution.contains("720", ignoreCase = true) }
        val preferred480 = sources.indexOfFirst { it.resolution.contains("480", ignoreCase = true) }
        val defaultIdx = when {
            preferred720 >= 0 -> preferred720
            preferred1080 >= 0 -> preferred1080
            preferred480 >= 0 -> preferred480
            sources.isNotEmpty() -> 0
            else -> 0
        }
        mutableIntStateOf(defaultIdx.coerceIn(0, (sources.size - 1).coerceAtLeast(0)))
    }

    val currentSource = sources.getOrNull(selectedSourceIndex)

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(exoPlayer.currentPosition) }
    var duration by remember { mutableLongStateOf(exoPlayer.duration.coerceAtLeast(0L)) }
    var isBuffering by remember { mutableStateOf(exoPlayer.playbackState == Player.STATE_BUFFERING) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var autoRetryCount by remember { mutableIntStateOf(0) }
    var reconnectingMessage by remember { mutableStateOf<String?>(null) }
    var lastValidPlaybackPosition by remember { mutableLongStateOf(initialPositionMs) }

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var currentResizeMode by remember { mutableStateOf(ResizeMode.FIT) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Gesture HUD feedback
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) }
    var volumeLevel by remember { mutableFloatStateOf(-1f) }
    var brightnessLevel by remember { mutableFloatStateOf(-1f) }

    // Subtitles
    var selectedCaptionIndex by remember { mutableIntStateOf(-1) }
    var parsedSubtitles by remember { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var currentSubtitleText by remember { mutableStateOf("") }
    var captionTextOpacity by remember { mutableFloatStateOf(CaptionPreferences.getTextOpacity(context)) }
    var captionBgOpacity by remember { mutableFloatStateOf(CaptionPreferences.getBackgroundOpacity(context)) }
    var subtitleDialogTab by remember { mutableIntStateOf(0) }
    var streamReloadTrigger by remember { mutableIntStateOf(0) }

    // Modals
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Immersive mode in landscape
    fun setImmersive(enabled: Boolean) {
        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (enabled) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(isLandscape) {
        setImmersive(isLandscape)
    }

    // Toggle orientation: Uses SCREEN_ORIENTATION_LANDSCAPE to lock orientation reliably
    fun toggleOrientation() {
        activity?.let { act ->
            act.requestedOrientation = if (isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }

    // BackHandler: return to portrait if in landscape
    BackHandler(enabled = isLandscape) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    // Seamless media loading: sets media item on source change, retry trigger, or error recovery
    LaunchedEffect(currentSource, streamReloadTrigger) {
        if (currentSource != null && currentSource.url.isNotBlank()) {
            val url = currentSource.url
            val currentMediaId = exoPlayer.currentMediaItem?.mediaId
            val isPlayerIdleOrError = exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playerError != null
            if (currentMediaId != url || isPlayerIdleOrError || streamReloadTrigger > 0) {
                val resumePos = exoPlayer.currentPosition.takeIf { it > 0 } ?: lastValidPlaybackPosition.takeIf { it > 0 } ?: initialPositionMs
                hasError = false
                errorMessage = null
                isBuffering = true
                try {
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(url)
                        .setUri(url)
                        .build()
                    exoPlayer.setMediaItem(mediaItem, resumePos.coerceAtLeast(0L))
                    exoPlayer.prepare()
                    exoPlayer.play()
                } catch (e: Exception) {
                    hasError = true
                    errorMessage = e.localizedMessage ?: "Failed to load stream"
                }
            } else {
                if (!exoPlayer.isPlaying && exoPlayer.playbackState == Player.STATE_READY) {
                    exoPlayer.play()
                }
            }
        } else if (sources.isEmpty()) {
            hasError = true
            errorMessage = "No stream links found for this title."
        }
    }

    // Auto-select English or default available subtitle track when captions arrive
    LaunchedEffect(captions) {
        if (selectedCaptionIndex == -1 && captions.isNotEmpty()) {
            val defaultIndex = captions.indexOfFirst {
                it.lan.equals("en", ignoreCase = true) ||
                it.lanName?.contains("English", ignoreCase = true) == true
            }.takeIf { it >= 0 } ?: 0
            selectedCaptionIndex = defaultIndex
        }
    }

    // Subtitle parsing
    LaunchedEffect(selectedCaptionIndex) {
        if (selectedCaptionIndex >= 0 && selectedCaptionIndex < captions.size) {
            val caption = captions[selectedCaptionIndex]
            parsedSubtitles = SubtitleManager.loadAndParseSrt(caption.url)
        } else {
            parsedSubtitles = emptyList()
            currentSubtitleText = ""
        }
    }

    // Subtitle sync & position tick (250ms crisp interval)
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(250)
            if (exoPlayer.playbackState != Player.STATE_IDLE) {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                currentPosition = pos
                duration = dur
                if (pos > 1000L) {
                    lastValidPlaybackPosition = pos
                }

                if (parsedSubtitles.isNotEmpty()) {
                    val matchingCue = parsedSubtitles.firstOrNull { pos in it.startTimeMs..it.endTimeMs }
                    currentSubtitleText = matchingCue?.text ?: ""
                } else {
                    currentSubtitleText = ""
                }
            }
        }
    }

    // Keep preferred resolution synced with StreamTokenManager
    LaunchedEffect(currentSource) {
        currentSource?.resolution?.let { res ->
            StreamTokenManager.setPreferredResolution(res)
        }
    }

    // Proactive background keep-alive: keeps signed CDN tokens warm before they expire
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(35_000L)
                if (isPlaying) {
                    withContext(Dispatchers.IO) {
                        StreamTokenManager.keepAlive()
                    }
                }
            }
        }
    }

    // Progress persistence every 5 seconds
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(5000)
            if (exoPlayer.isPlaying && exoPlayer.duration > 0) {
                onProgressUpdate(exoPlayer.currentPosition, exoPlayer.duration)
            }
        }
    }

    // Auto-hide controls
    LaunchedEffect(controlsVisible, isPlaying, isLocked) {
        if (controlsVisible && isPlaying && !isLocked) {
            delay(3500)
            controlsVisible = false
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                    hasError = false
                    autoRetryCount = 0
                    reconnectingMessage = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val resumePos = exoPlayer.currentPosition.takeIf { it > 0 } ?: lastValidPlaybackPosition
                val isContainerError = error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                        error.errorCodeName.contains("CONTAINER", ignoreCase = true) ||
                        error.message?.contains("malformed", ignoreCase = true) == true
                val isNetworkOrHttp = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                        error.errorCode in 2000..2999 ||
                        error.errorCodeName.contains("IO", ignoreCase = true)

                // Seamless automatic recovery: if stream dropped, link expired, or container parse failed mid-stream, auto-reconnect
                if ((isContainerError || isNetworkOrHttp) && autoRetryCount < 4) {
                    autoRetryCount++
                    isBuffering = true
                    hasError = false

                    scope.launch {
                        if (isNetworkOrHttp) {
                            // Link expiration (ERROR_CODE_IO_UNSPECIFIED) or network drop: Fetch fresh signed token from API before reloading player
                            reconnectingMessage = "Stream link refreshed, resuming..."
                            val safePos = resumePos.coerceAtLeast(0L)
                            val freshSources = withContext(Dispatchers.IO) {
                                StreamTokenManager.forceRefreshSources()
                            }
                            val targetSource = freshSources.getOrNull(selectedSourceIndex)
                                ?: freshSources.firstOrNull()
                                ?: currentSource

                            if (targetSource != null && targetSource.url.isNotBlank()) {
                                reconnectingMessage = "Resuming playback..."
                                val mediaItem = MediaItem.Builder()
                                    .setMediaId(targetSource.url)
                                    .setUri(targetSource.url)
                                    .build()
                                exoPlayer.setMediaItem(mediaItem, safePos)
                                exoPlayer.prepare()
                                exoPlayer.play()
                            }
                            onRefreshStream(safePos)
                        } else {
                            // Container parse hiccup: re-prepare local stream
                            reconnectingMessage = "Reconnecting stream ($autoRetryCount/4)..."
                            delay(300)
                            try {
                                val url = currentSource?.url
                                val safePos = resumePos.coerceAtLeast(0L)
                                if (!url.isNullOrBlank()) {
                                    val mediaItem = MediaItem.Builder().setMediaId(url).setUri(url).build()
                                    exoPlayer.setMediaItem(mediaItem, safePos)
                                }
                                exoPlayer.prepare()
                                exoPlayer.play()
                            } catch (_: Exception) {
                                onRefreshStream(resumePos)
                            }
                        }
                    }
                } else if (selectedSourceIndex + 1 < sources.size) {
                    selectedSourceIndex++
                } else {
                    isBuffering = false
                    hasError = true
                    errorMessage = if (isContainerError) {
                        "Stream connection interrupted by server. Tap Refresh Stream to reconnect."
                    } else if (isNetworkOrHttp) {
                        "Stream link expired or disconnected. Tap Refresh Stream to reconnect."
                    } else {
                        "Stream playback error (${error.errorCodeName}). Tap Refresh Stream to reload."
                    }
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            if (exoPlayer.duration > 0 && exoPlayer.currentPosition > 0) {
                onProgressUpdate(exoPlayer.currentPosition, exoPlayer.duration)
            }
            exoPlayer.removeListener(listener)
            // DO NOT release exoPlayer here so it survives rotation!
            setImmersive(false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_container")
            .pointerInput(isLocked, isInPip) {
                if (isInPip) return@pointerInput
                detectTapGestures(
                    onTap = {
                        if (!isLocked) {
                            controlsVisible = !controlsVisible
                        }
                    },
                    onDoubleTap = { offset ->
                        if (!isLocked) {
                            val width = size.width
                            if (offset.x < width / 2) {
                                val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                                doubleTapFeedback = "-10s"
                            } else {
                                val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(duration)
                                exoPlayer.seekTo(target)
                                doubleTapFeedback = "+10s"
                            }
                            scope.launch {
                                delay(600)
                                doubleTapFeedback = null
                            }
                        }
                    }
                )
            }
            .pointerInput(isLocked, isLandscape, isInPip) {
                if (!isInPip && isLandscape && !isLocked) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            val width = size.width
                            val isLeft = change.position.x < width / 2
                            if (isLeft) {
                                activity?.window?.let { window ->
                                    val lp = window.attributes
                                    val currentB = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                    val newB = (currentB - (dragAmount / 800f)).coerceIn(0.05f, 1.0f)
                                    lp.screenBrightness = newB
                                    window.attributes = lp
                                    brightnessLevel = newB
                                }
                            } else {
                                audioManager?.let { am ->
                                    val maxV = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val curV = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val delta = (-dragAmount / 30f).roundToInt()
                                    val newV = (curV + delta).coerceIn(0, maxV)
                                    am.setStreamVolume(AudioManager.STREAM_MUSIC, newV, 0)
                                    volumeLevel = newV.toFloat() / maxV.toFloat()
                                }
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                delay(1000)
                                volumeLevel = -1f
                                brightnessLevel = -1f
                            }
                        }
                    )
                }
            }
    ) {
        // Video View - Attached to the persistent exoPlayer
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    resizeMode = currentResizeMode.mode
                }
            },
            update = { playerView ->
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
                playerView.resizeMode = currentResizeMode.mode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Subtitles Overlay
        if (currentSubtitleText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (controlsVisible) 64.dp else 20.dp)
                    .padding(horizontal = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = captionBgOpacity)
                ) {
                    Text(
                        text = currentSubtitleText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White.copy(alpha = captionTextOpacity),
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isLandscape) 16.sp else 13.sp,
                            shadow = if (captionBgOpacity < 0.40f) {
                                Shadow(
                                    color = Color.Black.copy(alpha = 0.95f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            } else null
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(
                            horizontal = if (captionBgOpacity > 0.05f) 12.dp else 4.dp,
                            vertical = if (captionBgOpacity > 0.05f) 6.dp else 2.dp
                        )
                    )
                }
            }
        }

        // Double-tap visual feedback
        doubleTapFeedback?.let { feedback ->
            Box(
                modifier = Modifier
                    .align(if (feedback.startsWith("-")) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 40.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = feedback,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Brightness HUD
        if (brightnessLevel >= 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 28.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier.padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = CinemaAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${(brightnessLevel * 100).toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Volume HUD
        if (volumeLevel >= 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 28.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier.padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = CinemaAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${(volumeLevel * 100).toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Buffering Spinner / Reconnecting Overlay
        if (isBuffering && !hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = CinemaRed,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    if (!reconnectingMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CinemaDark.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CinemaAmber.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = reconnectingMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = CinemaAmber,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Error Banner with one-tap Resume from exact interrupted position
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = CinemaAmber,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Playback Interrupted",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "Unable to stream. Stream connection was interrupted.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                hasError = false
                                isBuffering = true
                                autoRetryCount = 0
                                reconnectingMessage = "Fetching fresh stream token..."
                                val safePos = lastValidPlaybackPosition.coerceAtLeast(0L)
                                scope.launch {
                                    val freshSources = withContext(Dispatchers.IO) {
                                        StreamTokenManager.forceRefreshSources()
                                    }
                                    val targetSource = freshSources.getOrNull(selectedSourceIndex)
                                        ?: freshSources.firstOrNull()
                                        ?: currentSource
                                    if (targetSource != null && targetSource.url.isNotBlank()) {
                                        reconnectingMessage = "Resuming playback..."
                                        val mediaItem = MediaItem.Builder()
                                            .setMediaId(targetSource.url)
                                            .setUri(targetSource.url)
                                            .build()
                                        exoPlayer.setMediaItem(mediaItem, safePos)
                                        exoPlayer.prepare()
                                        exoPlayer.play()
                                    }
                                    onRefreshStream(safePos)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh Stream", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                hasError = false
                                isBuffering = true
                                autoRetryCount = 0
                                scope.launch {
                                    try {
                                        val url = currentSource?.url
                                        val safePos = lastValidPlaybackPosition.coerceAtLeast(0L)
                                        if (!url.isNullOrBlank()) {
                                            val mediaItem = MediaItem.Builder().setMediaId(url).setUri(url).build()
                                            exoPlayer.setMediaItem(mediaItem, safePos)
                                        } else if (safePos > 0L) {
                                            exoPlayer.seekTo(safePos)
                                        }
                                        exoPlayer.prepare()
                                        exoPlayer.play()
                                    } catch (_: Exception) {
                                        onRefreshStream(lastValidPlaybackPosition)
                                        streamReloadTrigger++
                                    }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            val resumeTime = formatTime(lastValidPlaybackPosition)
                            Text(
                                if (lastValidPlaybackPosition > 5000L) "Resume ($resumeTime)" else "Retry",
                                fontSize = 12.sp
                            )
                        }

                        if (sources.size > 1) {
                            OutlinedButton(
                                onClick = {
                                    selectedSourceIndex = (selectedSourceIndex + 1) % sources.size
                                    hasError = false
                                    isBuffering = true
                                    autoRetryCount = 0
                                    streamReloadTrigger++
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CinemaAmber),
                                modifier = Modifier.height(38.dp)
                            ) {
                                val nextSrc = sources.getOrNull((selectedSourceIndex + 1) % sources.size)
                                Text("Try ${nextSrc?.resolution ?: "Quality"}", fontSize = 12.sp)
                            }
                        }

                        if (isLandscape) {
                            OutlinedButton(
                                onClick = { toggleOrientation() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("Exit Fullscreen", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Lock Toggle Button (Landscape only or when locked, never in PiP)
        AnimatedVisibility(
            visible = !isInPip && ((controlsVisible && isLandscape) || isLocked),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
        ) {
            IconButton(
                onClick = { isLocked = !isLocked },
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isLocked) "Unlock" else "Lock Controls",
                    tint = if (isLocked) CinemaAmber else TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // CONTROLS OVERLAY (never in PiP)
        AnimatedVisibility(
            visible = !isInPip && controlsVisible && !isLocked,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // TOP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .displayCutoutPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isLandscape) {
                                toggleOrientation()
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitleInfo.isNullOrBlank()) {
                            Text(
                                text = subtitleInfo,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = LogoRed,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isLandscape) {
                        // Landscape controls in top bar
                        IconButton(
                            onClick = {
                                currentResizeMode = when (currentResizeMode) {
                                    ResizeMode.FIT -> ResizeMode.ZOOM
                                    ResizeMode.ZOOM -> ResizeMode.FILL
                                    ResizeMode.FILL -> ResizeMode.FIT
                                }
                            }
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = TextPrimary)
                        }

                        TextButton(onClick = { showSpeedDialog = true }) {
                            Text("${playbackSpeed}x", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        IconButton(onClick = { showSubtitleDialog = true }) {
                            Icon(Icons.Default.ClosedCaption, contentDescription = "Subtitles", tint = if (selectedCaptionIndex >= 0) LogoRed else TextPrimary)
                        }

                        // Floating Screen (PiP) button
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            IconButton(
                                onClick = {
                                    try {
                                        val pipParams = android.app.PictureInPictureParams.Builder()
                                            .setAspectRatio(android.util.Rational(16, 9))
                                            .build()
                                        activity?.enterPictureInPictureMode(pipParams)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.size(38.dp).testTag("player_pip_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Floating Screen (PiP)",
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (sources.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showQualityDialog = true },
                                color = CinemaRed
                            ) {
                                Text(
                                    text = currentSource?.resolution ?: "AUTO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Floating Screen (PiP) button
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                IconButton(
                                    onClick = {
                                        try {
                                            val pipParams = android.app.PictureInPictureParams.Builder()
                                                .setAspectRatio(android.util.Rational(16, 9))
                                                .build()
                                            activity?.enterPictureInPictureMode(pipParams)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.size(38.dp).testTag("player_pip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureInPictureAlt,
                                        contentDescription = "Floating Screen (PiP)",
                                        tint = PureWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Portrait settings button
                            IconButton(
                                onClick = { showSettingsSheet = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Player Settings",
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // CENTER CONTROLS (BALANCED & PERFECTLY CENTERED)
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isLandscape && isSeries) {
                        IconButton(
                            onClick = onPreviousEpisode,
                            enabled = hasPreviousEpisode,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Episode",
                                tint = if (hasPreviousEpisode) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                    }

                    // Rewind 10s
                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(target)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(LiquidGlassSurface, CircleShape)
                            .border(1.dp, LiquidGlassBorderBrush, CircleShape)
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    Spacer(modifier = Modifier.width(28.dp))

                    // Play / Pause Circle with Liquid Glass Border & Brand Gradient
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(BrandButtonGradient))
                            .border(1.dp, LiquidGlassBorderBrush, CircleShape)
                            .clickable {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                            .testTag("player_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = PureWhite,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(28.dp))

                    // Forward 10s
                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(duration)
                            exoPlayer.seekTo(target)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(LiquidGlassSurface, CircleShape)
                            .border(1.dp, LiquidGlassBorderBrush, CircleShape)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Fast Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    if (isLandscape && isSeries) {
                        Spacer(modifier = Modifier.width(24.dp))
                        IconButton(
                            onClick = onNextEpisode,
                            enabled = hasNextEpisode,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Episode",
                                tint = if (hasNextEpisode) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // BOTTOM CONTROLS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                        onValueChange = { frac ->
                            currentPosition = (frac * duration).toLong()
                        },
                        onValueChangeFinished = {
                            exoPlayer.seekTo(currentPosition)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = BrandCoral,
                            activeTrackColor = BrandCoral,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .testTag("player_seek_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )

                        // Fullscreen / Rotate button
                        IconButton(
                            onClick = { toggleOrientation() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("player_rotate_button")
                        ) {
                            Icon(
                                imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isLandscape) "Exit Fullscreen" else "Fullscreen Landscape",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // SETTINGS DIALOG (Portrait mode settings)
    if (showSettingsSheet) {
        AlertDialog(
            onDismissRequest = { showSettingsSheet = false },
            title = {
                Text(
                    text = "Playback Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quality Option
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LiquidGlassSurface)
                            .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(12.dp))
                            .clickable {
                                showSettingsSheet = false
                                showQualityDialog = true
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Video Quality", fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(currentSource?.resolution ?: "Auto", color = CinemaAmber, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Subtitles Option
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LiquidGlassSurface)
                            .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(12.dp))
                            .clickable {
                                showSettingsSheet = false
                                showSubtitleDialog = true
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtitles & Style", fontWeight = FontWeight.Medium, color = TextPrimary)
                            val subName = if (selectedCaptionIndex >= 0) captions.getOrNull(selectedCaptionIndex)?.lanName ?: "On" else "Off"
                            Text("$subName • ${(captionBgOpacity * 100).roundToInt()}% bg", color = CinemaAmber, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Speed Option
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LiquidGlassSurface)
                            .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(12.dp))
                            .clickable {
                                showSettingsSheet = false
                                showSpeedDialog = true
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Playback Speed", fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("${playbackSpeed}x", color = CinemaAmber, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Aspect Ratio Option
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LiquidGlassSurface)
                            .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(12.dp))
                            .clickable {
                                currentResizeMode = when (currentResizeMode) {
                                    ResizeMode.FIT -> ResizeMode.ZOOM
                                    ResizeMode.ZOOM -> ResizeMode.FILL
                                    ResizeMode.FILL -> ResizeMode.FIT
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Aspect Ratio", fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(currentResizeMode.label, color = CinemaAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsSheet = false }) {
                    Text("Close", color = CinemaRed)
                }
            },
            containerColor = CinemaDark,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // QUALITY DIALOG
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text(text = "Video Resolution", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sources.forEachIndexed { index, source ->
                        val isSelected = index == selectedSourceIndex
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) CinemaRed.copy(alpha = 0.22f) else LiquidGlassSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) LiquidGlassActiveBorderBrush else LiquidGlassBorderBrush,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedSourceIndex = index
                                    showQualityDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isSelected) CinemaRed else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = source.resolution,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = TextPrimary
                                    )
                                }
                                source.formattedSize?.let { sz ->
                                    Text(text = sz, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Cancel", color = CinemaRed)
                }
            },
            containerColor = CinemaDark,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // SUBTITLES DIALOG
    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = {
                Column {
                    Text(text = "Subtitles & Captions", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (subtitleDialogTab == 0) CinemaRed else CinemaSurface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { subtitleDialogTab = 0 }
                        ) {
                            Text(
                                text = "Tracks",
                                color = if (subtitleDialogTab == 0) Color.White else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (subtitleDialogTab == 1) CinemaRed else CinemaSurface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { subtitleDialogTab = 1 }
                        ) {
                            Text(
                                text = "Style & Transparency",
                                color = if (subtitleDialogTab == 1) Color.White else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            },
            text = {
                if (subtitleDialogTab == 0) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Turn Off option
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedCaptionIndex == -1) CinemaRed.copy(alpha = 0.2f) else CinemaSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCaptionIndex = -1
                                    showSubtitleDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (selectedCaptionIndex == -1) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (selectedCaptionIndex == -1) CinemaRed else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Off", color = TextPrimary)
                            }
                        }

                        // Available captions (with prominent Hindi highlight)
                        captions.forEachIndexed { index, caption ->
                            val isHindi = caption.lan == "hi" ||
                                    caption.lanName?.contains("हिन्दी") == true ||
                                    caption.lanName?.contains("Hindi", ignoreCase = true) == true

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (index == selectedCaptionIndex) CinemaRed.copy(alpha = 0.2f) else if (isHindi) CinemaAmber.copy(alpha = 0.08f) else CinemaSurface,
                                border = if (isHindi) androidx.compose.foundation.BorderStroke(1.dp, CinemaAmber.copy(alpha = 0.4f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCaptionIndex = index
                                        showSubtitleDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (index == selectedCaptionIndex) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (index == selectedCaptionIndex) CinemaRed else if (isHindi) CinemaAmber else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isHindi) "🇮🇳 हिन्दी (Hindi)" else (caption.lanName ?: caption.lan ?: "Subtitle"),
                                        color = if (isHindi) CinemaAmber else TextPrimary,
                                        fontWeight = if (isHindi || index == selectedCaptionIndex) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isHindi) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = CinemaAmber.copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                text = "Hindi Sub",
                                                color = CinemaAmber,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (captions.isEmpty()) {
                            Text(
                                text = "No subtitle tracks available for this title.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                } else {
                    // Style & Transparency Controls
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Live Preview Box
                        Text(
                            text = "Live Preview",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF141419),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E38)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = captionBgOpacity)
                                ) {
                                    Text(
                                        text = "Sample Subtitle / ক্যাপশন প্রিভিউ",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.White.copy(alpha = captionTextOpacity),
                                            fontWeight = FontWeight.Bold,
                                            shadow = if (captionBgOpacity < 0.40f) {
                                                Shadow(
                                                    color = Color.Black.copy(alpha = 0.95f),
                                                    offset = Offset(2f, 2f),
                                                    blurRadius = 4f
                                                )
                                            } else null
                                        ),
                                        modifier = Modifier.padding(
                                            horizontal = if (captionBgOpacity > 0.05f) 10.dp else 4.dp,
                                            vertical = if (captionBgOpacity > 0.05f) 5.dp else 2.dp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Background Transparency Controls
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Background Opacity",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                                Text(
                                    text = "${(captionBgOpacity * 100).roundToInt()}% (${if (captionBgOpacity == 0f) "Transparent" else if (captionBgOpacity >= 0.95f) "Solid" else "${(100 - (captionBgOpacity * 100)).roundToInt()}% transparent"})",
                                    style = MaterialTheme.typography.bodySmall.copy(color = CinemaAmber, fontWeight = FontWeight.Bold)
                                )
                            }
                            Slider(
                                value = captionBgOpacity,
                                onValueChange = { newVal ->
                                    captionBgOpacity = newVal
                                    CaptionPreferences.setBackgroundOpacity(context, newVal)
                                },
                                valueRange = 0.0f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CinemaRed,
                                    activeTrackColor = CinemaRed,
                                    inactiveTrackColor = CinemaSurface
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    0.0f to "0% None",
                                    0.25f to "25%",
                                    0.50f to "50%",
                                    0.75f to "75%",
                                    1.0f to "Solid"
                                ).forEach { (preset, label) ->
                                    val isSelected = kotlin.math.abs(captionBgOpacity - preset) < 0.06f
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) CinemaRed.copy(alpha = 0.25f) else CinemaSurface,
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CinemaRed) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                captionBgOpacity = preset
                                                CaptionPreferences.setBackgroundOpacity(context, preset)
                                            }
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) CinemaRed else TextPrimary,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Text Transparency Controls
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Text Opacity",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                                Text(
                                    text = "${(captionTextOpacity * 100).roundToInt()}% (${(100 - (captionTextOpacity * 100)).roundToInt()}% transparent)",
                                    style = MaterialTheme.typography.bodySmall.copy(color = CinemaAmber, fontWeight = FontWeight.Bold)
                                )
                            }
                            Slider(
                                value = captionTextOpacity,
                                onValueChange = { newVal ->
                                    captionTextOpacity = newVal
                                    CaptionPreferences.setTextOpacity(context, newVal)
                                },
                                valueRange = 0.2f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CinemaRed,
                                    activeTrackColor = CinemaRed,
                                    inactiveTrackColor = CinemaSurface
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    0.35f to "35%",
                                    0.50f to "50%",
                                    0.70f to "70%",
                                    0.85f to "85%",
                                    1.0f to "100%"
                                ).forEach { (preset, label) ->
                                    val isSelected = kotlin.math.abs(captionTextOpacity - preset) < 0.06f
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) CinemaRed.copy(alpha = 0.25f) else CinemaSurface,
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CinemaRed) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                captionTextOpacity = preset
                                                CaptionPreferences.setTextOpacity(context, preset)
                                            }
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) CinemaRed else TextPrimary,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleDialog = false }) {
                    Text("Done", color = CinemaRed)
                }
            },
            containerColor = CinemaDark
        )
    }

    // SPEED DIALOG
    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text(text = "Playback Speed", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    speeds.forEach { speed ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (playbackSpeed == speed) CinemaRed.copy(alpha = 0.2f) else CinemaSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playbackSpeed = speed
                                    exoPlayer.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (playbackSpeed == speed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (playbackSpeed == speed) CinemaRed else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("${speed}x", color = TextPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Cancel", color = CinemaRed)
                }
            },
            containerColor = CinemaDark
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}
