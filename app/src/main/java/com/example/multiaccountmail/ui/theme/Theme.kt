package com.example.multiaccountmail.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = accent.onAccent(),
    primaryContainer = accent.copy(alpha = 0.18f),
    onPrimaryContainer = accent.darken(0.45f),
    secondary = accent.darken(0.85f),
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceContainer = Color(0xFFEEEEF2),
    surfaceContainerLow = Color(0xFFF4F3F7),
    surfaceContainerHigh = Color(0xFFE6E6EA)
)

private fun lift(accent: Color): Color {
    val sum = accent.red + accent.green + accent.blue
    if (sum >= 1.15f) return accent
    fun c(v: Float) = (v * 1.45f).coerceAtMost(1f)
    return Color(c(accent.red), c(accent.green), c(accent.blue))
}

private fun darkScheme(accent: Color, amoled: Boolean) = darkColorScheme(
    primary = lift(accent),
    onPrimary = Color(0xFF1A1C1E),
    primaryContainer = accent.copy(alpha = 0.35f),
    onPrimaryContainer = Color(0xFFE8EAF6),
    secondary = accent,
    background = if (amoled) Color.Black else Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = if (amoled) Color.Black else Color(0xFF121212),
    onSurface = Color(0xFFE6E1E5),
    surfaceContainer = if (amoled) Color(0xFF0E0E0E) else Color(0xFF1E1E1E),
    surfaceContainerLow = if (amoled) Color(0xFF0A0A0A) else Color(0xFF1A1A1A),
    surfaceContainerHigh = if (amoled) Color(0xFF1A1A1A) else Color(0xFF2A2A2A)
)

@Composable
fun MultiAccountMailTheme(
    content: @Composable () -> Unit
) {
    val appearance by AppearanceStore.state.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val dark = when (appearance.mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    // Динамический цвет используется только при явном выборе «Цвет системы».
    // Любой выбранный пользователем акцент всегда имеет приоритет.
    val useDynamic = appearance.accent == Accent.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        useDynamic -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkScheme(appearance.accent.color, appearance.amoled)
        else -> lightScheme(appearance.accent.color)
    }.let { scheme ->
        if (dark && appearance.amoled && appearance.accent == Accent.DYNAMIC) {
            scheme.copy(
                background = Color.Black,
                surface = Color.Black
            )
        } else scheme
    }.let { scheme ->
        // Безопасная косметика: меняются только цвета Material-поверхностей.
        val alpha = appearance.accentStrength.containerAlpha
        val accentContainer = scheme.primary.copy(alpha = alpha)
        when (appearance.surfaceStyle) {
            SurfaceStyle.MINIMAL -> scheme.copy(
                primaryContainer = accentContainer,
                surfaceContainer = scheme.surface,
                surfaceContainerLow = scheme.surface
            )
            SurfaceStyle.SOFT -> scheme.copy(
                primaryContainer = accentContainer,
                surfaceContainer = lerp(scheme.surface, scheme.primary, if (dark) 0.10f else 0.05f),
                surfaceContainerLow = lerp(scheme.surface, scheme.primary, if (dark) 0.06f else 0.025f)
            )
            SurfaceStyle.CONTRAST -> scheme.copy(
                primaryContainer = accentContainer,
                surfaceContainer = lerp(scheme.surfaceContainer, scheme.primary, if (dark) 0.18f else 0.10f),
                surfaceContainerLow = lerp(scheme.surfaceContainerLow, scheme.primary, if (dark) 0.10f else 0.05f)
            )
        }
    }

    val density = LocalDensity.current
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            // Убирает чёрную системную полосу под нижней пилюлей.
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale * appearance.fontScale.value
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun appearance(): Appearance {
    val value by AppearanceStore.state.collectAsState()
    return value
}
