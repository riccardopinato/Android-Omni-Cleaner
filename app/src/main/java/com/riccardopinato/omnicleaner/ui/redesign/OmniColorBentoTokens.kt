package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.riccardopinato.omnicleaner.R

/**
 * Omni Color Bento — an original Android-native implementation inspired by
 * the user's six Linktree visual references. No Linktree branding/assets.
 *
 * Values are deliberately flat and precise: the mock-up's arbitrary glossy
 * gradients and fake numbers are not part of this design system.
 */
internal object OmniColorBento {
    // Palette confirmed against the user's 20 Nov 2026 Linktree Lens references.
    // Chalk is the main canvas; burgundy is an accent, not a blanket overlay.
    val background = Color(0xFFF3F3F1)
    val sageBackground = Color(0xFFE8EFD6)
    val backgroundDark = Color(0xFF240A15)
    val panel = Color(0xFF780016)
    val olive = Color(0xFF70774F) // olive from the user's first screenshot
    val forest = Color(0xFF254F1A)
    val pink = Color(0xFFE9C0E9)
    val magenta = Color(0xFFC20CD7) // 4.85:1 contrast with white (normal text)
    val cobalt = Color(0xFF061492)
    val blue = Color(0xFF2665D6)
    val lime = Color(0xFFD2E823)
    val ochre = Color(0xFFE8B838)
    val violet = Color(0xFF502274)
    val cyan = Color(0xFF13A8C9)
    val cream = Color(0xFFF3F3F1)
    val ink = Color(0xFF1E2330)
    val white = Color(0xFFFFFFFF)

    val display = FontFamily(
        Font(R.font.space_grotesk_variable, weight = FontWeight.Normal),
        // A true bold face avoids variable-font default weight appearing thin
        // on Android emulators/devices even when Text requests Bold.
        Font(R.font.space_grotesk_bold, weight = FontWeight.Bold),
    )
    val body = FontFamily(Font(R.font.manrope_variable))
}

/**
 * Soft Pastel Cleaner — calm native palette approved for Bento Home.
 *
 * Preserve the nine positional swatch identities and their harmonic order,
 * but lower saturation, use warm paper and readable ink. Neutral surfaces,
 * error/destructive colors and app functionality remain independent.
 */
internal object OmniColorBentoV2Palette {
    val green = Color(0xFFDCEEC9)      // pear
    val sage = Color(0xFFDBEEE4)       // mint/sage
    val gold = Color(0xFFF9E5D3)       // peach
    val yellow = Color(0xFFF8EFD2)     // butter cream
    val blue = Color(0xFFD5E5FA)       // powder blue
    val sky = Color(0xFFD9EFF1)        // misty aqua
    val raspberry = Color(0xFFF6E0EB)  // powder pink
    val blush = Color(0xFFF6DFDA)      // gentle coral
    val violet = Color(0xFFE9E2F6)     // lavender
    val cream = Color(0xFFF8F9F6)      // warm paper
    val legibleInk = Color(0xFF263440)
    val white = Color(0xFFFFFFFF)
    val subtleInk = Color(0xFF60717C)

    /** Positional harmonizer remains the single owner of swatch indices. */
    val positionSwatches = listOf(
        green, blue, gold, violet, yellow, sky, blush, sage, raspberry,
    )

    /** Pastel surfaces are all light: use dark ink, never white on powder blue. */
    fun tileForeground(background: Color): Color = legibleInk

    /**
     * Give larger rectangles slightly more presence without tying tone to
     * the underlying function. Small rectangles become calmer surfaces.
     */
    fun surfaceForPosition(swatch: Color, columns: Int, rows: Int): Color =
        androidx.compose.ui.graphics.lerp(
            cream,
            swatch,
            when {
                columns * rows >= 4 -> 1f
                columns >= 2 || rows >= 2 -> .87f
                else -> .69f
            },
        )
}
