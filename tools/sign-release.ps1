param(
    [Parameter(Mandatory = $true)][string]$InputApk,
    [Parameter(Mandatory = $true)][string]$OutputApk,
    [string]$Keystore,
    [string]$Alias,
    [string]$StorePass,
    [string]$KeyPass
)

$ErrorActionPreference = 'Stop'

$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) {
    $scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
}
$repoRoot = Split-Path -Parent $scriptRoot

# Параметры подписи: аргументы -> env NEONDODGE_* -> keystore.properties.
$props = @{}
$propsPath = Join-Path $repoRoot 'keystore.properties'
if (Test-Path $propsPath) {
    foreach ($line in Get-Content $propsPath -Encoding UTF8) {
        if ($line -match '^\s*([^#=]+?)\s*=\s*(.*)$') {
            $props[$matches[1].Trim()] = $matches[2].Trim()
        }
    }
}
if (-not $StorePass) { $StorePass = $env:NEONDODGE_STORE_PASSWORD; if (-not $StorePass) { $StorePass = $props['storePassword'] } }
if (-not $KeyPass) { $KeyPass = $env:NEONDODGE_KEY_PASSWORD; if (-not $KeyPass) { $KeyPass = $props['keyPassword'] } }
if (-not $Alias) { $Alias = $env:NEONDODGE_KEY_ALIAS; if (-not $Alias) { $Alias = $props['keyAlias'] } }
if (-not $Keystore) { $Keystore = $env:NEONDODGE_STORE_FILE; if (-not $Keystore) { $Keystore = $props['storeFile'] } }

if (-not $Keystore) { $Keystore = 'keystore\release.jks' }
if (-not [System.IO.Path]::IsPathRooted($Keystore)) {
    $Keystore = Join-Path $repoRoot $Keystore
}

foreach ($required in @(@('keystore', $Keystore), @('alias', $Alias), @('storePassword', $StorePass), @('keyPassword', $KeyPass))) {
    if (-not $required[1]) {
        throw "Missing $($required[0]): задайте параметр, переменную NEONDODGE_* или строку в keystore.properties"
    }
}
if (-not (Test-Path $Keystore)) {
    throw "Keystore not found: $Keystore"
}

$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if (-not $sdk) {
    $localProps = Join-Path $scriptRoot '..\local.properties'
    if (Test-Path $localProps) {
        $line = Get-Content $localProps | Where-Object { $_ -match '^\s*sdk\.dir\s*=' } | Select-Object -First 1
        if ($line) {
            $sdk = ($line -replace '^\s*sdk\.dir\s*=', '').Trim()
            $sdk = $sdk -replace '\\:', ':' -replace '\\\\', '\'
        }
    }
}
if (-not $sdk) {
    $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
}
$bt = Join-Path $sdk 'build-tools\36.1.0'
$aligned = Join-Path $env:TEMP 'neondodge-aligned.apk'

if (Test-Path $aligned) { Remove-Item $aligned -Force }
if (Test-Path $OutputApk) { Remove-Item $OutputApk -Force }

& "$bt\zipalign.exe" -p -f 4 $InputApk $aligned
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

& "$bt\apksigner.bat" sign `
    --ks $Keystore `
    --ks-key-alias $Alias `
    --ks-pass "pass:$StorePass" `
    --key-pass "pass:$KeyPass" `
    --v1-signing-enabled true `
    --v2-signing-enabled true `
    --v3-signing-enabled true `
    --out $OutputApk `
    $aligned
if ($LASTEXITCODE -ne 0) { throw 'apksigner sign failed' }

& "$bt\apksigner.bat" verify --min-sdk-version 23 --verbose $OutputApk
if ($LASTEXITCODE -ne 0) { throw 'apksigner verify failed' }

Remove-Item $aligned -Force
Write-Host ''
Write-Host "Signed APK: $OutputApk"
