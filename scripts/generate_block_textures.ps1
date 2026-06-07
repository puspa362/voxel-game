Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceBlockDirectory = Join-Path $projectRoot "texture_sources\blocks"
$importDirectory = Join-Path $projectRoot "texture_sources\import"
$tileSize = 16

$null = New-Item -ItemType Directory -Path $sourceBlockDirectory -Force
$null = New-Item -ItemType Directory -Path $importDirectory -Force

function New-Color([int]$r, [int]$g, [int]$b, [int]$a = 255) {
    return [System.Drawing.Color]::FromArgb($a, $r, $g, $b)
}

function Mix-Color([System.Drawing.Color]$a, [System.Drawing.Color]$b, [double]$t) {
    $u = [Math]::Max(0.0, [Math]::Min(1.0, $t))
    return New-Color `
        ([int]($a.R + ($b.R - $a.R) * $u)) `
        ([int]($a.G + ($b.G - $a.G) * $u)) `
        ([int]($a.B + ($b.B - $a.B) * $u)) `
        ([int]($a.A + ($b.A - $a.A) * $u))
}

function Get-Noise([int]$x, [int]$y, [int]$seed) {
    $value = ($x * 374761393 + $y * 668265263 + $seed * 1442695041) -band 0x7fffffff
    $value = ($value -bxor ($value -shr 13)) * 1274126177
    return (($value -band 0xffff) / 65535.0)
}

function Set-TilePixel([System.Drawing.Bitmap]$bitmap, [int]$x, [int]$y, [System.Drawing.Color]$color) {
    if ($x -ge 0 -and $x -lt $tileSize -and $y -ge 0 -and $y -lt $tileSize) {
        $bitmap.SetPixel($x, $y, $color)
    }
}

function Save-Tile([string]$name, [scriptblock]$draw) {
    $outputPath = Join-Path $sourceBlockDirectory "$name.png"
    $importPath = Join-Path $importDirectory "$name.png"
    if (Test-Path -LiteralPath $importPath) {
        Import-Tile $importPath $outputPath
        return
    }

    $bitmap = [System.Drawing.Bitmap]::new($tileSize, $tileSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        & $draw $bitmap
        $bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }
}

function Import-Tile([string]$inputPath, [string]$outputPath) {
    $source = [System.Drawing.Bitmap]::FromFile($inputPath)
    $target = [System.Drawing.Bitmap]::new($tileSize, $tileSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($target)
    try {
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.DrawImage($source, 0, 0, $tileSize, $tileSize)
        $target.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $target.Dispose()
        $source.Dispose()
    }
}

function Draw-DitheredTile([System.Drawing.Bitmap]$bitmap, [System.Drawing.Color]$base, [System.Drawing.Color]$dark, [System.Drawing.Color]$light, [int]$seed) {
    for ($y = 0; $y -lt $tileSize; $y++) {
        for ($x = 0; $x -lt $tileSize; $x++) {
            $n = Get-Noise $x $y $seed
            $shade = if ($n -lt 0.20) { $dark } elseif ($n -gt 0.82) { $light } else { $base }
            if ((($x + $y + $seed) % 11) -eq 0) {
                $shade = Mix-Color $shade $light 0.30
            }
            Set-TilePixel $bitmap $x $y $shade
        }
    }
}

function Draw-GrassTop([System.Drawing.Bitmap]$bitmap) {
    Draw-DitheredTile $bitmap (New-Color 92 166 69) (New-Color 58 125 55) (New-Color 132 198 86) 11
    for ($i = 0; $i -lt 22; $i++) {
        $x = (3 + $i * 7) % 16
        $y = (5 + $i * 11) % 16
        Set-TilePixel $bitmap $x $y (New-Color 157 218 95)
        Set-TilePixel $bitmap (($x + 1) % 16) $y (New-Color 74 144 61)
    }
}

function Draw-GrassSide([System.Drawing.Bitmap]$bitmap) {
    Draw-DitheredTile $bitmap (New-Color 122 87 55) (New-Color 89 62 42) (New-Color 153 113 73) 23
    for ($y = 0; $y -lt 5; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $color = if ((Get-Noise $x $y 29) -gt 0.55) { New-Color 99 174 70 } else { New-Color 63 137 58 }
            Set-TilePixel $bitmap $x $y $color
        }
    }
    for ($x = 0; $x -lt 16; $x += 3) {
        Set-TilePixel $bitmap $x 5 (New-Color 53 117 50)
        Set-TilePixel $bitmap (($x + 1) % 16) 6 (New-Color 81 150 61)
    }
}

