# MAXI STEP 3 — APK Parity Audit

Status: **IN PROGRESS**

## Authority

Donor APK SHA-256:
`c77f48bbbac1d8e5e16ee44dab67346f3caa0f846bd117e5b05d2cb3b2a88310`

The reconstructed source is checked against the explicit contracts in
`parity/DONOR_CONTRACT.json`. Missing donor behavior is not inferred from
generic cleaner-app conventions.

## Evidence layers

1. Forensic donor evidence.
2. Deterministic JVM fixture.
3. Controlled Android emulator fixture.
4. AppLab v4.5 static/runtime evidence.
5. Physical-device evidence for storage/file-picker/billing.

## Runtime fixture

The emulator creates a known empty directory, two identical duplicates, one
same-size non-duplicate, a 12 MiB large-file candidate and a thumbnail fixture.

The parity journey verifies storage overview, duplicates, large-file filtering,
thumbnail discovery and reachability of Gallery Cleaner / EXIF Privacy.

No destructive confirmation is automatically accepted.

## Verdict vocabulary

`RECOVERED / IMPROVED / INTENTIONALLY_CHANGED / MISSING / BLOCKED`

Emulator PASS is never physical-device PASS.
