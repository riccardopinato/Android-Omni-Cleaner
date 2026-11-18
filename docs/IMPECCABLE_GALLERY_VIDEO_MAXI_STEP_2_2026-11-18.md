# Omni Cleaner — Gallery/Video UI polish (Maxi-step 2)

Factory baseline: `d660cccd57f2e252c573c484457f1ae9234b1e33` (Master v27, Golden Index v29).
Base repository head: `0c88bc6af909a37dcaf5eccdec9946a0c4a1ef50`.
Design direction: Impeccable / DSK-002 applied to native Android, not via a browser detector.

## Source critique and repairs
- Preview loading previously showed a premature "unavailable" state, and `produceState(initialValue = null, item.id)` can retain the prior bitmap during a new media item's loading window. The new per-item keyed loading state avoids displaying the *wrong photo* before decoding finishes.
- Review filter buttons and Keep/Trash/Undo previously used horizontal scrolling. Filter chips now wrap; Keep and Trash stay in a two-column row with an Undo button below.
- Exact loaded-window position and persisted reviewed count are visible without pretending they represent the entire device library.
- Fullscreen inspection uses a black, non-cropping viewing surface, with immediate Close access and explicit copy that review decisions occur outside the viewer.
- Native VideoView retains Android decoding/media access, but exposes persistent Play/Pause, ±10-second seek controls, slider and clock. Player releases resources on dialog disposal; metadata edge cases clamp seek targets. No external player dependency added.

## Non-goals and safety invariants
- No change to `GalleryController`, `MediaStoreHelper`, media-selection state, Android Trash confirmation, or permanent deletion pathways.
- Fullscreen video/photo view does not expose delete/keep actions; user closes the dialog and deliberately chooses an action.
- Media access remains Android permission mediated; photo EXIF, save/export, and Premium are unaffected.

## Acceptance plan
1. Immutable-head PR Android CI: locale parity, unit tests (including playback seek math), debug APK and androidTest APK compilation.
2. Maestro emulator navigation of Gallery in no-permission/empty-media state and full-screen photo/video when a deterministic media fixture is available.
3. Real screenshot review across light/dark, narrow width, large text and 2 media aspect ratios. Verify no crop, 48dp touch targets, controls don't hide, back navigation works.
4. Test an actual device with portrait/landscape images and MP4 playback: enter, pause, seek, background, resume, close, then Keep/Trash/Undo and Android confirmation. No physical-device certification inferred from emulator.
5. Keep PR isolated from #27 scan-engine and issue #30 intermittent hash-cache unit-test investigation.

Current runtime evidence: NOT YET CAPTURED. Mark no view or delete pathway as physically certified before evidence.
