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


## Follow-up implementativo — 27/11/2026, chiusura source P0/P1 senza telefono

**Stato: SOURCE IMPLEMENTED / EXACT-HEAD CI PENDING; non equivalgono a collaudo OEM.** Aggiornato su PR #77, non su `main`.

### DSAFE-05: miniature — P0 risolta a livello sorgente
- `ThumbnailFileCleanup.clean()` non chiama più `File.delete()` né esegue sweep ricorsivi di directory.
- `ThumbnailDirectoryPolicy` riconosce soltanto `DCIM/.thumbnails`, `Pictures/.thumbnails` e `Android/media/<app>/.thumbnails`; esclusione di directory generiche, symlink, path fuori root e record obsoleti.
- `SafeTrashManager.moveVerifiedThumbnailToTrash` sposta snapshot con SHA-256 nel Cestino recuperabile persistente; su fallimento il file resta in sede oppure la copia verificabile nel Cestino viene preservata. La speciale deroga al divieto su file media è circoscritta a un file nella cartella thumbnail riconosciuta.
- L'interfaccia EN/IT/ES/FR/PT ora parla di **spostamento recuperabile e byte trattenuti**, non di cancellazione definitiva o spazio recuperato. In Maintenance e Pro Batch è iniettato il gestore Safe Trash, mentre un cleaner senza gestore rifiuta l'operazione.
- Test JVM: cache JPG recuperabile, file cambiato rifiutato, nessun gestore => nessuna operazione, cartella personale rifiutata, `Android/media` ordinario protetto, thumbnail sotto `Android/media` ammessa solo con hash e root validi.

### DSAFE-06: callback OS — P1 risolta a livello sorgente
- `MediaStoreHelper.verifyTrashedUris`: lettura `IS_TRASHED` ripetuta fino a tre volte per ogni URI; `null`/assenza/errore/false non sono successi.
- `OmniFileTrashReview`, `StorageCleanupSection`, `SmartCategoriesSection`, `ProBatchController` e `GalleryController` usano la verifica; selezioni incerte rimangono aperte, evitando `onFinished` che le eliminerebbe localmente.
- Gli eventi di pulizia riuscita si basano solo sui movimenti confermati (Safe Trash locale o URI verificati), non sul solo `RESULT_OK`.
- Test strumentale Android con media sintetico, URI inesistente, media non ancora nel Cestino, media poi messo nel Cestino.

### DSAFE-07/09: selezioni obsolete — P1 risolta a livello sorgente
- `StorageCleanupEngine.prepareSnapshots` lega URI + snapshot originale e prima del prompt riapplica `CleanupSnapshotValidator`, verifica il MediaStore URI e il nome/dimensione/stato non già nel Cestino.
- Nessun percorso raw-URI privo di snapshot rimane disponibile nell'engine di pulizia.
- `GalleryController.createTrashRequest` rifiuta selezioni salvate il cui MediaStore URI non corrisponde più a nome/dimensione originali o non è accessibile. I media cancellati o non leggibili non vengono dichiarati puliti.
- **Limite documentato:** esiste sempre una finestra di modifica tra verifica e callback OS; la conferma nativa e il controllo a posteriori restano obbligatori. Non si può impedire ogni edit contemporaneo di app terze senza un primitivo di lock di provider.

### DSAFE-08: restore no-overwrite — P1 risolta nel percorso Android, collaudo fisico da confermare
- `SafeTrashManager.restore` non usa più `exists()` seguito da `renameTo()` verso il file definitivo.
- Prova prima hard link `Os.link` (pubblicazione atomica senza sovrascrivere il path esistente); se il filesystem non supporta hardlink, apre la destinazione Android 24+ con `O_CREAT | O_EXCL | O_WRONLY | O_NOFOLLOW` e verifica il digest dopo la copia.
- Se la copia fallisce conserva il payload del Safe Trash, senza eliminare un eventuale file di destinazione la cui proprietà non è certa. Solo i test JVM usano `File.createNewFile()` come fallback perché la API Android native non è eseguibile nell'ambiente unitario.
- Il comportamento specifico OEM/FUSE/SAF deve ancora passare test runtime su hardware.

### DSAFE-10: test OEM/SAF — non dichiarabili conclusi senza dispositivo
- La matrice sintetica Android e gli unit test sono stati estesi ma non sostituiscono prove fisiche con permessi reali, SAF revocato, MediaStore del costruttore, interruzione, collisioni concorrenti e dati di upgrade.
- **Blocco distribuzione resta attivo:** PR Draft, nessun merge `main`, nessun APK firmato dichiarato certificato.
- Quando disponibili i risultati del nuovo exact-head: registrare singolarmente Android CI, R1, synthetic safety, Android functional acceptance, video/visual QA. Un PASS precedente sullo SHA vecchio non basta.

**Riferimento Android ufficiale:** MediaStore createTrashRequest imposta `IS_TRASHED` quando approvato; `QUERY_ARG_MATCH_TRASHED` rende leggibili anche le righe nel Cestino. `android.system.Os.link/open` supportano le operazioni native; l'uso rimane sempre condizionato ai vincoli del filesystem reale.
