# Omni Cleaner — AMX-006 Accordion Loader (approved 19 November 2026)

Status: **USER-APPROVED; IMPLEMENTED ON FEATURE BRANCH; RELEASE REQUIRES CI + VISUAL QA**.

## Design-App source of truth
- Design-App registered component: `AMX-006` / `UID-0006`.
- Original Amicro React/Framer Motion source: `riccardopinato/Design-App/BIBLIOTECA_COMPONENTI/sorgenti/amicro/amx-006.tsx`, MIT.
- Existing Flutter draft: `riccardopinato/Design-App/BIBLIOTECA_COMPONENTI/flutter_adapters/lib/src/amx_006_accordion_loader.dart`.
- Omni Cleaner is Kotlin/Jetpack Compose; this is a **native Compose redraw** that does not import Flutter or a React runtime.

## Visual contract
- Four horizontally drawn bars within 40 dp; bar thickness 4 dp; gap 6 dp; left-anchored.
- Each scales horizontally between 100% and 20% over a 1500 ms mirrored ease cycle.
- Successive bars start 150 ms later.
- Theme primary color, rounded corners; no percentage or false status is generated.
- Respects Android animation-disabled setting (renders four static bars).
- On state change, the composable exits the UI tree and its infinite animation is disposed.

## Real scanning integration
1. Cleaner hub: `CleanerUiState.scanning`, actual active `currentOperation`, Cancel still present.
2. Cleaner dedicated routes: overview/large files/duplicate inventory/folders use AMX-006 only while indeterminate. For duplicates, bounded processed/total still uses the existing *determinate* progress bar.
3. Clean actions: same active state, with determinate progress for an actual bounded duplicate total.
4. Smart Categories: active `scanning` state, text retained.
5. Gallery Pro photo similarity: active `analyzing` state; media page load uses its existing indicator.

## Non-goals and safety
- No changes to scanner algorithms, data stores, indexes, permissions, Android Trash, retention or deletion, media playback, subscriptions or translations.
- Do not replace actual measurable progress with this indeterminate loader.
- No loops when there is no actual scan, no background processing and no false speed claims.

## QA gates
- [ ] Android CI exact-head build and JVM tests green.
- [ ] Visual screenshot / font-scale / landscape / dark mode inspection.
- [ ] Confirm cancellation/error hides loader immediately and retains proper error states.
- [ ] Confirm duplicate scan measured progress retains percentages.
- [ ] TalkBack announces indeterminate progress, respecting system animations off.
- [ ] Physical device performance and actual file scanning still needed before release.
