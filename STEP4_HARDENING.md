# MAXI STEP 4 — Safety & Storage Hardening

Status: **IN PROGRESS**

## Goals

1. Keep Standard Cleaning useful without broad filesystem access.
2. Make Deep Cleaning explicit, optional and policy-defensible.
3. Never mutate a file solely because an old scan selected it.
4. Never remove every copy in a duplicate group.
5. Never report trash movement as bytes already reclaimed.
6. Keep physical/device-only behavior outside emulator certification claims.

## Implemented contracts

### Capability health

- Android 11+: Deep Cleaning requires explicit All files access.
- Android 10: scoped storage is not represented as full-device access.
- Android 6–9: legacy read + write permissions are requested only for Deep Cleaning.
- unmounted shared storage disables storage-dependent lanes truthfully;
- Android 14+ selected photo/video access is modeled as PARTIAL rather than denied or falsely FULL.

### Destructive revalidation

Every selected large/duplicate file carries scan-time:
- path;
- byte length;
- modified timestamp;
- duplicate SHA-256 when applicable.

Immediately before mutation the app rechecks those values. Duplicate cleanup also
revalidates an unselected keeper against the same SHA-256 group.

### Path protection

Empty-folder cleanup blocks:
- storage/system roots;
- Android/Data/OBB boundaries;
- developer/system artifacts;
- symlinks;
- well-known top-level user collections such as DCIM, Download, Documents,
  Pictures, Movies and Music.

### Reclaimed-space truth

- Safe Trash: reversible movement, **not reclaimed bytes**.
- Android system trash: trash operation, **not reclaimed bytes**.
- thumbnail deletion: actual deleted bytes are measured.
- candidate/recoverable estimates remain estimates until the operation completes.

## Evidence required to close Step 4

- Android CI PASS;
- AppLab Intelligence PASS;
- AppLab bounded runtime PASS;
- no actionable safety finding left open;
- physical-only OEM/storage/SAF behavior remains explicitly BLOCKED where it
  cannot be exercised in CI.
