# X264Android #

## Description ##

x264 encoder for use through JNI in Android.

> For AI coding agents (or a deeper architectural overview): see
> [`AGENTS.md`](AGENTS.md) / [`CLAUDE.md`](CLAUDE.md) and the `docs/`
> folder.

## Using ##

* Create `X264Encoder` and `X264Params`

```
X264Encoder encoder = new X264Encoder();

X264Params params = new X264Params();
params.width =  1280;
params.height = 720;
params.bitrate = 1000 * 1024;
params.fps = 25;
params.gop = params.fps * 2;
params.preset = "ultrafast";
params.profile = "baseline";
```

* Init encoder

```
X264InitResult initRs = encoder.initEncoder(params);

if (initRs.err == 0) {
  // process initRs.sps, initRs.pps
}
```

* Encode frame

```
X264EncodeResult encodedFrame = encoder.encodeFrame(inputFrame, X264Params.CSP_NV21, presentationTimestamp);

if (encodedFrame.err == 0) {
  // process encodedFrame.data
}
```

## Build instructions ##

### 1. Prerequisites

* Install the [Android NDK][] (tested with `26.3.11579264`).
* Install JDK 17 and Android SDK (compileSdk 36) and set `JAVA_HOME` accordingly:
  ```
  export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS example
  ```
* Install the Android Gradle plugin dependencies via `./gradlew`.

[Android NDK]: https://developer.android.com/tools/sdk/ndk/index.html

### 2. Configure the native toolchain

Set the following environment variables before building `libx264`:
```
export ANDROID_NDK_ROOT="<path-to-ndk>"
export ANDROID_NDK_PLATFORM=android-21
export ANDROID_NDK_HOST=darwin-x86_64   # or linux-x86_64 on Linux
```

### 3. Fetch and build `libx264`

From `x264-android/src/main/cpp` clone the upstream encoder and run the helper script:
```
cd x264-android/src/main/cpp
git clone http://git.videolan.org/git/x264.git libx264
pushd libx264 && git checkout ae03d92b52bb7581df2e75d571989cb1ecd19cbd && popd
./build_x264.sh
```

This produces static archives under `prebuilt/<ABI>/lib/libx264.a` and the matching headers in `prebuilt/<ABI>/include`.

### 4. Build `libx264a.so` (JNI shared library)

Use `ndk-build` to link the JNI wrapper (`libx264_jni.cpp`) against the prebuilt `libx264.a` for all supported ABIs:
```
cd x264-android/src/main/cpp
$ANDROID_NDK_ROOT/ndk-build NDK_PROJECT_PATH=. \
    APP_BUILD_SCRIPT=Android.mk \
    NDK_APPLICATION_MK=Application.mk
```

The resulting shared libraries are written to `x264-android/src/main/cpp/libs/<ABI>/libx264a.so`. Gradle/Android Studio also rebuild these automatically during the next step, so this command is optional if you plan to use `./gradlew` immediately.

### 5. Assemble the Android library (AAR)

Return to the repository root and run:
```
./gradlew :x264-android:assembleRelease
```

Gradle invokes `externalNativeBuild` (`ndk-build`) to regenerate `libx264a.so`, bundles the Java sources, and produces the final `x264-android-release.aar` under `x264-android/build/outputs/aar/`. This AAR already contains the JNI binaries for `armeabi-v7a`, `arm64-v8a`, `x86`, and `x86_64`.

### 6. (Optional) Publish the AAR

If you need to consume the library via Maven coordinates (`com.displaynote.x264lib:x264lib:1.0.1`), configure your credentials in `~/.gradle/gradle.properties` and run:
```
./gradlew :x264-android:artifactoryPublish
```
This uploads the generated AAR to the configured Artifactory repository.
