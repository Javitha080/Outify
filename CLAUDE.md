# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Outify is a third-party open-source Android Spotify client (Kotlin, Jetpack Compose, Material 3) built on [librespot](https://github.com/librespot-org/librespot) for audio streaming. Package namespace: `cc.tomko.outify`.

## Critical: Native Library Requirement

The app **will not work** without the Rust-built `liblibrespot_ffi.so` in `app/src/main/jniLibs/{arm64-v8a,armeabi-v7a}/`. A fresh checkout does **not** contain these files (`rust/target` may only have host-arch debug artifacts). Gradle will still assemble an APK without them — it crashes at runtime with `UnsatisfiedLinkError`.

Build the native libs before anything else:

- Linux/macOS/WSL2: `./buildLibrespot.sh` from repo root (locates its own JDK 21)
- Windows: `Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process` then `.\build-librespot.ps1`
- Manual (any OS), from `rust/librespot-ffi/`:
  ```bash
  cargo ndk -t arm64-v8a --platform 21 build --release
  cargo ndk -t armeabi-v7a --platform 21 build --release
  ```
  then copy `rust/target/<triple>/release/liblibrespot_ffi.so` into the matching `jniLibs/<abi>/` dir.

Native toolchain prerequisites: Rust 1.90.0 (pinned in `rust/rust-toolchain.toml`, which includes clippy/rustfmt), `cargo install cargo-ndk`, Android NDK r29 (`29.x` under `$ANDROID_SDK_ROOT/ndk/`), and Rust targets `aarch64-linux-android` + `armv7-linux-androideabi`. Env vars: `ANDROID_SDK_ROOT`, `ANDROID_NDK_HOME`.

## Build Commands

```bash
./gradlew assembleDebug        # debug APKs
./gradlew assembleRelease      # release APKs (falls back to debug signing without keystore env vars)
./gradlew build                # full build incl. checks
./gradlew lint                 # Android lint
```

- Gradle side uses **JDK 17** (`jvmToolchain(17)` in `app/build.gradle.kts`; CI installs Temurin 17).
- No unit-test source set currently exists (`app/src/test` is absent); JUnit deps are declared but unused.
- Nightly release pipeline (canonical reference): `.github/workflows/nightly.yml` — cargo ndk both ABIs → drop `.so` into `jniLibs` → `./gradlew assembleRelease`.
- Version bumps happen in `app/build.gradle.kts` (`majorVersion`/`minorVersion`/`patchVersion`; versionCode derived).

## Repository Layout

Three logical modules:

1. **`app/`** — the entire Android app (single Gradle module `:app`). Kotlin + Compose.
2. **`rust/librespot-ffi/`** — Rust crate (cdylib) exposing JNI functions to Kotlin. Wraps librespot plus extra Spotify Web API client logic living in `src/spotify/client/`.
3. **`rust/deps/librespot/`** — git submodule pointing to iTomKo's fork of librespot. Clone with `--recurse-submodules`. It is a workspace member of `rust/Cargo.toml`.

**`Metrolist-main/` is a vendored copy of an unrelated project** (Metrolist YouTube Music client) kept as UI/UX reference only. It is NOT part of the Gradle build (`settings.gradle.kts` includes only `:app`). Never edit it when changing Outify behavior.

## Architecture

### JNI bridge (the core seam)

Kotlin declares `external fun`s in three places; Rust implements each as `Java_cc_tomko_outify_...`:

| Kotlin declaration site | Purpose | Rust implementation |
|---|---|---|
| `LibrespotFfi.kt` (root object) | `libInit(context, clientId, secret)`, credential updates | `lib.rs` |
| `core/SpClient.kt` | Web-API-style ops: search, playlists, saved items, lyrics, radio, remote device playback control, OAuth flow | `jni_impl/spclient.rs`, `jni_impl/oauth.rs`; logic in `spotify/client/*` |
| `core/spirc/Spirc.kt` | Spotify Connect (spirc) session/playback control | `jni_impl/spirc.rs`; wrapper in `spirc.rs` |
| `playback/AudioEngine.kt` | PCM callback + player-event listener registration (GlobalRefs held by Rust) | `jni_impl/playback.rs` |

