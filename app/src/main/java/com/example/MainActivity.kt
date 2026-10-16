package com.example

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.components.DevCreditDialog
import com.example.ui.screens.catalog.CatalogScreen
import com.example.ui.screens.detail.DetailScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.theme.*
import com.example.util.DevCreditManager

class MainActivity : ComponentActivity() {
    var isInPipMode by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp(isInPipMode = isInPipMode)
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        if (!isInPictureInPictureMode) {
            // When leaving PiP, if the user closed/dismissed the popup window, immediately halt playback
            if (isFinishing || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
                viewModel.closePlayer()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop playback when activity is stopped and not actively displaying PiP
        if (!isInPipMode || isFinishing) {
            val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
            viewModel.closePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.closePlayer()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        if (viewModel.detailPlayerState.value.isPlaying) {
            enterPipMode()
        }
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun MainApp(
    viewModel: MainViewModel = viewModel(),
    isInPipMode: Boolean = false
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val detailState by viewModel.detailPlayerState.collectAsStateWithLifecycle()

    val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()

    val isDetailOpen = detailState.slug != null
    val isFullscreenPlaying = isLandscape && detailState.isPlaying

    val exoPlayer = remember(context) { viewModel.getPlayer(context) }

    // Developer credit dialog on app start with 7-day snooze capability (hidden in PiP mode)
    var showDevCreditDialog by remember {
        mutableStateOf(!isInPipMode && DevCreditManager.shouldShowDialog(context))
    }

    if (showDevCreditDialog && !isInPipMode) {
        DevCreditDialog(
            onDismiss = { showDevCreditDialog = false },
            onSnooze7Days = {
                DevCreditManager.snoozeFor7Days(context)
                showDevCreditDialog = false
            }
        )
    }

    // Handle back button: In landscape, exit fullscreen back to portrait first
    BackHandler(enabled = !isInPipMode && (isLandscape || isDetailOpen || currentTab != AppTab.HOME)) {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else if (isDetailOpen) {
            viewModel.closeDetail()
        } else {
            viewModel.selectTab(AppTab.HOME)
        }
    }

    Scaffold(
        containerColor = JetBlack,
        bottomBar = {
            if (!isInPipMode && !isDetailOpen && !isFullscreenPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Authentic iOS Frosted Glass Floating Dock
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = LiquidGlassNavBg,
                        border = BorderStroke(1.dp, LiquidGlassBorderBrush),
                        shadowElevation = 24.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .testTag("bottom_nav_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LiquidGlassNavItem(
                                selected = currentTab == AppTab.HOME,
                                onClick = { viewModel.selectTab(AppTab.HOME) },
                                icon = if (currentTab == AppTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                label = "Home",
                                testTag = "nav_home",
                                modifier = Modifier.weight(1f)
                            )

                            LiquidGlassNavItem(
                                selected = currentTab == AppTab.CATALOG,
                                onClick = { viewModel.selectTab(AppTab.CATALOG) },
                                icon = if (currentTab == AppTab.CATALOG) Icons.Filled.Explore else Icons.Outlined.Explore,
                                label = "Browse",
                                testTag = "nav_browse",
                                modifier = Modifier.weight(1f)
                            )

                            LiquidGlassNavItem(
                                selected = currentTab == AppTab.SEARCH,
                                onClick = { viewModel.selectTab(AppTab.SEARCH) },
                                icon = Icons.Filled.Search,
                                label = "Search",
                                testTag = "nav_search",
                                modifier = Modifier.weight(1f)
                            )

                            LiquidGlassNavItem(
                                selected = currentTab == AppTab.LIBRARY,
                                onClick = { viewModel.selectTab(AppTab.LIBRARY) },
                                icon = if (currentTab == AppTab.LIBRARY) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                label = "Library",
                                testTag = "nav_library",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        // Edge-to-edge content: screens manage their own top status bar clearance to avoid top black voids
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(JetBlack)
        ) {
            if (isDetailOpen) {
                DetailScreen(
                    uiState = detailState,
                    exoPlayer = exoPlayer,
                    onBackClick = {
                        if (isLandscape) {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                        viewModel.closeDetail()
                    },
                    onPlayClick = { se, ep ->
                        val s = detailState.slug
                        val id = detailState.subjectId
                        if (s != null && id != null) {
                            viewModel.playItem(s, id, se, ep)
                        }
                    },
                    onSelectEpisode = { se, ep ->
                        viewModel.selectEpisode(se, ep)
                    },
                    onToggleWatchlist = { viewModel.toggleWatchlist() },
                    onProgressUpdate = { pos, dur ->
                        viewModel.saveProgress(pos, dur)
                    },
                    onFetchStreamForDownload = { se, ep ->
                        val slug = detailState.slug
                        val id = detailState.subjectId
                        if (slug != null && id != null) {
                            viewModel.getStreamForDownload(id, slug, se, ep)
                        } else {
                            emptyList()
                        }
                    }
                )
            } else {
                when (currentTab) {
                    AppTab.HOME -> {
                        HomeScreen(
                            uiState = homeState,
                            watchHistory = watchHistory,
                            onMovieClick = { movie ->
                                viewModel.openDetail(movie.safeSlug, movie.subjectId, movie.safeName, movie.type)
                            },
                            onWatchHeroClick = { movie ->
                                val isSeries = movie.isTvSeries
                                viewModel.openDetail(movie.safeSlug, movie.subjectId, movie.safeName, movie.type)
                                viewModel.playItem(
                                    slug = movie.safeSlug,
                                    subjectId = movie.subjectId,
                                    season = if (isSeries) 1 else 0,
                                    episode = if (isSeries) 1 else 0
                                )
                            },
                            onResumeHistoryClick = { history ->
                                viewModel.openDetail(history.slug, history.subjectId, history.title, history.type)
                                viewModel.playItem(history.slug, history.subjectId, history.season, history.episode, history.positionMs)
                            },
                            onDeleteHistoryClick = { subId ->
                                viewModel.deleteHistory(subId)
                            },
                            onRetry = { viewModel.loadHomeFeed() },
                            onSearchClick = { viewModel.selectTab(AppTab.SEARCH) }
                        )
                    }

                    AppTab.CATALOG -> {
                        CatalogScreen(
                            uiState = catalogState,
                            onTypeSelected = { type -> viewModel.setCatalogType(type) },
                            onGenreSelected = { genre -> viewModel.setCatalogGenre(genre) },
                            onSortSelected = { sort -> viewModel.setCatalogSort(sort) },
                            onMovieClick = { movie ->
                                viewModel.openDetail(movie.safeSlug, movie.subjectId, movie.safeName, movie.type)
                            },
                            onLoadMore = { viewModel.loadCatalog(reset = false) }
                        )
                    }

                    AppTab.SEARCH -> {
                        SearchScreen(
                            uiState = searchState,
                            onQueryChanged = { q -> viewModel.onSearchQueryChanged(q) },
                            onSearchSubmit = { q -> viewModel.performSearch(q) },
                            onMovieClick = { movie ->
                                viewModel.openDetail(movie.safeSlug, movie.subjectId, movie.safeName, movie.type)
                            }
                        )
                    }

                    AppTab.LIBRARY -> {
                        LibraryScreen(
                            watchlist = watchlist,
                            watchHistory = watchHistory,
                            onMovieClick = { movie ->
                                viewModel.openDetail(movie.safeSlug, movie.subjectId, movie.safeName, movie.type)
                            },
                            onResumeHistoryClick = { history ->
                                viewModel.openDetail(history.slug, history.subjectId, history.title, history.type)
                                viewModel.playItem(history.slug, history.subjectId, history.season, history.episode, history.positionMs)
                            },
                            onDeleteHistoryItem = { subId ->
                                viewModel.deleteHistory(subId)
                            },
                            onClearHistory = { viewModel.clearAllHistory() },
                            onExploreClick = { viewModel.selectTab(AppTab.HOME) },
                            onOpenDevCredit = { showDevCreditDialog = true }
                        )
                    }
                }
            }
        }
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

@Composable
private fun LiquidGlassNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .width(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(
                    if (selected) {
                        Modifier.background(Brush.horizontalGradient(BrandButtonGradient))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) PureWhite else PureWhite.copy(alpha = 0.55f),
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) PureWhite else PureWhite.copy(alpha = 0.55f),
            letterSpacing = (-0.2).sp
        )
    }
}
