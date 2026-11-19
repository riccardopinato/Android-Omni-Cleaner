# Omni Cleaner — Proposta 9: filtri video Professional Minimal

Approvazione: 20 novembre 2026.
Status: **source implemented, exact-head Android CI and screenshot checks pending**.

## Design-App e direzione visiva
- `DSK-011 Unslop UI` come audit grafico sottrattivo, già approvato per Omni Clean Hub.
- L'intervento rimuove i quattro `FilterChip` generici e introduce una barra proprietaria in una sola riga con quattro opzioni di uguale larghezza.
- Stile: superfici preesistenti, contorni sottili neutri, raggio 8dp, etichette compatte, stato attivo con bordo e peso tipografico e non con sfondi saturi, nessuna animazione.
- I testi dei quattro filtri restano quelli esistenti nelle cinque lingue EN/IT/ES/FR/PT.

## Contratto funzionale preservato
- `GalleryVideoReviewFilter` rimane immutato: `ALL`, `LARGE_100_MB`, `LONG_5_MIN`, `UHD_4K`.
- Identico stato `videoFilter`; `onSelect = { videoFilter = it }` rende l'interazione reale e conserva la funzione `filterGalleryVideos`, combinabile con ricerca Gentleface e con il Range Slider WML-004.
- La scelta è unica, come prima: non si possono selezionare simultaneamente due preset; l'intersezione con ricerca e dimensione invece resta possibile.
- Il controllo usa `selectable` con `Role.RadioButton` e stato `selected` accessibile, hit target minimo 48dp, testTag per ogni opzione.
- Non modifica permessi, MediaStore, risultati di analisi, Gallery Keep/Trash, lettore multimediale, prezzo premium né database.

## Gate
- [ ] Exact-head Android CI: test, lint, debug, release/AAB.
- [ ] Exact-head screenshot workflow: PASS, poi revisione specifica della riga dei filtri.
- [ ] Prova selezione dei quattro preset con 0 e N video caricati, combinazione con slider e ricerca e cambio tema.
- [ ] Responsive width 320/360dp, 150% text scaling, TalkBack e switch access.
- [ ] Integrazione in main solo con controlli verdi e senza conflitti; collaudo su telefono separato.
