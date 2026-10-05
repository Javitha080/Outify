### Modules
Outify composes of these modules:
- [app](https://github.com/iTomKo/Outify/tree/master/docs/modules/app.md): the UI itself, written in Kotlin.
- [rust/librespot-ffi](https://github.com/iTomKo/Outify/tree/master/docs/modules/ffi.md): Rust backend utilizing JNI to bridge librespot with the UI
- [rust/librespot](https://github.com/iTomKo/Outify/tree/master/docs/modules/librespot.md): fork of [librespot-org/librespot](https://github.com/librespot-org/librespot), serves as the main backend

For more information about the modules take a look at their docs.

### Prerequisites
- [Rust toolchain (1.90.0)](https://rustup.rs/)
- cargo-ndk
```bash
cargo install cargo-ndk # Download
cargo ndk --version # Verify installation
````
- Android SDK
    - including NDK, platform-tools
- Android NDK (29.0.13113456 - r29)
    - Android Studio: SDK Manager -> SDK Tools -> NDK
    - CLI: `sdkmanager "ndk;29.0.13113456"`
- Rust targets
    - `aarch64-linux-android`
    - `armv7-linux-androideabi`
    - Install via `rustup`:
        - `rustup target add aarch64-linux-android`
        - `rustup target add armv7-linux-androideabi`
- Environment variables set
    - `ANDROID_SDK_ROOT` - e.g. `/opt/android-sdk/`
    - `ANDROID_NDK_HOME` - e.g. `/opt/android-ndk/`
    - `JAVA_HOME` - e.g. `/usr/lib/jvm/java-21-openjdk`

### Building from source
Prerequisites:
- JDK21 in `$JAVA_HOME`
- Gradle
- Cargo (for librespot-ffi, librespot)
- Linux/WSL2 - for `./buildLibrespot.sh`

When building from source, please clone the repository with submodules.
```bash
git clone --recurse-submodules https://github.com/iTomKo/Outify
```

Make sure you have JDK17 in `$JAVA_HOME`.

Next step is: __Building Rust backend__

> [!NOTE]
> Without built Rust backend the app **will not work**!

#### Building Rust backend (Linux)
Run `./buildLibrespot.sh` (a bash script) from the repository root.
Note that this can take a while when running for the first time.
This script automatically builds the `.so` library files and moves them to the appropiate place.

#### Building Rust backend (Windows)
> [!NOTE]
> This built has not been manually tested as we do not use Windows on any machine

Run the build PowerShell script from the repository root.
Note that this can take a while when running for the first time.
This script automatically builds the `.so` library files and moves them to the appropiate place.

```bash
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
.\build-librespot.ps1
```

#### Building Rust backend (Any OS)
We recommend using WSL2 if possible and then using the Linux way.
You can still build it manually.

Ensure you have prerequisities installed and working as expected - including environmental variables.
Note that this can take a while when running for the first time.

I. Change directory to `librespot-ffi`:

  ```bash
  cd rust/librespot-ffi
  ```
IIa. Build ARM64

  ```bash
  cargo ndk \
      -t arm64-v8a \
      --platform 21 \
      build --release
  ```
IIb. Build ARMv7

  ```bash
  cargo ndk \
      -t armeabi-v7a \
      --platform 21 \
      build --release
  ```
III. Create Android folders

  ```bash
  cd .. # Navigate to Outify root
  mkdir -p app/src/main/jniLibs/arm64-v8a
  mkdir -p app/src/main/jniLibs/armeabi-v7a
  ```
IV. Copy built `.so`

  ```bash
  cp rust/target/aarch64-linux-android/release/liblibrespot_ffi.so \
    app/src/main/jniLibs/arm64-v8a/

  cp rust/target/armv7-linux-androideabi/release/liblibrespot_ffi.so \
    app/src/main/jniLibs/armeabi-v7a/
  ```

#### Building the app
- from Android Studio
- using `./gradlew build`

## Troubleshooting
In case of failed Rust backend:
- if it failed due to error in code - it will most likely be fixed in upcoming patches. If not, create issue.
- if it failed due to error during build - ensure you have all prerequisites installed and set.
    - Note, that we recommend using the `./buildLibrespot.sh` script via Linux CLI or WSL2
2
