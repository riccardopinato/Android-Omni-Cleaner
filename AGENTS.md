# AGENTS.md — Android Omni Cleaner

## Canonical sources

Product truth:
1. PRODUCT_BIBLE.md
2. ROADMAP.md
3. PROJECT_BASELINE.yaml
4. RECOVERY_AUDIT.md

Historical Factory baseline for the original recovery (provenance, NOT current authority):
- riccardopinato/App-Factory-Core@03406ef5e511dfc580b67967c67a11ab58e20141
- Master v26.3 / Golden Index v15

CURRENT App Factory authority for new work:
- resolve App-Factory-Core/CURRENT.yaml once per session, then current Master, Golden Index, Registry and relevant Goldens;
- the 2026-11-15 audit pinned c5adc147046beb7024629466d10eeb799c3c2619 (Master v27, Index v29, Registry v2);
- a future session resolves CURRENT rather than assuming this audit pin is still current;
- RC1 remains frozen under RELEASE_FREEZE.md; audit UX goes to draft PR, not main.

Do not substitute remembered older Factory rules.

## Development rules

- Android native Kotlin + Jetpack Compose is the preserved stack.
- Do not rewrite to Flutter unless a later explicit decision changes this.
- Main stays stable; substantial changes use branch/PR.
- Feature branches use the pull-request gate as the single automatic CI authority; avoid duplicate push + PR runs unless a specific evidence requirement justifies both.
- PR gates must test the immutable source head, not silently substitute the synthetic merge ref.
- Local-first core.
- Destructive actions must be inspectable and verified.
- Never report cleanup success based only on a request/API call.
- Permission granted does not equal capability healthy.
- MANAGE_EXTERNAL_STORAGE must remain isolated and justified if used.
- Do not commit keystores, passwords, service-account files or private credentials.
- No live AdMob traffic in debug/QA.
- Billing/provider SDK calls must stay behind adapters/domain state.
- APK decompilation is evidence, not authoritative source code.

## Recovery discipline

For each recovered subsystem:
1. define contract;
2. reconstruct clean source;
3. test;
4. compare with APK behavior where possible;
5. document intentional differences.

Do not add unrelated features during recovery.
