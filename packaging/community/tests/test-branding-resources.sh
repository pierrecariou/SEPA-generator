#!/usr/bin/env bash
#
# Static test for the Community installer branding resources.
#
# The artwork is committed (not generated at build time) so every runner
# produces an identical installer, which means nothing else would notice if a
# file went missing, changed size, or lost the hooks that actually wire it into
# jpackage. This test checks exactly that, without building anything.
#
# Run: bash packaging/community/tests/test-branding-resources.sh
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${HERE}/../../.." && pwd)"

fails=0
pass()  { printf 'PASS: %s\n' "$1"; }
failc() { printf 'FAIL: %s\n' "$1"; fails=$((fails + 1)); }

# Reads the pixel dimensions of a Windows BMP from its DIB header (little-endian
# 32-bit width at offset 18 and height at offset 22). Avoids needing ImageMagick.
bmp_dimensions() {
  od -An -tu4 -j18 -N8 "$1" 2>/dev/null | tr -s ' ' | sed 's/^ //' | tr ' ' 'x'
}

# ---------------------------------------------------------------------------
# 1. The artwork exists and has the dimensions WiX / Finder expect.
# ---------------------------------------------------------------------------
BANNER="${ROOT}/packaging/windows/branding/banner.bmp"
DIALOG="${ROOT}/packaging/windows/branding/dialog.bmp"
DMG_BG="${ROOT}/packaging/macos/branding/dmg-background.png"

for f in "${BANNER}" "${DIALOG}" "${DMG_BG}"; do
  if [ -s "${f}" ]; then
    pass "branding artwork present: ${f#"${ROOT}/"}"
  else
    failc "branding artwork missing or empty: ${f#"${ROOT}/"}"
  fi
done

# WiX's standard bitmap sizes; anything else is stretched and looks broken.
if [ -f "${BANNER}" ] && [ "$(bmp_dimensions "${BANNER}")" = "493x58" ]; then
  pass "banner.bmp is WiX's required 493x58"
else
  failc "banner.bmp must be 493x58; got $( [ -f "${BANNER}" ] && bmp_dimensions "${BANNER}" || echo missing)"
fi

if [ -f "${DIALOG}" ] && [ "$(bmp_dimensions "${DIALOG}")" = "493x312" ]; then
  pass "dialog.bmp is WiX's required 493x312"
else
  failc "dialog.bmp must be 493x312; got $( [ -f "${DIALOG}" ] && bmp_dimensions "${DIALOG}" || echo missing)"
fi

# ---------------------------------------------------------------------------
# 2. The WiX main.wxs override still carries its branding hooks.
# ---------------------------------------------------------------------------
MAIN_WXS="${ROOT}/packaging/windows/branding/main.wxs"

if [ -f "${MAIN_WXS}" ]; then
  for marker in '<!-- BEGIN SEPA-BRANDING' '<!-- END SEPA-BRANDING -->'; do
    if grep -qF "${marker}" "${MAIN_WXS}"; then
      pass "main.wxs contains marker: ${marker}"
    else
      failc "main.wxs is missing marker '${marker}' (package-windows.ps1 uses it to verify the override against the JDK template)"
    fi
  done

  for token in 'WixUIBannerBmp' 'WixUIDialogBmp' '@JP_BANNER_BMP@' '@JP_DIALOG_BMP@'; do
    if grep -qF "${token}" "${MAIN_WXS}"; then
      pass "main.wxs wires ${token}"
    else
      failc "main.wxs is missing ${token}"
    fi
  done

  # The upgrade code must keep coming from jpackage's variable, never a literal:
  # hard-coding it here would silently break in-place upgrades.
  if grep -qF 'UpgradeCode="$(var.JpProductUpgradeCode)"' "${MAIN_WXS}"; then
    pass "main.wxs still takes the upgrade code from jpackage"
  else
    failc "main.wxs must keep UpgradeCode=\$(var.JpProductUpgradeCode)"
  fi
else
  failc "main.wxs override not found: ${MAIN_WXS}"
fi

# ---------------------------------------------------------------------------
# 3. The packaging scripts actually pass the resources to jpackage.
# ---------------------------------------------------------------------------
WIN_SCRIPT="${ROOT}/packaging/community/package-windows.ps1"
MAC_SCRIPT="${ROOT}/packaging/community/package-macos.sh"

if grep -q -- '--resource-dir' "${WIN_SCRIPT}"; then
  pass "package-windows.ps1 passes --resource-dir to jpackage"
else
  failc "package-windows.ps1 must pass --resource-dir to jpackage"
fi

if grep -q -- '--resource-dir' "${MAC_SCRIPT}"; then
  pass "package-macos.sh passes --resource-dir to jpackage"
else
  failc "package-macos.sh must pass --resource-dir to jpackage"
fi

# jpackage only recognises this exact filename for the DMG volume background.
if grep -qF 'background_dmg.tiff' "${MAC_SCRIPT}"; then
  pass "package-macos.sh stages background_dmg.tiff (jpackage's expected name)"
else
  failc "package-macos.sh must stage the background as 'background_dmg.tiff'"
fi

# ---------------------------------------------------------------------------
# 4. Released packaging identity is branded and unchanged where it must be.
# ---------------------------------------------------------------------------
EDITION_PROPS="${ROOT}/packaging/edition.properties"
expect_prop() {
  local key="$1" want="$2" got
  got="$(grep -E "^${key}=" "${EDITION_PROPS}" | head -n1 | cut -d= -f2-)"
  if [ "${got}" = "${want}" ]; then
    pass "${key}=${want}"
  else
    failc "${key} should be '${want}' but is '${got}'"
  fi
}

expect_prop APP_NAME 'SEPA Generator Community'
expect_prop VENDOR   'Niryosys'
expect_prop HOMEPAGE 'https://sepa-xml-generator.com'
# Debian shows this verbatim in `apt show`, so it must carry the publisher name.
expect_prop DEB_MAINTAINER 'Niryosys <contact@sepa-xml-generator.com>'
# Changing this breaks in-place upgrades for every installed Community user.
expect_prop WIN_UPGRADE_UUID 'b1f8e2a4-3c7d-4e15-9a2b-7c6d5e4f3a21'

if [ "${fails}" -gt 0 ]; then
  printf '%d branding test(s) failed.\n' "${fails}"
  exit 1
fi
printf 'All branding tests passed.\n'
