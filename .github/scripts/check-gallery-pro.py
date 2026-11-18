#!/usr/bin/env python3
"""Read-only Gallery Pro API / language / navigation verification."""
from pathlib import Path
from xml.etree import ElementTree as ET
root = Path(__file__).resolve().parents[2]
src = root / "app/src/main/java/com/riccardopinato/omnicleaner"
files = (
    src / "gallery/GalleryProAnalysis.kt",
    src / "gallery/GalleryProPhotoScanner.kt",
    src / "gallery/GalleryProSection.kt",
)
contents = [p.read_text(encoding="utf-8") for p in files]
gallery = (src / "gallery/GalleryCleanerSection.kt").read_text(encoding="utf-8")
nav = (src / "ui/redesign/RedesignedOmniCleanerApp.kt").read_text(encoding="utf-8")
assert "GALLERY_INSIGHTS" in nav
assert "omni_gallery_open_pro" in gallery
assert "groupSimilarGalleryPhotos" in contents[0]
assert "take(160)" in contents[1]
assert "loadThumbnail" in contents[1]
assert "Video analysis" in (root / ".maestro/gallery-pro.yaml").read_text(encoding="utf-8")
assert (root / "app/src/test/java/com/riccardopinato/omnicleaner/gallery/GalleryProAnalysisTest.kt").exists()
for code in ("deleteRecursively(", ".delete(", "moveToTrash(", "trashMediaFiles(",
             "createTrashRequest(", "FileOutputStream("):
    for path, content in zip(files, contents):
        assert code not in content, f"{path.name}: forbidden mutation API {code}"
keys = {
"gp_title", "gp_intro", "gp_open_insights", "gp_loaded_scope", "gp_load_first",
"gp_video_title", "gp_video_scope", "gp_video_all", "gp_video_large", "gp_video_long",
"gp_video_4k", "gp_video_search", "gp_video_count", "gp_video_details",
"gp_results_limited", "gp_similar_title", "gp_similar_explain", "gp_photo_available",
"gp_scan_similar", "gp_photo_error", "gp_similar_results", "gp_possible_group",
"gp_unavailable_photo", "gp_none_detected", "gp_no_auto_delete"
}
for lang in ("values", "values-it", "values-es", "values-fr", "values-pt"):
    parsed = ET.parse(root / "app/src/main/res" / lang / "strings.xml")
    actual = {x.attrib["name"] for x in parsed.getroot()
              if x.tag == "string" and "name" in x.attrib}
    assert keys.issubset(actual), f"{lang}: missing {keys - actual}"
print("Gallery Pro read-only / 5 locales / routing / bounded fingerprints PASS")
