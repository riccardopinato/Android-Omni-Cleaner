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
| AUD-0008 (P1) | Filter/sort reset deleted Gallery trash queue | GalleryController, GalleryPreferencesManager: queue persists atomically across view changes; stale pagination cannot discard queue | CODED | Filter/sort, force-stop, reboot and MediaStore permission transitions |
| AUD-0009 (P2) | EXIF multi-step UI too technical | ExifCleanerSection.kt: simple GPS-removal default on JPEG copies, advanced controls collapsed | PARTIAL | One-file SAF flow optional v1.1; real output orientation/GPS/original checks |
| AUD-0010 (P2) | Language embedded inside Pro, no dedicated Settings | OmniPremiumHome gear + Settings bottom sheet; language/UMP options moved, Pro remains commercial workflow | CODED | Screen reader + insets |
| AUD-0011 (P2) | Theme did not survive app restart; untranslated progress semantics | AppAppearance.kt persists System/Light/Dark; OmniPremiumHome uses localized storage percentage semantics | CODED | Restart/OS theme switch/localization |
| AUD-0012 (P2) | Fixed-height Bento tiles could clip large text | OmniPremiumHome.kt: minimum rather than fixed height | CODED | 320dp, fontScale 1.0/1.5/2.0, TalkBack |
| AUD-0013 (P3) | Obsolete HomeScreen implementation persisted beside approved Premium Home | CleanerScreens.kt: delete unused old HomeScreen and supporting quick-action class | CODED | No references; CI exact source |
| AUD-0014 (P1) | Physical visual/gesture/accessibility UX was unverified | Dedicated acceptance matrix below; repo CI remains automated evidence only | **NOT VERIFIED** | On-device media, TalkBack, insets, animations |
| AUD-0015 (release gate) | Signing, Play Billing, AdMob/UMP and permission declarations not live-validated | RELEASE_FREEZE.md / STEP10_STORE_RELEASE_CERTIFICATION.md remains canonical | **BLOCKED EXTERNAL** | Release key secrets, Play internal testing, provider config and physical validation |
| AUD-0016 (P2) | All five offscreen destinations remained composed | RedesignedOmniCleanerApp.kt skips inactive Home/Gallery/Pro; Clean and Privacy remain for in-flight selection and SAF state safety | PARTIAL | Profile memory/jank and test navigation state |
| AUD-0017 (P2) | Nested screens repeated destination headings | GalleryCleanerSection, ExifCleanerSection, MonetizationSection now rely on parent heading | CODED | Screenshot comparison |

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
