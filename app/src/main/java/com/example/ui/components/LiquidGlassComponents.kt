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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandButtonGradient
import com.example.ui.theme.BrandCoral
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.JetBlack
import com.example.ui.theme.LiquidGlassActiveBorderBrush
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.LiquidGlassBorderBrush
import com.example.ui.theme.LiquidGlassSheenBrush
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LiquidGlassSurfaceGradient
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// Shared spring physics for fluid Apple iOS dynamics
// ─────────────────────────────────────────────────────────────────────────────
val GlassSpringSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness    = Spring.StiffnessMedium
)

// ─────────────────────────────────────────────────────────────────────────────
// LiquidGlassCard (iOS 28px squircle, specular rim, ambient sheen)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassCard(
    modifier:     Modifier  = Modifier,
    cornerRadius: Dp        = 28.dp,
    onClick:      (() -> Unit)? = null,
    content:      @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(LiquidGlassSurfaceGradient, shape)
            .border(
                width = 1.dp,
                brush = LiquidGlassBorderBrush,
                shape = shape
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
    ) {
        content()

        // Specular sheen & ambient glare overlay – sits on top of content
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(LiquidGlassSheenBrush)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LiquidGlassGrip (38x5px rounded sheet indicator from liquid-glass-main)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassGrip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(38.dp)
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.40f))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// LiquidGlassPill (supports label and text parameter names)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassPill(
    label:    String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(999.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(LiquidGlassBackground, shape)
            .border(
                width = 0.8.dp,
                brush = LiquidGlassBorderBrush,
                shape = shape
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text       = label,
            color      = PureWhite,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines   = 1,
            softWrap   = false
        )
    }
}

@Composable
fun LiquidGlassPill(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = PureWhite
) {
    val shape = RoundedCornerShape(999.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(LiquidGlassBackground, shape)
            .border(
                width = 0.8.dp,
                brush = LiquidGlassBorderBrush,
                shape = shape
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text       = text,
            color      = textColor,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines   = 1,
            softWrap   = false
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LiquidGlassButton (Primary — Brand Gradient #FF6A3D -> #E63956 with White Text)
// Always crystal clear text visibility with drop shadow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassButton(
    text:        String,
    onClick:     () -> Unit,
    modifier:    Modifier    = Modifier,
    leadingIcon: ImageVector? = null,
    enabled:     Boolean     = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = GlassSpringSpec,
        label = "LiquidGlassButton_scale"
    )

    val shape = RoundedCornerShape(999.dp)
    val buttonBrush = if (enabled) {
        Brush.horizontalGradient(BrandButtonGradient)
    } else {
        Brush.horizontalGradient(listOf(Color(0x40EE1E50), Color(0x20EE1E50)))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .scale(scale)
            .clip(shape)
            .background(buttonBrush, shape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0x66FFFFFF), Color(0x1AFFFFFF))
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = enabled,
                onClick           = onClick
            )
    ) {
        // Specular top light
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        )

        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector        = leadingIcon,
                    contentDescription = null,
                    tint               = PureWhite,
                    modifier           = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text       = text,
                color      = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize   = 14.5.sp,
                maxLines   = 1,
                softWrap   = false,
                style      = TextStyle(
                    shadow = Shadow(color = Color(0x66000000), blurRadius = 6f)
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LiquidGlassSecondaryButton (Secondary — Frosted Glass with White Text)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassSecondaryButton(
    text:        String,
    onClick:     () -> Unit,
    modifier:    Modifier    = Modifier,
    leadingIcon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = GlassSpringSpec,
        label = "LiquidGlassSecondaryButton_scale"
    )

    val shape = RoundedCornerShape(999.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .scale(scale)
            .clip(shape)
            .background(LiquidGlassSurface, shape)
            .border(
                width = 1.dp,
                brush = LiquidGlassBorderBrush,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
    ) {
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 18.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector        = leadingIcon,
                    contentDescription = null,
                    tint               = PureWhite,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text       = text,
                color      = PureWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp,
                maxLines   = 1,
                softWrap   = false
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GlassChip (Active: Brand Gradient | Inactive: Frosted Glass)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassChip(
    text:     String,
    selected: Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = GlassSpringSpec,
        label = "GlassChip_scale"
    )

    val shape = RoundedCornerShape(999.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(Brush.horizontalGradient(BrandButtonGradient), shape)
                        .border(1.dp, Brush.horizontalGradient(BrandButtonGradient), shape)
                } else {
                    Modifier
                        .background(LiquidGlassBackground, shape)
                        .border(0.8.dp, LiquidGlassBorderBrush, shape)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text       = text,
            color      = PureWhite,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize   = 12.5.sp,
            maxLines   = 1,
            softWrap   = false,
            style      = if (selected) TextStyle(shadow = Shadow(color = Color(0x66000000), blurRadius = 4f)) else TextStyle()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GlassIconButton (Round 40dp glass action button)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassIconButton(
    icon:               ImageVector,
    contentDescription: String?,
    onClick:            () -> Unit,
    modifier:           Modifier = Modifier,
    size:               Dp       = 40.dp,
    tint:               Color    = PureWhite
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = GlassSpringSpec,
        label = "GlassIconButton_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(LiquidGlassSurface, CircleShape)
            .border(
                width = 1.dp,
                brush = LiquidGlassBorderBrush,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = contentDescription,
            tint               = tint,
            modifier           = Modifier.size(size * 0.52f)
        )
    }
}
