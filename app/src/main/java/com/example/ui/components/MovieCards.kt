package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.MovieItem
import com.example.ui.theme.BrandButtonGradient
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.JetBlack
import com.example.ui.theme.LiquidGlassBorderBrush
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RatingGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
//  MoviePosterCard — Liquid glass poster card with authentic brand accents
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MoviePosterCard(
    movie: MovieItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 135.dp,
    cardHeight: Dp = 200.dp
) {
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .width(cardWidth)
            .testTag("movie_card_${movie.safeSlug}")
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .clip(shape)
                .background(CinemaSurfaceVariant)
                .border(0.8.dp, LiquidGlassBorderBrush, shape)
                .clickable { onClick() }
        ) {
            // Poster image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(movie.posterUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = movie.safeName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, JetBlack.copy(alpha = 0.90f))
                        )
                    )
            )

            // Rating badge — top right
            if (!movie.rating.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .padding(7.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JetBlack.copy(alpha = 0.85f))
                        .border(0.6.dp, LiquidGlassBorderBrush, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = RatingGold,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = movie.rating ?: "",
                        color = PureWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Type/badge pill — top left (Brand Gradient #FF6A3D -> #E63956)
            val badgeText = movie.badge?.takeIf { it.isNotBlank() } ?: movie.type?.uppercase()
            if (!badgeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .padding(7.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.horizontalGradient(BrandButtonGradient))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = PureWhite,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Title + meta below poster
        Spacer(Modifier.height(7.dp))
        Text(
            text = movie.safeName,
            color = PureWhite,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = movie.year ?: "",
                color = TextSecondary,
                fontSize = 11.sp
            )
            Text(
                text = movie.genre?.split(",")?.firstOrNull()?.trim() ?: "",
                color = BrandOrange,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 70.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ContinueWatchingCard — horizontal progress card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ContinueWatchingCard(
    history: WatchHistoryEntity,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cw_scale"
    )

    Column(
        modifier = modifier
            .width(210.dp)
            .scale(scale)
            .clip(shape)
            .background(LiquidGlassSurface)
            .border(0.8.dp, LiquidGlassBorderBrush, shape)
            .clickable(interactionSource = interactionSource, indication = null) { onPlayClick() }
            .testTag("continue_card_${history.subjectId}")
    ) {
        // Thumbnail section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(history.posterUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = history.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, JetBlack.copy(alpha = 0.80f))
                        )
                    )
            )

            // Brand Gradient Play Button in Center
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(BrandButtonGradient))
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    tint = PureWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Delete button top right
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = PureWhite,
                    modifier = Modifier.size(14.dp)
                )
            }

            // S:E badge
            if (history.season > 0 || history.episode > 0) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .border(0.5.dp, LiquidGlassBorderBrush, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "S${history.season}:E${history.episode}",
                        color = PureWhite,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Progress bar at bottom of thumbnail
            val progress = if (history.durationMs > 0) {
                (history.positionMs.toFloat() / history.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color(0x40FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(Brush.horizontalGradient(BrandButtonGradient))
                )
            }
        }

        // Info section
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = history.title,
                color = PureWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            val remainingMins = ((history.durationMs - history.positionMs) / 60000L).coerceAtLeast(0)
            Text(
                text = if (remainingMins > 0) "${remainingMins}m remaining" else "Watched",
                color = TextSecondary,
                fontSize = 10.5.sp
            )
        }
    }
}
