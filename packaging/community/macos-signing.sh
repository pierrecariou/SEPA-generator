#!/usr/bin/env bash
#
# Optional macOS Developer ID signing + notarization helpers for the Community
# packaging script (package-macos.sh).
#
# This logic lives in a sibling file so the pure signing-DECISION and PARSING
# logic (resolve_macos_signing_plan, team_id_from_identity, json_scalar) can be
# unit tested without executing the whole packaging script and without any Apple
# tooling present.
#
# SECURITY:
#   * No credentials are stored in this file.
#   * All sensitive inputs (certificate, certificate password, App Store Connect
#     API key) arrive at runtime via environment variables / CI secrets only.
#   * Secrets are never printed; command previews avoid echoing credentials.
#   * Shell tracing is never enabled around credential handling.
#   * When a certificate is imported, it goes into a TEMPORARY keychain that is
#     always removed (trap); the decoded .p12 and the decoded App Store Connect
#     .p8 temp files are deleted by the same trap, including after a failure.
#
# This file implements signing/notarization HOOKS plus the verification that
# proves an artifact really is signed, notarized and stapled. Sourcing it does
# not sign or notarize anything. Every check fails closed, so a caller can never
# mislabel an unverified artifact.

# -----------------------------------------------------------------------------
# Pure helper: extract the Apple Team ID from a Developer ID identity string.
# "Developer ID Application: NIRYOSYS (233LH9G5PD)" -> "233LH9G5PD"
# Prints nothing and returns non-zero when the identity carries no team suffix,
# so callers fail closed instead of skipping the team check.
# -----------------------------------------------------------------------------
team_id_from_identity() {
  local identity="${1:-}" team
  team="$(printf '%s' "${identity}" | sed -n 's/.*(\([A-Z0-9]\{6,\}\))[[:space:]]*$/\1/p')"
  [ -n "${team}" ] || return 1
  printf '%s' "${team}"
}

# -----------------------------------------------------------------------------
# Pure helper: read a top-level string scalar out of the flat JSON emitted by
# `notarytool ... --output-format json`. Avoids adding a jq dependency for the
# two fields that are needed ("id" and "status"). Returns non-zero when the key
# is absent, so a missing field is never silently treated as success.
# -----------------------------------------------------------------------------
json_scalar() {
  local json="${1:-}" key="${2:-}" value
  value="$(printf '%s' "${json}" \
    | tr ',{}' '\n\n\n' \
    | sed -n "s/^[[:space:]]*\"${key}\"[[:space:]]*:[[:space:]]*\"\\(.*\\)\"[[:space:]]*$/\\1/p" \
    | head -n1)"
  [ -n "${value}" ] || return 1
  printf '%s' "${value}"
}

# -----------------------------------------------------------------------------
# Pure decision function. Reads the signing/notarization environment variables
# and sets:
#     MAC_DO_SIGN       true|false
#     MAC_DO_NOTARIZE   true|false
# Returns non-zero (with a message on stderr) if an explicit request is
# incomplete. Performs NO tool calls, so it is deterministic and unit-testable.
# Fail-closed: notarization is refused for an unsigned app, and any explicit
# request missing required inputs fails rather than silently downgrading.
#
# Notarization authenticates with an App Store Connect API key (Team Key):
#   APPLE_API_KEY_P8_BASE64  base64 of the downloaded AuthKey_<KEYID>.p8
#   APPLE_API_KEY_ID         the key identifier
#   APPLE_API_ISSUER_ID      the issuer UUID of the App Store Connect account
# An API key is scoped to App Store Connect, is revocable on its own, needs no
# interactive 2FA, and is the credential Apple recommends for automated builds.
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
    # The identity must carry the Apple Team ID so the produced artifact can be
    # verified against it after signing; a bare name cannot be verified.
    if ! team_id_from_identity "${MAC_SIGNING_IDENTITY}" >/dev/null; then
      echo "ERROR: MAC_SIGNING_IDENTITY must end with the Apple Team ID in parentheses, e.g. 'Developer ID Application: Name (TEAMID)'." >&2
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
    [ -n "${APPLE_API_KEY_P8_BASE64:-}" ] || missing="${missing} APPLE_API_KEY_P8_BASE64"
    [ -n "${APPLE_API_KEY_ID:-}" ]        || missing="${missing} APPLE_API_KEY_ID"
    [ -n "${APPLE_API_ISSUER_ID:-}" ]     || missing="${missing} APPLE_API_ISSUER_ID"
    if [ -n "${missing}" ]; then
      echo "ERROR: MAC_NOTARIZE=true but required App Store Connect API key input(s) missing:${missing}." >&2
      return 1
    fi
    MAC_DO_NOTARIZE=true
  fi

  return 0
}

