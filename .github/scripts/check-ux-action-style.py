#!/usr/bin/env python3
"""Check the single blue/white action button contract and native broom tab."""
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
        if expected not in text:
            raise SystemExit(f"{path}: {symbol} not using shared action style")
        if f"import androidx.compose.material3.{symbol}\n" in text:
            raise SystemExit(f"{path}: direct Material3 {symbol} import remains")

buttons = (UI / "ui/theme/OmniActionButtons.kt").read_text(encoding="utf-8")
assert "Color(0xFF285DE5)" in buttons, "action blue changed"
assert "contentColor = Color.White" in buttons, "action white text changed"

broom = (UI / "ui/redesign/OmniNavigationIcon.kt").read_text(encoding="utf-8")
assert "OmniNavigationSymbol.CLEAN -> {" in broom
assert "val bristleFan = Path().apply {" in broom
assert "val sparkle = Path().apply {" in broom

wave = (UI / "ui/redesign/OmniPremiumHome.kt").read_text(encoding="utf-8")
assert ".drawWithCache {" in wave
nav = (UI / "ui/redesign/RedesignedOmniCleanerApp.kt").read_text(encoding="utf-8")
assert "val layerAlpha by animateFloatAsState(" in nav
print(f"OMNI_ACTIONS_AND_BROOM_PASS: {len(SCREEN_FILES)} Kotlin screens")
