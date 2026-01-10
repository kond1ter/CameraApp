package edu.konditer.cameraapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun CameraAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Android 12+ (API 31+): используем Dynamic Colors (Material You)
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> {
            // Для старых версий используем стандартную темную тему
            androidx.compose.material3.darkColorScheme()
        }
        else -> {
            // Для старых версий используем стандартную светлую тему
            androidx.compose.material3.lightColorScheme()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}