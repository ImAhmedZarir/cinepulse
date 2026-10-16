package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * CinePulse official brand logo.
 * Uses the authentic logo asset from cinepulse_logo_wide matching cinepulse-icon.jpg.
 */
@Composable
fun CinePulseLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
    textSize: TextUnit = 20.sp,
    showTagline: Boolean = false,
    taglineSize: TextUnit = 9.sp
) {
    Image(
        painter = painterResource(id = R.drawable.cinepulse_logo_wide),
        contentDescription = "CinePulse",
        modifier = modifier.height(iconSize),
        contentScale = ContentScale.Fit
    )
}
