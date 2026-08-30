package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium Slate Dark Theme Palette
val SlateDarkBackground = Color(0xFF0C1017)
val SlateDarkSurface = Color(0xFF161F2B)
val SlateDarkCard = Color(0xFF1F2B3E)

val EmeraldGreen = Color(0xFF00E676)   // Income / Salary
val OceanBlue = Color(0xFF29B6F6)     // Investments (CDB, FIIs)
val GoldAmber = Color(0xFFFFA726)     // Box / Goals
val LavenderPurple = Color(0xFFAB47BC) // Money Lent
val CoralRed = Color(0xFFEF5350)      // Bills / Expenses
val ApartmentTeal = Color(0xFF26C6DA)  // Apartment / Housing Expenses

val SlateDarkPrimary = EmeraldGreen
val SlateDarkSecondary = OceanBlue
val SlateDarkTertiary = GoldAmber

// Raw values for DarkColorScheme mapping
val RawTextPrimaryDark = Color(0xFFF3F4F6)
val RawTextSecondaryDark = Color(0xFF9CA3AF)
val RawBorderColorDark = Color(0xFF2D3D54)

// Dynamic theme-aware color properties
val TextPrimary: Color
    @Composable
    get() = MaterialTheme.colorScheme.onBackground

val TextSecondary: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val BorderColor: Color
    @Composable
    get() = MaterialTheme.colorScheme.outline

