# CLAUDE.md

Claude Code-specific instructions. Full detail lives in `AGENTS.md` — read
that first for the repository map, "where to add X" table, and gotchas.
This file is the dense, imperative summary.

## What this repo is

Android library (AAR) wrapping upstream x264 via JNI. Single Gradle
module: `x264-android`. No sample app, no automated tests.

## DO

- Read `README.md` before touching the build — it has the exact,
  verified build commands (env vars, upstream x264 clone/checkout,
  `ndk-build`, `./gradlew :x264-android:assembleRelease`).
- Set `ANDROID_NDK_ROOT` explicitly to NDK `26.3.11579264` before running
  `build_x264.sh`. Do not rely on its fallback default.
- Keep `libx264_jni.cpp` field lookups (`GetFieldID`) in sync by name with
  `X264Params.java` / `X264InitResult.java` / `X264EncodeResult.java` —
  they're bound by string, not by generated glue.
- When adding a native source file, register it in `Android.mk`'s
  `LOCAL_SRC_FILES`.
- Preserve the `-Wl,-z,max-page-size=16384` linker flags in `Android.mk`
  and `Application.mk` (Android 16 KB page-size compliance).
- Verify JNI/native changes manually on-device (no test suite exists) —
  encode a frame and check `err == 0`.

## DON'T

- Don't rename the `com.github.bakaoh.x264` Java package casually — it's
  a public API surface, not dead code.
- Don't vendor a patched copy of `libx264` sources into this repo; pin a
  different upstream commit in `README.md` / re-clone instead.
- Don't remove `LOCAL_DISABLE_FORMAT_STRING_CHECKS` /
  `LOCAL_DISABLE_FATAL_LINKER_WARNINGS` from `Android.mk` — intentional,
  suppresses noise from the vendored static lib.
- Don't lower `APP_PLATFORM` below `android-21` without re-checking
  whether `stdio_compat.c`'s `__ANDROID_API__ < 23` shim still covers the
  gap.
- Don't raise `minSdkVersion` above `26` without checking with the
  Montage product team first — `26` is a hard requirement from them, not
  an arbitrary default.
- Don't assume this AAR is freely redistributable — there's no LICENSE
  file and upstream x264 is GPLv2+/commercial dual-licensed, and no
  license is planned as part of this docs pass. See the licensing note in
  `AGENTS.md`.

## Where things live

- Full architecture + diagram: `docs/architecture.md`
- Module detail (native + Java layers): `docs/modules/x264-android.md`
- Setup / testing / release / troubleshooting: `docs/runbooks/`
- Domain terms: `docs/glossary.md`

## Known open questions

The SDK triple (`minSdk 26` / `targetSdk 35` / `compileSdk 36`) is a
settled decision, not open. What's still unresolved: whether Android
Gradle Plugin `7.4.2` (root `build.gradle`) needs bumping to officially
support `compileSdk 36` — see the AGP gotcha in `AGENTS.md`. The x264
GPL/commercial licensing gap is a standing, acknowledged risk (no
LICENSE file, none planned here) — don't "fix" it by adding one
yourself; that's a legal decision. Adding automated tests (and the CI to
run them) is the **#1 technical-debt priority** for this repo, ahead of
everything else in the backlog — see `docs/runbooks/testing.md`.
