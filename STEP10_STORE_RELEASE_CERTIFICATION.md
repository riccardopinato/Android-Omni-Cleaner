# MAXI STEP 10 — Store & Release Certification

Status: **RELEASE PIPELINE PREPARED / EXTERNAL GATES BLOCKED**

This step converts a source-complete app into an artifact-bound, production
identity that can be tested through Google Play and eventually declared Store
Ready.

## Source-side work implemented

### AAB build gate

The full Android CI lane now compiles:
- debug APK;
- androidTest APK;
- release APK;
- release AAB.

This proves source/build compatibility only. The ordinary CI release artifact may
remain unsigned and is not a Store artifact.

### Persistent production signing lane

Manual workflow:
`.github/workflows/release-candidate.yml`

The workflow is fail-closed and requires:

GitHub Secrets:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

GitHub Actions variable:
- `OMNI_EXPECTED_CERT_SHA256`

When production advertising is required it also requires:

GitHub Secrets:
- `OMNI_ADMOB_APP_ID`
- `OMNI_ADMOB_BANNER_ID`
- `OMNI_ADMOB_INTERSTITIAL_ID`

The workflow:
1. refuses to run without persistent signing identity;
2. restores the keystore only in the runner temp directory;
3. runs locale parity, lint, JVM tests and androidTest compilation;
4. builds release APK + AAB;
5. verifies the APK signer;
6. compares actual certificate SHA-256 with the expected pinned fingerprint;
7. generates artifact SHA-256;
8. uploads signed APK, AAB and release-identity evidence;
9. removes the temporary keystore.

No debug-signing fallback exists.

## One-time user action gate — production key

Before the first signed release run:

1. generate one permanent release/upload keystore on a trusted local machine;
2. keep at least two secure copies;
3. store alias/passwords in a password manager;
4. extract the public certificate SHA-256 fingerprint;
5. create the four GitHub Secrets above;
6. create repository variable `OMNI_EXPECTED_CERT_SHA256`;
7. run **Android Release Candidate** manually;
8. retain source SHA, workflow run, certificate SHA-256 and artifact SHA-256.

Do not send the keystore or its passwords through chat.

## Google Play gate

Required outside source control:
- create the Play app for `com.riccardopinato.omnicleaner`;
- configure Play App Signing/upload key strategy;
- upload the exact signed AAB to Internal Testing;
- activate `premium_unlock`;
- add license testers;
- install from Play, not sideload, for Billing evidence;
- verify purchase, cancel, pending, acknowledgement, already-owned, restore after reinstall and revocation/refund;
- retain screenshots/logical evidence bound to the exact artifact.

## AdMob / UMP gate

Required outside source control:
- real AdMob app;
- banner + interstitial IDs;
- UMP Privacy & messaging configuration;
- developer website;
- published privacy policy;
- real app-ads.txt line;
- AdMob verification;
- physical banner/interstitial placement check;
- Premium verified to generate zero ad requests.

No production identifiers are fabricated in source.

## Physical capability gate

Must be executed on the exact release candidate:
- storage scan;
- All files access/Deep Cleaning disclosure and health;
- file picker / SAF export;
- Gallery / MediaStore trash;
- Safe Trash restore + permanent delete;
- Android partial-media access where applicable;
- performance Step 9.7 evidence;
- install/update-in-place with the same signing identity.

## Store declarations

Before final verdict:
- final Privacy Policy public URL;
- final developer/legal name and contact email;
- Data Safety reconciled against exact production SDK/configuration;
- MANAGE_EXTERNAL_STORAGE declaration justified against actual product behavior;
- screenshots/listing;
- content rating / audience selections;
- app-ads.txt verified where ads are enabled.

## Verdict rules

`CERTIFIED` only when:
- exact source SHA known;
- exact signed AAB/APK hashes known;
- production certificate fingerprint verified;
- full CI green;
- physical device evidence complete;
- Play Internal Testing evidence complete;
- Billing/provider checks complete;
- Store declarations complete.

`BLOCKED` while external provider/signing inputs are missing.

Current verdict: **BLOCKED — signing identity, Play Internal Testing, production Ads/UMP and final physical evidence not yet provisioned.**
