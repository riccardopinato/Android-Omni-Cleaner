# Bug assessment — Omni Cleaner hour-long video duration

**Factory:** `f8b9f550c544735237f19cdd723bd2df667ceee6` · **Original Omni source:** `e2959f4680e1c3c6c40720d72d1c1262287c1925` · **FSL ID:** `FSL-20261127-OMNI-DURATION`.

## Reproduction based on original source

`GalleryModels.kt` formats `durationMs=3_723_000L` via `minutes = (durationMs/1000)/60`; expected original output is **62:03**. In the *same* Gallery, `OmniVideoScrubControls.kt` already formats a similar Int duration as **01:02:03** by preserving hours. Thus the original gallery label is inconsistent with the existing video timeline contract.

This is a deterministic reproduction from the **original implementation formula**, not a phone screenshot or a previously executed failing JUnit test. No media file or device data is required.

## Severity and decision

P2 display correctness; bounded source fix is compatible with development v1.1 and does not alter frozen RC1 identity, MediaStore, trash, ads, signing or code paths unrelated to the gallery. The older RC1 remains a separately frozen release snapshot.
