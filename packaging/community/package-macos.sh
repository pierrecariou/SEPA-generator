#!/usr/bin/env bash
#
# Builds a professional macOS DMG for SEPA Generator Community Edition using the
# official JDK `jpackage` tool.
#
# The DMG contains a launchable "SEPA Generator Community.app" that:
#   * bundles its own Java runtime (end users do NOT need to install Java),
#   * uses the macOS .icns icon when available,
#   * can be dragged into /Applications and launched from Launchpad/Finder.
#
# Maven remains the main build tool; jpackage only wraps the built application.
#
# Community Edition only. The Pro edition is packaged separately.
#
set -euo pipefail

# -----------------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------------
step() { printf '==> %s\n' "$1"; }
ok()   { printf '    %s\n' "$1"; }
fail() { printf 'ERROR: %s\n' "$1" >&2; exit 1; }

# Read a required KEY=VALUE from edition.properties (value taken verbatim after
# the first '='). Blank lines and '#' comments are ignored by the '^KEY=' match.
prop() {
  local key="$1" val
  val="$(grep -E "^${key}=" "${EDITION_PROPS}" | head -n1 | cut -d= -f2-)"
  [ -n "${val}" ] || fail "Required key '${key}' missing from ${EDITION_PROPS}."
  printf '%s' "${val}"
}

# Derive the authoritative application version from the Maven generator module,
# so the packaging version can never drift from the POM.
derive_app_version() {
  command -v mvn >/dev/null 2>&1 || fail "Maven (mvn) was not found on PATH; it is required to derive the application version."
  local v
  v="$(cd "${REPO_ROOT}" && mvn -q -DforceStdout -pl generator \
        org.apache.maven.plugins:maven-help-plugin:3.5.0:evaluate -Dexpression=project.version)" \
    || fail "Failed to derive the application version from Maven (help:evaluate)."
  v="$(printf '%s' "${v}" | tail -n1 | tr -d '[:space:]')"
  case "${v}" in
    ''|\$*) fail "Maven returned an invalid application version: '${v}'." ;;
  esac
  printf '%s' "${v}"
}

# -----------------------------------------------------------------------------
# Configuration (from packaging/edition.properties + authoritative Maven version)
# -----------------------------------------------------------------------------
# Paths are resolved relative to the repository root (two levels above this
# script: <repo>/packaging/community/package-macos.sh).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
EDITION_PROPS="${REPO_ROOT}/packaging/edition.properties"
[ -f "${EDITION_PROPS}" ] || fail "Edition properties file not found: ${EDITION_PROPS}"

APP_NAME="$(prop APP_NAME)"
ARTIFACT_SLUG="$(prop ARTIFACT_SLUG)"
VENDOR="$(prop VENDOR)"
DESCRIPTION="$(prop DESCRIPTION)"
MAIN_CLASS="$(prop MAIN_CLASS)"
MAC_PACKAGE_IDENTIFIER="$(prop MAC_PACKAGE_IDENTIFIER)"
MAC_MENU_NAME="$(prop MAC_MENU_NAME)"
COPYRIGHT="Copyright (c) $(date +%Y) ${VENDOR}"

# Authoritative application version (derived from Maven, never hardcoded).
APP_VERSION="$(derive_app_version)"

# Runnable fat JAR produced by the Maven build (generator module, shaded).
MAIN_JAR_NAME="generator-${APP_VERSION}.jar"
MAIN_JAR_PATH="${REPO_ROOT}/generator/target/${MAIN_JAR_NAME}"

# macOS icon. Resolution order (handled later, after the Maven build):
#   1. Prefer a ready-made .icns at ICON_ICNS.
#   2. Otherwise, if a 1024x1024 source PNG exists at ICON_SOURCE_PNG, generate
#      the .icns automatically with the native macOS tools `sips` + `iconutil`.
# Generated icon files are written to an ignored build folder (GENERATED_ICON_DIR
# under target/) so nothing is committed.
ICON_ICNS="${REPO_ROOT}/packaging/macos/sepa-generator.icns"
ICON_SOURCE_PNG="${REPO_ROOT}/packaging/macos/sepa-generator-1024.png"
GENERATED_ICON_DIR="${REPO_ROOT}/target/packaging/macos"

OUTPUT_DIR="${REPO_ROOT}/dist"

