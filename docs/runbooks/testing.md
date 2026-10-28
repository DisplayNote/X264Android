# Testing

## What exists today

- **JVM unit tests** (JUnit 4, `x264-android/src/test/java/com/github/bakaoh/x264/`), run by
  Gradle without a device, the NDK or the native library:
  - `X264ParamsTest` — the defaults (1280x720, 24 fps, gop 48 = two seconds, `baseline`,
    `ultrafast`), the `CSP_*` constants against x264.h's `X264_CSP_*` values (`encodeFrame` passes
    the colour format straight to x264), and that instances don't share settings. It pins the
    default `bitrate = 500`, which the JNI layer turns into 0 kbps (see *Suspected bugs* below).
  - `X264InitResultTest`, `X264EncodeResultTest` — the result objects keep the error code, the
    SPS/PPS or frame arrays (by reference), the `long` pts and the key-frame flag, including the
    error shapes the JNI layer returns (`err` < 0, null data).
  - `JniBindingContractTest` — `libx264_jni.cpp` finds the `X264Params` fields and the result
    constructors by name and JNI signature at run time (`(I[B[B)V`, `(I[BJZ)V`); a rename compiles
    and only fails on a device, so the names and types are pinned here.
  - `X264Encoder` is not tested in the JVM: its static block loads `libx264a.so`.
- **No native (C/C++) tests and no instrumented tests** yet. The JNI path (`initEncoder` →
  `encodeFrame` → `releaseEncoder`) is still verified by hand, see *Manual verification* below.
- **CI** runs the JVM unit tests with JaCoCo coverage in the `tests_coverage_java` stage of
  `ci/azure-pipelines.yml` (roadmap 2.23, see below), and builds the release AAR on every run.

**Still to do (technical-debt recovery item):** the rest of DisplayNote's standard test pyramid:

- **Native (C/C++) unit tests** — GoogleTest/CTest for anything in
  `x264-android/src/main/cpp/` that can be exercised without a JVM (e.g.
  the plane/stride logic currently inline in
  `libx264_jni.cpp#encodeFrame`, if extracted into a testable function).
  Follow DisplayNote's standard GoogleTest/CTest conventions for
  structure, sanitizers, and coverage — there is no separate written
  spec for this in the current repo.
- **Instrumented tests** (`x264-android/src/androidTest/`) — AndroidX
  Test / `AndroidJUnitRunner`, on-device or emulator, to drive the real
  `X264Encoder` JNI path end-to-end (the same init → encode → release
  sequence described in "Manual verification" below, made repeatable).

## Commands

```bash
./gradlew :x264-android:test -Px264.skipNativeBuild=true
```

`-Px264.skipNativeBuild=true` (read in `x264-android/build.gradle`) leaves `ndk-build` out of the
build, so the JVM tests need neither NDK `26.3.11579264` nor a prebuilt `libx264` (without it,
Gradle wants the NDK just to configure, and installs it if the SDK licences allow). Never use it to
assemble or publish: the AAR would ship without `libx264a.so`. Test reports land under
`x264-android/build/reports/tests/`.

### Coverage

```bash
./gradlew :x264-android:testDebugUnitTest -Px264.skipNativeBuild=true
./gradlew :x264-android:jacocoTestReport -x :x264-android:testDebugUnitTest -Px264.skipNativeBuild=true
```

`jacocoTestReport` (defined in `x264-android/build.gradle`) writes
`x264-android/build/reports/jacoco/jacocoTestReport/jacocoTestReport.{xml,html}` from the
`testDebugUnitTest` run (`enableUnitTestCoverage` is on for the debug build type). `R`,
`BuildConfig` and the manifest classes are not counted. What the figure measures is in the README,
under *Coverage scope*.

### In CI

`ci/azure-pipelines.yml` runs the same two tasks in the `tests_coverage_java` stage
(`android-gradle/tests-coverage.yml` from qt-conan-ci, with `-Px264.skipNativeBuild=true`),
publishes the JUnit results and the JaCoCo report to the run and checks line coverage against the
75% target. It is in audit mode (`blocking: false`): failing tests or a figure below target are
warnings and the build still runs. Set `blocking: true` once the suite has been green on five
consecutive `master` runs; from then on a PR must not lower the figure.

The build and publish stages run `ci/build-libx264.sh` first: it fetches upstream x264 at the
commit pinned in `README.md` step 3 and runs `build_x264.sh` with NDK `26.3.11579264`, as the
README describes.

## Suspected bugs (found while writing the tests, not fixed)

- `X264Params.bitrate` defaults to `500`, but `initEncoder` divides it by 1000 for x264's kbps
  (`rc.i_bitrate`), so the default asks x264 for 0 kbps. `X264ParamsTest` pins today's value.
- `initEncoder` releases the profile string against the wrong `jstring`
  (`ReleaseStringUTFChars(preset, c_profile)`, `libx264_jni.cpp`).
- `encodeFrame` returns early on an unsupported colour format or an encode error without
  `ReleaseByteArrayElements(frame, ...)`, so the frame stays pinned or copied.
- Those two early returns pass `0` for the `jlong pts` argument of the variadic `NewObject`; an
  `int` where a 64-bit value is read is undefined behaviour on the 32-bit ABIs (`armeabi-v7a`,
  `x86`). `(jlong) 0` would be correct.
- `releaseEncoder` dereferences the context without a null check, so calling it before
  `initEncoder` crashes.

## Manual verification (current practice)

Because the JNI bridge and native params mapping have no automated
coverage, verify changes by driving the real API end-to-end:

1. Build the AAR (`docs/runbooks/local-setup.md`).
2. In a consuming app (or a throwaway instrumented harness), construct
   `X264Params` with realistic values: 1280x720, 24 fps, gop 48,
   `"baseline"` profile, `"ultrafast"` preset. Do **not** use
   `X264Params.java`'s default `bitrate = 500` as-is — the JNI layer
   divides `bitrate` by 1000 to get x264's kbps (`rc.i_bitrate`), so 500
   yields 0 kbps. Set a real bps value (e.g. `2_000_000` for ~2000 kbps).
3. Call `X264Encoder.initEncoder(params)` and check
   `X264InitResult.err == 0` and that `sps`/`pps` are non-null/non-empty.
4. Feed one or more frames of the color format you changed/added through
   `encodeFrame(frame, colorFormat, pts)` and check
   `X264EncodeResult.err == 0`, `data` non-empty, and `isKey` is `true`
   for the first frame.
5. Call `releaseEncoder()` and confirm no crash (it drains delayed frames
   before `x264_encoder_close`).

## Adding a new color space or parameter

Exercise every `switch` branch you touch in
`libx264_jni.cpp#encodeFrame` — each colorspace has a distinct plane
layout (I420/YV12 = 3 planes, NV12/NV21 = 2 planes) and a wrong stride
calculation will corrupt output silently rather than crash.

## Writing new tests

Put JVM tests in `x264-android/src/test/java/com/github/bakaoh/x264/`, keep them JUnit 4, name
them after the behaviour they check, and don't touch `X264Encoder` (it loads the native library).
A new `X264Params` field or result constructor gets a line in `JniBindingContractTest`, matching
the `GetFieldID`/`GetMethodID` lookup in `libx264_jni.cpp`.
