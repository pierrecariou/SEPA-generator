#!/usr/bin/env bash
#
# Reusable, UI-independent helpers for the Community RELEASE pipeline
# (.github/workflows/package-community.yml, release job).
#
# This logic lives in a sourced library so the pure decision/verification
# functions can be unit tested (packaging/community/tests/test-release-lib.sh)
# without running the whole workflow and without GitHub's runners.
#
# Every function is deterministic and does only local filesystem / string work.
# None of them publish anything or talk to GitHub. Fail-closed: verification
# helpers return non-zero (with a message on stderr) rather than silently
# continuing with an incomplete or mismatched artifact set.
#
# SECURITY: no credentials are read or printed here. Signing readiness checks
# only test for the PRESENCE of environment variables; their values are never
# echoed.

# -----------------------------------------------------------------------------
# Strip a single optional leading 'v' from a git tag to obtain the version, e.g.
#   v1.4.0 -> 1.4.0     1.4.0 -> 1.4.0
# -----------------------------------------------------------------------------
normalize_tag_version() {
  printf '%s' "${1#v}"
}

# -----------------------------------------------------------------------------
# Assert <version> is in the exact supported form X.Y.Z (three non-negative
# dot-separated integers, no pre-release/build suffix). Called on the normalized
# (v-stripped) tag before assert_version_match so malformed tags like
#   vv1.4.0  ->  v1.4.0  (still fails: v is not a digit)
#   v1.4     ->  1.4     (fails: only two parts)
#   v1.4.0a  ->  1.4.0a  (fails: trailing non-digit)
# produce a clear diagnostic instead of a confusing mismatch error.
# -----------------------------------------------------------------------------
assert_semver_format() {
  local v="$1"
  if ! printf '%s' "${v}" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
    echo "ERROR: tag version '${v}' is not a valid X.Y.Z semantic version. Check the tag format (expected e.g. v1.4.0 → 1.4.0)." >&2
    return 1
  fi
  return 0
}

# Fails (non-zero) on any empty value or mismatch. Never mutates state.
# -----------------------------------------------------------------------------
assert_version_match() {
  local tag_version="$1" maven_version="$2"
  if [ -z "${tag_version}" ] || [ -z "${maven_version}" ]; then
    echo "ERROR: version check received an empty value (tag='${tag_version}', maven='${maven_version}')." >&2
    return 1
  fi
  if [ "${tag_version}" != "${maven_version}" ]; then
    echo "ERROR: release version mismatch — tag/input is '${tag_version}' but Maven \${revision} resolves to '${maven_version}'. Refusing to package." >&2
    return 1
  fi
  return 0
}

# -----------------------------------------------------------------------------
# Echo (one per line) the exact set of distributable artifact filenames for a
# given edition slug + version. This is the single source of truth for release
# artifact naming and completeness checks.
# -----------------------------------------------------------------------------
expected_artifacts() {
  local slug="$1" version="$2"
  printf '%s\n' \
    "${slug}-${version}-windows-x64.msi" \
    "${slug}-${version}-macos-arm64.dmg" \
    "${slug}-${version}-macos-x64.dmg" \
    "${slug}-${version}-linux-x64.deb"
}

# -----------------------------------------------------------------------------
# Verify that every expected artifact is present in <dir>, that none is empty,
# and that no foreign-edition artifact leaked in (e.g. a 'Pro' installer). Fails
# listing every problem. Does not modify anything.
# -----------------------------------------------------------------------------
verify_artifact_set() {
  local dir="$1" slug="$2" version="$3"
  local problems=0 f

  if [ ! -d "${dir}" ]; then
    echo "ERROR: artifact directory not found: ${dir}" >&2
    return 1
  fi

  while IFS= read -r f; do
    if [ ! -f "${dir}/${f}" ]; then
      echo "ERROR: missing expected artifact: ${f}" >&2
      problems=1
    elif [ ! -s "${dir}/${f}" ]; then
      echo "ERROR: expected artifact is empty: ${f}" >&2
      problems=1
    fi
  done < <(expected_artifacts "${slug}" "${version}")

  # Guard against cross-edition contamination: this is the Community release, so
  # a Pro-named installer must never appear in the collected set.
  local stray
  stray="$(find "${dir}" -maxdepth 1 -type f \
             \( -iname '*.msi' -o -iname '*.dmg' -o -iname '*.deb' \) \
             -iname '*Pro*' 2>/dev/null || true)"
  if [ -n "${stray}" ]; then
    echo "ERROR: foreign (Pro) artifact present in Community release set:" >&2
    printf '  %s\n' ${stray} >&2
    problems=1
  fi

  return "${problems}"
}

