<#
.SYNOPSIS
    Builds a professional Windows installer for SEPA Generator Community Edition
    using the official JDK `jpackage` tool.

.DESCRIPTION
    This script produces a real Windows installer (MSI by default, EXE optionally)
    that:
      * installs the application under Program Files,
      * bundles its own Java runtime (no separate JRE/JDK required on the target machine),
      * registers the app in "Apps & features" (installed apps),
      * creates a Start Menu entry,
      * creates a Desktop shortcut,
      * can be uninstalled cleanly.

    It keeps Maven as the main build tool and only uses `jpackage` (shipped with
    the JDK) for packaging. WiX Toolset is required by `jpackage` to build MSI/EXE
    installers on Windows.

.NOTES
    Community Edition only. macOS/Linux packaging is handled separately.

    -------------------------------------------------------------------------
    Manual MSI upgrade smoke test (run before publishing a new version)
    -------------------------------------------------------------------------
    Goal: confirm that installing a newer MSI upgrades in place (one app, kept
    preferences) rather than installing a second copy.

      a. Install the previous version MSI (e.g. v1.3.0) and launch the app.
      b. Change some settings: switch theme, pick a default SEPA format, and
         fill in debtor / initiating-party details, then close the app.
      c. Bump the module version in the Maven POMs to the new version, rebuild
         the MSI (this script derives $AppVersion from Maven), and install it
         WITHOUT uninstalling the old one.
      d. Open "Apps & features": confirm only ONE "SEPA Generator Community"
         entry remains (the new version), not two side-by-side installs.
      e. Confirm the Start Menu and Desktop shortcuts still launch the app.
      f. Confirm preferences are preserved: theme, default SEPA format and
         debtor settings are still the values set in step (b). These live in
         %USERPROFILE% (~/.sepa-generator-config.json and the Java Preferences
         registry node), so the in-place upgrade must not reset them.
      g. Confirm the title bar / About shows the new version.

    The in-place upgrade relies on a STABLE UpgradeUuid and a stable AppName
    across releases (both come from packaging/edition.properties). Do not change
    them between versions.
#>

[CmdletBinding()]
param(
    # Installer type produced by jpackage. MSI is preferred and is the default.
    # "exe" is an optional later target. "app-image" produces an unpacked
    # application folder (no installer, no WiX needed) and is only meant for
    # local smoke-testing.
    [ValidateSet('msi', 'exe', 'app-image')]
    [string]$Type = 'msi',

    # Skip the Maven build (use the jar already present in target/).
    [switch]$SkipBuild,

    # Architecture tag for the output file name (e.g. "x64"). This does NOT
    # affect the build - jpackage produces an installer for the architecture of
    # the Windows runner; the label only makes the release artifact name explicit
    # (SEPA-Generator-Community-<ver>-windows-<arch>.<type>).
    [string]$ArchLabel = 'x64'
)

# -----------------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------------
$ErrorActionPreference = 'Stop'

function Write-Step  ([string]$m) { Write-Host "==> $m" -ForegroundColor Cyan }
function Write-Ok    ([string]$m) { Write-Host "    $m" -ForegroundColor Green }
function Fail        ([string]$m) { Write-Host "ERROR: $m" -ForegroundColor Red; exit 1 }

# Parse a simple KEY=VALUE .properties file into a hashtable (ignores blank
# lines and '#' comments; values are taken verbatim after the first '=').
function Read-EditionProperties ([string]$path) {
    if (-not (Test-Path $path)) { Fail "Edition properties file not found: $path" }
    $props = @{}
    foreach ($line in Get-Content -LiteralPath $path) {
        $trim = $line.Trim()
        if ($trim -eq '' -or $trim.StartsWith('#')) { continue }
        $idx = $trim.IndexOf('=')
        if ($idx -lt 1) { continue }
        $props[$trim.Substring(0, $idx).Trim()] = $trim.Substring($idx + 1).Trim()
    }
    return $props
}

function Get-EditionValue ([hashtable]$props, [string]$key) {
    if (-not $props.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($props[$key])) {
        Fail "Required key '$key' missing from edition.properties."
    }
    return $props[$key]
}

# Locate Maven (mvn.cmd preferred on Windows, then mvn).
function Resolve-Mvn {
    $m = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if (-not $m) { $m = Get-Command mvn -ErrorAction SilentlyContinue }
    if (-not $m) { Fail 'Maven (mvn) was not found on PATH. Install Maven or add it to PATH.' }
    return $m.Source
}

