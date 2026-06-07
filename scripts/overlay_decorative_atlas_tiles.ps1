$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$atlasPath = Join-Path $projectRoot "src\main\resources\assets\textures\atlas.png"
$tileSize = 16

if (-not (Test-Path -LiteralPath $atlasPath)) {
    throw "Missing atlas: $atlasPath"
}

$loadedAtlas = [System.Drawing.Bitmap]::FromFile($atlasPath)
$bitmap = [System.Drawing.Bitmap]::new($loadedAtlas)
$loadedAtlas.Dispose()
if ($bitmap.Width -ne 256 -or $bitmap.Height -ne 256) {
    $bitmap.Dispose()
    throw "Expected a 256x256 atlas, found a $($bitmap.Width)x$($bitmap.Height) atlas."
}

function New-Color([int]$r, [int]$g, [int]$b, [int]$a = 255) {
    return [System.Drawing.Color]::FromArgb($a, $r, $g, $b)
}

function Set-Pixel([int]$x, [int]$y, [System.Drawing.Color]$color) {
    if ($x -ge 0 -and $x -lt $bitmap.Width -and $y -ge 0 -and $y -lt $bitmap.Height) {
        $bitmap.SetPixel($x, $y, $color)
    }
}

function Fill-Rect([int]$tileX, [int]$tileY, [int]$x, [int]$y, [int]$width, [int]$height, [System.Drawing.Color]$color) {
    $originX = $tileX * $tileSize
    $originY = $tileY * $tileSize
    for ($py = $y; $py -lt ($y + $height); $py++) {
        for ($px = $x; $px -lt ($x + $width); $px++) {
            Set-Pixel ($originX + $px) ($originY + $py) $color
        }
    }
}

function Clear-Tile([int]$tileX, [int]$tileY) {
    Fill-Rect $tileX $tileY 0 0 16 16 (New-Color 0 0 0 0)
}

function Draw-Line([int]$tileX, [int]$tileY, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [System.Drawing.Color]$color, [int]$thickness = 1) {
    $dx = [Math]::Abs($x1 - $x0)
    $sx = if ($x0 -lt $x1) { 1 } else { -1 }
    $dy = -[Math]::Abs($y1 - $y0)
    $sy = if ($y0 -lt $y1) { 1 } else { -1 }
    $err = $dx + $dy
    while ($true) {
        Fill-Rect $tileX $tileY $x0 $y0 $thickness $thickness $color
        if ($x0 -eq $x1 -and $y0 -eq $y1) {
            break
        }
        $e2 = 2 * $err
        if ($e2 -ge $dy) {
            $err += $dy
            $x0 += $sx
        }
        if ($e2 -le $dx) {
            $err += $dx
            $y0 += $sy
        }
    }
}

function Fill-NoiseTile([int]$tileX, [int]$tileY, [System.Drawing.Color]$base, [System.Drawing.Color]$dark, [System.Drawing.Color]$light) {
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $noise = (($x * 31 + $y * 47 + (($x -bxor $y) * 11)) % 19)
            $color = if ($noise -lt 4) { $dark } elseif ($noise -gt 14) { $light } else { $base }
            Fill-Rect $tileX $tileY $x $y 1 1 $color
        }
    }
}

function Draw-Plant([int]$tileX, [int]$tileY, [System.Drawing.Color]$bloom, [bool]$clustered) {
    Clear-Tile $tileX $tileY
    $outline = New-Color 30 54 31 235
    $stem = New-Color 58 134 58 235
    $leaf = New-Color 82 162 70 235
    Draw-Line $tileX $tileY 8 15 8 5 $outline 2
    Draw-Line $tileX $tileY 8 15 8 5 $stem 1
    Draw-Line $tileX $tileY 8 12 4 9 $outline 2
    Draw-Line $tileX $tileY 8 12 4 9 $leaf 1
    Draw-Line $tileX $tileY 8 10 12 7 $outline 2
    Draw-Line $tileX $tileY 8 10 12 7 $leaf 1
    if ($clustered) {
        Fill-Rect $tileX $tileY 5 4 3 3 $outline
        Fill-Rect $tileX $tileY 8 3 3 3 $outline
        Fill-Rect $tileX $tileY 9 6 3 3 $outline
        Fill-Rect $tileX $tileY 6 5 1 1 $bloom
        Fill-Rect $tileX $tileY 9 4 1 1 $bloom
        Fill-Rect $tileX $tileY 10 7 1 1 $bloom
    } else {
        Fill-Rect $tileX $tileY 5 4 7 5 $outline
        Fill-Rect $tileX $tileY 6 5 5 3 $bloom
        Fill-Rect $tileX $tileY 8 4 1 5 (New-Color 244 221 98 235)
    }
}

