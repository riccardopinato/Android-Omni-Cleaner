# Android Omni Cleaner — audit mirato contro operazioni distruttive
**Data:** 2026-11-27
**Ambito:** PR #77, ramo `integration/omni-v11-colors-gallery-qa-20261121`, revisione statica dei percorsi di scansione, selezione, pulizia, media, Safe Trash, Pro Batch, recupero e indicizzazione.
**Stato:** AUDIT STATICO CON CORREZIONI IN DRAFT; **NON CERTIFICATO SU DISPOSITIVO**; `main` invariato. Nessun dato dell'utente letto o modificato.
**Factory:** Master v27, Golden Index v31 (baseline della sessione). Processi pertinenti: Release Reality, Signing Identity, Immutable Artifact, Lifecycle/Trash; la loro applicabilità non implica certificazione runtime.

## Risultati: evidenza vs rischio

| ID | Priorità | Osservazione verificata nel codice | Decisione / correzione |
|---|---|---|---|
| DSAFE-01 | P0 | `SafeTrashManager.migrateLegacyTrashIfNeeded()`, invocata dalla UI, eliminava ricorsivamente la directory legacy dopo la copia. Un file estraneo in una voce storica non era coperto dalla validazione del payload. | **CORRETTO IN PR #77:** solo directory con esattamente `payload` e `original.properties`; rimozione nominativa non ricorsiva, fallimento conservativo |
| DSAFE-02 | P0 | `purgeEntry` cancellava ricorsivamente una voce dichiarata valida anche con contenuti ulteriori inattesi. | **CORRETTO:** rifiuto delle voci con file extra, symlink o directory impreviste; purge solo dei due file noti |
| DSAFE-03 | P0 | Un `payload` del Cestino cambiato dopo l'inserimento poteva essere eliminato definitivamente senza nuova verifica SHA. | **CORRETTO:** confronto dimensione e SHA-256 prima del purge; un digest diverso fa fallire la cancellazione |
| DSAFE-04 | P1 | Il riconoscimento di una migrazione già completata usava il digest del payload, ma non controllava l'identità del percorso originale del record legacy. | **CORRETTO:** confronto `originalPath`, `originalName`, `sizeBytes`, `lastModified` e, se presente, digest legacy |
| DSAFE-05 | P0 release-blocker | `ThumbnailFileCleanup.clean()` usa `File.delete()` direttamente e rimuove cartelle vuote. La pulizia delle miniature non passa dal Cestino reversibile. | **APERTO:** non approvare rilascio o uso indiscriminato finché non esiste una policy esplicita di rimozione irreversibile, una via recuperabile o test fisici rappresentativi |
| DSAFE-06 | P1 | `GalleryController.onTrashResult()` usa letture ripetute di `MediaStore.IS_TRASHED` e conserva le selezioni non verificate. Al contrario, `OmniFileTrashReview`, `StorageCleanupSection` e `SmartCategoriesSection` possono completare/aggiornare il flusso dopo `RESULT_OK` senza analogo readback per ogni URI. | **APERTO:** unificare le verifiche post-consenso; selezioni incerte conservate; nessuna riuscita apparente quando il provider restituisce `null` |
| DSAFE-07 | P1 | In `ProBatchController.onSystemTrashResult()` il consenso Android veniva interpretato come cleanup completato, potenzialmente pubblicando un evento/ad anche senza media verificato. | **CORRETTO:** il consenso non conta più come evento di pulizia; restano validi soltanto gli spostamenti di file/folder verificati |
| DSAFE-08 | P1 | `SafeTrashManager.restore` usa check del percorso e poi `renameTo`. Il percorso di destinazione viene controllato prima del rename, ma non esiste in questo audit prova di un primitive atomico no-overwrite su tutti gli Android/OEM. | **DA VALIDARE / possibile TOCTOU:** non attestare copertura race; test su collisione e process death, considerare fallback atomico controllato per API compatibili |
| DSAFE-09 | P1 | `MediaStore.createTrashRequest` sui media selezionati non applica in ogni percorso la stessa revisione di contenuto/identità presente nei `CleanupFileSnapshot`. | **DA INDAGARE:** rivalidazione subito prima del consenso, specialmente per selezioni vecchie e MediaStore URI riutilizzate |
| DSAFE-10 | P0 QA | Nessuna prova fisica del comportamento su Xiaomi/Pixel/Samsung, SAF revocato, Trash parziale, restore concorrente, aggiornamento dati e 50 MB. | **BLOCCO CERTIFICAZIONE**, non bug dimostrato |

