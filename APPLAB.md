# AppLab integration

Pinned AppLab baseline:

- repository: `riccardopinato/AppLab`
- version family: v4.2 Evidence Graph
- SHA: `d3681e4d2b92631ce96fcfc80349a079cbed40bb`

## Lanes

### AppLab Intelligence
Runs on `main`, `recovery/**`, pull requests and manual dispatch.

Purpose:
- Product Analysis;
- UX/Product static review;
- Architecture & Data Intelligence;
- feature truth and source evidence.

This lane is advisory and cannot certify runtime behavior.

### AppLab Autonomous Review
Runs on pull requests and manual dispatch.

Purpose:
- immutable target resolution;
- native project build;
- trusted APK verification;
- runtime evidence;
- Product Contract reconciliation;
- State/Edge review;
- Evidence Graph / Analyst / experiment planning.

Runtime evidence is authoritative only inside AppLab's trusted evidence boundary.

## Recovery-specific rule

The lost-source APK is the donor/evidence source for reconstruction, while AppLab evaluates the newly reconstructed repository/artifact. A green AppLab result does not by itself prove parity with the donor APK; parity remains a separate explicit audit.
