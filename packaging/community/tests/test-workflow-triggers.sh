#!/usr/bin/env bash
#
# Static structural test for .github/workflows/package-community.yml.
# Verifies the trigger configuration without running the workflow or needing
# a GitHub runner. Uses Python's PyYAML (available in GitHub Actions and in the
# local dev environment used for the other packaging tests).
#
# Run: bash packaging/community/tests/test-workflow-triggers.sh
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKFLOW="${HERE}/../../../.github/workflows/package-community.yml"

fails=0
pass()  { printf 'PASS: %s\n' "$1"; }
failc() { printf 'FAIL: %s\n' "$1"; fails=$((fails + 1)); }

# Require PyYAML for robust parsing (no fragile line-by-line grep).
# Try ${PYTHON} first, then python (Windows environments typically have python).
PYTHON=""
for py in ${PYTHON} python; do
  if command -v "${py}" >/dev/null 2>&1 && "${py}" -c "import yaml" 2>/dev/null; then
    PYTHON="${py}"; break
  fi
done
if [ -z "${PYTHON}" ]; then
  echo "SKIP: PyYAML not available; cannot run static workflow tests." >&2
  exit 0
fi

if [ ! -f "${WORKFLOW}" ]; then
  printf 'FAIL: workflow file not found: %s\n' "${WORKFLOW}"
  exit 1
fi

# ---------------------------------------------------------------------------
# Extract the tag trigger patterns from the YAML.
# ---------------------------------------------------------------------------
TAG_PATTERNS="$(${PYTHON} - "${WORKFLOW}" <<'PYEOF'
import sys, yaml
d = yaml.safe_load(open(sys.argv[1], encoding='utf-8'))
on = d.get('on') or d.get(True, {})
tags = (on.get('push') or {}).get('tags', [])
for t in tags:
    print(t)
PYEOF
)"

# 1. The glob pattern 'v*' must be present (triggers v1.4.0, v2.0.0, etc.).
if printf '%s\n' "${TAG_PATTERNS}" | grep -qxF 'v*'; then
  pass "tag trigger contains 'v*' (matches versioned tags)"
else
  failc "tag trigger must contain 'v*'; got: $(printf '%s' "${TAG_PATTERNS}" | tr '\n' ' ')"
fi

# 2. The bare literal 'v' must NOT be the only or primary pattern — it would
#    match a tag named literally 'v' and NOT match 'v1.4.0'.
if printf '%s\n' "${TAG_PATTERNS}" | grep -qxF 'v'; then
  failc "bare literal 'v' is present in tag triggers (only matches a tag named 'v', not v1.4.0)"
else
  pass "no bare literal 'v' in tag triggers"
fi

# 3. Exactly one tag pattern is configured (keeps the trigger minimal/readable).
PATTERN_COUNT="$(printf '%s\n' "${TAG_PATTERNS}" | grep -c '.')"
if [ "${PATTERN_COUNT}" -eq 1 ]; then
  pass "exactly one tag trigger pattern (minimal and readable)"
else
  failc "expected exactly one tag trigger pattern; got ${PATTERN_COUNT}: $(printf '%s' "${TAG_PATTERNS}" | tr '\n' ' ')"
fi

# ---------------------------------------------------------------------------
# Verify the release job fires only when is_release is true (tag mode).
# ---------------------------------------------------------------------------
RELEASE_IF="$(${PYTHON} - "${WORKFLOW}" <<'PYEOF'
import sys, yaml
d = yaml.safe_load(open(sys.argv[1], encoding='utf-8'))
print(d['jobs']['release'].get('if', ''))
PYEOF
)"

if printf '%s' "${RELEASE_IF}" | grep -q 'is_release'; then
  pass "release job is conditioned on 'is_release' output"
else
  failc "release job should be guarded by 'is_release' output; got: ${RELEASE_IF}"
fi

# ---------------------------------------------------------------------------
# Confirm --verify-tag and --latest=false are present in the release create cmd.
# ---------------------------------------------------------------------------
RELEASE_STEP_RUN="$(${PYTHON} - "${WORKFLOW}" <<'PYEOF'
import sys, yaml
d = yaml.safe_load(open(sys.argv[1], encoding='utf-8'))
for step in d['jobs']['release']['steps']:
    if 'gh release create' in (step.get('run') or ''):
        print(step['run'])
        break
PYEOF
)"

if printf '%s' "${RELEASE_STEP_RUN}" | grep -q -- '--verify-tag'; then
  pass "gh release create uses --verify-tag"
else
  failc "gh release create is missing --verify-tag"
fi

if printf '%s' "${RELEASE_STEP_RUN}" | grep -q -- '--latest=false'; then
  pass "gh release create uses --latest=false"
else
  failc "gh release create is missing --latest=false"
fi

if printf '%s' "${RELEASE_STEP_RUN}" | grep -q -- '--draft'; then
  pass "gh release create uses --draft"
else
  failc "gh release create is missing --draft"
fi

# ---------------------------------------------------------------------------
# Confirm gh release edit also sets --latest=false (rerun path).
# ---------------------------------------------------------------------------
EDIT_STEP_RUN="$(${PYTHON} - "${WORKFLOW}" <<'PYEOF'
import sys, yaml
d = yaml.safe_load(open(sys.argv[1], encoding='utf-8'))
for step in d['jobs']['release']['steps']:
    if 'gh release edit' in (step.get('run') or ''):
        print(step['run'])
        break
PYEOF
)"

if printf '%s' "${EDIT_STEP_RUN}" | grep -q -- '--latest=false'; then
  pass "gh release edit uses --latest=false (rerun path)"
else
  failc "gh release edit is missing --latest=false (rerun path)"
fi

# ---------------------------------------------------------------------------
# Confirm ordinary pushes cannot trigger (no branch filter set).
# ---------------------------------------------------------------------------
BRANCH_TRIGGERS="$(${PYTHON} - "${WORKFLOW}" <<'PYEOF'
import sys, yaml
d = yaml.safe_load(open(sys.argv[1], encoding='utf-8'))
on = d.get('on') or d.get(True, {})
branches = (on.get('push') or {}).get('branches', [])
for b in branches:
    print(b)
PYEOF
)"

if [ -z "${BRANCH_TRIGGERS}" ]; then
  pass "no push-branch trigger (ordinary commits do not build releases)"
else
  failc "unexpected push-branch trigger found: ${BRANCH_TRIGGERS}"
fi

if [ "${fails}" -gt 0 ]; then
  printf '%d workflow-trigger test(s) failed.\n' "${fails}"
  exit 1
fi
printf 'All workflow-trigger tests passed.\n'
