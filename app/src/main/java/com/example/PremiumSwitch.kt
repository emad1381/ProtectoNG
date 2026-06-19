package com.example

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SecondaryCyan

@Composable
fun PremiumSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Elegant, slender track dimensions modeled after Apple & Surfshark premium designs
    val trackWidth = 52.dp
    val trackHeight = 28.dp
    val thumbSize = 22.dp
    val padding = 3.dp

    val transition = updateTransition(targetState = checked, label = "highRefreshSwitchTransition")

    // High performance spring physics for flagship 90/120Hz displays
    val thumbOffset by transition.animateDp(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy, // Sleek responsive spring bounce
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "highPerfThumbOffset"
    ) { state ->
        if (state) (trackWidth - thumbSize - padding) else padding
    }

    // Glassmorphism active gradient track
    val activeTrackBrush = Brush.horizontalGradient(
        colors = listOf(Color(0x3D0052D4), Color(0x4D00E5FF))
    )
    
    // Inactive translucent dark slate glass track
    val inactiveTrackBrush = Brush.linearGradient(
        colors = listOf(Color(0x22121824), Color(0x3F0D1117))
    )

    val trackBorderColor by transition.animateColor(
        transitionSpec = { tween(180, easing = LinearOutSlowInEasing) },
        label = "highPerfTrackBorder"
    ) { state ->
        if (state) SecondaryCyan.copy(alpha = 0.65f) else Color(0x28FFFFFF)
    }

    val thumbGlowAlpha by transition.animateFloat(
        transitionSpec = { tween(240, easing = FastOutSlowInEasing) },
        label = "highPerfGlow"
    ) { state ->
        if (state) 0.65f else 0.0f
    }

    // High fidelity active/inactive colors
    val thumbColor by transition.animateColor(
        transitionSpec = { tween(200) },
        label = "highPerfThumbColor"
    ) { state ->
        if (state) Color.White else Color(0xFFECEFF1)
    }

    Box(
        modifier = modifier
            .size(trackWidth, trackHeight)
            .clip(CircleShape)
            .background(if (checked) Color.Transparent else Color.Transparent)
            .drawBehind {
                // Background fill
                if (checked) {
                    drawRoundRect(
                        brush = activeTrackBrush,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                    )
                } else {
                    drawRoundRect(
                        brush = inactiveTrackBrush,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                    )
                }
            }
            .border(1.2.dp, trackBorderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        // Cyan Glow strictly around the thumb in active zone
        if (thumbGlowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset - 4.dp)
                    .size(thumbSize + 8.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SecondaryCyan.copy(alpha = thumbGlowAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Luxurious animated Thumb button
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(
                    elevation = if (checked) 6.dp else 2.dp,
                    shape = CircleShape,
                    ambientColor = if (checked) SecondaryCyan else Color.Black,
                    spotColor = if (checked) SecondaryCyan else Color.Black
                )
                .background(
                    brush = if (checked) {
                        Brush.verticalGradient(listOf(Color.White, Color(0xFFE0F7FA)))
                    } else {
                        Brush.verticalGradient(listOf(Color.White, Color(0xFFCFD8DC)))
                    },
                    shape = CircleShape
                )
                .border(
                    width = 0.5.dp,
                    color = if (checked) SecondaryCyan.copy(alpha = 0.6f) else Color(0x3F000000),
                    shape = CircleShape
                )
        )
    }
}
