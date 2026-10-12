# Google Play Data Safety — Source-Aligned Draft

Status: **DRAFT / REQUIRES FINAL SDK + PLAY CONSOLE VALIDATION**

This document is a preparation aid, not a substitute for the Google Play Data Safety form. Final answers must match the exact production artifact, enabled SDK configuration, privacy policy and Play Console declarations.

## Core app processing

Current source performs storage/media/EXIF analysis locally. Core cleaner logic does not contain an app backend or cloud upload path.

Local-only functional data can include:
- files/folders selected or scanned by the user;
- file paths/names/sizes/timestamps;
- cryptographic hashes for duplicate and integrity verification;
- media metadata from MediaStore;
- EXIF metadata.

## Third-party SDKs present

Production dependency set includes:
- Google Play Billing;
- Google Mobile Ads;
- Google User Messaging Platform.

Their production behavior must be reflected in the final Data Safety answers based on the final SDK versions, consent configuration and Google's current disclosures.

## Permissions requiring Store review

Current manifest includes:
- `MANAGE_EXTERNAL_STORAGE`;
- legacy storage permissions with SDK limits;
- `READ_MEDIA_IMAGES`;
- `READ_MEDIA_VIDEO`;
- `READ_MEDIA_VISUAL_USER_SELECTED`;
- network permissions for monetization/provider access.

All files access must be declared and justified using the actual Deep Cleaning functionality described in `STORAGE_PERMISSION_POLICY.md`.

## Safe Trash disclosure

Safe Trash stores reversible local file copies in hidden shared storage so they can survive uninstall. These copies remain user-owned device data and consume storage until restored or permanently deleted.

## Final release checklist

- [ ] build exact production AAB/APK;
- [ ] inventory final SDK versions;
- [ ] verify no undeclared analytics/crash SDK was introduced;
- [ ] reconcile Google Mobile Ads/UMP current Data Safety guidance;
- [ ] reconcile Play Billing current Data Safety guidance;
- [ ] confirm optional All files access declaration;
- [ ] confirm selected-photos behavior;
- [ ] publish final privacy policy URL;
- [ ] complete Play Data Safety form from the exact artifact;
- [ ] retain screenshots/export of submitted declarations in release evidence.

No unverified yes/no Data Safety answer is asserted in this draft.
