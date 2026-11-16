# Android Omni Cleaner — Audit 360° remediation ledger
Date: 2026-11-15
Mode: implementation on audit-only follow-up explicitly authorized by user
Repository: riccardopinato/Android-Omni-Cleaner
Primary release main: 984474d9af62d8b7e17b975a31332161f24f2df7 (RC1 freeze, unchanged)
Design predecessor: PR #22, 46dfdfa484738c8874b851fac214c071e168ac2c
Audit implementation: draft PR #23, audit/omni-safety-ux-360-20261115
App Factory resolved baseline: c5adc147046beb7024629466d10eeb799c3c2619
Factory Master: v27; Golden Index: v29; Golden Registry: v2
Historical factory pins in recovery documentation are historical, not a replacement for CURRENT.yaml.

## Authority and evidence
Product Bible sets safe cleaning and local-first scope.
No background deletion, AI/autodelete, cloud dependency, fake reclaimed bytes or policy bypass.
RC1's RELEASE_FREEZE.md allows only blockers/correctness/security/policy/accessibility into RC1.
These UX refinements belong to a **DRAFT** follow-up and are not merged into main.
CI runs only prove that code compiles/tests pass; the physical device and Play gates are separate.

## Issue-by-issue remediation matrix
Here **CODED / NOT PHYSICALLY VERIFIED** never means Store Ready.

| Audit ID | Finding | Implementation / owner | Evidence status | Remaining gate |
| --- | --- | --- | --- | --- |
| AUD-0001 (P0) | Bulk and preselected cleanup could include files hidden by UI take() limits | StorageCleanupSection.kt and ProBatchReview.kt: progressive lists + complete selected-file preview in confirmation; ProBatchController.kt now starts with no selected candidates | CODED | Simulated 100+/1000+ candidates; all selected files inspectable and deselectable; physical mutation safety |
| AUD-0002 (P1) | Gallery trash queue not inspectable item-by-item | GalleryCleanerSection.kt: review dialog, URI and one-by-one Keep/unmark; GalleryController.removeFromTrashSelection | CODED | UI snapshot and Android permission test |
| AUD-0003 (P1) | Undo unavailable after app/process restart | GalleryPreferencesManager and GalleryController: bounded last-action journal; last image restored with MediaStore anchor check, state validated; JVM journal tests | CODED | Kill/reopen after Keep and after Trash on real media; stale gallery ordering |
| AUD-0004 (P1) | Only first 12 Safe Trash entries reachable | SafeTrashSection.kt: progressive entry list, individual restore or confirmed permanent delete | CODED | 30+ stored files, migration/restart, SHA-verified restore, purge |
| AUD-0005 (P1) | Clean was a long technical mega-scroll | CleanerScreens.kt: primary scan actions first, optional scan tools/filters, advanced maintenance and explicit trash entry | CODED | Guided user journey on small phone |
| AUD-0006 (P2) | Large/duplicate/folder preview repeated again in selection view | CleanerScreens.kt: remove redundant read-only list rows, keep summaries and actionable review | CODED | View all result types after scans |
| AUD-0007 (P2) | Three unrelated search boxes and complex filters shown together | CleanerScreens.kt: search mode selector for folders/large files/duplicates, panel collapsed by default | CODED | Accessibility with long locale strings |
| AUD-0008 (P1) | Filter/sort reset deleted Gallery trash queue | GalleryController, GalleryPreferencesManager: queue persists across view changes and full MediaStore access refresh | CODED / NEW CI PENDING | Filter/sort, force-stop, reboot and MediaStore permission transitions |
| AUD-0009 (P2) | EXIF multi-step UI too technical | ExifCleanerSection.kt and FolderExifCleaner.kt: direct one-JPEG SAF copy path with read-only source, SHA integrity and EXIF verification; folder batch preserved | CODED / NEW CI PENDING | Android SAF, orientation/GPS/original checks on device |
| AUD-0010 (P2) | Language embedded inside Pro, no dedicated Settings | OmniPremiumHome gear + Settings bottom sheet; language/UMP options moved, Pro remains commercial workflow | CODED | Screen reader + insets |
| AUD-0011 (P2) | Theme did not survive app restart; untranslated progress semantics | AppAppearance.kt persists System/Light/Dark; OmniPremiumHome uses localized storage percentage semantics | CODED | Restart/OS theme switch/localization |
| AUD-0012 (P2) | Fixed-height Bento tiles could clip large text | OmniPremiumHome.kt: minimum rather than fixed height | CODED | 320dp, fontScale 1.0/1.5/2.0, TalkBack |
| AUD-0013 (P3) | Obsolete HomeScreen implementation persisted beside approved Premium Home | CleanerScreens.kt: delete unused old HomeScreen and supporting quick-action class | CODED | No references; CI exact source |
| AUD-0014 (P1) | Physical visual/gesture/accessibility UX was unverified | Dedicated acceptance matrix below; repo CI remains automated evidence only | **NOT VERIFIED** | On-device media, TalkBack, insets, animations |
| AUD-0015 (release gate) | Signing, Play Billing, AdMob/UMP and permission declarations not live-validated | RELEASE_FREEZE.md / STEP10_STORE_RELEASE_CERTIFICATION.md remains canonical | **BLOCKED EXTERNAL** | Release key secrets, Play internal testing, provider config and physical validation |
| AUD-0016 (P2) | All five offscreen destinations remained composed | RedesignedOmniCleanerApp.kt skips inactive Home/Gallery/Pro; Clean and Privacy remain for in-flight selection and SAF state safety | PARTIAL | Profile memory/jank and test navigation state |
| AUD-0017 (P2) | Nested screens repeated destination headings | GalleryCleanerSection, ExifCleanerSection, MonetizationSection now rely on parent heading | CODED | Screenshot comparison |

