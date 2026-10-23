param(
    [Parameter(Mandatory = $true)][string]$OutDir
)

Add-Type -AssemblyName System.Drawing
[System.IO.Directory]::CreateDirectory($OutDir) | Out-Null

function Draw-Glow {
    param($g, [float]$cx, [float]$cy, [float]$r, $color, [int]$alpha = 2)
    for ($i = 34; $i -ge 1; $i--) {
        $rr = $r * $i / 34.0
        $b = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb($alpha, $color))
        $g.FillEllipse($b, $cx - $rr, $cy - $rr, $rr * 2, $rr * 2)
        $b.Dispose()
    }
}

function Draw-Diamond {
    param($g, [float]$cx, [float]$cy, [float]$s, $color)
    Draw-Glow $g $cx $cy ($s * 2.1) $color 2
    $pts = [System.Drawing.PointF[]]@(
        [System.Drawing.PointF]::new($cx, $cy - $s),
        [System.Drawing.PointF]::new($cx + $s * 0.62, $cy),
        [System.Drawing.PointF]::new($cx, $cy + $s),
        [System.Drawing.PointF]::new($cx - $s * 0.62, $cy)
    )
    $b = [System.Drawing.SolidBrush]::new($color)
    $g.FillPolygon($b, $pts)
    $b.Dispose()
    $pen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(80, 255, 255, 255), 2.0)
    $g.DrawPolygon($pen, $pts)
    $pen.Dispose()
}

function Draw-Orb {
    param($g, [float]$cx, [float]$cy, [float]$r)
    Draw-Glow $g $cx $cy ($r * 2.2) ([System.Drawing.Color]::FromArgb(255, 95, 243, 255)) 3

    $ring = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(110, 255, 255, 255), [float]($r * 0.04 + 1.6))
    $g.DrawEllipse($ring, $cx - $r * 1.45, $cy - $r * 1.45, $r * 2.9, $r * 2.9)
    $ring.Dispose()

    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddEllipse($cx - $r, $cy - $r, $r * 2, $r * 2)
    $pgb = [System.Drawing.Drawing2D.PathGradientBrush]::new($path)
    $pgb.CenterColor = [System.Drawing.Color]::FromArgb(255, 228, 255, 255)
    $pgb.SurroundColors = [System.Drawing.Color[]]@([System.Drawing.Color]::FromArgb(255, 61, 214, 255))
    $g.FillPath($pgb, $path)
    $pgb.Dispose()
    $path.Dispose()

    $core = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
    $g.FillEllipse($core, $cx - $r * 0.30, $cy - $r * 0.30, $r * 0.60, $r * 0.60)
    $core.Dispose()
}

# ------------------------------------------------------------------ icon 512
$S = 512
$bmp = [System.Drawing.Bitmap]::new($S, $S, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic

$rect = [System.Drawing.Rectangle]::new(0, 0, $S, $S)
$bg = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
    $rect,
    [System.Drawing.Color]::FromArgb(255, 6, 10, 24),
    [System.Drawing.Color]::FromArgb(255, 21, 38, 84),
    62.0)
$g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
$g.FillRectangle($bg, $rect)
$g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
$bg.Dispose()

Draw-Glow $g 250 300 300 ([System.Drawing.Color]::FromArgb(255, 90, 60, 210)) 2
Draw-Glow $g 120 90 180 ([System.Drawing.Color]::FromArgb(255, 255, 79, 216)) 2

$rnd = [System.Random]::new(7)
for ($i = 0; $i -lt 46; $i++) {
    $x = $rnd.Next(6, $S - 6)
    $y = $rnd.Next(6, $S - 6)
    $rr = $rnd.Next(1, 3)
    $a = $rnd.Next(35, 120)
    $b = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb($a, 220, 240, 255))
    $g.FillEllipse($b, $x, $y, $rr, $rr)
    $b.Dispose()
}

Draw-Diamond $g 140 150 52 ([System.Drawing.Color]::FromArgb(255, 255, 79, 216))
Draw-Diamond $g 372 112 42 ([System.Drawing.Color]::FromArgb(255, 255, 158, 79))
Draw-Diamond $g 402 262 33 ([System.Drawing.Color]::FromArgb(255, 182, 255, 92))
Draw-Orb $g 256 352 74

$g.Dispose()

