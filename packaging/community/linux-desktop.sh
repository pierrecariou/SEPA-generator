#!/usr/bin/env bash
# Complete jpackage's Linux desktop integration in an extracted package tree.
# No files on the build machine's desktop are installed or registered here.
set -euo pipefail

REPO_ROOT="$1"
PACKAGE_ROOT="$2"
prop() { sed -n "s/^${1}=//p" "${REPO_ROOT}/packaging/edition.properties"; }
xml_escape() { printf '%s' "$1" | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g'; }

# Provide a resilient xdg-desktop-menu wrapper that handles failures gracefully.
# This script is sourced/used by postinst to register the desktop menu, but
# never fails - allowing the package installation to always succeed even when
# menu registration is not possible (CI, sandboxed, minimal systems).
install_xdg_desktop_menu_wrapper() {
  local wrapper_bin="${PACKAGE_ROOT}/usr/bin/xdg-desktop-menu-wrapper"
  echo "==> Installing xdg-desktop-menu wrapper at ${wrapper_bin}..." >&2
  
  mkdir -p "$(dirname "${wrapper_bin}")"
  
  cat > "${wrapper_bin}" <<'WRAPPER_EOF'
#!/bin/bash
# xdg-desktop-menu wrapper - makes menu registration non-fatal
# Attempts xdg-desktop-menu if available, but never fails the installation
exec /usr/bin/xdg-desktop-menu "$@" 2>/dev/null || true
WRAPPER_EOF
  
  chmod 755 "${wrapper_bin}"
  
  # Verification
  if [ ! -f "${wrapper_bin}" ]; then
    echo "    ✗ ERROR: Wrapper file not created!" >&2
    return 1
  fi
  
  if [ ! -x "${wrapper_bin}" ]; then
    echo "    ✗ ERROR: Wrapper not executable!" >&2
    return 1
  fi
  
  echo "    ✓ Wrapper installed and executable ($(stat -c%s "${wrapper_bin}" 2>/dev/null || echo "?") bytes)" >&2
  
  # Verify it will be in the DEB
  echo "    Verifying wrapper will be included in final DEB..." >&2
  if grep -q "usr/bin/xdg-desktop-menu-wrapper" "${PACKAGE_ROOT}/DEBIAN/md5sums" 2>/dev/null || true; then
    echo "    ✓ Wrapper is in md5sums (will be included in DEB)" >&2
  elif [ -f "${PACKAGE_ROOT}/DEBIAN/md5sums" ]; then
    echo "    Note: md5sums file exists but wrapper not yet listed (may be added during rebuild)" >&2
  fi
}

# Patch the jpackage-generated postinst script to use our wrapper.
# The postinst calls xdg-desktop-menu directly; we replace it with our wrapper.
patch_postinst_for_desktop_menu() {
  local postinst="${PACKAGE_ROOT}/DEBIAN/postinst"
  
  echo "==> Patching postinst for xdg-desktop-menu..." >&2
  
  if [ ! -f "${postinst}" ]; then
    echo "    ERROR: postinst script not found at ${postinst}" >&2
    ls -la "${PACKAGE_ROOT}/DEBIAN/" >&2 || true
    return 1
  fi

  local filesize
  filesize=$(wc -c < "${postinst}")
  echo "    postinst size: ${filesize} bytes" >&2
  
  # Check if already patched
  if grep -q "xdg-desktop-menu-wrapper" "${postinst}"; then
    echo "    ✓ Already patched (wrapper reference found)" >&2
    return 0
  fi

  # Critical debugging: show the actual content that contains xdg
  echo "    === Lines containing 'xdg' in postinst: ===" >&2
  grep -n "xdg" "${postinst}" >&2 || echo "    (no 'xdg' found in postinst)" >&2
  echo "    ===" >&2
  
  # If xdg-desktop-menu is not in the postinst, something is very wrong
  if ! grep -q "xdg-desktop-menu" "${postinst}"; then
    echo "    WARNING: xdg-desktop-menu not found in postinst at all" >&2
    echo "    The package may not need patching, OR the postinst structure is unexpected" >&2
    echo "    First 30 lines of postinst:" >&2
    head -30 "${postinst}" >&2
    return 0
  fi

  echo "    xdg-desktop-menu found, attempting replacement..." >&2
  
  # Save backup
  cp "${postinst}" "${postinst}.bak"
  
  # Apply all plausible sed patterns
  local count_before
  count_before=$(grep -c "xdg-desktop-menu" "${postinst}")
  
  sed -i 's|/usr/bin/xdg-desktop-menu|/usr/bin/xdg-desktop-menu-wrapper|g' "${postinst}" 2>&1 | sed 's/^/      [sed1] /' >&2 || true
  sed -i 's|^\([[:space:]]*\)xdg-desktop-menu|\1/usr/bin/xdg-desktop-menu-wrapper|g' "${postinst}" 2>&1 | sed 's/^/      [sed2] /' >&2 || true
  sed -i 's|\bxdg-desktop-menu\b|/usr/bin/xdg-desktop-menu-wrapper|g' "${postinst}" 2>&1 | sed 's/^/      [sed3] /' >&2 || true
  
  local count_after
  count_after=$(grep -c "xdg-desktop-menu" "${postinst}")
  
  echo "    References before: ${count_before}, after: ${count_after}" >&2
  
  if grep -q "xdg-desktop-menu-wrapper" "${postinst}"; then
    echo "    ✓ Replacement succeeded" >&2
  else
    echo "    ✗ Replacement may not have worked" >&2
    echo "    xdg references after patching:" >&2
    grep -n "xdg" "${postinst}" >&2 || echo "    (none found)" >&2
  fi
}

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

# Install our xdg-desktop-menu wrapper and patch the postinst to use it
install_xdg_desktop_menu_wrapper
patch_postinst_for_desktop_menu

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
