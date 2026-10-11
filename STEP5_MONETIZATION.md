# MAXI STEP 5 — Monetization Productization

Status: **IN PROGRESS — PROVIDER VALIDATION BLOCKED**

## Implemented

- Google Play Billing Library 9.1.0.
- One app-level BillingClient with automatic service reconnection.
- One-time product `premium_unlock`.
- Ownership query at startup and explicit Restore purchase.
- PENDING never grants Premium; PURCHASED does.
- Purchased unacknowledged items are acknowledged.
- Free core cleaner; Premium adds Pro batch and removes all ad requests.
- UMP 4.0.0 gates ads through `canRequestAds()`.
- Debug uses Google demo ads; release ads default OFF without production IDs.
- Inline adaptive banner for scrollable content.
- Donor interstitial cadence retained: 3 completed cleanups and 180 seconds.

## External validation still required

- Play Console product creation and activation.
- Persistent release signing identity and signed AAB.
- Internal Testing with license testers.
- Success/cancel/pending/restore/already-owned/refund tests.
- Production AdMob app/banner/interstitial IDs.
- Provider-side UMP Privacy & messaging setup.
- app-ads.txt on the final developer website.

## Security boundary

Purchase verification is currently client-side Play ownership. Google recommends server-side verification for stronger anti-fraud protection. This remains explicit future hardening, not claimed as already solved.
