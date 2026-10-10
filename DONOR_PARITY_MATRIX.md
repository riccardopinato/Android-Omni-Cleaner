# Android Omni Cleaner — Donor APK Parity Matrix

Donor APK SHA-256:
`c77f48bbbac1d8e5e16ee44dab67346f3caa0f846bd117e5b05d2cb3b2a88310`

Recovery branch:
`recovery/maxi-step-2-full-apk-reconstruction`

## Status vocabulary

- **SOURCE_RECOVERED** — donor behavior has corresponding reconstructed source and automated build/test evidence.
- **RECOVERED_SAFER** — donor behavior is reconstructed with an intentional safety hardening.
- **PARTIAL** — meaningful donor behavior exists but parity is not complete.
- **RUNTIME_PENDING** — source exists but trusted runtime/physical-device parity has not yet been established.
- **EXTERNAL_CONFIG_PENDING** — implementation exists but provider/store configuration is still required.

## Matrix

| Donor capability | Recovery status | Notes |
|---|---|---|
| Storage overview | SOURCE_RECOVERED / RUNTIME_PENDING | Real storage and logical inventory scan restored. |
| Folder scan | SOURCE_RECOVERED / RUNTIME_PENDING | Empty/below-threshold model, configurable threshold and search restored. |
| Large files | SOURCE_RECOVERED / RUNTIME_PENDING | Size filters, search, review and guarded cleanup restored. |
| Duplicate scan | SOURCE_RECOVERED / RUNTIME_PENDING | SHA-256 grouping restored. Stale donor UI wording mentioning MD5 is not preserved. |
| Duplicate keep-oldest / keep-newest | SOURCE_RECOVERED / RUNTIME_PENDING | Guardian logic guarantees at least one keeper when all copies are selected. |
| Safe Trash | RECOVERED_SAFER / RUNTIME_PENDING | Small non-media files use reversible Safe Trash. Large non-media files are blocked instead of falling back to permanent delete. |
| Media cleanup | SOURCE_RECOVERED / RUNTIME_PENDING | Android MediaStore trash confirmation restored. |
| Empty-folder cleanup | SOURCE_RECOVERED / RUNTIME_PENDING | Revalidated immediately before deletion and constrained by PathSafety. |
| Thumbnail/cache scan | SOURCE_RECOVERED / RUNTIME_PENDING | Donor paths/inventory concept restored. |
| Thumbnail cleanup | RECOVERED_SAFER / RUNTIME_PENDING | Size, timestamp and SHA-256 are revalidated before each delete. |
| App residue scan | SOURCE_RECOVERED / RUNTIME_PENDING | Accessible Android/media candidates restored. |
| App residue automatic cleanup | PARTIAL / INTENTIONAL_SAFETY_CHANGE | Candidates remain review-only when package visibility cannot reliably prove uninstall state. |
| Manual maintenance | SOURCE_RECOVERED / RUNTIME_PENDING | Clipboard and app-management helpers restored. |
| Gallery Cleaner | SOURCE_RECOVERED / RUNTIME_PENDING | MediaStore paging, filters/sort, visual preview, swipe Keep/Trash, prefetch and persisted progress restored. |
| Gallery Undo | SOURCE_RECOVERED / RUNTIME_PENDING | Last review action can be undone before system trash execution. |
| Gallery system trash | SOURCE_RECOVERED / RUNTIME_PENDING | Explicit app confirmation plus Android system confirmation. |
| EXIF privacy scan | SOURCE_RECOVERED / RUNTIME_PENDING | JPEG metadata inspection/grouping restored. |
| EXIF safe-copy cleaning | SOURCE_RECOVERED / RUNTIME_PENDING | Original preservation is the default recovery rule. |
| Pro scan categories | SOURCE_RECOVERED / RUNTIME_PENDING | Exact five donor category contracts restored. |
| Pro DataStore preferences | SOURCE_RECOVERED | `pro_batch_prefs` / `selected_pro_scan_types` restored. |
| Pro batch review | SOURCE_RECOVERED / RUNTIME_PENDING | Real candidates, keeper controls and per-item review restored. |
| Pro batch resolve | RECOVERED_SAFER / RUNTIME_PENDING | Safe categories execute; media go through Android trash; app residues remain review-only. |
| Billing | SOURCE_RECOVERED / EXTERNAL_CONFIG_PENDING | Donor one-time SKU `premium_unlock` restored on current Billing Library adapter. Play Console product/runtime test still required. |
| Premium entitlement | SOURCE_RECOVERED / RUNTIME_PENDING | Central entitlement state now controls feature access and ad eligibility. |
| UMP privacy consent | SOURCE_RECOVERED / RUNTIME_PENDING | Ads are not eligible before consent. |
| Banner ads | SOURCE_RECOVERED / EXTERNAL_CONFIG_PENDING | Debug uses official Google test unit; release ad requests remain disabled until production IDs are supplied. |
| Interstitial ads | SOURCE_RECOVERED / EXTERNAL_CONFIG_PENDING | Donor cadence restored: 3 completed cleanup actions + minimum 180 seconds. |
| Premium = no ads | SOURCE_RECOVERED / RUNTIME_PENDING | Premium blocks ad eligibility/request path, not merely visual display. |
| Functional Google Sign-In | UNPROVEN_DONOR | Play Services sign-in resources existed in APK, but a functional account flow was not proven by forensic evidence. Not invented during recovery. |
| Original visual design | NOT_A_PARITY_GATE | User explicitly plans a later redesign. Functional structure is recovered first. |

## Remaining parity gates

1. Trusted runtime execution of the reconstructed APK.
2. Side-by-side donor vs reconstructed behavior on representative storage/media fixtures.
3. Physical-device validation of permissions, MediaStore trash, Gallery and EXIF.
4. Google Play Billing test for `premium_unlock`.
5. Production AdMob IDs + UMP verification.
6. Decide whether app-residue deletion can be safely re-enabled without relying on ambiguous package visibility.

Build green or AppLab static green does not satisfy these runtime gates.
