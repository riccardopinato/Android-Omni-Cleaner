# Runtime evidence lanes

## Step 3 bounded emulator parity — PASS

Final audited runtime evidence:

- source head before closure: `99d6938f689771f64bc28a0547afb7c76e9f5a24`;
- AppLab Runtime Smoke run: `38076397823`;
- AppLab v4.5 pin: `dc826264c31dd79fc7617ab7a4a8aee61f9b5aba`;
- package: `com.riccardopinato.omnicleaner.debug`;
- Maestro: PASS;
- Smart Visual QA: PASS;
- Multi-Screen Visual Journey: PASS;
- System UI Lab: PASS;
- Persistence & Restart: WARN, 0 errors;
- Storage & Data Integrity: WARN, 0 errors.

The controlled journey verified storage overview, duplicate discovery, large-file
discovery, thumbnail discovery and reachability of Gallery Cleaner / EXIF Privacy.

Authority: `NON_TRUSTED_SMOKE`.

## Test APK distribution

A GitHub prerelease is generated only after the runtime job succeeds. It contains:
- the exact debug APK exercised by the parity lane;
- a `.sha256` sidecar;
- source SHA and APK SHA-256 in the release notes.

This bypasses GitHub Actions artifact quota for manual/physical testing, but does
not create trusted AppLab artifact-bound certification.

## Physical authority

`applab.project.json` requires physical validation for:
- storage;
- file picker / SAF;
- Billing.

See `parity/PHYSICAL_DEVICE_PLAN.md`.

Emulator PASS is never physical-device PASS.
