#!/usr/bin/env python3
"""Review Center, photo comparison and QA source consistency contract."""
from pathlib import Path
from xml.etree import ElementTree as ET
root = Path(__file__).resolve().parents[2]
src = root / "app/src/main/java/com/riccardopinato/omnicleaner"
review = (src / "ui/redesign/ReviewCenterSection.kt").read_text(encoding="utf-8")
model = (src / "ui/ReviewCenterModel.kt").read_text(encoding="utf-8")
compare = (src / "gallery/GalleryPhotoComparison.kt").read_text(encoding="utf-8")
clean = (src / "ui/redesign/CleanerScreens.kt").read_text(encoding="utf-8")
nav = (src / "ui/redesign/RedesignedOmniCleanerApp.kt").read_text(encoding="utf-8")
flow = (root / ".maestro/review-center.yaml").read_text(encoding="utf-8")
assert "CleanerToolRoute.REVIEW" in nav and "CLEAN_REVIEW" in nav
assert "ReviewCenterSection(" in clean and "omni_clean_detail_${tool.name.lowercase(Locale.ROOT)}" in clean
assert "omni_review_center" in review and "omni_review_open_gallery" in review
assert "omni_review_open_gallery" in flow
assert "filesystemEntries" in model and "mediaEntries" in model and "distinctBy" in model
assert "ContentScale.Fit" in compare and "loadPreviewBitmap" in compare
assert "omni_gallery_compare" in compare
assert "Android MediaStore Trash" in review or "Android MediaStore Trash" in model
forbidden = ("deleteRecursively(", ".delete(", "moveToTrash(", "trashMediaFiles(",
             "createTrashRequest(", "FileOutputStream(", "MediaStore.createTrashRequest(")
for file, contents in (("ReviewCenterSection", review), ("ReviewCenterModel", model),
                       ("GalleryPhotoComparison", compare)):
    for code in forbidden:
        assert code not in contents, f"{file}: forbidden destructive API {code}"
required = (
"gp_compare_title", "gp_compare_note", "gp_compare_loading", "gp_compare_meta",
"gp_compare_next", "gp_compare_previous", "gp_compare_open",
"review_center_title", "review_center_explain", "review_center_filesystem_title",
"review_center_file_summary", "review_center_no_guarantee", "review_center_stale",
"review_center_none", "review_center_clear", "review_center_gallery_title",
"review_center_gallery_summary", "review_center_gallery_note",
"review_center_open_gallery", "review_center_footer",
)
for lang in ("values", "values-it", "values-es", "values-fr", "values-pt"):
    values = ET.parse(root / "app/src/main/res" / lang / "strings.xml").getroot()
    present = {e.attrib["name"] for e in values if e.tag == "string" and "name" in e.attrib}
    assert set(required).issubset(present), (lang, set(required) - present)
assert (root / "app/src/test/java/com/riccardopinato/omnicleaner/ui/ReviewCenterModelTest.kt").exists()
print("OMNI_REVIEW_COMPARE_SAFETY_PASS: review/no-mutation/fit/5 locales")
