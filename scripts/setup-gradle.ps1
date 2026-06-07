param(
    [string]$GradleVersion = "9.4.0"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsDirectory = Join-Path $projectRoot ".tools"
$distributionDirectory = Join-Path $toolsDirectory "gradle-$GradleVersion"
$gradleExecutable = Join-Path $distributionDirectory "bin\\gradle.bat"

if (Test-Path -LiteralPath $gradleExecutable) {
    Write-Output $gradleExecutable
    exit 0
}

New-Item -ItemType Directory -Path $toolsDirectory -Force | Out-Null

$archivePath = Join-Path ([IO.Path]::GetTempPath()) ("gradle-$GradleVersion-" + [Guid]::NewGuid() + ".zip")
$distributionUrl = "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"

Write-Host "Downloading Gradle $GradleVersion..."
$curlExecutable = Get-Command curl.exe -ErrorAction SilentlyContinue
if ($null -ne $curlExecutable) {
    & $curlExecutable.Source "--fail" "--location" "--output" $archivePath $distributionUrl
} else {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $distributionUrl -OutFile $archivePath
}

if (!(Test-Path -LiteralPath $archivePath) -or (Get-Item -LiteralPath $archivePath).Length -eq 0) {
    throw "Failed to download Gradle distribution from $distributionUrl."
}

Write-Host "Extracting Gradle..."
Expand-Archive -LiteralPath $archivePath -DestinationPath $toolsDirectory -Force
Remove-Item -LiteralPath $archivePath -Force

Write-Output $gradleExecutable
