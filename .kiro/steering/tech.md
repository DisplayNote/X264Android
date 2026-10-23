# Tech stack

- Gradle `7.5.1` (wrapper) / Android Gradle Plugin `7.4.2`.
- `ndk-build` (`Android.mk` / `Application.mk`) — **not** CMake; no
  `CMakeLists.txt` exists anywhere in the repo.
- Android NDK `26.3.11579264` (pinned in `x264-android/build.gradle`).
- `compileSdkVersion 36`, `minSdkVersion 26` (Montage requirement),
  `targetSdkVersion 35` — a settled SDK triple, not an open question.
- **Known risk**: root `build.gradle` still pins Android Gradle Plugin
  `7.4.2`, which predates official support for `compileSdk 36` — verify
  locally before relying on a from-scratch build (see the AGP gotcha in
  `AGENTS.md`).
- Native `APP_PLATFORM := android-21` — below `minSdkVersion`, so no
  runtime mismatch, but don't lower it without re-checking
  `stdio_compat.c`'s API-level shim.
- JVM unit tests (JUnit 4, `src/test`) for the Java classes; no
  native or instrumented tests yet (technical-debt recovery item, see
  `docs/runbooks/testing.md`).
- Build CI: `ci/azure-pipelines.yml` (Azure DevOps) — scans, JVM tests +
  JaCoCo, `assembleRelease`, publish on version tags.
- Optional publishing via Artifactory (`com.jfrog.artifactory` plugin).

Build commands, environment variables, and the full cross-compilation
sequence for upstream x264: [`docs/runbooks/local-setup.md`](../../docs/runbooks/local-setup.md)
(canonical source: [`README.md`](../../README.md)).
