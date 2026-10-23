# Encodes the generated music loop to OGG/Vorbis for a much smaller APK.
# Usage: node tools/make_audio.js; pwsh tools/encode-audio.ps1
param(
    [string]$Ffmpeg = ""
)

$ErrorActionPreference = "Stop"

$candidates = @(
    $Ffmpeg,
    $env:FFMPEG,
    (Get-Command ffmpeg -ErrorAction SilentlyContinue).Source,
    "D:\DS\Programs\Octave-10.1.0\mingw64\bin\ffmpeg.exe",
    "C:\ffmpeg\bin\ffmpeg.exe"
) | Where-Object { $_ -and (Test-Path $_) }

$ffmpegExe = $candidates | Select-Object -First 1
if (-not $ffmpegExe) {
    throw "ffmpeg не найден. Укажите путь: .\tools\encode-audio.ps1 -Ffmpeg C:\path\to\ffmpeg.exe"
}

$root = Split-Path $PSScriptRoot -Parent
$source = Join-Path $PSScriptRoot "audio-src\music.wav"
$rawDir = Join-Path $root "app\src\main\res\raw"
$target = Join-Path $rawDir "music.ogg"

if (-not (Test-Path $source)) {
    throw "Нет $source. Сначала выполните: node tools/make_audio.js"
}

& $ffmpegExe -y -hide_banner -loglevel error -i $source `
    -c:a libvorbis -q:a 5 $target
if ($LASTEXITCODE -ne 0) {
    throw "ffmpeg завершился с кодом $LASTEXITCODE"
}

# Старый WAV не должен попасть в APK: иначе будет дубликат ресурса music.
$stale = Join-Path $rawDir "music.wav"
if (Test-Path $stale) {
    Remove-Item $stale -Force
}

$size = (Get-Item $target).Length
Write-Host ("music.ogg: {0:N1} KB" -f ($size / 1024))