# Derive the authoritative application version from the Maven generator module,
# so the packaging version can never drift from the POM.
function Get-MavenVersion ([string]$repoRoot, [string]$mvn) {
    $out = & $mvn -q -DforceStdout -pl generator -f (Join-Path $repoRoot 'pom.xml') `
        org.apache.maven.plugins:maven-help-plugin:3.5.0:evaluate '-Dexpression=project.version'
    if ($LASTEXITCODE -ne 0) { Fail 'Failed to derive the application version from Maven (help:evaluate).' }
    $version = ($out | Select-Object -Last 1).ToString().Trim()
    if ([string]::IsNullOrWhiteSpace($version) -or $version.StartsWith('$')) {
        Fail "Maven returned an invalid application version: '$version'."
    }
    return $version
}

# -----------------------------------------------------------------------------
# Configuration (from packaging/edition.properties + authoritative Maven version)
# -----------------------------------------------------------------------------
# Paths are resolved relative to the repository root (two levels above this
# script: <repo>/packaging/community/package-windows.ps1).
$RepoRoot     = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$EditionFile  = Join-Path $RepoRoot 'packaging\edition.properties'
$Edition      = Read-EditionProperties $EditionFile

$AppName      = Get-EditionValue $Edition 'APP_NAME'
$ArtifactSlug = Get-EditionValue $Edition 'ARTIFACT_SLUG'
$Vendor       = Get-EditionValue $Edition 'VENDOR'
$Description  = Get-EditionValue $Edition 'DESCRIPTION'
$MainClass    = Get-EditionValue $Edition 'MAIN_CLASS'
$UpgradeUuid  = Get-EditionValue $Edition 'WIN_UPGRADE_UUID'
$Copyright    = "Copyright (c) $(Get-Date -Format yyyy) $Vendor"

# Authoritative application version (derived from Maven, never hardcoded).
$Mvn         = Resolve-Mvn
$AppVersion  = Get-MavenVersion $RepoRoot $Mvn

# Runnable fat JAR produced by the Maven build (generator module, shaded).
$MainJarName = "generator-$AppVersion.jar"
$MainJarPath = Join-Path $RepoRoot "generator\target\$MainJarName"
$IconPath    = Join-Path $RepoRoot 'packaging\windows\sepa-generator.ico'
$OutputDir   = Join-Path $RepoRoot 'dist'

# jpackage staging directories (kept under generator/target, which is git-ignored).
$StageRoot   = Join-Path $RepoRoot 'generator\target\jpackage'
$InputDir    = Join-Path $StageRoot 'input'
$JpDestDir   = Join-Path $StageRoot 'out'

# Final installer name placed in dist/.
$FinalArtifact = "$ArtifactSlug-$AppVersion-windows-$ArchLabel.$Type"

# -----------------------------------------------------------------------------
# 1. Verify we are on Windows
# -----------------------------------------------------------------------------
Write-Step 'Checking operating system'
$onWindows = ($env:OS -eq 'Windows_NT') -or
             ([System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform(
                 [System.Runtime.InteropServices.OSPlatform]::Windows))
if (-not $onWindows) {
    Fail 'This script builds a Windows installer and must run on Windows. macOS/Linux packaging is handled separately.'
}
Write-Ok 'Running on Windows.'

# -----------------------------------------------------------------------------
# 2. Locate jpackage (JDK 17+)
# -----------------------------------------------------------------------------
Write-Step 'Locating jpackage'
$jpackage = $null

# Prefer JAVA_HOME, then PATH, then a few well-known JDK install locations.
$candidates = @()
if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME 'bin\jpackage.exe') }
$onPath = Get-Command jpackage.exe -ErrorAction SilentlyContinue
if ($onPath) { $candidates += $onPath.Source }
$candidates += (Get-ChildItem -Path @(
    "$env:ProgramFiles\Java",
    "$env:ProgramFiles\Eclipse Adoptium",
    "$env:ProgramFiles\Microsoft",
    "$env:ProgramFiles\Zulu",
    "$env:ProgramFiles\Amazon Corretto"
) -Recurse -Filter 'jpackage.exe' -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty FullName)

foreach ($c in $candidates) {
    if ($c -and (Test-Path $c)) { $jpackage = $c; break }
}

if (-not $jpackage) {
    Fail @"
jpackage was not found.
  jpackage ships with the JDK (17 or newer). Install a JDK and either:
    * set JAVA_HOME to the JDK directory, or
    * add <jdk>\bin to PATH.
  Recommended: a current LTS or later JDK (this project builds with JDK 23).
"@
}
$jpVersion = (& $jpackage --version) 2>&1
Write-Ok "jpackage found: $jpackage (version $jpVersion)"

# -----------------------------------------------------------------------------
# 3. Verify WiX (required by jpackage for MSI/EXE on Windows)
# -----------------------------------------------------------------------------
if ($Type -in @('msi', 'exe')) {
    Write-Step "Checking WiX Toolset (required for '$Type' installers)"
    # jpackage works with WiX 3.x (candle.exe/light.exe) or WiX 4/5 (wix.exe).
    $wix3 = Get-Command candle.exe -ErrorAction SilentlyContinue
    $wix4 = Get-Command wix.exe    -ErrorAction SilentlyContinue
    if (-not ($wix3 -or $wix4)) {
        Fail @"
WiX Toolset was not found on PATH, so jpackage cannot build a '$Type' installer.
  This is the only blocker for MSI/EXE generation (it is a jpackage prerequisite,
  not a Maven dependency). Smallest fix - install WiX, then re-run this script:

    Option A (WiX 4/5, recommended):
      winget install --id WiXToolset.WiX
        - or -
      dotnet tool install --global wix

    Option B (classic WiX 3.x):
      winget install --id WiXToolset.WiXToolset
      (then add the WiX 'bin' directory containing candle.exe/light.exe to PATH)

  After installing, open a new shell so PATH is refreshed and run again.
  To smoke-test packaging WITHOUT WiX, you can run:  -Type app-image
  (that produces an unpacked app folder, not an installer).
"@
    }
    if ($wix4) { Write-Ok "WiX found: $($wix4.Source)" } else { Write-Ok "WiX found: $($wix3.Source)" }
}

# -----------------------------------------------------------------------------
# 4. Verify supporting inputs and build with Maven
# -----------------------------------------------------------------------------
if (-not (Test-Path $IconPath)) { Fail "Application icon not found: $IconPath" }

if ($SkipBuild) {
    Write-Step 'Skipping Maven build (-SkipBuild)'
} else {
    Write-Step 'Building application with Maven (mvn clean package)'
    Push-Location $RepoRoot
    try {
        & $Mvn clean package
        if ($LASTEXITCODE -ne 0) { Fail "Maven build failed (exit code $LASTEXITCODE)." }
    } finally {
        Pop-Location
    }
    Write-Ok 'Maven build succeeded.'
}

if (-not (Test-Path $MainJarPath)) {
    Fail "Runnable JAR not found: $MainJarPath`n  Run the Maven build first (omit -SkipBuild)."
}
Write-Ok "Application JAR: $MainJarPath"

