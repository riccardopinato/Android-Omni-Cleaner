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
 * User-approved "A — Colorata" palette, 20 November 2026.
 * These exact nine swatches are used as tile backgrounds; neutral ink is
 * permitted only for accessibility-critical text, borders and dark mode.
 */
internal object OmniColorBentoV2Palette {
    val green = Color(0xFF8BC45C)
    val sage = Color(0xFFB3CF8C)
    val gold = Color(0xFFEFBD51)
    val yellow = Color(0xFFFFD371)
    val blue = Color(0xFF597ED1)
    val sky = Color(0xFF88C0F8)
    val raspberry = Color(0xFFE72F63)
    val blush = Color(0xFFF6B1CF)
    val cream = Color(0xFFFDF7E8)

    // White or ordinary charcoal on blue/raspberry has insufficient
    // contrast for small text. This near-black yields >=4.5:1 throughout.
    val legibleInk = Color(0xFF070D13)

    /** One function, one canonical colour; no random/unstable colours. */
    fun tileColor(id: String): Color = when (id) {
        "storage" -> green
        "duplicates" -> gold
        "large" -> yellow
        "gallery" -> sky
        "privacy", "privacyQuick" -> raspberry
        "trash", "privacyBatch" -> blush
        "folders", "categories" -> sage
        "review", "galleryPro", "advanced" -> blue
        "intelligence", "settings" -> cream
        "galleryReview" -> sky
        "filters" -> yellow
        "pro" -> raspberry
        else -> cream
    }
}
