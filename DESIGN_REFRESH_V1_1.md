# Visual refresh v1.1 candidate — Design-App adoption

Status: **DRAFT / NOT MERGED / NOT RC1**  
Source base: `Android-Omni-Cleaner@984474d9af62d8b7e17b975a31332161f24f2df7`  
Factory baseline: `App-Factory-Core@c5adc147046beb7024629466d10eeb799c3c2619` (CURRENT Master v27 / Index v29)  
Design reference: `Design-App@f7b7ef9a899ff21206b28877a521bbbc65878fe5`

## Authority and constraints

`PRODUCT_BIBLE.md` and `RELEASE_FREEZE.md` remain authoritative. The current `1.0.0-rc1` is source frozen. This change is a **future v1.1 candidate**; do not merge into RC1 or declare a new release without an explicit release review.

## Design-App sources and reuse mode

- `UIX-STYLE-006 BENTO UI`: modular dashboard and hierarchy. REFERENCE/ADAPT, not a source-code donor.
- `UIX-STYLE-009 MINIMAL CONTROLS`: compact, semantic control family. REFERENCE/ADAPT.
- `UI-UX/DESIGN SYSTEM`: reusable component hierarchy, tokens and accessible states. REFERENCE.
- `packs/icons`: design guidance on semantic icons, localized labels, 48dp targets and avoiding extended icon packs. A **new first-party vector implementation** is used instead of copying the pack or bundling external images.
- Illustrative visual references: `UIR-0025` (design system), `UIR-0006` (bento), `UIR-0034` (navigation). These are inspirations, **not shipped artwork**.

## Implementation in this branch

1. **Bottom navigation** — replace H/C/G/P characters with five original Compose Canvas line icons (Home, Clean, Gallery, Privacy, Pro). Labels remain from the existing localized Android resources.
2. **Storage hero** — a richer restrained blue/purple gradient with original decorative geometry and a real data-bound progress ring. The value remains actual storage used. The ring displays the existing `usedPercent` (0..1), not inferred cleanup savings.
3. **Metrics** — neutral raised-priority statistics with semantic accents and consistent border treatment. Unknown metrics remain `—`, not fake zero.
4. **Quick actions** — a 2×2 bento grid on ordinary-width devices, with horizontal scrolling retained on small screens and at large font scales.
5. **Design tokens** — centralized token anchors and a more disciplined headline rhythm.

No change to storage permissions, file deletion, Safe Trash, Gallery engines, EXIF, billing, advertisements, versionCode or signed release.

## Required verification before merge

- GitHub PR exact-head: locale parity; JVM tests; `assembleDebug`; `assembleDebugAndroidTest`.
- AppLab / screenshot QA on **all five primary destinations**, including Android dark/light, narrow 320dp, large font at least 1.3×, and TalkBack.
- Verify real-storage progress percentages and unknown/loading state on device.
- Verify navigation labels, 48dp tap targets, selected state and screen-reader behavior.
- Review APK size impact and touch interactions on a physical Android device.
- Maintain RC1 source freeze; merge only when targeting a subsequent approved release, with all required gates.

Not yet claimed: visual approval, device validation, performance parity or Store Ready.
