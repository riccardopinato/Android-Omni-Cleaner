# Android Omni Cleaner — app-ads.txt / AdMob Readiness

Status: **CODE READY / EXTERNAL CONFIGURATION PENDING**

The app deliberately does not invent production AdMob identifiers.

## Before production ads

Required external inputs:
- AdMob app ID;
- production banner ad unit ID;
- production interstitial ad unit ID;
- AdMob publisher ID for app-ads.txt;
- developer website/domain used by the store listing.

Gradle properties expected by the current release build:

```text
OMNI_ADMOB_APP_ID
OMNI_ADMOB_BANNER_ID
OMNI_ADMOB_INTERSTITIAL_ID
OMNI_REQUIRE_PRODUCTION_MONETIZATION=true
```

With the production guard enabled, missing AdMob IDs fail the release configuration instead of silently shipping an accidental production setup.

## app-ads.txt

Publish on the exact developer website domain referenced by the Store listing:

```text
https://<DEVELOPER_DOMAIN>/app-ads.txt
```

The file entry must be copied from the actual AdMob account.

```text
TODO_EXTERNAL_ADMOB_APP_ADS_ENTRY
```

Do **not** commit or publish a guessed publisher ID.

## Readiness gate

- [ ] app created/linked in AdMob;
- [ ] production IDs provisioned securely;
- [ ] UMP message configured;
- [ ] privacy policy URL published;
- [ ] developer website visible in Play listing;
- [ ] app-ads.txt served as plain text from the correct domain;
- [ ] AdMob reports app-ads.txt verified;
- [ ] audience/age treatment explicitly selected for this product;
- [ ] test devices/sample IDs used for every QA/manual test;
- [ ] Premium verified to produce zero ad requests;
- [ ] no-fill/network failure leaves UI functional;
- [ ] physical banner placement checked away from accidental-click zones.

Production advertising remains **BLOCKED** until these external items are evidenced.


## Provider re-check — 2026-10-14

Current AdMob guidance:
- app-ads.txt discovery is based on the developer website listed for the app in
  the Store;
- the file is expected at the developer-domain root path;
- the AdMob-provided personalized publisher line must be used, not a guessed ID;
- AdMob scanning/verification can take up to 24 hours after setup or listing changes;
- new AdMob apps require app-ads.txt verification before full ad serving.

Current UMP guidance:
- call `requestConsentInfoUpdate()` before relying on consent state;
- `canRequestAds()` remains false until that update has been requested;
- ad request work must avoid duplicate requests when consent checks complete
  through more than one callback/path;
- privacy options must be surfaced when UMP reports that they are required.

Official guidance:
https://support.google.com/admob/answer/9363762
https://developers.google.com/admob/android/privacy
