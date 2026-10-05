#!/usr/bin/env bash

set -e

# -- Checking for SDK Manager
if command -v sdkmanager >/dev/null 2>&1; then
	echo "SDK Manager is installed.."
else
	echo "SDK Manager is not installed!"
	exit 1
fi

if [ -z "$ANDROID_SDK_ROOT" ]; then
	if ls "/opt/android-sdk/" >/dev/null 2>&1; then
		ANDROID_SDK_ROOT="/opt/android-sdk"
	else
    echo "ANDROID_SDK_ROOT is not set!"
    exit 1
	fi
fi
NDK_DIR="$ANDROID_SDK_ROOT/ndk"

# -- Checking for NDK r29
if ls "$NDK_DIR"/29*/source.properties >/dev/null 2>&1; then
	echo "Android NDK r29 is installed.."
else
	echo "Android NDK r29 is not installed!"
	exit 1
fi

# -- Checking for rustup
if command -v rustup >/dev/null 2>&1; then
	echo "rustup is installed.."
else
	echo "rustup is not installed!"
	exit 1
fi

# -- Checking for cargo
if command -v cargo >/dev/null 2>&1; then
	echo "cargo is installed.."
else
	echo "cargo is not installed!"
	exit 1
fi

echo "Installing Rust Android Targets.."

rustup target add \
	armv7-linux-androideabi \
	aarch64-linux-android \
	i686-linux-android \
	x86_64-linux-android

if [ "$?" -ne 0 ]; then
	echo "Failed to install Rust Android Targets!"
	exit 1
fi

echo "Installing Cargo NDK.."
cargo install cargo-ndk

if [ "$?" -ne 0 ]; then
	echo "Failed to install Cargo NDK!"
	exit 1
fi

echo "Environment preparation complete!"
