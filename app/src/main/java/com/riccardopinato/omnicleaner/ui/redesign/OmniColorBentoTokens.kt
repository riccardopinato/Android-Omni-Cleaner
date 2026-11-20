package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.riccardopinato.omnicleaner.R

/**
 * Omni Color Bento — an original Android-native implementation inspired by
 * the user's six Linktree visual references. No Linktree branding/assets.
 *
 * Values are deliberately flat and precise: the mock-up's arbitrary glossy
 * gradients and fake numbers are not part of this design system.
 */
internal object OmniColorBento {
    val background = Color(0xFF76001D)
    val backgroundDark = Color(0xFF420014)
    val panel = Color(0xFF8A1735)
    val olive = Color(0xFF70774F)
    val pink = Color(0xFFF0C5EB)
    val magenta = Color(0xFFD20CDF)
    val cobalt = Color(0xFF0B1A94)
    val lime = Color(0xFFD9ED27)
    val cream = Color(0xFFF1F1DF)
    val ink = Color(0xFF151729)
    val white = Color(0xFFFEFDF8)

    val display = FontFamily(Font(R.font.space_grotesk_variable))
    val body = FontFamily(Font(R.font.manrope_variable))
}
