# APK di collaudo — navigazione e anteprima media (16 novembre 2026)

Questo documento registra la richiesta di distribuzione del debug APK dal ramo audit/omni-safety-ux-360-20261115, senza modificare le funzionalita applicative o la versione RC1 in main.

## Evidenze a monte (sorgente applicativo)

- Android CI success: https://github.com/riccardopinato/Android-Omni-Cleaner/actions/runs/43855585871 (app code commit b22c2d015db8).
- Dieci schermate principali e di dettaglio: https://github.com/riccardopinato/Android-Omni-Cleaner/actions/runs/43855585935 (stesso codice applicativo).
- VideoView originale con MP4 sintetico e screenshot aggiuntivo, 11 immagini: https://github.com/riccardopinato/Android-Omni-Cleaner/actions/runs/43856934501 (commit fd2abe31ed72c3e57475c4ff638dfaca3a91f920, aggiornamento del solo harness di prova).
- AppLab PASS: https://github.com/riccardopinato/Android-Omni-Cleaner/actions/runs/43857284207 (commit 0d3cca049cc88da7d4e693466df78a5b754b47ee, aggiornamento del solo wrapper di test), inclusi Maestro, UI e font scale. Persistenza e storage mantengono 3 WARN per ui_hierarchy_missing, 0 errori.

## Distribuzione

- La pipeline Android CI gia configurata pubblica su ogni push del ramo audit una prerelease GitHub `fast-apk-<sha12>` con APK debug e checksum SHA-256 del commit esatto.
- L'APK deve essere distribuito solo dopo SUCCESS del nuovo Android CI e verifica effettiva dell'asset.
- Debug test installabile separatamente da release ufficiale; nessuna pretesa di certificazione Play, firma production o test fisico.
- PR #23 resta Draft, main RC1 invariato.

Non contiene dati sensibili.
