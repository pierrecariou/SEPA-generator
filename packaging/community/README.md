# Packaging — SEPA Generator Community Edition

This folder contains the **desktop packaging workflows** for the Community
Edition. They produce real, native installers using the official JDK
[`jpackage`](https://docs.oracle.com/en/java/javase/23/docs/specs/man/jpackage.html)
tool. Maven remains the main build tool; `jpackage` only wraps the built
application into a platform installer.

| Platform | Script               | Output (CI release artifact)                              |
| -------- | -------------------- | --------------------------------------------------------- |
| Windows  | `package-windows.ps1`| `dist/SEPA-Generator-Community-1.4.0-windows-x64.msi`     |
| macOS    | `package-macos.sh`   | `dist/SEPA-Generator-Community-1.4.0-macos-arm64.dmg`     |
| macOS    | `package-macos.sh`   | `dist/SEPA-Generator-Community-1.4.0-macos-x64.dmg`       |
| Linux    | `package-linux.sh`   | `dist/SEPA-Generator-Community-1.4.0-linux-x64.deb`       |

> Each installer **bundles its own Java runtime** — end users do **not** need to
> install Java separately. The Linux `.deb` targets **Debian/Ubuntu-compatible**
> distributions (anything using `dpkg`/`apt`).

### Final v1.4.0 release artifacts

The [`Package Community`](../../.github/workflows/package-community.yml) workflow
(manual `workflow_dispatch`) builds four architecture-specific packages, each on
a native runner, by calling the scripts in this folder (no jpackage/icon logic is
duplicated in YAML):

| Artifact                                            | Runner            | Arch label |
| --------------------------------------------------- | ----------------- | ---------- |
| `SEPA-Generator-Community-1.4.0-windows-x64.msi`    | `windows-latest`  | `x64`      |
| `SEPA-Generator-Community-1.4.0-macos-arm64.dmg`    | `macos-26`        | `arm64`    |
| `SEPA-Generator-Community-1.4.0-macos-x64.dmg`      | `macos-26-intel`  | `x64`      |
| `SEPA-Generator-Community-1.4.0-linux-x64.deb`      | `ubuntu-latest`   | `x64`      |

Shared packaging identity (app name, artifact slug, vendor, description, main
class, and the permanent Windows/macOS/Linux package identifiers) lives in a
single [`packaging/edition.properties`](../edition.properties) file that all
three scripts read. The **application version is not hardcoded**: each script
derives it from the authoritative Maven module version (the `generator` module,
via `help:evaluate`), so package metadata, filenames and the in-app version can
never drift. Bump the release version by editing the Maven POMs only.

| Setting     | Value                             | Source                      |
| ----------- | --------------------------------- | --------------------------- |
| App name    | `SEPA Generator Community`        | `edition.properties`        |
| Version     | `1.4.0`                           | Maven (`generator` module)  |
| Vendor      | `Niryosys`                        | `edition.properties`        |
| Main JAR    | `generator-1.4.0.jar` (shaded)    | Maven (`generator` module)  |
| Main class  | `com.pcariou.generator.Generator` | `edition.properties`        |
| Output dir  | `dist/`                           | script (constant)           |

---

## Windows (MSI)

Produces a real Windows installer that installs under `Program Files`, registers
the app in **Apps & features**, and creates **Start Menu** and **Desktop**
shortcuts using the `.ico` icon. It can be uninstalled cleanly.

### Prerequisites

1. **JDK 17 or newer** (this project builds with **JDK 23**). `jpackage` ships
   with the JDK. The script finds it via `JAVA_HOME`, then `PATH`, then common
   JDK install locations.
2. **Apache Maven** on `PATH` (`mvn`).
3. **WiX Toolset** — required by `jpackage` to build Windows **MSI/EXE**
   installers. The script verifies it and stops with instructions if missing.
   Install one of:

   ```powershell
   # WiX 4/5 (recommended)
   winget install --id WiXToolset.WiX
   # or
   dotnet tool install --global wix

   # Classic WiX 3.x (also supported by jpackage)
   winget install --id WiXToolset.WiXToolset
   ```

   Open a new shell afterwards so `PATH` is refreshed.

### How to run

```powershell
# Build the app with Maven and create the MSI (default)
powershell -ExecutionPolicy Bypass -File packaging\community\package-windows.ps1

# Reuse an existing Maven build (skip 'mvn clean package')
.\packaging\community\package-windows.ps1 -SkipBuild

# Optional later target: a Windows .exe installer
.\packaging\community\package-windows.ps1 -Type exe

# Smoke-test packaging WITHOUT WiX: an unpacked app folder (no installer)
.\packaging\community\package-windows.ps1 -Type app-image

# Override the architecture tag in the artifact name (default: x64)
.\packaging\community\package-windows.ps1 -ArchLabel x64
```

The installer is written to
`dist\SEPA-Generator-Community-1.4.0-windows-x64.msi`.

### Test install / uninstall

1. **Install:** double-click the MSI and follow the wizard.
2. **Launch:** from the **Start Menu** group *SEPA Generator Community* or the
   **Desktop shortcut**. Confirm the correct icon appears.
3. **Verify registration:** *Settings → Apps → Installed apps* should list
   *SEPA Generator Community 1.4.0* by *Niryosys*.
4. **Uninstall:** from *Installed apps*, or run
   `msiexec /x dist\SEPA-Generator-Community-1.4.0-windows-x64.msi`.

---

## macOS (DMG)

Produces a `.dmg` containing a launchable **`SEPA Generator Community.app`** with
a bundled Java runtime. Users drag the app into `/Applications` and launch it
from Launchpad or Finder.

### Prerequisites

- A **macOS machine** (or a **macOS CI runner**). macOS DMGs can only be built on
  macOS — `jpackage --type dmg` relies on native macOS tooling.
- **JDK 17 or newer** with `jpackage` (this project builds with **JDK 23**). The
  script finds it via `JAVA_HOME`, then `PATH`, then `/usr/libexec/java_home`.
- **Apache Maven** on `PATH` (`mvn`).

### Icon (automatic)

The script resolves the macOS icon in this order:

1. **Prefer** a ready-made `packaging/macos/sepa-generator.icns`.
2. **Otherwise**, if `packaging/macos/sepa-generator-1024.png` exists (a
   1024×1024 source PNG), the script **automatically generates** the `.icns`
   using the native macOS tools `sips` + `iconutil`. Generated files are written
   to the ignored build folder `target/packaging/macos/` (nothing is committed).

For **release** packaging (the default, `RELEASE=true`) the build **fails with a
clear error** if neither the `.icns` nor the source PNG is present — a release
DMG must ship with a real icon. For local testing you can set `RELEASE=false` to
allow jpackage's default icon instead.

To provide an icon, drop a 1024×1024 PNG at
`packaging/macos/sepa-generator-1024.png` and re-run the script — no manual
`sips`/`iconutil` steps are needed.

### How to run

```bash
# Build the app with Maven and create the DMG
./packaging/community/package-macos.sh

# Reuse an existing Maven build (skip 'mvn clean package')
SKIP_BUILD=true ./packaging/community/package-macos.sh

# Tag the architecture in the artifact name (used by CI for arm64 / x64).
# Without ARCH_LABEL the canonical name ...-macos.dmg is produced.
ARCH_LABEL=arm64 ./packaging/community/package-macos.sh

# Local/test DMG that allows jpackage's default icon (no icon required)
RELEASE=false ./packaging/community/package-macos.sh
```

By default the DMG is written to
`dist/SEPA-Generator-Community-1.4.0-macos.dmg`. With `ARCH_LABEL` set, the name
becomes `…-macos-arm64.dmg` or `…-macos-x64.dmg`. The CI workflow builds both:
`arm64` on `macos-26` (Apple Silicon) and `x64` on `macos-26-intel`.

### Test the .app

1. Double-click the DMG to mount it.
2. Drag **SEPA Generator Community** into **Applications**.
3. Launch it from Launchpad/Finder and confirm it opens (no separate Java
   install needed) and shows the correct icon.

### Signing & notarization (future step)

The basic DMG build is **unsigned** and does **not** require an Apple Developer
account. Because it is unsigned, Gatekeeper warns on first launch
("…cannot be opened because the developer cannot be verified"); users must
right-click → **Open**, or allow it under *System Settings → Privacy & Security*.

Signing is cleanly optional and **off by default**. To enable it later:

```bash
SIGN=true MAC_SIGNING_IDENTITY="Developer ID Application: Your Name (TEAMID)" \
  ./packaging/community/package-macos.sh
```

Full **notarization** (`xcrun notarytool` + stapling) is a separate, future step
for a more professional macOS distribution and is intentionally not performed by
this script yet.

---

## Linux (DEB)

Produces a Debian package (`.deb`) for **Debian/Ubuntu-compatible** distributions
(anything using `dpkg`/`apt`). It installs a launchable application with a bundled
Java runtime and registers a desktop/menu entry using the PNG icon. The package
can be removed cleanly with the system package manager.

### Prerequisites

- A **Linux machine** (or a **Linux CI runner**). The `.deb` is built with
  `jpackage --type deb`, which relies on native Debian tooling.
- **JDK 17 or newer** with `jpackage` (this project builds with **JDK 23**). The
  script finds it via `JAVA_HOME`, then `PATH`.
- **Apache Maven** on `PATH` (`mvn`).
- **Debian packaging tools**: `dpkg-deb` (from the `dpkg` package, preinstalled on
  Debian/Ubuntu) and `fakeroot`. The script verifies both and stops with
  instructions if either is missing. Install with:

  ```bash
  sudo apt-get update && sudo apt-get install -y fakeroot
  ```

### Icon

Linux packaging uses a PNG icon, resolved in this order:

1. **Prefer** `packaging/linux/sepa-generator.png`.
2. **Otherwise** fall back to the shared 1024×1024 source PNG
   `packaging/macos/sepa-generator-1024.png` (jpackage scales it for the desktop
   entry).

For **release** packaging (the default, `RELEASE=true`) the build **fails** if no
PNG is found. Set `RELEASE=false` for a local build with jpackage's default icon.

### How to run

```bash
# Build the app with Maven and create the .deb
./packaging/community/package-linux.sh

# Reuse an existing Maven build (skip 'mvn clean package')
SKIP_BUILD=true ./packaging/community/package-linux.sh

# Local/test .deb that allows jpackage's default icon (no icon required)
RELEASE=false ./packaging/community/package-linux.sh
```

The package is written to
`dist/SEPA-Generator-Community-1.4.0-linux-x64.deb`.

### Package metadata

| Setting           | Value                                |
| ----------------- | ------------------------------------ |
| Package name      | `sepa-generator-community`           |
| Menu categories   | `Office;Finance`                     |
| Debian section    | `utils`                              |
| Maintainer        | `contact@sepa-xml-generator.com`     |

### Test install / uninstall

```bash
# Install
sudo apt-get install -y ./dist/SEPA-Generator-Community-1.4.0-linux-x64.deb
#   - or -
sudo dpkg -i ./dist/SEPA-Generator-Community-1.4.0-linux-x64.deb

# Launch from the application menu (Office/Finance), then uninstall:
sudo apt-get remove -y sepa-generator-community
#   - or -
sudo dpkg -r sepa-generator-community
```

---

## Troubleshooting

- **`jpackage was not found`** — install a JDK 17+ and set `JAVA_HOME` or add
  `<jdk>/bin` (Windows: `<jdk>\bin`) to `PATH`.
- **`WiX Toolset was not found`** (Windows) — install WiX (see above) and open a
  new shell. This is the only blocker for MSI/EXE generation; the script never
  silently falls back to a ZIP.
- **macOS script refuses to run on Windows/Linux** — DMGs must be built on macOS.
- **`No macOS icon available for release packaging`** — add
  `packaging/macos/sepa-generator.icns` or a 1024×1024
  `packaging/macos/sepa-generator-1024.png`, or run with `RELEASE=false` for a
  local build with the default icon.
- **`Missing Debian packaging tool(s)`** (Linux) — install `fakeroot` (and ensure
  `dpkg` is present): `sudo apt-get install -y fakeroot`. These are jpackage
  prerequisites for `.deb`, not Maven dependencies.
- **Linux script refuses to run on Windows/macOS** — `.deb` packages must be built
  on Linux.
- **`No Linux icon available for release packaging`** — add
  `packaging/linux/sepa-generator.png` (or the shared
  `packaging/macos/sepa-generator-1024.png`), or run with `RELEASE=false`.
- **`Maven build failed`** — run `mvn clean package` directly to see the error.