# jpackage staging directories (kept under generator/target, which is git-ignored).
STAGE_ROOT="${REPO_ROOT}/generator/target/jpackage"
INPUT_DIR="${STAGE_ROOT}/input"
JP_DEST_DIR="${STAGE_ROOT}/out"

# Final DMG name placed in dist/.
#
# ARCH_LABEL is an optional architecture tag for the output file name (e.g.
# "arm64" or "x64"). It does NOT affect the build itself - jpackage always
# produces a DMG for the architecture of the macOS runner this script runs on;
# the label simply makes the artifact name explicit for release distribution.
# When unset, the canonical name "<slug>-<ver>-macos.dmg" is used (preserving
# the original default behavior).
ARCH_LABEL="${ARCH_LABEL:-}"
if [ -n "${ARCH_LABEL}" ]; then
  FINAL_ARTIFACT="${ARTIFACT_SLUG}-${APP_VERSION}-macos-${ARCH_LABEL}.dmg"
else
  FINAL_ARTIFACT="${ARTIFACT_SLUG}-${APP_VERSION}-macos.dmg"
fi

# -----------------------------------------------------------------------------
# Code signing / notarization (OPTIONAL; DISABLED by default)
#
# A basic DMG build does NOT require an Apple Developer account. Unsigned macOS
# builds are usable for testing/direct distribution, but Gatekeeper warns on
# first launch; users must right-click -> Open. Signing + notarization is opt-in
# and fail-closed (see packaging/community/macos-signing.sh). Enable via:
#   MAC_SIGN=true       and MAC_SIGNING_IDENTITY="Developer ID Application: ..."
#   MAC_NOTARIZE=true   and APPLE_ID / APPLE_TEAM_ID / APPLE_APP_PASSWORD
# Optionally import the certificate from MACOS_CERT_P12_BASE64 (+ MACOS_CERT_PASSWORD)
# into a temporary keychain. Notarization requires signing.
# -----------------------------------------------------------------------------
ENTITLEMENTS_PLIST="${REPO_ROOT}/packaging/macos/entitlements.plist"
# shellcheck source=./macos-signing.sh
source "${SCRIPT_DIR}/macos-signing.sh"
resolve_macos_signing_plan || fail "Invalid macOS signing configuration (see error above)."

# Skip the Maven build (use the jar already present in target/).
SKIP_BUILD="${SKIP_BUILD:-false}"

# Release packaging requires a real app icon: if neither the .icns nor the
# source PNG is available the build fails. Set RELEASE=false for a local/test
# DMG that may fall back to jpackage's default icon.
RELEASE="${RELEASE:-true}"

# -----------------------------------------------------------------------------
# Helpers (icon generation)
# -----------------------------------------------------------------------------
# Generate a macOS .icns from a single high-resolution PNG using the native
# macOS tools `sips` and `iconutil`. Writes an intermediate .iconset next to the
# output .icns. Both tools ship with macOS.
generate_icns_from_png() {
  local src="$1" out_icns="$2"
  command -v sips     >/dev/null 2>&1 || fail "sips not found; it is required to generate the .icns and ships with macOS."
  command -v iconutil >/dev/null 2>&1 || fail "iconutil not found; it is required to generate the .icns and ships with macOS."

  local iconset="${out_icns%.icns}.iconset"
  rm -rf "${iconset}"
  mkdir -p "${iconset}"

  # Standard Apple iconset sizes (1x and 2x from 16 up to 512).
  sips -z 16   16   "${src}" --out "${iconset}/icon_16x16.png"      >/dev/null
  sips -z 32   32   "${src}" --out "${iconset}/icon_16x16@2x.png"   >/dev/null
  sips -z 32   32   "${src}" --out "${iconset}/icon_32x32.png"      >/dev/null
  sips -z 64   64   "${src}" --out "${iconset}/icon_32x32@2x.png"   >/dev/null
  sips -z 128  128  "${src}" --out "${iconset}/icon_128x128.png"    >/dev/null
  sips -z 256  256  "${src}" --out "${iconset}/icon_128x128@2x.png" >/dev/null
  sips -z 256  256  "${src}" --out "${iconset}/icon_256x256.png"    >/dev/null
  sips -z 512  512  "${src}" --out "${iconset}/icon_256x256@2x.png" >/dev/null
  sips -z 512  512  "${src}" --out "${iconset}/icon_512x512.png"    >/dev/null
  sips -z 1024 1024 "${src}" --out "${iconset}/icon_512x512@2x.png" >/dev/null

  iconutil -c icns "${iconset}" -o "${out_icns}" || fail "iconutil failed to build ${out_icns}."
}