# Remove the temporary signing keychain, the decoded certificate, the decoded
# App Store Connect API key and any DMG still mounted for verification. Safe to
# call repeatedly; installed as an EXIT trap by register_macos_cleanup so
# credentials are removed even after a failure.
cleanup_macos_keychain() {
  if [ -n "${MAC_TEMP_KEYCHAIN:-}" ] && [ -f "${MAC_TEMP_KEYCHAIN}" ]; then
    security delete-keychain "${MAC_TEMP_KEYCHAIN}" >/dev/null 2>&1 || true
    rm -f "${MAC_TEMP_KEYCHAIN}" 2>/dev/null || true
  fi
  if [ -n "${MAC_TEMP_CERT_FILE:-}" ] && [ -f "${MAC_TEMP_CERT_FILE}" ]; then
    rm -f "${MAC_TEMP_CERT_FILE}" 2>/dev/null || true
  fi
  if [ -n "${MAC_TEMP_API_KEY_FILE:-}" ] && [ -f "${MAC_TEMP_API_KEY_FILE}" ]; then
    rm -f "${MAC_TEMP_API_KEY_FILE}" 2>/dev/null || true
  fi
  if [ -n "${MAC_DMG_MOUNTPOINT:-}" ] && [ -d "${MAC_DMG_MOUNTPOINT}" ]; then
    hdiutil detach "${MAC_DMG_MOUNTPOINT}" -quiet >/dev/null 2>&1 || true
    rmdir "${MAC_DMG_MOUNTPOINT}" 2>/dev/null || true
  fi
}

# Install the credential cleanup trap exactly once, so every helper that decodes
# a credential is covered regardless of the order they are called in.
register_macos_cleanup() {
  [ -n "${MAC_CLEANUP_REGISTERED:-}" ] && return 0
  MAC_CLEANUP_REGISTERED=true
  trap cleanup_macos_keychain EXIT
}

# Import the Developer ID certificate from MACOS_CERT_P12_BASE64 into a fresh
# temporary keychain, so signing does not depend on (or pollute) the login
# keychain. No-op when MACOS_CERT_P12_BASE64 is not provided (the identity is
# then expected to already exist in an available keychain, e.g. a developer's
# Mac). Never prints the password.
setup_macos_keychain() {
  register_macos_cleanup

  [ -n "${MACOS_CERT_P12_BASE64:-}" ] || { ok "Using signing identity from the existing keychain (no MACOS_CERT_P12_BASE64 supplied)."; return 0; }

  command -v security >/dev/null 2>&1 || fail "'security' tool not found; cannot import the signing certificate."

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
      -P "${MACOS_CERT_PASSWORD}" -T /usr/bin/codesign                      || fail "Failed to import the signing certificate (wrong password, or an unsupported PKCS#12 encryption; re-export with '-legacy' if needed)."
  # Allow codesign to use the imported key without an interactive prompt. A
  # failure here would otherwise surface much later as a codesign hang, so it
  # is fatal rather than best-effort.
  security set-key-partition-list -S apple-tool:,apple: -s -k "${kc_pass}" "${MAC_TEMP_KEYCHAIN}" >/dev/null 2>&1 \
    || fail "Failed to set the keychain partition list; codesign would block on an interactive prompt."

  # Prepend the temp keychain to the search list so codesign can find the identity.
  local existing
  existing="$(security list-keychains -d user | sed 's/[\"[:space:]]//g')"
  security list-keychains -d user -s "${MAC_TEMP_KEYCHAIN}" ${existing}     || true

  # Remove the decoded certificate immediately (the key now lives in the keychain).
  rm -f "${MAC_TEMP_CERT_FILE}" 2>/dev/null || true
  MAC_TEMP_CERT_FILE=""
  ok "Imported signing certificate into a temporary keychain (auto-removed on exit)."
}

