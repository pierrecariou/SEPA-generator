#!/usr/bin/env bash
#
# Optional macOS Developer ID signing + notarization helpers for the Community
# packaging script (package-macos.sh).
#
# This logic lives in a sibling file so the pure signing-DECISION logic
# (resolve_macos_signing_plan) can be unit tested without executing the whole
# packaging script and without any Apple tooling present.
#
# SECURITY:
#   * No credentials are stored in this file.
#   * All sensitive inputs (signing identity, certificate, Apple app password)
#     arrive at runtime via environment variables / CI secrets only.
#   * Secrets are never printed; command previews avoid echoing credentials.
#   * When a certificate is imported, it goes into a TEMPORARY keychain that is
#     always removed (trap), and the decoded .p12 temp file is deleted.
#
# This file implements signing/notarization HOOKS. Sourcing it does not sign or
# notarize anything and does not prove any artifact is signed or notarized.

# -----------------------------------------------------------------------------
# Pure decision function. Reads the signing/notarization environment variables
# and sets:
#     MAC_DO_SIGN       true|false
#     MAC_DO_NOTARIZE   true|false
# Returns non-zero (with a message on stderr) if an explicit request is
# incomplete. Performs NO tool calls, so it is deterministic and unit-testable.
# Fail-closed: notarization is refused for an unsigned app, and any explicit
# request missing required inputs fails rather than silently downgrading.
# -----------------------------------------------------------------------------
resolve_macos_signing_plan() {
  MAC_DO_SIGN=false
  MAC_DO_NOTARIZE=false

  local sign="${MAC_SIGN:-false}"
  local notarize="${MAC_NOTARIZE:-false}"

  if [ "${sign}" = "true" ]; then
    if [ -z "${MAC_SIGNING_IDENTITY:-}" ]; then
      echo "ERROR: MAC_SIGN=true but MAC_SIGNING_IDENTITY is empty (e.g. 'Developer ID Application: Name (TEAMID)')." >&2
      return 1
    fi
    # If a certificate blob is supplied it must have a password (fail-closed).
    if [ -n "${MACOS_CERT_P12_BASE64:-}" ] && [ -z "${MACOS_CERT_PASSWORD:-}" ]; then
      echo "ERROR: MACOS_CERT_P12_BASE64 is set but MACOS_CERT_PASSWORD is empty." >&2
      return 1
    fi
    MAC_DO_SIGN=true
  fi

  if [ "${notarize}" = "true" ]; then
    if [ "${MAC_DO_SIGN}" != "true" ]; then
      echo "ERROR: MAC_NOTARIZE=true requires MAC_SIGN=true; notarization cannot run on an unsigned app." >&2
      return 1
    fi
    local missing=""
    [ -n "${APPLE_ID:-}" ]           || missing="${missing} APPLE_ID"
    [ -n "${APPLE_TEAM_ID:-}" ]      || missing="${missing} APPLE_TEAM_ID"
    [ -n "${APPLE_APP_PASSWORD:-}" ] || missing="${missing} APPLE_APP_PASSWORD"
    if [ -n "${missing}" ]; then
      echo "ERROR: MAC_NOTARIZE=true but required secret(s) missing:${missing}." >&2
      return 1
    fi
    MAC_DO_NOTARIZE=true
  fi

  return 0
}

# Remove the temporary signing keychain and any decoded certificate file. Safe
# to call repeatedly; installed as an EXIT trap by setup_macos_keychain.
cleanup_macos_keychain() {
  if [ -n "${MAC_TEMP_KEYCHAIN:-}" ] && [ -f "${MAC_TEMP_KEYCHAIN}" ]; then
    security delete-keychain "${MAC_TEMP_KEYCHAIN}" >/dev/null 2>&1 || true
    rm -f "${MAC_TEMP_KEYCHAIN}" 2>/dev/null || true
  fi
  if [ -n "${MAC_TEMP_CERT_FILE:-}" ] && [ -f "${MAC_TEMP_CERT_FILE}" ]; then
    rm -f "${MAC_TEMP_CERT_FILE}" 2>/dev/null || true
  fi
}

