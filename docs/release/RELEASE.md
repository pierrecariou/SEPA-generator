# Community release runbook

The authoritative, step-by-step procedure for publishing a **SEPA Generator
Community** release. It ties together the Maven version authority, the packaging
scripts, the tag-driven GitHub Actions workflow, and the update manifest that
tells installed copies a newer version exists.

Substitute the real version for `X.Y.Z` throughout. There is exactly **one**
production source of truth for the version — the root Maven `<revision>`
property. Never hand-edit a version into installer names, scripts, or the
manifest.

## Update-channel contract

Installed copies poll a small static JSON manifest published at:

```
https://sepa-xml-generator.com/releases/community/latest.json
```

- The running version comes from the Maven-filtered `app.properties`
  (`AppInfo.getVersion()`), never a hard-coded literal.
- The manifest must declare `"edition": "community"`. The checker **rejects**
  (and never offers an update for) a Pro manifest, a missing/unknown edition,
  malformed JSON, a missing/non-numeric version, or a manifest with no usable
  `http(s)` download URL. A rejected manifest is silently ignored — startup and
  generation are never disrupted.
- Because the checker requires `edition: community`, a Community build can never
  offer a Pro artifact or Pro download page.
- All network work is off the Event Dispatch Thread with short timeouts; being
  offline, timing out, or receiving an unexpected HTTP response is non-blocking.
  No payment or transaction data is ever transmitted. Requests use the
  `SEPA-Generator-Community` user agent.

The manifest schema is illustrated by
[`community-latest.json.example`](community-latest.json.example). It is an
example only — the published manifest's `latestVersion` and download URLs are
produced from the release version (below), so the example is not a second
version source.

`highlights` and `releaseNotesUrl` are optional. When `highlights` is present the
update dialog shows at most **five** plain-text lines under "What's new"; a
missing, empty or malformed value simply falls back to the version-only dialog
and never affects update detection. "View full release notes" is offered only
when `releaseNotesUrl` is an absolute `http(s)` URL. Neither field replaces the
release notes bundled with the application, which remain the source of truth.

`releaseNotesUrl` points at the public, version-specific page on the product
website, following the Community version route:

```
https://sepa-xml-generator.com/releases/community/X.Y.Z/
```

For the current release that page is:

```
https://sepa-xml-generator.com/releases/community/1.4.0/
```

That page must be deployed and confirmed reachable **before** the `latest.json`
manifest that references it is published. If the page is not ready, omit
`releaseNotesUrl` entirely rather than publishing a dead link: omitting it only
hides the "View full release notes" link in the update dialog, and the bundled
offline notes reached from **Help → What's new…** remain fully available.

## Release steps

1. **Bump the version.** Edit the root POM `<revision>` to `X.Y.Z`. This is the
   only place the release number is defined; all four modules and the packaging
   scripts derive it via Maven.

2. **Write the release notes** for `X.Y.Z` in
   `view/src/main/resources/help/release-notes-X.Y.Z.html`. Every release version
   must have matching bundled notes: they are the single source of truth for what
   changed, they are shown offline by **Help → What's new…**, and
   `ReleaseNotesAvailabilityTest` fails the build if they are missing. Describe
   only verified, user-visible Community changes; never claim a Pro-only
   capability, and never describe an unsigned artifact as signed. The file is a
   filtered Maven resource, so it must contain no `${...}` or `@token@` sequence.

3. **Run the full test suite** from a clean checkout:

   ```bash
   mvn -B clean verify
   ```

   All modules must pass before going further.

4. **Commit and tag** the exact release commit:

   ```bash
   git commit -am "Community X.Y.Z"
   git tag -a vX.Y.Z -m "Community X.Y.Z"
   git push origin main
   git push origin vX.Y.Z          # pushing the tag triggers the release run
   ```

5. **Build native packages.** Pushing the `vX.Y.Z` tag runs the
   `Package Community` workflow in **final-release** mode: it preflights that the
   tag equals Maven `${revision}`, re-runs the suite, and builds the Windows x64
   MSI, macOS arm64/x64 DMGs, and Linux x64 DEB on native runners. (Use the
   manual `workflow_dispatch` RC mode to smoke-test packaging without releasing.)

