# Android Omni Cleaner — Impeccable design pilot (17 November 2026)

**Pilot status:** implementation on isolated branch; CI and runtime evidence must be checked on final exact head. **VISUAL/PHYSICAL NOT VERIFIED.**
**Source baseline:** `main@984474d9af62d8b7e17b975a31332161f24f2df7`.
**Sources:** App Factory Core Master v27 / Golden Index v29 (fixed Core baseline `c5adc147046beb7024629466d10eeb799c3c2619`), Core design process PR #100, Design-App DSK-002/Impeccable PR #34 and visual QA PR #35.
**Product authority:** `PRODUCT_BIBLE.md` and `ROADMAP.md`; existing Compose Material 3 blue/purple/mint brand. The scanner improvement in Draft PR #27 is independently owned and must not be overwritten.

## Findings on actual source (not conjectured screenshot results)

- `GalleryMediaPreview.kt` on baseline displayed previews using `ContentScale.Crop` in an enforced square frame. This can cut off portrait/landscape photographs or video thumbnails.
- Video items were displayed only as thumbnail frames. The gallery contained no in-place playable video preview in this surface.
- Gallery `Keep`, `Trash`, `Undo`, MediaStore permission gate and Android system trash logic were separate from the preview; they must remain unchanged.

## Bounded pilot change (no new dependency)

- Photo and video thumbnails now use `ContentScale.Fit` and a bounded aspect ratio derived from the decoded frame instead of forced 1:1 cropping.
- For photos, the image is sampled directly from the source stream where possible and camera EXIF rotation/mirroring is applied; fullscreen bounded decode up to 2048 px allows visual inspection without decoding the unbounded original into memory. Revoked or missing source URIs fail to a safe fallback instead of crashing the viewer.
- An explicit localized `Open photo / Play video` button opens a **non-destructive** full-screen viewer. Photos fit without cropping; videos use Android platform `VideoView` with media controls, and playback stops on dialog disposal. This avoids adding a heavy media library to a storage cleaner.
- The fullscreen dialog has no delete control. Existing review `Keep/Trash/Undo` remain on the main gallery surface with the same controller and Android confirmations.
- EN/IT/ES/FR/PT strings added together. Aspect-ratio edge cases covered by focused JVM tests.
- Media playback correctness, codec support, rotation metadata, pause/background, partial Android 14 access and accessibility **require native runtime/device checks**. A successful compilation does not prove those properties.

## Eight-gate Impeccable mapping

| IWC gate | Evidence / remaining work |
|---|---|
| 01 Context | Product Bible, native Android Compose only and existing brand preserved |
| 02 Direction | Focus on uncropped media, separate fullscreen preview; no Home redesign |
| 03 Critique | Confirmed source-level crop; real before/after screenshot review pending |
| 04 Technical | Aspect ratio unit tests; TalkBack / font scaling / OEM insets pending |
| 05 States | Playback error string, no misleading deletion state; permission-denied flow runtime pending |
| 06 Polish | Full-width action, no new animation/glow, photo/video original proportions; screenshot comparison pending |
| 07 Reuse | Android platform VideoView and existing tokens/controllers; no external runtime donor |
| 08 Split | Native Android emulator and physical evidence required, Web detector not applicable |

## Native QA acceptance matrix

1. Android 10, Android 13, Android 14+ partial/full media access: gallery loads only allowed photos/videos, unavailable state is truthful.
2. Portrait 9:16, landscape 16:9, panoramas, square, HEIC/JPEG if supported: preview and full-screen must show complete frame, with no unintended crop.
3. Local MP4 portrait/landscape: player opens on explicit tap, plays, pauses/seeks, closes without audio leakage or crashed surface.
4. Gallery review: `Keep`, `Trash`, `Undo` and system trash confirmation unchanged before and after full-screen inspection.
5. Empty media, revoked permission, unreadable URI and unsupported codec: friendly error/fallback and no automatic mutation.
6. Dark/light, narrow device, larger fonts, TalkBack focus labels, rotation, app background/re-entry.
7. Measure frame latency and memory on real photo batches; avoid decoding a 48MP original directly into UI.

**Required evidence:** exact PR SHA + Android CI run + Android/Compose UI screenshots from actual emulator/device + reviewer decision, with separate physical status. Existing `.maestro/ui-redesign.yaml` and `.maestro/journey/gallery.yaml` cover gallery navigation only; they do not by themselves prove player functionality. `.github/workflows/applab-runtime-smoke.yml` is manual dispatch by policy; do not claim it executed automatically.

**Do not promote to Golden or Certified Donor:** single consumer change with unresolved visual/physical gates. If real QA passes, register scoped source proof in Design-App and evaluate a reusable aspect-ratio-safe preview primitive for other apps, keeping the original implementation evidence pinned.
