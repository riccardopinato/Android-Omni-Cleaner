# MAXI STEP 6 — UI/UX Redesign

Status: **IN PROGRESS — VALIDATION PENDING**

## Objective

Replace the recovery-era single long screen with a consumer-oriented shell while
keeping every recovered engine independent from presentation.

## Information architecture

### Home
- storage hero with used/free capacity;
- quick metrics for large files, duplicate groups and scanned folders;
- quick actions for storage, duplicates, Gallery and EXIF;
- capability-health card for Standard / Deep Cleaning;
- Premium callout.

### Clean
- full-device scan actions;
- search/filter thresholds;
- storage overview and findings;
- cleanup review;
- Safe Trash;
- advanced thumbnail/residue maintenance.

### Gallery
Focused Gallery Cleaner surface.

### Privacy
Focused EXIF safe-copy surface.

### Pro
Premium ownership, purchase/restore, privacy options, ads state and Pro batch tools.

## Visual language

- fixed brand palette: blue / purple / mint with orange and rose utility accents;
- soft neutral background;
- rounded 22–30 dp surfaces;
- stronger headline hierarchy;
- horizontal metric / quick-action cards;
- persistent bottom navigation;
- dedicated dark palette;
- no dynamic-color dependency;
- no heavy icon/navigation dependency.

## Architecture boundary

Step 6 changes presentation only. Scanner, cleanup, Safe Trash, Gallery, EXIF,
Billing, entitlement and Ads engines remain reusable and independent.

The design tokens and shell can be changed later when the final visual direction
is supplied, without rewriting those engines.

## Evidence

- primary Maestro: `.maestro/ui-redesign.yaml`;
- critical recovery parity journey remains retained;
- AppLab visual manifest: `.maestro/applab-journey.json`;
- checkpoints: home, clean, gallery, privacy, pro;
- AppLab v4.5 static + runtime required;
- Android CI required.

Visual PASS does not imply physical storage, SAF or Billing validation.
