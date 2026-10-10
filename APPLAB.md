# AppLab integration

Pinned AppLab baseline:

- repository: `riccardopinato/AppLab`
- version family: v4.2 Evidence Graph
- SHA: `d3681e4d2b92631ce96fcfc80349a079cbed40bb`

## Lanes

### AppLab Intelligence
Runs on `main`, `recovery/**`, pull requests and manual dispatch.

It executes the pinned AppLab analyzer directly and prints the complete Markdown report to the workflow log and job summary.

Purpose:
- Product Analysis;
- UX/Product static review;
- Architecture & Data Intelligence;
- Feature Truth and source evidence.

This lane intentionally does not upload a GitHub Actions artifact because the account artifact quota is currently saturated. The analysis itself remains deterministic and the JSON/Markdown evidence is validated inside the job before completion.

### AppLab Autonomous Review
Kept as a manual workflow while GitHub artifact storage is saturated.

Purpose:
- immutable target resolution;
- native project build;
- trusted APK verification;
- runtime evidence;
- Product Contract reconciliation;
- State/Edge review;
- Evidence Graph / Analyst / experiment planning.

The trusted runtime pipeline depends on GitHub Actions artifacts as part of its evidence boundary. It must not be represented as successful while artifact upload is blocked.

## Recovery-specific rule

The lost-source APK is the donor/evidence source for reconstruction, while AppLab evaluates the newly reconstructed repository/artifact.

A green AppLab static result does not prove parity with the donor APK. APK parity remains a separate explicit audit.