function Draw-StoneLike([System.Drawing.Bitmap]$bitmap, [System.Drawing.Color]$base, [System.Drawing.Color]$dark, [System.Drawing.Color]$light, [int]$seed) {
    Draw-DitheredTile $bitmap $base $dark $light $seed
    for ($i = 0; $i -lt 10; $i++) {
        $x = (2 + $i * 5 + $seed) % 16
        $y = (1 + $i * 7 + $seed) % 16
        Set-TilePixel $bitmap $x $y (Mix-Color $dark $base 0.35)
        Set-TilePixel $bitmap (($x + 1) % 16) $y (Mix-Color $light $base 0.55)
    }
}

function Draw-Ore([System.Drawing.Bitmap]$bitmap, [System.Drawing.Color]$ore, [System.Drawing.Color]$oreLight, [int]$seed) {
    Draw-StoneLike $bitmap (New-Color 105 111 113) (New-Color 73 80 83) (New-Color 143 148 149) $seed
    $points = @(@(3, 4), @(10, 3), @(7, 8), @(12, 11), @(4, 12))
    foreach ($point in $points) {
        $x = ($point[0] + $seed) % 14
        $y = ($point[1] + [Math]::Floor($seed / 3)) % 14
        Set-TilePixel $bitmap $x $y $ore
        Set-TilePixel $bitmap ($x + 1) $y $ore
        Set-TilePixel $bitmap $x ($y + 1) $ore
        Set-TilePixel $bitmap ($x + 1) ($y + 1) $oreLight
    }
}

function Draw-Water([System.Drawing.Bitmap]$bitmap) {
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $wave = [Math]::Sin(($x + $y * 0.45) * 0.9) * 0.5 + 0.5
            $n = Get-Noise $x $y 71
            $base = Mix-Color (New-Color 44 141 186 190) (New-Color 91 207 218 178) (($wave * 0.45) + ($n * 0.20))
            Set-TilePixel $bitmap $x $y $base
        }
    }
    for ($x = 0; $x -lt 16; $x++) {
        if (($x % 5) -lt 3) {
            Set-TilePixel $bitmap $x (($x + 3) % 16) (New-Color 152 238 231 150)
        }
    }
}

function Draw-Log([System.Drawing.Bitmap]$bitmap) {
    Draw-LogVariant $bitmap (New-Color 133 84 45) (New-Color 83 52 32) (New-Color 171 111 61) 83
}

function Draw-LogVariant([System.Drawing.Bitmap]$bitmap, [System.Drawing.Color]$base, [System.Drawing.Color]$dark, [System.Drawing.Color]$light, [int]$seed) {
    Draw-DitheredTile $bitmap $base $dark $light $seed
    for ($x = 2; $x -lt 16; $x += 5) {
        for ($y = 0; $y -lt 16; $y++) {
            Set-TilePixel $bitmap $x $y $dark
        }
    }
}

function Draw-Planks([System.Drawing.Bitmap]$bitmap) {
    Draw-DitheredTile $bitmap (New-Color 176 122 64) (New-Color 125 82 45) (New-Color 213 156 87) 89
    foreach ($y in @(4, 10)) {
        for ($x = 0; $x -lt 16; $x++) { Set-TilePixel $bitmap $x $y (New-Color 103 68 39) }
    }
    foreach ($segment in @(@(5, 0, 4), @(11, 5, 5), @(7, 11, 5))) {
        for ($y = $segment[1]; $y -lt ($segment[1] + $segment[2]); $y++) { Set-TilePixel $bitmap $segment[0] $y (New-Color 103 68 39) }
    }
}

