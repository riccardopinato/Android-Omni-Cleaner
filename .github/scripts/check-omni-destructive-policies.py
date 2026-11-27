#!/usr/bin/env python3
"""Fail-closed structural guard for Android Omni Cleaner user-data mutations.
This is a defense-in-depth source scan, not a substitute for runtime tests.
"""
from pathlib import Path
import re
import sys

ROOT = Path("app/src/main/java/com/riccardopinato/omnicleaner")
files = {p.relative_to(ROOT).as_posix(): p.read_text(encoding="utf-8")
         for p in ROOT.rglob("*.kt")}
errors = []

def require(path, marker, explanation):
    if path not in files or marker not in files[path]:
        errors.append(f"{path}: missing {explanation}")

def forbid(path, pattern, explanation):
    if path in files and re.search(pattern, files[path]):
        errors.append(f"{path}: forbidden {explanation}")

for name, text in files.items():
    if "deleteRecursively(" in text:
        errors.append(f"{name}: recursive deletion forbidden in app source")
    if "MediaStore.createDeleteRequest(" in text:
        errors.append(f"{name}: permanent provider delete request forbidden")
    if name.startswith(("ui/", "gallery/", "monetization/")):
        if re.search(r"\.purgeAll\s*\(|\.purgeEntry\s*\(", text):
            errors.append(f"{name}: raw permanent purge from UI forbidden")
        if re.search(r"\bFile\s*\([^\n]*\)\.delete\s*\(", text):
            errors.append(f"{name}: unreviewed UI file delete forbidden")
        if re.search(r"\bcontentResolver\.delete\s*\(", text):
            errors.append(f"{name}: direct provider deletion forbidden")

safe = "cleaner/SafeTrashManager.kt"
core = "cleaner/SafeOperationsCore.kt"
engine = "cleaner/StorageCleanupEngine.kt"
thumb = "cleaner/ThumbnailFileCleanup.kt"
media = "cleaner/MediaStoreHelper.kt"
require(safe, "fun planPurge(", "frozen exact-approval purge plan")
require(safe, "fun purgeApproved(manifest:", "approved-only bulk purge")
require(safe, "fun purgeApproved(entry:", "approved-only single purge")
require(safe, "No raw ID is authorization", "deny raw entry-id purge")
require(safe, "Inspection must never mutate metadata", "side-effect-free inventory")
require(safe, "Cross-volume copy followed by File.delete()", "cross-volume unlink disabled")
forbid(safe, r"if\s*\(\s*!file\.delete\s*\(", "source copy+unlink")
require(core, "class DeletionCoordinator", "single local writer")
require(core, "class SafeOperationsJournal", "persisted write-ahead record")
require(core, "fun needsReview()", "nonmutating crash inspection")
require(engine, "coordinator.moveApproved(", "local changes routed via coordinator")
require(engine, "coordinator.removeEmptyFoldersApproved(", "folder removal routed via coordinator")
require(engine, "fun reconcilePendingMedia()", "persistent MediaStore reconciliation")
require(thumb, "moveVerifiedThumbnailToTrash(", "reversible verified thumbnail operation")
forbid(thumb, r"\bcurrent\.delete\s*\(", "direct thumbnail unlink")
require(media, "MediaStore.createTrashRequest(", "OS-mediated media Trash consent")
require(media, "fun verifyTrashedUris(", "post-consent per URI verification")
require("gallery/GalleryController.kt", "media.generationModified", "generation-bound Gallery review")
for screen in (
    "ui/OmniFileTrashReview.kt",
    "ui/StorageCleanupSection.kt",
    "ui/SmartCategoriesSection.kt",
    "monetization/ProBatchController.kt",
):
    require(screen, "recordMediaTrashOutcome(", "durable MediaStore callback record")
require("ui/SafeTrashSection.kt", "approvedPurgePlan", "frozen UI approval")
require("ui/SafeTrashSection.kt", "confirmEmptySecond", "second irreversible confirmation")
forbid("ui/SafeTrashSection.kt", r"manager\.purgeAll\(", "unscoped dynamic purge")

if errors:
    for error in errors:
        print("DESTRUCTIVE_POLICY_FAIL:", error)
    sys.exit(1)
print(f"OMNI_FAIL_CLOSED_SOURCE_GATE_PASS: checked {len(files)} Kotlin source files")