# -----------------------------------------------------------------------------
# Prove that the requested Developer ID identity is actually usable for code
# signing BEFORE jpackage runs, and print the (non-sensitive) identity line.
# Fails closed when the identity is missing, unusable, or ambiguous.
# -----------------------------------------------------------------------------
assert_macos_signing_identity() {
  command -v security >/dev/null 2>&1 || fail "'security' tool not found; cannot verify the signing identity."

  case "${MAC_SIGNING_IDENTITY}" in
    "Developer ID Application:"*) ;;
    *) fail "MAC_SIGNING_IDENTITY must be a 'Developer ID Application: ...' identity for direct distribution outside the Mac App Store." ;;
  esac

  local listing matches count
  if [ -n "${MAC_TEMP_KEYCHAIN:-}" ]; then
    listing="$(security find-identity -v -p codesigning "${MAC_TEMP_KEYCHAIN}" 2>/dev/null || true)"
  else
    listing="$(security find-identity -v -p codesigning 2>/dev/null || true)"
  fi

  matches="$(printf '%s\n' "${listing}" | grep -F "${MAC_SIGNING_IDENTITY}" || true)"
  count="$(printf '%s' "${matches}" | grep -c . || true)"

  if [ "${count}" -eq 0 ]; then
    printf 'ERROR: no valid code-signing identity matching "%s" is available.\n' "${MAC_SIGNING_IDENTITY}" >&2
    printf 'Valid code-signing identities found:\n%s\n' "${listing:-  <none>}" >&2
    exit 1
  fi
  if [ "${count}" -gt 1 ]; then
    printf 'ERROR: %s code-signing identities match "%s"; refusing to guess which one to use.\n' \
      "${count}" "${MAC_SIGNING_IDENTITY}" >&2
    exit 1
  fi

  # The identity name and its certificate hash are public certificate metadata.
  ok "Signing identity:$(printf '%s' "${matches}" | sed 's/^[[:space:]]*[0-9]*)//')"
  ok "Apple Team ID: $(team_id_from_identity "${MAC_SIGNING_IDENTITY}")"
}

