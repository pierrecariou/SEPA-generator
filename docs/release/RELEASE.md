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

## Release steps

1. **Bump the version.** Edit the root POM `<revision>` to `X.Y.Z`. This is the
   only place the release number is defined; all four modules and the packaging
   scripts derive it via Maven.

2. **Run the full test suite** from a clean checkout:

   ```bash
   mvn -B clean verify
   ```

   All modules must pass before going further.

3. **Commit and tag** the exact release commit:

   ```bash
   git commit -am "Community X.Y.Z"
   git tag -a vX.Y.Z -m "Community X.Y.Z"
   git push origin main
   git push origin vX.Y.Z          # pushing the tag triggers the release run
   ```

4. **Build native packages.** Pushing the `vX.Y.Z` tag runs the
   `Package Community` workflow in **final-release** mode: it preflights that the
   tag equals Maven `${revision}`, re-runs the suite, and builds the Windows x64
   MSI, macOS arm64/x64 DMGs, and Linux x64 DEB on native runners. (Use the
   manual `workflow_dispatch` RC mode to smoke-test packaging without releasing.)

5. **Verify signatures / notarization when enabled.** Signing is off by default.
   When the repository variable `COMMUNITY_RELEASE_SIGN=true`, the build fails
   closed if required secrets are missing and verifies signatures during
   packaging. Never describe an unsigned artifact as signed.

6. **Verify artifact names and checksums.** Confirm the four installers are named
   `SEPA-Generator-Community-X.Y.Z-<os>-<arch>.<ext>` and that the workflow's
   `SHA256SUMS.txt` verifies. Locally a user checks a download with:

   ```bash
   sha256sum -c SHA256SUMS.txt
   ```

7. **Test installation / upgrade** on each platform, in particular that the new
   Windows MSI upgrades a previous Community installation in place.

8. **Review and manually publish the draft.** The workflow creates/updates a
   **draft** GitHub Release (never auto-published, not marked *latest*) with the
   installers, `SHA256SUMS.txt`, and a reviewer checklist. Review it, then click
   **Publish** in the GitHub UI. Publication is the deliberate final action.

9. **Update the website download links** to point at the newly published assets.

10. **Publish the update manifest last.** Only after the release is public and
    the download links work, publish the manifest at
    `releases/community/latest.json` with `latestVersion: X.Y.Z` and the final
    asset URLs (shape per the `.example` file). Publishing it last guarantees the
    manifest never advertises a version whose downloads are not yet live.

11. **Verify discovery from an older install.** Launch a previous Community
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
