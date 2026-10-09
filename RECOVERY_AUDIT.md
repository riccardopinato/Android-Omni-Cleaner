# Recovery Audit — Supplied APK

Status: FORENSIC BASELINE
Date: 2026-10-09

## Scope

This document records what can be reasonably recovered from the supplied APK without pretending that decompiled bytecode is identical to the lost source repository.

## High-confidence findings

### Native stack
The APK is Android native, not Flutter.

Observed architecture/components indicate:
- Kotlin;
- Jetpack Compose;
- Material 3;
- Android-native storage/media APIs.

Decision:
retain native Kotlin/Compose.

### Recoverable class/function surface

Notable recovered logical components include:
- FileScanner
- FileInventoryScanner
- SpaceOverviewScanner
- SafeTrashManager
- MediaStoreHelper
- PathSafety
- ThumbnailFileCleanup
- GalleryCursorSource
- GalleryPreferencesManager
- ProPreferencesManager
- FolderExifCleaner
- JpegExifEditor
- JpegContentDigest
- BillingManager
- AdManager
- OmniCleanerViewModel

Class names and method structure make reconstruction substantially easier than a heavily obfuscated APK.

## Functional recovery map

### A. Folder and storage scan
Evidence supports:
- filesystem inventory;
- folder statistics;
- selection/review;
- cleanup.

### B. Large files
Evidence supports:
- discovering large files;
- selecting candidates;
- deleting selected files.

### C. Duplicate files
Evidence supports:
- duplicate candidate discovery;
- content digest/hash comparison;
- progressive scanning;
- keeper/delete strategy.

### D. Thumbnail/cache cleanup
Evidence supports:
- thumbnail directory detection;
- thumbnail inventory;
- cleanup with path checks.

### E. Gallery cleaner
Evidence supports:
- MediaStore-backed gallery paging;
- prefetch;
- keep/delete decisions;
- undo;
- system trash integration.

### F. Safe Trash
Evidence supports:
- internal trash concept;
- trash size/state;
- purge/permanent deletion path.

### G. EXIF/privacy
Evidence supports:
- JPEG metadata handling;
- EXIF cleanup;
- cleaned-copy/export workflow.

## Safety strengths worth preserving

- PathSafety exists as a dedicated concern.
- Gallery deletion is MediaStore-aware.
- Undo exists in the gallery flow.
- Safe Trash exists instead of relying only on immediate permanent deletion.
- Duplicate cleanup contains keeper/guardian logic rather than naive delete-all matching.

## Release / productization blockers found

### P0/P1
- Legacy applicationId is provisional: com.aistudio.foldercleaner.zjijip
- versionCode is 1
- supplied APK is debuggable
- supplied APK uses Android debug signing identity
- no persistent production/internal signing identity is established in this repository yet
- Google Play Billing bundled in APK is legacy for a new 2026 store baseline
- AdMob configuration visible in APK uses Google sample/test identity
- MANAGE_EXTERNAL_STORAGE requires explicit product/policy justification and capability design

### P1/P2
- source repository was lost;
- exact original Gradle/dependency graph must be reconstructed;
- code organization must be recovered without blindly reproducing decompiler artifacts;
- automated tests and CI evidence are absent from this new repository;
- localization status must be rebuilt/verified;
- release-mode physical validation must be redone from exact artifacts.

## Recovery rule

Do not treat decompiled code as automatically correct production source.

For each recovered subsystem:
1. document expected behavior;
2. reconstruct clean source;
3. add tests where feasible;
4. compare behavior with the supplied APK;
5. keep only behavior that is safe and product-consistent;
6. record deviations intentionally.

## Evidence boundary

APK analysis can establish:
- package metadata;
- manifest declarations;
- bundled libraries;
- class/method/resource evidence;
- broad architecture and behavior.

APK analysis cannot perfectly recover:
- original comments;
- source formatting;
- all local variable names;
- original Gradle files;
- original commit history;
- developer intent where bytecode is ambiguous.

Those gaps must be reconstructed explicitly rather than invented silently.
