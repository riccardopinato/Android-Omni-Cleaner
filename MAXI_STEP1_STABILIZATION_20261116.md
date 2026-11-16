# Maxi-step 1 — Stabilizzazione e sicurezza (2026-11-16)

Ambito: PR #23, branch `audit/omni-safety-ux-360-20261115`. Main RC1 e PR #22 non si modificano.

## Baseline verificata
- Build esatta: Android CI #43867570532 SUCCESS per `5d2b22d07a2f62f73c3becce8c75f37678f66e93`, APK debug SHA-256 `d95ebb37f638831718254f98b11168a6d3816c25e887eafa9057b2940719cd07`.
- AppLab precedente #43857284207 SUCCESS, senza crash, Maestro/UI PASS e font 1.3/1.5/2.0 PASS. Tre WARN `ui_hierarchy_missing` (2 persistence, 1 storage), non falsamente convertiti in PASS.
- Screenshot + video sintetico MP4: #43856934501, 11 screenshot inclusa la schermata video.
- Queste sono evidenze software; non autorizzano Play o un merge in RC1.

## Interventi di questo step (da verificare su nuovo head)
1. AppLab persistence/storage: configurazione repository-scoped con tempo di stabilizzazione di 5 secondi, testuale atteso `Smart cleaning`, segnalazioni genuine per errori e mancanza della gerarchia. `required=false` finché la stabilità di UIAutomator non viene dimostrata; nessun downgrade/azzeramento dei WARN.
2. Dopo il verifier e i test storage, due ulteriori cicli non distruttivi di `force-stop`, riapertura e navigazione Maestro attraverso Home, Clean, Duplicates, Gallery, Privacy e Back. **Un fallimento di questi cicli blocca l'intero AppLab job**.
3. Android CI verifica la sintassi shell, la validità JSON e il collegamento al percorso Maestro. I test AppLab continuano a includere `connectedDebugAndroidTest`, storage parity e font scale.
4. Nessuna modifica a meccanismi di pulizia, autorizzazioni Android, Safe Trash, Gallery Trash, SAF/EXIF, Billing/Ads o ai file dell'utente.

## Exit gate
- [ ] Android CI exact head: SUCCESS (lint, JVM, APK/AAB, static checks)
- [ ] AppLab exact head: SUCCESS, due nuovi restart/recovery journeys PASS
- [ ] Riesaminare WARN originali dei laboratori UIAutomator: se persistono, **non considerarli risolti**
- [ ] Android emulator: screenshot/MP4 evidence precedente ancora valido per codice applicativo invariato
- [ ] APK debug pubblicato da GitHub sul nuovo commit con digest verificato
- [ ] Test fisico esterno: MediaStore/SAF/EXIF, cestino e ripristino reale, TalkBack, dark mode, performance su seconda macchina
- [ ] Release/Play: firma di produzione, Billing, AdMob/UMP e autorizzazioni speciali; **non certificati**

Nessuna eliminazione automatica, nessun dato reale modificato dalle nuove prove.
