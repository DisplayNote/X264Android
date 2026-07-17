# Release

There is no CI/CD in this repo (no `.github/workflows/`, no
`azure-pipelines.yml`, no equivalent) — releases are a manual, local
process today. Standing up CI (alongside the automated test suite) is
the **#1 technical-debt priority** for this repo — see
`docs/runbooks/testing.md` — so this manual process is expected to
change.

## Steps

1. Bump `version` in `x264-android/build.gradle` (currently `1.0.1`).
   If you bump it, also update the Maven coordinate reference in
   `README.md` step 6 and `docs/runbooks/local-setup.md` step 6 — these
   are duplicated by design (human quick-start vs. agent runbook), not
   linked, so both need editing.
2. Build the release AAR:
   ```bash
   ./gradlew :x264-android:assembleRelease
   ```
3. Publish, with Artifactory credentials configured
   (`displaynoteDeployerArtifactoryUrl`/`Username`/`Password`, typically
   in `~/.gradle/gradle.properties`):
   ```bash
   ./gradlew :x264-android:artifactoryPublish
   ```
   This task `dependsOn("assembleRelease")` (see
   `x264-android/build.gradle`), so step 2 is technically redundant but
   useful for a pre-publish sanity check.
4. Tag the commit in git (no existing convention in this repo's history
   to follow — pick one, e.g. `v1.0.1`, and record it going forward).

## Before releasing

- Confirm the upstream x264 commit pinned in `README.md` step 3 is still
  the one actually used to produce `prebuilt/<ABI>/lib/libx264.a` in this
  build (there is no automated check tying the two together).
- Re-read the licensing note in `AGENTS.md` — redistributing the AAR
  means redistributing a statically-linked GPLv2+ x264 binary with no
  LICENSE file in this repo. This is a legal question, not an engineering
  one; do not resolve it unilaterally.
