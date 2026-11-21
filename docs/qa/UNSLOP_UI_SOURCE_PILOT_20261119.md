# Omni Cleaner — Unslop UI native pilot (19/11/2026)

**Status:** LIMITED_SOURCE_FIX_CANDIDATE / EXACT_HEAD_CI_PENDING / VISUAL_NOT_VERIFIED / PHYSICAL_NOT_VERIFIED.  
**Input:** `riccardopinato/Android-Omni-Cleaner@3b652d57c433d23025869b8daea7e7998e9003a2` (`main` on intake).  
**Source library:** Design-App DSK-011, Unslop UI MIT pinned `cd891160453f26aaacc569bad96a8d10d595076d`; Core bridge U1–U20 → AT-01…64 (candidate).  
**Product authority:** `PRODUCT_BIBLE.md`, `ROADMAP.md`, `AGENTS.md`, `RELEASE_FREEZE.md`; this is Kotlin/Jetpack Compose, not Flutter. Consumer does not depend on Python or the upstream Web runtime.

## Source findings and bounded correction

| Source finding | Rule → AT | Decision |
|---|---|---|
| Two ordinary Clean hubs in `ui/redesign/CleanerScreens.kt` render an uppercase `Deep Cleaning` eyebrow above the `Clean` title, although deep access is **optional**, not guaranteed | U1 → AT-18/27/40, U6/U9 state-truth lens → AT-45 | **REMOVE_CANDIDATE implemented on isolated PR**: pass `eyebrow = null` in precisely these two Clean hub call sites |
| `OmniSectionHeader` in `DesignComponents.kt` forced an eyebrow | U1, U3 → AT-40 | Change `eyebrow` to nullable and render its `Text` only when nonblank; all other callers retain their provided label. Title/subtitle unchanged |
| Text describing reversible / Android system-mediated deletion | U3/U4 lens | **KEEP_PURPOSEFUL**: safety instructions, not botsplaining; preserve translations, review-before-delete and errors |
| `OmniHeroCard` blue-purple gradient, circular decorative artwork | U17 → AT-01,05,12 | **KEEP_PENDING_VISUAL**: Home identity/user-approved artwork; PR #40 already owns cosmetic changes on separate files, no global replacement |
| `OmniStatusPill` status dot and `OmniMetricCard` colored dot | U10/U11 → AT-09,45,49 | **NEEDS_RUNTIME_EVIDENCE**: verify call-site state binding and screen before changing |
| Existing `OmniModuleCard` glyph + arrow affordance | U11 | **KEEP_PENDING_VISUAL**: route affordance may be meaningful, no mechanical removal |

## Isolation and safety

The current aesthetic [PR #40](https://github.com/riccardopinato/Android-Omni-Cleaner/pull/40) changes `OmniPremiumHome.kt`, `RedesignedOmniCleanerApp.kt` and its own doc. This pilot modifies **only** `DesignComponents.kt` and `CleanerScreens.kt` for U1, without touching #40's files. RC1 frozen release artifacts, gallery/video, Trash/undo, scanning, permissions, purchases, localization resource files and feature routing are unchanged. It does not change the meaning of the `Deep Cleaning` permission guidance.

## Evidence gates

- Required: inspect exact-head diff; execute Kotlin/Compose compilation, JVM tests, lint and Android CI in PR. Code change stays unmerged if required checks fail.
- Capture actual Compose Clean hub and Clean detail (light/dark, 360dp, 150–200% text; permission granted/denied) before/after using identical fixtures. No screenshot capture or visual reviewer PASS is claimed by source change.
- Assert other `OmniSectionHeader` callers still render correct label (nullable change only when caller explicitly passes null).
- Source check alone is not Android physical certification, release approval or store readiness.
- Classify follow-up visual findings and false positives before broader U17 "surface" cleanup.

## Promotion boundary

This is a first candidate consumer intervention; Anna's Diary is sampled separately. One changed source file or CI green does not certify a Golden or the wider Native Unslop process.