# Import the Developer ID certificate from MACOS_CERT_P12_BASE64 into a fresh
# temporary keychain, so signing does not depend on (or pollute) the login
# keychain. No-op when MACOS_CERT_P12_BASE64 is not provided (the identity is
# then expected to already exist in an available keychain, e.g. a developer's
# Mac). Never prints the password.
setup_macos_keychain() {
  [ -n "${MACOS_CERT_P12_BASE64:-}" ] || { ok "Using signing identity from the existing keychain (no MACOS_CERT_P12_BASE64 supplied)."; return 0; }

  command -v security >/dev/null 2>&1 || fail "'security' tool not found; cannot import the signing certificate."

  trap cleanup_macos_keychain EXIT

  local kc_pass
  kc_pass="$(openssl rand -base64 24 2>/dev/null || echo "tmp-$(date +%s)-$$")"
  MAC_TEMP_KEYCHAIN="${TMPDIR:-/tmp}/sepa-signing-$$.keychain-db"
  MAC_TEMP_CERT_FILE="${TMPDIR:-/tmp}/sepa-signing-$$.p12"

  printf '%s' "${MACOS_CERT_P12_BASE64}" | base64 --decode > "${MAC_TEMP_CERT_FILE}" \
    || fail "Failed to decode MACOS_CERT_P12_BASE64."

  security create-keychain -p "${kc_pass}" "${MAC_TEMP_KEYCHAIN}"           || fail "Failed to create temporary keychain."
  security set-keychain-settings -lut 21600 "${MAC_TEMP_KEYCHAIN}"          || true
  security unlock-keychain -p "${kc_pass}" "${MAC_TEMP_KEYCHAIN}"           || fail "Failed to unlock temporary keychain."
  security import "${MAC_TEMP_CERT_FILE}" -k "${MAC_TEMP_KEYCHAIN}" \
      -P "${MACOS_CERT_PASSWORD}" -T /usr/bin/codesign                      || fail "Failed to import the signing certificate."
  security set-key-partition-list -S apple-tool:,apple: -s -k "${kc_pass}" "${MAC_TEMP_KEYCHAIN}" >/dev/null 2>&1 || true

  # Prepend the temp keychain to the search list so codesign can find the identity.
  local existing
  existing="$(security list-keychains -d user | sed 's/[\"[:space:]]//g')"
  security list-keychains -d user -s "${MAC_TEMP_KEYCHAIN}" ${existing}     || true

  # Remove the decoded certificate immediately (the key now lives in the keychain).
  rm -f "${MAC_TEMP_CERT_FILE}" 2>/dev/null || true
  MAC_TEMP_CERT_FILE=""
  ok "Imported signing certificate into a temporary keychain (auto-removed on exit)."
}

# Notarize and staple a DMG, then validate. Fails (non-zero) if submission,
# stapling, or validation fails, so callers never mislabel a DMG as notarized.
# The Apple app password is passed as an argument to notarytool only; it is not
# echoed by this function.
notarize_and_staple() {
  local dmg="$1"

  command -v xcrun >/dev/null 2>&1 || { echo "ERROR: 'xcrun' not found; cannot notarize." >&2; return 1; }

  step "Submitting DMG to Apple notarization (this can take several minutes)"
  if ! xcrun notarytool submit "${dmg}" \
        --apple-id "${APPLE_ID}" \
        --team-id "${APPLE_TEAM_ID}" \
        --password "${APPLE_APP_PASSWORD}" \
        --wait; then
    echo "ERROR: notarytool submission failed or was rejected." >&2
    return 1
  fi

  step "Stapling notarization ticket"
  xcrun stapler staple "${dmg}"    || { echo "ERROR: stapler staple failed." >&2; return 1; }
  xcrun stapler validate "${dmg}"  || { echo "ERROR: stapler validate failed." >&2; return 1; }

  # Gatekeeper assessment of the stapled DMG (best-effort final check).
  if command -v spctl >/dev/null 2>&1; then
    spctl -a -t open --context context:primary-signature -v "${dmg}" || {
      echo "ERROR: spctl Gatekeeper assessment failed for ${dmg}." >&2; return 1; }
  fi
  return 0
}
