package com.example.ui.screens.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MovieItem
import com.example.ui.SearchUiState
import com.example.ui.components.MoviePosterCard
import com.example.ui.theme.*

private val POPULAR_SEARCHES = listOf(
    "Breaking Bad", "Avatar", "Oppenheimer", "Batman",
    "John Wick", "Dune", "Peaky Blinders", "Anime", "Action"
)

// ─── Shimmer Placeholder ─────────────────────────────────────────────────────

@Composable
private fun ShimmerCard(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -600f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            LiquidGlassSurface,
            LiquidGlassSurfaceElevated,
            LiquidGlassSurface
        ),
        start = Offset(shimmerX, 0f),
        end = Offset(shimmerX + 400f, 400f)
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(shimmerBrush)
            .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(16.dp))
    )
}

@Composable
private fun ShimmerGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        items(count = 8) {
            ShimmerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(193.dp)
            )
        }
    }
}

// ─── Main Screen ─────────────────────────────────────────────────────────────

@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onMovieClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
    ) {

        // ── Liquid Glass Search Bar ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(LiquidGlassSurface)
                .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(999.dp))
        ) {
            // Top specular sheen stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                LiquidGlassSpecularHighlight,
                                Color.Transparent
                            )
                        )
                    )
            )
            TextField(
                value = uiState.query,
                onValueChange = onQueryChanged,
                placeholder = {
                    Text(
                        text = "Search movies, series, anime…",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = BrandCoral
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotBlank()) {
                        IconButton(onClick = { onQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    onSearchSubmit(uiState.query)
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = BrandCoral
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_field")
            )
        }

        // ── Animated Suggestions Dropdown ────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.suggestions.isNotEmpty(),
            enter = expandVertically(tween(220)) + fadeIn(tween(220)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LiquidGlassSurfaceElevated)
                    .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(16.dp))
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.suggestions.forEach { suggestion ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(LiquidGlassSurface)
                                .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(999.dp))
                                .clickable {
                                    keyboardController?.hide()
                                    onSearchSubmit(suggestion)
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        if (uiState.suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Content Area ─────────────────────────────────────────────────────
        AnimatedContent(
            targetState = when {
                uiState.isSearching               -> "loading"
                uiState.results.isNotEmpty()       -> "results"
                uiState.query.isNotBlank()         -> "empty_query"
                else                               -> "idle"
            },
            transitionSpec = {
                fadeIn(tween(260)) togetherWith fadeOut(tween(180))
            },
            label = "SearchContentArea",
            modifier = Modifier.fillMaxSize()
        ) { state ->
            when (state) {

                // Loading shimmer
                "loading" -> ShimmerGrid()

                // Results grid
                "results" -> {
                    val uniqueResults = remember(uiState.results) {
                        uiState.results.distinctBy { "${it.safeSlug}_${it.subjectId}" }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 8.dp, bottom = 96.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_results_grid")
                    ) {
                        items(
                            count = uniqueResults.size,
                            key = { i -> "${uniqueResults[i].safeSlug}_${uniqueResults[i].subjectId}_$i" }
                        ) { i ->
                            val movie = uniqueResults[i]
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

                // No results for this query
                "empty_query" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Glass card empty state
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
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(LiquidGlassSurfaceElevated)
                                        .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No results for\n\"${uiState.query}\"",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try different keywords or title names.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    }
                }

                // Idle — trending searches
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Trending Searches",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            POPULAR_SEARCHES.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(LiquidGlassSurface)
                                        .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(14.dp))
                                        .clickable {
                                            keyboardController?.hide()
                                            onSearchSubmit(tag)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
