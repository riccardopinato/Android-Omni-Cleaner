# Omni Cleaner — Design-App-first visual pilot (2026-11-19)

**Status:** design candidate / SOURCE_IMPLEMENTED on isolated branch; **NOT aesthetically approved**, no merge to main, no native physical acceptance. Exact Android source baseline: `3b652d57c433d23025869b8daea7e7998e9003a2`. Factory baseline: `6dfde2d6b6a5be6cbc898cea137f87803701814d` (CURRENT Master v27 / Golden Index v29 / Registry v2). Design-App pinned baseline: `068264f82d10bd645ea18b3ae13efa5bb9555734`.

## Product brief
- Audience: Android users inspecting storage, images, video and cleanup candidates before any reversible/system-mediated destructive action.
- Product promise: **safe cleaning first, maximum cleaning second**. Design must never imply deleted bytes before a confirmed deletion or invent scan progress.
- Current tech: Kotlin/Compose Android, local-first; retain architecture, permissions, cleaning logic, playback, EXIF and persisted data.
- Core visual intent: **Quiet Utility** — branded Home retains its approved blue identity, while dedicated task screens are calmer, content-first, restrained and legible; primary color signifies the actual next action.
- Explicit anti-references: fake storage graphs/achievements; indiscriminate gradients, wave décor and giant filled secondary actions; repeated pill/card nesting.

## Priority source mapping: user-curated references FIRST
| Reference | Actual source | Decision | Consumer-specific action |
|---|---|---|---|
| [DCL-002](https://github.com/riccardopinato/Design-App/blob/068264f82d10bd645ea18b3ae13efa5bb9555734/UI-UX/DRIBBBLE_COLLECTUI_APP_MAP_2026-11-18.md) | Design-App mapped to Omni Cleaner | ADATTATA (existing group navigation already implemented) | Simplify task-screen visual hierarchy without redoing the approved functional grouping |
| [DCL-007](https://github.com/riccardopinato/Design-App/blob/068264f82d10bd645ea18b3ae13efa5bb9555734/PER_APP/omni-cleaner.md) | App dossier, secondary-page / utility hierarchy | ADATTATA | Back is a tertiary navigational action; keep Scan / Preview / Confirm as prominent actual task action |
| [DSK-002](https://github.com/riccardopinato/Design-App/blob/068264f82d10bd645ea18b3ae13efa5bb9555734/PER_APP/omni-cleaner.md) | Impeccable, HIGH fit | ADATTATA as review process only | Audit hierarchy, empty/populated, responsive and dark/large-text views; no HTML/CSS library imported |
| [DSK-001](https://github.com/riccardopinato/Design-App/blob/068264f82d10bd645ea18b3ae13efa5bb9555734/PER_APP/omni-cleaner.md) | Anti UI Slop, MEDIUM fit | ADATTATA as critique | Avoid unmotivated multi-stop gradient on information-heavy screens |
| [UIX-STYLE-006](https://github.com/riccardopinato/Design-App/blob/068264f82d10bd645ea18b3ae13efa5bb9555734/PER_APP/omni-cleaner.md) | Bento module grouping | EXISTING / KEEP | Do not implement a second dashboard system |

Existing PR #35 and merged #37 already introduced section navigation, original-aspect media preview, gallery/video experience and button hierarchy. This pilot deliberately does not redo those changes. Third-party icons/HTML/CSS remain REFERENCE_ONLY unless individual code and license are verified.

## Evidence: existing Android native screenshot baseline
Real emulator screenshot artifact, source `56df78c0688f52e2b8160b4e94d17c0368692792`, PR #37 visual workflow `44396585539`, artifact `14470232571`. Relevant captures:
- `01-home.png` / `16-home-dark-large.png`: **preserve** approved Home visual hierarchy and logo; Home should remain unchanged by this pilot.
- `02-clean.png`, `03-gallery.png`, `04-privacy.png`, `05-pro.png`: relatively large gradient header region, task hierarchy worth refining.
- `06-clean-duplicates.png`, `07-clean-filters.png`, `08-privacy-quick.png`, `09-privacy-batch.png`: blue full-width-feeling Back pill competes with actual function CTA; wave crosses header behind text.
- `17-clean-dark-large.png` through `20-pro-dark-large.png`: dark mode + increased font-size contrast and density must remain tested.
- `21-gallery-360dp-large.png`: small screen / large text must stay usable.

This pilot does **not** claim those screenshots are from its own source SHA. AFTER screenshots must be captured from exact pilot head before aesthetic acceptance.

## Narrow SOURCE pilot on separate branch
1. `OmniPremiumHome.kt` — preserve the already approved wave **only on Home**; replace the dramatic wave under task content with a quiet surface gradient for non-home screens. No CPU-heavy animation, asset or dependency added; keeps caching via `drawWithCache`.
2. `RedesignedOmniCleanerApp.kt` — use existing product-owned `OmniTextButton` as tertiary Back affordance on dedicated detail screens, preserving 48 dp height, localization, navigation/back behavior and primary CTA emphasis.

**No other change:** no feature code, storage schema, permissions, database, gallery cropping, playback, loader, storage analysis, cleanup, billing, signing or translations.

## Visual QA and acceptance (IWC-01..08)
- Android CI exact-head: lint, unit, assembleDebug, androidTest APK compile, release build — **NOT VERIFIED until CI completes**.
- Emulator screenshots exact-head, light/dark, 360dp + large text: **NOT VERIFIED until after captures**. Compare with same fixtures from source `56df78...`.
- Reviewer disposition: **NOT_VERIFIED**, not automatically approved.
- Physical device review, TalkBack, rotation/insets, independent font-scale and smoke QA: **PENDING**.
- Validate decorative contrast around headers, dark content readability, minimum 48dp Back hit target and scan CTA prominence.
- Any visual regression in Home, photo/video aspect ratio, safety confirmation or navigation is a **blocker**.
- Measure no significant app performance or APK-size regression; screenshots are not enough for smoothness.
- If reviewer prefers original wave, revert or keep branch as a rejected alternative. No automatic merge.

## Important policy reconciliation
The **current** Master v27 still labels Material 3 *optional/selective* and the historical Omni Product Bible lists Compose Material 3 as its component stack. The user's previous request is to move away from its **stock appearance**. This pilot follows that visual preference while preserving already-working Compose components. Removing every M3 reference from the Factory/Product Bible would be a distinct governance/architecture decision, not a hidden by-product of this visual pilot.

## Separation of concerns
Do not mix with performance PR #38 or #39; both retain independent exact-head CI and physical benchmarking.
