# Moonlight IME — unofficial Android client

Modified 2026-09-23 from official Moonlight Android 12.2, commit
`b48494cb96bff23d8886c4775cc4f39a1075495d` (the upstream HEAD checked during this build).
This is an independent personal build, not an official Moonlight release.

## What changed

* A floating **⌨** button in the upper-right of the stream opens/closes the normal Android keyboard. Select Gboard as your Android keyboard. The existing three-finger keyboard gesture uses the same toggle.
* The **⋯** button expands a horizontally scrollable desktop-key strip: Esc, Ctrl, Alt, Win, Shift, Tab, arrows, Del. Tap modifiers to arm them (blue); tap again to cancel. They apply to **one** following key and then release. Combine multiple modifiers before that key.
* A full Android `InputConnection` keeps composing spans and recent local text. Composing updates appear immediately on Windows; committed text reconciles against that text rather than duplicating it. The connection supports `commitText`, `setComposingText`, `finishComposingText`, composing regions, batch edits, deletion in UTF-16/code points, IME key events, and Enter actions.
* Text uses the existing `NvConnection.sendUtf8Text()` protocol. Desktop controls use existing keyboard down/up packets. No new server, protocol, clipboard service, Accessibility Service, microphone permission, or system-overlay permission was added.
* Local editing context is invalidated on desktop touch/mouse interaction, physical keyboard input, navigation controls, focus loss, and stream termination. Closed connections reject late IME callbacks. Text is not written to logs or files.
* The fork has its own app ID/name, so the official Moonlight app remains installed independently. Stream rendering, decoding, audio, pairing, networking, and controller implementation are unchanged.

## Installation and use

1. Copy the signed `Moonlight-IME-12.2-ime1.apk` to your Redmi and open it. Allow installation from the particular Files/browser app when Android asks. No root is needed. Minimum Android version is 5.0; the universal APK includes ARM64 and ARM32 as well as x86 architectures.
2. Open **Moonlight IME**. Its pairing/settings data are separate from official Moonlight. Add the same Windows PC and pair normally using your existing Sunshine pairing page. This adds a client; it does not replace or reconfigure Sunshine.
3. For internet access, keep Tailscale connected on both devices and add the PC's Tailscale address as you already do. No port forwarding is needed for this setup.
4. Start the usual Desktop/app stream. Tap the target Windows text field, then **⌨**. Gboard should appear over the stream. Press **⌨** again or Android Back to close it.
5. Press **⋯** for desktop keys. Examples: **Ctrl → C**, **Ctrl → V**, **Alt → Tab**, **Win → R**, **Ctrl → Shift → Esc**. Letters come from Gboard. For another Alt+Tab, arm Alt again.
6. The keyboard covers part of the stream without resizing the video surface. You can use Gboard's own floating keyboard mode to reveal more of the desktop.

## Phone-to-Windows test (about five minutes)

1. On the LAN, stream the desktop, open Notepad, and focus its document. Press **⌨**.
2. Type `Hello Redmi! 123 @#$% & (test), punctuation.` Check uppercase/lowercase, spaces, number/symbol pages, and no duplicated words. Press Enter, type another line, and hold/tap Backspace.
3. Type a misspelled word, pick a Gboard correction, swipe several words, then use Gboard's microphone to dictate a short sentence if voice input is enabled. Verify each final word appears once. Gboard owns voice permissions and availability.
4. Type a partial word, switch to another Windows field using touch, and type again. Text/corrections must affect only the new field. Hide/reopen Gboard and repeat.
5. Use Ctrl+A, Ctrl+C, Enter, Ctrl+V in Notepad. Use Win+R, type `notepad`, and press Enter. Test Alt+Tab and Ctrl+Shift+Esc. After each shortcut, plain letters must type normally (no stuck modifiers).
6. Test Esc, Tab, arrows, and Del. Rotate the phone, hide/show the toolbar, background/return to the app, and reconnect. Verify normal video, audio, touch/mouse, and any controller you use.
7. Repeat ordinary typing over mobile data with Tailscale on both devices, using the PC's Tailscale address.

## Known limitations

