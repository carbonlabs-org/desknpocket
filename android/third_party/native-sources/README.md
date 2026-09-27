# Native dependency sources

The APK uses the unmodified static libraries already supplied by official Moonlight Android.
Upstream commit `31b700304a9eb2827516f52c33d9da0845fc1ece` identifies them as Opus 1.6.1 and OpenSSL 4.0.2.

The source snapshots and Android build scripts here were obtained from upstream's documented
dependency builder, https://github.com/cgutman/moonlight-mobile-deps at
`419349aa689e8749a9fb91a22dc45531ddd3df45`:

* OpenSSL: https://github.com/openssl/openssl at `f089acdf4bc7ba94a79f4bf6eb7362c3e7d14aa9` (4.0.2).
* Opus: https://github.com/xiph/opus at `22244de5a79bd1d6d623c32e72bf1954b56235be` (1.6.1).

Each tar.gz is a `git archive` of that commit, including its license. OpenSSL's optional external
test/fuzz submodules are not used by the no-tests/no-apps Android library build and are not included.
The project also contains the full checked-out Moonlight common C, enet, and nanors sources and licenses.

To rebuild these static dependencies, extract each archive into an `openssl`/`opus` directory next
to the supplied corresponding shell scripts, use Linux with NDK 29 and the standard upstream
Perl/autotools/make prerequisites, and follow the moonlight-mobile-deps build instructions. Normal
APK builds use the included upstream static libraries and do not need this extra step.