## Follow-up remediation — additional source gate pending
- Preserves user-reviewed Gallery Trash picks through a full access/media refresh; no automatic media deletion.
- Adds a single-file JPEG OpenDocument/CreateDocument clean-copy flow; retains existing folder batch flow.
- Previous SUCCESS at fd9a24e does **not** certify these later source changes. Run exact-head software gates before treating these fixes as verified.
- AUD-0016 remains PARTIAL intentionally: the inactive Clean/Privacy compositions retain in-flight review and SAF state. Elimination without navigation-state preservation and profiling would be a safety regression.


## Roadmap continuation — automated validation (2026-11-15)

Baseline revalidated: `d821abf838c4514615f96f4f92879f3310b6fd9f`.
[Android CI #43732421434](https://github.com/riccardopinato/Android-Omni-Cleaner/actions/runs/43732421434)
passed **exact-head** locale/launcher parity, JVM unit tests, debug + androidTest APK compilation,
`lintDebug`, `lintRelease`, `assembleRelease` and `bundleRelease`.
Debug APK artifact tag: `fast-apk-d821abf838c4` (19,010,525 bytes,
GitHub release asset SHA-256 `5931f01fe6ef188ad9fd47a47072d70d499d276bfa9e88633b059e6411db5c2b`).
Thus the *older* checklist marks for these exact-source software tasks can be reconciled as PASS
for that SHA; this does not certify any later commit.

The legacy Maestro `ui-redesign.yaml`, `parity-storage.yaml` and
`localization-accessibility.yaml` referenced the pre-audit layout. The new
audit-bound smoke suite tests actual Home Settings, five destinations,
single-JPEG privacy entry, advanced panel, a non-destructive storage scan,
Clean filter state through a destination switch, and locale switching at 1.3x.
A dedicated push trigger for `AppLab Runtime Smoke` applies **only** to the
audit branch, with code/journey paths, because that stacked PR cannot receive
the main-targeted PR gate. No PR, main merge or store release is created.

**Evidence pending:** the updated Maestro flows and emulator smoke require a
successful immutable-head AppLab run. No PASS is claimed until GitHub reports it.
These scripted checks do not replace real-device gesture/permission/MediaStore,
SAF, EXIF, TalkBack, appearance or OEM validation.

**AUD-0016 safety boundary:** inactive Clean and Privacy remain composed while
containing in-flight picker state or non-saveable destructive selections. Do
not drop them from the composition just to improve an unmeasured benchmark;
the safe next change needs retained selection/SAF state, navigation
restoration tests and measured jank/memory before composing only the active tab.

**Still BLOCKED EXTERNAL:** AUD-0014 physical UX/OEM QA, AUD-0015 release
key and signing identity, Play Internal Testing and billing, AdMob/UMP, Data
Safety, MANAGE_EXTERNAL_STORAGE policy review. Second-device Golden
performance validation is also unfulfilled.

## Roadmap follow-up — fluidity and blue action language (2026-11-16)

The audit Draft branch now prepares the following **source changes, not yet CI-certified at
this new HEAD**:

- Shared Compose action component `OmniActionButton` sets a blue
  `#285DE5` container and white action text in light and dark themes.
  Existing Material3 `Button`, `OutlinedButton` and `TextButton`
  call sites in the 12 interactive screens are routed through this shared component;
  selection chips and navigation tabs retain distinct selection semantics.
- Premium Home Settings action and quick-analysis action also use blue/white
  action treatment.
- Bottom navigation **Clean** now uses an original 24dp tint-aware vector broom
  with filled bristle fan, handle and small sparkle. It replaces only that
  destination's icon; approved full launcher icon remains unchanged.
- Premium wave background uses `drawWithCache` so decorative paths/gradient
  brushes are not regenerated on each redraw; navigation uses a short
  125ms alpha animation and retains existing Clean/Privacy composition for
  non-saveable destructive selections and in-flight SAF state.
- `.github/scripts/check-ux-action-style.py` is added to the Android CI
  source gate to detect future style regressions and missing broom icon.
- No change to scanning, deletion policy, pricing, permissions, release identity
  or app package.

**Gate:** new exact-head Android CI compilation and AppLab/emulator runtime
must succeed before claiming PASS or delivering an updated APK.
**AUD-0016 stays PARTIAL** until an instrumented and measured state-ownership
migration makes offscreen composition removable without sacrificing safety.
**AUD-0014 / AUD-0015 stay NOT VERIFIED / BLOCKED EXTERNAL** pending physical
Android and release-provider evidence.

## Audit continuation — 2026-11-16 (source changes require exact-head gates)

- **Navigation P1:** the 1.3x font-scale journey failed after Clean → Home despite
  standard Maestro and visual journey passing. Navigation items now have stable
  accessibility resource IDs via Compose semantics and the 1.3x Maestro flow
  taps the clickable tab itself, then tests Clean → Home twice. If this still
  fails, treat as a real navigation/input regression; do not declare PASS.
- **Embedded XMP hardening:** beyond JPEG sidecar segments, a GPS cleanup
  now also rejects the Exif TIFF XMP attribute before mutation. The predicate
  has explicit JVM tests for location vs non-location selections.
- **EXIF GPS P1:** selected GPS cleanup now blocks unsupported auxiliary
  metadata (non-Exif APP1/XMP, APP13/IPTC and unknown sidecar segments) before
  cleaning any temporary copy. The conservative approach leaves the original
  untouched and does not claim complete location removal from unreviewable
  metadata. Added JVM synthetic JPEG cases for XMP, IPTC and Exif-only images.
- **Blue action safety P1:** Gallery Keep and Trash retain blue/white action
  surfaces but have distinct green/red border cues, plus existing icons and
  descriptive accessibility labels; Android Trash still requires confirmation.
- **Software gates pending:** exact-head Android CI, AppLab with 1.3x font,
  persistence warning and destructive-state stability. No claim of physical
  test, performance uplift, Store signature or provider certification.
- **AUD-0016** remains PARTIAL until navigation and responsiveness measurements
  pass on representative devices; destructive selection state is intentionally
  retained rather than uncomposed.

## Roadmap follow-up — responsive Gallery and deeper emulator gates (2026-11-16)

Source work staged after the preceding exact-head software gates:
- Gallery choice/exit cue reduced from 210/225 ms to 155/170 ms, preserving
  the release animation before an actual Keep or Trash selection is committed.
  This changes perceived latency only; system-managed deletion stays untouched.
  A JVM timing-contract test asserts that commit still follows the animation.
- AppLab smoke is extended to execute the Android instrumented test suite on
  the emulator, not merely compile the androidTest APK, and to repeat the
  localization journey under 1.3x, 1.5x, 2.0x Android font scales.
- Full suite, physical responsiveness, TalkBack and Play provider certification
  must be reported separately. Instrumentation on emulator is NOT physical QA.

## UX design decisions
- **Preserve** approved broom launcher asset and the Premium Light/Dark visual language.
- **Preserve** the five primary destinations for this iteration; moving Pro out of nav would change the approved UI and needs a separate user design decision.
- **Preserve** Kotlin + Compose and all canonical storage/media engines.
- **Preserve** local-first semantics; no new backend, no new sensitive permissions, no new monetization SKU.
- **Preserve** Android Trash confirmation as the actual media mutation gate; Gallery red action selects, never directly deletes.
- **Preserve** Safe Trash no-overwrite/integrity policy and report only real reclaim on permanent deletion.

## Automated acceptance checklist
- [ ] Exactly matching locale resource keys, placeholders EN/IT/ES/FR/PT
- [ ] Adaptive/monochrome launcher parity
- [ ] Unit tests: duplicate keeper, snapshots, Safe Trash, Gallery paging, persisted Undo, billing rules
- [ ] Kotlin debug + androidTest APK compilation
- [ ] Release lint/assembleRelease/bundleRelease on exact final source (separate from fast PR CI)
- [ ] Android emulator UI journeys: Home, Clean, Gallery, Privacy, Pro, Settings
- [ ] AppLab smoke and static quality gates bound to exact source SHA
- [ ] Final GitHub Actions run is from the exact **last** source SHA, not a prior commit
- [ ] Exact-head debug APK published with SHA-256 and download verified

## Physical UX acceptance checklist
- [ ] 320dp, 360dp, 412dp and 480dp; portrait and supported landscape
- [ ] System/Light/Dark; restart preserves explicit choice; system mode tracks OS
- [ ] font scale 1.0/1.5/2.0, TalkBack labels, motion reduction and system insets
- [ ] Safe Trash with at least 30 entries: reach late entries; restore one, delete one; original SHA match
- [ ] Cleanup 30+ duplicates, 100+ large files, 30+ empty folders: every selected file is visible in confirmation and can be excluded
- [ ] Pro scan starts with **zero** selected destructive candidates
- [ ] Selection after 50/100+ duplicates: keeper always survives, no hidden unauthorized deletion
- [ ] Gallery LEFT Cestino, RIGHT Tieni; Annulla; buttons/taps and swipe agree
- [ ] Gallery select -> change filter -> change sort -> kill process -> reload -> selected queue preserved and individually editable
- [ ] Gallery last Keep/Trash -> kill -> restart -> Undo returns to prior media where gallery anchor still matches
- [ ] Gallery original media unchanged on system Trash cancellation; actual Android system dialog appears
- [ ] GPS-only EXIF export: original unchanged, output has selected metadata removed, picture/orientation preserved
- [ ] Standard capability remains available without broad access; Deep Cleaning requires deliberate disclosure
- [ ] Ads banner/no-fill and UMP controls do not interfere with bottom gestures/important actions
- [ ] Offline/restart/revoked permissions, storage unavailable and low-space fail safely

## Release / external blockers
- Permanent release keystore, backups and exact fingerprint pin are not available in this code audit.
- Play Internal Testing (purchase, pending, already-owned, restore, refund/revoke) is not simulated by debug builds.
- Production AdMob app/unit IDs, consent messages, app-ads.txt and public policy URL must be verified by providers.
- Android MediaStore/system trash, SAF picker and OEM permission semantics require device checks.
- Android all-files access approval is never inferred from the mere existence of policy documentation.
- RC1 main freeze remains active; the stacked draft PR must not be merged until authorized gates resolve.

## Work policy
Use SOLO CHAT. Subsequent runs must verify this ledger against GitHub HEAD and CI before reporting success.
Avoid duplicate branches, duplicated PRs or repeated non-source-changing builds.
No auto-merge into frozen RC1 main; never fabricate physical/provider proof.
