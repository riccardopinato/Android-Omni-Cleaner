# Monetization contract — MAXI STEP 5

Status: **IMPLEMENTED — PLAY/ADMOB PRODUCTION VALIDATION PENDING**

## Commercial model

The recovered donor contract remains the source of truth for v1:

- product type: Google Play one-time INAPP;
- product id: `premium_unlock`;
- entitlement: lifetime Premium unlock while Google Play reports ownership;
- price: never hardcoded; localized price comes from Google Play;
- core cleaner: Free;
- Pro batch analysis: Premium;
- ads: Free only;
- Premium: no ad requests.

No subscription is introduced in Step 5. A subscription can be evaluated later as
a separate product decision.

## Billing lifecycle

- Billing Library: 9.1.0.
- one app-level BillingClient;
- automatic service reconnection enabled;
- ownership query at app startup;
- explicit Restore purchase action;
- PENDING never grants Premium;
- PURCHASED grants Premium;
- unacknowledged PURCHASED items are acknowledged;
- ITEM_ALREADY_OWNED triggers an authoritative ownership query;
- purchase cancellation is not shown as an error.

Current verification is client-side Play Billing ownership. Google recommends
server-side verification for stronger anti-fraud protection. This is a known
security trade-off, not hidden certification evidence.

## Ads / privacy

- UMP 4.0.0;
- consent information is refreshed before ads become eligible;
- `canRequestAds()` is required;
- unresolved entitlement => zero ad requests;
- Premium => zero ad requests;
- debug builds use Google's demo IDs;
- release ads remain disabled unless all three production AdMob IDs are supplied;
- store-bound builds can enforce configuration using
  `OMNI_REQUIRE_PRODUCTION_MONETIZATION=true`;
- banner uses an inline adaptive size suitable for scrollable content;
- donor interstitial cadence remains 3 completed cleanup actions + minimum 180 s.

## Free / Premium boundary

### Free
- storage analysis / cleaner core;
- folder scan;
- large files;
- duplicates;
- Safe Trash;
- Gallery Cleaner;
- EXIF Privacy;
- ads only after UMP and entitlement resolution.

### Premium
- everything in Free;
- Pro combined/batch analysis;
- no ad requests.

Safety features are never paywalled.
