[CmdletBinding()]
param(
    [string]$Repository = "riccardopinato/Android-Omni-Cleaner",
    [string]$Alias = "omni-upload",
    [switch]$ConfigureGitHub,
    [switch]$RunReleaseCandidate
)

$ErrorActionPreference = "Stop"

function ConvertTo-PlainText {
    param([Security.SecureString]$SecureString)

    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureString)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Read-ConfirmedPassword {
    param([string]$Label)

    while ($true) {
        $firstSecure = Read-Host "$Label (minimo 12 caratteri)" -AsSecureString
        $secondSecure = Read-Host "Conferma $Label" -AsSecureString
        $first = ConvertTo-PlainText $firstSecure
        $second = ConvertTo-PlainText $secondSecure

        if ($first.Length -lt 12) {
            Write-Warning "Usa almeno 12 caratteri."
            continue
        }

        if ($first -ne $second) {
            Write-Warning "Le password non coincidono."
            continue
        }

        return $first
    }
}

$keytool = Get-Command keytool -ErrorAction Stop
$userHome = [Environment]::GetFolderPath("UserProfile")
$outputDir = Join-Path $userHome "OmniCleanerSigning"
$keystorePath = Join-Path $outputDir "omni-upload.jks"
$certificatePath = Join-Path $outputDir "omni-upload-certificate.pem"
$base64Path = Join-Path $outputDir "ANDROID_KEYSTORE_BASE64.txt"
$summaryPath = Join-Path $outputDir "SIGNING_IDENTITY.txt"

New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

if (Test-Path $keystorePath) {
    throw "Esiste gia' un keystore in $keystorePath. Non sovrascriverlo. Verifica se e' gia' la chiave permanente del progetto."
}

Write-Host ""
Write-Host "Creazione upload key permanente per Android Omni Cleaner."
Write-Host "Il keystore verra' salvato FUORI dal repository:"
Write-Host "  $keystorePath"
Write-Host ""
Write-Host "Conserva almeno due backup sicuri del file .jks e le password in un password manager."
Write-Host ""

$storePassword = Read-ConfirmedPassword "Password keystore"
$keyPassword = Read-ConfirmedPassword "Password chiave"

$env:OMNI_BOOTSTRAP_STORE_PASS = $storePassword
$env:OMNI_BOOTSTRAP_KEY_PASS = $keyPassword

