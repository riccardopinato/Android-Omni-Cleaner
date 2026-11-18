# Omni Cleaner premium — UX roadmap A–E (18/11/2026)

## Canonical source and user intent
- Repo: `riccardopinato/Android-Omni-Cleaner`, main restored with PR #34 / `e827297b78c445b57b5f706a5363bad270b45af5`.
- Preserve the premium blue/purple Home, broom logo, five navigation destinations, dedicated tools, Gallery Pro and all SAFE cleaning constraints.
- Core Factory CURRENT baseline: `63c3ecba07dafdf46e27e175bd973ee05107fecb`, Master 27, Golden Index 29. Read once in this task.
- Design-App committed references: `d27e8b8b608d523506b64fdec044adb1f9814ded`.
- Product Bible: safe cleaning first; no invented recovered space; no automatic deletion; no new all-files permissions; no hidden billing expansion.
- Reuse mode for every source listed below: `ADATTATA` / original Kotlin/Compose implementation, NOT copied HTML, CSS, screenshots or third-party identity.

## A — Hierarchy and navigation
- `DCL-002`, `UIX-STYLE-006` (Bento): clean hub now groups the original nine navigable tools into Explore storage / Review & recover / Advanced tools. Separate tools remain separate screens.
- Existing `omni_clean_tool_*` test tags and all nine destinations remain; no tool removed.
- `DCL-004` quiet navigation: secondary screens retain their back action and the five primary tabs remain unchanged.

## B — Gallery/video
- `DCL-006`, `WML-003` (concept only): original-ratio `ContentScale.Fit` preview + new inspection-only edge-to-edge dialog for photo/video.
- Video continues to use the original MediaStore URI and native `VideoView + MediaController` timeline; only one `VideoView` instance is mounted per media item during immersive mode.
- Full-screen controls never call `onTrash`, `onKeep` or delete. Android Trash, explicit confirmation, Undo, batch policy and permissions remain exactly as previously implemented.
- UI strings in EN, IT, ES, FR, PT. Back dismisses the dialog. No transcoding and no extra photo/video permissions.
- **Not claimed:** pinch-zoom photo, OEM codec support, complete physical photo deletion certification, native 4K quality assessment. Any follow-up would be a separate product change.

## C — Status and truth
- `DCL-005/011`: scan activity is displayed only while `state.scanning` is true; current actual operation is shown if provided, else translated fallback; indeterminate progress is not displayed as fake %.
- Existing scan cancellation, error and real overview result remain visible.
- The current Gallery/Privacy/Pro empty-state policies are preserved pending physical review.

## D — Premium polish
- `UIX-STYLE-009`, `DCL-007`: Privacy/Pro selection hubs replace nested blue buttons with calm full-width semantic destination cards and subtle brand-color affordances.
- No new theme framework, SDK, third-party asset, heavy rendering engine or haptic permission. Existing blue/purple premium Home remains byte-identical.
- Uiverse components are NOT wholesale installed; choices `UIV-064` (comic-brutal CTA), `UIV-093` (animated face), `UIV-011` (3D infinite loader) are intentionally `NON ADATTA` due to incompatible tone or performance.

## E — Quality and release evidence
- `android-ci.yml` existing PR-to-main gate verifies locale parity, UX structure, tests, Android test APK, release build and bundle.
- `omni-visual-capture.yml` now runs on main-targeted PR head SHA, capturing 5 primary + 9 detail routes on Android emulator API 35, including optional synthetic video playback and new immersive video screenshot.
- `check-two-level-media.py` includes immersive ownership, all nine Clean routes and five-language keys.
- Screenshots from prior main (14 screens) are before-evidence. New screenshot files must be genuinely captured and reviewed before claiming visual QA PASS.
- Negative or missing evidence: no physical-device Trash/MediaStore/OEM signing or Play-store certification; remain NOT_VERIFIED.
- PR code changes are a product UX refresh only, not release identity changes.

## Acceptance
1. All nine Clean tools launch their corresponding dedicated pages, including filtered scroll at 360dp and 150% text.
2. Gallery viewer opens/closes; video can play; no bitmap crop; selection remains unchanged by fullscreen.
3. Navigation, theme and locale unaffected.
4. Unit tests, Android test APK build, PR screenshots and original Safe Trash flow unchanged.
5. No merge without exact-head green CI and review of actual screenshots; post-merge main tree must equal the tested source.
