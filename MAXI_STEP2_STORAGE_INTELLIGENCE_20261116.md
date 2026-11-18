# Maxi-step 2 — Storage Intelligence (working draft)

## Scope and baseline
- Branch: `feature/omni-storage-intelligence-v1-20261116`, stacked on the unchanged app baseline of PR #23 at `47a1fb759258c62d8fb71c20af98e7084d7684de`.
- This is a feature follow-up, not an RC1 hotfix and not a merge into `main`.
- Step 1 Android CI: PASS #43883453727; exact-head AppLab #43883453793 must be checked separately, not inferred from compilation.

## Implementation (read-only)
1. A dedicated eighth Clean tool, **Storage Intelligence**, launched as its own screen under the existing bottom-tab/hub-detail navigation contract.
2. A proportional horizontal storage map of accessible, indexed top-level folders. Selecting a folder filters the search. The bar denominator is **indexed bytes**, not total device capacity.
3. Read-only file search by name, extension or relative path, with folder scope and 0 / 10 MiB / 100 MiB size filters. Results are sorted by size, paged 20 at a time and capped at 200 visible entries. The UI states the number of all matches.
4. Contextual review suggestions use the existing conservative SmartCategoryScanner classifier: old downloads 90+ days, installation packages, archives and screenshots. Categories may overlap: never add their bytes and call the sum recovered space.
5. Result actions open the existing Large Files and Smart Categories review screens rather than creating new deletion logic.
6. Shares one StorageInventoryRepository snapshot across the modules; no new full-device traversal or permission is requested. Search and aggregation use Dispatchers.Default; UI recomposition never performs the heavy work on the main thread.
7. Five-language resource parity (English, Italian, Spanish, French, Portuguese) and accessibility through standard Compose controls and labeled folder rows.

## Safety constraints
- NO direct delete, Trash, SafeTrash, restore or file mutation APIs in the new engine or screen.
- No automatic selection, background deletion, heavy machine-learning model or misleading RAM/CPU boosting.
- Existing storage permission gate applies before entering the screen; no new Manifest permissions.
- Unreadable and excluded directory counts are disclosed, rather than claiming an exhaustive device inventory.
- All actual deletions still require the existing separate reviewed, snapshot-revalidated cleaners.

## QA and certification
- JVM tests: category grouping, top-level/root files, overlapping hints, search by name/ext/path, folder filter, minimum size, bounded results, outside-root exclusion and empty inventory.
- Source gate: read-only API contract, navigation, 5 locale keys and Maestro journey.
- AppLab: synthetic emulator read-only journey through scan/search/Advanced, plus retained parity/accessibility and restart checks.
- Visual workflow: real Android screenshot `12-storage-intelligence.png` after scanning.
- Android CI: lint and full native debug/release compilation, JVM tests; exact-head debug APK only when green.
- External: OEM SAF/MediaStore Trash, real 35+ Safe Trash entries, physical jank, TalkBack, large libraries and Play provider/signing gates remain separate.

## Operational exit checklist
- [ ] All exact-head Android CI checks PASS
- [ ] AppLab exact-head journey PASS
- [ ] Native screenshot captured; visually inspect truncation, overflow and display density
- [ ] Debug APK asset download and SHA-256 verified
- [ ] Physical OEM checks performed before labeling production ready

No User files were opened or altered while preparing this change. Its new code reads only the indexed paths and file metadata.
