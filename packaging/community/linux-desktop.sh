#!/bin/bash
# Complete Linux desktop integration for SEPA Generator Community DEB package.
# Adds standard desktop discovery metadata, icons, and AppStream information
# to the DEB structure before final repackaging.
#
# Called by package-linux.sh after jpackage creates the base DEB.
# Input: $1 = repository root, $2 = extracted DEB root path
set -euo pipefail

REPO_ROOT="$1"
PACKAGE_ROOT="$2"

# Read properties from the edition file (used by all packaging scripts).
prop() {
  sed -n "s/^${1}=//p" "${REPO_ROOT}/packaging/edition.properties"
}

# Safely escape XML special characters.
xml_escape() {
  printf '%s' "$1" | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g'
}

APP_NAME="$(prop APP_NAME)"
PACKAGE_NAME="$(prop LINUX_PACKAGE_NAME)"
MAIN_CLASS="$(prop MAIN_CLASS)"
APP_ID="$(prop MAC_PACKAGE_IDENTIFIER)"
VENDOR="$(prop VENDOR)"
HOMEPAGE="$(prop HOMEPAGE)"
MENU_GROUP="$(prop LINUX_MENU_GROUP)"
DESKTOP_ID="${PACKAGE_NAME}-${APP_NAME// /_}.desktop"
APP_LIB="${PACKAGE_ROOT}/opt/${PACKAGE_NAME}/lib"
DESKTOP_SOURCE="${APP_LIB}/${DESKTOP_ID}"

echo "==> Adding Linux desktop integration (${PACKAGE_NAME})..." >&2

# Verify the expected desktop entry exists (jpackage provides this).
if [ ! -f "${DESKTOP_SOURCE}" ]; then
  echo "ERROR: jpackage did not produce expected desktop entry: ${DESKTOP_SOURCE}" >&2
  exit 1
fi

# Fix the ALSA library dependency to support both old and new package names.
# The bundled JVM needs libasound.so.2, which is provided by:
#   - libasound2 (Ubuntu 22.04, Debian 12)
#   - libasound2t64 (Ubuntu 24.04)
# jpackage detected only the build-host package. We allow either.
echo "    Updating ALSA dependency for portability..." >&2
sed -i -E 's/(^Depends:[[:space:]]*|,[[:space:]]*)libasound2t64([[:space:]]*,|[[:space:]]*$)/\1libasound2t64 | libasound2\2/' \
  "${PACKAGE_ROOT}/DEBIAN/control"

# Configure the desktop entry with proper metadata.
# Remove auto-generated fallback entries and set correct values.
echo "    Configuring desktop entry..." >&2
sed -i "/^StartupWMClass=/d; s|^Icon=.*|Icon=${PACKAGE_NAME}|; s|^Categories=.*|Categories=${MENU_GROUP};|" \
  "${DESKTOP_SOURCE}"

# Add StartupWMClass for proper window/launcher association.
# OpenJDK XToolkit derives WM_CLASS from the main class, converting dots to dashes.
# This matches the expected window class and ensures the desktop shell associates
# the running application with the launcher (correct icon in task switcher, etc.).
printf 'StartupWMClass=%s\n' "${MAIN_CLASS//./-}" >> "${DESKTOP_SOURCE}"

# Install the desktop entry under /usr/share/applications for discovery.
# Modern package managers and desktop shells read this location. Keep the /opt
# copy for jpackage's existing xdg-desktop-menu hooks (both use the same ID,
# resulting in one menu entry, not duplicates).
echo "    Installing desktop entry to /usr/share/applications..." >&2
install -Dm644 "${DESKTOP_SOURCE}" "${PACKAGE_ROOT}/usr/share/applications/${DESKTOP_ID}"

# Install icon-theme entries for proper UI representation.
# Use existing PNG files from the application resources (no image tools required).
echo "    Installing application icons to icon theme..." >&2
for size in 16 32 48 64 256; do
  install -Dm644 \
    "${REPO_ROOT}/view/src/main/resources/sepa-generator-icon-app-bg-${size}.png" \
    "${PACKAGE_ROOT}/usr/share/icons/hicolor/${size}x${size}/apps/${PACKAGE_NAME}.png"
done

# Install AppStream metadata for package manager discovery.
# Provides the application's name, description, categories, and publisher info
# to package managers and app stores that read this metadata.
echo "    Installing AppStream metadata..." >&2
METAINFO="${PACKAGE_ROOT}/usr/share/metainfo/${APP_ID}.metainfo.xml"
mkdir -p "$(dirname "${METAINFO}")"
cat > "${METAINFO}" <<'METAINFO_EOF'
<?xml version="1.0" encoding="UTF-8"?>
<component type="desktop-application">
  <id>com.pcariou.sepa.community</id>
  <metadata_license>CC0-1.0</metadata_license>
  <project_license>Apache-2.0</project_license>
  <name>SEPA Generator Community</name>
  <summary>Generate SEPA credit transfer XML from CSV and Excel</summary>
  <description>
    <p>SEPA Generator Community, published by Niryosys, generates SEPA credit transfer XML files from CSV, XLS and XLSX payment files.</p>
    <p>Payment processing runs locally on your computer. Configure the debtor, select an input file, execution date and pain.001 format, then generate the XML.</p>
  </description>
  <developer id="com.niryosys">
    <name>Niryosys</name>
  </developer>
  <url type="homepage">https://sepa-xml-generator.com</url>
  <launchable type="desktop-id">sepa-generator-community-SEPA_Generator_Community.desktop</launchable>
  <icon type="stock">sepa-generator-community</icon>
  <categories>
    <category>Office</category>
    <category>Finance</category>
  </categories>
  <content_rating type="oars-1.1" />
</component>
METAINFO_EOF
chmod 644 "${METAINFO}"

echo "    ✓ Linux desktop integration complete." >&2
