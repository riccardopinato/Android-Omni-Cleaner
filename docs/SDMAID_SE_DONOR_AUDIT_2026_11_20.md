# SD Maid SE v2.2.3-rc0 → Android Omni Cleaner: clean-room donor audit

Date: 2026-11-20. Donor: [SD Maid 2/SE](https://github.com/d4rken-org/sdmaid-se) release [v2.2.3-rc0](https://github.com/d4rken-org/sdmaid-se/releases/tag/v2.2.3-rc0), released 2026-11-19; release commit `1cc83e8e1132b851b667d6105302ca512d177afd`. Target: `riccardopinato/Android-Omni-Cleaner`, audited against the Color Bento 2.0 feature branch `design/omni-linktree-home-compose-v1-20261120`, head `c226c4ebfc4287909e3e1133738a47b409d2c5b2`. The target branch is **not changed** by this audit.

## Licensing and separation

- Donor code is **GPL-3.0**. The donor README specifically excludes icons/logos/mascots/marketing assets, animations/videos, documentation, store screenshots, texts, and translations from its code-license grant. Do not copy those assets.
- **Do not paste or incorporate donor GPL Kotlin source into Omni Cleaner's current monetized distribution** unless electing the GPL-compliant distribution route and meeting the corresponding obligations. Commercial distribution is possible under GPL; closed-source distribution of a combined derivative is the problem. Seek legal review before distribution.
- This audit contains **original technical findings and implementation specifications only**, no donor source code or verbatim algorithms. All donor references are hyperlinks to the immutable release tag.
- Prefer independent implementation with clean-room tests and attribution to the public conceptual inspiration; avoid code translation of donor source line-for-line.

## Source-backed donor map

| Priority | Module and donor file (immutable release) | What it actually does | Independent Omni Cleaner target |
| --- | --- | --- | --- |
| P0 | [DataAreaManager]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-common-data/src/main/java/eu/darken/sdmse/common/areas/DataAreaManager.kt) and [APathGateway]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-common-io/src/main/java/eu/darken/sdmse/common/files/APathGateway.kt) | Discovers typed storage areas and abstracts multiple path/gateway operations | A `ScanSource` interface for primary shared storage, other accessible volumes, MediaStore and user-consented SAF trees; report coverage per source |
| P0 | [EscalatingWalker]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-common-io/src/main/java/eu/darken/sdmse/common/files/local/EscalatingWalker.kt) | Traversal attempts normal file access, optionally escalates to ADB/root and guards cancellation/symlink cycles | Independent bounded walker with explicit inaccessible reasons. Optional Shizuku/ADB backend only after separate policy review and informed user authorization |
| P0 | [SystemCrawler]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-tool-systemcleaner/src/main/java/eu/darken/sdmse/systemcleaner/core/SystemCrawler.kt) | Scan locations determined by enabled filters; exclusions; cancellable Flow; bounded concurrent matching; grouped results | Reuse Omni inventory to feed independent, tested read-only classifiers with guaranteed exclusions and stage-labelled progress |
| P0 | [PkgRepo]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-common-pkgs/src/main/java/eu/darken/sdmse/common/pkgs/PkgRepo.kt) and [CorpseFinder]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-tool-corpsefinder/src/main/java/eu/darken/sdmse/corpsefinder/core/CorpseFinder.kt) | Multiple package sources, ownership and exclusions, multiple user profiles and locations | Do not classify an app as uninstalled based only on missing PackageManager visibility. Require reliable package inventory, account for archived apps and shared ownership, otherwise mark inconclusive |
| P1 | [DuplicatesScanner]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-tool-deduplicator/src/main/java/eu/darken/sdmse/deduplicator/core/scanner/DuplicatesScanner.kt) | Checksum, perceptual image-hash and media-based similarity detection; reconciles overlaps | Keep existing **exact** duplicate engine intact; add separately labeled **similar photos** and optional audio similarity modules, with preview-only defaults |
| P1 | [DuplicatesArbiter]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-tool-deduplicator/src/main/java/eu/darken/sdmse/deduplicator/core/arbiter/DuplicatesArbiter.kt) | Chooses a preferred keeper using criteria such as path, type, size, modification and media providers | Add transparent keeper-choice reasons; no unattended destructive choices; keep existing byte-for-byte revalidation at deletion time |
| P1 | [AppScanner]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-tool-appcleaner/src/main/java/eu/darken/sdmse/appcleaner/core/scanner/AppScanner.kt) and [stock filters]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-tool-appcleaner/src/main/java/eu/darken/sdmse/appcleaner/core/forensics/filter) | Package-aware cache/junk matching, per-app exclusions, accessibility/ADB/root-dependent inaccessible cache flows, OEM recipes | Consent-aware app-cache discovery; clearly distinguish readable cache files, OS-reported cache and protected/inaccessible cache; any deletion needs review and compatibility testing |
| P1 | [Stock SystemCleaner filters]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-tool-systemcleaner/src/main/java/eu/darken/sdmse/systemcleaner/core/filter/stock) and [TrashedFilter]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-tool-systemcleaner/src/main/java/eu/darken/sdmse/systemcleaner/core/filter/stock/TrashedFilter.kt) | Specific configurable classifications including temporary files, logs, thumbnails, APKs and trash naming | Original allowlisted rules with age/location guards; never sweep all logs, APKs, screenshots or system trash on a name match alone |
| P2 | [StorageScanner]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-tool-analyzer/src/main/java/eu/darken/sdmse/analyzer/core/storage/StorageScanner.kt) | Separately accounts for app/media/system/other-user usage and computes residual conservatively | Display `accessible scanned`, `OS-reported occupied`, `unattributed/restricted` rather than claiming to have found all device bytes |
| P2 | [AutomationService]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-common-automation/src/main/java/eu/darken/sdmse/automation/core/AutomationService.kt) and [OEM recipes]( https://github.com/d4rken-org/sdmaid-se/blob/v2.2.3-rc0/app-tool-appcleaner/src/main/java/eu/darken/sdmse/appcleaner/core/automation/specs) | Optional accessibility-assisted cache management per device UI | Evaluate only after independent legal/Play policy review, accessibility consent and OEM regression suite. No unattended cross-app UI tapping by default |

## Actual current Omni Cleaner: preserve / fix

### Strengths that must not regress

- `FileInventoryScanner.kt`: cancellable IO traversal, canonical-path/symlink protection, reported unreadable/excluded counts.
- `FileInventoryScanner.duplicatesProgressive()`: size buckets → sample fingerprint → full SHA-256 → byte comparison, revision checks, at most four bounded hash workers; persistent hash-cache reuse.
- `StorageInventoryRepository.kt`: shared in-process inventory, persisted index, invalidation and freshness state.
- `BentoScanRuntime.kt`: user-initiated FIFO scanner with foreground notification and resume of queued scan IDs; no background deletion.
- `StorageCleanupEngine`, `SafeTrashManager` and `CleanupSnapshot`: independent safeguards and revalidation before moving/removing files.

### Concrete gaps confirmed by source

1. `StorageAccess.primaryRoot()` uses the primary shared-storage path only. Neither a comprehensive multi-volume walker nor a unified MediaStore/SAF content-provider enumeration is the current inventory source. This limits USB/SD/SAF coverage.
2. `AppResidueScanner.kt` examines package-like directories under `Android/media` and the OS-visible package list. It reports possible visibility restrictions. It does not reproduce donor's multi-area / multi-user / archived-app ownership safeguards. **Do not expand auto-delete from its results.**
3. `SmartCategoryScanner.kt` uses a small conservative category set. Extensible rule packs, explicit folder ownership, exclusion scopes and validated OEM-specific caches are missing.
4. `BentoScanRuntime.kt` uses polling intervals (400 ms and 850 ms) to observe scan completion and update notifications. Prefer a genuinely observable scan status stream and coalesced progress UI to reduce unnecessary wakeups and perceived hangs.
5. `StorageIntelligence.kt` is read-only and reuses the inventory; it should also explain that missing inaccessible/other-volume bytes are **unknown**, not zero.
6. Deep scanner currently gates on broad-access capability. Separate capability checks by source: file walk, MediaStore, SAF and enhanced optional mode. Do not make unrelated permitted scans depend on all-files permission unnecessarily.

## Recommended delivery sequence

### Stage 1: P0 **Coverage Foundation** (first PR)

Create an independently authored `ScanSource`/ `ScanSourceResult` abstraction. Source IDs and capabilities must be explicit: `PRIMARY_FILES`, `MEDIASTORE`, `SAF_TREE`, `SECONDARY_VOLUME` and later `ELEVATED`. Deduplicate physical file identity conservatively across sources. Record `accessibleFiles`, `bytesExamined`, `skippedByRule`, `unreadablePaths`, `unsupportedSources`, `sourceErrors`, `elapsedMs`, `permissionState` and `cancelled`. Emit a deterministic status per source, not a fabricated completeness percentage. First deliver read-only evidence and integration tests.

### Stage 2: P0 **Accurate App Residue & Safe System Rules**

Conservative app ownership resolver across permitted package sources and archived/disabled/user-profile ambiguity. Introduce `UNCERTAIN` classifications and never auto-delete them. Use rule IDs, reasons, confidence, origin, exclusions, preview and explicit user selection; adapt known useful categories such as stale APKs, user-approved temp folders, empty directories, regenerable thumbnails and old backup candidates. Ensure no accidental deletion of current WhatsApp backups or Android's own trash. On new detection, changes to destructive paths must pass snapshot revalidation and safe-trash tests.

### Stage 3: P1 **Photo Similarity / Scanner Latency**

Keep existing byte-exact duplicate pipeline. Independently develop a media-specific similarity pipeline (decode bounded thumbnails; perceptual fingerprint; configurable threshold; cluster; preview, retain originals by default). This finds visually similar but not byte-identical photos, which must be labeled separately. Add no automatic deletions. Drive UI with state streams, throttle progress updates and preserve foreground scan limits.

### Stage 4: Optional enhanced access

Assess a user-configured Shizuku/ADB capability for side-loaded / compliant builds. Never infer root/ADB access or attempt privileged operations silently. Test Play policy eligibility and Accessibility declarations before shipping. Performance and completeness claims must be backed by physical device results.

## Regression and acceptance gates

- Controlled fixture dataset: exact duplicates, almost-identical images, same-size different-content files, empty directories, protected documents, archives, active app caches, fake package-like directories, Android trash, WhatsApp current and historic backups, archived apps, inaccessible subtrees, symlinks, mounted SD/USB and SAF grants/revocations.
- Matrix: Android 10, 11, 13, 14, 15, 16 plus OEM scenarios where supported, low-memory 2–4GB device, real devices versus emulator. Never assume the platform grants forbidden `Android/data` access.
- Coverage: report accessible sources, skipped/unreadable counts, false negatives on known-accessible fixture files, zero unattended destructive actions, and no duplicate physical files across sources.
- Fluidity: compare baseline and candidate **time to first real result**, full scan latency, UI jank and frozen frames, P95 main-thread execution, peak heap, GC, battery/thermal and restart/cancel correctness. Avoid guaranteeing universal speedups before measurements.
- File safety: preview-only by default, preserve user choices and exclusions, verify source identity and keeper existence immediately before deletion, use Android-managed deletion consent when necessary, no automatic deletion of media trash or backups.
- Deliver separately signed APK after CI, instrumented tests, emulator captures, then physical-device acceptance. Do not merge to main without passing those gates.

## Notes about latest donor release (2026-11-19)

The v2.2.3-rc0 release specifically includes more cache/log patterns, warnings/fixes for WhatsApp backups, keeping images in Android trash, archived/uninstalled app protection, Android 17 Motorola cache-handling, slower-phone startup freezes, accessibility/ADB failure handling and very large photo preview crashes. These are **regression scenarios**, not source files to copy.

## Public policy constraints

- [Google Play: All files access](https://support.google.com/googleplay/android-developer/answer/10467955): permission requires core-function necessity, alternatives assessment and declaration/approval; user granting permissions does not automatically mean Play approval.
- [SD Maid SE setup FAQ](https://sdmse.darken.eu/setup): package visibility can make attribution unsafe; limited-package cases cause SD Maid to disable app-related tools rather than risk users' files.
- [SD Maid SE feature FAQ](https://sdmse.darken.eu/faq): Shizuku/ADB, root and accessibility are optional capability alternatives with device/OS limitations.

**Current state:** Research and actionable blueprint completed. NO donor GPL source imported, NO runtime scanner changes, NO APK created by this document-only PR.
