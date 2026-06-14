# Packaging — SEPA Generator Community Edition

This folder contains the **desktop packaging workflows** for the Community
Edition. They produce real, native installers using the official JDK
[`jpackage`](https://docs.oracle.com/en/java/javase/23/docs/specs/man/jpackage.html)
tool. Maven remains the main build tool; `jpackage` only wraps the built
application into a platform installer.

| Platform | Script               | Output                                              |
| -------- | -------------------- | --------------------------------------------------- |
| Windows  | `package-windows.ps1`| `dist/SEPA-Generator-Community-1.3.0-windows.msi`   |
| macOS    | `package-macos.sh`   | `dist/SEPA-Generator-Community-1.3.0-macos.dmg`     |

> Each installer **bundles its own Java runtime** — end users do **not** need to
> install Java separately. Linux packaging will be handled later, in its own
> script.

Shared configuration (app name, version, vendor, main JAR, icon, output dir,
package name) is centralized near the top of each script.

| Setting     | Value                             |
| ----------- | --------------------------------- |
| App name    | `SEPA Generator Community`        |
| Version     | `1.3.0`                           |
| Vendor      | `Pierre Cariou`                   |
| Main JAR    | `generator-1.3.0.jar` (shaded)    |
| Main class  | `com.pcariou.generator.Generator` |
| Output dir  | `dist/`                           |

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
```

The installer is written to `dist\SEPA-Generator-Community-1.3.0-windows.msi`.

### Test install / uninstall

1. **Install:** double-click the MSI and follow the wizard.
2. **Launch:** from the **Start Menu** group *SEPA Generator Community* or the
   **Desktop shortcut**. Confirm the correct icon appears.
3. **Verify registration:** *Settings → Apps → Installed apps* should list
   *SEPA Generator Community 1.3.0* by *Pierre Cariou*.
4. **Uninstall:** from *Installed apps*, or run
   `msiexec /x dist\SEPA-Generator-Community-1.3.0-windows.msi`.

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

# Local/test DMG that allows jpackage's default icon (no icon required)
RELEASE=false ./packaging/community/package-macos.sh
```

The DMG is written to `dist/SEPA-Generator-Community-1.3.0-macos.dmg`.

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
- **`Maven build failed`** — run `mvn clean package` directly to see the error.
