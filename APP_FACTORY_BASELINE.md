# App Factory Baseline

Date: 2026-10-09

Factory source of truth:
- Repository: riccardopinato/App-Factory-Core
- Commit SHA: a4a97ca79e85d2faebcdcc9a3ae4a9644f5caf03
- Master Prompt: v25.1 FULL
- Golden Index: v10
- Golden Registry: v2

Relevant rules selected for this recovery:
- Assembly first / bootstrap discipline
- Capability / Permission / Health
- Android Signing / Release Identity
- Release Reality / Physical Validation
- AdMob Safe & Adaptive Monetization
- Entitlement Contract
- App Shell / Responsive / Visual QA
- Localization

Project-specific decision:
- Preserve Android native Kotlin + Jetpack Compose.
- Do not rewrite in Flutter without a concrete product or technical benefit.
- APK-derived behavior is forensic evidence, not automatically trusted production source.
- Main must remain stable; substantial recovery work proceeds by branch/PR.
