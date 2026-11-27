#!/usr/bin/env python3
"""Check branded primary, quiet secondary, selected states and broom tab."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
UI = ROOT / "app/src/main/java/com/riccardopinato/omnicleaner"
SCREEN_FILES = [
    "exif/ExifCleanerSection.kt",
    "gallery/GalleryCleanerSection.kt",
    "gallery/GalleryMediaPreview.kt",
    "monetization/MonetizationSection.kt",
    "monetization/ProBatchReview.kt",
    "ui/MaintenanceSection.kt",
    "ui/SafeTrashSection.kt",
    "ui/SmartCategoriesSection.kt",
    "ui/StorageCleanupSection.kt",
    "ui/redesign/CleanerScreens.kt",
    "ui/redesign/OmniPremiumHome.kt",
    "ui/redesign/RedesignedOmniCleanerApp.kt",
]

for path in SCREEN_FILES:
    text = (UI / path).read_text(encoding="utf-8")
    for symbol in ("Button", "OutlinedButton", "TextButton"):
        if f"{symbol}(" not in text:
            continue
        expected = f"import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as {symbol}"
        preferred = {
            "Button": "OmniActionButton",
            "OutlinedButton": "OmniSecondaryButton",
            "TextButton": "OmniTextButton",
        }[symbol]
        adapted = f"import com.riccardopinato.omnicleaner.ui.theme.{preferred} as {symbol}"
        if expected not in text and adapted not in text:
            raise SystemExit(f"{path}: {symbol} not using first-party semantic action style")
        if f"import androidx.compose.material3.{symbol}\n" in text:
            raise SystemExit(f"{path}: direct Material3 {symbol} import remains")

buttons = (UI / "ui/theme/OmniActionButtons.kt").read_text(encoding="utf-8")
assert "Color(0xFF285DE5)" in buttons, "action blue changed"
assert "contentColor = Color.White" in buttons, "action white text changed"
assert "fun OmniSecondaryButton(" in buttons, "quiet secondary action missing"
assert "fun OmniTextButton(" in buttons, "quiet tertiary action missing"
assert "colors = colors ?: ButtonDefaults.buttonColors(" in buttons, "semantic color overrides ignored"
gallery_controls = (UI / "gallery/GalleryCleanerSection.kt").read_text(encoding="utf-8")
assert "FlowRow(" in gallery_controls and "omni_gallery_filter_" in gallery_controls, "gallery filters must wrap"
assert "semantics { selected = selectedFilter }" in gallery_controls
clean_controls = (UI / "ui/redesign/CleanerScreens.kt").read_text(encoding="utf-8")
assert "omni_threshold_unit_" in clean_controls and 'Text("✓  " + unit.label)' in clean_controls
assert "listOf(listOf(CleanerToolRoute.OVERVIEW))" in clean_controls, "bento primary hierarchy missing"


broom = (UI / "ui/redesign/OmniNavigationIcon.kt").read_text(encoding="utf-8")
assert "OmniNavigationSymbol.CLEAN -> {" in broom
assert "val bristleFan = Path().apply {" in broom
assert "val sparkle = Path().apply {" in broom

wave = (UI / "ui/redesign/OmniPremiumHome.kt").read_text(encoding="utf-8")
assert ".drawWithCache {" in wave
nav = (UI / "ui/redesign/RedesignedOmniCleanerApp.kt").read_text(encoding="utf-8")
assert "val layerAlpha by animateFloatAsState(" in nav
assert '"omni_home_back"' in nav, "functional pages must return directly to Home"
assert '"omni_admob_bottom_banner"' in nav, "single-Home shell must own the banner"
assert "NavigationBar(" not in nav, "five-tab bar must not reappear"
assert "testTagsAsResourceId = true" in nav
gallery = (UI / "gallery/GalleryMediaPreview.kt").read_text(encoding="utf-8")
assert "BorderStroke(2.dp, TrashRed)" in gallery
assert "BorderStroke(2.dp, KeepGreen)" in gallery
print(f"OMNI_ACTIONS_BENTO_FILTERS_PASS: {len(SCREEN_FILES)} Kotlin screens")
