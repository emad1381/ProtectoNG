package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = staticCompositionLocalOf { true }

val PrimaryBlue = Color(0xFF0EA5E9)   // #0EA5E9
val SecondaryCyan = Color(0xFF06B6D4) // #06B6D4

val DarkBg: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0xFF0A0F1E) else Color(0xFFF8FAFC)

val DarkSurface: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0xFF0F172A) else Color(0xFFFFFFFF)

val TextPrimary: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0xFFF1F5F9) else Color(0xFF0F172A)

val TextSecondary: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0xFF94A3B8) else Color(0xFF64748B)

val ColorGreen = Color(0xFF10B981)    // Success / Connected (emerald-500)
val ColorYellow = Color(0xFFF59E0B)   // Connecting / Warning
val ColorRed = Color(0xFFF43F5E)      // Disconnected / Failed (rose-500)

val GlassColor: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0x0DFFFFFF) else Color(0x081E293B) // Slate-800 subtle gray in light mode

val GlassBorderColor: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0x1AFFFFFF) else Color(0x1F1E293B)
