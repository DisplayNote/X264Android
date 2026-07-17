# Copilot instructions

This file is read by GitHub Copilot in both VS Code and Visual Studio.
Full detail lives in [`AGENTS.md`](../AGENTS.md) — read that first. Below
are the rules that hurt most when violated.

- No automated tests exist in this repo. Verify JNI/native changes
  manually on-device (see `docs/runbooks/testing.md`) — don't assume a
  green build means the encoder still works.
- `minSdkVersion` is `26`, a hard requirement from the Montage product,
  not an arbitrary default — don't lower it, and don't raise it without
  checking with that team first.
- Keep `libx264_jni.cpp`'s `GetFieldID` lookups in sync **by name** with
  `X264Params.java` / `X264InitResult.java` / `X264EncodeResult.java` —
  there is no generated glue catching a rename.
- Don't rename the `com.github.bakaoh.x264` Java package — it's a public
  API surface, not dead code, despite the Maven `group` being
  `com.displaynote.x264lib`.
- Preserve the `-Wl,-z,max-page-size=16384` linker flags in `Android.mk`
  / `Application.mk` (Android 16 KB page-size compliance) and the
  `LOCAL_DISABLE_FORMAT_STRING_CHECKS` / `LOCAL_DISABLE_FATAL_LINKER_WARNINGS`
  settings (intentional, suppress noise from the vendored static lib).
- Don't add a LICENSE file or otherwise imply this AAR is freely
  redistributable — the x264 GPL/commercial licensing question is an
  acknowledged open risk, not something to resolve unilaterally.
- New native source files must be registered in `Android.mk`'s
  `LOCAL_SRC_FILES`, or `ndk-build` silently won't compile them.
