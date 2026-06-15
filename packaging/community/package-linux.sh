#!/usr/bin/env bash
#
# Builds a Linux x64 Debian package (.deb) for SEPA Generator Community Edition
# using the official JDK `jpackage` tool.
#
# The .deb installs a launchable application that:
#   * bundles its own Java runtime (end users do NOT need to install Java),
#   * registers a desktop/menu entry with the app icon,
#   * can be removed cleanly with the system package manager.
#
# Targets Debian/Ubuntu-compatible distributions (anything using dpkg/apt).
#
# Maven remains the main build tool; jpackage only wraps the built application.
#
# Community Edition only. The Pro edition is packaged separately.
#
set -euo pipefail

# -----------------------------------------------------------------------------
# Configuration (centralized - edit here)
# -----------------------------------------------------------------------------
APP_NAME="SEPA Generator Community"
APP_VERSION="1.3.0"
VENDOR="Pierre Cariou"
DESCRIPTION="Generate SEPA payment XML files from spreadsheet inputs."
COPYRIGHT="Copyright (c) $(date +%Y) ${VENDOR}"

# Linux package metadata.
LINUX_PACKAGE_NAME="sepa-generator-community"
# Freedesktop menu categories for an office/finance utility (placed in the
# generated .desktop "Categories=" entry).
LINUX_MENU_GROUP="Office;Finance"
# Debian "Section" field for the package.
LINUX_APP_CATEGORY="utils"
# Debian maintainer (matches the project's official contact in AppLinks.CONTACT).
LINUX_DEB_MAINTAINER="contact@sepa-xml-generator.com"

# Runnable fat JAR produced by the Maven build (generator module, shaded).
MAIN_JAR_NAME="generator-${APP_VERSION}.jar"
MAIN_CLASS="com.pcariou.generator.Generator"

# Paths are resolved relative to the repository root (two levels above this
# script: <repo>/packaging/community/package-linux.sh).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MAIN_JAR_PATH="${REPO_ROOT}/generator/target/${MAIN_JAR_NAME}"

# Linux icon (PNG). Resolution order (handled after the Maven build):
#   1. Prefer packaging/linux/sepa-generator.png.
#   2. Otherwise fall back to the shared 1024x1024 source PNG under packaging/macos.
# jpackage scales the PNG for the generated desktop entry.
ICON_PNG="${REPO_ROOT}/packaging/linux/sepa-generator.png"
ICON_SOURCE_PNG="${REPO_ROOT}/packaging/macos/sepa-generator-1024.png"

OUTPUT_DIR="${REPO_ROOT}/dist"

# jpackage staging directories (kept under generator/target, which is git-ignored).
STAGE_ROOT="${REPO_ROOT}/generator/target/jpackage"
INPUT_DIR="${STAGE_ROOT}/input"
JP_DEST_DIR="${STAGE_ROOT}/out"

# Final .deb name placed in dist/.
#
# ARCH_LABEL is an architecture tag for the output file name (e.g. "x64"). It
# does NOT affect the build - jpackage produces a .deb for the architecture of
# the Linux runner; the label only makes the release artifact name explicit.
ARCH_LABEL="${ARCH_LABEL:-x64}"
FINAL_ARTIFACT="SEPA-Generator-Community-${APP_VERSION}-linux-${ARCH_LABEL}.deb"

# Skip the Maven build (use the jar already present in target/).
SKIP_BUILD="${SKIP_BUILD:-false}"

# Release packaging requires a real app icon: if neither the PNG nor the shared
# source PNG is available the build fails. Set RELEASE=false for a local/test
# build that may fall back to jpackage's default icon.
RELEASE="${RELEASE:-true}"

# -----------------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------------
step() { printf '==> %s\n' "$1"; }
ok()   { printf '    %s\n' "$1"; }
fail() { printf 'ERROR: %s\n' "$1" >&2; exit 1; }

# -----------------------------------------------------------------------------
# 1. Verify we are on Linux
# -----------------------------------------------------------------------------
step "Checking operating system"
if [ "$(uname -s)" != "Linux" ]; then
  fail "This script builds a Linux .deb and must run on Linux. Use a Linux machine or a Linux CI runner. Windows packaging lives in package-windows.ps1 and macOS packaging in package-macos.sh."
fi
ok "Running on Linux."

# -----------------------------------------------------------------------------
# 2. Locate jpackage (JDK 17+)
# -----------------------------------------------------------------------------
step "Locating jpackage"
JPACKAGE=""
if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/jpackage" ]; then
  JPACKAGE="${JAVA_HOME}/bin/jpackage"
