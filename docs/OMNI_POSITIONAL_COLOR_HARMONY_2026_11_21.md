# Android Omni Cleaner — position-based chromatic harmony

Decision: **APPROVED by user, 21 November 2026**.
Implementation: **PR #77 / integration branch**, not production main.
Baseline: App-Factory-Core `4df94ac3b3ea22a2323634fe83c3c7770fe3b835` (CURRENT: Master v27, Golden Index v31).

## User contract

A Home Color Bento tile inherits the swatch of the **place where it appears**, not the function it implements. Reordering, resizing or hiding tiles changes the packed mosaic, and colors rebalance to stay harmonious. An unchanged mosaic must keep an unchanged palette. No randomness, external AI, network data or image-based color extraction.

## Deterministic native implementation

- `OmniBentoV2Layout.kt`: `bentoV2HarmonizedColorIndices(placements)` sorts the **committed visible positions** by (row, column). It scores the nine approved colors using a low-cost deterministic heuristic:
  - preserve the gentle editorial sequence as a baseline (Lime, Electric Blue, Tangerine, Violet, Sunshine, Aqua, Coral, Spring Green, Bubblegum);
  - encourage distribution instead of clustering one tone;
  - heavily penalize the same color on tiles sharing an edge;
  - penalize similar color families where rectangles share an edge, weighted by the *length of the shared edge* (wide/tall tiles matter).
- The function sees rectangles and positional coordinates. It never examines feature `id` to decide their color. Replacing/switching feature IDs while holding geometric positions fixed leaves the chromatic composition unchanged.
- `OmniBentoV2Home.kt`: uses `remember(displayed)` on the committed `bentoV2Pack(tiles)`, passes the assigned swatch directly to `BentoV2Card`. Drag and resize *preview motion* does not recalculate swatches; new colors apply only on a valid saved layout, avoiding flashing.
- `OmniColorBentoTokens.kt`: still owns the exact approved 9 HEX values. For text and icons, Electric Blue uses white; all other swatches use dark navy. Background stays `#F7F7F4`. Safety red/green, errors, permission confirmation, and action semantics are independent of decorative Home color.
- No new preference keys, migration, database schema, or media/file storage. Existing saved tile visibility, sizes, and order stay intact.

## Nine approved swatches (unchanged)

| Swatch | HEX | Color family |
|---|---|---|
| Lime Pop | `#C9F25D` | Green |
| Electric Blue | `#3C55E8` | Blue |
| Tangerine | `#FFAE58` | Warm |
| Soft Violet | `#C4A2FF` | Violet/pink |
| Sunshine | `#FFE066` | Warm |
| Aqua Fresh | `#5EDBD5` | Blue/aqua |
| Coral Pop | `#FF7A73` | Warm |
| Spring Green | `#87DEAA` | Green |
| Bubblegum | `#FF83C2` | Violet/pink |

## Acceptance and evidence required

- JVM: each approved color exact, foreground contrast choice, neutral background excluded; same mosaic gives same colors regardless of the feature identities / iteration order; physically adjacent tiles must not share an identical swatch; large cards count all shared edges; hidden tiles consume no color.
- Android exact-head: compile/debug tests/lint and safety/SQLite gates remain green, no changes to destructive actions or permission grants.
- Real screenshots: 360dp/200% font, normal light/dark Home, edit/rearrange and resized mosaics. Confirm reading order and contrast, not only screenshot generation.
- Physical device: TalkBack, large-font comfort, drag/reorder release and resize, persisted layout after restart. Never claim physical certification from emulator evidence.
- Any new source commit invalidates the earlier exact-head QA results until rerun.

No merge or signed APK is authorized by this document alone.


## Large-text narrow-title polish — 21 November 2026

Observed genuine 360dp / 200% screenshot: 1×1 tiles showed full
`Find duplicates` with `duplicates` broken mid-word, despite the taller
accessibility rows. Card width is still limited by the intentionally preserved
three-column layout; increasing vertical height alone cannot fix an
overlong single word.

Fix: when `tile.columns == 1` and Android font scale is at least 150%, use
one intentionally localized **brief visual title** from
`bento_v2_narrow_<feature>`; retain the original full `spec.title`
as `contentDescription` for TalkBack. All 16 resizable destinations have
short titles in EN/IT/ES/FR/PT. At normal text size and for cards two/three
columns wide, the full original title remains displayed. No automatic
in-word break, ellipsis-based abbreviation or font-scale override is used.
The card retains its approved 3-column geometry, colors, click behavior,
and saved layout.

Validation: the locale parity script rejects missing titles, spaces, and
visual titles longer than six Unicode characters; JVM tests require every
Bento definition to resolve its short resource. Native exact-head screenshot
of `24-home-360dp-200pct` must be reviewed to confirm `Dupes` and
`Large` render as whole words without internal splits. Emulator evidence
does not certify hardware TalkBack until physical accessibility QA.

## Current visual candidate — Soft Pastel Cleaner (27 November 2026)

The Pop Fresh swatches recorded above are **historical** and are **not** the current PR #77 implementation. `OmniColorBentoV2Palette` now uses nine low-saturation, positional pastels: pear `#DCEEC9`, powder blue `#D5E5FA`, peach `#F9E5D3`, lavender `#E9E2F6`, butter `#F8EFD2`, aqua `#D9EFF1`, coral `#F6DFDA`, mint `#DBEEE4` and pink `#F6E0EB`. Warm-paper surface `#F8F9F6`; readable dark ink `#263440` on every tile. The feature-to-color mapping remains based on geometry, never feature identity. Native illustrations are retained. The visual archive for `d7fb405` passed its capture workflow, but only physical-device review can certify 360dp / font-scale 2.0 readability, gesture operation and accessibility. Post-change exact-head screenshots and CI are separate gates.
