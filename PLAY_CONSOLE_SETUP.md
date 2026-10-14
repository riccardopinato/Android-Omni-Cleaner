# Google Play / AdMob setup — Omni Cleaner

Step 5 prepares the app code but cannot create provider-side products or ad units
without the owner's Play Console / AdMob configuration.

## Google Play Billing

Package:
`com.riccardopinato.omnicleaner`

Create and activate a one-time product:

- product id: `premium_unlock`;
- type: one-time / non-consumable lifetime unlock;
- price: configure in Play Console by market; do not hardcode it in the app.

Before production:
1. provision the persistent release signing identity;
2. upload an AAB to Internal Testing;
3. add license testers;
4. verify successful purchase;
5. verify user cancel;
6. verify PENDING -> PURCHASED;
7. verify PENDING -> cancelled;
8. verify restore after reinstall;
9. verify Premium offline grace after a prior verified purchase;
10. verify grace expiry returns to unresolved/no-ads until Play answers;
11. verify already-owned flow;
12. verify refund/revocation behavior;
13. verify acknowledgement.

Google recommends server-side purchase verification for stronger security. The
current v1 implementation is intentionally client-side and this limitation must
remain visible in release review.

## AdMob

Create the Android app in AdMob using the production package and create:
- one banner unit;
- one interstitial unit.

Provide these Gradle properties in the protected release environment:

`OMNI_ADMOB_APP_ID`
`OMNI_ADMOB_BANNER_ID`
`OMNI_ADMOB_INTERSTITIAL_ID`
`OMNI_REQUIRE_PRODUCTION_MONETIZATION=true`

If the IDs are absent, release compilation remains possible for non-store
validation, but `ADS_ENABLED=false`. A store-bound production build must enable
the guard above.

## UMP / privacy

Create the required Privacy & messaging configuration in AdMob. The app calls
`requestConsentInfoUpdate()` and only enables ads when UMP
`canRequestAds()` is true.

The privacy-options entry point is exposed when UMP reports it as required.

## app-ads.txt

Follow `APP_ADS_SETUP.md`. Before monetized production:
- publish a developer website in the Play listing;
- publish the real AdMob app-ads.txt line at the website root;
- wait for AdMob verification;
- do not invent publisher IDs in source.

## Testing rule

Never click production ads during development. Use Google's demo units or an
AdMob-configured test app/device.

## Current blockers

- Play Console product not verifiable from CI;
- signed AAB/Internal Testing not yet completed;
- production AdMob IDs not supplied;
- UMP provider-side message not verifiable from source;
- app-ads.txt depends on the final developer website/store listing.


## Privacy / Data Safety source package

Use the source-aligned drafts as the starting point for provider/store configuration:
- `PRIVACY_POLICY.md`
- `DATA_SAFETY.md`
- `APP_ADS_SETUP.md`

The final public URL, legal/developer identity, contact email, AdMob publisher ID and final Play declarations remain external inputs.


## Release candidate workflow

Use `.github/workflows/release-candidate.yml` only after the permanent signing
identity has been created and backed up.

The workflow requires the four standard Android signing secrets plus repository
variable `OMNI_EXPECTED_CERT_SHA256`. It builds and verifies signed APK/AAB
artifacts and records their hashes.

An AAB produced by ordinary CI is only a compile gate. Upload to Play Internal
Testing must use the exact signed AAB from the release-candidate lane.

See `RELEASE_SIGNING.md` and `STEP10_STORE_RELEASE_CERTIFICATION.md`.
