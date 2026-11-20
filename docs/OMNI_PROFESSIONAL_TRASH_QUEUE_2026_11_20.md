# Omni Cleaner — Proposta 7: coda di eliminazione Professional Minimal

Approvata il 20 novembre 2026. Stato: **source implemented / exact-head checks pending**.

## Fonte grafica e consenso
- Design-App `DSK-011 Unslop UI`, riferimento sottrattivo per evitare ornamentazioni e visuali artificiali.
- Intervento circoscritto al dialogo di revisione dei file destinati al Cestino Android. Non cambia il modello di selezione né il gesto di conferma di Android.

## Implementazione Compose
- `OmniTrashQueueItem.kt`: due pittogrammi vettoriali Compose monocromatici foto/video, rappresentazione del nome del media con ellissi e metadati reali `media.formattedSize`, unmark con accessibilità e testTag per ID.
- `GalleryCleanerSection.kt`: conserva `AlertDialog` e `LazyColumn` con chiavi `media.id`; visualizza quantità selezionata, righe ordinate e una nota che informa che Android chiederà conferma.
- `onRemove` richiama esclusivamente `controller.removeFromTrashSelection(media.id)`, già usato prima della modifica. Dopo la rimozione dell'ultimo file, il dialogo si chiude; l'azione è reversibile tramite il normale flusso di revisione.
- Rimane invariato il bottone esterno di spostamento `gallery_move_android_trash`, la richiesta `controller.createTrashRequest()`, il `trashLauncher.launch(IntentSenderRequest)` e la callback `controller.onTrashResult(success)`.
- Struttura in superfici neutre, bordi sottili e angoli a 12 dp, niente gradienti e niente URI tecnici nell'elenco principale.
- EN, IT, ES, FR, PT.

## Requisiti di accettazione
- [ ] Exact-head Android CI PASS (Kotlin compile, lint, JVM, androidTest APK, release/AAB).
- [ ] Exact-head screenshot workflow PASS; ispezione specifica del dialogo con media fixture distinta dalla normale cattura schermate.
- [ ] Conferma Android ancora obbligatoria: nessun file può essere eliminato dalla schermata di sola revisione.
- [ ] Prova fisica con selezione multipla, rimozione ultima riga, >100 elementi/batching, revoca permessi, annullamento dialogo.
- [ ] Test TalkBack, font scale 150%, 360dp, dark mode e nomi lunghi.
- [ ] Nessuna modifica al motore di pulizia o all'identità dell'APK.

Non dichiarare la release certificata sulla sola base di CI o screenshot generici.
