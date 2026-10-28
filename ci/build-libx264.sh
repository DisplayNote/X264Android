#!/bin/bash
# Fetches upstream x264 at the pinned commit and cross-compiles libx264.a for
# the four ABIs with the repo's build_x264.sh - README.md steps 2 and 3, as the
# pipeline's build and publish stages run them before Gradle (prebuilt/ and
# libx264/ are gitignored). Run from the repository root on Linux, with NDK
# 26.3.11579264 installed under the Android SDK.
set -euo pipefail

X264_URL=https://code.videolan.org/videolan/x264.git
X264_COMMIT=ae03d92b52bb7581df2e75d571989cb1ecd19cbd   # keep in sync with README.md step 3
NDK_VERSION=26.3.11579264                              # keep in sync with x264-android/build.gradle

sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/usr/local/lib/android/sdk}}"
export ANDROID_NDK_ROOT="${ANDROID_NDK_ROOT:-$sdk/ndk/$NDK_VERSION}"
export ANDROID_NDK_PLATFORM=android-21
export ANDROID_NDK_HOST=linux-x86_64
[ -d "$ANDROID_NDK_ROOT/toolchains/llvm" ] || { echo "No NDK at $ANDROID_NDK_ROOT" >&2; exit 1; }

cd x264-android/src/main/cpp
rm -rf libx264 prebuilt
git init -q libx264
git -C libx264 fetch -q --depth 1 "$X264_URL" "$X264_COMMIT"
git -C libx264 checkout -q FETCH_HEAD

# build_x264.sh does not stop on a failed configure/make, so check its output.
bash ./build_x264.sh
for abi in armeabi-v7a arm64-v8a x86 x86_64; do
    [ -f "prebuilt/$abi/lib/libx264.a" ] || { echo "libx264.a missing for $abi" >&2; exit 1; }
done
ls -l prebuilt/*/lib/libx264.a
