# Local setup — from zero to a built AAR

This is the runbook version of `README.md`'s build instructions, with
nothing added — `README.md` remains the canonical source; update both if
either changes.

## 1. Prerequisites

- Android NDK `26.3.11579264` (the version pinned in
  `x264-android/build.gradle`).
- JDK 17.
- Android SDK with `compileSdk 36` available.
- Git (to clone upstream x264).

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS example
```

## 2. Configure the native toolchain environment

```bash
export ANDROID_NDK_ROOT="<path-to-ndk-26.3.11579264>"
export ANDROID_NDK_PLATFORM=android-21
export ANDROID_NDK_HOST=linux-x86_64   # or darwin-x86_64 on macOS
```

Do **not** skip `ANDROID_NDK_ROOT` — `build_x264.sh` has a stale fallback
default (`/home/forlayo/android-ndk-r21e`, NDK r21e) that does not match
the pinned NDK 26 and almost certainly does not exist on your machine.

## 3. Fetch and cross-compile upstream libx264

```bash
cd x264-android/src/main/cpp
git clone http://git.videolan.org/git/x264.git libx264
(cd libx264 && git checkout ae03d92b52bb7581df2e75d571989cb1ecd19cbd)
./build_x264.sh
```

This runs upstream's own `./configure && make install` once per ABI
(`armeabi-v7a`, `arm64-v8a`, `x86_64`, `x86`), producing
`prebuilt/<ABI>/lib/libx264.a` and `prebuilt/<ABI>/include/*.h`.

## 4. (Optional) Build the JNI `.so` manually

```bash
cd x264-android/src/main/cpp
$ANDROID_NDK_ROOT/ndk-build NDK_PROJECT_PATH=. \
    APP_BUILD_SCRIPT=Android.mk \
    NDK_APPLICATION_MK=Application.mk
```

Optional because step 5 (Gradle) re-invokes `ndk-build` automatically via
`externalNativeBuild`.

## 5. Assemble the AAR

```bash
cd <repo root>
./gradlew :x264-android:assembleRelease
```

Output: `x264-android/build/outputs/aar/x264-android-release.aar`,
bundling `libx264a.so` for all 4 ABIs.

## 6. (Optional) Publish to Artifactory

Requires `displaynoteDeployerArtifactoryUrl` /
`displaynoteDeployerArtifactoryUsername` /
`displaynoteDeployerArtifactoryPassword` set (typically in
`~/.gradle/gradle.properties`).

```bash
./gradlew :x264-android:artifactoryPublish
```

Publishes under Maven coordinates `com.displaynote.x264lib:x264lib:1.0.1`
(the version currently set in `x264-android/build.gradle`).
