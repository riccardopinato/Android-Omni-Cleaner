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
9. verify already-owned flow;
10. verify refund/revocation behavior;
11. verify acknowledgement.

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

Before monetized production:
- publish a developer website in the Play listing;
- publish the AdMob app-ads.txt line at the website root;
- wait for AdMob verification.

## Testing rule

Never click production ads during development. Use Google's demo units or an
AdMob-configured test app/device.

## Current blockers

- Play Console product not verifiable from CI;
- signed AAB/Internal Testing not yet completed;
- production AdMob IDs not supplied;
- UMP provider-side message not verifiable from source;
- app-ads.txt depends on the final developer website/store listing.
