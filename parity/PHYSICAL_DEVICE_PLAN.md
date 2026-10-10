# Step 3 — Physical Device Parity Plan

Authority: AppLab v4.5 Physical Evidence Hub.

No scenario below is PASS yet. Emulator/runtime evidence cannot satisfy these
checks.

## Required target binding

Before a physical verdict can be trusted, record:

- immutable source SHA;
- exact APK SHA-256;
- package id;
- version name/code;
- phone manufacturer/model;
- Android version/build.

The APK tested on the phone must be the exact Step 3 parity pre-release asset.

## STORAGE-01 — broad storage scan

Capabilities: `storage`

Preconditions:
- install parity APK;
- grant all-files access through the app's guided settings flow;
- create known empty, duplicate, large-file and thumbnail fixtures.

Checks:
1. storage overview completes without crash/ANR;
2. known duplicate group is found;
3. 10 MB filter finds the known large file;
4. empty/small folder classification is consistent;
5. thumbnail fixture is discovered;
6. no destructive action occurs during scan.

## STORAGE-02 — safe cleanup

Capabilities: `storage`

Checks:
1. duplicate review always retains a keeper;
2. small non-media cleanup enters Safe Trash;
3. stale/changed thumbnail candidates are rejected;
4. dangerous/root paths cannot be selected for deletion;
5. reported reclaimed bytes correspond to completed operations.

## GALLERY-01 — MediaStore review

Capabilities: `storage`

Checks:
1. photo/video permission flow works on the physical Android version;
2. Gallery Cleaner loads real media;
3. Keep and Trash decisions advance correctly;
4. Undo restores the last pre-trash decision;
5. app confirmation appears before Android system trash confirmation;
6. cancellation leaves media untouched.

## EXIF-01 — SAF safe-copy export

Capabilities: `file_picker`

Checks:
1. select a real source folder through SAF;
2. scan a JPEG containing selected EXIF metadata;
3. select a different destination folder;
4. export cleaned copy;
5. original remains byte-present and unmodified;
6. cleaned copy removes selected tags while preserving orientation/image data.

## BILLING-01 — one-time Premium

Capabilities: `billing`

Requires Google Play Internal Testing.

Checks:
1. product id is `premium_unlock`;
2. product is one-time INAPP;
3. purchase activates Premium;
4. purchase is acknowledged;
5. reinstall/restore resolves Premium ownership;
6. Premium prevents ad requests, not only visual display.

## Verdict rule

Each scenario ends in exactly one of:

`PASS / FAIL / BLOCKED`

A BLOCKED scenario stays BLOCKED. It is never inferred as PASS from emulator,
static analysis or source inspection.
