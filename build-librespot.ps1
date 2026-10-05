$ErrorActionPreference = "Stop"

$AbiMap = @{
    "arm64-v8a"    = "aarch64-linux-android"
    "armeabi-v7a"  = "armv7-linux-androideabi"
}

$ProjectRoot = Get-Location
$LibrespotDir = Join-Path $ProjectRoot "rust\librespot-ffi"
$OutputDir = Join-Path $ProjectRoot "app\src\main\jniLibs"
$PlatformVersion = 21

if (-not $env:ANDROID_SDK_ROOT) {
    Write-Host "ANDROID_SDK_ROOT is not set"
    exit 1
}

$NdkBase = Join-Path $env:ANDROID_SDK_ROOT "ndk"

if (-not (Test-Path $NdkBase)) {
    Write-Host "No NDK directory found at $NdkBase"
    exit 1
}

# Pick latest NDK 29.x
$NdkPath = Get-ChildItem $NdkBase -Directory |
    Where-Object { $_.Name -like "29.*" } |
    Sort-Object Name |
    Select-Object -Last 1

if (-not $NdkPath) {
    Write-Host "No Android NDK 29.x found"
    exit 1
}

$ANDROID_NDK = $NdkPath.FullName

Write-Host "Using ANDROID_NDK=$ANDROID_NDK"

$Toolchain = Join-Path $ANDROID_NDK "toolchains\llvm\prebuilt\windows-x86_64\bin"

if (-not (Test-Path $Toolchain)) {
    Write-Host "NDK toolchain not found for Windows at $Toolchain"
    exit 1
}

$env:Path = "$Toolchain;$env:Path"

$env:ANDROID_NDK_HOME = $ANDROID_NDK

foreach ($abi in $AbiMap.Keys) {

    $triple = $AbiMap[$abi]

    Write-Host "Building librespot for $abi ($triple)..."

    Set-Location $LibrespotDir

    cargo ndk `
        -t $abi `
        --platform $PlatformVersion `
        build --release

    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to build for $abi"
        exit 1
    }

    Set-Location $ProjectRoot

    $TargetDir = Join-Path $LibrespotDir "..\target\$triple\release"
    $SoFile = Join-Path $TargetDir "liblibrespot_ffi.so"

    $OutAbiDir = Join-Path $OutputDir $abi
    New-Item -ItemType Directory -Force -Path $OutAbiDir | Out-Null

    Copy-Item $SoFile $OutAbiDir -Force
}

Write-Host "Build completed successfully!"
