# Omni Cleaner — Impeccable home/navigation pilot

Date: 2026-11-18
Source main baseline: `bc1b9227dfcd1636ec53c061c8eafa72614ff749`.
Factory baseline: `d660cccd57f2e252c573c484457f1ae9234b1e33` (Master v27 / Golden Index v29).
Design-App reference: DSK-002 / Impeccable at commit `ae190d5bed59a37bebf7189d7a26b6647a797440`.

## Critique and implementation
Before: Home metrics and four quick actions require horizontal scrolling, hiding actions offscreen. Bottom nav uses letter glyphs.
After: storage remains hero; summary strip shows 3 existing metrics (unscanned is —); four actions form 2 columns, or 1 on narrow content widths; seven local vector assets replace glyphs. New strings localized EN/IT/ES/FR/PT.
Scope isolated to Compose UI, drawables, locale resources, and vector availability instrumentation assertion. Existing scan, permission, trash, billing, and EXIF code is untouched.

## Harden and polish acceptance
1. Verify exact-head PR JVM/Android compile, locale parity, debug APK, test APK.
2. Capture consistent before/after screenshots on emulator for Home, Clean, Gallery, Privacy and Pro, matching device/locale/theme/fontScale.
3. Confirm no clipped actions, no horizontal overflow, readable text at large font scale, TalkBack labels and focus.
4. Verify permission disclosures and file cleanup review are unchanged; no false success indications.
5. Record source and screenshot hashes plus visual reviewer disposition. Green compilation alone does not certify visual quality.
6. Signed release, physical storage cleanup and Play/All-files-permission checks remain independent gates.

Visual runtime status: NOT VERIFIED. Web HTML/CSS detectors do not certify native Android.
