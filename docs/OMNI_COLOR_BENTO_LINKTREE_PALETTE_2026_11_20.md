# Color Bento — Linktree visual reference and palette provenance

Source inspected 2026-11-20: https://uicolours.com/sites/linktree (UIColours, captured 2025-12-09).
The page presents these seven colors as the primary marketing-site palette:

| Role in Omni Cleaner (proposed) | Hex | Source |
|---|---|---|
| Action/highlight lime | `#D2E823` | UIColours |
| Deep forest contrast | `#254F1A` | UIColours |
| Bordeaux editorial accent | `#780016` | UIColours |
| Pastel pink / lilac card | `#E9C0E9` | UIColours |
| Saturated blue accent | `#2665D6` | UIColours |
| Sage background | `#E8EFD6` | UIColours |
| Primary graphite ink | `#1E2330` | UIColours |

User-supplied reference images and Lens analysis additionally suggest chalk `#F3F3F1`, white `#FFFFFF`, dark cobalt `#061492`, olive `#70774F`, and secondary violet/ochre/cyan. These **are not** among UIColours' seven listed tokens. Keep distinct source levels.

## Art direction

- Home uses chalk `#F3F3F1` or sage `#E8EFD6` as the main canvas. Burgundy is confined to selected editorial/stack surfaces.
- Color blocking belongs to components; avoid decorative gradients, repetitive glows or meaningless 3D.
- Space Grotesk for display/numbers, Manrope for operational labels/body; original fonts bundled locally under SIL OFL.
- Real storage and scan metrics only, never hardcoded marketing statistics. Unknown/unscanned shows unavailable state.
- Native Compose cards, accessibility semantics, existing navigation destinations, unchanged MediaStore and Android Trash safety.
- 360dp/large-font/dark modes require screenshot review before design acceptance. Green CI is not visual equivalence.
- Linktree source is *reference-only*: no cloned marks, brand images, proprietary typeface or marketing illustration assets.

## Design review status

Reference palette reconciled; composed implementation still **in progress** in a separate draft design branch. A screenshot acquisition by CI is not sufficient to claim the user's reference has been matched. Main and production APK unchanged.
