# Omni Cleaner — animated media icons (approved 19 November 2026)

Status: **USER APPROVED; BRANCH IMPLEMENTED; QA/MAIN INTEGRATION NOT YET VERIFIED**.

## Exact source and visual acceptance
- The user supplied two full animated SVG/SMIL source elements in the conversation: Gentleface `images` and Gentleface `video`.
- Original design: https://gentleface.com/icons/images and https://gentleface.com/icons/video, with archived MIT SVG and native Dart references in `riccardopinato/Design-App` (UID-0062 / ICO-008 and UID-0063 / ICO-009).
- These are **not** generic Material icons. Paths and distinctive movements are interpreted in **native Kotlin/Jetpack Compose**, not embedded WebView or Flutter runtime.
- The main approved premium blue/purple Home, broom logo, navigation structure and real storage values are preserved. No new remote dependency or asset packaging.

## Implementation mapping
1. `OmniAnimatedGalleryIcon` — double-frame/photo silhouette; offset corner, sliding main image and pulsing image dot. Playback **once** (3.2 seconds) when Gallery becomes selected. Home tile shows the matching static/rest icon. Other tabs keep their original icons.
2. `OmniAnimatedVideoIcon` — original outlined camera body, animated lens extension/retraction and small recording dot. Playback **once** (3 seconds) while the existing video Play button is visible for an eligible media item.
3. Nonvisual states: the parent NavigationBarItem and Play button retain their localized labels, tap targets and action semantics; the icons are decorative. Android system disabled animations render the static end pose.
4. Animations are lifecycle/scoping bound, not background loops. There is no change to autoplay, video source, codec handling, media dimension, Gallery selection, trash, deletion, database, storage permissions, billing or signature.

## Verification gates
- [ ] Exact-head Kotlin compilation and unit tests.
- [ ] Emulator screenshot/video review for 360dp, dark/light, larger font scale, Gallery selected and Video play pending.
- [ ] Check selection without false cleanup success state; playback does not start before Play.
- [ ] Check Android animation scale set to 0 (static); TalkBack labels and minimum target sizes unchanged.
- [ ] Confirm pause/disposal and no sustained animation after leaving the relevant screen.
- [ ] Verify memory/CPU/jank and APK size on a physical device before release.
- [ ] PR exact-head green required before merge; main integration requires actual merge evidence.

Note: SMIL timing/easing and frame-by-frame stroke-dash morph cannot be rendered identically in native Compose without a more complex animation engine; this implementation preserves the recognizable contours and primary choreography while reducing the runtime footprint. It is not a pixel-accurate SMIL interpreter.
