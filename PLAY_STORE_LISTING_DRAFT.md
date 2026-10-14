# Google Play Store Listing — Draft

Status: **SOURCE-ALIGNED DRAFT / NOT YET PUBLISHED**

## App name

Android Omni Cleaner

## Short description

Find large files, duplicates and privacy risks with safe local cleanup.

## Full description

Android Omni Cleaner helps you understand and clean device storage without fake
RAM boosters, CPU cleaners or antivirus claims.

### See what uses your storage

Review:
- storage usage;
- large files;
- folders;
- exact duplicate files;
- old downloads;
- installer packages;
- archives;
- screenshots.

Results stay review-first. The app does not automatically delete your files in
the background.

### Safer cleanup

Supported non-media files can use Safe Trash so you can restore them before
permanent deletion.

Media cleanup uses Android's system Trash flow where supported.

Files are revalidated before destructive operations so an old scan result is
not treated as permanent deletion authority.

### Gallery review

Review photos/videos with focused media controls and Android permission-aware
access, including partial media access on supported Android versions.

### EXIF Privacy

Inspect JPEG metadata locally and create verified cleaned copies in a folder you
choose. The original is left unchanged by this workflow.

### Standard and Deep Cleaning

Many scoped features work without broad filesystem access.

Deep Cleaning is optional and is only requested when you explicitly choose
full-device file/folder analysis on Android versions where that capability
requires All files access.

### Local-first

Core file analysis, duplicate hashing and EXIF processing are performed on the
device. Omni Cleaner does not require a cloud backend for core cleaning.

### Premium

Premium is a one-time Google Play lifetime unlock for Pro batch tools and removes
ad requests from the app.

Pricing is provided by Google Play.

## Store disclosure notes

Do not publish until:
- final privacy-policy URL exists;
- developer/legal contact details are final;
- MANAGE_EXTERNAL_STORAGE declaration is submitted/approved if the full Deep
  Cleaning build is used;
- Data Safety matches the exact production artifact;
- production Ads/UMP/app-ads.txt are configured if ads are enabled;
- Premium product `premium_unlock` is active;
- exact release AAB has passed Internal Testing.

## Claims deliberately excluded

Do not add claims such as:
- boost RAM;
- speed up CPU;
- extend battery life;
- antivirus/malware protection;
- automatic safe deletion;
- cloud backup;
- AI cleanup.

Those are not part of the implemented product truth.
