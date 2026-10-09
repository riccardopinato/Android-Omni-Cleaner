# AGENTS.md — Android Omni Cleaner

## Canonical sources

Product truth:
1. PRODUCT_BIBLE.md
2. ROADMAP.md
3. PROJECT_BASELINE.yaml
4. RECOVERY_AUDIT.md

Factory baseline for this recovery:
- riccardopinato/App-Factory-Core
- SHA a4a97ca79e85d2faebcdcc9a3ae4a9644f5caf03
- Master v25.1

Do not substitute remembered older Factory rules.

## Development rules

- Android native Kotlin + Jetpack Compose is the preserved stack.
- Do not rewrite to Flutter unless a later explicit decision changes this.
- Main stays stable; substantial changes use branch/PR.
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
