package com.riccardopinato.omnicleaner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val OmniBlue = Color(0xFF4776FF)
val OmniPurple = Color(0xFF8E63FF)
val OmniMint = Color(0xFF30C79B)
val OmniOrange = Color(0xFFFF9F43)
val OmniRose = Color(0xFFFF6B8A)

private val LightColors = lightColorScheme(
    primary = OmniBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7EDFF),
    onPrimaryContainer = Color(0xFF163B9C),
    secondary = OmniPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0EAFF),
    onSecondaryContainer = Color(0xFF4C2B9C),
    tertiary = OmniMint,
    onTertiary = Color(0xFF00382A),
    tertiaryContainer = Color(0xFFD8F8EC),
    onTertiaryContainer = Color(0xFF075D45),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF15171D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17191F),
    surfaceVariant = Color(0xFFF0F2F7),
    onSurfaceVariant = Color(0xFF646977),
    outline = Color(0xFFD9DCE5),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF002D70),
    primaryContainer = Color(0xFF183E8C),
    onPrimaryContainer = Color(0xFFD9E3FF),
    secondary = Color(0xFFD0BCFF),
    onSecondary = Color(0xFF381E72),
    secondaryContainer = Color(0xFF4F378B),
    onSecondaryContainer = Color(0xFFEADDFF),
    tertiary = Color(0xFF7FE1BF),
    onTertiary = Color(0xFF00382A),
    tertiaryContainer = Color(0xFF075D45),
    onTertiaryContainer = Color(0xFFA0F2D4),
    background = Color(0xFF101116),
    onBackground = Color(0xFFE8EAF0),
    surface = Color(0xFF17191F),
    onSurface = Color(0xFFF0F1F5),
    surfaceVariant = Color(0xFF22252D),
    onSurfaceVariant = Color(0xFFB9BDC9),
    outline = Color(0xFF3A3E48),
    error = Color(0xFFFFB4AB),
)

private val OmniTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)


private val OmniShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
)

@Composable
fun OmniCleanerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = OmniTypography,
        shapes = OmniShapes,
        content = content,
    )
}
