#!/usr/bin/env bash
# Builds the signed APK and publishes it as the latest GitHub release.
# Signed with ccez-tube's key, so it lives where that key does (y8,
# ~/.config/ccez-tube) and the phone's ccez-tube may listen without a
# list: run from a clean checkout of a pushed commit,
#   fleet run --on y8 --snapshot -- scripts/release.sh
set -euo pipefail
cd "$(dirname "$0")/.."
KEYDIR="${KEYDIR:-$HOME/.config/ccez-tube}"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
pushed() { gh api "repos/chris-straka/ccez-whisper/commits/$1" --jq .sha >/dev/null 2>&1; }
SHA=$(git rev-parse HEAD)
if ! pushed "$SHA" && git diff --quiet HEAD^ HEAD && pushed "$(git rev-parse HEAD^)"; then
  SHA=$(git rev-parse HEAD^)
fi
pushed "$SHA" || { echo "push $SHA first: the release points at it"; exit 1; }
git submodule update --init --depth 1 third_party/whisper.cpp

N=$(git rev-list --count "$SHA")
export CCEZ_WHISPER_VERSION_CODE="$N"
export CCEZ_WHISPER_VERSION_NAME="0.1.$N"
export CCEZ_WHISPER_KEY_PASSWORD="$(cat "$KEYDIR/key-password")"
cp "$KEYDIR/release.keystore" android/release.keystore
trap 'rm -f android/release.keystore' EXIT

echo "sdk.dir=$SDK" > android/local.properties
(cd android && ./gradlew --console=plain -q assembleRelease)
APK="ccez-whisper-v0.1.$N.apk"
cp android/app/build/outputs/apk/release/app-release.apk "$APK"
gh release create "v0.1.$N" "$APK" -R chris-straka/ccez-whisper --target "$SHA" --title "v0.1.$N" --notes "" --latest
rm -f "$APK"
