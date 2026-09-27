# Build verification — 2026-09-23

* Release APK: `Moonlight-IME-12.2-ime1.apk` (universal, non-root, optimized release).
* Application ID: `com.limelight.redmiime.unofficial`; label: `Moonlight IME`.
* Version: `12.2-ime1`, code 315. Minimum Android API 21; target API 36; compile API 37.
* Native ABIs verified from the packaged APK: arm64-v8a, armeabi-v7a, x86, x86_64.
* APK SHA-256: `27f3e16da4ec8dba0741570673ab8c2f315b3f0a583e201ec94541f48d6bd530`.
* Signing certificate SHA-256: `7a6e099127a0310a346f86347304898ff070c119243e60e8bc2e9960a09ba5e8`.
* Android apksigner verification passed: v1, v2, v3 signatures; 3072-bit RSA personal-build certificate.
* `zipalign -c -P 16 4` verification passed.

## Automated checks

Final Gradle invocation: `:app:testNonRootDebugUnitTest :app:lintNonRootRelease :app:assembleNonRootRelease`.
Result: **BUILD SUCCESSFUL**.

**36 tests, zero failures/errors/skips**: 18 IME regression scenarios each on simulated Android
API 28 and API 35 using Robolectric 4.16.1. Covered ordinary text, punctuation/capitalization,
composing updates, correction/replacement, finish-only composition, swipe/voice-style commits,
UTF-16 and code-point backspace, deletion into pre-existing host text, batches, composing regions,
empty composition, late callbacks, disconnected input, Enter, arrow keys, newline commits,
unsupported local selections, surrogate-safe deltas, and modern Android text-attribute overloads.

These tests exercise Android's InputConnection APIs with a recording host sink. They do not
exercise a real Gboard installation, network transmission, Sunshine, or a Windows application.

Full Android lint completed with **0 errors and 185 warnings**. No remaining warnings are located
in the new IME/overlay classes or modified StreamView. Lint also logged a non-fatal quick-fix report
generation exception for an existing Turkish translation; it still produced its reports and succeeded.
Gradle emitted upstream deprecation notices about future Gradle 10 compatibility.

The build used Temurin JDK 17.0.20.1, upstream Gradle 9.7.1 / AGP 9.4.0, Android SDK platform
37.0, build-tools 36.0.0, and NDK 29.0.14206865 on Windows. JDK archive checksum was checked
against Adoptium's published API checksum. No upstream build/dependency versions were downgraded.

## Unverified on hardware

No Android phone or emulator was attached (`adb devices` listed no devices). Installation,
Gboard UI behavior, live Sunshine input, decoder/audio behavior, Redmi rotation/insets, and Tailscale
streaming need the supplied phone-to-Windows checklist. No Sunshine installation/settings were modified.

The signing key remains privately in the local build workspace and is not included in the source
archive. Keep the same key for future in-place updates; do not distribute it with the public source.
