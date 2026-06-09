$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$cacheRoot = Join-Path ([IO.Path]::GetTempPath()) "voxel-game-gradle"
$gradleUserHome = Join-Path $cacheRoot "home"
$projectCache = Join-Path $cacheRoot "project-cache"
$gradleExecutable = Join-Path $projectRoot "gradlew.bat"

New-Item -ItemType Directory -Path $gradleUserHome -Force | Out-Null
New-Item -ItemType Directory -Path $projectCache -Force | Out-Null
$env:GRADLE_USER_HOME = $gradleUserHome

& $gradleExecutable "--project-dir" $projectRoot "--no-configuration-cache" "--project-cache-dir" $projectCache "clean" "jar" "sourceZip"
