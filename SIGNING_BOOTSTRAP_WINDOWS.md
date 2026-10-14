# Android Omni Cleaner — Bootstrap signing su Windows

Status: **PRONTO / AZIONE LOCALE DEL PROPRIETARIO RICHIESTA**

Questa procedura crea la **upload key** permanente usata per firmare l'AAB prima
del caricamento su Google Play. Per una nuova app con Play App Signing, la upload
key resta sotto il controllo dello sviluppatore mentre Google Play gestisce la
app-signing key usata sugli APK distribuiti agli utenti.

Non inviare mai in chat:
- `.jks`;
- contenuto Base64 del keystore;
- password del keystore;
- password della chiave.

La fingerprint SHA-256 del certificato e' invece informazione pubblica.

## Prerequisiti

Sul PC Windows:
- JDK 17+ con `keytool` disponibile;
- clone aggiornato del repository;
- facoltativo: GitHub CLI `gh` gia' autenticato, se vuoi configurare GitHub in automatico.

Il repository ignora gia' `*.jks`, `*.keystore`, `keystore.properties` e
`secrets.properties`. Lo script salva comunque il materiale in
`%USERPROFILE%\OmniCleanerSigning`, fuori dal repository.

## 1. Crea la upload key

Dalla root del repository:

~~~powershell
powershell -ExecutionPolicy Bypass -File .\tools\bootstrap-release-signing.ps1
~~~

Lo script:
- crea una chiave RSA 4096 bit;
- usa un keystore JKS dedicato;
- esporta il certificato pubblico PEM;
- calcola la fingerprint SHA-256;
- genera il Base64 necessario al GitHub Secret;
- non stampa ne' salva le password in file.

Prima di proseguire crea **almeno due backup sicuri** di
`%USERPROFILE%\OmniCleanerSigning\omni-upload.jks` e registra le due password
nel tuo password manager.

## 2A. Configurazione GitHub automatica con gh

Dopo avere creato **e verificato i due backup** del keystore del punto 1, rilancia
lo script sulla stessa chiave:

~~~powershell
powershell -ExecutionPolicy Bypass -File .\tools\bootstrap-release-signing.ps1 -UseExisting -ConfigureGitHub -RunReleaseCandidate
~~~

`-UseExisting` impedisce qualsiasi rigenerazione e usa il keystore gia' presente.
Ti verranno richieste nuovamente le due password per validare/esportare la stessa
identita'.

`-RunReleaseCandidate` avvia il workflow con
`require_production_monetization=false`, quindi valida la firma prima di avere
gli ID AdMob di produzione.

Lo script rifiuta sia di sovrascrivere una chiave esistente senza
`-UseExisting`, sia di usare `-UseExisting` se il keystore non esiste.

## 2B. Configurazione GitHub manuale

Repository -> **Settings -> Secrets and variables -> Actions**.

Secrets:
- `ANDROID_KEYSTORE_BASE64`: contenuto di `ANDROID_KEYSTORE_BASE64.txt`;
- `ANDROID_KEYSTORE_PASSWORD`: password del keystore;
- `ANDROID_KEY_ALIAS`: `omni-upload` salvo alias diverso scelto volontariamente;
- `ANDROID_KEY_PASSWORD`: password della chiave.

Variable:
- `OMNI_EXPECTED_CERT_SHA256`: valore `certificate_sha256` presente in
  `SIGNING_IDENTITY.txt`.

## 3. Avvia il primo Release Candidate firmato

GitHub -> **Actions -> Android Release Candidate -> Run workflow**.

Per il primo controllo firma:
- branch: `main`;
- `require_production_monetization`: **false**.

La pipeline deve:
- eseguire locale parity + `lintRelease` + unit test + androidTest compilation;
- produrre APK e AAB release firmati;
- verificare separatamente il certificato dell'APK e dell'AAB;
- confrontare entrambi con `OMNI_EXPECTED_CERT_SHA256`;
- produrre SHA-256 degli artefatti;
- caricare APK, AAB e `RELEASE_IDENTITY.txt`.

## Gate successivo

Dopo il PASS del workflow:
1. conserva run ID, source SHA, fingerprint e SHA-256 artefatti;
2. crea/configura l'app in Google Play Console;
3. usa Play App Signing;
4. carica **l'esatto AAB** prodotto dal workflow in Internal Testing;
5. prosegui con Billing `premium_unlock`, AdMob/UMP e physical release QA.

SIGNING BOOTSTRAP COMPLETE != PLAY INTERNAL TESTING COMPLETE != STORE CERTIFIED.
