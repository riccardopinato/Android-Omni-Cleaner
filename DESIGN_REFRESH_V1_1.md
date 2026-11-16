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


## Premium Light & Dark implementation — 2026-11-15

Reference: approved generated concept showing light and dark dashboards side by side, with a cobalt/cyan/magenta storage ring, rounded gradient action, four tinted functional tiles, background waves, highlighted navigation, and the rounded-square broom app icon.

### Native implementation

- `OmniPremiumHome.kt`: original responsive Compose implementation, gradient waves, gradient broom mark, dual-theme storage ring and progress, four feature tiles, entry-point CTA, access disclosure and existing Pro upsell.
- `MainActivity.kt`: light/dark override with `rememberSaveable`; system theme remains the initial default.
- `RedesignedOmniCleanerApp.kt`: native navigation and themed background retained on all five destinations; the four Home tiles lead to the current working screens.
- Launcher resources: gradient background, broom foreground, monochrome vector, pre-26 fallback and 26+ adaptive icon.
- Supported locale text updated without changing resource key count.
- Actual storage metrics come from the existing `storageInfo` source. No mockup values, inferred freeable bytes, fake scans, deletion-on-tap or new permissions are introduced.

### Scope and fidelity limitations

This is a **native mobile interpretation** of a concept image that depicts desktop/sidebar layout. Small Android screens retain a bottom navigation, preventing the sidebar from shrinking the content. Lighting, glow, hand-painted 3D icon effects and shadow softness will vary by Android GPU/density; vector artwork is original and not a pixel-by-pixel bitmap replica. Physical screenshot comparison is required before any claim of exact visual parity.

### Release gates

- GitHub CI: locale parity + unit tests + debug + Android test compilation on exact head.
- Visual comparison: light and dark / 320dp, 360dp, 412dp / 1.3× and 1.6× font scale / TalkBack focus order.
- Confirm real storage-percent data, unknown-data fallback, permission prompt, and the Analyze/Review flow.
- Confirm icon round/squircle masks and monochrome theme icon on an Android 13+ launcher.
- Verify contrast and hit targets, image/gradient performance, memory, and APK size.
- PR remains **DRAFT**. RC1 main/release freeze remains unchanged.

### Final approved launcher artwork: raster asset, not approximation

- User-approved image: *glossy cyan-to-blue-to-purple rounded-square broom icon*, star and curved bristles preserved. This approval supersedes previous first-party vector mockup illustrations.
- Source artwork rendered at 1254×1254, packaged as a lightweight **192×192 WebP** with alpha rounded-square mask (source-backed resampling, not a regenerated drawing). File: `app/src/main/res/drawable-nodpi/omni_approved_icon.webp`. Git object SHA: `0bfe3ae03629d19d273cfc4b5aa6ace522121b3a`.
- Launcher pre-26 points to the packaged art; adaptive icon foreground uses a safe-zone inset over the existing gradient background; themed monochrome fallback remains available.
- In-app premium branding uses the same raster approved art, while the standalone illustrative broom tile remains vector.
- Visual QA required: actual device icon masks and contrast, independent of CI; the source PNG itself is not stored in Git to avoid large APK/resource impact.
- RC1 source freeze unchanged: this PR remains a **future v1.1 candidate**, not a signed release.
