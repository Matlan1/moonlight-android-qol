# CLAUDE.md

Guidance for working in this repo. This is **moonlight-android-qol**, a quality-of-life fork of
[moonlight-stream/moonlight-android](https://github.com/moonlight-stream/moonlight-android) — the
Android client for Moonlight game streaming (NVIDIA GameStream / Sunshine / Apollo hosts).

## Repo layout & remotes

- `origin` → upstream `moonlight-stream/moonlight-android` (for pulling upstream changes).
- `qol` → `Matlan1/moonlight-android-qol` (this fork; push feature branches here).
- The native streaming engine is the **git submodule** `app/src/main/jni/moonlight-core/moonlight-common-c`.
  It is vendored by commit SHA. This fork points it at **`Matlan1/moonlight-common-c`** (forked from upstream)
  so we can patch native code (see "Native fork workflow").

## Build & toolchain (the non-obvious parts)

- **Always populate the submodule first** — a fresh clone leaves it empty and the native build fails:
  `git submodule update --init --recursive`
- Requirements: **JDK 17** (AGP 8.5.1 / Gradle 8.7 need it — the app's *source* level is Java 11, but the
  build toolchain itself requires 17), **Android SDK platform 34** + **build-tools 34.0.0**, and
  **NDK 27.0.12077973**. Native code builds via **ndk-build** (`app/src/main/jni/Android.mk`), not CMake.
- `local.properties` (git-ignored, machine-specific) must set `sdk.dir`.
- Product flavors: `root` / `nonRoot`. Build types: `debug` / `release` (both have `minifyEnabled true`).
  Primary debug APK task: **`./gradlew assembleNonRootDebug`** (run with `JAVA_HOME` pointing at a JDK 17).
- Exact local toolchain paths for this machine are recorded in the per-project memory store, not here.

## Architecture map (Java ↔ JNI ↔ native)

Streaming flows: UI/activity → `NvConnection` → `MoonBridge` (JNI) → `moonlight-common-c` (native), with
native callbacks coming back up through `MoonBridge` to the `NvConnectionListener` (implemented by `Game`).

Key files:
- `app/src/main/java/com/limelight/Game.java` — the streaming activity. Implements `NvConnectionListener`
  (`stageStarting`/`stageFailed`/`connectionStarted`/`connectionTerminated`), builds the
  `StreamConfiguration`, handles touch/mouse input (`TouchContext` map), hosts the on-screen controller.
- `app/src/main/java/com/limelight/nvstream/NvConnection.java` — connection lifecycle on a background
  thread; calls the blocking `MoonBridge.startConnection(...)`; `stop()` interrupts + tears down.
- `app/src/main/java/com/limelight/nvstream/jni/MoonBridge.java` — the JNI bridge: native methods
  (`startConnection`, mouse/gamepad/keyboard senders, `submitDecodeUnit`) and the `bridgeClStage*` callbacks.
- `app/src/main/java/com/limelight/preferences/PreferenceConfiguration.java` — all global stream settings
  (resolution/fps/bitrate/videoFormat/framePacing/HDR/…) read from `SharedPreferences`.
- `app/src/main/java/com/limelight/AppView.java` — the app/game grid + per-app long-press context menu.
- `app/src/main/java/com/limelight/binding/input/touch/` — `TouchContext` (Absolute/Relative) mouse-from-touch.
- `app/src/main/java/com/limelight/binding/input/virtual_controller/` — on-screen controls
  (`VirtualController`, `VirtualControllerElement` with JSON `getConfiguration`/`loadConfiguration`,
  `VirtualControllerConfigurationLoader` persisting to the `"OSC"` SharedPreferences).
- `app/src/main/java/com/limelight/binding/video/MediaCodecDecoderRenderer.java` — decode + frame pacing
  (`FRAME_PACING_*` modes; renderer thread + optional Choreographer; `releaseOutputBuffer` present timing).

In the native submodule: `src/Connection.c` (`LiStartConnection` stage machine), `src/RtspConnection.c`
(`performRtspHandshake`, `transactRtspMessageTcp`), `src/PlatformSockets.c/.h`.

## Native fork workflow (moonlight-common-c)

To change native code: edit in the submodule working tree, commit on a branch in
**`Matlan1/moonlight-common-c`** (remote `fork`), push, then in this repo repoint `.gitmodules` to the fork,
`git submodule sync`, and commit the bumped gitlink. Keep native diffs **minimal** so they rebase cleanly
onto upstream and stay submittable as upstream PRs.

## Conventions

- Match the surrounding code's style; native code is C (C99-ish), app code is Java.
- Each QoL feature ships on its own branch/PR into `qol`. Keep changes focused.
- See `zany-giggling-star.md` plan (in the Claude plans dir) for the active roadmap: RTSP auto-retry,
  per-app resolution/bitrate/FPS, mouse/trackpad modes, custom on-screen buttons, Warp Drive frame pacing.