* A physical Redmi/Gboard/Sunshine session was not available on the build machine. Automated Android IME tests are not an end-to-end device test; the procedure above is still required.
* GameStream provides input injection, not the Windows document/caret/selection. Gboard can edit the locally remembered suffix; it cannot read old Windows text. Gboard spacebar cursor dragging/arbitrary selection is deliberately not emulated. Use the desktop arrows or touch, then resume typing with fresh context.
* Changes to the Windows caret made by another device/application cannot be detected. Tap the desired Windows field again before typing if the remote caret moved externally.
* Live autocorrection rewrites the changed suffix using Backspace plus Unicode text. Unicode combining sequences/emoji and apps that treat Backspace unusually can differ from ordinary Notepad. Test the languages/apps you use. Games that require physical key events may reject Unicode typing; the toolbar is for desktop keys, not a full gaming keyboard.
* Shortcuts accept one ASCII letter, digit, space, or a toolbar key. Swipe/voice/multi-character commits are not shortcut input. Ctrl+V pastes the **Windows clipboard**; no phone-to-PC clipboard synchronization was added. Windows secure-attention sequences such as Ctrl+Alt+Del are not implemented.
* Modifiers are one-shot, not locked. Using a hardware keyboard and the toolbar simultaneously is not supported as a combined modifier source. Hardware input otherwise follows upstream behavior.
* Remote password fields cannot advertise their password status to Gboard. The client requests no personalized learning, but Gboard's own suggestions/voice/cloud settings remain under your control.
* The button/toolbar are fixed near the top-right, and the toolbar scrolls horizontally on narrow screens. The strip is optional via **⋯**; no persistent custom layout editor was added.

## Source, license, and attribution

The supplied source archive contains the modified project, all checked-out native submodules, original license files, original README/author attribution, and tests. `CUSTOM-IME.patch` records the changes against the commit above.

Moonlight Android is GPL-3.0 licensed; see `LICENSE.txt`. Original authors credited upstream include Cameron Gutman, Diego Waxemberg, Aaron Neyer, and Andrew Hennessy, along with upstream contributors. These custom input/UI changes were developed for this personal build on 2026-09-23 and are provided under GPL-3.0-or-later. Existing component licenses remain in force. No warranty is provided.

When sharing this APK, provide recipients its corresponding source (this source archive), license, build instructions, and notices. Do not present it as an official release. Signing keys and downloaded SDK/JDK binaries are not included in the source archive.

## Research reviewed before implementation

* [Official Moonlight Android](https://github.com/moonlight-stream/moonlight-android): `StreamView`, `Game` input handling, `NvConnection`, native keyboard/text paths, build files.
* [PR #1556](https://github.com/moonlight-stream/moonlight-android/pull/1556/files): `commitText` routing idea. Its discarded composing text and stateless deletion were not adopted.
* [PR #1583](https://github.com/moonlight-stream/moonlight-android/pull/1583/files) and [issue #1579](https://github.com/moonlight-stream/moonlight-android/issues/1579): visible keyboard toggle and gesture conflicts.
* [Issue #1496](https://github.com/moonlight-stream/moonlight-android/issues/1496), [#1106](https://github.com/moonlight-stream/moonlight-android/issues/1106), and [#802](https://github.com/moonlight-stream/moonlight-android/issues/802): software keyboard access, dismissal, and symbol mapping concerns.
* [Artemis source](https://github.com/ClassicOldSong/moonlight-android): reviewed `StreamView`; kept the upstream client/server architecture.
* [lyc8503 keyboard fork](https://github.com/lyc8503/moonlight-android-no-keyboard-shortcut) and [Skylark keyboard fork](https://github.com/KaleidSkylark/moonlight-android-full-keyboard-skylark): reviewed hardware-keyboard approach/source. Their Accessibility Service approach is unnecessary for this on-screen toolbar and was not adopted.
* [Android InputConnection documentation](https://developer.android.com/reference/android/view/inputmethod/InputConnection).

The custom implementation was written in this project; no third-party fork was merged or executed.

## Rebuilding

Install JDK 17 and Android SDK command-line tools. Install `platforms;android-37.0`, `build-tools;36.0.0`, and `ndk;29.0.14206865` with sdkmanager. The platform uses the SDK package name `android-37.0` even though the Gradle compile API is 37. Accept the relevant SDK licenses. Create `local.properties` with `sdk.dir=C\:/path/to/Android/sdk` (or the corresponding path on your OS).

The native source is already included in the archive. If cloning instead, initialize submodules recursively, check out the upstream commit above, and apply the patch.

Run `gradlew.bat :app:testNonRootDebugUnitTest :app:assembleNonRootRelease` on Windows (`./gradlew` elsewhere). The pinned upstream Gradle wrapper downloads Gradle 9.7.1; the project uses Android Gradle Plugin 9.4.0. Network access to Google Maven, Maven Central, JitPack, and Gradle distribution hosting is needed on first build.

The release output is unsigned. Use Android `zipalign`, then `apksigner sign` with your own key, followed by `apksigner verify --verbose`. To update the delivered APK in place, you need the same signing key used for this build; a differently signed build requires uninstalling this fork first, which removes its pairing/settings. The official Moonlight installation is separate.
