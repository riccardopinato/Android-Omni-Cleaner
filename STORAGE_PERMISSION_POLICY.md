# Storage permission policy — MAXI STEP 4

Reviewed: 2026-10-11

## Product decision

Android Omni Cleaner uses two explicit capability lanes.

### Standard Cleaning

Standard Cleaning must remain useful without `MANAGE_EXTERNAL_STORAGE`.

Examples:
- Gallery Cleaner through MediaStore;
- EXIF Privacy through user-selected SAF trees;
- manual maintenance flows;
- app-local Safe Trash management.

The app must not ask for All files access at first launch merely to make these
scoped features work.

### Deep Cleaning

Deep Cleaning is the full-device filesystem analysis lane:
- storage overview;
- empty/small folders;
- large files;
- duplicates;
- thumbnail/cache directories;
- accessible app-residue review.

On Android 11+ this lane may require `MANAGE_EXTERNAL_STORAGE` because its core
user-facing purpose is repeated on-device storage analysis across many directories.
The permission is requested only after the user explicitly chooses to enable Deep
Cleaning.

Google Play approval is **not assumed**. Store readiness remains blocked until
the declaration is reviewed and accepted.

On Android 10 (API 29), a targetSdk 36 app uses scoped storage and the legacy read
permission must not be represented as equivalent to full-filesystem access. The
app therefore keeps Standard Cleaning available but does not claim a complete
Deep Cleaning scan.

On Android 9 and lower, legacy storage permission remains the platform capability
for the reconstructed donor behavior and is requested just-in-time.

## Destructive-flow rules

1. Scan results are snapshots, never permanent authorization to delete.
2. Selected files are revalidated immediately before mutation.
3. Duplicate deletion requires both the selected file and a surviving keeper to
   still match the recovered donor SHA-256 group.
4. Well-known top-level user folders such as DCIM, Download, Documents, Pictures,
   Music and Movies are never removed as empty-folder candidates.
5. Media use Android's system trash request.
6. Small non-media files may enter app-private Safe Trash only after revalidation.
7. Large non-media files have no permanent-delete fallback.
8. Thumbnail deletion requires size + modified time + SHA-256 to remain unchanged.
9. App residues remain review-only where package visibility makes uninstall state ambiguous.
10. Moving an item to Safe Trash or Android trash is **not** reported as bytes already freed.
    Only actual deletion may be counted as reclaimed bytes.

## Current official policy basis

Android Developers — Manage all files on a storage device:
https://developer.android.com/training/data-storage/manage-all-files

Android Developers — Data and file storage overview:
https://developer.android.com/training/data-storage

Google Play — All files access permission:
https://support.google.com/googleplay/android-developer/answer/10467955

Google Play classifies All files access as high-risk/sensitive. It must be tied
to essential core user-facing functionality, requested clearly, and approved in
Play review. This document records the product justification; it does not claim
that Google Play has approved the app.
