Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot "generate_block_textures.ps1")

$outputPath = Join-Path $projectRoot "src\main\resources\assets\textures\atlas.png"
$outputDirectory = Split-Path -Parent $outputPath
$tileSize = 16
$atlasTiles = 16
$sourceBlockDirectory = Join-Path $projectRoot "texture_sources\blocks"
$null = New-Item -ItemType Directory -Path $outputDirectory -Force

$bitmap = [System.Drawing.Bitmap]::new(
    $tileSize * $atlasTiles,
    $tileSize * $atlasTiles,
    [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
)

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

function Copy-SourceTile([int]$tileX, [int]$tileY, [string]$fileName, [bool]$flipY = $false, [double]$alphaScale = 1.0) {
    $path = Join-Path $sourceBlockDirectory $fileName
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Missing source texture: $path"
    }

    $source = [System.Drawing.Bitmap]::FromFile($path)
    try {
        if ($source.Width -ne $tileSize -or $source.Height -ne $tileSize) {
            throw "Source texture $path has size $($source.Width)x$($source.Height), expected ${tileSize}x${tileSize}."
        }
        for ($y = 0; $y -lt $tileSize; $y++) {
            for ($x = 0; $x -lt $tileSize; $x++) {
                $sourceY = if ($flipY) { $tileSize - 1 - $y } else { $y }
                $color = $source.GetPixel($x, $sourceY)
                if ($alphaScale -lt 1.0) {
                    $alpha = [Math]::Max(0, [Math]::Min(255, [int]($color.A * $alphaScale)))
                    $color = [System.Drawing.Color]::FromArgb($alpha, $color.R, $color.G, $color.B)
                }
                Set-Pixel ($tileX * $tileSize + $x) ($tileY * $tileSize + $y) $color
            }
        }
    } finally {
        $source.Dispose()
    }
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

function Fill-Tile([int]$tileX, [int]$tileY, [System.Drawing.Color]$base, [System.Drawing.Color]$dark, [System.Drawing.Color]$light) {
    $originX = $tileX * $tileSize
    $originY = $tileY * $tileSize
    for ($y = 0; $y -lt $tileSize; $y++) {
        for ($x = 0; $x -lt $tileSize; $x++) {
            $noise = (($x * 37 + $y * 57 + (($x -bxor $y) * 13)) % 17)
            $color = if ($noise -lt 4) { $dark } elseif ($noise -gt 13) { $light } else { $base }
            Set-Pixel ($originX + $x) ($originY + $y) $color
        }
    }
}

function Draw-GrassSide([int]$tileX, [int]$tileY) {
    Fill-Tile $tileX $tileY (New-Color 115 85 53) (New-Color 82 58 38) (New-Color 143 108 68)
    Fill-Rect $tileX $tileY 0 0 16 5 (New-Color 75 147 54)
    Fill-Rect $tileX $tileY 0 5 16 2 (New-Color 49 111 42)
}

function Draw-Log([int]$tileX, [int]$tileY) {
    Fill-Tile $tileX $tileY (New-Color 124 78 40) (New-Color 81 48 28) (New-Color 158 103 56)
    for ($x = 2; $x -lt 16; $x += 5) {
        Fill-Rect $tileX $tileY $x 0 2 16 (New-Color 86 52 30)
    }
}

function Draw-Planks([int]$tileX, [int]$tileY) {
    Fill-Tile $tileX $tileY (New-Color 170 119 62) (New-Color 126 82 43) (New-Color 207 151 82)
    Fill-Rect $tileX $tileY 0 4 16 1 (New-Color 103 68 38)
    Fill-Rect $tileX $tileY 0 10 16 1 (New-Color 103 68 38)
    Fill-Rect $tileX $tileY 5 0 1 4 (New-Color 103 68 38)
    Fill-Rect $tileX $tileY 11 5 1 5 (New-Color 103 68 38)
    Fill-Rect $tileX $tileY 7 11 1 5 (New-Color 103 68 38)
}

function Draw-Leaves([int]$tileX, [int]$tileY) {
    Fill-Tile $tileX $tileY (New-Color 55 132 55) (New-Color 31 95 42) (New-Color 91 166 72)
    Fill-Rect $tileX $tileY 2 3 3 2 (New-Color 96 177 78)
    Fill-Rect $tileX $tileY 10 9 3 2 (New-Color 34 105 44)
}

function Draw-CraftingTable([int]$tileX, [int]$tileY) {
    Draw-Planks $tileX $tileY
    Fill-Rect $tileX $tileY 2 2 12 12 (New-Color 126 77 39 225)
    Fill-Rect $tileX $tileY 3 3 10 2 (New-Color 188 135 71)
    Fill-Rect $tileX $tileY 3 6 10 1 (New-Color 72 46 28)
    Fill-Rect $tileX $tileY 5 8 6 4 (New-Color 151 95 48)
    Fill-Rect $tileX $tileY 7 8 1 4 (New-Color 72 46 28)
}

function Draw-Furnace([int]$tileX, [int]$tileY) {
    Fill-Tile $tileX $tileY (New-Color 95 98 101) (New-Color 60 63 67) (New-Color 136 139 141)
    Fill-Rect $tileX $tileY 3 3 10 10 (New-Color 70 73 77)
    Fill-Rect $tileX $tileY 5 5 6 4 (New-Color 24 24 25)
    Fill-Rect $tileX $tileY 6 8 4 2 (New-Color 212 93 39)
    Fill-Rect $tileX $tileY 7 7 2 1 (New-Color 252 183 79)
}

function Draw-FurnaceBlock([int]$tileX, [int]$tileY, [bool]$active) {
    Fill-Tile $tileX $tileY (New-Color 97 101 103) (New-Color 61 64 68) (New-Color 137 140 143)
    Fill-Rect $tileX $tileY 3 3 10 10 (New-Color 71 74 78)
    Fill-Rect $tileX $tileY 4 4 8 5 (New-Color 31 33 36)
    if ($active) {
        Fill-Rect $tileX $tileY 5 7 6 3 (New-Color 218 89 36)
        Fill-Rect $tileX $tileY 6 6 4 2 (New-Color 252 179 65)
    }
    Fill-Rect $tileX $tileY 4 11 8 2 (New-Color 48 50 54)
}

function Draw-FurnaceArrow([int]$tileX, [int]$tileY, [bool]$filled) {
    $body = if ($filled) { New-Color 226 147 55 245 } else { New-Color 55 61 68 215 }
    $edge = if ($filled) { New-Color 118 73 38 235 } else { New-Color 24 29 36 220 }
    $tip = if ($filled) { New-Color 248 197 83 250 } else { New-Color 83 91 101 220 }
    Fill-Rect $tileX $tileY 1 6 9 4 $body
    Fill-Rect $tileX $tileY 1 5 9 1 $edge
    Fill-Rect $tileX $tileY 1 10 9 1 $edge
    Fill-Rect $tileX $tileY 10 4 2 8 $tip
    Fill-Rect $tileX $tileY 12 5 2 6 $tip
    Fill-Rect $tileX $tileY 14 7 1 2 $tip
    Fill-Rect $tileX $tileY 10 4 1 1 $edge
    Fill-Rect $tileX $tileY 10 11 1 1 $edge
    Fill-Rect $tileX $tileY 14 6 1 1 $edge
    Fill-Rect $tileX $tileY 14 9 1 1 $edge
    if ($filled) {
        Fill-Rect $tileX $tileY 3 6 5 1 (New-Color 255 225 116 170)
        Fill-Rect $tileX $tileY 11 5 2 1 (New-Color 255 233 137 180)
    }
}

function Draw-FurnaceFlame([int]$tileX, [int]$tileY) {
    Fill-Rect $tileX $tileY 6 2 3 4 (New-Color 255 226 93 235)
    Fill-Rect $tileX $tileY 5 5 6 4 (New-Color 245 142 45 245)
    Fill-Rect $tileX $tileY 4 8 8 5 (New-Color 214 72 36 245)
    Fill-Rect $tileX $tileY 3 11 10 3 (New-Color 135 47 43 230)
    Fill-Rect $tileX $tileY 7 6 2 5 (New-Color 255 235 132 245)
    Fill-Rect $tileX $tileY 5 9 2 3 (New-Color 250 169 57 240)
    Fill-Rect $tileX $tileY 10 9 2 3 (New-Color 247 111 43 240)
    Fill-Rect $tileX $tileY 8 3 1 2 (New-Color 255 250 181 210)
}

function Draw-GlassItem([int]$tileX, [int]$tileY) {
    Fill-Rect $tileX $tileY 4 3 8 10 (New-Color 154 220 235 80)
    Fill-Rect $tileX $tileY 4 3 8 1 (New-Color 227 255 255 160)
    Fill-Rect $tileX $tileY 4 12 8 1 (New-Color 82 151 181 130)
    Fill-Rect $tileX $tileY 4 3 1 10 (New-Color 227 255 255 150)
    Fill-Rect $tileX $tileY 11 3 1 10 (New-Color 82 151 181 120)
    Draw-Line $tileX $tileY 6 10 10 5 (New-Color 240 255 255 130) 1
}

function Draw-Apple([int]$tileX, [int]$tileY) {
    Fill-Rect $tileX $tileY 7 2 2 3 (New-Color 93 61 35)
    Fill-Rect $tileX $tileY 9 3 4 2 (New-Color 71 151 57)
    Fill-Rect $tileX $tileY 4 5 8 7 (New-Color 192 35 45)
    Fill-Rect $tileX $tileY 3 7 10 4 (New-Color 218 45 56)
    Fill-Rect $tileX $tileY 5 12 6 2 (New-Color 142 22 35)
    Fill-Rect $tileX $tileY 5 6 2 2 (New-Color 255 111 110)
}

function Draw-Stick([int]$tileX, [int]$tileY) {
    Draw-Line $tileX $tileY 4 13 11 3 (New-Color 126 78 41) 2
    Draw-Line $tileX $tileY 5 13 12 4 (New-Color 82 50 30) 1
}

function Draw-Lump([int]$tileX, [int]$tileY, [System.Drawing.Color]$base, [System.Drawing.Color]$dark, [System.Drawing.Color]$light) {
    Fill-Rect $tileX $tileY 5 4 6 2 $light
    Fill-Rect $tileX $tileY 4 6 9 5 $base
    Fill-Rect $tileX $tileY 6 11 6 2 $dark
    Fill-Rect $tileX $tileY 3 8 2 2 $dark
    Fill-Rect $tileX $tileY 9 5 2 1 (New-Color 255 255 255 80)
}

function Draw-Ingot([int]$tileX, [int]$tileY, [System.Drawing.Color]$base, [System.Drawing.Color]$dark, [System.Drawing.Color]$light) {
    Fill-Rect $tileX $tileY 4 5 8 2 $light
    Fill-Rect $tileX $tileY 3 7 10 4 $base
    Fill-Rect $tileX $tileY 5 11 6 2 $dark
    Fill-Rect $tileX $tileY 5 6 5 1 (New-Color 255 255 255 90)
}

function Draw-DiamondItem([int]$tileX, [int]$tileY) {
    Fill-Rect $tileX $tileY 6 3 4 2 (New-Color 178 255 248)
    Fill-Rect $tileX $tileY 4 5 8 3 (New-Color 72 218 224)
    Fill-Rect $tileX $tileY 5 8 6 2 (New-Color 42 159 186)
    Fill-Rect $tileX $tileY 7 10 2 3 (New-Color 28 119 157)
    Fill-Rect $tileX $tileY 7 5 2 2 (New-Color 235 255 255 160)
}

function Draw-Sapling([int]$tileX, [int]$tileY, [System.Drawing.Color]$leaf, [System.Drawing.Color]$leafDark) {
    Fill-Rect $tileX $tileY 7 8 2 6 (New-Color 96 58 32)
    Fill-Rect $tileX $tileY 4 5 5 3 $leaf
    Fill-Rect $tileX $tileY 8 4 4 4 $leaf
    Fill-Rect $tileX $tileY 5 8 3 2 $leafDark
    Fill-Rect $tileX $tileY 10 7 3 2 $leafDark
}

function Draw-Boat([int]$tileX, [int]$tileY, [System.Drawing.Color]$wood, [System.Drawing.Color]$dark) {
    Fill-Rect $tileX $tileY 3 8 10 3 $wood
    Fill-Rect $tileX $tileY 4 11 8 2 $dark
    Fill-Rect $tileX $tileY 2 9 2 2 $dark
    Fill-Rect $tileX $tileY 12 9 2 2 $dark
    Fill-Rect $tileX $tileY 6 7 4 1 (New-Color 230 180 105 120)
}

function Draw-Seeds([int]$tileX, [int]$tileY) {
    foreach ($point in @(@(5, 6), @(9, 5), @(7, 9), @(11, 10), @(4, 11))) {
        Fill-Rect $tileX $tileY $point[0] $point[1] 2 2 (New-Color 169 139 61)
        Fill-Rect $tileX $tileY ($point[0] + 1) $point[1] 1 1 (New-Color 221 190 91)
    }
}

function Draw-Wheat([int]$tileX, [int]$tileY) {
    Draw-Line $tileX $tileY 7 13 7 4 (New-Color 154 111 41) 1
    foreach ($point in @(@(6, 4), @(8, 5), @(5, 6), @(9, 7), @(6, 8), @(8, 9))) {
        Fill-Rect $tileX $tileY $point[0] $point[1] 2 2 (New-Color 224 181 72)
    }
}

function Draw-Bread([int]$tileX, [int]$tileY) {
    Fill-Rect $tileX $tileY 4 6 8 6 (New-Color 190 123 52)
    Fill-Rect $tileX $tileY 5 4 6 3 (New-Color 226 167 82)
    Fill-Rect $tileX $tileY 5 11 6 2 (New-Color 121 74 38)
    Fill-Rect $tileX $tileY 6 5 2 1 (New-Color 255 217 133 130)
}

function Draw-Feather([int]$tileX, [int]$tileY) {
    Draw-Line $tileX $tileY 5 13 11 3 (New-Color 226 231 218) 2
    Draw-Line $tileX $tileY 6 12 12 4 (New-Color 119 132 135) 1
    Fill-Rect $tileX $tileY 4 9 3 2 (New-Color 190 202 201)
    Fill-Rect $tileX $tileY 8 5 4 2 (New-Color 244 247 234)
}

function Draw-Leather([int]$tileX, [int]$tileY) {
    Fill-Rect $tileX $tileY 4 4 8 8 (New-Color 132 78 43)
    Fill-Rect $tileX $tileY 3 6 2 4 (New-Color 95 55 34)
    Fill-Rect $tileX $tileY 11 6 2 4 (New-Color 95 55 34)
    Fill-Rect $tileX $tileY 6 5 4 2 (New-Color 177 111 61)
}

function Draw-Meat([int]$tileX, [int]$tileY, [System.Drawing.Color]$base, [System.Drawing.Color]$dark, [bool]$cooked) {
    Fill-Rect $tileX $tileY 5 5 7 6 $base
    Fill-Rect $tileX $tileY 4 7 3 4 $base
    Fill-Rect $tileX $tileY 7 11 4 2 $dark
    Fill-Rect $tileX $tileY 10 4 2 2 $(if ($cooked) { New-Color 235 174 93 } else { New-Color 255 172 181 })
}

function Draw-Heart([int]$tileX, [int]$tileY, [double]$fill) {
    $shape = @("01100110", "11111111", "11111111", "11111111", "01111110", "00111100", "00011000")
    for ($row = 0; $row -lt $shape.Count; $row++) {
        for ($col = 0; $col -lt 8; $col++) {
            if ($shape[$row][$col] -eq "1") {
                $color = if (($col / 7.0) -lt $fill) { New-Color 218 42 54 } else { New-Color 68 47 52 210 }
                Fill-Rect $tileX $tileY ($col * 2) (($row + 3) * 2) 2 2 $color
            }
        }
    }
    Fill-Rect $tileX $tileY 4 6 2 2 (New-Color 255 126 136 180)
}

function Draw-Hunger([int]$tileX, [int]$tileY, [double]$fill) {
    $full = New-Color 223 137 48
    $empty = New-Color 78 55 35 215
    for ($y = 3; $y -le 12; $y++) {
        for ($x = 4; $x -le 11; $x++) {
            if ((($x - 7.5) * ($x - 7.5) + ($y - 7.5) * ($y - 7.5)) -lt 24) {
                Set-Pixel ($tileX * $tileSize + $x) ($tileY * $tileSize + $y) $(if ((($x - 4) / 7.0) -lt $fill) { $full } else { $empty })
            }
        }
    }
    Fill-Rect $tileX $tileY 10 10 2 4 $(if ($fill -gt 0.75) { $full } else { $empty })
    Fill-Rect $tileX $tileY 6 5 2 2 (New-Color 255 196 91 170)
}

function Draw-Slot([int]$tileX, [int]$tileY, [bool]$selected) {
    Fill-Rect $tileX $tileY 0 0 16 16 (New-Color 42 45 50 230)
    Fill-Rect $tileX $tileY 1 1 14 14 (New-Color 75 80 87 235)
    Fill-Rect $tileX $tileY 3 3 10 10 (New-Color 24 27 31 225)
    $highlight = if ($selected) { New-Color 255 226 93 } else { New-Color 180 185 195 }
    Fill-Rect $tileX $tileY 0 0 16 2 $highlight
    Fill-Rect $tileX $tileY 0 0 2 16 $highlight
    Fill-Rect $tileX $tileY 0 14 16 2 (New-Color 17 19 23 240)
    Fill-Rect $tileX $tileY 14 0 2 16 (New-Color 17 19 23 240)
}

function Draw-Tool([int]$tileX, [int]$tileY, [string]$kind, [System.Drawing.Color]$head, [System.Drawing.Color]$headDark) {
    $wood = New-Color 103 62 33
    switch ($kind) {
        "pickaxe" {
            Draw-Line $tileX $tileY 5 13 11 7 $wood 2
            Fill-Rect $tileX $tileY 4 3 8 2 $head
            Fill-Rect $tileX $tileY 2 4 4 2 $head
            Fill-Rect $tileX $tileY 10 4 4 2 $headDark
        }
        "axe" {
            Draw-Line $tileX $tileY 6 13 10 5 $wood 2
            Fill-Rect $tileX $tileY 4 3 6 5 $head
            Fill-Rect $tileX $tileY 3 5 3 3 $head
            Fill-Rect $tileX $tileY 9 5 2 2 $headDark
        }
        "shovel" {
            Draw-Line $tileX $tileY 6 13 9 6 $wood 2
            Fill-Rect $tileX $tileY 6 2 4 5 $head
            Fill-Rect $tileX $tileY 5 4 6 2 $headDark
        }
        "sword" {
            Draw-Line $tileX $tileY 5 11 12 4 $head 2
            Fill-Rect $tileX $tileY 4 10 6 2 $headDark
            Fill-Rect $tileX $tileY 3 12 3 2 $wood
            Fill-Rect $tileX $tileY 2 14 3 1 (New-Color 66 40 24)
        }
    }
}

function Draw-Crack([int]$tileX, [int]$tileY, [int]$stage) {
    $alpha = 70 + ($stage * 35)
    $color = New-Color 12 12 12 $alpha
    Draw-Line $tileX $tileY 8 1 7 5 $color 1
    Draw-Line $tileX $tileY 7 5 9 8 $color 1
    Draw-Line $tileX $tileY 9 8 7 14 $color 1
    if ($stage -ge 2) {
        Draw-Line $tileX $tileY 7 5 3 7 $color 1
        Draw-Line $tileX $tileY 9 8 13 10 $color 1
    }
    if ($stage -ge 3) {
        Draw-Line $tileX $tileY 3 7 1 12 $color 1
        Draw-Line $tileX $tileY 13 10 15 14 $color 1
        Draw-Line $tileX $tileY 9 8 11 4 $color 1
    }
    if ($stage -ge 4) {
        Draw-Line $tileX $tileY 7 14 4 15 $color 1
        Draw-Line $tileX $tileY 11 4 14 2 $color 1
        Draw-Line $tileX $tileY 5 4 2 2 $color 1
    }
}

$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
$graphics.Dispose()

# Blocks used by world rendering. Existing coordinates stay stable.
Copy-SourceTile 0 0 "grass_top.png"
Copy-SourceTile 1 0 "grass_side.png" $true
Copy-SourceTile 2 0 "dirt.png"
Copy-SourceTile 3 0 "stone.png"
Copy-SourceTile 4 0 "oak_log.png"
Copy-SourceTile 5 0 "oak_planks.png"
Copy-SourceTile 6 0 "leaves.png"
Copy-SourceTile 7 0 "water.png" $false 0.45
Copy-SourceTile 8 0 "bedrock.png"
Draw-Crack 9 0 1
Draw-Crack 10 0 2
Draw-Crack 11 0 3
Draw-Crack 12 0 4
Copy-SourceTile 13 0 "sand.png"
Copy-SourceTile 14 0 "sandstone.png"
Copy-SourceTile 15 0 "snow.png"

# HUD.
Draw-Heart 0 1 1.0
Draw-Heart 1 1 0.5
Draw-Heart 2 1 0.0
Copy-SourceTile 3 1 "ice.png" $false 0.78
Copy-SourceTile 4 1 "coal_ore.png"
Copy-SourceTile 5 1 "iron_ore.png"
Copy-SourceTile 6 1 "copper_ore.png"
Copy-SourceTile 7 1 "gold_ore.png"
Copy-SourceTile 8 1 "diamond_ore.png"
Copy-SourceTile 9 1 "mossy_stone.png"
Copy-SourceTile 10 1 "gravel.png"
Copy-SourceTile 11 1 "spruce_planks.png"
Copy-SourceTile 12 1 "birch_planks.png"
Copy-SourceTile 13 1 "acacia_planks.png"
Copy-SourceTile 14 1 "dark_oak_planks.png"
Draw-FurnaceBlock 15 1 $false
Draw-Hunger 0 2 1.0
Draw-Hunger 1 2 0.5
Draw-Hunger 2 2 0.0
Copy-SourceTile 11 2 "spruce_log.png"
Copy-SourceTile 12 2 "birch_log.png"
Copy-SourceTile 13 2 "dark_oak_log.png"
Copy-SourceTile 14 2 "spruce_leaves.png"
Copy-SourceTile 15 2 "birch_leaves.png"

# Item icons.
Copy-SourceTile 0 3 "dirt.png"
Copy-SourceTile 1 3 "grass_side.png"
Copy-SourceTile 2 3 "stone.png"
Copy-SourceTile 3 3 "oak_log.png"
Copy-SourceTile 4 3 "leaves.png"
Copy-SourceTile 5 3 "oak_planks.png"
Draw-CraftingTable 6 3
Draw-Furnace 7 3
Draw-Apple 8 3
Copy-SourceTile 9 3 "sand.png"
Copy-SourceTile 10 3 "sandstone.png"
Copy-SourceTile 11 3 "snow.png"
Copy-SourceTile 12 3 "ice.png" $false 0.78
Copy-SourceTile 13 3 "coal_ore.png"
Copy-SourceTile 14 3 "iron_ore.png"
Copy-SourceTile 15 3 "copper_ore.png"
Copy-SourceTile 9 4 "gold_ore.png"
Copy-SourceTile 10 4 "diamond_ore.png"
Copy-SourceTile 11 4 "mossy_stone.png"
Copy-SourceTile 12 4 "gravel.png"
Copy-SourceTile 13 4 "spruce_planks.png"
Copy-SourceTile 14 4 "birch_planks.png"
Copy-SourceTile 15 4 "acacia_planks.png"

$woodHead = New-Color 150 104 58
$woodDark = New-Color 105 69 39
$stoneHead = New-Color 138 143 147
$stoneDark = New-Color 82 87 91
$ironHead = New-Color 213 219 222
$ironDark = New-Color 141 150 154
Draw-Tool 0 4 "pickaxe" $woodHead $woodDark
Draw-Tool 1 4 "axe" $woodHead $woodDark
Draw-Tool 2 4 "shovel" $woodHead $woodDark
Draw-Tool 3 4 "sword" $woodHead $woodDark
Draw-Tool 0 5 "pickaxe" $stoneHead $stoneDark
Draw-Tool 1 5 "axe" $stoneHead $stoneDark
Draw-Tool 2 5 "shovel" $stoneHead $stoneDark
Draw-Stick 3 5
Draw-Lump 4 5 (New-Color 34 37 39) (New-Color 16 17 18) (New-Color 76 82 84)
Draw-Lump 5 5 (New-Color 54 45 35) (New-Color 24 20 17) (New-Color 107 87 63)
Draw-Lump 6 5 (New-Color 160 119 88) (New-Color 92 62 48) (New-Color 225 172 124)
Draw-Lump 7 5 (New-Color 190 100 58) (New-Color 105 55 38) (New-Color 87 186 143)
Draw-Lump 8 5 (New-Color 216 165 56) (New-Color 143 94 36) (New-Color 255 224 100)
Draw-Ingot 9 5 (New-Color 204 211 214) (New-Color 128 138 142) (New-Color 244 249 250)
Draw-Ingot 10 5 (New-Color 229 174 53) (New-Color 160 102 33) (New-Color 255 228 93)
Draw-DiamondItem 11 5
Copy-SourceTile 12 5 "dark_oak_planks.png"
Draw-Tool 0 6 "pickaxe" $ironHead $ironDark
Draw-Tool 1 6 "axe" $ironHead $ironDark
Draw-Tool 2 6 "shovel" $ironHead $ironDark
Draw-Sapling 3 6 (New-Color 79 161 70) (New-Color 40 106 48)
Draw-Sapling 4 6 (New-Color 48 119 67) (New-Color 30 76 49)
Draw-Sapling 5 6 (New-Color 130 183 83) (New-Color 77 127 62)
Draw-Boat 6 6 (New-Color 159 102 55) (New-Color 92 58 35)
Draw-Boat 7 6 (New-Color 110 73 50) (New-Color 61 42 31)
Draw-Seeds 8 6
Draw-Wheat 9 6
Draw-Bread 10 6
Draw-Feather 11 6
Draw-Leather 12 6
Draw-Meat 13 6 (New-Color 201 86 91) (New-Color 129 48 61) $false
Draw-Meat 14 6 (New-Color 166 95 50) (New-Color 105 55 32) $true
Draw-Meat 15 6 (New-Color 186 72 78) (New-Color 116 40 52) $false
Draw-Meat 3 7 (New-Color 219 151 130) (New-Color 145 86 78) $false
Draw-Meat 4 7 (New-Color 151 91 54) (New-Color 94 51 34) $true
Draw-Meat 5 7 (New-Color 201 139 87) (New-Color 122 75 48) $true
Draw-GlassItem 6 7
Draw-FurnaceBlock 7 7 $true
Draw-FurnaceArrow 8 7 $false
Draw-FurnaceArrow 9 7 $true
Draw-FurnaceFlame 10 7
Copy-SourceTile 11 7 "dark_oak_leaves.png"
Copy-SourceTile 12 7 "oak_sapling.png"
Copy-SourceTile 13 7 "spruce_sapling.png"
Copy-SourceTile 14 7 "birch_sapling.png"
Copy-SourceTile 15 7 "dark_oak_sapling.png"

# UI.
Draw-Slot 0 7 $false
Draw-Slot 1 7 $true
Fill-Rect 2 7 0 0 16 16 (New-Color 32 36 42 220)
Fill-Rect 2 7 0 0 16 2 (New-Color 86 91 103 230)
Fill-Rect 2 7 0 14 16 2 (New-Color 12 14 18 230)

$bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
Write-Output "Generated atlas at $outputPath"
