# Omni Cleaner — user-supplied Gentleface animated Check

Date: 2026-11-20. Owner supplied an explicit animated SVG as the preferred version of proposal 8, replacing the prior static ICO-014 illustration.

## Original supplied SVG
- Native viewBox 0 0 16 16.
- Stroke: currentColor, 1.001, round linecaps and joins, no fill.
- Path: `M2.5 8.5L5.5 11.5L12.5 4.5`.
- Animation: 2200ms repeating loop. Dash stroke retracts and retraces with a short hidden interval, plus slight extension of the line endpoint `12.5 4.5 -> 12.9 4.1 -> 12.5 4.5`.
- The SVG source was provided directly by the owner in the conversation. See also Design-App ICO-014 (original still static): `BIBLIOTECA_COMPONENTI/sorgenti/gentleface/ICO-014.svg`. No external runtime or license copy is needed to use the owner-supplied animation reference.

## Native implementation
- `OmniGentlefaceAnimatedCheck.kt`: Compose Canvas and PathMeasure with native 2.2s stroke reveal/erase plus tip motion. Avoids adding a WebView, SVG runtime or a network dependency.
- The button for `GalleryDecisionButtonLabel("✓", keepLabel)` becomes `GalleryKeepButtonLabel(keepLabel)`, where the check is animated and the same localized title, semantics, KeepGreen fill, controller callback, 170ms commit timing and swipe direction remain.
- The Keep feedback badge also uses the icon as a **static** check, appropriate for its very short appearance.
- When Android animator scale disables animations, the check renders fully drawn, without running its animation.
- Existing Trash/Undo semantics and immersive media remain unaffected.

## Visual and safety acceptance
- [ ] Exact-head Android CI PASS.
- [ ] Exact-head screenshot workflow PASS (CI screenshots are not equivalent to a reviewed Keep action on a physical phone).
- [ ] Visually test full 2.2s loop and disabled animation, varying font sizes and dark theme.
- [ ] Verify Keep / Trash / Undo and swipe timing unchanged with real media.
- [ ] Physical Android test before release.

Do not merge with failing/incomplete checks, and do not claim media-deletion certification from UI checks.
