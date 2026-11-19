# Omni Cleaner — WML-003 Video Scrub Slider · Professional Minimal

Approval: 2026-11-20. Status: **source implemented on feature branch; exact-head CI and visual validation pending**.

## Provenance and scope
- Design-App `PER_APP/omni-cleaner.md`: WML-003 Scrub Slider (base), Watermelon UI.
- Original source fixed at `WatermelonCorp/watermelon-platform` commit `9d6defcf89880d0a00fbccad773c046bb51d7afb`, `src/data/contents/animated-components/scrub-slider/base.tsx`, exact blob `50dd8724adec082588cb4970463a37e65702f2d2`.
- The original source is a general-purpose React/Motion discrete slider with floating numeric display, spring motion and repeated ticks. **These effects are intentionally omitted** as they conflict with the approved Professional Minimal identity. Only the operational interaction, scrub position mapping and tactile timeline concept are adopted.
- This is a Kotlin/Jetpack Compose native implementation; no React, Framer Motion, third-party media runtime or new external package.

## Real player integration
- Existing `GalleryVideoPlayer` retains `VideoView`, `MediaStore` URI, lifecycle owner, thumbnail, error state, real video decoding, and the user tap to opt into playback.
- Replaces the old overlaid system `MediaController` with the `OmniVideoScrubControls` composed *outside* the video rendering box to avoid obscuring photos or video. The container does not crop; the native video engine preserves aspect ratio.
- `setOnPreparedListener` obtains real duration; the player polls actual `currentPosition` at 250ms while active and exposes it as a timestamp and progress.
- Dragging the timeline previews the chosen time but applies `VideoView.seekTo()` only when drag finishes. Clamp endpoints to real duration; no seeking before prepared. Ten-second backward/forward and explicit play/pause act on the same VideoView instance.
- `setOnCompletionListener` resets playing state and marks end position. Lifecycle stop/pause pauses playback; disposing closes the VideoView playback. Re-entry creates a fresh instance, preventing competing players.
- Accessibility: controls have localized labels EN/IT/ES/FR/PT and stable test tags. Duration 0 is non-seekable. One neutral line and one subtle blue accent, no decorative spring, gradient, glow or animated numeral.

## No deletion risk
- No changes to `GalleryController`, Keep, Trash, Android confirmation, file permissions, storage engine, scan or media reads/writes.
- No conversion, transcoding or modification of video file.
- The Gallery Pro size range and its filters remain independent.

## QA gates
- [ ] Exact head Android CI (compile, unit tests, lint, release, AAB) PASS.
- [ ] Exact head screenshot workflow PASS.
- [ ] Dedicated runtime visual test on real video: seek to zero/center/end, variable duration, pause/resume, 10s jump, completion, errors, app background and immersive/player switch.
- [ ] TalkBack, large font, landscape, dark theme and small screen.
- [ ] Confirm video rendering preserves aspect ratio and no duplicate audio stream; device/OEM codec behavior remains unverified until physical QA.
- [ ] Merge only once CI and screenshots are green; release certification is separate.