function Draw-PlanksVariant([System.Drawing.Bitmap]$bitmap, [System.Drawing.Color]$base, [System.Drawing.Color]$dark, [System.Drawing.Color]$light, [int]$seed) {
    Draw-DitheredTile $bitmap $base $dark $light $seed
    $groove = Mix-Color $dark (New-Color 34 24 18) 0.35
    foreach ($y in @(4, 10)) {
        for ($x = 0; $x -lt 16; $x++) { Set-TilePixel $bitmap $x $y $groove }
    }
    foreach ($segment in @(@(5, 0, 4), @(11, 5, 5), @(7, 11, 5))) {
        for ($y = $segment[1]; $y -lt ($segment[1] + $segment[2]); $y++) { Set-TilePixel $bitmap $segment[0] $y $groove }
    }
}

function Draw-Leaves([System.Drawing.Bitmap]$bitmap) {
    Draw-LeavesVariant $bitmap (New-Color 58 137 64) (New-Color 35 96 47) (New-Color 97 174 78) 97
}

function Draw-LeavesVariant([System.Drawing.Bitmap]$bitmap, [System.Drawing.Color]$base, [System.Drawing.Color]$dark, [System.Drawing.Color]$light, [int]$seed) {
    Draw-DitheredTile $bitmap $base $dark $light $seed
    for ($i = 0; $i -lt 26; $i++) {
        $x = ($i * 7 + 2 + $seed) % 16
        $y = ($i * 5 + 3 + $seed) % 16
        Set-TilePixel $bitmap $x $y $light
    }
}

function Draw-SaplingBlock([System.Drawing.Bitmap]$bitmap, [System.Drawing.Color]$leaf, [System.Drawing.Color]$leafDark, [System.Drawing.Color]$wood) {
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            Set-TilePixel $bitmap $x $y ([System.Drawing.Color]::Transparent)
        }
    }
    for ($y = 8; $y -lt 15; $y++) {
        Set-TilePixel $bitmap 7 $y $wood
        Set-TilePixel $bitmap 8 $y (Mix-Color $wood (New-Color 255 255 255) 0.18)
    }
    foreach ($point in @(@(5, 6), @(8, 5), @(10, 7), @(4, 9), @(9, 9))) {
        Set-TilePixel $bitmap $point[0] $point[1] $leaf
        Set-TilePixel $bitmap ($point[0] + 1) $point[1] $leaf
        Set-TilePixel $bitmap $point[0] ($point[1] + 1) $leafDark
    }
}

function Draw-Sandstone([System.Drawing.Bitmap]$bitmap) {
    Draw-DitheredTile $bitmap (New-Color 193 169 104) (New-Color 149 126 76) (New-Color 225 206 141) 101
    for ($y = 4; $y -lt 16; $y += 5) {
        for ($x = 0; $x -lt 16; $x++) { Set-TilePixel $bitmap $x $y (New-Color 139 117 72) }
    }
    for ($x = 7; $x -lt 16; $x += 7) {
        for ($y = 0; $y -lt 16; $y++) { if (($y % 5) -ne 4) { Set-TilePixel $bitmap $x $y (New-Color 169 143 87) } }
    }
}

function Draw-Ice([System.Drawing.Bitmap]$bitmap) {
    Draw-DitheredTile $bitmap (New-Color 139 211 226 210) (New-Color 90 166 202 220) (New-Color 213 250 250 190) 107
    for ($i = 0; $i -lt 6; $i++) {
        $x = 2 + $i * 2
        for ($y = 0; $y -lt 16; $y++) {
            if ((($x + $y) % 5) -eq 0) { Set-TilePixel $bitmap $x $y (New-Color 231 255 255 180) }
        }
    }
}

