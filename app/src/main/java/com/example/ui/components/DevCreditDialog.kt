package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.*
import com.example.util.DevCreditManager

@Composable
fun DevCreditDialog(
    onDismiss: () -> Unit,
    onSnooze7Days: () -> Unit
) {
    val context = LocalContext.current
    var snoozeChecked by remember { mutableStateOf(false) }
    var showInAppBrowser by remember { mutableStateOf(false) }

    if (showInAppBrowser) {
        InAppWebBrowserDialog(
            url = DevCreditManager.DEV_URL,
            title = "Ahmed Ishtiaque Zarir — Portfolio",
            onDismiss = { showInAppBrowser = false }
        )
    }

    Dialog(
        onDismissRequest = {
            if (snoozeChecked) onSnooze7Days() else onDismiss()
        },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(LiquidGlassSurfaceElevated)
                .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(26.dp))
                .testTag("dev_credit_dialog")
        ) {
            // Specular sheen top stripe
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Official CinePulse Brand Logo with specular circle border
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(LiquidGlassSurface)
                        .border(1.dp, LiquidGlassBorderBrush, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.cinepulse_logo),
                        contentDescription = "CinePulse Logo",
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(CinemaAmber.copy(alpha = 0.15f))
                        .border(0.7.dp, CinemaAmber.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "APPLICATION DEVELOPER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CinemaAmber,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = DevCreditManager.DEV_NAME,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Crafted with passion for cinema and anime streaming enthusiasts.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Visit Website CTA - Liquid Glass Button with Red Gradient
                LiquidGlassButton(
                    text = "Visit Portfolio Website",
                    onClick = { showInAppBrowser = true },
                    leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dev_website_button")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Snooze for 7 days glass pill toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(LiquidGlassSurface)
                        .border(1.dp, LiquidGlassBorderBrush, RoundedCornerShape(14.dp))
                        .clickable { snoozeChecked = !snoozeChecked }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = snoozeChecked,
                            onCheckedChange = { snoozeChecked = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.White,
                                checkmarkColor = PitchBlack,
                                uncheckedColor = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Don't show again for 7 days",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Continue / Dismiss button
                LiquidGlassSecondaryButton(
                    text = if (snoozeChecked) "Snooze & Continue" else "Continue to CinePulse",
                    onClick = {
                        if (snoozeChecked) onSnooze7Days() else onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
