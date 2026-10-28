# Release

`ci/azure-pipelines.yml` (Azure DevOps) runs the JVM tests with coverage
and `assembleRelease` on every `master` push and on every non-draft PR
except those that only change `docs/` or `README.md`, and publishes with
`artifactoryPublish` when a bare `X.Y.Z` tag is pushed (like the existing
`1.0.1` tag). A tag in any other format skips the publish stage instead of
failing it. The pipeline checks the tag's format only, not its value:
`artifactoryPublish` publishes the `version` in `x264-android/build.gradle`,
so tag only a commit whose `version` equals the tag. The manual steps below
remain the fallback and describe what the pipeline does.

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
4. Tag the commit in git with the bare version (`git tag 1.0.1`, the
   convention of the existing tag) and push it; the pipeline publishes
   from the tag build.

## Before releasing

- Confirm the upstream x264 commit pinned in `README.md` step 3 is still
  the one actually used to produce `prebuilt/<ABI>/lib/libx264.a` in this
  build (there is no automated check tying the two together).
- Re-read the licensing note in `AGENTS.md` — redistributing the AAR
  means redistributing a statically-linked GPLv2+ x264 binary with no
  LICENSE file in this repo. This is a legal question, not an engineering
  one; do not resolve it unilaterally.
