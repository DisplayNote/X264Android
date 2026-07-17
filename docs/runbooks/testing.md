# Testing

## What exists today

Nothing automated. There is no `x264-android/src/test/` (JVM unit tests)
and no `x264-android/src/androidTest/` (instrumented tests) directory in
this repo. The only "test" that currently gates a change is: does
`./gradlew :x264-android:assembleRelease` succeed for all 4 ABIs.

**Decision (technical-debt recovery item):** this repo will gain
automated tests, following DisplayNote's standard test pyramid rather
than a bespoke setup:

- **Native (C/C++) unit tests** — GoogleTest/CTest for anything in
  `x264-android/src/main/cpp/` that can be exercised without a JVM (e.g.
  the plane/stride logic currently inline in
  `libx264_jni.cpp#encodeFrame`, if extracted into a testable function).
  Follow DisplayNote's standard GoogleTest/CTest conventions for
  structure, sanitizers, and coverage — there is no separate written
  spec for this in the current repo.
- **JVM unit tests** (`x264-android/src/test/`) — JUnit, for the Java
  data classes (`X264Params`, `X264InitResult`, `X264EncodeResult`) and
  any pure-Java logic that doesn't require a device/native library.
- **Instrumented tests** (`x264-android/src/androidTest/`) — AndroidX
  Test / `AndroidJUnitRunner`, on-device or emulator, to drive the real
  `X264Encoder` JNI path end-to-end (the same init → encode → release
  sequence described in "Manual verification" below, made repeatable).

**Priority: this is the #1 technical-debt item for this repo**, ahead of
any other tech-debt work — including standing up CI/CD to actually run
the resulting suite (there is currently no CI/CD in this repo at all, see
`docs/runbooks/release.md`; that gap is now in scope as part of this same
initiative, not a separate follow-on). No source sets exist yet, so treat
the "Manual verification" steps below as the interim process until the
instrumented suite (and the CI that runs it) lands.

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
