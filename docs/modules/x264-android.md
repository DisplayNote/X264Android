# Module: `x264-android`

The only module in this repo (`settings.gradle`: `include ':x264-android'`).
An Android library producing one AAR with two layers: a Java API and a
native JNI/x264 layer.

## Purpose and boundaries

Encodes raw YUV/NV video frames to H.264 on-device, in real time, at low
latency (`preset = "ultrafast"`, `zerolatency` tune hardcoded in
`libx264_jni.cpp`). Does **not** handle capture, muxing/containerization,
decoding, or network transport — callers own the camera pipeline and
whatever container format (e.g. RTP, MP4) wraps the output NALs.

## Public API

`x264-android/src/main/java/com/github/bakaoh/x264/`:

- **`X264Encoder`** — the entry point. 4 native methods:
  `initEncoder(X264Params)`, `encodeFrame(byte[], int colorFormat, long pts)`,
  `releaseEncoder()`, `getVersion()`. Loads `libx264a.so` in a static
  initializer.
- **`X264Params`** — plain input struct: `width`, `height`, `bitrate`
  (bits/sec, converted to kbps internally), `fps`, `gop`, `profile`
  (e.g. `"baseline"`), `preset` (e.g. `"ultrafast"`). Also defines the
  supported color-space constants: `CSP_I420`, `CSP_YV12`, `CSP_NV12`,
  `CSP_NV21`.
- **`X264InitResult`** — `err` (0 = OK, see error codes below), `sps`,
  `pps` (byte arrays, valid for the whole stream once init succeeds).
- **`X264EncodeResult`** — `err`, `data` (encoded NAL payload), `pts`,
  `isKey` (true for IDR frames).

Error codes (defined in `libx264_jni.cpp`, not exposed as Java constants —
callers currently compare raw ints):

| Code | Meaning |
|---|---|
| `0` | OK |
| `-2` | `x264_param_apply_profile` failed (bad `profile` string) |
| `-3` | `x264_encoder_open` returned NULL |
| `-4` | Unsupported `colorFormat` passed to `encodeFrame` |
| `-5` | `x264_encoder_encode` failed |

Left as raw ints for now (a deliberate, revisit-later decision, not an
oversight) — see the `TODO` comment directly above the `#define`s in
`libx264_jni.cpp`.

## Native layer

`x264-android/src/main/cpp/`:

- `libx264_jni.cpp` — the JNI bridge. One `EncoderContext` struct
  (`x264_param_t` + `x264_t*` + `x264_picture_t`) per `X264Encoder`
  instance, stored via `SetLongField`/`GetLongField` on the Java object's
  `ctx` field. `initEncoder` builds params from the `X264Params` object,
  opens the encoder, and returns the stream's SPS/PPS. `encodeFrame`
  builds an `x264_picture_t` view over the caller's byte array (I420/YV12
  as 3-plane, NV12/NV21 as 2-plane) and calls `x264_encoder_encode`.
  `releaseEncoder` drains delayed frames before `x264_encoder_close`.
- `stdio_compat.c` — defines `stderr` manually when
  `__ANDROID_API__ < 23`, working around its removal from unified NDK
  headers at low API levels while the build targets `android-21`.
- `Android.mk` / `Application.mk` — `ndk-build` scripts. See
  `docs/architecture.md` for the full build-time flow and why the 16 KB
  page-size flags and `LOCAL_DISABLE_*` settings are there.
- `build_x264.sh` — cross-compiles upstream x264 (cloned into `libx264/`,
  not committed) for `armeabi-v7a`, `arm64-v8a`, `x86_64`, `x86` into
  `prebuilt/<ABI>/`. See Gotchas in `AGENTS.md` for its stale default
  NDK path and the binutils fallback.

## Upstream and downstream dependencies

- **Upstream**: VideoLAN x264 (`http://git.videolan.org/git/x264.git`),
  pinned by commit hash in `README.md`. Not a Gradle/Maven dependency —
  fetched and compiled from source by `build_x264.sh`.
- **Downstream**: any Android app depending on
  `com.displaynote.x264lib:x264lib` via Artifactory/Maven.

## How to test it in isolation

No automated test suite exists (`src/test`, `src/androidTest` are both
absent). In practice, verification is:

1. `./gradlew :x264-android:assembleRelease` succeeds for all 4 ABIs.
2. A consuming app (or a throwaway instrumented harness) calls
   `initEncoder` with representative `X264Params`, checks
   `X264InitResult.err == 0` and non-null `sps`/`pps`, then feeds frames
   through `encodeFrame` and checks `X264EncodeResult.err == 0`.

See `docs/runbooks/testing.md`.

## Extension points and typical changes

- **New encoder parameter** (e.g. exposing `x264_param_t.rc.i_qp_max`):
  add a field to `X264Params.java`, read it in `libx264_jni.cpp`'s
  `initEncoder` via `GetFieldID`/`GetIntField` (or the appropriate
  `Get*Field`), assign it onto `ctx->params`.
- **New color-space support**: add a `CSP_*` constant to
  `X264Params.java` matching the upstream `X264_CSP_*` value, add a case
  to the `switch (csp)` block in `encodeFrame` (`libx264_jni.cpp`)
  describing its plane layout.
- **New target ABI**: needs changes in three places —
  `x264-android/build.gradle` (`abiFilters`), `Application.mk`
  (`APP_ABI`), and a new `build_one` invocation block in `build_x264.sh`.