# -----------------------------------------------------------------------------
# Post-jpackage verification. Mounts the produced DMG read-only, then proves the
# bundled application really carries a Developer ID signature from the expected
# team, with the hardened runtime enabled, and that the nested launcher and the
# bundled Java runtime are signed too. Also records whether the DMG itself is
# signed so the caller can sign it exactly once if jpackage did not.
#
# Sets MAC_DMG_IS_SIGNED=true|false. Returns non-zero on any verification
# failure, so an unverified artifact can never reach notarization or upload.
# -----------------------------------------------------------------------------
verify_signed_app() {
  local dmg="$1"
  local expected_team
  expected_team="$(team_id_from_identity "${MAC_SIGNING_IDENTITY}")" \
    || { echo "ERROR: cannot derive the expected Apple Team ID from MAC_SIGNING_IDENTITY." >&2; return 1; }

  command -v codesign >/dev/null 2>&1 || { echo "ERROR: 'codesign' not found; cannot verify the signature." >&2; return 1; }
  command -v hdiutil  >/dev/null 2>&1 || { echo "ERROR: 'hdiutil' not found; cannot inspect the DMG." >&2; return 1; }

  register_macos_cleanup

  step "Checking whether jpackage signed the DMG container"
  MAC_DMG_IS_SIGNED=false
  if codesign --verify --strict "${dmg}" >/dev/null 2>&1; then
    MAC_DMG_IS_SIGNED=true
    ok "The DMG produced by jpackage is already signed."
  else
    ok "The DMG produced by jpackage is NOT signed; it will be signed once, explicitly."
  fi

  step "Mounting the DMG read-only for signature verification"
  MAC_DMG_MOUNTPOINT="$(mktemp -d "${TMPDIR:-/tmp}/sepa-dmg-XXXXXX")"
  hdiutil attach "${dmg}" -nobrowse -readonly -noverify -mountpoint "${MAC_DMG_MOUNTPOINT}" >/dev/null \
    || { echo "ERROR: failed to mount ${dmg}." >&2; return 1; }

  local app
  app="$(find "${MAC_DMG_MOUNTPOINT}" -maxdepth 1 -name '*.app' | head -n1)"
  if [ -z "${app}" ]; then
    echo "ERROR: no .app bundle found inside ${dmg}." >&2
    return 1
  fi
  ok "Application bundle: $(basename "${app}")"

  # 1. Whole-tree signature verification. --deep is used here for VERIFICATION
  #    only (never for signing): it walks every nested bundle and Mach-O binary
  #    that jpackage signed and reports any that is missing or broken.
  step "Verifying the application signature (strict, whole tree)"
  codesign --verify --deep --strict --verbose=2 "${app}" \
    || { echo "ERROR: codesign verification of the application bundle failed." >&2; return 1; }

  # 2. Nested code that jpackage signs as separate units: the native launcher
  #    and the bundled Java runtime. Verified explicitly so a missing nested
  #    signature is named precisely instead of hiding inside the tree walk.
  step "Verifying nested launcher and bundled Java runtime signatures"
  local launcher
  launcher="$(find "${app}/Contents/MacOS" -type f -perm +111 2>/dev/null | head -n1)"
  [ -n "${launcher}" ] || { echo "ERROR: no launcher executable found in ${app}/Contents/MacOS." >&2; return 1; }
  codesign --verify --strict --verbose=2 "${launcher}" \
    || { echo "ERROR: the native launcher is not correctly signed." >&2; return 1; }
  ok "Launcher signed: $(basename "${launcher}")"

  if [ -d "${app}/Contents/runtime" ]; then
    codesign --verify --deep --strict --verbose=2 "${app}/Contents/runtime" \
      || { echo "ERROR: the bundled Java runtime is not correctly signed." >&2; return 1; }
    ok "Bundled Java runtime signed."
  else
    echo "ERROR: no bundled Java runtime found at ${app}/Contents/runtime." >&2
    return 1
  fi

  # 3. Authority chain, team identity and hardened runtime.
  step "Verifying the Developer ID authority, Team ID and hardened runtime"
  local details
  details="$(codesign --display --verbose=4 "${app}" 2>&1)" \
    || { echo "ERROR: could not read the signature details of the application." >&2; return 1; }

  printf '%s\n' "${details}" | grep -E '^(Identifier|Authority|TeamIdentifier|CodeDirectory)' || true

  printf '%s' "${details}" | grep -q "Authority=${MAC_SIGNING_IDENTITY}" || {
    echo "ERROR: the application was not signed by the expected identity '${MAC_SIGNING_IDENTITY}'." >&2
    return 1; }
  printf '%s' "${details}" | grep -q 'Authority=Developer ID Certification Authority' || {
    echo "ERROR: the signature does not chain to Apple's Developer ID Certification Authority." >&2
    return 1; }
  printf '%s' "${details}" | grep -q 'Authority=Apple Root CA' || {
    echo "ERROR: the signature does not chain to the Apple Root CA." >&2
    return 1; }
  printf '%s' "${details}" | grep -q "TeamIdentifier=${expected_team}" || {
    echo "ERROR: the signature does not carry the expected Apple Team ID '${expected_team}'." >&2
    return 1; }
  printf '%s' "${details}" | grep -qE 'flags=0x[0-9a-f]*\(.*runtime.*\)' || {
    echo "ERROR: the application is not signed with the hardened runtime enabled." >&2
    return 1; }
  ok "Developer ID authority chain, Team ID ${expected_team} and hardened runtime confirmed."

  step "Unmounting the DMG"
  hdiutil detach "${MAC_DMG_MOUNTPOINT}" -quiet || true
  rmdir "${MAC_DMG_MOUNTPOINT}" 2>/dev/null || true
  MAC_DMG_MOUNTPOINT=""
  return 0
}

# Sign the DMG container itself when jpackage did not already do so. Called only
# after verify_signed_app has set MAC_DMG_IS_SIGNED, so the DMG is never signed
# twice. Notarization requires a timestamped signature.
sign_dmg_if_needed() {
  local dmg="$1"
  if [ "${MAC_DMG_IS_SIGNED:-false}" = "true" ]; then
    ok "DMG already signed by jpackage; not signing it again."
    return 0
  fi
  step "Signing the DMG container"
  codesign --sign "${MAC_SIGNING_IDENTITY}" --timestamp "${dmg}" \
    || { echo "ERROR: failed to sign the DMG." >&2; return 1; }
  codesign --verify --strict --verbose=2 "${dmg}" \
    || { echo "ERROR: DMG signature verification failed after signing." >&2; return 1; }
  ok "DMG signed and verified."
  return 0
}

