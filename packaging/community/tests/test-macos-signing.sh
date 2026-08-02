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
        APPLE_API_KEY_P8_BASE64 APPLE_API_KEY_ID APPLE_API_ISSUER_ID
}

VALID_IDENTITY="Developer ID Application: NIRYOSYS (233LH9G5PD)"

# -----------------------------------------------------------------------------
# team_id_from_identity (pure)
# -----------------------------------------------------------------------------
if [ "$(team_id_from_identity "${VALID_IDENTITY}")" = "233LH9G5PD" ]; then
  pass "team id extracted from a Developer ID identity"
else
  failc "team id extracted from a Developer ID identity"
fi

if team_id_from_identity "Developer ID Application: NIRYOSYS" >/dev/null 2>&1; then
  failc "identity without a team suffix should be rejected"
else
  pass "identity without a team suffix rejected"
fi

# -----------------------------------------------------------------------------
# json_scalar (pure) — parses the flat JSON emitted by notarytool.
# -----------------------------------------------------------------------------
NOTARY_JSON='{"id":"1234abcd-56ef-78ab-90cd-1234567890ab","message":"Processing complete","status":"Accepted"}'
if [ "$(json_scalar "${NOTARY_JSON}" id)" = "1234abcd-56ef-78ab-90cd-1234567890ab" ]; then
  pass "notarytool submission id parsed"
else
  failc "notarytool submission id parsed"
fi
if [ "$(json_scalar "${NOTARY_JSON}" status)" = "Accepted" ]; then
  pass "notarytool status parsed"
else
  failc "notarytool status parsed"
fi
if [ "$(json_scalar '{"id":"x","status":"Invalid"}' status)" = "Invalid" ]; then
  pass "rejected notarytool status parsed"
else
  failc "rejected notarytool status parsed"
fi
if json_scalar '{"message":"no status here"}' status >/dev/null 2>&1; then
  failc "missing json key should be rejected"
else
  pass "missing json key rejected"
fi

# -----------------------------------------------------------------------------
# resolve_macos_signing_plan (pure)
# -----------------------------------------------------------------------------

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

# 2b. Sign requested with an identity that carries no Team ID -> rejected, so
#     the produced artifact can always be verified against a known team.
reset_env; MAC_SIGN=true; MAC_SIGNING_IDENTITY="Developer ID Application: NIRYOSYS"
if resolve_macos_signing_plan 2>/dev/null; then failc "identity without team id should fail"; else pass "identity without team id rejected"; fi

# 3. Complete sign config -> sign enabled, notarize off.
reset_env; MAC_SIGN=true; MAC_SIGNING_IDENTITY="${VALID_IDENTITY}"
if resolve_macos_signing_plan 2>/dev/null && [ "${MAC_DO_SIGN}" = "true" ] && [ "${MAC_DO_NOTARIZE}" = "false" ]; then
  pass "sign enabled"
else
  failc "sign enabled"
fi

# 4. p12 supplied without its password -> rejected.
reset_env; MAC_SIGN=true; MAC_SIGNING_IDENTITY="${VALID_IDENTITY}"; MACOS_CERT_P12_BASE64="QUJD"
if resolve_macos_signing_plan 2>/dev/null; then failc "p12 without password should fail"; else pass "p12 without password rejected"; fi

# 5. Notarize requested without signing -> rejected.
reset_env; MAC_NOTARIZE=true
if resolve_macos_signing_plan 2>/dev/null; then failc "notarize without sign should fail"; else pass "notarize without sign rejected"; fi

# 6. Notarize requested, signing on, but the API key is incomplete -> rejected.
reset_env; MAC_SIGN=true; MAC_SIGNING_IDENTITY="${VALID_IDENTITY}"; MAC_NOTARIZE=true
APPLE_API_KEY_P8_BASE64="QUJD"
if resolve_macos_signing_plan 2>/dev/null; then failc "notarize with missing key id/issuer should fail"; else pass "notarize with missing key id/issuer rejected"; fi

# 6b. Key id present but issuer missing -> rejected.
reset_env; MAC_SIGN=true; MAC_SIGNING_IDENTITY="${VALID_IDENTITY}"; MAC_NOTARIZE=true
APPLE_API_KEY_P8_BASE64="QUJD"; APPLE_API_KEY_ID="KEYID12345"
if resolve_macos_signing_plan 2>/dev/null; then failc "notarize with missing issuer should fail"; else pass "notarize with missing issuer rejected"; fi

# 6c. Issuer and key id present but no key material -> rejected.
reset_env; MAC_SIGN=true; MAC_SIGNING_IDENTITY="${VALID_IDENTITY}"; MAC_NOTARIZE=true
APPLE_API_KEY_ID="KEYID12345"; APPLE_API_ISSUER_ID="11111111-2222-3333-4444-555555555555"
if resolve_macos_signing_plan 2>/dev/null; then failc "notarize with missing p8 should fail"; else pass "notarize with missing p8 rejected"; fi

