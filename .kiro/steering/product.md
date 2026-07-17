# Product

X264Android wraps the upstream VideoLAN [x264](https://www.videolan.org/developers/x264.html)
H.264 encoder in a JNI layer and packages it as an Android library (AAR).
It gives consuming Android apps a small Java API (`X264Encoder`,
`X264Params`) to encode raw YUV/NV video frames to H.264 in real time,
low-latency (`preset = "ultrafast"`, `zerolatency` tune).

- No sample app ships in this repo — it produces exactly one artifact:
  `com.displaynote.x264lib:x264lib`.
- `minSdkVersion` is `26`, a hard requirement from the Montage product
  that consumes this library.
- Does not handle capture, muxing/containerization, decoding, or network
  transport — callers own the rest of the pipeline.

Full detail: [`AGENTS.md`](../../AGENTS.md), [`docs/architecture.md`](../../docs/architecture.md).
