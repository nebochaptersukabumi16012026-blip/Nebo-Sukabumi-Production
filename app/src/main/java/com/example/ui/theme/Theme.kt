package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = PremiumBlue,
    secondary = PremiumGreen,
    tertiary = PremiumRed,
    background = Color(0xFF121212),
    surface = PremiumSurface,
    surfaceVariant = PremiumSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = PremiumOnSurface,
    onSurface = PremiumOnSurface,
    onSurfaceVariant = PremiumOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = PremiumBlue,
    secondary = PremiumGreen,
    tertiary = PremiumRed,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFE0E0E0),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.DarkGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = androidx.compose.material3.Shapes(
            medium = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PremiumNavy,
                            PremiumBlack
                        )
                    )
                )
        ) {
            content()
        }
    }
}
