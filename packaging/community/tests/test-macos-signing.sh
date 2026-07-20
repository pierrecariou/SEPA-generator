#!/usr/bin/env bash
#
# Focused tests for the macOS signing DECISION logic (resolve_macos_signing_plan).
# No Apple tooling and no certificate are required: these exercise the pure
# decision branches only.
# Run: bash packaging/community/tests/test-macos-signing.sh
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../macos-signing.sh
source "${HERE}/../macos-signing.sh"

fails=0
pass()  { printf 'PASS: %s\n' "$1"; }
failc() { printf 'FAIL: %s\n' "$1"; fails=$((fails + 1)); }

reset_env() {
  unset MAC_SIGN MAC_NOTARIZE MAC_SIGNING_IDENTITY \
        MACOS_CERT_P12_BASE64 MACOS_CERT_PASSWORD \
        APPLE_ID APPLE_TEAM_ID APPLE_APP_PASSWORD
}

# 1. Disabled by default.
reset_env
if resolve_macos_signing_plan 2>/dev/null && [ "${MAC_DO_SIGN}" = "false" ] && [ "${MAC_DO_NOTARIZE}" = "false" ]; then
  pass "disabled by default"
else
  failc "disabled by default"
fi

# 2. Sign requested without identity -> rejected.
reset_env; MAC_SIGN=true
if resolve_macos_signing_plan 2>/dev/null; then failc "sign without identity should fail"; else pass "sign without identity rejected"; fi

# 3. Complete sign config -> sign enabled, notarize off.
reset_env; MAC_SIGN=true; MAC_SIGNING_IDENTITY="Developer ID Application: X (TEAMID)"
if resolve_macos_signing_plan 2>/dev/null && [ "${MAC_DO_SIGN}" = "true" ] && [ "${MAC_DO_NOTARIZE}" = "false" ]; then
  pass "sign enabled"
else
  failc "sign enabled"
fi

# 4. p12 supplied without its password -> rejected.
reset_env; MAC_SIGN=true; MAC_SIGNING_IDENTITY="id"; MACOS_CERT_P12_BASE64="QUJD"
if resolve_macos_signing_plan 2>/dev/null; then failc "p12 without password should fail"; else pass "p12 without password rejected"; fi

# 5. Notarize requested without signing -> rejected.
reset_env; MAC_NOTARIZE=true
if resolve_macos_signing_plan 2>/dev/null; then failc "notarize without sign should fail"; else pass "notarize without sign rejected"; fi

# 6. Notarize requested, signing on, but Apple creds incomplete -> rejected.
reset_env; MAC_SIGN=true; MAC_SIGNING_IDENTITY="id"; MAC_NOTARIZE=true; APPLE_ID="a@b.c"
if resolve_macos_signing_plan 2>/dev/null; then failc "notarize with missing creds should fail"; else pass "notarize with missing creds rejected"; fi

# 7. Full notarize config -> both enabled.
reset_env; MAC_SIGN=true; MAC_SIGNING_IDENTITY="id"; MAC_NOTARIZE=true
APPLE_ID="a@b.c"; APPLE_TEAM_ID="TEAMID"; APPLE_APP_PASSWORD="pw"
if resolve_macos_signing_plan 2>/dev/null && [ "${MAC_DO_SIGN}" = "true" ] && [ "${MAC_DO_NOTARIZE}" = "true" ]; then
  pass "full notarize enabled"
else
  failc "full notarize enabled"
fi

# 8. Error output must not leak the Apple app password value.
reset_env; MAC_SIGN=true; MAC_SIGNING_IDENTITY="id"; MAC_NOTARIZE=true; APPLE_APP_PASSWORD="S3cr3tAppPw"
msg="$(resolve_macos_signing_plan 2>&1 || true)"
if printf '%s' "${msg}" | grep -q "S3cr3tAppPw"; then
  failc "app password leaked in error output"
else
  pass "no app password leak in error output"
fi

if [ "${fails}" -gt 0 ]; then
  printf '%d macOS signing-plan test(s) failed.\n' "${fails}"
  exit 1
fi
printf 'All macOS signing-plan tests passed.\n'
