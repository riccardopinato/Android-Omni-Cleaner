# MAXI STEP 7 — Product Expansion

Factory baseline: `a4a97ca79e85d2faebcdcc9a3ae4a9644f5caf03`  
App baseline entering Step 7: `1115a1b5e1e62d5a5d8fb0fa0562ccd25de67407`

## Goal

Expand Android Omni Cleaner with useful, truthful storage-review capabilities without adding fake RAM/CPU/battery/antivirus claims, background deletion, or broader permissions.

## REUSE-FIRST review

Patterns were reviewed from five open-source Android storage/media projects:

1. `d4rken-org/sdmaid-se` — mature maintenance categories, corpse/app residue analysis, storage analysis, deduplication and swipe review. GPL-3.0: product patterns only, no code copied.
2. `MZarc/Clearo` — categorized storage (APKs, Downloads, archives, media) and local-first duplicate/media analysis. MIT: evaluated as a pattern reference; no code copied.
3. `EngShabban/DedupeX` — MediaStore-first truthful storage intelligence, screenshots and exact duplicate detection. Apache-2.0: evaluated as a pattern reference; no code copied.
4. `Mo-Array/Nothing-Files` — old downloads, categorized storage and safe trash patterns. GPL-3.0: product patterns only, no code copied.
5. `loopotto/CleanSweep` — media-review and similar-media direction. GPL-3.0: product patterns only, no code copied.

The Step 7 implementation is original code built on Omni Cleaner's existing storage and cleanup contracts.

## Selected Step 7A scope — Smart Categories

One full-device inventory pass classifies unique review candidates into overlapping labels:

- **Old Downloads** — top-level Download/Downloads files at least 90 days old;
- **Installers** — APK/APKS/XAPK/APKM;
- **Archives** — ZIP/RAR/7Z/TAR/GZ/TGZ/BZ2/XZ;
- **Screenshots** — screenshot folder or conservative filename-prefix signals.

A file can carry multiple labels but exists only once in the candidate inventory.

## Safety contract

- no automatic selection;
- no automatic deletion;
- no new permission;
- Deep Cleaning capability gate remains authoritative;
- candidate file size + last-modified values are bound to the scan snapshot;
- every selected file is revalidated immediately before mutation;
- media uses Android system trash where supported;
- small non-media files use verified Safe Trash where supported;
- unsupported irreversible cleanup remains blocked;
- moving to trash is not reported as already reclaimed bytes.

## Explicit deferrals

### Similar-photo / perceptual duplicate detection

Potentially useful, but deferred to MAXI STEP 9 because it adds CPU/memory cost, caching questions and a new false-positive surface. It should be benchmarked before productization.

### Shizuku/root access

Not required for the current product promise and would materially expand permission/support complexity.

### Scheduled/background cleanup

Rejected for now. Review-before-delete remains a core product invariant.

## Version

Step 7 development version:
- `0.8.0-recovery`
- versionCode `8`

## Validation gate

Step 7 is not COMPLETE until all applicable evidence is green:

1. focused JVM classification tests;
2. Android CI;
3. AppLab Intelligence;
4. bounded AppLab runtime + Maestro;
5. visual journey PASS/WARN with no high-confidence visual failure;
6. exact-head Step 7 APK prerelease + SHA-256 sidecar.

Physical filesystem/OEM behavior remains a separate release-certification gate and must not be inferred from emulator evidence.
