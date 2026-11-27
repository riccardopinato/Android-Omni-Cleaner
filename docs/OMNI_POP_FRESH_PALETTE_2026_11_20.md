# Omni Cleaner — Pop Fresh 2027 palette-only change

Status: SOURCE_CANDIDATE / VISUAL_AND_CI_QA_PENDING. No APK, no merge to main.

> **21/11/2026 — user-approved behavior change in PR #77:** the nine original
> colors remain exact, but the table below records the **historical semantic
> assignment only**. In the new version swatches belong to packed **positions**
> rather than features. A module can take a different color after the user
> reorders, resizes or hides tiles. See
> `docs/OMNI_POSITIONAL_COLOR_HARMONY_2026_11_21.md` for the active contract.

## Approved inputs
The user selected the nine HEX colors proposed for Android Omni Cleaner on 20 November 2026, and requested to change only colors for now while feature work proceeds in a different chat.

| UI tile / role | Swatch | Exact HEX |
|---|---|---|
| Device storage | Electric Blue | `#3C55E8` |
| Duplicates | Lime Pop | `#C9F25D` |
| Large files | Coral Pop | `#FF7A73` |
| Gallery | Aqua Fresh | `#5EDBD5` |
| Privacy | Soft Violet | `#C4A2FF` |
| Safe Trash | Tangerine | `#FFAE58` |
| Folders & categories | Spring Green | `#87DEAA` |
| Review & advanced | Sunshine | `#FFE066` |
| Pro | Bubblegum | `#FF83C2` |

Neutral background: `#F7F7F4`; ordinary text: `#192033`. On Electric Blue use pure white ink to maintain small-text contrast (calculated WCAG contrast approximately 5.74:1). No ad, status, danger or permission behavior is changed.

## Integration lineage
- Change starts from the existing Color Bento Home design PR #73 at `c226c4ebfc4287909e3e1133738a47b409d2c5b2`.
- Feature development proceeds independently in PR #75; no edits to that branch.
- Only Bento v2 visual tokens and their foreground contrast rendering are modified.
- Existing home layout, draggable resizing/edit mode, animation timing, navigation, scan logic, storage operations, data persistence, copy, localization and Monetization remain untouched.
- Existing older Bento v1 tokens are left unchanged intentionally because the current preview is Color Bento v2; avoid unrelated changes to other UI variants.
- Result is maintained as a stacked review PR targeting PR #73, to be reconciled with the eventual integrated feature branch before any release.

## Validation required before final integration
- Android exact-head CI and palette JVM unit test.
- Native screenshots: light/dark, 360dp, font scaling 200%, scan in progress, edit/resize guide and Pro/Privacy panels.
- Audit visual contrast, particularly blue tiles and navigation controls.
- Owner review and integration conflict checks against PR #75.
- Never claim a final APK, signed release or main merge from this palette-only change.
