#!/usr/bin/env python3
"""Guard vector onboarding and complete five-language translations."""
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "app/src/main/java/com/riccardopinato/omnicleaner"
MAIN_RES = ROOT / "app/src/main/res"
KEYS = (
    "welcome_clean_title", "welcome_clean_body",
    "welcome_clean_step1", "welcome_clean_step2", "welcome_clean_step3",
    "welcome_gallery_title", "welcome_gallery_body",
    "welcome_gallery_step1", "welcome_gallery_step2", "welcome_gallery_step3",
    "welcome_privacy_title", "welcome_privacy_body",
    "welcome_privacy_step1", "welcome_privacy_step2", "welcome_privacy_step3",
    "welcome_pro_title", "welcome_pro_body",
    "welcome_pro_step1", "welcome_pro_step2", "welcome_pro_step3",
)
for locale in ("values", "values-it", "values-es", "values-fr", "values-pt"):
    doc = ET.parse(MAIN_RES / locale / "strings.xml")
    actual = {e.attrib["name"]: e.text for e in doc.findall(".//string")}
    missing = [key for key in KEYS if not actual.get(key, "").strip()]
    if missing:
        raise SystemExit(f"{locale}: incomplete illustrated onboarding strings: {missing}")
panel = (JAVA / "ui/redesign/OmniWelcomePanel.kt").read_text(encoding="utf-8")
assert "Canvas(" in panel and "clearAndSetSemantics" in panel
assert "DrawScope.drawWelcomeArtwork" in panel
assert not any(word in panel for word in ("BitmapFactory", "AsyncImage", "painterResource", "rememberInfiniteTransition"))
clean = (JAVA / "ui/redesign/CleanerScreens.kt").read_text(encoding="utf-8")
assert "state.overview == null && !state.foldersScanned" in clean
assert "!state.scanning && state.error == null" in clean
gallery = (JAVA / "gallery/GalleryCleanerSection.kt").read_text(encoding="utf-8")
assert "state.items.isEmpty() && state.totalReviewedCount == 0" in gallery
assert "state.pendingTrashItems.isEmpty()" in gallery
privacy = (JAVA / "exif/ExifCleanerSection.kt").read_text(encoding="utf-8")
assert "singleSource == null && sourceTree == null" in privacy
assert "status == null" in privacy
pro = (JAVA / "monetization/MonetizationSection.kt").read_text(encoding="utf-8")
assert "if (!premium) {" in pro and "OmniWelcomeKind.PRO" in pro
for kind in ("CLEAN", "GALLERY", "PRIVACY", "PRO"):
    assert f"OmniWelcomeKind.{kind}" in panel or f"enum class OmniWelcomeKind" in panel
print(f"OMNI_WELCOME_UI_PASS: 4 states, {len(KEYS)} translated keys in 5 locales, vector only")