function Draw-Grass([int]$tileX, [int]$tileY, [int]$height) {
    Clear-Tile $tileX $tileY
    $outline = New-Color 31 77 36 220
    $dark = New-Color 48 118 47 230
    $mid = New-Color 79 158 62 230
    $light = New-Color 125 192 80 220
    Draw-Line $tileX $tileY 3 15 6 (16 - $height) $outline 2
    Draw-Line $tileX $tileY 3 15 6 (16 - $height) $dark 1
    Draw-Line $tileX $tileY 7 15 7 (15 - $height) $outline 2
    Draw-Line $tileX $tileY 7 15 7 (15 - $height) $mid 1
    Draw-Line $tileX $tileY 11 15 9 (17 - $height) $outline 2
    Draw-Line $tileX $tileY 11 15 9 (17 - $height) $light 1
    Draw-Line $tileX $tileY 13 15 12 (18 - $height) $outline 2
    Draw-Line $tileX $tileY 13 15 12 (18 - $height) $mid 1
}

function Draw-DirtPath([int]$tileX, [int]$tileY) {
    Fill-NoiseTile $tileX $tileY (New-Color 133 94 54) (New-Color 87 58 35) (New-Color 174 130 75)
    Fill-Rect $tileX $tileY 0 0 16 2 (New-Color 168 130 83)
    Fill-Rect $tileX $tileY 2 6 4 1 (New-Color 205 167 105)
    Fill-Rect $tileX $tileY 10 11 3 1 (New-Color 82 55 34)
}

function Draw-Farmland([int]$tileX, [int]$tileY) {
    Fill-NoiseTile $tileX $tileY (New-Color 92 64 39) (New-Color 60 43 29) (New-Color 130 91 54)
    for ($x = 1; $x -lt 16; $x += 4) {
        Draw-Line $tileX $tileY $x 0 ($x - 2) 15 (New-Color 53 38 27) 1
        Draw-Line $tileX $tileY ($x + 1) 0 ($x - 1) 15 (New-Color 126 88 52) 1
    }
}

function Draw-Bed([int]$tileX, [int]$tileY) {
    Clear-Tile $tileX $tileY
    $outline = New-Color 53 35 31
    Fill-Rect $tileX $tileY 2 8 12 6 $outline
    Fill-Rect $tileX $tileY 3 9 10 4 (New-Color 180 48 58)
    Fill-Rect $tileX $tileY 3 6 10 3 $outline
    Fill-Rect $tileX $tileY 4 7 8 2 (New-Color 230 226 205)
    Fill-Rect $tileX $tileY 3 14 2 1 (New-Color 111 74 43)
    Fill-Rect $tileX $tileY 11 14 2 1 (New-Color 111 74 43)
}

function Draw-Workstation([int]$tileX, [int]$tileY) {
    Fill-NoiseTile $tileX $tileY (New-Color 148 96 52) (New-Color 83 52 32) (New-Color 196 136 73)
    Fill-Rect $tileX $tileY 2 3 12 10 (New-Color 70 47 32)
    Fill-Rect $tileX $tileY 3 4 10 8 (New-Color 139 91 49)
    Fill-Rect $tileX $tileY 4 5 3 2 (New-Color 201 157 86)
    Fill-Rect $tileX $tileY 9 8 3 2 (New-Color 73 47 31)
}

function Draw-Lamp([int]$tileX, [int]$tileY) {
    Fill-NoiseTile $tileX $tileY (New-Color 124 83 48) (New-Color 66 45 31) (New-Color 176 125 68)
    Fill-Rect $tileX $tileY 2 2 12 12 (New-Color 50 39 31)
    Fill-Rect $tileX $tileY 3 3 10 10 (New-Color 240 176 70)
    Fill-Rect $tileX $tileY 5 5 6 6 (New-Color 255 226 129)
    Fill-Rect $tileX $tileY 3 3 10 1 (New-Color 94 66 43)
    Fill-Rect $tileX $tileY 3 12 10 1 (New-Color 94 66 43)
}

function Draw-Lantern([int]$tileX, [int]$tileY) {
    Clear-Tile $tileX $tileY
    Fill-Rect $tileX $tileY 6 1 4 2 (New-Color 38 34 33)
    Fill-Rect $tileX $tileY 4 3 8 11 (New-Color 38 34 33)
    Fill-Rect $tileX $tileY 5 4 6 8 (New-Color 230 153 61)
    Fill-Rect $tileX $tileY 6 5 4 6 (New-Color 255 226 125)
    Fill-Rect $tileX $tileY 5 13 6 2 (New-Color 38 34 33)
    Fill-Rect $tileX $tileY 4 6 1 4 (New-Color 117 100 85)
    Fill-Rect $tileX $tileY 11 6 1 4 (New-Color 18 18 18)
}

