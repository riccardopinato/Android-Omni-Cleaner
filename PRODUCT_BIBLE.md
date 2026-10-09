# Android Omni Cleaner — Product Bible

Status: RECOVERY BASELINE / NOT STORE READY
Date: 2026-10-09

## Product problem

Android storage becomes difficult to understand and clean safely. Generic cleaners often overclaim savings, request broad permissions without context, or make destructive actions hard to review.

## Product promise

Help the user reclaim storage while making every destructive action inspectable, reversible where practical, and truthful about what was actually scanned or removed.

Core principle:

SAFE CLEANING FIRST, MAXIMUM CLEANING SECOND.

## Target

Android users who need:
- storage overview;
- large-file cleanup;
- duplicate cleanup;
- gallery cleanup;
- residual/thumbnails cleanup;
- privacy-oriented photo metadata cleanup.

## Product pillars

1. Storage visibility
2. Safe review before deletion
3. Reversible cleanup where possible
4. MediaStore-aware operations
5. Clear permission/capability health
6. No fake "GB cleaned" or "optimized" claims
7. Local-first operation for core cleaning

## Recovered functional areas from supplied APK

### Storage
- folder inventory / scanning;
- large files;
- storage overview;
- app residues / folder candidates;
- thumbnail/cache cleanup.

### Smart Clean
- duplicate detection;
- progressive duplicate scan;
- keep/delete strategies;
- content digest support.

### Photos
- gallery cursor / paging;
- swipe-based keep/delete flow;
- undo;
- MediaStore trash operations;
- gallery preferences.

### Safe deletion
- Safe Trash manager;
- path safety checks;
- guarded cleanup;
- explicit permanent deletion paths.

### Privacy
- JPEG EXIF parsing/editing;
- folder EXIF cleaner;
- cleaned-copy/export flow.

### Monetization infrastructure already visible in APK
- AdMob integration;
- Google Play Billing;
- Pro preferences.

These monetization pieces are prototype evidence only and must be rebuilt against current provider requirements.

## Explicit exclusions for recovery phase

Do not add during forensic reconstruction:
- RAM booster claims;
- CPU cooler claims;
- battery optimizer claims;
- antivirus claims;
- automatic background deletion;
- AI-generated cleanup decisions;
- cloud dependency for basic cleanup.

## Platform

Android native only for the recovered product baseline.

Stack:
- Kotlin
- Jetpack Compose
- Material 3
- Android MediaStore / Storage APIs

A Web Preview is not considered a truthful functional certification target for filesystem/media cleanup.

## Identity

Repository: riccardopinato/Android-Omni-Cleaner

Final app name: TBD
Final applicationId: TBD

Legacy APK identity, for forensic reference only:
- applicationId: com.aistudio.foldercleaner.zjijip
- versionName: 1.0
- versionCode: 1

The legacy identity must not be reused blindly.

## Monetization direction

Candidate:
- Free: useful cleaning core + non-invasive ads
- Premium/Lifetime: no ads + premium workflows/limits to be defined

Rule:
Premium / No Ads means no ad request, not "load then hide".

## Permission philosophy

Just-in-time permissions only.

MANAGE_EXTERNAL_STORAGE, if retained, must be isolated to functionality that materially needs all-files access and must never be treated as equivalent to feature health.

Permission granted != capability healthy != cleanup verified.

## Release truth

The supplied APK is recovery evidence only.

It is not a production release baseline because the forensic audit found prototype/release issues including debug signing and debuggable build state.
