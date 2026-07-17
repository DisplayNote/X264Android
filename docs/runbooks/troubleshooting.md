# Troubleshooting

## `build_x264.sh` fails to find `strings`/`ar`/etc. for a toolchain

Modern NDKs (23+) ship only generic `llvm-*` binutils, not the old
`<triple>-strings` style tools. `build_x264.sh` already has a fallback
(`if [ ! -x "${CROSS_PREFIX}strings" ]; then CROSS_PREFIX=${TOOLCHAIN}/bin/llvm-; fi`)
— if it still fails, check that `ANDROID_NDK_ROOT`/`ANDROID_NDK_HOST`
actually point at NDK `26.3.11579264`'s `toolchains/llvm/prebuilt/<host>`.

## `build_x264.sh` silently uses the wrong NDK

If `ANDROID_NDK_ROOT` is unset, the script defaults to
`/home/forlayo/android-ndk-r21e` — a specific developer's local path for
an old NDK version, not the pinned `26.3.11579264`. Always export
`ANDROID_NDK_ROOT` explicitly before running it.

## `UnsatisfiedLinkError: dlopen failed ... libx264a.so` at runtime

This library's `minSdkVersion` (`26`, `x264-android/build.gradle`) is
already comfortably above the native `APP_PLATFORM := android-21` floor
(`Application.mk`), so this specific mismatch shouldn't occur via this
library alone. If you still see it, check whether the *consuming app*
declares a lower `minSdkVersion` than this library requires — Gradle's
manifest merger should catch that at build time, but confirm the
merged manifest if it doesn't.

## Missing `stderr` symbol / crash referencing `__sF`

`stdio_compat.c` defines `stderr` manually for `__ANDROID_API__ < 23`.
If you see link or runtime errors related to `stderr`, confirm
`stdio_compat.c` is still listed in `Android.mk`'s `LOCAL_SRC_FILES` and
that `Application.mk`'s `APP_PLATFORM` hasn't been changed without
revisiting this shim.

## `encodeFrame` returns `err == -4` (`X264A_ERR_NOT_SUPPORT_CSP`)

The `colorFormat` passed doesn't match one of `X264Params.CSP_I420`,
`CSP_YV12`, `CSP_NV12`, `CSP_NV21`. There's no fifth "unknown, try
anyway" path — add a new `case` in `libx264_jni.cpp#encodeFrame` if you
need a new color space, with the correct plane count/stride math for it.

## AAR builds but is missing an ABI

Check three places agree: `x264-android/build.gradle` (`ndk.abiFilters`),
`Application.mk` (`APP_ABI`), and that `build_x264.sh` has a
corresponding `build_one` block that actually populated
`prebuilt/<ABI>/lib/libx264.a` for that ABI.
