# Desknpocket for Android

**Your PC desktop on your Android device. A Carbon Labs project.**

Desknpocket Network Lab combines Moonlight streaming, our custom Android keyboard/IME bridge and embedded Tailscale networking in one Android application.

## Current setup

| Device | Install |
|---|---|
| Windows PC | Sunshine and Tailscale, separately |
| Android 8.0+ | Desknpocket Network Lab |

**The integrated Desknpocket Desktop application is still in development.** It has unresolved reliability problems and is outside this Android publication. Use Sunshine and Tailscale on the PC while desktop work continues. Our long-term goal is one Desknpocket application on each device.

Read the [setup and troubleshooting guide](docs/ANDROID-QUICKSTART.md) to enroll Android, add your PC and pair with Sunshine.

## Development preview

The current version is `12.2-ime1-network2` (316), with Android package `local.desknpocket.networkspike.debug`. The project owner has reported successful sign-in, pairing and PC control. Broader device testing remains in progress, including audio/IME regression checks, cellular reconnection, process death and battery restrictions. No universal reliability or latency claim is made.

This source publication does not itself provide a production-signed APK. Existing local builds use Gradle debug signing. A downloadable release needs a maintained signing/update strategy and a complete dependency notice/source bundle.

The preview needs manual PC address entry and Sunshine PIN pairing. It has no logout/account-switching UI, always-on VPN or custom control-server support. After app restart, start the VPN from the app. Another Android VPN may disconnect it.

## Build and contribute

See [build instructions](docs/BUILDING.md). `android/` contains the custom client, including the original IME bridge and native streaming implementation. Native submodule sources are included directly in this snapshot. `SOURCE-PROVENANCE.txt` and `SOURCE-SHA256SUMS.txt` record its origin and exact client files.

The files `android/CUSTOM-IME.md`, `android/BUILD-VERIFICATION.md` and `android/SOURCE-REVISIONS.txt` preserve the earlier standalone IME client's history. Their APK names, external Android VPN instructions and references to a separately supplied patch describe that earlier build. Use this repository's root README and `docs/` for Network Lab instructions. The full modified source is included here; no separate IME patch is needed.

Preserve the streaming sockets, keyboard behavior and upstream license notices when contributing. Include reproduction steps and distinguish unit-test/build results from real-device results in changes and bug reports. Never attach authentication links, credentials or private network information.

## License and credits

This Android derivative is distributed under GNU GPL version 3; see [LICENSE.txt](LICENSE.txt). Existing file-level grants and third-party licenses remain applicable, including files offering GPL-3.0-or-later. Newly authored Carbon Labs publication documentation and build/export helpers are provided under GPL-3.0-or-later.

Built on [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) and [Tailscale Android](https://github.com/tailscale/tailscale-android), connecting to [Sunshine](https://github.com/LizardByte/Sunshine). Desknpocket is an independent project and is not an official release of those projects. Upstream documentation remains in `android/README.md`; its upstream download links are not Desknpocket downloads. Native notices and source archives remain in the Android tree. Tailscale source retains its BSD license; the hosted service is provided separately by Tailscale.
