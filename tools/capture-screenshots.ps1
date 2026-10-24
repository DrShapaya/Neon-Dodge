param(
    [string]$Apk = "dist\NeonDodge-1.8.apk",
    [string]$PackageName = "com.isaev.neondodge",
    [string]$Avd = "NeonDodgeCapture",
    [string]$OutDir = "screenshots",
    [int]$EmulatorPort = 5564,
    [switch]$SkipEmulator
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
$apkPath = Join-Path $repo $Apk
$outPath = Join-Path $repo $OutDir
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
$emulator = Join-Path $sdk "emulator\emulator.exe"

if (!(Test-Path $adb)) { throw "adb.exe не найден: $adb" }
if (!(Test-Path $apkPath)) { throw "APK не найден: $apkPath" }
New-Item -ItemType Directory -Force $outPath | Out-Null

function Invoke-Adb([string[]]$Args) {
    & $adb @Args
    if ($LASTEXITCODE -ne 0) { throw "adb завершился с кодом ${LASTEXITCODE}: $($Args -join ' ')" }
}

function Save-Screenshot([string]$Name) {
    $target = Join-Path $outPath $Name
    & $adb @("exec-out", "screencap", "-p") > $target
    if ($LASTEXITCODE -ne 0) { throw "Не удалось сохранить $target" }
    Write-Host "Сохранён $target"
}

$device = (& $adb devices | Select-String "^emulator-$EmulatorPort\s+device")
if (!$device -and !$SkipEmulator) {
    if (!(Test-Path $emulator)) { throw "emulator.exe не найден: $emulator" }
    Start-Process -FilePath $emulator -ArgumentList @(
        "-avd", $Avd, "-no-snapshot", "-no-boot-anim", "-no-audio",
        "-gpu", "swiftshader_indirect", "-port", $EmulatorPort
    ) -WindowStyle Hidden | Out-Null
}

if (!$device) {
    $deadline = (Get-Date).AddMinutes(3)
    do {
        Start-Sleep -Seconds 3
        $device = (& $adb devices | Select-String "^emulator-$EmulatorPort\s+device")
    } while (!$device -and (Get-Date) -lt $deadline)
}
if (!$device) {
    throw "Эмулятор не появился в adb. Проверьте виртуализацию, ADB и имя AVD '$Avd'."
}

Invoke-Adb @("-s", "emulator-$EmulatorPort", "wait-for-device")
Invoke-Adb @("-s", "emulator-$EmulatorPort", "install", "-r", $apkPath)
Invoke-Adb @("-s", "emulator-$EmulatorPort", "shell", "monkey", "-p", $PackageName, "1")
Start-Sleep -Seconds 3
Save-Screenshot "menu.png"

# Меню рассчитано на портрет 1080×2400; кнопка «Играть» находится по центру ниже выбора режима.
Invoke-Adb @("-s", "emulator-$EmulatorPort", "shell", "input", "tap", "540", "1940")
Start-Sleep -Seconds 3
Save-Screenshot "gameplay.png"

Read-Host "Перейдите в игре на экран паузы и нажмите Enter"
Save-Screenshot "pause.png"
Read-Host "Завершите забег и откройте экран game over, затем нажмите Enter"
Save-Screenshot "game-over.png"

Write-Host "Готово. Эти файлы сняты через adb screencap с запущенного APK."
