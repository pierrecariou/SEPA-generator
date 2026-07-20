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
builds four architecture-specific packages, each on a native runner, by calling
the scripts in this folder (no jpackage/icon logic is duplicated in YAML):

| Artifact                                            | Runner            | Arch label |
| --------------------------------------------------- | ----------------- | ---------- |
| `SEPA-Generator-Community-1.4.0-windows-x64.msi`    | `windows-latest`  | `x64`      |
| `SEPA-Generator-Community-1.4.0-macos-arm64.dmg`    | `macos-26`        | `arm64`    |
| `SEPA-Generator-Community-1.4.0-macos-x64.dmg`      | `macos-26-intel`  | `x64`      |
| `SEPA-Generator-Community-1.4.0-linux-x64.deb`      | `ubuntu-latest`   | `x64`      |

The workflow has **two clearly-separated modes** — a manual **release
candidate** and a tag-driven **final release** that produces a reviewable draft
GitHub Release with checksums. See
[Release workflow (RC & final)](#release-workflow-rc--final) below.

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

### Signing & notarization (optional)

The basic DMG build is **unsigned** and does **not** require an Apple Developer
account. Because it is unsigned, Gatekeeper warns on first launch
("…cannot be opened because the developer cannot be verified"); users must
right-click → **Open**, or allow it under *System Settings → Privacy & Security*.

Signing and notarization are **optional, off by default, and fail-closed**: if
you explicitly request them but required inputs are missing, packaging aborts
rather than producing an artifact mislabeled as signed. See the dedicated
[Signing & notarization](#signing--notarization-optional-cross-platform)
section below for the full environment-variable contract.

Quick reference (see below for the full list of secrets):

```bash
# Sign the .app + bundled runtime with a Developer ID identity already present
# in an available keychain:
MAC_SIGN=true MAC_SIGNING_IDENTITY="Developer ID Application: Your Name (TEAMID)" \
  ./packaging/community/package-macos.sh

# Sign AND notarize + staple (requires Apple credentials):
MAC_SIGN=true MAC_SIGNING_IDENTITY="Developer ID Application: Your Name (TEAMID)" \
MAC_NOTARIZE=true APPLE_ID="you@example.com" APPLE_TEAM_ID="TEAMID" \
APPLE_APP_PASSWORD="app-specific-password" \
  ./packaging/community/package-macos.sh
```

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

### Package signing (not applicable)

DEB **signing is intentionally not implemented**. Repository signing
(`dpkg-sig` / GPG-signed APT `Release` files) is only meaningful when
distributing through an APT repository. The Community release model is direct
download of a standalone `.deb`, whose integrity is covered by the published
**SHA-256 checksums** (added in the release-workflow stage). No Linux signing
system existed previously, so there is nothing to preserve.

---

## Release workflow (RC & final)

The `Package Community` workflow orchestrates the per-platform scripts. It runs
in **two deliberately separated modes** and **never publishes a release
automatically** — final publication is always a manual action.

### Mode 1 — Release candidate (manual)

Trigger: **Actions → Package Community → Run workflow** (`workflow_dispatch`).

- Builds all four installers on native runners and uploads them as **workflow
  artifacts** (downloadable from the run page).
- **Creates no GitHub Release** and generates no checksums file.
- Unsigned by default. Tick the **`sign`** input to attempt a signed build (only
  succeeds if the signing secrets are configured; otherwise it fails closed).

Use RC builds to smoke-test packaging on any branch without touching releases.

### Mode 2 — Final release (tag)

Trigger: pushing an annotated **`vX.Y.Z`** tag whose version exactly matches
Maven's `${revision}` (currently `1.4.0`).

```bash
# From a clean, green checkout of the exact release commit:
mvn clean verify                     # full suite must pass
git tag -a v1.4.0 -m "Community 1.4.0"
git push origin v1.4.0               # <-- this triggers the release run
```

The run then:

1. **Preflight** — resolves the Maven version, asserts the tag matches it
   (mismatch fails **before** any packaging), and runs the full test suite.
2. **Build** — produces the four installers from the immutable tagged commit.
3. **Release** — downloads the four artifacts, writes **`SHA256SUMS.txt`**
   (after any signing/notarization), **verifies** it, and creates/updates a
   **draft** GitHub Release titled `SEPA Generator Community <version>` with the
   installers, the checksum file, and a reviewer checklist.

The draft is **not** published and **not** marked *latest*. A maintainer reviews
it (see the checklist in the generated notes) and clicks **Publish** manually.

Ordinary pushes and pull requests do **not** trigger this workflow, so secrets
are never exposed to forks.

### Version contract

The tag version and Maven `${revision}` must be identical. To ship a new
version, bump the Maven `<revision>` (root POM) — never edit installer names or
script versions by hand — then tag the matching `vX.Y.Z`.

### Checksums

`SHA256SUMS.txt` lists one SHA-256 per distributed installer (basenames only)
and is verified in CI before the draft is created. Users verify a download with:

```bash
sha256sum -c SHA256SUMS.txt          # from the folder containing the installers
```

### Signing in the release

Signing is a **separate, deliberate release gate** (see the next section):

- **RC:** the `sign` input (default off).
- **Final release:** the repository variable `COMMUNITY_RELEASE_SIGN` (`true`
  to enable; default off). When enabled, the build **fails closed** if the
  required signing secrets are missing, and signatures are verified during
  packaging. Unsigned artifacts are never described as signed.

### Rerun / retry behavior

- Reruns for the same tag **update the single existing draft** (assets are
  re-uploaded with `--clobber`) rather than creating duplicates; a `concurrency`
  group serializes runs for the same ref.
- If a release for the tag already exists and is **published**, the workflow
  **refuses to modify it** and fails — publish is final.
- To redo a botched draft: delete the draft release (and, if needed, the tag),
  fix the issue, then re-tag/re-push.

### Least-privilege permissions

The workflow default is `contents: read`. Only the `release` job is granted
`contents: write` (to create the draft), using the built-in `GITHUB_TOKEN`.

### Secondary artifacts

Community ships **only** the four native installers above (MSI, two DMGs, DEB).
There is **no** portable ZIP, standalone JAR, RPM, AppImage, or ARM Linux
deliverable, and none is removed by this change — none existed. If a secondary
artifact is added later, add it to `expected_artifacts` in
[`release-lib.sh`](release/release-lib.sh) so completeness/checksum checks cover
it.

### Update manifest & full runbook

Publishing a release also involves the **update manifest** that tells installed
copies a newer version exists. The complete, ordered procedure — version bump,
tests, tag, build, verify, manual publish, website links, and publishing the
manifest last — is the [Community release runbook](../../docs/release/RELEASE.md).
The manifest schema (edition-safe, `"edition": "community"`) is illustrated by
[`community-latest.json.example`](../../docs/release/community-latest.json.example).

---

## Signing & notarization (optional, cross-platform)

Signing is **infrastructure only**: the hooks below let a signed/notarized build
happen when credentials are supplied, but their presence does **not** mean any
current artifact is signed. Unsigned development builds always work with no
secrets.

Key properties:

- **Disabled by default.** A normal build (and any ordinary pull request) needs
  no secrets and produces an unsigned installer.
- **Explicit opt-in.** Signing activates only when you set the enable flag
  (`WINDOWS_SIGN=true` / `-Sign` on Windows, `MAC_SIGN=true` on macOS).
- **Fail-closed.** If you request signing but a required input is missing,
  packaging **aborts** instead of emitting an unsigned artifact under a "signed"
  label. A single stray partial credential can never silently switch signing on.
- **No secret leakage.** Passwords are never echoed; command previews redact
  them. Temporary certificates/keychains are always cleaned up.

> Enabling these hooks is a **separate release gate**. Actually signing,
> notarizing, and verifying artifacts is decided and performed outside these
> packaging steps. This document does **not** claim any artifact is signed.

### Windows (Authenticode)

| Variable                  | Secret? | Purpose                                                        |
| ------------------------- | ------- | -------------------------------------------------------------- |
| `WINDOWS_SIGN` / `-Sign`  | no      | Enable signing (`true`). Off by default.                       |
| `WINDOWS_CERT_PFX_BASE64` | **yes** | Base64 of the code-signing `.pfx` (PKCS#12).                   |
| `WINDOWS_CERT_PASSWORD`   | **yes** | Password for the `.pfx`.                                       |
| `WINDOWS_TIMESTAMP_URL`   | no      | RFC3161 timestamp URL. Default `http://timestamp.digicert.com`.|

Requires `signtool.exe` (Windows SDK). The MSI is signed with
`/fd SHA256 /tr <url> /td SHA256` and then **verified** (`signtool verify /pa`);
verification failure aborts packaging. The decoded `.pfx` is written to a unique
temp file that is always deleted.

```powershell
$env:WINDOWS_CERT_PFX_BASE64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("cert.pfx"))
$env:WINDOWS_CERT_PASSWORD   = "…"
.\packaging\community\package-windows.ps1 -Sign
```

### macOS (Developer ID + notarization)

| Variable               | Secret? | Purpose                                                            |
| ---------------------- | ------- | ------------------------------------------------------------------ |
| `MAC_SIGN`             | no      | Enable Developer ID signing (`true`). Off by default.              |
| `MAC_SIGNING_IDENTITY` | no*     | e.g. `Developer ID Application: Name (TEAMID)`. Required if signing.|
| `MACOS_CERT_P12_BASE64`| **yes** | Optional: base64 `.p12` imported into a temporary keychain.        |
| `MACOS_CERT_PASSWORD`  | **yes** | Password for the `.p12` (required if `MACOS_CERT_P12_BASE64` set). |
| `MAC_NOTARIZE`         | no      | Enable notarization + stapling (`true`). Requires `MAC_SIGN=true`. |
| `APPLE_ID`             | **yes** | Apple ID for `notarytool`. Required if notarizing.                 |
| `APPLE_TEAM_ID`        | **yes** | Apple Developer Team ID. Required if notarizing.                   |
| `APPLE_APP_PASSWORD`   | **yes** | App-specific password for `notarytool`. Required if notarizing.    |

\*Not a secret, but signing fails closed if it is empty.

`jpackage` signs the `.app` and its bundled Java runtime, applying the
hardened-runtime entitlements in `packaging/macos/entitlements.plist`. When
`MACOS_CERT_P12_BASE64` is supplied the certificate is imported into a
**temporary keychain** that is removed on exit; otherwise the identity is taken
from an existing keychain. Notarization requires signing — `notarytool submit
--wait`, `stapler staple`, `stapler validate` and a `spctl` Gatekeeper check all
must pass or packaging aborts.

### Local vs GitHub Actions

These variables can be exported locally for a one-off signed build. In CI they
are wired as encrypted **GitHub Actions secrets/variables** consumed by the
`Package Community` release workflow (see
[Release workflow](#release-workflow-rc--final)):

| Name                                                | Kind     | Used by            |
| --------------------------------------------------- | -------- | ------------------ |
| `WINDOWS_CERT_PFX_BASE64`, `WINDOWS_CERT_PASSWORD`  | secret   | Windows job        |
| `WINDOWS_TIMESTAMP_URL`                             | variable | Windows job (opt.) |
| `MACOS_CERT_P12_BASE64`, `MACOS_CERT_PASSWORD`      | secret   | macOS jobs         |
| `APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_PASSWORD`   | secret   | macOS jobs         |
| `MAC_SIGNING_IDENTITY`                              | variable | macOS jobs         |
| `COMMUNITY_RELEASE_SIGN`                            | variable | enables tag-release signing |

Signing stays **off** unless explicitly enabled (RC `sign` input, or the
`COMMUNITY_RELEASE_SIGN` variable for tag releases). No secret is stored in the
repository, and no placeholder credential is committed. Because the workflow
never runs on pull requests, secrets are not exposed to forks.

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
