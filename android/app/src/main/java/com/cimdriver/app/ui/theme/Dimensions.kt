package com.cimdriver.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * CIMDriver Design System Spacing Tokens.
 * Standardized spacing hierarchy across all screens and components.
 */
object AppSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp   // Standard screen & card padding
    val lg: Dp = 24.dp   // Section spacing
    val xl: Dp = 32.dp   // Major block separation
    val xxl: Dp = 48.dp
}

/**
 * CIMDriver Design System Shape Tokens.
 * Consistent corner radii across cards, buttons, dialogs, and badges.
 */
object AppShapes {
    val card = RoundedCornerShape(16.dp)
    val cardSmall = RoundedCornerShape(12.dp)
    val dialog = RoundedCornerShape(20.dp)
    val button = RoundedCornerShape(12.dp)
    val input = RoundedCornerShape(12.dp)
    val chip = RoundedCornerShape(8.dp)
    val badge = RoundedCornerShape(6.dp)
    val tag = RoundedCornerShape(4.dp)
    val pill = CircleShape
}

/**
 * CIMDriver Design System Icon Size Tokens.
 */
object AppIconSizes {
    val sm: Dp = 16.dp
    val md: Dp = 20.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val hero: Dp = 48.dp
}

