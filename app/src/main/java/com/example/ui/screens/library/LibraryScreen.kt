package com.example.ui.screens.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.MovieItem
import com.example.ui.components.*
import com.example.ui.theme.*

enum class LibraryTab {
    WATCHLIST,
    HISTORY
}

// ─── Glass Tab Pill Row ───────────────────────────────────────────────────────

@Composable
private fun GlassTabRow(
    selectedTab: LibraryTab,
    watchlistCount: Int,
    historyCount: Int,
    onTabSelected: (LibraryTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LiquidGlassSurface)
            .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(16.dp))
            .padding(3.dp)
    ) {
        Row {
            LibraryTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                val label = when (tab) {
                    LibraryTab.WATCHLIST -> "Watchlist ($watchlistCount)"
                    LibraryTab.HISTORY   -> "History ($historyCount)"
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(CinemaRedGradient)
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.Transparent)
                                )
                            }
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            brush = if (isSelected) LiquidGlassActiveBorderBrush
                            else Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.Transparent)
                            ),
                            shape = RoundedCornerShape(13.dp)
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PureWhite else TextSecondary,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

// ─── Empty States ─────────────────────────────────────────────────────────────

@Composable
private fun WatchlistEmptyState(onExploreClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(LiquidGlassSurface)
                .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(24.dp))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(LiquidGlassSurfaceElevated)
                        .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Your Watchlist is empty",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Save movies and series you want to watch later.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                LiquidGlassButton(
                    text = "Discover Content",
                    onClick = onExploreClick,
                    leadingIcon = Icons.Default.Explore,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun HistoryEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(LiquidGlassSurface)
                .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(24.dp))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(LiquidGlassSurfaceElevated)
                        .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "No Watch History",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Movies and episodes you watch will appear here to resume anytime.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

// ─── History Row Card ─────────────────────────────────────────────────────────

@Composable
private fun HistoryRowCard(
    historyItem: WatchHistoryEntity,
    onResumeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LiquidGlassSurface)
            .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(16.dp))
            .clickable { onResumeClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with play overlay and progress bar
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(62.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CinemaSurfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(historyItem.posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = historyItem.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Frosted play button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(15.dp))
                        .background(LiquidGlassSurface)
                        .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Progress bar
                val progress = if (historyItem.durationMs > 0) {
                    (historyItem.positionMs.toFloat() / historyItem.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = BrandCoral,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = historyItem.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (historyItem.season > 0 || historyItem.episode > 0) {
                    Text(
                        text = "Season ${historyItem.season} • Episode ${historyItem.episode}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
                val posMin = (historyItem.positionMs / 1000) / 60
                val durMin = (historyItem.durationMs / 1000) / 60
                Text(
                    text = if (durMin > 0) "Watched ${posMin}m of ${durMin}m" else "In Progress",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }

            // Delete icon
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─── Main Screen ─────────────────────────────────────────────────────────────

@Composable
fun LibraryScreen(
    watchlist: List<WatchlistEntity>,
    watchHistory: List<WatchHistoryEntity>,
    onMovieClick: (MovieItem) -> Unit,
    onResumeHistoryClick: (WatchHistoryEntity) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    onExploreClick: () -> Unit,
    onOpenDevCredit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.WATCHLIST) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "My Library",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Dev credit — small glass icon button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LiquidGlassSurface)
                        .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(12.dp))
                        .clickable { onOpenDevCredit() }
                        .testTag("library_dev_credit_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Developer Credit",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clear History — glass danger button (CinemaRed border)
                if (selectedTab == LibraryTab.HISTORY && watchHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LiquidGlassSurface)
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        CinemaRed.copy(alpha = 0.70f),
                                        CinemaRed.copy(alpha = 0.25f)
                                    )
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { showClearHistoryDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            tint = CinemaRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ── Glass Tab Pills ───────────────────────────────────────────────────
        GlassTabRow(
            selectedTab = selectedTab,
            watchlistCount = watchlist.size,
            historyCount = watchHistory.size,
            onTabSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Tab Content with crossfade ────────────────────────────────────────
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(160))
            },
            label = "LibraryTabContent",
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                LibraryTab.WATCHLIST -> {
                    if (watchlist.isEmpty()) {
                        WatchlistEmptyState(onExploreClick = onExploreClick)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 110.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, bottom = 96.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                count = watchlist.size,
                                key = { i -> "${watchlist[i].subjectId}_$i" }
                            ) { i ->
                                val item = watchlist[i]
                                val movie = MovieItem(
                                    name = item.title,
                                    posterUrl = item.posterUrl,
                                    slug = item.slug,
                                    subjectId = item.subjectId,
                                    rating = item.rating,
                                    year = item.year,
                                    genre = item.genre,
                                    type = item.type
                                )
                                MoviePosterCard(
                                    movie = movie,
                                    onClick = { onMovieClick(movie) },
                                    cardWidth = 110.dp,
                                    cardHeight = 165.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                LibraryTab.HISTORY -> {
                    if (watchHistory.isEmpty()) {
                        HistoryEmptyState()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, bottom = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                count = watchHistory.size,
                                key = { i -> "${watchHistory[i].subjectId}_$i" }
                            ) { i ->
                                val historyItem = watchHistory[i]
                                HistoryRowCard(
                                    historyItem = historyItem,
                                    onResumeClick = { onResumeHistoryClick(historyItem) },
                                    onDeleteClick = { onDeleteHistoryItem(historyItem.subjectId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Clear History Confirmation Dialog ─────────────────────────────────────
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = {
                Text(
                    text = "Clear Watch History?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will remove all your saved playback positions and history.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(LiquidGlassSurface)
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    CinemaRed.copy(alpha = 0.80f),
                                    CinemaRed.copy(alpha = 0.30f)
                                )
                            ),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            onClearHistory()
                            showClearHistoryDialog = false
                        }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "Clear All",
                        color = CinemaRed,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CinemaDark,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