Conventions crossing the boundary:

- **Data is JSON strings.** Kotlin decodes with kotlinx.serialization (`Json { ignoreUnknownKeys = true; explicitNulls = false }`); Rust serializes with serde. Track metadata JSON flows Rust → Kotlin in `Player.onTrackChange`.
- **Errors are JSON too**: `{"error":{"type":"...","message":"..."}}`. Route results through `SpClient.checkAndHandleError()` which parses via `NativeErrorHandler` and throws `SpClientException`.
- **Callbacks flow Rust → Java** through JNI GlobalRefs: PCM samples into `AudioEngine.onPcmReady` (writes into an `AudioTrack`), player events into `PlayerEventCallback`.
- Rust holds global statics initialized once: Tokio multi-thread runtime, JVM handle, files/cache dirs (`JNI_OnLoad` / `libInit`). `unsafe_code = "forbid"` is set workspace-wide except where JNI requires it.
- Rust-side layout rule: `jni_impl/*.rs` contains JNI-glue only; reusable logic lives in sibling modules (`spotify/client/player.rs`, `session.rs`, `metadata/*`) so it stays testable without a JVM.

### Playback stack

`services/PlaybackService` (Media3 `MediaSessionService`, foreground) → `playback/Player.kt` (extends Media3 `SimpleBasePlayer`, adapts librespot state to Media3) → `AudioEngine` (S16 PCM → `AudioTrack`). Ad blocking happens in `Player`'s event callback (detects `spotify:ad` URIs, auto-skips).

Two control paths exist:
- **Local streaming** via spirc/Spotify Connect: `Session` → `SpircController.start()` wires settings (bitrate, gapless, normalization, device name) into `Spirc.initializeSpirc`; state surfaces through `PlaybackStateHolder` and `VolumeController`.
- **Remote devices** via Web API: `SpClient.startRemotePlayback*` family (requires OAuth login with `user-modify-playback-state` scope). Implemented in `rust/librespot-ffi/src/spotify/client/player.rs`.

### Auth (two independent layers)

1. **Streaming credentials** — librespot session login (works with free accounts, which get premium features).
2. **Web API OAuth** — needed for search/library/playlist edits/profile features. Flow: `SpClient.startOAuthFlow()` → browser/webview → NanoHTTPD localhost server (`AuthCallbackServer`, port 5588, redirect URI `http://127.0.0.1:5588/account/login`) → `completeOAuthFlow(code)`. Users supply their own client ID/secret in Settings > Playback > Advanced (see `docs/CLIENT.md`); defaults are baked into `LibrespotFfi.libInit`.

### App layering (MVVM)

- UI: `ui/screens/*` (Compose) + `ui/viewmodel/*`; navigation via navigation-compose.
- DI: Hilt (`di/AppModule.kt`, SingletonComponent).
- Data: repositories in `data/repository/*` orchestrate Room cache + native calls. Metadata helpers in `data/metadata/*Helper.kt` fetch JSON from Rust and persist into Room (`data/database/AppDatabase`: tracks/albums/artists/playlists/liked). Settings live in DataStore (`SettingsRepository`, `data/setting/*`).
- `data/queue/SavedQueue` + `SavedQueueRepository` persist playback queue across restarts.

## Other Notes

- `TODO` (root, extension-less file) tracks pending work items with pointers into the relevant files.
- ABI splits enabled: `arm64-v8a`, `armeabi-v7a`, plus universal APK. minSdk 26, compile/target SDK 36.
- Module docs live in `docs/modules/`; contributing/build docs in `docs/CONTRIBUTING.md` (note: it contains some stale JDK-version contradictions — trust this file and the build scripts over it).
