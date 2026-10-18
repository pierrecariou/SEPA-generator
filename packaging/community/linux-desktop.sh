#!/usr/bin/env bash
# Complete jpackage's Linux desktop integration in an extracted package tree.
# No files on the build machine's desktop are installed or registered here.
set -euo pipefail

REPO_ROOT="$1"
PACKAGE_ROOT="$2"
prop() { sed -n "s/^${1}=//p" "${REPO_ROOT}/packaging/edition.properties"; }
xml_escape() { printf '%s' "$1" | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g'; }

APP_NAME="$(prop APP_NAME)"
PACKAGE_NAME="$(prop LINUX_PACKAGE_NAME)"
MAIN_CLASS="$(prop MAIN_CLASS)"
# Reuse the released edition identity as the new AppStream component ID.
# This is technical metadata, not the displayed name or publisher.
APP_ID="$(prop MAC_PACKAGE_IDENTIFIER)"
DESKTOP_ID="${PACKAGE_NAME}-${APP_NAME// /_}.desktop"
APP_LIB="${PACKAGE_ROOT}/opt/${PACKAGE_NAME}/lib"
DESKTOP_SOURCE="${APP_LIB}/${DESKTOP_ID}"

if [ ! -f "${DESKTOP_SOURCE}" ]; then
  echo "ERROR: jpackage desktop layout changed: missing ${DESKTOP_SOURCE}" >&2
  exit 1
fi

# The bundled JVM needs libasound.so.2. On amd64, both pre-t64 libasound2
# (Debian 12 / Ubuntu 22.04) and libasound2t64 supply that SONAME. jpackage
# records only the build host's package name. Allow either provider, retaining
# every other dependency. Match only an unversioned standalone dependency;
# leave version constraints and existing alternatives alone.
sed -i -E 's/(^Depends:[[:space:]]*|,[[:space:]]*)libasound2t64([[:space:]]*,|[[:space:]]*$)/\1libasound2t64 | libasound2\2/' \
  "${PACKAGE_ROOT}/DEBIAN/control"

# OpenJDK's XToolkit derives WM_CLASS from the main class, replacing dots with
# dashes. Match it instead of renaming Java packages or using private AWT APIs.
# Keep jpackage's quoted Exec path and the released desktop filename unchanged.
sed -i "/^StartupWMClass=/d; s|^Icon=.*|Icon=${PACKAGE_NAME}|; s|^Categories=.*|Categories=$(prop LINUX_MENU_GROUP);|" "${DESKTOP_SOURCE}"
printf 'StartupWMClass=%s\n' "${MAIN_CLASS//./-}" >> "${DESKTOP_SOURCE}"

# Ship the entry in the standard location so package viewers can discover it
# before installation. Keep the /opt copy for jpackage's existing xdg install /
# uninstall hooks; both copies have the SAME desktop ID, hence one menu entry.
install -Dm644 "${DESKTOP_SOURCE}" "${PACKAGE_ROOT}/usr/share/applications/${DESKTOP_ID}"

# Reuse the already-rendered application icons. No new artwork or image tools.
for size in 16 32 48 64 256; do
  install -Dm644 \
    "${REPO_ROOT}/view/src/main/resources/sepa-generator-icon-app-bg-${size}.png" \
    "${PACKAGE_ROOT}/usr/share/icons/hicolor/${size}x${size}/apps/${PACKAGE_NAME}.png"
done

METAINFO="${PACKAGE_ROOT}/usr/share/metainfo/${APP_ID}.metainfo.xml"
mkdir -p "$(dirname "${METAINFO}")"
cat > "${METAINFO}" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<component type="desktop-application">
  <id>${APP_ID}</id>
  <metadata_license>CC0-1.0</metadata_license>
  <project_license>Apache-2.0</project_license>
  <name>$(xml_escape "${APP_NAME}")</name>
  <summary>Generate SEPA credit transfer XML from CSV and Excel</summary>
  <description>
    <p>$(xml_escape "${APP_NAME}"), published by $(xml_escape "$(prop VENDOR)"), generates SEPA credit transfer XML files from CSV, XLS and XLSX payment files.</p>
    <p>Payment processing runs locally on your computer. Configure the debtor, select an input file, execution date and pain.001 format, then generate the XML.</p>
  </description>
  <developer id="com.niryosys">
    <name>$(xml_escape "$(prop VENDOR)")</name>
  </developer>
  <url type="homepage">$(xml_escape "$(prop HOMEPAGE)")</url>
  <launchable type="desktop-id">${DESKTOP_ID}</launchable>
  <icon type="stock">${PACKAGE_NAME}</icon>
  <categories>
    <category>Office</category>
    <category>Finance</category>
  </categories>
  <content_rating type="oars-1.1" />
</component>
EOF
chmod 644 "${METAINFO}"
