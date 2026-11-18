#!/usr/bin/env python3
"""Verify two-level navigation and original-aspect Gallery media playback contracts."""
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "app/src/main/java/com/riccardopinato/omnicleaner"
root = (JAVA / "ui/redesign/RedesignedOmniCleanerApp.kt").read_text(encoding="utf-8")
clean = (JAVA / "ui/redesign/CleanerScreens.kt").read_text(encoding="utf-8")
preview = (JAVA / "gallery/GalleryMediaPreview.kt").read_text(encoding="utf-8")
gallery = (JAVA / "gallery/GalleryCleanerSection.kt").read_text(encoding="utf-8")
exif = (JAVA / "exif/ExifCleanerSection.kt").read_text(encoding="utf-8")
assert "OmniDetailRoute" in root and "BackHandler(enabled = activeDetail != null)" in root
assert "if (activeDetail == null)" in root
assert "OmniDetailRoute.GALLERY_REVIEW" in root
assert "CleanHubScreen(" in root and "CleanDetailScreen(" in root
assert "CleanHubScreen(" in clean and "CleanDetailScreen(" in clean
assert "omni_clean_tool_" in clean and "omni_scan_" in clean
assert "GalleryCleanerSection(" in gallery and "omni_gallery_open_review" in gallery
assert "ContentScale.Crop" not in preview
assert "ContentScale.Fit" in preview
assert "VideoView(context)" in preview and "MediaController(context)" in preview
assert "video.stopPlayback()" in preview and "Lifecycle.Event.ON_PAUSE" in preview
assert "galleryFitHeightDp" in preview
# First-party immersive viewer is read-only, keeps both media types at original
# proportions and disposes the embedded player before opening its fullscreen peer.
for expected in (
    "omni_gallery_immersive_open",
    "omni_gallery_immersive_close",
    "omni_gallery_immersive_content",
    "if (!showImmersivePreview)",
    "DialogProperties(",
    "WindowInsets.safeDrawing",
):
    assert expected in preview, f"missing immersive gallery contract: {expected}"
assert "CleanHubToolGroup(" in clean and "CleanerToolRoute.TRASH" in clean
assert "CleanerToolRoute.DUPLICATES" in clean and "controller::cancelScan" in clean

assert "internal class ExifWorkflowState" in exif and "scope: CoroutineScope" in exif
assert "onTrash()" in preview and "onKeep()" in preview
keys = (
    "nav_back", "nav_gallery_review", "nav_privacy_quick",
    "nav_privacy_batch", "nav_pro_manage", "gallery_open_review",
    "gallery_video_play", "gallery_video_unavailable",
    "omni_clean_group_analyze", "omni_clean_group_review", "omni_clean_group_advanced",
    "omni_scan_running", "omni_gallery_immersive_open",
    "omni_gallery_immersive_close", "omni_gallery_immersive_hint",
)
for lang in ("values","values-it","values-es","values-fr","values-pt"):
    doc = ET.parse(ROOT / "app/src/main/res" / lang / "strings.xml")
    found = {item.attrib["name"]: item.text for item in doc.findall(".//string")}
    missing = [key for key in keys if not found.get(key, "").strip()]
    if missing:
        raise SystemExit(f"{lang}: missing {missing}")
print("OMNI_TWO_LEVEL_MEDIA_PASS: separate tool screens; full-aspect media; native video player; 5 locales")
