# Release signing baseline

Status: **PIPELINE PREPARED / PRODUCTION IDENTITY NOT YET PROVISIONED**  
Date: 2026-10-14

## Identity

Canonical applicationId:

`com.riccardopinato.omnicleaner`

Current release candidate:
- versionName: `1.0.0-rc1`;
- versionCode: `14`.

The APK recovery identity `com.aistudio.foldercleaner.zjijip` is forensic-only
and is not reused.

## Debug lane

- debug signing only;
- applicationId suffix: `.debug`;
- debuggable: true;
- never accepted as Store/production identity.

## Release lane

Release source configuration now supports a persistent keystore supplied only at
runtime.

Gradle runtime inputs:
- `OMNI_RELEASE_STORE_FILE`
- `OMNI_RELEASE_STORE_PASSWORD`
- `OMNI_RELEASE_KEY_ALIAS`
- `OMNI_RELEASE_KEY_PASSWORD`
- `OMNI_REQUIRE_RELEASE_SIGNING=true`

When signing is required and any input is missing, the build fails.

No debug-signing fallback exists.

## GitHub production secrets

Manual workflow:
`.github/workflows/release-candidate.yml`

Required GitHub Secrets:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Required repository Actions variable:
- `OMNI_EXPECTED_CERT_SHA256`

The expected certificate SHA-256 is public identity metadata; the private key and
passwords are secrets.

## Windows bootstrap helper

For the Windows development machine, use:
- `SIGNING_BOOTSTRAP_WINDOWS.md`;
- `tools/bootstrap-release-signing.ps1`.

The helper creates the upload keystore outside the repository, exports the public
certificate/fingerprint, prepares the Base64 GitHub secret value and can
optionally configure GitHub through an authenticated `gh` session. Private
key material and passwords never belong in chat or source control.

## One-time key gate

Before the first production release candidate:

1. generate one permanent release/upload key on a trusted local machine;
2. use a strong Android-supported key, e.g. RSA 4096;
3. give it a stable alias;
4. keep at least two secure backups;
5. store passwords in a password manager/secret store;
6. verify the backup is readable with `keytool -list`;
7. record the certificate SHA-256 fingerprint;
8. configure GitHub Secrets and `OMNI_EXPECTED_CERT_SHA256`;
9. run **Android Release Candidate**.

Never:
- commit the keystore;
- send keystore/passwords through chat;
- generate a fresh key per build;
- use the debug key as production identity.

## Fail-closed signer verification

The release workflow:
- restores the keystore only under `$RUNNER_TEMP`;
- builds release APK + AAB;
- locates Android `apksigner`;
- reads the actual APK and AAB certificate SHA-256 fingerprints;
- normalizes and compares both against `OMNI_EXPECTED_CERT_SHA256`;
- fails on mismatch;
- generates SHA-256 for APK and AAB;
- records source SHA + workflow run + certificate SHA-256;
- removes the temporary keystore in an `always()` cleanup step.

## Evidence required before Store Ready

Record:

```text
applicationId
versionName
versionCode
sourceSha
workflowRun
certificateSha256
apkCertificateSha256
aabCertificateSha256
apkSha256
aabSha256
signingLane=PRODUCTION
```

Then validate:
- install/update-in-place with the same identity where applicable;
- Play App Signing/upload-key setup;
- exact signed AAB uploaded to Internal Testing.

CI GREEN != PRODUCTION SIGNING VERIFIED.  
PRODUCTION SIGNING VERIFIED != STORE READY.

Current production signing verdict: **BLOCKED — RC1 source is prepared, but the permanent key/secrets/fingerprint are not yet provisioned.**
