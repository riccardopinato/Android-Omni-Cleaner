# Android Omni Cleaner — Release Evidence Template

Verdict: `BLOCKED | NOT CERTIFIED | CERTIFIED`

## Source identity

```text
applicationId=com.riccardopinato.omnicleaner
versionName=
versionCode=
sourceSha=
factoryBaseline=03406ef5e511dfc580b67967c67a11ab58e20141
factoryMaster=26.3
factoryGoldenIndex=15
```

## Signing identity

```text
signingLane=PRODUCTION
certificateSha256=
releaseWorkflowRun=
apkSha256=
aabSha256=
```

Evidence:
- [ ] permanent keystore created once;
- [ ] primary secure backup;
- [ ] second secure backup;
- [ ] backup verified readable;
- [ ] expected certificate fingerprint pinned;
- [ ] actual signer fingerprint matches;
- [ ] secrets absent from logs/artifacts.

## Build gates

- [ ] locale parity;
- [ ] lint;
- [ ] JVM unit tests;
- [ ] androidTest APK compilation;
- [ ] release APK;
- [ ] release AAB;
- [ ] signer verification;
- [ ] SHA-256 evidence.

## Step 9.7 physical performance

Attach/paste `OMNI_PERF_V1` report.

```text
device=
cold_scan_ms=
warm_scan_ms=
warm_persistent_hit=
warm_speedup_x=
hash_prewarm_ms=
hash_1_workers_mib_s=
hash_2_workers_mib_s=
hash_4_workers_mib_s=
battery_temp_start_c=
battery_temp_end_c=
thermal_start=
thermal_end=
selected_worker_policy=
```

- [ ] no unexpected file-count divergence;
- [ ] peak in-flight <= configured workers;
- [ ] worker policy chosen from measurements;
- [ ] no blocking thermal/performance issue.

## Physical storage/capability validation

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
testerAccount=
```

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

## Final decision

```text
blockingIssues=
warnings=
finalVerdict=
certifiedAt=
```

A build can be technically green and still remain BLOCKED by signing, physical
or provider evidence.
