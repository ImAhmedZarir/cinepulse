package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MovieItem
import com.example.ui.theme.BrandButtonGradient
import com.example.ui.theme.JetBlack
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.LiquidGlassBorderBrush
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RatingGold
import kotlinx.coroutines.delay

/**
 * Apple TV & Netflix style full-bleed cinematic Hero Banner.
 * Designed with official brand gradient #FF6A3D -> #E63956 and true liquid glass.
 */
@Composable
fun HeroBanner(
    items:        List<MovieItem>,
    onItemClick:  (MovieItem) -> Unit,
    onWatchClick: (MovieItem) -> Unit,
    modifier:     Modifier = Modifier
) {
    if (items.isEmpty()) return

    val pageCount  = items.size.coerceAtMost(8)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // Auto-advance every 5 seconds
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5_000)
            if (pagerState.pageCount > 1) {
                val next = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(440.dp)
            .testTag("hero_banner")
    ) {
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            HeroBannerSlide(
                item         = items[page],
                onItemClick  = { onItemClick(items[page]) },
                onWatchClick = { onWatchClick(items[page]) }
            )
        }

        // ── Pager Dot Indicators ─────────────────────────────────────────────
        if (pagerState.pageCount > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                repeat(pagerState.pageCount) { index ->
                    val isActive = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue   = if (isActive) 22.dp else 6.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness    = Spring.StiffnessMedium
                        ),
                        label = "dot_width_$index"
                    )
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isActive) Brush.horizontalGradient(BrandButtonGradient)
                                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.35f)))
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBannerSlide(
    item:         MovieItem,
    onItemClick:  () -> Unit,
    onWatchClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onItemClick)
    ) {
        // Full-bleed cinematic backdrop
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.safeName,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )

        // Top vignette (for status bar clarity)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x75000000), Color.Transparent)
                    )
                )
        )

        // Cinematic bottom scrim fading seamlessly into true Jet Black (#000000)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.35f to Color.Transparent,
                            0.62f to JetBlack.copy(alpha = 0.70f),
                            0.82f to JetBlack.copy(alpha = 0.95f),
                            1.00f to JetBlack
                        )
                    )
                )
        )

        // ── Spotlight Content & Actions (Bottom Aligned) ───────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 26.dp)
        ) {
            // Spotlight Tag
            LiquidGlassPill(label = "FEATURED SPOTLIGHT")

            Spacer(modifier = Modifier.height(8.dp))

            // Movie/Show Title
            Text(
                text       = item.safeName,
                color      = PureWhite,
                fontSize   = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Row: Rating, Year, Genre
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (!item.rating.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(LiquidGlassBackground)
                            .border(0.8.dp, LiquidGlassBorderBrush, RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Star,
                            contentDescription = null,
                            tint               = RatingGold,
                            modifier           = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text       = item.rating ?: "",
                            color      = PureWhite,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item.year?.takeIf { it.isNotBlank() }?.let { year ->
                    LiquidGlassPill(label = year)
                }

                item.genre?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }?.let { genre ->
                    LiquidGlassPill(label = genre)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Primary Actions: Watch (Brand Gradient) & Details (Liquid Glass) ──
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                LiquidGlassButton(
                    text        = "Watch Now",
                    onClick     = onWatchClick,
                    leadingIcon = Icons.Default.PlayArrow,
                    modifier    = Modifier.weight(1f).testTag("hero_watch_button")
                )

                LiquidGlassSecondaryButton(
                    text        = "Details",
                    onClick     = onItemClick,
                    leadingIcon = Icons.Default.Info,
                    modifier    = Modifier.weight(1f).testTag("hero_details_button")
                )
            }
        }
    }
}
