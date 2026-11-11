# Android Omni Cleaner — RC1 Release Evidence

Verdict: `BLOCKED`

## Source identity

```text
applicationId=com.riccardopinato.omnicleaner
versionName=1.0.0-rc1
versionCode=14
sourceSha=a753baa8ad0b647be747050ac7e2f6d1fde85465
factoryBaseline=5f6b2489df2ac85fb85ccfea059983a090689de4
factoryMaster=26.5
factoryGoldenIndex=17
```

## Signing identity

```text
signingLane=PRODUCTION
certificateSha256=270B2B2378DB5A7C2615FA775AD716377B91D7AFA826D14CD7258635DDAECDF1
apkCertificateSha256=270B2B2378DB5A7C2615FA775AD716377B91D7AFA826D14CD7258635DDAECDF1
aabCertificateSha256=270B2B2378DB5A7C2615FA775AD716377B91D7AFA826D14CD7258635DDAECDF1
releaseWorkflowRun=39869956007
workflowAttempt=1
apkSha256=df68083acea11f8e8cfe2bd2afb524ca1b770b5f5a011419c5783e35e6e6c2a1
aabSha256=2672307d2c1b91c94fc534fad067366f39877202a24388c96c3a00fda17bbb0d
```

Signing evidence:
- [x] permanent upload keystore created once;
- [ ] primary secure backup explicitly confirmed;
- [ ] second secure backup explicitly confirmed;
- [ ] backup restore/readability explicitly verified;
- [x] expected certificate fingerprint pinned in GitHub Actions variable;
- [x] restored CI keystore fingerprint matches expected fingerprint;
- [x] APK signer fingerprint matches expected fingerprint;
- [x] AAB signer fingerprint matches expected fingerprint;
- [x] APK and AAB certificate fingerprints match each other;
- [x] artifact SHA-256 values reproduced after artifact download;
- [x] secrets not emitted in inspected workflow logs/artifacts.

## Build gates — workflow run 39869956007

- [x] locale parity;
- [x] lintRelease;
- [x] JVM unit tests;
- [x] androidTest APK compilation;
- [x] signed release APK;
- [x] signed release AAB;
- [x] restored signing identity verification;
- [x] APK signer verification;
- [x] AAB signer verification;
- [x] SHA-256 evidence;
- [x] temporary CI keystore removed.

## Step 9.7 physical performance baseline

```text
files=87136
cold_scan_s=61.1
warm_scan_s=2.6
warm_speedup_x=23.5
hot_memory_scan_ms=1
selected_worker_policy=4
hash_4_workers_mib_s=1267
thermal_throttling=none_observed
```

This performance evidence predates the exact signed RC artifact and remains a
baseline, not a substitute for final signed-artifact physical QA.

## Exact signed RC physical capability validation

- [ ] install exact signed RC APK on physical device;
- [ ] Standard Cleaning without All files access;
- [ ] Deep Cleaning disclosure;
- [ ] All files access health;
- [ ] large-file scan;
- [ ] exact duplicate detection;
- [ ] duplicate cleanup with surviving keeper;
- [ ] Android MediaStore Trash confirmation;
- [ ] Safe Trash move;
- [ ] Safe Trash restore;
- [ ] Safe Trash permanent delete;
- [ ] SAF EXIF export;
- [ ] Android partial-media access when applicable;
- [ ] process restart/recovery;
- [ ] install/update-in-place with same signing identity.

## Play Billing / Internal Testing

```text
playTrack=internal
productId=premium_unlock
```

- [ ] exact AAB uploaded to Play Internal Testing;
- [ ] Play App Signing enrollment/identity recorded;
- [ ] tester opt-in/install from Play;
- [ ] successful non-consumable purchase;
- [ ] decline;
- [ ] pending -> purchased;
- [ ] pending -> cancelled;
- [ ] acknowledgement;
- [ ] already owned cannot repurchase;
- [ ] restore after reinstall;
- [ ] refund/revocation;
- [ ] valid offline grace after prior Play verification;
- [ ] grace expiry does not authorize ads/premium incorrectly.

## Ads / UMP

- [ ] production AdMob app ID;
- [ ] banner ID;
- [ ] interstitial ID;
- [ ] UMP message configured;
- [ ] consent update before ad request;
- [ ] `canRequestAds()` respected;
- [ ] Privacy Options shown when required;
- [ ] Premium produces zero ad requests;
- [ ] no-fill/network failure keeps app usable;
- [ ] banner/interstitial placement physically reviewed;
- [ ] developer website in Play listing;
- [ ] app-ads.txt published;
- [ ] app-ads.txt verified by AdMob.

## Store policy / declarations

- [ ] privacy policy public URL;
- [ ] developer/legal name;
- [ ] support/contact email;
- [ ] Data Safety completed from exact production artifact;
- [ ] third-party SDK behavior included;
- [ ] All files access declaration submitted if applicable;
- [ ] Play approval evidenced;
- [ ] content rating;
- [ ] audience/age treatment;
- [ ] final screenshots/listing.

## Current decision

```text
blockingIssues=backup evidence confirmation; Play Internal Testing; Billing; production Ads/UMP; signed-artifact physical QA; store declarations
warnings=none in signing identity evidence
finalVerdict=BLOCKED
certifiedAt=
```

Production signing is verified for this RC. Store certification remains blocked
until the external/provider and exact-artifact physical evidence above is complete.