# -----------------------------------------------------------------------------
# Generate SHA256SUMS.txt inside <dir> covering EXACTLY the expected artifacts
# (basenames only, so the file is portable). Runs after signing/notarization so
# checksums describe the final bytes. Requires the artifact set to be complete.
# Echoes the path of the generated file on success.
# -----------------------------------------------------------------------------
generate_checksums() {
  local dir="$1" slug="$2" version="$3"
  verify_artifact_set "${dir}" "${slug}" "${version}" || return 1

  local sums_file="${dir}/SHA256SUMS.txt"
  (
    cd "${dir}" || exit 1
    : > SHA256SUMS.txt
    while IFS= read -r f; do
      sha256sum "${f}" >> SHA256SUMS.txt || exit 1
    done < <(expected_artifacts "${slug}" "${version}")
  ) || { echo "ERROR: failed to generate ${sums_file}." >&2; return 1; }

  echo "${sums_file}"
  return 0
}

# -----------------------------------------------------------------------------
# Verify SHA256SUMS.txt in <dir> against the actual files. Fails if any file is
# missing or its digest does not match.
# -----------------------------------------------------------------------------
verify_checksums() {
  local dir="$1"
  if [ ! -f "${dir}/SHA256SUMS.txt" ]; then
    echo "ERROR: SHA256SUMS.txt not found in ${dir}." >&2
    return 1
  fi
  ( cd "${dir}" && sha256sum -c SHA256SUMS.txt ) \
    || { echo "ERROR: checksum verification failed in ${dir}." >&2; return 1; }
  return 0
}

# -----------------------------------------------------------------------------
# When release signing is declared MANDATORY, ensure the required inputs exist.
# Only tests for presence; never echoes secret values. A false/empty 'required'
# flag is a no-op (unsigned release candidates remain allowed).
#
#   assert_signing_ready windows true    # checks Windows Authenticode inputs
#   assert_signing_ready macos   true    # checks Developer ID + notarization
# -----------------------------------------------------------------------------
assert_signing_ready() {
  local platform="$1" required="${2:-false}"
  [ "${required}" = "true" ] || return 0

  local missing=""
  case "${platform}" in
    windows)
      [ -n "${WINDOWS_CERT_PFX_BASE64:-}" ] || missing="${missing} WINDOWS_CERT_PFX_BASE64"
      [ -n "${WINDOWS_CERT_PASSWORD:-}" ]   || missing="${missing} WINDOWS_CERT_PASSWORD"
      ;;
    macos)
      [ -n "${MAC_SIGNING_IDENTITY:-}" ]  || missing="${missing} MAC_SIGNING_IDENTITY"
      [ -n "${MACOS_CERT_P12_BASE64:-}" ] || missing="${missing} MACOS_CERT_P12_BASE64"
      [ -n "${MACOS_CERT_PASSWORD:-}" ]   || missing="${missing} MACOS_CERT_PASSWORD"
      [ -n "${APPLE_ID:-}" ]              || missing="${missing} APPLE_ID"
      [ -n "${APPLE_TEAM_ID:-}" ]         || missing="${missing} APPLE_TEAM_ID"
      [ -n "${APPLE_APP_PASSWORD:-}" ]    || missing="${missing} APPLE_APP_PASSWORD"
      ;;
    *)
      echo "ERROR: assert_signing_ready: unknown platform '${platform}'." >&2
      return 1
      ;;
  esac

  if [ -n "${missing}" ]; then
    echo "ERROR: signing was required for '${platform}' but these input(s) are missing:${missing}." >&2
    return 1
  fi
  return 0
}