# Store icons must have an opaque background: flatten to 24bpp RGB.
$flatIcon = [System.Drawing.Bitmap]::new($S, $S, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$fgIcon = [System.Drawing.Graphics]::FromImage($flatIcon)
$fgIcon.Clear([System.Drawing.Color]::FromArgb(255, 6, 10, 24))
$fgIcon.DrawImage($bmp, 0, 0, $S, $S)
$fgIcon.Dispose()
$bmp.Dispose()
$flatIcon.Save((Join-Path $OutDir 'icon-512.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$flatIcon.Dispose()
Write-Host 'icon-512.png done'

# ------------------------------------------------------- feature 1024x500
$W = 1024
$H = 500
$fb = [System.Drawing.Bitmap]::new($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$fg = [System.Drawing.Graphics]::FromImage($fb)
$fg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$fg.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

$frect = [System.Drawing.Rectangle]::new(0, 0, $W, $H)
$fbg = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
    $frect,
    [System.Drawing.Color]::FromArgb(255, 5, 8, 20),
    [System.Drawing.Color]::FromArgb(255, 17, 31, 70),
    20.0)
$fg.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
$fg.FillRectangle($fbg, $frect)
$fg.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
$fbg.Dispose()

Draw-Glow $fg 800 260 300 ([System.Drawing.Color]::FromArgb(255, 60, 200, 240)) 3
Draw-Glow $fg 250 420 260 ([System.Drawing.Color]::FromArgb(255, 130, 40, 170)) 2

$frnd = [System.Random]::new(11)
for ($i = 0; $i -lt 70; $i++) {
    $x = $frnd.Next(6, $W - 6)
    $y = $frnd.Next(6, $H - 6)
    $rr = $frnd.Next(1, 3)
    $a = $frnd.Next(30, 110)
    $b = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb($a, 220, 240, 255))
    $fg.FillEllipse($b, $x, $y, $rr, $rr)
    $b.Dispose()
}

Draw-Diamond $fg 610 100 34 ([System.Drawing.Color]::FromArgb(255, 255, 79, 216))
Draw-Diamond $fg 930 90 30 ([System.Drawing.Color]::FromArgb(255, 182, 255, 92))
Draw-Diamond $fg 940 400 24 ([System.Drawing.Color]::FromArgb(255, 255, 158, 79))
Draw-Orb $fg 795 255 82

$family = [System.Drawing.FontFamily]::new('Arial Black')
$subFamily = [System.Drawing.FontFamily]::new('Segoe UI')

function Draw-GlowText {
    param($g, [string]$text, $family, [float]$size, [float]$x, [float]$y, $fill, $glow)
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddString($text, $family, [int][System.Drawing.FontStyle]::Bold, $size,
        [System.Drawing.PointF]::new($x, $y), [System.Drawing.StringFormat]::GenericDefault)
    $pen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(90, $glow), 16.0)
    $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    $g.DrawPath($pen, $path)
    $brush = [System.Drawing.SolidBrush]::new($fill)
    $g.FillPath($brush, $path)
    $brush.Dispose()
    $pen.Dispose()
    $path.Dispose()
}

Draw-GlowText $fg 'NEON' $family 92 58 58 ([System.Drawing.Color]::FromArgb(255, 255, 255, 255)) ([System.Drawing.Color]::FromArgb(255, 60, 220, 255))
Draw-GlowText $fg 'DODGE' $family 92 58 170 ([System.Drawing.Color]::FromArgb(255, 255, 236, 251)) ([System.Drawing.Color]::FromArgb(255, 255, 79, 216))

$sub = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(210, 190, 226, 242))
$fg.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$fg.DrawString('уклоняйся от астероидов', [System.Drawing.Font]::new($subFamily, 24, [System.Drawing.FontStyle]::Regular), $sub, 64, 325)
$sub.Dispose()

$ver = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(120, 160, 210, 230))
$fg.DrawString('NEON DODGE  ·  v1.2', [System.Drawing.Font]::new($subFamily, 16, [System.Drawing.FontStyle]::Regular), $ver, 64, 380)
$ver.Dispose()

$fg.Dispose()

$flatFeature = [System.Drawing.Bitmap]::new($W, $H, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$fgFlat = [System.Drawing.Graphics]::FromImage($flatFeature)
$fgFlat.Clear([System.Drawing.Color]::FromArgb(255, 5, 8, 20))
$fgFlat.DrawImage($fb, 0, 0, $W, $H)
$fgFlat.Dispose()
$fb.Dispose()
$flatFeature.Save((Join-Path $OutDir 'feature-1024x500.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$flatFeature.Dispose()
Write-Host 'feature-1024x500.png done'