# -----------------------------------------------------------------------------
# 1. Verify we are on macOS
# -----------------------------------------------------------------------------
step "Checking operating system"
if [ "$(uname -s)" != "Darwin" ]; then
  fail "This script builds a macOS DMG and must run on macOS (Darwin). Use a macOS machine or a macOS CI runner. Windows packaging lives in package-windows.ps1."
fi
ok "Running on macOS."

# -----------------------------------------------------------------------------
# 2. Locate jpackage (JDK 17+)
# -----------------------------------------------------------------------------
step "Locating jpackage"
JPACKAGE=""
if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/jpackage" ]; then
  JPACKAGE="${JAVA_HOME}/bin/jpackage"
elif command -v jpackage >/dev/null 2>&1; then
  JPACKAGE="$(command -v jpackage)"
elif command -v /usr/libexec/java_home >/dev/null 2>&1; then
  # Fall back to the system's default JDK location on macOS.
  _jh="$(/usr/libexec/java_home 2>/dev/null || true)"
  if [ -n "${_jh}" ] && [ -x "${_jh}/bin/jpackage" ]; then
    JPACKAGE="${_jh}/bin/jpackage"
  fi
fi

if [ -z "${JPACKAGE}" ]; then
  fail "jpackage was not found.
  jpackage ships with the JDK (17 or newer). Install a JDK and either:
    * set JAVA_HOME to the JDK directory, or
    * add <jdk>/bin to PATH.
  Recommended: a current LTS or later JDK (this project builds with JDK 23)."
fi
ok "jpackage found: ${JPACKAGE} (version $("${JPACKAGE}" --version))"

# -----------------------------------------------------------------------------
# 3. Verify Maven and build the application
# -----------------------------------------------------------------------------
if [ "${SKIP_BUILD}" = "true" ]; then
  step "Skipping Maven build (SKIP_BUILD=true)"
else
  step "Building application with Maven (mvn clean package)"
  if ! command -v mvn >/dev/null 2>&1; then
    fail "Maven (mvn) was not found on PATH. Install Maven or add it to PATH."
  fi
  ( cd "${REPO_ROOT}" && mvn clean package ) || fail "Maven build failed."
  ok "Maven build succeeded."
fi

if [ ! -f "${MAIN_JAR_PATH}" ]; then
  fail "Runnable JAR not found: ${MAIN_JAR_PATH}
  Run the Maven build first (do not set SKIP_BUILD=true)."
fi
ok "Application JAR: ${MAIN_JAR_PATH}"

# -----------------------------------------------------------------------------
# 3b. Resolve the macOS icon (prefer .icns; otherwise generate it from PNG).
# Done after the Maven build so 'mvn clean' cannot wipe the generated icon.
# -----------------------------------------------------------------------------
step "Resolving macOS icon"
RESOLVED_ICON=""
if [ -f "${ICON_ICNS}" ]; then
  RESOLVED_ICON="${ICON_ICNS}"
  ok "Using existing icon: ${ICON_ICNS}"
elif [ -f "${ICON_SOURCE_PNG}" ]; then
  mkdir -p "${GENERATED_ICON_DIR}"
  RESOLVED_ICON="${GENERATED_ICON_DIR}/sepa-generator.icns"
  generate_icns_from_png "${ICON_SOURCE_PNG}" "${RESOLVED_ICON}"
  ok "Generated icon from ${ICON_SOURCE_PNG} -> ${RESOLVED_ICON}"
elif [ "${RELEASE}" = "true" ]; then
  fail "No macOS icon available for release packaging.
  Provide one of:
    * ${ICON_ICNS}            (ready-made .icns), or
    * ${ICON_SOURCE_PNG}  (1024x1024 source PNG; auto-converted via sips/iconutil).
  To build a non-release DMG with jpackage's default icon, set RELEASE=false."
else
  ok "No icon found and RELEASE=false; jpackage will use its default icon."
