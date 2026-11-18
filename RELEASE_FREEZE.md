> **Aggiornamento 18/11/2026 — distinzione sviluppo/release:** il congelamento RC1 descritto qui protegge lo snapshot storico e la sua identità di firma, **non** impone che la nuova linea di sviluppo v1.1 ritorni alla vecchia UI. Lo snapshot di `main` precedente alla promozione è conservato in `backup/omni-main-before-apk-baseline-20261118`; il commit esatto dell'APK fornito dall'utente è conservato in `backup/omni-user-approved-apk-837b66d8-20261118`. La nuova baseline di sviluppo proviene da `1f626905a58b124b41fff6be535ede5aa8f62e86`. Questa promozione NON costituisce una release certificata, NON autorizza modifiche distruttive e NON completa signing, Play, MediaStore OEM o validazione fisica. Per qualunque RC1 di produzione il freeze resta vincolante.

# Android Omni Cleaner — v1.0 RC1 Release Freeze

Status: **SOURCE FREEZE ACTIVE / SIGNED ARTIFACT PENDING**

Release candidate:
- applicationId: `com.riccardopinato.omnicleaner`
- versionName: `1.0.0-rc1`
- versionCode: `14`
- targetSdk: 36
- compileSdk: 37

## Freeze rule

Until v1.0 is certified, accepted source changes are limited to:
- release blockers;
- correctness or data-safety regressions;
- crashes / ANRs / build failures;
- Play policy compliance;
- signing / Billing / AdMob / UMP integration fixes;
- accessibility defects that block release;
- exact-release physical QA findings.

No new product feature enters RC1.

## Source audit summary

Checked before RC1:
- cleaner / Gallery / EXIF core: no TODO/FIXME/debug endpoint residue found;
- monetization layer: one AdMob diagnostic log found and restricted to debug;
- Performance Lab: retained for engineering evidence, hidden and blocked in release;
- release signing: fail-closed, no debug-key fallback; APK and AAB certificates are independently pinned;
- release minification/resource shrinking enabled;
- manifest package/permissions reviewed;
- targetSdk 36 matches the Google Play submission requirement in force from 31 August 2026;
- All files access remains isolated to explicit Deep Cleaning and requires Play declaration/review;
- recovery-era Safe Trash comments retained where they document migration compatibility.

## External gates

The next gate requires a permanent release/upload signing identity:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- repository variable `OMNI_EXPECTED_CERT_SHA256`

Private key material and passwords must never be committed or sent through chat.

After signing:
1. build exact signed APK + AAB;
2. verify certificate and artifact SHA-256;
3. upload the exact AAB to Play Internal Testing;
4. validate Billing `premium_unlock`;
5. configure/validate production AdMob + UMP + app-ads.txt;
6. run exact-RC physical capability QA;
7. reconcile privacy policy, Data Safety and MANAGE_EXTERNAL_STORAGE declaration;
8. issue final CERTIFIED / NOT CERTIFIED / BLOCKED verdict.

SOURCE FREEZE != SIGNED RELEASE != STORE CERTIFIED.