try {
    $genArgs = @(
        "-genkeypair",
        "-v",
        "-keystore", $keystorePath,
        "-storetype", "JKS",
        "-alias", $Alias,
        "-keyalg", "RSA",
        "-keysize", "4096",
        "-validity", "10000",
        "-dname", "CN=Omni Cleaner Upload Key, O=Omni Cleaner, C=IT",
        "-storepass:env", "OMNI_BOOTSTRAP_STORE_PASS",
        "-keypass:env", "OMNI_BOOTSTRAP_KEY_PASS"
    )
    & $keytool.Source @genArgs
    if ($LASTEXITCODE -ne 0) {
        throw "keytool -genkeypair e' terminato con codice $LASTEXITCODE."
    }

    $exportArgs = @(
        "-exportcert",
        "-rfc",
        "-keystore", $keystorePath,
        "-alias", $Alias,
        "-file", $certificatePath,
        "-storepass:env", "OMNI_BOOTSTRAP_STORE_PASS"
    )
    & $keytool.Source @exportArgs
    if ($LASTEXITCODE -ne 0) {
        throw "keytool -exportcert e' terminato con codice $LASTEXITCODE."
    }

    $listArgs = @(
        "-list",
        "-v",
        "-keystore", $keystorePath,
        "-alias", $Alias,
        "-storepass:env", "OMNI_BOOTSTRAP_STORE_PASS",
        "-J-Duser.language=en",
        "-J-Duser.country=US"
    )
    $listing = & $keytool.Source @listArgs
    if ($LASTEXITCODE -ne 0) {
        throw "keytool -list e' terminato con codice $LASTEXITCODE."
    }

    $listingText = $listing -join [Environment]::NewLine
    $fingerprintMatch = [regex]::Match($listingText, "SHA256:\s*([0-9A-Fa-f:]+)")
    if (-not $fingerprintMatch.Success) {
        throw "Impossibile estrarre la fingerprint SHA-256 dal certificato."
    }

    $fingerprint = $fingerprintMatch.Groups[1].Value.ToUpperInvariant()
    $base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath))
    [IO.File]::WriteAllText($base64Path, $base64, [Text.Encoding]::ASCII)

    $summary = @(
        "repository=$Repository",
        "alias=$Alias",
        "certificate_sha256=$fingerprint",
        "keystore_path=$keystorePath",
        "certificate_pem=$certificatePath",
        "base64_secret_file=$base64Path"
    )
    $summary | Set-Content -Path $summaryPath -Encoding UTF8

    Write-Host ""
    Write-Host "Upload key creata."
    Write-Host "SHA-256 certificato: $fingerprint"
    Write-Host "Certificato pubblico: $certificatePath"
    Write-Host "Base64 per GitHub Secret: $base64Path"
    Write-Host "Riepilogo non segreto: $summaryPath"
    Write-Host ""

    if ($ConfigureGitHub) {
        $gh = Get-Command gh -ErrorAction SilentlyContinue
        if ($null -eq $gh) {
            throw "GitHub CLI (gh) non trovato. Installa/autentica gh oppure configura i valori manualmente in GitHub."
        }

        & $gh.Source auth status
        if ($LASTEXITCODE -ne 0) {
            throw "GitHub CLI non autenticato. Esegui 'gh auth login' e rilancia con -ConfigureGitHub."
        }

        $base64 | & $gh.Source secret set ANDROID_KEYSTORE_BASE64 --repo $Repository
        if ($LASTEXITCODE -ne 0) { throw "Impostazione ANDROID_KEYSTORE_BASE64 fallita." }

        $storePassword | & $gh.Source secret set ANDROID_KEYSTORE_PASSWORD --repo $Repository
        if ($LASTEXITCODE -ne 0) { throw "Impostazione ANDROID_KEYSTORE_PASSWORD fallita." }

        $Alias | & $gh.Source secret set ANDROID_KEY_ALIAS --repo $Repository
        if ($LASTEXITCODE -ne 0) { throw "Impostazione ANDROID_KEY_ALIAS fallita." }

        $keyPassword | & $gh.Source secret set ANDROID_KEY_PASSWORD --repo $Repository
        if ($LASTEXITCODE -ne 0) { throw "Impostazione ANDROID_KEY_PASSWORD fallita." }

        & $gh.Source variable set OMNI_EXPECTED_CERT_SHA256 --repo $Repository --body $fingerprint
        if ($LASTEXITCODE -ne 0) { throw "Impostazione OMNI_EXPECTED_CERT_SHA256 fallita." }

        Write-Host "Secrets/variable GitHub configurati."

        if ($RunReleaseCandidate) {
            & $gh.Source workflow run release-candidate.yml --repo $Repository -f require_production_monetization=false
            if ($LASTEXITCODE -ne 0) {
                throw "Avvio Android Release Candidate fallito."
            }

            Write-Host "Workflow Android Release Candidate avviato senza monetizzazione production obbligatoria."
        }
    }
    else {
        Write-Host "GitHub NON e' stato modificato."
        Write-Host "Dopo avere creato due backup sicuri, configura GitHub manualmente"
        Write-Host "oppure usa i valori descritti in SIGNING_BOOTSTRAP_WINDOWS.md."
    }
}
finally {
    Remove-Item Env:OMNI_BOOTSTRAP_STORE_PASS -ErrorAction SilentlyContinue
    Remove-Item Env:OMNI_BOOTSTRAP_KEY_PASS -ErrorAction SilentlyContinue
    $storePassword = $null
    $keyPassword = $null
}