## Protezioni già presenti nel codice osservato

- `CleanupSnapshotValidator` confronta bytes/mtime, SHA-256 quando atteso, symlink e percorsi rischiosi; per i duplicati verifica anche l'hash del keeper prima di muovere un candidato.
- `StorageCleanupEngine` impedisce il fallback irreversibile per file non multimediali troppo grandi; usa il Safe Trash per i file ammissibili. Le cartelle vuote vengono ricontrollate immediatamente prima di `File.delete()`.
- Le nuove scansioni `BackgroundIndexWorker` sono read-only, cancellabili e senza azioni di delete.
- `GalleryController` non considera `RESULT_OK` una prova sufficiente: verifica `IS_TRASHED` e mantiene gli elementi non verificati.
- `AppResidueScanner` produce candidati informativi. L'assenza del package non è prova di disinstallazione; nel flusso Pro i residui sono riportati, non eliminati.
- `SafeTrashSection` presenta conferme esplicite prima di singolo purge e svuotamento totale, ma la conferma UI non sostituisce le nuove verifiche del payload.

## Test aggiunti senza usare file personali

In `SafeTrashManagerTest.kt` quattro regressioni:
1. Una voce valida con un terzo file sconosciuto non può essere né purgata né ripristinata.
2. Una migrazione legacy con contenuto inatteso conserva integralmente i file originali.
3. Una collisione UUID con stesso payload ma metadati differenti non può consumare il record originale errato.
4. Anche se la lunghezza resta identica, un payload cambiato non può essere purgato.

**Nota sull'esecuzione:** il solo commit dei test NON è un PASS. Le pipeline exact-head Android CI, synthetic safety, Android functional matrix, R1 e visual QA vanno lette dopo completamento. Questo audit non ha eseguito operazioni su file reali del telefono.

## Policy release proposta

1. **Niente auto-delete di contenuti dell'utente:** WorkManager read-only, scansione e selezione non modificano i file.
2. **MediaStore:** consenso OS `RESULT_OK` ≠ file verificato nel Cestino; guardia read-after-write per tutti i percorsi.
3. **File locali:** preferire Safe Trash e snapshot; il purge richiede integrità verificata e conferma umana.
4. **Miniature:** cancellazione irreversibile segnalata come release-blocker finché non c'è percorso sicuro documentato e collaudato.
5. **No overwrite:** restore rifiuta destinazioni esistenti, con protezione ulteriore da valutare per collisione concorrente.
6. **Fail closed:** accesso revocato, hash non corrispondente, entry sconosciute, URI invisibili o richieste parziali NON equivalgono a successo.
7. **No auto-merge o signed-release:** serve stesso SHA testato + provenance APK + prove fisiche dei casi distruttivi.

## Checklist futura per test fisici (non richiesta ora all'utente)

- P0: file sintetico 50 MiB, Safe Trash -> restore byte-for-byte -> collision/no-overwrite -> purge.
- P0: MediaStore Trash con grant/deny, partial, SAF revocation e process death.
- P0: keeper duplicato modificato tra scan e cleanup; non perdere l'ultima copia.
- P1: thumbnail cache only in app test fixture, no real gallery assets; documentare reversibilità.
- P1: 360dp, 200% font, TalkBack, background interruption, exact APK upgrade/rollback.

**Decisione attuale:** restare in PR #77 Draft, non mergiare su `main`. Il codice è stato irrigidito sui percorsi Safe Trash, ma **non si può dichiarare zero rischio di perdita dati senza i gate rimanenti**.
