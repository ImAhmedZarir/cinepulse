package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.MovieItem
import com.example.ui.HomeUiState
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    watchHistory: List<WatchHistoryEntity>,
    onMovieClick: (MovieItem) -> Unit,
    onWatchHeroClick: (MovieItem) -> Unit,
    onResumeHistoryClick: (WatchHistoryEntity) -> Unit,
    onDeleteHistoryClick: (String) -> Unit,
    onRetry: () -> Unit,
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Loading state
    if (uiState.isLoading && uiState.sections.isEmpty() && uiState.bannerItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(JetBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = BrandCoral,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Loading CinePulse...",
                    color = TextSecondary,
                    fontSize = 13.5.sp
                )
            }
        }
        return
    }

    // Error state
    if (uiState.errorMessage != null && uiState.sections.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(JetBlack).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(cornerRadius = 24.dp) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Connection Error",
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = uiState.errorMessage,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    LiquidGlassButton(
                        text = "Retry",
                        onClick = onRetry,
                        leadingIcon = Icons.Default.Refresh,
                        modifier = Modifier.width(140.dp)
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(JetBlack)
            .testTag("home_feed_list"),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // ── Top Header (Tightened: No wide black void) ────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Official CinePulse Logo asset matching cinepulse-icon.jpg
                CinePulseLogo(iconSize = 28.dp)

                // Glass search pill
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(LiquidGlassSurface)
                        .border(0.8.dp, LiquidGlassBorderBrush, RoundedCornerShape(999.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onSearchClick() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = BrandOrange,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Search movies & shows...",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ── Full-Bleed Apple TV Hero Banner ─────────────────────────────────
        if (uiState.bannerItems.isNotEmpty()) {
            item {
                HeroBanner(
                    items = uiState.bannerItems,
                    onItemClick = onMovieClick,
                    onWatchClick = onWatchHeroClick
                )
            }
        }

        // ── Continue Watching ────────────────────────────────────────────────
        if (watchHistory.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = null,
                            tint = BrandCoral,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Continue Watching",
                            color = PureWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(watchHistory, key = { it.subjectId }) { historyItem ->
                            ContinueWatchingCard(
                                history = historyItem,
                                onPlayClick = { onResumeHistoryClick(historyItem) },
                                onDeleteClick = { onDeleteHistoryClick(historyItem.subjectId) }
                            )
                        }
                    }
                }
            }
        }

        // ── Content Sections ─────────────────────────────────────────────────
        items(uiState.sections, key = { it.section }) { section ->
            Column(modifier = Modifier.padding(top = 18.dp)) {
                // Section heading with brand gradient accent
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.verticalGradient(listOf(BrandOrange, BrandCoral))
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = section.section,
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(10.dp))

                val uniqueItems = remember(section.items) {
                    section.items.distinctBy { "${it.safeSlug}_${it.subjectId}" }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uniqueItems, key = { "${it.safeSlug}_${it.subjectId}" }) { movie ->
                        MoviePosterCard(
                            movie = movie,
                            onClick = { onMovieClick(movie) }
                        )
                    }
                }
            }
        }
    }
}
