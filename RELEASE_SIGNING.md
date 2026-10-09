# Release signing baseline

Status: NOT CONFIGURED
Date: 2026-10-09

## Identity

Canonical applicationId selected for the reconstructed project:

`com.riccardopinato.omnicleaner`

The APK recovery identity `com.aistudio.foldercleaner.zjijip` is forensic-only and is not reused.

## Current build lanes

### Debug
- Android debug signing may be used locally/CI.
- applicationId suffix: `.debug`
- debuggable: true

### Release
- debuggable: false
- shrinking enabled
- no debug signing fallback
- no keystore material committed
- CI may build an unsigned release artifact until the INTERNAL signing identity is provisioned.

## Required before distribution certification

Create and preserve a dedicated INTERNAL signing identity outside the repository, then store only the required encrypted/secret material in the distribution environment.

Never commit:
- keystore files;
- store passwords;
- key passwords;
- private signing credentials.

Production signing and Play App Signing are separate release decisions and must be evidenced before STORE READY.
