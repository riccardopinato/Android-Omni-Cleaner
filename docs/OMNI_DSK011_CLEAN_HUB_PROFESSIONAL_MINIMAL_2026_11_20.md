# Omni Cleaner — DSK-011 Clean Hub Professional Minimal

Approved by product owner on 2026-11-20. Status: **SOURCE IMPLEMENTED ON FEATURE BRANCH / CI AND DEVICE QA PENDING**.

## Source and priority
- `riccardopinato/Design-App/PER_APP/omni-cleaner.md`: DSK-011 Unslop UI, a subtraction/diagnostic reference (not a design system). Pinned upstream `felipebasurto/unslop-ui` commit `cd891160453f26aaacc569bad96a8d10d595076d`.
- Owner-approved follow-up to the Professional Minimal video filter WML-004, explicitly rejecting decorative AI-style gradients, repeated badges, colored borders and dots.
- Existing Omni Cleaner Product Bible and approved Home/broom brand take precedence; do not replace Home UI.

## Scope and verified source logic
- Only the `CleanHubToolGroup` composable changes visually. The nine existing destinations, group membership, existing strings and click callbacks are unchanged:
  - Analyze: Overview, Storage Intelligence, Large files, Duplicates
  - Review: Review Center, Folders, Safe Trash
  - Advanced: Filters, Advanced tools
- The full-width priority Overview tile remains full-width at normal font scaling, other tiles remain two-column, and width <360 dp or font scale >1.25 remains one-column.
- All nine `omni_clean_tool_${tool.name.lowercase(Locale.ROOT)}` accessibility/UI test tags remain unchanged.
- Removed 8 dp accent-colored decorative dots, tint-based tile borders, tinted container on Overview. Every tile now has a 1 dp neutral outline, 12 dp corner radius and monochrome original first-party vector icon mapped to its actual tool.
- Dark/light surface, text and borders take existing color-scheme values; no new UI dependency, gradients, emoji-glyph reliance or animation.
- Tile labels, secondary chevron, minimum card heights and click handlers retained. Icon is decorative and excluded from TalkBack; parent Card label remains localized.

## What is NOT changed
- No modification to cleaner scanner, storage indexes, Gallery, Safe Trash, media deletion, access permissions, navigation/back behavior, billing, monetization, files or databases.
- No replacement of previously approved rich Home/dashboard tiles.

## QA gates
- [ ] Exact HEAD Android CI source, JVM, lint, debug & release builds and AAB pass.
- [ ] Screenshot workflow pass; independently inspect the Clean page itself against baseline reference (CI capture does not by itself prove aesthetic preference).
- [ ] 360dp and 150% text scaling, dark theme, TalkBack content and target sizing.
- [ ] All nine routes navigable, return to Clean hub works, no clipped titles.
- [ ] Tests and physical APK review before shipping.

The changes deliberately consume DSK-011 as a *subtractive design QA aid* only. DSK-011 remains a reference, not a vendored component.