fi

# -----------------------------------------------------------------------------
# 4. Prepare the jpackage input directory (only the app jar goes here)
# -----------------------------------------------------------------------------
step "Preparing jpackage input directory"
rm -rf "${STAGE_ROOT}"
mkdir -p "${INPUT_DIR}" "${JP_DEST_DIR}" "${OUTPUT_DIR}"
cp "${MAIN_JAR_PATH}" "${INPUT_DIR}/${MAIN_JAR_NAME}"
ok "Staged ${MAIN_JAR_NAME} for packaging."

# -----------------------------------------------------------------------------
# 5. Run jpackage
# -----------------------------------------------------------------------------
step "Running jpackage (--type dmg)"

JP_ARGS=(
  --type dmg
  --name "${APP_NAME}"
  --app-version "${APP_VERSION}"
  --vendor "${VENDOR}"
  --copyright "${COPYRIGHT}"
  --description "${DESCRIPTION}"
  --input "${INPUT_DIR}"
  --main-jar "${MAIN_JAR_NAME}"
  --main-class "${MAIN_CLASS}"
  --dest "${JP_DEST_DIR}"
  --mac-package-identifier "${MAC_PACKAGE_IDENTIFIER}"
  --mac-package-name "${MAC_MENU_NAME}"
)

# Attach the macOS icon when one was resolved (existing or generated).
if [ -n "${RESOLVED_ICON}" ]; then
  JP_ARGS+=( --icon "${RESOLVED_ICON}" )
  ok "Using icon: ${RESOLVED_ICON}"
fi

# Optional Developer ID signing. jpackage signs the .app AND its nested Java
# runtime; --mac-entitlements applies the hardened-runtime entitlements needed
# by the bundled JVM. The signing identity value is not echoed.
if [ "${MAC_DO_SIGN}" = "true" ]; then
  setup_macos_keychain
  JP_ARGS+=( --mac-sign --mac-signing-key-user-name "${MAC_SIGNING_IDENTITY}" )
  if [ -f "${ENTITLEMENTS_PLIST}" ]; then
    JP_ARGS+=( --mac-entitlements "${ENTITLEMENTS_PLIST}" )
  fi
  ok "Code signing ENABLED (Developer ID; hardened-runtime entitlements applied)."
else
  ok "Code signing DISABLED (unsigned build; Gatekeeper will warn on first launch)."
fi

"${JPACKAGE}" "${JP_ARGS[@]}" || fail "jpackage failed. See the output above for the exact cause."

# -----------------------------------------------------------------------------
# 6. Move the produced DMG into dist/ with the canonical name
# -----------------------------------------------------------------------------
step "Collecting DMG"
PRODUCED="$(find "${JP_DEST_DIR}" -maxdepth 1 -name '*.dmg' | head -n 1)"
[ -n "${PRODUCED}" ] || fail "Could not find the generated .dmg in ${JP_DEST_DIR}."
FINAL_PATH="${OUTPUT_DIR}/${FINAL_ARTIFACT}"
rm -f "${FINAL_PATH}"
mv "${PRODUCED}" "${FINAL_PATH}"
ok "DMG: ${FINAL_PATH}"

# -----------------------------------------------------------------------------
# 7. Optional notarization + stapling (requires a signed app)
# -----------------------------------------------------------------------------
NOTARIZED=false
if [ "${MAC_DO_NOTARIZE}" = "true" ]; then
  notarize_and_staple "${FINAL_PATH}" || fail "Notarization/stapling failed; the DMG is NOT notarized."
  NOTARIZED=true
  ok "DMG notarized, stapled and validated."
elif [ "${MAC_DO_SIGN}" = "true" ]; then
  ok "App is signed but the DMG was NOT notarized (MAC_NOTARIZE not set)."
fi

printf '\n'
if [ "${NOTARIZED}" = "true" ]; then
  SIGN_STATE="signed + notarized + stapled"
elif [ "${MAC_DO_SIGN}" = "true" ]; then
  SIGN_STATE="signed (not notarized)"
else
  SIGN_STATE="unsigned"
fi
printf 'SUCCESS: %s %s packaged (%s).\n' "${APP_NAME}" "${APP_VERSION}" "${SIGN_STATE}"
printf 'Output : %s\n' "${FINAL_PATH}"
