# Repository structure

Single Gradle module: `x264-android` (see `settings.gradle`).

```
X264Android/
├── build.gradle, settings.gradle, gradlew*   # root Gradle config
├── README.md, AGENTS.md, CLAUDE.md            # human + agent docs
├── docs/                                      # architecture, module, runbook, glossary docs
└── x264-android/
    ├── build.gradle                           # compileSdk 36, ndkVersion, minSdk 26/targetSdk 35, ABIs, publishing
    └── src/main/
        ├── cpp/          # native/JNI: Android.mk, Application.mk, build_x264.sh,
        │                 # libx264_jni.cpp (bridge), stdio_compat.c (API<23 shim)
        ├── java/com/github/bakaoh/x264/   # X264Encoder, X264Params, X264InitResult, X264EncodeResult
        └── res/values/strings.xml
```

Full map with one-line purpose per folder and the "where to add X" table:
[`AGENTS.md`](../../AGENTS.md). Per-layer detail:
[`docs/modules/x264-android.md`](../../docs/modules/x264-android.md).
