<#
Focused tests for the Windows signing DECISION logic (Resolve-WindowsSigningPlan).
No certificate and no signtool are required: these exercise the pure decision
branches only. Run: pwsh -File packaging/community/tests/test-windows-signing.ps1
#>
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '..\windows-signing.ps1')

$script:failures = 0
function Check([string]$name, [scriptblock]$body) {
    try { & $body; Write-Host "PASS: $name" -ForegroundColor Green }
    catch { $script:failures++; Write-Host "FAIL: $name -> $($_.Exception.Message)" -ForegroundColor Red }
}

Check 'signing disabled by default (no request)' {
    $p = Resolve-WindowsSigningPlan -SignRequested $false -PfxBase64 '' -PfxPassword '' -TimestampUrl ''
    if ($p.Enabled) { throw 'expected Enabled = false' }
}

Check 'partial config rejected (missing password)' {
    $threw = $false
    try { Resolve-WindowsSigningPlan -SignRequested $true -PfxBase64 'QUJD' -PfxPassword '' -TimestampUrl '' }
    catch { $threw = $true; if ($_.Exception.Message -notmatch 'WINDOWS_CERT_PASSWORD') { throw 'error should name the missing input' } }
    if (-not $threw) { throw 'expected a throw' }
}

Check 'partial config rejected (missing pfx)' {
    $threw = $false
    try { Resolve-WindowsSigningPlan -SignRequested $true -PfxBase64 '' -PfxPassword 'pw' -TimestampUrl '' }
    catch { $threw = $true; if ($_.Exception.Message -notmatch 'WINDOWS_CERT_PFX_BASE64') { throw 'error should name the missing input' } }
    if (-not $threw) { throw 'expected a throw' }
}

Check 'complete config enables signing with default timestamp' {
    $p = Resolve-WindowsSigningPlan -SignRequested $true -PfxBase64 'QUJD' -PfxPassword 'pw' -TimestampUrl ''
    if (-not $p.Enabled) { throw 'expected Enabled = true' }
    if ([string]::IsNullOrWhiteSpace($p.TimestampUrl)) { throw 'expected a default timestamp URL' }
}

Check 'custom timestamp url respected' {
    $p = Resolve-WindowsSigningPlan -SignRequested $true -PfxBase64 'QUJD' -PfxPassword 'pw' -TimestampUrl 'http://ts.example'
    if ($p.TimestampUrl -ne 'http://ts.example') { throw 'custom timestamp not honored' }
}

Check 'no secret rendered in plan object or error output' {
    $secret = 'S3cr3t-PfxPassword'
    $p = Resolve-WindowsSigningPlan -SignRequested $true -PfxBase64 'QUJD' -PfxPassword $secret -TimestampUrl ''
    $rendered = ($p | Format-List | Out-String)
    if ($rendered -match [regex]::Escape($secret)) { throw 'password leaked into the plan object' }
    try { Resolve-WindowsSigningPlan -SignRequested $true -PfxBase64 '' -PfxPassword $secret -TimestampUrl '' }
    catch { if ($_.Exception.Message -match [regex]::Escape($secret)) { throw 'password leaked into the error message' } }
}

if ($script:failures -gt 0) { Write-Host "$($script:failures) test(s) failed." -ForegroundColor Red; exit 1 }
Write-Host 'All Windows signing-plan tests passed.' -ForegroundColor Green
