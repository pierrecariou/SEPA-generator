#!/usr/bin/env bash
#
# Focused tests for the Community release helper library (release-lib.sh).
# Pure string/filesystem logic only — no GitHub runner and no network needed.
# Run: bash packaging/community/tests/test-release-lib.sh
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../release/release-lib.sh
source "${HERE}/../release/release-lib.sh"

SLUG="SEPA-Generator-Community"
VER="9.8.7"

fails=0
pass()  { printf 'PASS: %s\n' "$1"; }
failc() { printf 'FAIL: %s\n' "$1"; fails=$((fails + 1)); }

# Build a temp dir populated with the 4 (non-empty) expected artifacts.
make_complete_dir() {
  local d; d="$(mktemp -d)"
  while IFS= read -r f; do printf 'dummy-%s' "$f" > "${d}/${f}"; done < <(expected_artifacts "${SLUG}" "${VER}")
  printf '%s' "${d}"
}

# --- normalize_tag_version --------------------------------------------------
[ "$(normalize_tag_version v9.8.7)" = "9.8.7" ] && pass "normalize strips leading v" || failc "normalize strips leading v"
[ "$(normalize_tag_version 9.8.7)"  = "9.8.7" ] && pass "normalize leaves bare version" || failc "normalize leaves bare version"
[ "$(normalize_tag_version v0.9.99)" = "0.9.99" ] && pass "normalize strips v on multi-patch" || failc "normalize strips v on multi-patch"
# A double-v prefix is NOT the expected tag format; it must NOT strip both v's.
[ "$(normalize_tag_version vv9.8.7)" != "9.8.7" ] && pass "normalize does not strip double-v" || failc "normalize does not strip double-v"

# --- assert_version_match ---------------------------------------------------
assert_version_match "9.8.7" "9.8.7" 2>/dev/null && pass "version match accepted" || failc "version match accepted"
if assert_version_match "9.8.8" "9.8.7" 2>/dev/null; then failc "version mismatch should fail"; else pass "version mismatch rejected"; fi
if assert_version_match "" "9.8.7" 2>/dev/null; then failc "empty tag should fail"; else pass "empty tag rejected"; fi
if assert_version_match "9.8.7" "" 2>/dev/null; then failc "empty maven version should fail"; else pass "empty maven version rejected"; fi

# Simulates the exact workflow step: push tag v9.8.7, maven returns 9.8.7.
NORMALIZED="$(normalize_tag_version v9.8.7)"
assert_version_match "${NORMALIZED}" "9.8.7" 2>/dev/null \
  && pass "tag v9.8.7 -> normalize -> assert_version_match roundtrip" \
  || failc "tag v9.8.7 -> normalize -> assert_version_match roundtrip"

# Mismatch: tag v9.9.9 pushed but maven still at 9.8.7 should abort.
NORM_WRONG="$(normalize_tag_version v9.9.9)"
if assert_version_match "${NORM_WRONG}" "9.8.7" 2>/dev/null; then
  failc "tag v9.9.9 vs maven 9.8.7 should fail"
else
  pass "tag v9.9.9 vs maven 9.8.7 correctly rejected"
fi

# Error message for a mismatch must not be empty and must name both versions.
msg="$(assert_version_match "9.9.9" "9.8.7" 2>&1 || true)"
{ printf '%s' "${msg}" | grep -q "9.9.9" && printf '%s' "${msg}" | grep -q "9.8.7"; } \
  && pass "mismatch message names both versions" || failc "mismatch message names both versions"

# --- assert_semver_format ---------------------------------------------------
assert_semver_format "9.8.7"  2>/dev/null && pass "semver 9.8.7 accepted"  || failc "semver 9.8.7 accepted"
assert_semver_format "0.9.99" 2>/dev/null && pass "semver 0.9.99 accepted" || failc "semver 0.9.99 accepted"
assert_semver_format "10.20.300" 2>/dev/null && pass "semver large numbers accepted" || failc "semver large numbers accepted"
if assert_semver_format "v9.8.7"  2>/dev/null; then failc "semver with leading v rejected"; else pass "semver with leading v rejected"; fi
if assert_semver_format "vv9.8.7" 2>/dev/null; then failc "double-v prefix rejected"; else pass "double-v prefix rejected"; fi
if assert_semver_format "9.8"     2>/dev/null; then failc "two-part version rejected"; else pass "two-part version rejected"; fi
if assert_semver_format "9.8.7a"  2>/dev/null; then failc "trailing suffix rejected"; else pass "trailing suffix rejected"; fi
if assert_semver_format "9.8.7-beta" 2>/dev/null; then failc "pre-release suffix rejected"; else pass "pre-release suffix rejected"; fi
if assert_semver_format ""           2>/dev/null; then failc "empty version rejected"; else pass "empty version rejected"; fi

# Simulates the full preflight sequence for vv9.8.7 (a malformed tag):
#   normalize -> "v9.8.7" (only one v stripped) -> assert_semver_format fails clearly
NORM_MALFORMED="$(normalize_tag_version vv9.8.7)"
if assert_semver_format "${NORM_MALFORMED}" 2>/dev/null; then
  failc "vv9.8.7 -> normalize -> semver check should fail"
