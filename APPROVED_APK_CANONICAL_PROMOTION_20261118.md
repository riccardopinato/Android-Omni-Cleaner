# Android Omni Cleaner — Canonical main promotion (18 November 2026)

## User-approved authority
**The user-supplied APK is the product/visual baseline**, not the former `main` presentation.

- Exact supplied APK: `Android-Omni-Cleaner-837b66d8c860-debug.apk`
- Exact supplied APK SHA-256: `7d5623a46439ddf87b52526aa5914fee5d32b571825fc48a220bdee1c4c0e68b`
- Original source commit: `837b66d8c860b0bc13b0a96a7c508b5b680e7f56`
- Source approved to promote after non-regression checks: `1f626905a58b124b41fff6be535ede5aa8f62e86`, i.e. the same code line plus five subsequent Safe Trash, MediaStore, scanning and test corrections.
- Former default branch snapshot: `62bdd4ce2173e7145d4ae22d66c6256c2d17c9ad`; retained in `backup/omni-main-before-apk-baseline-20261118`.
- Original approved source snapshot: `backup/omni-user-approved-apk-837b66d8-20261118`.
- App Factory baseline resolved once in this session: `a8d20ceb71ce9e9e16e6756d257d611fa6f1e0ec` (Master 27, Golden Index 29).

## Identity parity / actual visual reference
The following verified Git blobs are **identical** at `837b66d8c860b0bc13b0a96a7c508b5b680e7f56` and `1f626905a58b124b41fff6be535ede5aa8f62e86`:
- `OmniPremiumHome.kt` — `8d8bfba144d080e5963ec22556bc9d23f4a33b3f`
- `RedesignedOmniCleanerApp.kt` — `5e68f734d82d512d4639890670d06cb9492fd17e`
- `CleanerScreens.kt` — `c402f1c47197db61f05f7f5cb35ed267bf4c917a`
- `GalleryMediaPreview.kt` — `e4a8a68d4d16fe5eddf9edc98fee5aaf2d6e45ee`
- `GalleryProSection.kt` — `94504f506e4a5ee91085bff9d40df03ef4f78a51`
- `Theme.kt` — `1461f804297b901a6e931a39c11cb1ded16eee65`
- `OmniNavigationIcon.kt` — `e627dafc321d141aa5e58c212cc04d912608388b`

The only user-visible change to `GalleryCleanerSection.kt` after the supplied APK is a truthful warning when selected Android Trash media exceed the 2,000 URI limit; Gallery Pro and playback code remain unchanged.

## Real screenshot evidence (same user-approved UI lineage)
- Workflow run `44119421800`, commit `028dce63d81ed25f5274bbf9ec620b711fdeb331`.
- Real Android 35 Pixel 7 Pro (1440 × 3120) artifact `14345410767`: 14 routes including premium Home, Clean, Gallery, Privacy, Pro, duplicate scan, filters, EXIF detail, billing, video review, storage intelligence, gallery Pro and review center.
- Synthetic media and storage numbers, NOT physical-device claims.
- Target latest `1f626905a58b124b41fff6be535ede5aa8f62e86`: Android CI run `44120748227` PASS; AppLab runtime smoke `44120748408` PASS. This is not Play certification.

## Promotion decision and exclusions
1. Restore the **complete latest approved-code-line tree** onto a new PR branch rooted in old `main` while preserving both histories using a two-parent bridge commit. Do NOT force-push `main`.
2. Preserve two named immutable pre-promotion Git backup refs.
3. Allow PR merge into main only after exact PR-head Android CI, real visual emulator screenshots (including Home/secondary routes), screenshot inspection, no critical regression and source/tree verification.
4. After merge, assert `main` has the approved UI blobs and contains no files from the wrong UI branch except shared files genuinely present in the approved lineage.
5. Mark new `main` as **v1.1 development baseline**, not the production RC1 release. Existing signed artifacts, release cert pin and published versions are **not** changed.
6. Gallery UX, fullscreen playback, performance, responsive accessibility, real MediaStore Trash, physical validation and signed Play release remain separately gated future work.
7. The former PRs #29/#31/#32 are NOT cherry-picked into the user-approved tree. No automatic deletion of branches, releases or PR history.

The completed promotion's specific PR number, commit and exact-head QA should be appended after the operation, not presumed successful in advance.