# 7. Full notarize config -> both enabled.
reset_env; MAC_SIGN=true; MAC_SIGNING_IDENTITY="${VALID_IDENTITY}"; MAC_NOTARIZE=true
APPLE_API_KEY_P8_BASE64="QUJD"; APPLE_API_KEY_ID="KEYID12345"
APPLE_API_ISSUER_ID="11111111-2222-3333-4444-555555555555"
if resolve_macos_signing_plan 2>/dev/null && [ "${MAC_DO_SIGN}" = "true" ] && [ "${MAC_DO_NOTARIZE}" = "true" ]; then
  pass "full notarize enabled"
else
  failc "full notarize enabled"
fi

# 8. Error output must not leak the API key material or the certificate password.
reset_env; MAC_SIGN=true; MAC_SIGNING_IDENTITY="${VALID_IDENTITY}"; MAC_NOTARIZE=true
APPLE_API_KEY_P8_BASE64="S3cr3tKeyMaterial"; MACOS_CERT_P12_BASE64="QUJD"; MACOS_CERT_PASSWORD=""
msg="$(resolve_macos_signing_plan 2>&1 || true)"
if printf '%s' "${msg}" | grep -q "S3cr3tKeyMaterial"; then
  failc "API key material leaked in error output"
else
  pass "no API key leak in error output"
fi

# -----------------------------------------------------------------------------
# 9. Entitlements plist must stay parseable by Apple's AMFIUnserializeXML.
#    That parser rejects XML comments outright ("syntax error"), which fails
#    codesign and therefore the whole build. Regression guard.
# -----------------------------------------------------------------------------
ENTITLEMENTS="${HERE}/../../macos/entitlements.plist"
if [ -f "${ENTITLEMENTS}" ]; then
  pass "entitlements plist exists"

  if grep -q '<!--' "${ENTITLEMENTS}"; then
    failc "entitlements plist must not contain XML comments (AMFI rejects them)"
  else
    pass "entitlements plist contains no XML comments"
  fi

  missing=""
  for key in com.apple.security.cs.allow-jit \
             com.apple.security.cs.allow-unsigned-executable-memory \
             com.apple.security.cs.disable-library-validation; do
    grep -q "<key>${key}</key>" "${ENTITLEMENTS}" || missing="${missing} ${key}"
  done
  if [ -n "${missing}" ]; then
    failc "entitlements plist is missing required JVM entitlement(s):${missing}"
  else
    pass "entitlements plist declares the required JVM entitlements"
  fi

  if command -v plutil >/dev/null 2>&1; then
    if plutil -lint "${ENTITLEMENTS}" >/dev/null 2>&1; then
      pass "entitlements plist passes plutil -lint"
    else
      failc "entitlements plist passes plutil -lint"
    fi
  fi
else
  failc "entitlements plist exists"
fi

# -----------------------------------------------------------------------------
# 10. JAR-embedded native libraries must be signed too.
#     Apple's notary service inspects binaries inside JARs; jpackage does not
#     sign them. An unsigned Mach-O there is a critical notarization error.
# -----------------------------------------------------------------------------
if declare -f sign_jar_native_binaries >/dev/null; then
  pass "sign_jar_native_binaries is defined"
else
  failc "sign_jar_native_binaries is defined"
fi

# Fails closed when the JAR does not exist.
if sign_jar_native_binaries "${HERE}/definitely-missing.jar" >/dev/null 2>&1; then
  failc "missing JAR should be rejected"
else
  pass "missing JAR rejected"
fi

# A JAR with no native libraries is a no-op, not a failure.
if command -v unzip >/dev/null 2>&1 && command -v jar >/dev/null 2>&1; then
  nat_tmp="$(mktemp -d)"
  ( cd "${nat_tmp}" && mkdir -p pkg && echo 'x' > pkg/Plain.txt && jar cf empty.jar pkg ) >/dev/null 2>&1
  if sign_jar_native_binaries "${nat_tmp}/empty.jar" >/dev/null 2>&1; then
    pass "JAR without native libraries is a no-op"
  else
    failc "JAR without native libraries is a no-op"
  fi
  rm -rf "${nat_tmp}"
fi

# The packaging script must sign the embedded natives BEFORE invoking jpackage,
# otherwise the signed libraries never reach the .app.
PKG_SCRIPT="${HERE}/../package-macos.sh"
if [ -f "${PKG_SCRIPT}" ]; then
  call_line="$(grep -n 'sign_jar_native_binaries' "${PKG_SCRIPT}" | head -1 | cut -d: -f1)"
  jp_line="$(grep -n '^"\${JPACKAGE}"' "${PKG_SCRIPT}" | head -1 | cut -d: -f1)"
  if [ -n "${call_line}" ] && [ -n "${jp_line}" ] && [ "${call_line}" -lt "${jp_line}" ]; then
    pass "package-macos.sh signs embedded natives before running jpackage"
  else
    failc "package-macos.sh signs embedded natives before running jpackage"
  fi
else
  failc "package-macos.sh is present"
fi

if [ "${fails}" -gt 0 ]; then
  printf '%d macOS signing-plan test(s) failed.\n' "${fails}"
  exit 1
fi
printf 'All macOS signing-plan tests passed.\n'