# -----------------------------------------------------------------------------
# 5. Prepare the jpackage input directory (only the app jar goes here)
# -----------------------------------------------------------------------------
Write-Step 'Preparing jpackage input directory'
if (Test-Path $StageRoot) { Remove-Item $StageRoot -Recurse -Force }
New-Item -ItemType Directory -Path $InputDir  -Force | Out-Null
New-Item -ItemType Directory -Path $JpDestDir -Force | Out-Null
Copy-Item $MainJarPath -Destination (Join-Path $InputDir $MainJarName) -Force
Write-Ok "Staged $MainJarName for packaging."

if (-not (Test-Path $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null }

# -----------------------------------------------------------------------------
# 6. Run jpackage
# -----------------------------------------------------------------------------
Write-Step "Running jpackage (--type $Type)"

$jpArgs = @(
    '--type',       $Type,
    '--name',       $AppName,
    '--app-version', $AppVersion,
    '--vendor',     $Vendor,
    '--copyright',  $Copyright,
    '--description', $Description,
    '--input',      $InputDir,
    '--main-jar',   $MainJarName,
    '--main-class', $MainClass,
    '--icon',       $IconPath,
    '--dest',       $JpDestDir
)

# Windows installer options (MSI/EXE only).
if ($Type -in @('msi', 'exe')) {
    $jpArgs += @(
        '--win-menu',
        '--win-menu-group', $AppName,
        '--win-shortcut',          # Desktop shortcut
        '--win-dir-chooser',       # let the user pick the install directory
        '--win-upgrade-uuid', $UpgradeUuid
    )
}

Write-Host "    $jpackage $($jpArgs -join ' ')" -ForegroundColor DarkGray
& $jpackage @jpArgs
if ($LASTEXITCODE -ne 0) {
    Fail "jpackage failed (exit code $LASTEXITCODE). See the output above for the exact cause."
}

# -----------------------------------------------------------------------------
# 7. Move the produced artifact into dist/ with the canonical name
# -----------------------------------------------------------------------------
Write-Step 'Collecting installer'
if ($Type -eq 'app-image') {
    # jpackage produces a folder named after the app.
    $produced = Join-Path $JpDestDir $AppName
    $finalPath = Join-Path $OutputDir "SEPA-Generator-Community-$AppVersion-windows-$ArchLabel-app-image"
    if (Test-Path $finalPath) { Remove-Item $finalPath -Recurse -Force }
    Move-Item $produced $finalPath
    Write-Ok "App image: $finalPath"
} else {
    # jpackage names the file "<AppName>-<version>.<type>".
    $produced = Get-ChildItem -Path $JpDestDir -Filter "*.$Type" | Select-Object -First 1
    if (-not $produced) { Fail "Could not find the generated .$Type in $JpDestDir." }
    $finalPath = Join-Path $OutputDir $FinalArtifact
    if (Test-Path $finalPath) { Remove-Item $finalPath -Force }
    Move-Item $produced.FullName $finalPath
    Write-Ok "Installer: $finalPath"
}

Write-Host ''
Write-Host "SUCCESS: $AppName $AppVersion packaged." -ForegroundColor Green
Write-Host "Output : $finalPath" -ForegroundColor Green
