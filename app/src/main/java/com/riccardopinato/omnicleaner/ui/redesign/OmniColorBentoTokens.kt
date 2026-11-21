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
 * Pop Fresh 2027 — nine user-selected swatches, 20 November 2026.
 * Color-only alternative to the previous Linktree Color Bento v2 palette.
 * Neutral off-white and dark navy are accessibility surfaces/ink, not extra tiles.
 * Keep screen structure, routes, motion and scan behavior unchanged.
 */
internal object OmniColorBentoV2Palette {
    val green = Color(0xFFC9F25D)      // Lime Pop
    val sage = Color(0xFF87DEAA)       // Spring Green
    val gold = Color(0xFFFFAE58)       // Tangerine
    val yellow = Color(0xFFFFE066)     // Sunshine
    val blue = Color(0xFF3C55E8)       // Electric Blue
    val sky = Color(0xFF5EDBD5)        // Aqua Fresh
    val raspberry = Color(0xFFFF83C2)  // Bubblegum
    val blush = Color(0xFFFF7A73)      // Coral Pop
    val violet = Color(0xFFC4A2FF)     // Soft Violet
    val cream = Color(0xFFF7F7F4)      // Neutral surface
    val legibleInk = Color(0xFF192033)
    val white = Color(0xFFFFFFFF)

    /**
     * Approved nine swatches in a stable editorial starting order. The actual
     * position-derived order is chosen by bentoV2HarmonizedColorIndices()
     * from visible tile geometry, not from a module ID.
     */
    val positionSwatches = listOf(
        green, blue, gold, violet, yellow, sky, blush, sage, raspberry,
    )

    /** Electric Blue needs white ink; all other swatches use dark navy. */
    fun tileForeground(background: Color): Color =
        if (background == blue) white else legibleInk
}