6. **Verify signatures / notarization when enabled.** Signing is off by default.
   When the repository variable `COMMUNITY_RELEASE_SIGN=true`, the build fails
   closed if required secrets are missing and verifies signatures during
   packaging. Never describe an unsigned artifact as signed.

   macOS signing uses the Niryosys **Developer ID Application** identity and
   notarizes with an **App Store Connect API key** (secrets
   `MACOS_CERT_P12_BASE64`, `MACOS_CERT_PASSWORD`, `APPLE_API_KEY_P8_BASE64`,
   `APPLE_API_KEY_ID`, `APPLE_API_ISSUER_ID`; variable `MAC_SIGNING_IDENTITY`).
   The macOS job log must show, for **both** arm64 and x64:

   - the signing identity that was used, and its Apple Team ID;
   - `codesign --verify --deep --strict` passing for the `.app`, the native
     launcher and the bundled Java runtime;
   - the `Developer ID Application → Developer ID Certification Authority →
     Apple Root CA` chain, the expected `TeamIdentifier`, and the hardened
     runtime flag;
   - a signed DMG container;
   - `Notarization status: Accepted` with its submission id;
   - `stapler validate` and the `spctl` Gatekeeper assessment passing.

   If notarization is rejected the job prints Apple's full notarization log and
   fails; no artifact is uploaded. Enabling `COMMUNITY_RELEASE_SIGN` is a
   deliberate, separate decision taken only after a signed
   `workflow_dispatch` RC has passed all of the above.

7. **Verify artifact names and checksums.** Confirm the four installers are named
   `SEPA-Generator-Community-X.Y.Z-<os>-<arch>.<ext>` and that the workflow's
   `SHA256SUMS.txt` verifies. Locally a user checks a download with:

   ```bash
   sha256sum -c SHA256SUMS.txt
   ```

8. **Test installation / upgrade** on each platform, in particular that the new
   Windows MSI upgrades a previous Community installation in place.

9. **Review and manually publish the draft.** The workflow creates/updates a
   **draft** GitHub Release (never auto-published, not marked *latest*) with the
   installers, `SHA256SUMS.txt`, and a reviewer checklist. Review it, then click
   **Publish** in the GitHub UI. Publication is the deliberate final action.

10. **Update the website download links** to point at the newly published assets,
    and deploy the version-specific release page at
    `https://sepa-xml-generator.com/releases/community/X.Y.Z/`. Open it and
    confirm it is reachable; a page that is not live must not be referenced.

11. **Publish the update manifest last.** Only after the release notes are
    approved, the release page is public with working download links, and every
    URL the manifest references has been confirmed live, publish the manifest at
    `releases/community/latest.json` with `latestVersion: X.Y.Z` and the final
    asset URLs (shape per the `.example` file). Add `highlights` (at most five
    concise lines), and add `releaseNotesUrl` **only** once
    `https://sepa-xml-generator.com/releases/community/X.Y.Z/` is confirmed
    reachable — otherwise omit the field rather than publishing a dead link.
    Omitting it only hides "View full release notes"; the bundled offline notes
    are unaffected. Publishing the manifest last guarantees it never advertises
    a version whose downloads or referenced pages are not yet live.

12. **Verify discovery from an older install.** Launch a previous Community
    version and confirm it detects `X.Y.Z` and links to the correct download.

## Rollback

- **Bad manifest** (wrong version, dead URL, wrong edition): revert
  `latest.json` to the previous good manifest, or remove it. Clients that fail
  to fetch or that reject an invalid manifest simply keep running on the current
  version — no update is offered, nothing breaks. This is the safest and fastest
  lever because it is a single static file.
- **Bad release artifact** discovered after publishing: unpublish (or delete)
  the GitHub Release and revert the website links first, then fix and re-run.
  Only after a corrected release is public should the manifest point to it again.
- **Botched draft** (before publishing): delete the draft release (and the tag
  if it must change), fix the issue, then re-tag and re-push. Reruns for the same
  tag update the single existing draft rather than creating duplicates; the
  workflow refuses to modify an already-**published** release.
