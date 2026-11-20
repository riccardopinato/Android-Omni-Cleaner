# Omni Cleaner — Gentleface Play/Pause, proposta 11

Approved 2026-11-20.

## Source of truth
- User-curated Design-App: `BIBLIOTECA_COMPONENTI/sorgenti/gentleface/ICO-020.svg` (Play), source blob `7823edcd1d548295a68d3576ff9f630b9266f02b`.
- User-curated Design-App: `BIBLIOTECA_COMPONENTI/sorgenti/gentleface/ICO-021.svg` (Pause), source blob `c0446971694a294d69647f4c58bbd120f8987ace`.
- The VectorDrawable `ic_gentleface_play.xml` and `ic_gentleface_pause.xml` use their **exact pathData**, native to Android, no new runtime dependency.

## Scope
- Consumer: `OmniVideoScrubControls` from WML-003 PR #63, selected icon depends on actual `playing` state from `VideoView`.
- The exact `onPlayPause` handler, semantic labels in EN/IT/ES/FR/PT, accessibility and `omni_video_toggle_play` testTag remain.
- 20dp icon adjacent to actual localized text, decorative `contentDescription = null` so TalkBack reads only the label; monochrome white for media controls.
- No new looped animation; no effects, brand gradients or system Material-style icon override.
- No changes to MediaStore, timeline, seek, playback/stop lifecycle, video ratio, codec, Keep/Trash, Android Trash or files.

## PR strategy and gates
- Source branch is based on exact PR #63 HEAD `0d0caf9b`; it is **dependent** on #63 until #63's Android CI + screenshot have both passed.
- Never merge the dependent PR directly to main before #63 is merged.
- After #63 merges via squash, retarget/rebase this PR as a clean main-based diff (do not accidentally merge duplicate historical changes).
- [ ] Exact-head Android CI PASS.
- [ ] Exact-head screenshot PASS.
- [ ] Runtime player state test on real device: start/pause/resume, end, background, error.
- [ ] TalkBack contrast and small-width inspection.

Status: SOURCE IMPLEMENTED, QA PENDING.
