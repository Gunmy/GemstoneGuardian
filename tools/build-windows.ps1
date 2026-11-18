# Builds the Windows version: a GemstoneGuardian folder with GemstoneGuardian.exe and its own Java runtime and JavaFX,
# zipped to dist\GemstoneGuardian-windows.zip, to be unzipped anywhere and started with a double click.
# It compiles into dist\build (not target\classes), so a game running from the project folder can stay open.
#
#   powershell -ExecutionPolicy Bypass -File tools\build-windows.ps1
#
# -Jdk is a JDK 17 or newer (it needs jpackage); the default is the Adoptium JDK 17 of the development machine

param(
    [string]$Jdk = "C:\Program Files\Eclipse Adoptium\jdk-17.0.6.10-hotspot"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $root "dist"
$build = Join-Path $dist "build"
$classes = Join-Path $build "classes"
$image = Join-Path $dist "image"
$zip = Join-Path $dist "GemstoneGuardian-windows.zip"

$javac = Join-Path $Jdk "bin\javac.exe"
$jpackage = Join-Path $Jdk "bin\jpackage.exe"
if (-not (Test-Path $jpackage)) { throw "No jpackage in $Jdk\bin: give a JDK 17 or newer with -Jdk" }

# The JavaFX jars from the local Maven cache. The -win jars hold the native DLLs as well
$fx = "$env:USERPROFILE\.m2\repository\org\openjfx"
$mp = (@("base", "controls", "fxml", "graphics") | ForEach-Object { "$fx\javafx-$_\19\javafx-$_-19-win.jar" }) -join ";"
foreach ($jar in $mp.Split(";")) { if (-not (Test-Path $jar)) { throw "Missing JavaFX jar $jar" } }

Write-Host "Cleaning dist"
foreach ($dir in @($build, $image)) { if (Test-Path $dir) { Remove-Item $dir -Recurse -Force } }
if (Test-Path $zip) { Remove-Item $zip -Force }
New-Item -ItemType Directory -Force $classes | Out-Null

Write-Host "Compiling"
$sources = (Get-ChildItem (Join-Path $root "src\main\java") -Recurse -Filter *.java).FullName
& $javac --release 17 -encoding UTF-8 --module-path $mp -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

# The resources go in as they are, except the owner's own settings (the game writes defaults when there are none) and
# the highscore, which starts empty (-1.0, as reset writes it)
Write-Host "Copying resources"
Copy-Item (Join-Path $root "src\main\resources\*") $classes -Recurse -Force
$reading = Join-Path $classes "game\reading"
Remove-Item (Join-Path $reading "settings.txt") -ErrorAction SilentlyContinue
[IO.File]::WriteAllText((Join-Path $reading "highscore.txt"), "-1.0")

# The icon is the rock from the sprite sheet (the 20x20 cell at (200, 300)), scaled up without smoothing
Write-Host "Making the icon"
Add-Type -AssemblyName System.Drawing
$icon = Join-Path $build "GemstoneGuardian.ico"
$sheet = [System.Drawing.Bitmap]::FromFile((Join-Path $root "src\main\resources\game\media\tiles.png"))
$sizes = @(16, 32, 48, 256)
$pngs = @()
foreach ($size in $sizes) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.DrawImage($sheet, (New-Object System.Drawing.Rectangle 0, 0, $size, $size), 200, 300, 20, 20, [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose()
    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $pngs += , $ms.ToArray()
}
$sheet.Dispose()
$out = New-Object System.IO.MemoryStream
$w = New-Object System.IO.BinaryWriter $out
$w.Write([UInt16]0); $w.Write([UInt16]1); $w.Write([UInt16]$sizes.Count)
$offset = 6 + 16 * $sizes.Count
for ($i = 0; $i -lt $sizes.Count; $i++) {
    $s = $sizes[$i] % 256
    $w.Write([byte]$s); $w.Write([byte]$s); $w.Write([byte]0); $w.Write([byte]0)
    $w.Write([UInt16]1); $w.Write([UInt16]32)
    $w.Write([UInt32]$pngs[$i].Length); $w.Write([UInt32]$offset)
    $offset += $pngs[$i].Length
}
foreach ($png in $pngs) { $w.Write($png) }
$w.Flush()
[IO.File]::WriteAllBytes($icon, $out.ToArray())
$w.Dispose()

# The runtime holds only the modules the game needs, plus jdk.crypto.ec, without which https does not work (style.css
# imports the font from Google Fonts; the game has its own copy too)
Write-Host "Packaging with jpackage"
& $jpackage --type app-image --name GemstoneGuardian --app-version 1.0 --vendor "Gemstone Guardian" `
    --icon $icon --module-path "$mp;$classes" --module GemstoneGuardian/game.app --add-modules jdk.crypto.ec --dest $image
if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

Write-Host "Zipping"
Compress-Archive -Path (Join-Path $image "GemstoneGuardian") -DestinationPath $zip -CompressionLevel Optimal
$mb = [math]::Round((Get-Item $zip).Length / 1MB, 1)
Write-Host "Done: $zip ($mb MB)"
