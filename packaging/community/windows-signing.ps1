<#
Optional Windows Authenticode signing helpers for the Community packaging
script (package-windows.ps1).

This logic lives in a sibling file so the pure signing-DECISION logic can be
unit tested without executing the whole packaging script.

SECURITY:
  * No credentials are stored in this file.
  * All sensitive inputs (PFX bytes, password) arrive at runtime via environment
    variables / CI secrets only.
  * The certificate password is never echoed; the command preview redacts it.
  * The decoded PFX is written to a unique temp file that is ALWAYS deleted.

This file implements a signing HOOK. Dot-sourcing it does not sign anything and
does not prove any artifact is signed.
#>

# Pure decision function. Given the requested state and inputs, decide whether
# signing is enabled and with which (non-secret) timestamp URL, or throw if an
# explicit signing request is missing required inputs. Performs no I/O and no
# signing, so it is deterministic and safe to unit test. Fail-closed: a request
# with incomplete credentials throws rather than silently producing an unsigned
# artifact under a "signed" label.
function Resolve-WindowsSigningPlan {
    [CmdletBinding()]
    param(
        [bool]$SignRequested,
        [string]$PfxBase64,
        [string]$PfxPassword,
        [string]$TimestampUrl
    )

    if (-not $SignRequested) {
        return [pscustomobject]@{ Enabled = $false; TimestampUrl = $null }
    }

    $missing = @()
    if ([string]::IsNullOrWhiteSpace($PfxBase64))   { $missing += 'WINDOWS_CERT_PFX_BASE64' }
    if ([string]::IsNullOrWhiteSpace($PfxPassword)) { $missing += 'WINDOWS_CERT_PASSWORD' }
    if ($missing.Count -gt 0) {
        throw ("Windows signing was requested (WINDOWS_SIGN=true / -Sign) but required " +
               "secret input(s) are missing: " + ($missing -join ', ') +
               ". Signing is fail-closed; aborting rather than shipping an unsigned artifact.")
    }

    $ts = $TimestampUrl
    if ([string]::IsNullOrWhiteSpace($ts)) { $ts = 'http://timestamp.digicert.com' }
    return [pscustomobject]@{ Enabled = $true; TimestampUrl = $ts }
}

# Locate signtool.exe (Windows SDK). Returns the path or throws.
function Resolve-Signtool {
    $cmd = Get-Command signtool.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $roots = @()
    if (${env:ProgramFiles(x86)}) { $roots += (Join-Path ${env:ProgramFiles(x86)} 'Windows Kits\10\bin') }
    if ($env:ProgramFiles)        { $roots += (Join-Path $env:ProgramFiles 'Windows Kits\10\bin') }
    foreach ($root in $roots) {
        if (Test-Path $root) {
            $found = Get-ChildItem -Path $root -Recurse -Filter signtool.exe -ErrorAction SilentlyContinue |
                Where-Object { $_.FullName -match '\\x64\\' } |
                Sort-Object FullName -Descending | Select-Object -First 1
            if ($found) { return $found.FullName }
        }
    }
    throw "signtool.exe was not found. Install the Windows SDK (which provides signtool) to sign, or run without signing for an unsigned build."
}

# Authenticode-sign a file with RFC3161 timestamping, then verify the signature.
# The decoded PFX is written to a unique temp file that is ALWAYS removed. The
# password is never printed.
function Invoke-WindowsAuthenticodeSign {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$FilePath,
        [Parameter(Mandatory)][string]$PfxBase64,
        [Parameter(Mandatory)][string]$PfxPassword,
        [Parameter(Mandatory)][string]$TimestampUrl,
        [string]$SigntoolPath
    )

    if ([string]::IsNullOrWhiteSpace($SigntoolPath)) { $SigntoolPath = Resolve-Signtool }

    $tempPfx = Join-Path ([System.IO.Path]::GetTempPath()) ("codesign-" + [System.Guid]::NewGuid().ToString('N') + ".pfx")
    try {
        [System.IO.File]::WriteAllBytes($tempPfx, [System.Convert]::FromBase64String($PfxBase64))

        # Command preview with the password and PFX path redacted (never echo secrets).
        Write-Host "    signtool sign /fd SHA256 /f <pfx> /p *** /tr $TimestampUrl /td SHA256 `"$FilePath`"" -ForegroundColor DarkGray

        & $SigntoolPath sign /fd SHA256 /f $tempPfx /p $PfxPassword /tr $TimestampUrl /td SHA256 $FilePath
        if ($LASTEXITCODE -ne 0) { throw "signtool sign failed (exit $LASTEXITCODE)." }

        & $SigntoolPath verify /pa /v $FilePath
        if ($LASTEXITCODE -ne 0) { throw "signtool verify failed (exit $LASTEXITCODE); the artifact is NOT correctly signed." }
    }
    finally {
        if (Test-Path $tempPfx) { Remove-Item $tempPfx -Force -ErrorAction SilentlyContinue }
    }
}