else
  pass "vv9.8.7 -> normalize -> semver check fails clearly"
fi

# --- expected_artifacts -----------------------------------------------------
count="$(expected_artifacts "${SLUG}" "${VER}" | wc -l | tr -d '[:space:]')"
[ "${count}" = "4" ] && pass "expected_artifacts lists 4 files" || failc "expected_artifacts lists 4 files (got ${count})"
if expected_artifacts "${SLUG}" "${VER}" | grep -q "${SLUG}-${VER}-windows-x64.msi"; then pass "artifact name format (msi)"; else failc "artifact name format (msi)"; fi

# --- verify_artifact_set ----------------------------------------------------
d1="$(make_complete_dir)"
if verify_artifact_set "${d1}" "${SLUG}" "${VER}" 2>/dev/null; then pass "complete artifact set accepted"; else failc "complete artifact set accepted"; fi

rm -f "${d1}/${SLUG}-${VER}-linux-x64.deb"
if verify_artifact_set "${d1}" "${SLUG}" "${VER}" 2>/dev/null; then failc "missing artifact should fail"; else pass "missing artifact rejected"; fi
rm -rf "${d1}"

# Empty artifact rejected.
d2="$(make_complete_dir)"
: > "${d2}/${SLUG}-${VER}-macos-arm64.dmg"
if verify_artifact_set "${d2}" "${SLUG}" "${VER}" 2>/dev/null; then failc "empty artifact should fail"; else pass "empty artifact rejected"; fi
rm -rf "${d2}"

# Cross-edition (Pro) contamination rejected.
d3="$(make_complete_dir)"
printf 'x' > "${d3}/SEPA-Generator-Pro-1.0.0-windows-x64.msi"
if verify_artifact_set "${d3}" "${SLUG}" "${VER}" 2>/dev/null; then failc "Pro contamination should fail"; else pass "Pro contamination rejected"; fi
rm -rf "${d3}"

# --- generate_checksums / verify_checksums ----------------------------------
d4="$(make_complete_dir)"
if sums="$(generate_checksums "${d4}" "${SLUG}" "${VER}" 2>/dev/null)" && [ -f "${sums}" ]; then pass "checksums generated"; else failc "checksums generated"; fi
lines="$(wc -l < "${d4}/SHA256SUMS.txt" | tr -d '[:space:]')"
[ "${lines}" = "4" ] && pass "SHA256SUMS covers 4 files" || failc "SHA256SUMS covers 4 files (got ${lines})"
if verify_checksums "${d4}" 2>/dev/null; then pass "checksums verify (unmodified)"; else failc "checksums verify (unmodified)"; fi

# Tamper with one file -> verification must fail.
printf 'tampered' >> "${d4}/${SLUG}-${VER}-windows-x64.msi"
if verify_checksums "${d4}" 2>/dev/null; then failc "tampered file should fail checksum"; else pass "tampered file fails checksum"; fi
rm -rf "${d4}"

# Missing SHA256SUMS.txt -> verification fails.
d5="$(mktemp -d)"
if verify_checksums "${d5}" 2>/dev/null; then failc "missing SHA256SUMS should fail"; else pass "missing SHA256SUMS rejected"; fi
rm -rf "${d5}"

# --- assert_signing_ready ---------------------------------------------------
unset WINDOWS_CERT_PFX_BASE64 WINDOWS_CERT_PASSWORD \
      MAC_SIGNING_IDENTITY MACOS_CERT_P12_BASE64 MACOS_CERT_PASSWORD \
      APPLE_ID APPLE_TEAM_ID APPLE_APP_PASSWORD

# Not required -> no-op success even with no inputs (unsigned RC allowed).
if assert_signing_ready windows false 2>/dev/null; then pass "signing not required is a no-op"; else failc "signing not required is a no-op"; fi

# Required but missing -> fail.
if assert_signing_ready windows true 2>/dev/null; then failc "windows signing required+missing should fail"; else pass "windows signing required+missing rejected"; fi
if assert_signing_ready macos true 2>/dev/null; then failc "macos signing required+missing should fail"; else pass "macos signing required+missing rejected"; fi

# Required and complete -> pass.
WINDOWS_CERT_PFX_BASE64="QUJD" WINDOWS_CERT_PASSWORD="pw"
if assert_signing_ready windows true 2>/dev/null; then pass "windows signing required+complete accepted"; else failc "windows signing required+complete accepted"; fi

# Required+complete must not leak the password value in any output.
out="$(assert_signing_ready windows true 2>&1 || true)"
if printf '%s' "${out}" | grep -q "pw"; then failc "signing check must not echo secrets"; else pass "signing check does not echo secrets"; fi

if [ "${fails}" -gt 0 ]; then
  printf '%d release-lib test(s) failed.\n' "${fails}"
  exit 1
fi
printf 'All release-lib tests passed.\n'
