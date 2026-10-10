# Android Omni Cleaner — Donor APK Parity Matrix

Donor APK SHA-256:
`c77f48bbbac1d8e5e16ee44dab67346f3caa0f846bd117e5b05d2cb3b2a88310`

Step 3 audit branch:
`audit/maxi-step-3-apk-parity`

## Final Step 3 vocabulary

- **RECOVERED** — donor behavior is reconstructed and supported by source/test/runtime evidence appropriate to the capability.
- **IMPROVED** — donor behavior is present with an intentional safety/truth hardening.
- **INTENTIONALLY_CHANGED** — donor behavior was deliberately not reproduced because the safer/product-truth contract is stronger.
- **BLOCKED** — parity cannot be established without a real device, Play/provider configuration or trusted artifact-bound evidence.
- **MISSING** — donor-proven behavior is absent from the reconstructed product.

## Matrix

| Donor capability | Step 3 verdict | Evidence / boundary |
|---|---|---|
| Storage overview | **RECOVERED** | Deterministic inventory tests + controlled emulator parity journey. |
| Folder scan | **RECOVERED** | Deterministic fixture verifies empty/below-threshold classification and ignore policy. Real-device broad-storage parity remains physical evidence. |
| Large files | **RECOVERED** | Controlled 12 MiB emulator fixture found with the 10 MiB filter. |
| Duplicate scan | **RECOVERED** | SHA-256 grouping, same-size prefilter and byte-identical verification recovered; controlled duplicate fixture found in runtime. |
| Duplicate keep-oldest / keep-newest | **RECOVERED** | Automated keeper/guardian contracts prevent deleting every copy. |
| Safe Trash | **IMPROVED / BLOCKED physical cleanup** | Reversible private trash is tested deterministically; large non-media permanent-delete fallback is intentionally blocked. |
| Media cleanup | **RECOVERED / BLOCKED physical confirmation** | MediaStore system-trash path is implemented; real system confirmation is reserved for physical testing. |
| Empty-folder cleanup | **RECOVERED / BLOCKED physical cleanup** | PathSafety and immediate revalidation are tested; destructive device behavior is not inferred from emulator scan evidence. |
| Thumbnail/cache scan | **RECOVERED** | Controlled `DCIM/.thumbnails` fixture is surfaced by Maestro. |
| Thumbnail cleanup | **IMPROVED / BLOCKED physical cleanup** | Size/timestamp/SHA-256 revalidation is tested before delete; destructive physical scenario remains pending. |
| App residue scan | **RECOVERED** | Accessible `Android/media` candidate scanner restored. |
| App residue automatic cleanup | **INTENTIONALLY_CHANGED** | Candidates remain review-only when package visibility cannot reliably prove uninstall state. |
| Manual maintenance | **RECOVERED** | Clipboard/app-management helper contracts restored. |
| Gallery Cleaner | **RECOVERED / BLOCKED physical media parity** | Paging/filter/sort/review model and UI are recovered; parity journey proves reachability. Real media permission/MediaStore behavior remains physical. |
| Gallery Undo | **RECOVERED / BLOCKED physical media parity** | Pre-trash history/undo contract is implemented and unit-tested; real media flow remains physical. |
| Gallery system trash | **RECOVERED / BLOCKED physical confirmation** | Explicit app confirmation + Android system trash request restored; not auto-confirmed in CI. |
| EXIF privacy scan | **RECOVERED / BLOCKED SAF physical parity** | EXIF groups, JPEG validation and content-digest contracts are tested; real SAF tree selection remains physical. |
| EXIF safe-copy cleaning | **IMPROVED / BLOCKED SAF physical parity** | Original-preserving export, digest verification, orientation/color/XMP protection restored. |
| Pro scan categories | **RECOVERED** | Exact five donor category contracts restored. |
| Pro DataStore preferences | **RECOVERED** | `pro_batch_prefs` / `selected_pro_scan_types` restored. |
| Pro batch review | **RECOVERED** | Candidate review, keeper controls and per-item selection restored. |
| Pro batch resolve | **IMPROVED / BLOCKED physical cleanup** | Safe categories execute; media use Android trash; app residues remain review-only. |
| Billing | **RECOVERED / BLOCKED Play test** | Donor one-time INAPP SKU `premium_unlock` restored on current Billing adapter; Internal Testing purchase/restore/acknowledge still required. |
| Premium entitlement | **RECOVERED** | Central resolved entitlement controls feature access and ad eligibility; fail-closed tests cover unresolved state. |
| UMP privacy consent | **RECOVERED / BLOCKED production validation** | Consent gate exists; production/provider validation belongs to monetization productization. |
| Banner ads | **RECOVERED / BLOCKED production config** | Debug uses Google test IDs; release requests remain disabled until production IDs are supplied. |
| Interstitial ads | **RECOVERED / BLOCKED production config** | Donor cadence restored: 3 completed cleanups + minimum 180 seconds. |
| Premium = no ads | **RECOVERED** | Premium blocks the request path, not only visual display. |
| Functional Google Sign-In | **BLOCKED — donor unproven** | Sign-in resources/classes existed in the APK, but no functional donor account flow was proven; recovery does not invent one. |
| Original visual design | **INTENTIONALLY_CHANGED / NOT A PARITY GATE** | A later redesign is explicitly planned. |

## Step 3 evidence

Final green audit head before closure:
`99d6938f689771f64bc28a0547afb7c76e9f5a24`

- Android CI: **PASS** — run `38076397786`.
- AppLab Intelligence v4.5: **PASS** — run `38076397838`.
- AppLab Runtime Smoke: **PASS** — run `38076397823`.
- Maestro parity journey: **PASS**.
- Smart Visual QA: **PASS**.
- Multi-Screen Visual Journey: **PASS**.
- System UI Lab: **PASS**.
- Persistence & Restart Lab: **WARN, 0 errors**.
- Storage & Data Integrity Lab: **WARN, 0 errors**.

The AppLab warnings are advisory and do not establish physical parity. They are retained as evidence rather than rewritten as PASS.

## Step 3 conclusion

Critical `MISSING` donor capabilities: **0**.

The remaining `BLOCKED` items require one of:
- physical-device execution;
- Android system UI / SAF behavior on a real phone;
- Google Play Internal Testing;
- production AdMob/UMP configuration;
- trusted artifact-bound AppLab evidence after GitHub artifact quota is available.

Step 3 is therefore complete within its declared evidence boundary. It does not claim store readiness or physical-device certification.
