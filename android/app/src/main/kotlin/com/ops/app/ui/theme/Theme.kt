package com.ops.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = OpsBlue40,
    onPrimary = OpsNeutral99,
    primaryContainer = OpsBlue90,
    onPrimaryContainer = OpsBlue10,
    secondary = OpsAmber40,
    onSecondary = OpsNeutral99,
    secondaryContainer = OpsAmber90,
    onSecondaryContainer = OpsAmber20,
    // v3: tertiary is the app's one dedicated "success" slot (paid,
    // accepted, completed) — deliberately NOT the same color as primary,
    // unlike v2 where green covered both brand and success.
    tertiary = OpsGreen40,
    onTertiary = OpsNeutral99,
    tertiaryContainer = OpsGreen90,
    onTertiaryContainer = OpsGreen10,
    error = OpsRed40,
    errorContainer = OpsRed90,
    background = OpsNeutral99,
    surface = OpsNeutral99,
    onBackground = OpsNeutral10,
    onSurface = OpsNeutral10,
    // v2/v3: card/list-row borders and secondary text get their own
    // deliberate tokens instead of Material's generated tones, so every
    // screen's "muted" text and outline reads as the same cool grey, not a
    // dozen slightly different auto-derived shades.
    surfaceVariant = OpsSurfaceSunkenLight,
    onSurfaceVariant = Color(0xFF5B6168),
    outline = OpsBorderLight,
    outlineVariant = OpsBorderLight,
)

private val DarkColors = darkColorScheme(
    primary = OpsBlue80,
    onPrimary = OpsBlue20,
    primaryContainer = OpsBlue20,
    onPrimaryContainer = OpsBlue90,
    secondary = OpsAmber90,
    onSecondary = OpsAmber20,
    secondaryContainer = OpsAmber20,
    onSecondaryContainer = OpsAmber90,
    tertiary = OpsGreen80,
    onTertiary = OpsGreen20,
    tertiaryContainer = OpsGreen20,
    onTertiaryContainer = OpsGreen90,
    error = OpsRed90,
    errorContainer = Color(0xFF3A1613),
    background = OpsNeutral10,
    surface = OpsNeutral10,
    onBackground = OpsNeutral90,
    onSurface = OpsNeutral90,
    surfaceVariant = OpsSurfaceSunkenDark,
    onSurfaceVariant = Color(0xFF95999E),
    outline = OpsBorderDark,
    outlineVariant = OpsBorderDark,
)

/**
 * OPS keeps its own small, fixed palette rather than opting into Android 12+
 * dynamic color: the app's brand mark and a business's own uploaded
 * logo/branding on quotes and invoices need to look the same on every phone,
 * not shift with the owner's wallpaper.
 */
@Composable
fun OpsTheme(
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OpsTypography,
        content = content,
    )
}