# Decode the App Store Connect API key into a temporary file readable only by
# the current user. The file is removed by the EXIT trap, including on failure.
# The key material itself is never printed.
_prepare_notary_credentials() {
  register_macos_cleanup
  MAC_TEMP_API_KEY_FILE="${TMPDIR:-/tmp}/sepa-notary-$$.p8"
  ( umask 077 && printf '%s' "${APPLE_API_KEY_P8_BASE64}" | base64 --decode > "${MAC_TEMP_API_KEY_FILE}" ) \
    || { echo "ERROR: failed to decode APPLE_API_KEY_P8_BASE64." >&2; return 1; }
  [ -s "${MAC_TEMP_API_KEY_FILE}" ] || { echo "ERROR: the decoded App Store Connect API key is empty." >&2; return 1; }
  return 0
}

# Notarize and staple a DMG, then validate and run the Gatekeeper assessment.
# Fails (non-zero) if submission, stapling, validation or assessment fails, so
# callers never mislabel a DMG as notarized. When Apple rejects the submission
# the full non-secret notarization log is retrieved and printed, so the cause is
# visible in the CI log without a rerun.
notarize_and_staple() {
  local dmg="$1"

  command -v xcrun >/dev/null 2>&1 || { echo "ERROR: 'xcrun' not found; cannot notarize." >&2; return 1; }
  _prepare_notary_credentials || return 1

  # The API key file path, key id and issuer id are arguments to notarytool
  # only; the key material is never echoed.
  local auth=(
    --key "${MAC_TEMP_API_KEY_FILE}"
    --key-id "${APPLE_API_KEY_ID}"
    --issuer "${APPLE_API_ISSUER_ID}"
  )

  step "Submitting DMG to Apple notarization (this can take several minutes)"
  local submit_json submit_rc=0
  submit_json="$(xcrun notarytool submit "${dmg}" "${auth[@]}" --wait --output-format json 2>&1)" || submit_rc=$?

  local submission_id status
  submission_id="$(json_scalar "${submit_json}" id || true)"
  status="$(json_scalar "${submit_json}" status || true)"

  printf '    Notarization submission id: %s\n' "${submission_id:-<unknown>}"
  printf '    Notarization status       : %s\n' "${status:-<unknown>}"

  if [ "${submit_rc}" -ne 0 ] || [ "${status}" != "Accepted" ]; then
    echo "ERROR: notarization did not succeed (status='${status:-unknown}')." >&2
    printf 'notarytool submit output:\n%s\n' "${submit_json}" >&2
    if [ -n "${submission_id}" ]; then
      echo "== Apple notarization log for submission ${submission_id} ==" >&2
      xcrun notarytool log "${submission_id}" "${auth[@]}" >&2 \
        || echo "(the notarization log could not be retrieved)" >&2
    else
      echo "(no submission id was returned, so no notarization log can be retrieved)" >&2
    fi
    return 1
  fi
  ok "Apple accepted the submission."

  step "Stapling notarization ticket"
  xcrun stapler staple "${dmg}"    || { echo "ERROR: stapler staple failed." >&2; return 1; }
  xcrun stapler validate "${dmg}"  || { echo "ERROR: stapler validate failed." >&2; return 1; }
  ok "Notarization ticket stapled and validated."

  # Gatekeeper assessment of the stapled DMG. This is the authoritative check
  # that an end user double-clicking the download will not be blocked. It is
  # mandatory: a missing spctl is an error, not a reason to skip the check.
  step "Running the Gatekeeper assessment"
  command -v spctl >/dev/null 2>&1 || { echo "ERROR: 'spctl' not found; cannot run the Gatekeeper assessment." >&2; return 1; }
  spctl -a -t open --context context:primary-signature -v "${dmg}" || {
    echo "ERROR: spctl Gatekeeper assessment failed for ${dmg}." >&2; return 1; }
  ok "Gatekeeper accepted the DMG."
  return 0
}
