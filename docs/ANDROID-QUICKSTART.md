# Use Desknpocket on Android

Desknpocket Network Lab combines the custom Moonlight streaming/keyboard client and Tailscale networking in one Android app. It is an early development preview from Carbon Labs. The integrated desktop app is still in development; use separate Sunshine and Tailscale installations on your PC for now.

## What you need

- An Android 8.0+ phone or tablet (API 26+).
- A Windows PC capable of running Sunshine, awake with a usable desktop/display and Internet access.
- Sunshine and Tailscale installed on that PC.
- Both devices enrolled in the same Tailscale network, with permission to communicate.
- A Desknpocket Network Lab APK built from this source or a published Carbon Labs release. The publication candidate is version `12.2-ime1-network2` (316). No public download is available until a release is actually published.

## Set up the PC

1. Install Sunshine from its [official releases](https://github.com/LizardByte/Sunshine/releases) using the [upstream setup guide](https://docs.lizardbyte.dev/projects/sunshine/latest/md_docs_2getting__started.html). Complete its local web setup on the PC at `https://localhost:47990` and configure credentials. A certificate warning for this local self-signed page is expected; confirm the address before proceeding.
2. Install and sign in to [Tailscale for Windows](https://tailscale.com/docs/install/windows). Note the PC's Tailscale IPv4 address, generally beginning with `100.`. Use this address when adding the PC on Android.
3. Keep Sunshine and Tailscale running. If you previously tested the integrated Desknpocket Desktop preview, stop that preview and its bundled services before using the separate applications. Avoid running competing Sunshine hosts. Existing preview and standalone pairing/configuration may differ.

Use the upstream Sunshine firewall configuration and your tailnet access policy. Do not expose the Sunshine administration page publicly or disable the firewall wholesale. This setup uses Tailscale connectivity and does not require public router port forwarding.

## Connect Android

1. Install and open **Desknpocket Network Lab**. If sideloading an APK, allow installation for the app that opens the downloaded file.
2. Tap **Sign in** and approve Android VPN consent. Android permits one active VPN per profile; this can disconnect a separate Tailscale or other VPN app. Desknpocket supplies its own VPN for its streaming traffic.
3. Tap **Continue sign-in in browser** once enabled. Choose the account/network used by the PC and complete device approval if required. Return to Desknpocket.
4. Wait for **Backend Running** and an active VPN tunnel. These indicators show networking state; the streaming test below confirms a usable session.
5. Tap **Open desktops**, use the add-PC control, and enter the PC's Tailscale IPv4 address. Automatic LAN discovery may not find a PC across Tailscale.
6. Select the PC. Enter the PIN shown by the Android app into Sunshine's **PIN** page on the PC to approve pairing.
7. Open **Desktop** (or an application configured in Sunshine). Confirm that video appears and that touch/mouse and keyboard input control the PC.
8. Test audio and text entry. Then put the phone on mobile data while keeping the PC online on its own connection, reconnect, and repeat the checks.

## Reconnect and troubleshoot

- After restarting the Android app, tap **Start VPN service**, then **Open desktops**. This preview does not automatically start the tunnel after process restart or reboot.
- **NeedsLogin:** complete browser sign-in. If device approval is required, approve it in the intended tailnet.
- **PC unavailable:** confirm the PC is awake, both devices are in the intended tailnet, the saved address is the PC's current Tailscale address, and Sunshine/firewall/access rules permit the connection.
- **Pairing fails:** submit the newly shown PIN to the Sunshine instance actually running on the PC. Do not use the stopped desktop preview's pairing interface.
- **Network is connected but video fails:** inspect Sunshine's capture/encoder/display status. VPN state alone does not prove streaming works.
- **Connection drops after another VPN starts:** reopen Desknpocket and approve/start its VPN again.

There is no in-app logout/account-switching UI yet. **Stop VPN service** stops the connection but does not remove enrollment. Always-on VPN and custom control servers are not supported by this preview. Wi-Fi/cellular handover, process death, battery restrictions and repeated reconnects still need broader device testing.

When reporting a bug, include Android/Windows versions, app version, Sunshine/Tailscale versions, reproduction steps and whether the phone was on Wi-Fi or cellular. Redact account details, authentication links, keys and private device/network information.
