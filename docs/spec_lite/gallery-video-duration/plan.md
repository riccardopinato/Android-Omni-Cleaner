# Plan — minimal duration formatter consolidation

1. Reproduce the mismatch from the old Long getter and existing Int scrubber function; no device data needed.
2. Move time formatting to one package-internal helper in pure `GalleryModels.kt`: Long implementation plus Int compatibility overload.
3. Delegate `GalleryMediaItem.formattedDuration` to the Long helper for positive durations; preserve empty labels for missing duration.
4. Remove superseded formatting implementation from `OmniVideoScrubControls.kt`; preserve all UI flow/callbacks.
5. Add 4 deterministic JVM tests for AC-001..004; existing scrub tests should remain green. Run existing exact-head native PR CI.
6. Rollback: revert 3-file code delta; no storage or config migration. Device/media playback remains unverified without a physical test.
