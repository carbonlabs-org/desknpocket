#!/usr/bin/env bash
# Build only the upstream network library; no account, signing or device enrollment.
set -euo pipefail
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
revision=803d93860da0652d501298883fcc61c70973290b
expected_toolchain=24ee2fd0610e6c505ee4ec061a81215afe119d1f
source_dir=${1:?Usage: bash build-tailscale-android.sh SOURCE_DIR LINUX_SDK_DIR OUTPUT_DIR}
sdk_dir=${2:?A Linux Android SDK is required}
output_dir=${3:?An output directory is required}
[[ $(uname -s) == Linux && $(uname -m) == x86_64 ]] || { echo 'Requires x86_64 Linux.' >&2; exit 1; }
for executable in git make curl unzip zip java javac sha256sum python3; do
    command -v "$executable" >/dev/null || { echo "Missing $executable" >&2; exit 1; }
done
source_dir=$(realpath "$source_dir")
sdk_dir=$(realpath "$sdk_dir")
mkdir -p "$output_dir"
output_dir=$(realpath "$output_dir")
[[ $(git -C "$source_dir" rev-parse HEAD) == "$revision" ]] || { echo 'Wrong upstream revision.' >&2; exit 1; }
git -C "$source_dir" diff --quiet HEAD -- || { echo 'Source has tracked modifications; refusing unrecorded build.' >&2; exit 1; }
[[ $(cat "$source_dir/go.toolchain.rev") == "$expected_toolchain" ]] || { echo 'Unexpected Go toolchain pin.' >&2; exit 1; }
export ANDROID_HOME="$sdk_dir"
export ANDROID_SDK_ROOT="$sdk_dir"
export NDK_ROOT="$sdk_dir/ndk/23.1.7779620"
export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
export GOTOOLCHAIN=local
export TS_USE_TOOLCHAIN=1
# gomobile builds four architectures; bound each compiler on memory-limited WSL.
export GOMAXPROCS=2
export GOFLAGS=-p=2
# Avoid inheriting an unrelated Go toolchain override.
unset TOOLCHAINDIR TOOLCHAIN_DIR
[[ -x "$NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/clang" ]] || { echo 'Pinned Linux NDK 23.1.7779620 missing.' >&2; exit 1; }
[[ -f "$sdk_dir/platforms/android-36/android.jar" ]] || { echo 'Android platform 36 missing.' >&2; exit 1; }
cd "$source_dir"
make libtailscale 2>&1 | tee "$output_dir/build.log"
aar="$source_dir/android/libs/libtailscale.aar"
python3 "$script_dir/verify-network-aar.py" "$aar" | tee "$output_dir/verification.json"
cp "$aar" "$output_dir/libtailscale.aar"
cp LICENSE "$output_dir/TAILSCALE-ANDROID-LICENSE"
cp go.mod go.sum go.toolchain.rev "$output_dir/"
./tool/go version > "$output_dir/go-version.txt"
./tool/go list -m all > "$output_dir/go-modules.txt"
printf '%s\n' "$revision" > "$output_dir/source-revision.txt"
(cd "$output_dir" && sha256sum libtailscale.aar > SHA256SUMS)
echo 'Library build complete. APK integration and device validation remain separate steps.'
