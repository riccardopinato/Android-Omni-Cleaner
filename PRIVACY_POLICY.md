# Android Omni Cleaner — Privacy Policy Draft

Status: **SOURCE-ALIGNED DRAFT — EXTERNAL PUBLISHING DETAILS REQUIRED**  
Last source audit: 2026-10-12

This draft describes the behavior implemented in the current Android source. It must be published at a stable public URL before Store release.

## Local storage analysis

Android Omni Cleaner scans user-accessible storage locally to provide storage overview, large-file review, duplicate detection, Smart Categories, thumbnail review, Gallery cleanup and EXIF privacy tools.

Depending on the feature, local processing can include:
- file/folder names and paths;
- file sizes and modification times;
- local content hashes used for duplicate/integrity verification;
- MediaStore metadata required to show and trash media;
- EXIF metadata selected by the user for removal.

Core cleaning data is not uploaded by Android Omni Cleaner.

## Deep Cleaning / All files access

Deep Cleaning is optional. On supported Android versions it can request Android's All files access so the app can perform full-device storage analysis that cannot be truthfully provided through scoped media access alone.

The app does not treat permission grant as proof that a cleanup succeeded. Selected files are revalidated before mutation.

## Gallery and Android Trash

Gallery review uses Android MediaStore. Selected media is sent to Android's system Trash flow and Android presents its own confirmation where supported.

## Safe Trash

Small supported non-media files can be moved to **Safe Trash** instead of being permanently deleted. Safe Trash is stored locally in hidden app-managed shared storage so the reversible copy can survive app uninstall/reinstall. It is not cloud backup and still occupies device storage until restored or permanently emptied.

## EXIF privacy

EXIF Privacy reads JPEG metadata locally and creates verified cleaned copies in a destination selected by the user. Originals are left unchanged by this workflow.

## Purchases

Google Play Billing is used for the one-time Premium entitlement. Android Omni Cleaner receives purchase/ownership state from Google Play; payment details are handled by Google Play and are not collected directly by the app.

A recent verified Premium state may be cached locally for a bounded offline grace period so a lifetime purchaser is not immediately locked out when Google Play is temporarily unreachable.

## Advertising and consent

Free mode can use Google Mobile Ads. Ad requests are disabled until:
- Premium ownership has been resolved;
- the user is not Premium;
- Google's consent system reports that ads may be requested.

Premium users generate no ad requests from the app's monetization layer.

Google Mobile Ads / UMP can process information according to Google's own policies and the consent choices presented to the user.

## Data retention

Local scan results are held only as needed for app workflows. Gallery session progress and user preferences can be stored locally.

Safe Trash payloads persist until the user restores or permanently deletes them, including across app uninstall because they live in shared storage.

## User controls

Users can:
- decline optional Deep Cleaning access;
- use scoped Gallery/EXIF features where available;
- change privacy choices when Google's UMP Privacy Options entry point is required;
- restore or permanently empty Safe Trash;
- restore eligible Premium ownership through Google Play.

## External details required before publication

- PRIVACY POLICY PUBLIC URL: `TODO_EXTERNAL`
- DEVELOPER/LEGAL NAME: `TODO_EXTERNAL`
- CONTACT EMAIL: `TODO_EXTERNAL`
- EFFECTIVE DATE: `TODO_EXTERNAL`

Do not publish this draft with unresolved `TODO_EXTERNAL` fields.
