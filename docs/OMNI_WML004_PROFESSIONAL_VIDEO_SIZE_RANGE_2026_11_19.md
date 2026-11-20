# Omni Cleaner — WML-004 Professional Minimal video-size range

Date: 2026-11-19. Status: **user-approved; implemented on feature branch; build, visual QA and main merge require exact-head evidence**.

## Decision and original source
- Design-App: `WML-004`, Watermelon **Range Selection Slider**; the source file was inspected at commit `9d6defcf89880d0a00fbccad773c046bb51d7afb`: `src/data/contents/animated-components/range-selection-slider/base.tsx`, blob `c9d73a57572b4c6928f27ce9e2ed2b60e8edc67f`.
- Source effect: two handles, selected interval, live numeric values and Apply/Cancel pattern. **This is a visual/product reference**. Android Omni Cleaner is Kotlin/Compose, therefore no React, Framer Motion or Flutter packages have been imported.
- The user rejected an overly decorated 'AI-style' concept and **explicitly approved Professional Minimal**. No gradients, glossy cards, glassmorphism, colorful badges, animated numerical wheels or decorative illustration.

## Native design
- Integrate directly into the existing Gallery Pro video-inspection section (no nested card).
- Neutral surface, hairline dividers, type hierarchy, precise numeric min/max readouts, one blue accent on the two-thumb track, neutral high-contrast apply action.
- One adjustable two-thumb `RangeSlider`; Android native thumb gestures, keyboard/accessibility behavior, no extra web/rendering runtime.
- Labels and actions localized in EN/IT/ES/FR/PT. Theme-sensitive surfaces/text for light/dark.
- `RangeSlider` range defaults to 0–5 GiB, extended to include actual videos currently loaded. If any video exceeds 1 TiB, slider maximum is capped to 1 TiB and the top edge remains unbounded; user-visible '5.0 GB+' (or appropriate scope maximum) denotes this.
- Slider labels/committed values both rounded to 1 MiB; all comparisons are inclusive.

## Real functional behavior
- Draft range does **not** filter until 'Apply filter'. Reset restores the full current-page range.
- `GalleryVideoSizeRange` applies to the same real MediaStore metadata as pre-existing All/100MB+/5min+/4K presets and text search. Filters combine with logical AND.
- `totalMatching`, top 20 visible suggestions and `bytesMatching` all reflect the same filtered set; no estimated recoverable-storage claim.
- Scope remains **only the loaded media page**, never the entire device. Changing the loaded video inventory resets stale custom bounds.
- All Gallery review/Keep/Trash/Android system confirmation and photo similarity code remain separate and unchanged.

## Acceptance and verification
- [ ] Android CI exact-head build and JVM unit tests.
- [ ] Automated screenshot capture PASS; review screenshot of this exact Gallery Pro panel on actual media fixtures independently.
- [ ] Emulated/touch test of two independent thumbs, Apply, Reset, filters AND, search, zero results.
- [ ] Accessibility TalkBack and font scale 1.3+, landscape, light/dark mode.
- [ ] Verify empty page, partial media permissions, >5 GiB, >1 TiB and 0-byte size metadata.
- [ ] Physical device APK validation prior to release.

Do not describe a passing build as complete visual/device certification. Merge requires exact-head green checks and no regressions.
