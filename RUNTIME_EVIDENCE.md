# Runtime evidence lanes

## Step 3 bounded emulator parity

AppLab pin:
`dc826264c31dd79fc7617ab7a4a8aee61f9b5aba` — v4.5 Physical Evidence Hub.

The runtime job:
- builds the debug APK;
- prepares a deterministic storage fixture;
- grants the emulator all-files app-op;
- runs `.maestro/parity-storage.yaml`;
- enables bounded AppLab system, persistence and storage labs;
- retries only a known PID-loss crawler flake when there is no FATAL/ANR/OOM/fatal-signal evidence;
- publishes the exact passing APK as a GitHub pre-release with SHA-256.

Authority: `NON_TRUSTED_SMOKE`.

## Physical authority

`applab.project.json` requires physical validation for:
- storage;
- file picker / SAF;
- Billing.

See `parity/PHYSICAL_DEVICE_PLAN.md`.

Physical PASS requires the AppLab v4.5 Physical Evidence Hub and exact APK
binding. GitHub Actions artifact quota currently blocks the trusted
artifact-bound certification path; this does not block the non-trusted emulator
parity lane or GitHub Release delivery of the test APK.
