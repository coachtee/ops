package com.ops.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = OpsGreen40,
    onPrimary = OpsNeutral99,
    primaryContainer = OpsGreen90,
    onPrimaryContainer = OpsGreen10,
    secondary = OpsAmber40,
    onSecondary = OpsNeutral99,
    secondaryContainer = OpsAmber90,
    onSecondaryContainer = OpsGreen10,
    error = OpsRed40,
    errorContainer = OpsRed90,
    background = OpsNeutral99,
    surface = OpsNeutral99,
    onBackground = OpsNeutral10,
    onSurface = OpsNeutral10,
)

private val DarkColors = darkColorScheme(
    primary = OpsGreen80,
    onPrimary = OpsGreen20,
    primaryContainer = OpsGreen20,
    onPrimaryContainer = OpsGreen90,
    secondary = OpsAmber90,
    onSecondary = OpsGreen20,
    error = OpsRed90,
    background = OpsNeutral10,
    surface = OpsNeutral10,
    onBackground = OpsNeutral90,
    onSurface = OpsNeutral90,
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
