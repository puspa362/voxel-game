$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradleUserHome = Join-Path $projectRoot ".gradle-user-home"
$gradleExecutable = & (Join-Path $PSScriptRoot "setup-gradle.ps1")

New-Item -ItemType Directory -Path $gradleUserHome -Force | Out-Null
$env:GRADLE_USER_HOME = $gradleUserHome

& $gradleExecutable "--project-dir" $projectRoot "clean" "build"
