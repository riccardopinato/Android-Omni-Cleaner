#!/usr/bin/env python3
"""Build-time contract for read-only Storage Intelligence (no mutation surface)."""
from pathlib import Path
from xml.etree import ElementTree as ET

root = Path(__file__).resolve().parents[2]
src = root / "app/src/main/java/com/riccardopinato/omnicleaner"
model = (src / "cleaner/StorageIntelligence.kt").read_text(encoding="utf-8")
screen = (src / "ui/redesign/StorageIntelligenceSection.kt").read_text(encoding="utf-8")
nav = (src / "ui/redesign/RedesignedOmniCleanerApp.kt").read_text(encoding="utf-8")
clean = (src / "ui/redesign/CleanerScreens.kt").read_text(encoding="utf-8")
flow = (root / ".maestro/storage-intelligence.yaml").read_text(encoding="utf-8")

assert "StorageInventoryRepository.snapshot(root)" in screen
assert "StorageIntelligenceAnalyzer().fromInventory(root, inventory)" in screen
assert "SmartCategoryScanner().classify(" in model
assert "StorageIntelligenceSection(" in clean
assert "CleanerToolRoute.INTELLIGENCE" in nav
assert 'omni_bento_v2_intelligence' in flow
assert 'omni_storage_intelligence_scan' in flow
assert "extendedWaitUntil" in flow
assert (root / "app/src/test/java/com/riccardopinato/omnicleaner/cleaner/StorageIntelligenceTest.kt").is_file()

for forbidden in ("deleteRecursively(", ".delete(", "moveToTrash(", "trashMediaFiles(",
                  "createTrashRequest(", "FileOutputStream(", "MANAGE_EXTERNAL_STORAGE"):
    assert forbidden not in model and forbidden not in screen, forbidden

langs = ("values", "values-it", "values-fr", "values-es", "values-pt")
required = {
    "si_title", "si_intro", "si_analyze", "si_loading", "si_error",
    "si_map_title", "si_indexed", "si_coverage", "si_map_hint",
    "si_root_files", "si_search_title", "si_read_only", "si_folder_active",
    "si_all_folders", "si_search_label", "si_search_hint", "si_min_size",
    "si_size_all", "si_size_10m", "si_size_100m", "si_matches",
    "si_no_matches", "si_show_more", "si_open_large", "si_suggestions",
    "si_suggestion_note", "si_old_downloads", "si_installers",
    "si_archives", "si_screenshots", "si_suggestion_count",
    "si_no_suggestions", "si_open_categories",
}
for lang in langs:
    file = root / "app/src/main/res" / lang / "strings.xml"
    xml = ET.parse(file)
    strings = {element.attrib["name"] for element in xml.getroot()
               if element.tag == "string" and "name" in element.attrib}
    assert required.issubset(strings), f"{lang} missing {sorted(required - strings)}"
print("Storage Intelligence safety/navigation/localization contract PASS")
