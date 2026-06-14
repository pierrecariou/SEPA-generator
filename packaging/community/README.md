# Packaging — SEPA Generator Community Edition (Windows)

This folder contains the **Windows packaging workflow** for the Community Edition.
It produces a real Windows installer (MSI) using the official JDK
[`jpackage`](https://docs.oracle.com/en/java/javase/23/docs/specs/man/jpackage.html)
tool. Maven remains the main build tool; `jpackage` only wraps the built
application into an installer.

> macOS and Linux packaging will be handled later, in their own scripts.

## What you get

The installer:

- installs **SEPA Generator Community** under `Program Files`,
- **bundles its own Java runtime** — end users do **not** need a separate JRE/JDK,
- registers the app in Windows **Apps & features** (installed apps),
- creates a **Start Menu** entry,
- creates a **Desktop shortcut**,
- uses the application **`.ico`** icon,
- can be **uninstalled cleanly**.

## Prerequisites

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

## How to run

From the repository root (or anywhere — paths are resolved relative to the
script):

```powershell
# Build the app with Maven and create the MSI (default)
powershell -ExecutionPolicy Bypass -File packaging\community\package-windows.ps1
```

Useful options:

```powershell
# Reuse an existing Maven build (skip 'mvn clean package')
.\packaging\community\package-windows.ps1 -SkipBuild

# Optional later target: a Windows .exe installer
.\packaging\community\package-windows.ps1 -Type exe

# Smoke-test packaging WITHOUT WiX: an unpacked app folder (no installer)
.\packaging\community\package-windows.ps1 -Type app-image
```

## Output

The final installer is written to the repository `dist/` folder:

```
dist\SEPA-Generator-Community-1.3.0-windows.msi
```

(`-Type exe` produces `...-windows.exe`; `-Type app-image` produces an unpacked
`...-windows-app-image\` folder for local testing only.)

## Configuration

All settings are centralized at the top of `package-windows.ps1`:

| Setting        | Value                              |
| -------------- | ---------------------------------- |
| App name       | `SEPA Generator Community`         |
| Version        | `1.3.0`                            |
| Vendor         | `Pierre Cariou`                    |
| Main JAR       | `generator-1.3.0.jar` (shaded)     |
| Main class     | `com.pcariou.generator.Generator`  |
| Icon           | `packaging\windows\sepa-generator.ico` |
| Output dir     | `dist\`                            |
| Upgrade UUID   | stable GUID (keep constant across releases) |

The **Upgrade UUID** must stay constant between releases so Windows treats new
versions as in-place upgrades and uninstalls cleanly.

## Test install / uninstall

1. **Install:** double-click the generated MSI and follow the wizard (you can
   choose the install directory).
2. **Launch:** open it from the **Start Menu** (group *SEPA Generator
   Community*) or the **Desktop shortcut**. Confirm the correct icon appears.
3. **Verify registration:** open *Settings → Apps → Installed apps* and confirm
   *SEPA Generator Community 1.3.0* by *Pierre Cariou* is listed.
4. **Uninstall:** from *Installed apps* choose **Uninstall**, or run:

   ```powershell
   msiexec /x dist\SEPA-Generator-Community-1.3.0-windows.msi
   ```

   Confirm the Start Menu entry, Desktop shortcut, and install folder are removed.

## Troubleshooting

- **`jpackage was not found`** — install a JDK 17+ and set `JAVA_HOME` or add
  `<jdk>\bin` to `PATH`.
- **`WiX Toolset was not found`** — install WiX (see *Prerequisites*) and open a
  new shell. This is the only blocker for MSI/EXE generation; the script never
  silently falls back to a ZIP.
- **`Maven build failed`** — run `mvn clean package` directly to see the error.
