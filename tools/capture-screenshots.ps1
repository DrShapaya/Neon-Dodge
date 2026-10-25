param(
    [string]$Apk = "dist\NeonDodge-1.9.apk",
    [string]$PackageName = "com.isaev.neondodge",
    [string]$Avd = "",
    [string]$OutDir = "screenshots",
    [int]$EmulatorPort = 5556,
    [switch]$SkipEmulator
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
$apkPath = Join-Path $repo $Apk
$outPath = Join-Path $repo $OutDir
$sdkCandidates = @(
    $env:ANDROID_HOME,
    $env:ANDROID_SDK_ROOT,
    (Join-Path $env:USERPROFILE "AppData\Local\Android\Sdk"),
    (Join-Path $env:LOCALAPPDATA "Android\Sdk")
) | Where-Object { $_ -and (Test-Path (Join-Path $_ "platform-tools\adb.exe")) }
if (!$sdkCandidates) { throw "Android SDK не найден. Укажите ANDROID_HOME или ANDROID_SDK_ROOT." }
$sdk = $sdkCandidates | Select-Object -First 1
$adb = Join-Path $sdk "platform-tools\adb.exe"
$emulator = Join-Path $sdk "emulator\emulator.exe"
$serial = "emulator-$EmulatorPort"

if (!(Test-Path $adb)) { throw "adb.exe не найден: $adb" }
if (!(Test-Path $apkPath)) { throw "APK не найден: $apkPath" }
New-Item -ItemType Directory -Force $outPath | Out-Null

function Invoke-Adb([string[]]$AdbArgs) {
    & $adb @AdbArgs
    if ($LASTEXITCODE -ne 0) { throw "adb завершился с кодом ${LASTEXITCODE}: $($AdbArgs -join ' ')" }
}

& $adb start-server | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "ADB-сервер не запускается. Откройте обычный PowerShell с доступом к TCP-порту 5037 и повторите." 
}

function Save-Screenshot([string]$Name) {
    $target = Join-Path $outPath $Name
    & $adb @("-s", $serial, "exec-out", "screencap", "-p") > $target
    if ($LASTEXITCODE -ne 0) { throw "Не удалось сохранить $target" }
    Write-Host "Сохранён $target"
}

$device = (& $adb devices | Select-String "^$serial\s+device")
if (!$device -and !$SkipEmulator) {
    if (!(Test-Path $emulator)) { throw "emulator.exe не найден: $emulator" }
    if (!$Avd) {
        $avds = @(& $emulator -list-avds | Where-Object { $_ -and $_.Trim() })
        $Avd = @('Codex_API_36', 'AndroidFT_API36') |
            Where-Object { $avds -contains $_ } |
            Select-Object -First 1
        if (!$Avd) { $Avd = $avds | Select-Object -First 1 }
    }
    if (!$Avd) { throw "В Android SDK нет ни одного AVD. Создайте виртуальное устройство в Device Manager." }
    Start-Process -FilePath $emulator -ArgumentList @(
        "-avd", $Avd, "-no-snapshot", "-no-boot-anim", "-no-audio",
        "-gpu", "swiftshader_indirect", "-port", $EmulatorPort
    ) -WindowStyle Hidden | Out-Null
}

if (!$device) {
    $deadline = (Get-Date).AddMinutes(3)
    do {
        Start-Sleep -Seconds 3
        $device = (& $adb devices | Select-String "^$serial\s+device")
    } while (!$device -and (Get-Date) -lt $deadline)
}
if (!$device) {
    throw "Эмулятор не появился в adb. Проверьте виртуализацию, ADB, порт $EmulatorPort и AVD '$Avd'."
}

$bootDeadline = (Get-Date).AddMinutes(2)
do {
    $boot = (& $adb -s "emulator-$EmulatorPort" shell getprop sys.boot_completed 2>$null)
    if ($boot -contains "1") { break }
    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $bootDeadline)
if ($boot -notcontains "1") { throw "Эмулятор подключён, но Android не завершил загрузку." }

Invoke-Adb @("-s", "emulator-$EmulatorPort", "wait-for-device")
Invoke-Adb @("-s", "emulator-$EmulatorPort", "install", "-r", $apkPath)
Invoke-Adb @("-s", "emulator-$EmulatorPort", "shell", "monkey", "-p", $PackageName, "1")
Start-Sleep -Seconds 8
Save-Screenshot "menu.png"

# Центр кнопки вычисляется по высоте экрана: меню адаптируется под 1080×1920 и 1080×2400.
$wm = (& $adb -s "emulator-$EmulatorPort" shell wm size) -join " "
if ($wm -match '(\d+)x(\d+)') {
    $width = [int]$Matches[1]
    $height = [int]$Matches[2]
} else {
    $width = 1080
    $height = 1920
}
$playY = [int]($height * 0.76)
$pauseX = [int]($width * 0.92)
$pauseY = [int]($height * 0.035)
Invoke-Adb @("-s", "emulator-$EmulatorPort", "shell", "input", "tap", ([string][int]($width / 2)), ([string]$playY))
Start-Sleep -Seconds 8
Save-Screenshot "gameplay.png"

Invoke-Adb @("-s", "emulator-$EmulatorPort", "shell", "input", "tap", ([string]$pauseX), ([string]$pauseY))
Start-Sleep -Seconds 3
Save-Screenshot "pause.png"
Read-Host "Завершите забег и откройте экран game over, затем нажмите Enter"
Save-Screenshot "game-over.png"

Write-Host "Готово. Эти файлы сняты через adb screencap с запущенного APK."
