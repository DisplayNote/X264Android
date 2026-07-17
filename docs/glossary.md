# Glossary

- **x264** — the upstream open-source H.264/AVC video encoder library
  (VideoLAN project) that this repo wraps. Not written or modified here;
  cloned and cross-compiled from a pinned commit by `build_x264.sh`.
- **JNI (Java Native Interface)** — the Android/Java mechanism used to
  call into native (C/C++) code. `libx264_jni.cpp` is this repo's JNI
  bridge.
- **`ndk-build`** — the NDK's `Android.mk`/`Application.mk`-driven native
  build system, as opposed to CMake (not used in this repo).
- **AAR (Android Archive)** — the packaged output of this repo
  (`x264-android-release.aar`), the distributable form of an Android
  library, bundling compiled Java/Kotlin classes and native `.so` files
  per ABI.
- **ABI (Application Binary Interface)** — the CPU architecture target
  for native code; this repo builds `armeabi-v7a`, `arm64-v8a`, `x86`,
  `x86_64`.
- **SPS / PPS (Sequence/Picture Parameter Set)** — H.264 metadata NAL
  units describing the encoded stream's parameters; returned once by
  `X264Encoder.initEncoder` and required by downstream decoders/muxers.
- **NAL (Network Abstraction Layer unit)** — the basic unit of H.264
  bitstream data; `encodeFrame` returns the encoded frame's NAL payload
  as `X264EncodeResult.data`.
- **IDR frame** — an Instantaneous Decoder Refresh frame, a keyframe that
  a decoder can start from with no prior state. Surfaced as
  `X264EncodeResult.isKey`.
- **CSP (color space)** — the pixel format of an input frame. This repo
  supports `I420`, `YV12` (3-plane, 4:2:0 planar) and `NV12`, `NV21`
  (2-plane, 4:2:0 semi-planar), matching upstream x264's `X264_CSP_*`
  constants.
- **GOP (Group of Pictures)** — the maximum interval between keyframes;
  `X264Params.gop` maps to `x264_param_t.i_keyint_max`.
- **Preset / profile** — x264 tuning knobs. `preset` trades encode speed
  for compression efficiency (e.g. `"ultrafast"`); `profile` constrains
  the bitstream to a compatibility tier (e.g. `"baseline"`).
- **16 KB page size** — an Android 15+ requirement for native libraries
  targeting newer devices; enforced here via
  `-Wl,-z,max-page-size=16384` linker flags in `Android.mk` and
  `Application.mk`.
