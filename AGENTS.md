# AGENTS.md

Instructions for AI coding agents (and humans) working in this repository.

## Project snapshot

X264Android wraps the upstream [x264](https://www.videolan.org/developers/x264.html)
H.264 encoder in a JNI layer and packages it as an Android library (AAR),
exposing a small Java API (`X264Encoder`, `X264Params`) for encoding raw
YUV/NV frames to H.264 in real time. Stack: Gradle 7.5.1 / Android Gradle
Plugin 7.4.2, `ndk-build` (no CMake), NDK `26.3.11579264`, Java sources at
Android `compileSdk 36` (`minSdk 26`, `targetSdk 35`). There is no sample
app — this repo produces a single library artifact,
`com.displaynote.x264lib:x264lib`.

> **Known risk**: `compileSdk 36` is paired with Android Gradle Plugin
> `7.4.2` (root `build.gradle`), which predates official support for API
> levels this high. This may only be a build-time warning, or it may hard
> fail depending on the exact AGP/Gradle/SDK combination installed — see
> the Gotchas section below. Not resolved as part of this docs pass.

## Repository map

```
X264Android/
├── build.gradle, settings.gradle   # root Gradle config (single module: x264-android)
├── gradle/, gradlew*                # Gradle wrapper (7.5.1)
├── README.md                        # human quick-start / build guide
├── AGENTS.md, CLAUDE.md             # this file and its Claude Code counterpart
├── docs/                            # architecture, module, runbook, glossary docs (this initiative)
└── x264-android/                    # the only module — Android library (AAR)
    ├── build.gradle                 # module config: compileSdk 36, ndkVersion, ABIs, publishing
    └── src/main/
        ├── cpp/                     # native/JNI layer
        │   ├── Android.mk, Application.mk   # ndk-build scripts
        │   ├── build_x264.sh                # cross-compiles upstream libx264 per ABI
        │   ├── libx264_jni.cpp               # JNI bridge: X264Encoder <-> libx264 C API
        │   ├── stdio_compat.c                 # stderr shim for API < 23 (see Gotchas)
        │   └── libx264/, prebuilt/, libs/, obj/  # fetched/generated, gitignored
        ├── java/com/github/bakaoh/x264/     # public Java API (X264Encoder, X264Params, results)
        └── res/values/strings.xml           # trivial resource stub
```

## Run / build / test / lint

There is no automated test suite yet (no `src/test`, no `src/androidTest`).
Verification is currently build-success plus manual on-device smoke
testing. Adding automated tests (native GoogleTest/CTest, JVM JUnit,
instrumented AndroidX Test) plus the CI/CD to run them is the **#1
technical-debt priority** for this repo — see `docs/runbooks/testing.md`
for the plan and what verification means in the interim.

Full step-by-step build instructions (env vars, cloning upstream x264,
`ndk-build`, `assembleRelease`, publishing) live in `README.md` — do not
duplicate them here; that is the canonical source. Quick reference:

```bash
# One-time: fetch and cross-compile upstream x264 for all 4 ABIs
cd x264-android/src/main/cpp
git clone http://git.videolan.org/git/x264.git libx264
(cd libx264 && git checkout ae03d92b52bb7581df2e75d571989cb1ecd19cbd)
export ANDROID_NDK_ROOT="<path-to-ndk-26.3.11579264>"
export ANDROID_NDK_PLATFORM=android-21
export ANDROID_NDK_HOST=linux-x86_64   # or darwin-x86_64
./build_x264.sh

# Build the AAR (Gradle re-invokes ndk-build automatically)
cd ../../../../..
./gradlew :x264-android:assembleRelease
# -> x264-android/build/outputs/aar/x264-android-release.aar
```

No lint task is configured beyond the Android Gradle plugin defaults; no
`ktlint`/`checkstyle` config exists.

## Architecture overview

See `docs/architecture.md` for the full diagram and data flow. In short:
a consuming Android app calls the Java `X264Encoder` API →
`libx264_jni.cpp` (JNI bridge, compiled into `libx264a.so` via
`Android.mk`) → statically-linked `libx264.a` (upstream x264, cross-compiled
per-ABI by `build_x264.sh`, never itself modified).

## Coding conventions

- Java package is `com.github.bakaoh.x264` — a leftover from the upstream
  fork this was based on (`bakaoh/x264-android`). It has **not** been
  renamed to a DisplayNote namespace even though Maven `group` is
  `com.displaynote.x264lib`. Don't "fix" this casually — it's part of the
  public API surface; changing it is a breaking change for consumers.
- The JNI bridge (`libx264_jni.cpp`) is intentionally thin: it copies
  `X264Params` fields into `x264_param_t`, calls the upstream C API, and
  marshals results back into `X264InitResult`/`X264EncodeResult`. Add new
  encoder features by extending this same pattern, not by introducing new
  abstractions.
- `Android.mk` sets `LOCAL_DISABLE_FORMAT_STRING_CHECKS` and
  `LOCAL_DISABLE_FATAL_LINKER_WARNINGS` to `true` — this is intentional,
  to suppress warnings originating in the vendored `libx264.a`, not a bug
  to fix.
- No `TODO`/`FIXME` markers exist in the source; treat any workaround
  comment (e.g. in `build_x264.sh`, `stdio_compat.c`) as load-bearing —
  see Gotchas below before touching them.

## Patterns to follow / anti-patterns to avoid

- **Follow**: keep `libx264.a` untouched and vendored via
  `build_x264.sh` + `prebuilt/<ABI>/`. Do not vendor patched x264 sources
  into this repo — pin a different upstream commit instead
  (`x264-android/src/main/cpp/libx264` is cloned fresh, not committed).
- **Follow**: any new native source file added to the JNI layer must be
  added to `LOCAL_SRC_FILES` in `Android.mk` (see how `stdio_compat.c` was
  added alongside `libx264_jni.cpp`).
- **Avoid**: don't lower `Application.mk`'s `APP_PLATFORM` below
  `android-21` without re-checking `stdio_compat.c` — that shim exists
  specifically to cover `__ANDROID_API__ < 23` symbol gaps.
- **Avoid**: don't drop the `-Wl,-z,max-page-size=16384` linker flags from
  `Android.mk`/`Application.mk` — they exist for Android 15+ 16 KB
  page-size compliance (see commit `4299827`).

## Where to add X

| You're adding... | Go to | Notes |
|---|---|---|
| A new native encoder parameter/feature | `libx264_jni.cpp` (+ `Android.mk` if new files) | Extend `EncoderContext` / `initEncoder`/`encodeFrame`, mirror the field in `X264Params.java` |
| A new Java-facing field or result | `X264Params.java`, `X264InitResult.java`, or `X264EncodeResult.java` | Keep field names matching what `libx264_jni.cpp` looks up via `GetFieldID` — the JNI code binds by string name |
| A new target ABI | `x264-android/build.gradle` (`abiFilters`), `Application.mk` (`APP_ABI`) | Also needs a `build_one` branch in `build_x264.sh` for cross-compilation |
| A newer NDK / x264 upstream pin | `x264-android/build.gradle` (`ndkVersion`), `README.md` step 3 (commit hash) | Re-verify `stdio_compat.c` and the binutils fallback in `build_x264.sh` still apply |
| Module documentation | `docs/modules/x264-android.md` | Single-module repo; update in place |
| A runbook (setup/testing/release/troubleshooting) | `docs/runbooks/` | |

## Gotchas

- **No automated tests.** Any change to the JNI bridge or native params
  mapping needs manual on-device verification (encode a frame, check
  `err == 0` and non-null `sps`/`pps`/`data`).
- **`build_x264.sh` defaults are stale/personal**: if `ANDROID_NDK_ROOT`
  is unset it falls back to `/home/forlayo/android-ndk-r21e` (a specific
  developer's local path, NDK r21e — inconsistent with the pinned NDK 26
  used everywhere else). Always set `ANDROID_NDK_ROOT` explicitly; don't
  rely on the fallback.
- **`stdio_compat.c`** works around the removal of the `stderr` symbol
  from unified NDK headers below API 23, needed because the native build
  targets `APP_PLATFORM := android-21`.
- **binutils fallback in `build_x264.sh`**: modern NDKs (23+) ship only
  generic `llvm-*` binutils, not the old `<triple>-strings` etc.; the
  script falls back to `llvm-` prefixed tools when the triple-prefixed
  ones aren't executable.
- **Version drift**: `x264-android/build.gradle` sets `version = "1.0.1"`
  while `README.md`'s publish section refers to `1.0.0` — the README
  string in this docs pass has been corrected to `1.0.1`; keep them in
  sync going forward.
- **`minSdkVersion` is 26**, set to match the Montage product's minimum
  supported Android version. This comfortably clears the native
  `APP_PLATFORM := android-21` floor (`Application.mk`), so no
  native-vs-Java API mismatch exists. `targetSdkVersion 35` and
  `compileSdkVersion 36` were set together in the same change; the SDK
  triple (`26`/`35`/`36`) is now a settled decision, not an open
  question.
- **AGP `7.4.2` vs. `compileSdk 36`**: root `build.gradle` still pins
  Android Gradle Plugin `7.4.2`, which predates official support for API
  36. This may surface only as a build warning, or as a hard failure,
  depending on the installed Gradle/AGP/SDK combination — verify locally
  before relying on a from-scratch build. Bumping AGP (and likely the
  Gradle wrapper alongside it) was out of scope for this change; flagged,
  not fixed.
- **No LICENSE file, and none is planned as part of this docs pass** —
  see External systems / Licensing below. Do not assume the AAR is
  freely redistributable; this is a standing, acknowledged risk, not an
  oversight to silently fix.

## Glossary

See `docs/glossary.md`.

## External systems

| System | Role | Configured in |
|---|---|---|
| VideoLAN x264 (upstream, GPLv2+) | Source of the actual H.264 encoder, cloned and cross-compiled at build time, pinned to a specific commit | `README.md` step 3, `build_x264.sh` |
| Android NDK `26.3.11579264` | Cross-compilation toolchain for both `libx264` and the JNI bridge | `x264-android/build.gradle` (`ndkVersion`), `README.md` |
| Artifactory (via `com.jfrog.artifactory` plugin) | Optional Maven publishing target for the AAR | root `build.gradle`, `x264-android/build.gradle` (`artifactoryPublish` task, `displaynoteDeployerArtifactory*` properties) |

### Licensing note

x264 upstream is dual-licensed **GPLv2 (or later) / commercial**. This
repo statically links `libx264.a` into the published AAR with **no
LICENSE/COPYING/NOTICE file** of its own, and none is planned as part of
this docs pass. This is a standing, acknowledged risk — confirmed as
still open, not an oversight. Do not distribute this AAR outside
DisplayNote, or add a LICENSE file yourself, without a legal decision on
the applicable x264 licensing basis.
