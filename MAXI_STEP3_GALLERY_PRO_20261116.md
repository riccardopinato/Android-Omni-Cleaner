# Maxi-step 3 — Gallery Pro: first functional slice (2026-11-16)

## Lineage
- Branch: `feature/omni-gallery-pro-v1-20261116`.
- Base: Storage Intelligence PR #24 (`6082abb59e9a72d6c769366d38a42898ddb1ae19`).
- PR #23 remains a separate Draft; `main` / RC1 is untouched.

## Implemented
1. A dedicated **Gallery Pro** screen opened from the Gallery hub, preserving the existing full-size MediaStore photo and native video player under Gallery Review.
2. Read-only video inventory for the already loaded gallery page: name/album search and ALL / >= 100 MiB / >= 5 minutes / 4K filters. Shows resolution, duration and indexed bytes. Results do not imply safe deletion or recoverable space.
3. Optional, on-demand, bounded photo-similarity detection: up to 160 dated photos from the loaded page, scoped MediaStore thumbnails only (96 px), 64-bit difference hash, at most 60 seconds between candidate captures and Hamming distance <= 8. Missing/unreadable thumbnails are skipped. No model download, originals changed or network usage.
4. Separate return navigation to the current full-screen review, retaining existing Trash queue and Android permission confirmation.
5. All labels in five project languages, JVM analytical tests and a no-mutation contract check.
6. AppLab journey and Android visual capture extended to verify the new screen.

## Caveats and follow-on work
- This first slice is **not** an entire-gallery duplicate scanner: pagination, access restrictions and the 160-thumbnail limit are shown to users.
- Similarity groups are *possible visual similarities*, not byte-identical duplicates. No automatic keep decision, removal, trashing or selection.
- Side-by-side original/full-resolution visual comparison, burst navigation, full-video duplicate comparison and metadata-based drilldown are still future work in Maxi-step 3.
- Android CI and emulator AppLab need to pass on the exact feature commit; the signed debug APK is not a Play release.
- OEM MediaStore permission changes, TalkBack, battery/performance on thousands of photos, codec support and Play/Billing/Ads remain external validation gates.

## Exit tests
- [ ] Android CI Kotlin and JVM PASS on feature head
- [ ] AppLab gallery journey PASS on feature head
- [ ] 13-gallery-pro.png present and inspected
- [ ] Correct GitHub APK debug asset and digest for final feature head
- [ ] Physical test and manual verification before production

Do not merge a branch solely because a build is green.