elif command -v jpackage >/dev/null 2>&1; then
  JPACKAGE="$(command -v jpackage)"
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
# 3. Verify Debian packaging tools required by jpackage for .deb
# -----------------------------------------------------------------------------
step "Checking Debian packaging tools (required by jpackage for .deb)"
_missing=""
command -v dpkg-deb >/dev/null 2>&1 || _missing="${_missing} dpkg-deb"
command -v fakeroot >/dev/null 2>&1 || _missing="${_missing} fakeroot"
if [ -n "${_missing}" ]; then
  fail "Missing Debian packaging tool(s):${_missing}
  jpackage needs these to build a .deb. On Debian/Ubuntu install them with:
    sudo apt-get update && sudo apt-get install -y fakeroot dpkg
  (dpkg-deb is part of the 'dpkg' package; 'fakeroot' is a separate package.)"
fi
ok "Debian packaging tools found (dpkg-deb, fakeroot)."

# -----------------------------------------------------------------------------
# 4. Verify Maven and build the application
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
# 4b. Resolve the Linux icon (PNG). Done after the Maven build so 'mvn clean'
# cannot wipe anything staged under target/.
# -----------------------------------------------------------------------------
step "Resolving Linux icon"
RESOLVED_ICON=""
if [ -f "${ICON_PNG}" ]; then
  RESOLVED_ICON="${ICON_PNG}"
  ok "Using icon: ${ICON_PNG}"
elif [ -f "${ICON_SOURCE_PNG}" ]; then
  RESOLVED_ICON="${ICON_SOURCE_PNG}"
  ok "Using shared source icon: ${ICON_SOURCE_PNG}"
elif [ "${RELEASE}" = "true" ]; then
  fail "No Linux icon available for release packaging.
  Provide a PNG at:
    * ${ICON_PNG}   (preferred), or
    * ${ICON_SOURCE_PNG}  (shared 1024x1024 source).
  To build a non-release .deb with jpackage's default icon, set RELEASE=false."
else
  ok "No icon found and RELEASE=false; jpackage will use its default icon."
fi

# -----------------------------------------------------------------------------
# 5. Prepare the jpackage input directory (only the app jar goes here)
# -----------------------------------------------------------------------------
step "Preparing jpackage input directory"
rm -rf "${STAGE_ROOT}"
mkdir -p "${INPUT_DIR}" "${JP_DEST_DIR}" "${OUTPUT_DIR}"
cp "${MAIN_JAR_PATH}" "${INPUT_DIR}/${MAIN_JAR_NAME}"
ok "Staged ${MAIN_JAR_NAME} for packaging."

# -----------------------------------------------------------------------------
# 6. Run jpackage
# -----------------------------------------------------------------------------
step "Running jpackage (--type deb)"

JP_ARGS=(
  --type deb
  --name "${APP_NAME}"
  --app-version "${APP_VERSION}"
  --vendor "${VENDOR}"
  --copyright "${COPYRIGHT}"
  --description "${DESCRIPTION}"
  --input "${INPUT_DIR}"
  --main-jar "${MAIN_JAR_NAME}"
  --main-class "${MAIN_CLASS}"
  --dest "${JP_DEST_DIR}"
  --linux-package-name "${LINUX_PACKAGE_NAME}"
  --linux-app-category "${LINUX_APP_CATEGORY}"
  --linux-menu-group "${LINUX_MENU_GROUP}"
  --linux-deb-maintainer "${LINUX_DEB_MAINTAINER}"
  --linux-shortcut
)

# Attach the Linux icon when one was resolved.
if [ -n "${RESOLVED_ICON}" ]; then
  JP_ARGS+=( --icon "${RESOLVED_ICON}" )
  ok "Using icon: ${RESOLVED_ICON}"
fi

"${JPACKAGE}" "${JP_ARGS[@]}" || fail "jpackage failed. See the output above for the exact cause."

# -----------------------------------------------------------------------------
# 7. Move the produced .deb into dist/ with the canonical name
# -----------------------------------------------------------------------------
step "Collecting .deb"
PRODUCED="$(find "${JP_DEST_DIR}" -maxdepth 1 -name '*.deb' | head -n 1)"
[ -n "${PRODUCED}" ] || fail "Could not find the generated .deb in ${JP_DEST_DIR}."
FINAL_PATH="${OUTPUT_DIR}/${FINAL_ARTIFACT}"
rm -f "${FINAL_PATH}"
mv "${PRODUCED}" "${FINAL_PATH}"
ok ".deb: ${FINAL_PATH}"

printf '\n'
printf 'SUCCESS: %s %s packaged.\n' "${APP_NAME}" "${APP_VERSION}"
printf 'Output : %s\n' "${FINAL_PATH}"
