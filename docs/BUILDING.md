# Build the Android development preview

This publication snapshot contains the current custom client in `android/`, including its uncommitted IME/network work at export time. It does not contain the SDK, JDK, caches, credentials or generated Tailscale AAR. The native common-c, ENet and nanors submodule contents are flattened into the source tree; no submodule initialization is needed for this snapshot.

## Android toolchain

Use JDK 17, Android platform `platforms;android-37.0`, build-tools `36.0.0` and NDK `29.0.14206865`. The checked-in Gradle wrapper pins Gradle; the project pins AGP. Dependencies are resolved from Google Maven, Maven Central and JitPack. Configure `android/local.properties` with `sdk.dir` pointing to your Android SDK, escaping Windows backslashes as required by Java properties. Keep this machine-specific file untracked.

The network flavor requires Android API 26+ and targets API 36. The legacy nonRoot flavor has a lower minimum but does not embed Tailscale.

## Build the network library on Linux

Use x86_64 Linux with JDK 21, git, make, gcc, libc6 development files, curl, unzip, zip and Python 3. Provision a separate Linux Android SDK with NDK `23.1.7779620`, `platforms;android-36` and build-tools `36.0.0`. Accept the SDK terms in that environment.

```bash
git clone https://github.com/tailscale/tailscale-android.git tailscale-android
git -C tailscale-android checkout 803d93860da0652d501298883fcc61c70973290b
bash scripts/build-tailscale-android.sh "$PWD/tailscale-android" /path/to/linux-sdk /path/to/output
```

The wrapper checks the upstream source and Go compiler pins, bounds concurrency and verifies the AAR's four ABIs and 16 KB ELF alignment. Copy the resulting `libtailscale.aar` into `android/app/libs/`. Retain the accompanying revision, Go module graph, compiler details and license for release provenance. Fetches require network access.

The local reference AAR has SHA-256 `a2958859073a3a9dc938141fc71300281222d446db9ce311623e33efad738c52`. Byte-identical reproduction on another machine has not been established; the Windows validation wrapper rejects a different hash. Review any rebuilt library's provenance and verification report before changing that pin.

## Validate on Windows

The project validation entry point supports explicit paths. Its toolchain directory layout is `jdk/jdk-17.0.20.1+1`, `sdk` and `gradle-cache`:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Build-Android.ps1 -ClientPath ./android -ToolchainPath C:/path/to/toolchain -NetworkSpike
```

For a normal separately installed SDK/JDK, set `JAVA_HOME`, `ANDROID_HOME` and `ANDROID_SDK_ROOT` to those installations, configure `local.properties`, and run the same Gradle tasks from `android/`:

```powershell
./gradlew.bat :app:testNonRootDebugUnitTest :app:lintNonRootRelease :app:assembleNonRootRelease :app:testNetworkDebugUnitTest :app:lintNetworkDebug :app:assembleNetworkDebug --no-daemon
```

The integrated APK is `android/app/build/outputs/apk/network/debug/app-network-debug.apk`. The nonRoot release APK is a separate legacy client and is not the integrated Network Lab deliverable. Network debug assembly uses the developer machine's Gradle debug key; this repository supplies no signing keys. A different signing certificate requires uninstalling the existing app before installation, losing its local state.

The native tree retains upstream static libraries plus corresponding OpenSSL/Opus source snapshots and build scripts under `android/third_party/native-sources/`. See that directory's README for rebuilding them. Do not confuse source presence with a completed audit of every packaged dependency notice.

The OpenSSL source archive is published in seven byte-exact parts to fit the upload transport. Before extracting it on Linux, run from the repository root:

```bash
cd android/third_party/native-sources
cat openssl-4.0.2.tar.gz.part0{1..7} > openssl-4.0.2.tar.gz
```

The source manifest checks each published part. The archive contains the same upstream source and license as the unsplit local snapshot. The ordinary APK build uses the included static libraries and does not need to extract this archive.

## Device verification

A passing build or unit test does not establish a working desktop session. Follow the quickstart on actual hardware and record video/control, audio, IME composition/correction/deletion, Wi-Fi and cellular reconnect, rotation, competing VPN, permission revocation and restart results separately. Publish exact version/hash information with test results.
