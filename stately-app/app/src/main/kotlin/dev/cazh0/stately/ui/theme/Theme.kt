package dev.cazh0.stately.ui.theme

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
    primary = Indigo500,
    onPrimary = White,
    primaryContainer = Indigo50,
    onPrimaryContainer = Indigo900,
    secondary = Teal500,
    onSecondary = White,
    background = Grey50,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    error = Red700,
    onError = White,
)

private val DarkColors = darkColorScheme(
    primary = Indigo200,
    onPrimary = Black,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo50,
    secondary = Teal200,
    onSecondary = Black,
    background = Grey900,
    onBackground = White,
    surface = Grey850,
    onSurface = White,
    error = Red300,
    onError = Black,
)

/**
 * Wraps the whole app.
 *
 * Why dynamic colour is on by default: on Android 12 and up the system derives a palette from
 * the user's wallpaper, and an app that ignores it looks like a visitor on the device. The
 * brand scheme is the fallback, not the preference — but it is a complete, contrast-checked
 * scheme, so nothing degrades on older versions.
 *
 * Typography and shapes are Material 3 defaults deliberately. The legacy app's five themes are
 * a feature to port, not a reason to fork the type scale.
 */
@Composable
fun StatelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