function Draw-Stair([int]$tileX, [int]$tileY, [System.Drawing.Color]$base, [System.Drawing.Color]$dark, [System.Drawing.Color]$light) {
    Clear-Tile $tileX $tileY
    Fill-Rect $tileX $tileY 1 10 14 5 $dark
    Fill-Rect $tileX $tileY 2 9 12 4 $base
    Fill-Rect $tileX $tileY 5 6 9 4 $dark
    Fill-Rect $tileX $tileY 6 5 8 3 $light
    Fill-Rect $tileX $tileY 9 2 5 4 $dark
    Fill-Rect $tileX $tileY 10 1 4 3 $base
}

function Draw-Fence([int]$tileX, [int]$tileY, [bool]$gate) {
    Clear-Tile $tileX $tileY
    $outline = New-Color 53 35 24
    $wood = New-Color 158 101 55
    $light = New-Color 203 145 76
    Fill-Rect $tileX $tileY 3 3 3 12 $outline
    Fill-Rect $tileX $tileY 10 3 3 12 $outline
    Fill-Rect $tileX $tileY 4 4 1 10 $light
    Fill-Rect $tileX $tileY 11 4 1 10 $wood
    Fill-Rect $tileX $tileY 1 6 14 3 $outline
    Fill-Rect $tileX $tileY 1 11 14 3 $outline
    Fill-Rect $tileX $tileY 2 7 12 1 $wood
    Fill-Rect $tileX $tileY 2 12 12 1 $light
    if ($gate) {
        Fill-Rect $tileX $tileY 7 5 2 10 $outline
        Fill-Rect $tileX $tileY 8 6 1 8 $wood
    }
}

function Draw-Door([int]$tileX, [int]$tileY) {
    Clear-Tile $tileX $tileY
    Fill-Rect $tileX $tileY 3 1 10 14 (New-Color 48 31 24)
    Fill-Rect $tileX $tileY 4 2 8 12 (New-Color 159 98 53)
    Fill-Rect $tileX $tileY 5 3 6 4 (New-Color 198 137 72)
    Fill-Rect $tileX $tileY 5 9 6 4 (New-Color 120 73 43)
    Fill-Rect $tileX $tileY 10 8 1 1 (New-Color 230 184 72)
}

function Draw-Bell([int]$tileX, [int]$tileY) {
    Clear-Tile $tileX $tileY
    Fill-Rect $tileX $tileY 5 2 6 2 (New-Color 55 45 34)
    Fill-Rect $tileX $tileY 4 4 8 8 (New-Color 76 52 31)
    Fill-Rect $tileX $tileY 5 5 6 6 (New-Color 210 147 54)
    Fill-Rect $tileX $tileY 3 10 10 3 (New-Color 92 63 35)
    Fill-Rect $tileX $tileY 4 9 8 2 (New-Color 242 190 80)
    Fill-Rect $tileX $tileY 7 13 2 2 (New-Color 53 39 30)
}

Draw-DirtPath 0 8
Draw-Farmland 1 8
Draw-Bed 2 8
Draw-Workstation 3 8
Draw-Plant 4 8 (New-Color 205 38 58 240) $true
Draw-Plant 5 8 (New-Color 145 91 214 240) $true
Draw-Plant 6 8 (New-Color 239 238 215 240) $false
Draw-Plant 7 8 (New-Color 190 105 220 240) $true
Draw-Grass 8 8 12
Draw-Grass 9 8 7
Draw-Lamp 10 8
Draw-Lantern 11 8
Draw-Stair 12 8 (New-Color 165 103 56) (New-Color 75 48 32) (New-Color 211 151 80)
Draw-Stair 13 8 (New-Color 127 131 132) (New-Color 67 70 73) (New-Color 170 174 174)
Draw-Fence 14 8 $false
Draw-Fence 15 8 $true
Draw-Door 0 9
Draw-Bell 1 9

$tempPath = Join-Path (Split-Path -Parent $atlasPath) "atlas.decorative.tmp.png"
$bitmap.Save($tempPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
[System.IO.File]::Copy($tempPath, $atlasPath, $true)
try {
    Remove-Item -LiteralPath $tempPath -Force
} catch {
    Write-Warning "Decorative atlas temp file could not be removed yet: $tempPath"
}
Write-Output "Overlayed decorative tiles into $atlasPath"