Save-Tile "grass_top" { param($b) Draw-GrassTop $b }
Save-Tile "grass_side" { param($b) Draw-GrassSide $b }
Save-Tile "dirt" { param($b) Draw-DitheredTile $b (New-Color 122 87 55) (New-Color 88 61 42) (New-Color 153 112 72) 31 }
Save-Tile "stone" { param($b) Draw-StoneLike $b (New-Color 105 111 113) (New-Color 73 80 83) (New-Color 143 148 149) 41 }
Save-Tile "sand" { param($b) Draw-DitheredTile $b (New-Color 211 190 121) (New-Color 166 142 88) (New-Color 238 221 154) 43 }
Save-Tile "sandstone" { param($b) Draw-Sandstone $b }
Save-Tile "snow" { param($b) Draw-DitheredTile $b (New-Color 222 235 238) (New-Color 177 201 213) (New-Color 251 255 255) 47 }
Save-Tile "ice" { param($b) Draw-Ice $b }
Save-Tile "coal_ore" { param($b) Draw-Ore $b (New-Color 41 45 48) (New-Color 75 82 86) 53 }
Save-Tile "iron_ore" { param($b) Draw-Ore $b (New-Color 190 132 87) (New-Color 231 174 118) 59 }
Save-Tile "copper_ore" { param($b) Draw-Ore $b (New-Color 194 103 58) (New-Color 82 181 143) 61 }
Save-Tile "gold_ore" { param($b) Draw-Ore $b (New-Color 223 174 57) (New-Color 255 225 102) 67 }
Save-Tile "diamond_ore" { param($b) Draw-Ore $b (New-Color 67 207 215) (New-Color 178 255 248) 71 }
Save-Tile "water" { param($b) Draw-Water $b }
Save-Tile "leaves" { param($b) Draw-Leaves $b }
Save-Tile "oak_log" { param($b) Draw-Log $b }
Save-Tile "spruce_log" { param($b) Draw-LogVariant $b (New-Color 94 64 45) (New-Color 54 38 30) (New-Color 128 91 62) 151 }
Save-Tile "birch_log" { param($b) Draw-LogVariant $b (New-Color 211 198 164) (New-Color 68 61 52) (New-Color 239 231 198) 157 }
Save-Tile "dark_oak_log" { param($b) Draw-LogVariant $b (New-Color 76 49 34) (New-Color 38 27 22) (New-Color 111 75 51) 163 }
Save-Tile "spruce_leaves" { param($b) Draw-LeavesVariant $b (New-Color 42 103 62) (New-Color 26 70 48) (New-Color 73 137 83) 167 }
Save-Tile "birch_leaves" { param($b) Draw-LeavesVariant $b (New-Color 117 168 74) (New-Color 72 118 55) (New-Color 165 210 94) 173 }
Save-Tile "dark_oak_leaves" { param($b) Draw-LeavesVariant $b (New-Color 43 102 48) (New-Color 25 68 36) (New-Color 76 138 62) 179 }
Save-Tile "oak_sapling" { param($b) Draw-SaplingBlock $b (New-Color 79 161 70) (New-Color 40 106 48) (New-Color 96 58 32) }
Save-Tile "spruce_sapling" { param($b) Draw-SaplingBlock $b (New-Color 48 119 67) (New-Color 30 76 49) (New-Color 82 52 34) }
Save-Tile "birch_sapling" { param($b) Draw-SaplingBlock $b (New-Color 130 183 83) (New-Color 77 127 62) (New-Color 178 151 88) }
Save-Tile "dark_oak_sapling" { param($b) Draw-SaplingBlock $b (New-Color 50 116 54) (New-Color 27 77 39) (New-Color 68 42 27) }
Save-Tile "oak_planks" { param($b) Draw-Planks $b }
Save-Tile "spruce_planks" { param($b) Draw-PlanksVariant $b (New-Color 112 74 49) (New-Color 70 45 31) (New-Color 147 101 68) 131 }
Save-Tile "birch_planks" { param($b) Draw-PlanksVariant $b (New-Color 209 184 119) (New-Color 158 126 76) (New-Color 238 219 159) 137 }
Save-Tile "acacia_planks" { param($b) Draw-PlanksVariant $b (New-Color 182 91 48) (New-Color 118 58 36) (New-Color 221 129 72) 139 }
Save-Tile "dark_oak_planks" { param($b) Draw-PlanksVariant $b (New-Color 82 52 35) (New-Color 45 30 23) (New-Color 119 78 52) 149 }
Save-Tile "mossy_stone" { param($b) Draw-StoneLike $b (New-Color 87 111 88) (New-Color 58 82 64) (New-Color 125 148 116) 109 }
Save-Tile "gravel" { param($b) Draw-DitheredTile $b (New-Color 115 112 108) (New-Color 72 73 73) (New-Color 158 153 145) 113 }
Save-Tile "bedrock" { param($b) Draw-StoneLike $b (New-Color 62 62 68) (New-Color 32 34 40) (New-Color 98 98 105) 127 }

Write-Output "Generated normalized 16x16 block textures in $sourceBlockDirectory"
