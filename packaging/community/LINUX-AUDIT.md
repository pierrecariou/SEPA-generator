# Community Linux packaging audit

Audited from `main` at `f8da580`, with Linux as the primary target. Community
and Pro are separate repositories. Public identity is SEPA Generator Community,
published by Niryosys; the product name is SEPA Generator.

## Evidence and root causes

The published
[1.4.0 Linux package](https://github.com/pierrecariou/SEPA-generator/releases/download/v1.4.0/SEPA-Generator-Community-1.4.0-linux-x64.deb)
was downloaded and its SHA-256 verified against the release's `SHA256SUMS.txt`:

```text
2602cbdbf5b652c57412934fd9a40ef4a6e0b41db895d754631ba00815c0d22e
```

Its archive contains `debian-binary`, `control.tar.zst` and `data.tar.zst`, with
no embedded signature member. Inspection of the actual control and data
archives found:

- `Package: sepa-generator-community`, version `1.4.0`, architecture `amd64`.
- `Maintainer: Niryosys <contact@sepa-xml-generator.com>`, the official homepage
  and the configured spreadsheet-to-SEPA description, already correct.
- Native launcher `/opt/sepa-generator-community/bin/SEPA Generator Community`.
- Desktop entry
  `/opt/sepa-generator-community/lib/sepa-generator-community-SEPA_Generator_Community.desktop`,
  with the correct `Name`, quoted executable path and `Office;Finance` categories.
- Valid 1024×1024 PNGs under `/opt/sepa-generator-community/lib/`, including the
  absolute icon path named in the desktop entry. The icon is not absent from
  the released package. jpackage copies this PNG; it does not create icon-theme
  sizes for the application's menu icon.
- No `StartupWMClass`, standard icon-theme entries, AppStream metainfo, or
  package-owned `/usr/share/applications` entry. jpackage's `postinst` registers
  the `/opt` desktop entry with `xdg-desktop-menu`; `prerm` unregisters it.

### Running application icon and internal-looking name

The main window already sets its title to `SEPA Generator v<version>` and loads
the existing classpath window icons. The native launcher and desktop `Name`
are correctly branded. There is no evidence that Java packages need renaming.

[OpenJDK 23 XToolkit](https://github.com/openjdk/jdk/blob/jdk-23-ga/src/java.desktop/unix/classes/sun/awt/X11/XToolkit.java)
derives the application class from the main class on the initial toolkit stack,
and replaces dots with dashes. Here it becomes
`com-pcariou-generator-Generator`. jpackage's stock desktop template lacks a
`StartupWMClass` association. A desktop shell can therefore fail to associate
the running window with the branded launcher, exposing the technical class or
a generic icon. This is the identified integration defect; the precise desktop
symptom still needs confirmation on the user's Linux environment. A missing
menu icon alone cannot be attributed to a missing PNG, since the released
absolute icon reference is valid.

The fix matches this existing class through the standard
[StartupWMClass key](https://specifications.freedesktop.org/desktop-entry-spec/latest/recognized-keys.html).
No private AWT reflection, new JVM options, package renames, or storage changes
are needed. Native Wayland support in a future JDK would need separate testing;
the current JDK 23 Swing application runs through X11/XWayland.

### Package-manager artwork

The old artwork was confined to `/opt`, and the desktop entry became discoverable
in the menu through a post-install hook. A GUI examining the local archive had
no AppStream description or standard icon-theme resources to associate with it.
There is no Debian control field for a universal installer banner/icon.

The fix adds package-owned standard desktop metadata, icon-theme PNGs and
[AppStream metainfo](https://www.freedesktop.org/software/appstream/docs/chap-Metadata.html).
This supplies application metadata to consumers that support it. Some graphical
installers read only package fields or repository catalogs when opening a local
`.deb`; no promise is made that all such installers will display artwork before
installation. File managers may also keep their generic archive icon.

### Untrusted-download warning

The inspected Linux artifact is an unsigned standalone download. The workflow's
release signing requirement applies Authenticode to Windows and Developer ID
signing/notarization to macOS; there is no Linux signing step or APT repository.
Correct `Maintainer` and artwork metadata cannot confer trust.

The likely cause of the reported GUI warning is installation from outside a
trusted APT repository. The exact distribution, installer and warning text were
not supplied, so a browser-origin warning or a loose-desktop-launcher trust
prompt cannot be conclusively excluded. Those are separate mechanisms.

[APT's trust chain](https://manpages.debian.org/testing/apt/apt-secure.8.en.html)
authenticates repository release metadata and package hashes, not ordinary
embedded `.deb` signatures. Adding `dpkg-sig` or a detached signature would not,
by itself, make typical Ubuntu/Debian graphical installers trust a local file.
Checksums from the same release detect changed bytes but provide no independent
publisher authentication. No bypass or unused signing infrastructure was added.

Future options are a maintained signed APT repository with a scoped `Signed-By`
keyring, inclusion in a distribution, or authenticated checksum/provenance
publication for manual verification. Only the former distribution approaches
integrate with normal package-manager repository trust. The release notes now
explicitly distinguish the unsigned Linux download from signed Windows/macOS.

## Implementation and compatibility

- `package-linux.sh`: retain jpackage, extract its output, add desktop resources,
  update installed size, then rebuild with `dpkg-deb --root-owner-group`.
- `linux-desktop.sh`: retain the released desktop ID and launcher path, add the
  window-class mapping, terminate the categories list, install the desktop entry
  under `/usr/share/applications` and existing 16/32/48/64/256 pixel PNGs under
  `/usr/share/icons/hicolor`, and render metainfo under `/usr/share/metainfo`.
  The `/opt` desktop copy remains identical for jpackage's existing xdg hooks;
  both copies use the same ID and produce one menu item.
- `tests/test-linux-packaging.py`: exercise staging and XML escaping; optionally
  extract and inspect the actual final `.deb`, including native validators.
- `package-community.yml`: run the staging tests and final-artifact inspection
  in the Linux job, install native validators, and clarify Linux trust in the
  draft-release notes. Windows/macOS build and signing steps are unchanged.
- `README.md` in this directory: document installation metadata, package
  inspection, artwork limitations and trust accurately.
- Root `pom.xml`: replace the stale personal project URL with the official
  product homepage. Maven coordinates and main-class metadata remain unchanged.

### Branding-leak classification

| Occurrence | Classification / action |
| --- | --- |
| Java package declarations, imports, Maven group IDs, shaded main class, `app.mainclass` | Legitimate internal linkage; retained. |
| `MAIN_CLASS=com.pcariou.generator.Generator` | Technical entry point; retained and used for the WM class association. |
| `MAC_PACKAGE_IDENTIFIER=com.pcariou.sepa.community` | Released bundle identity; retained. Reused as the internal AppStream component ID, never as its public name/publisher. |
| New `StartupWMClass=com-pcariou-generator-Generator` | Internal window matching metadata that prevents fallback public branding; retained. |
| Main-class references in packaging documentation/tests | Technical documentation; retained. |
| Root Maven `http://www.pcariou.com` project URL | Accidental public project metadata; changed to the official homepage. |
| GitHub source URLs using the author's account | Source location, not publisher branding; retained. |
| `Pierre Cariou` in packaging/resources/configuration | No matches found. Legal/source attribution is not rebranded. |

### Windows and macOS sanity audit

Windows still receives the shared `APP_NAME=SEPA Generator Community`,
`VENDOR=Niryosys`, the existing ICO, branded WiX resources, stable upgrade UUID,
and the existing menu/desktop shortcut configuration. No Windows script,
artwork, signing identity or upgrade behavior changed.

macOS still receives `APP_NAME` as the app name, the deliberate short
`MAC_MENU_NAME=SEPA Generator`, and the existing `com.pcariou.sepa.community`
bundle identifier. Its icon is generated from the existing 1024 pixel PNG with
`sips`/`iconutil`; copyright/vendor use Niryosys. Developer ID identity comes from
the existing CI signing configuration, not a hardcoded personal name. No
analogous public-name mistake was found in source configuration, so no macOS
packaging change was made. The actual certificate's displayed identity and
generated `Info.plist` still require a real Mac/package inspection.

## Verification performed

- Published 1.4.0 `.deb`: checksum verified and actual archive/control/desktop/
  launcher configuration/icon content inspected as described above.
- New Linux staging tests: 12 discovered, **10 passed, 2 skipped**. Skips are
  the native Debian artifact check and native desktop/AppStream validators.
  Tests include failure when jpackage's expected layout is missing, repeated
  staging, publisher XML escaping and paths containing spaces.
- `bash -n`: Linux packaging and desktop staging scripts passed.
- Existing packaging suites: **102 checks passed** across branding resources,
  release helpers, workflow triggers and macOS signing plans.
- Windows signing-plan tests: **6 checks passed**.
- `mvn -B verify`: **BUILD SUCCESS**, **311 tests**, zero failures/errors/skips
  (model 67, service 41, view 179, generator 24).

This audit ran on Windows with Git Bash. WSL is not installed and no Linux
container runtime is available. No new native `.deb`, MSI or DMG was built, no
GUI warning was reproduced, and no native desktop/AppStream validator ran
locally. The Linux workflow now validates the actual rebuilt package before
upload; that workflow was not triggered during this task. A fixture test is not
represented as a native packaging or installation test.

## Manual release checks

On Linux (record distribution/version, desktop/session and package installer):

1. Build with `package-linux.sh`, run the `--deb` inspection documented in the
   README, and open the new `.deb` in the graphical installer. Record whether
   the GUI uses the supplied icon/name/publisher and capture any exact warning.
2. Install with `sudo apt install ./<package>.deb`. Verify one Office/Finance
   menu entry named **SEPA Generator Community**, with the expected artwork.
3. Launch from that menu; check dock hover, task switcher, window icon, pinning,
   and relaunch from the pinned icon. Test GNOME and KDE where available, using
   X11 and Wayland/XWayland sessions. On X11, `xprop WM_CLASS` on the running
   window should report `com-pcariou-generator-Generator`.
4. Open settings/help dialogs and generate from demo CSV and Excel files.
5. Upgrade an existing installation and check there is no duplicate menu item;
   unpin/re-pin an old manually created favorite if its cached metadata persists.
6. Remove the package and confirm its desktop entry, metainfo and icon files
   disappear. Confirm reinstall works. Do not disable package trust checks.

On a real Mac, test both Apple Silicon and Intel DMGs: mounted-volume artwork,
drag-to-Applications installation, Finder/Launchpad/Dock icons and names, short
menu-bar name, About publisher, generated `Info.plist` name/icon/copyright and
unchanged bundle ID. For release artifacts verify Developer ID certificate
identity, hardened-runtime signature, notarization/stapling and Gatekeeper on a
fresh downloaded copy. Test upgrade/replacement and one CSV/Excel generation.

Pro needs an explicit reviewed port in its own repository. Shared scripts are
intended to be reusable across editions, but Pro supplies its own identity
properties, workflow and metadata expectations. Port the Linux staging/build
logic, verify the Pro main class/window-class association and desktop ID,
adapt the AppStream description and edition-specific tests, then build/inspect
Pro's actual package. Do not blindly cherry-pick Community expectations or
change either edition's released technical identifiers.
