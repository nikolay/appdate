#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_JAVA_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home"
SDK_DIR="${ANDROID_HOME:-${HOME}/Library/Android/sdk}"
BUILD_TOOLS="${BUILD_TOOLS:-${SDK_DIR}/build-tools/36.1.0}"
OUT_DIR="${ROOT_DIR}/build/outputs/apk"

APKSIGNER="${BUILD_TOOLS}/apksigner"

for tool in "$APKSIGNER"; do
  if [[ ! -e "$tool" ]]; then
    echo "Missing required Android SDK file: $tool" >&2
    exit 1
  fi
done

if [[ -z "${JAVA_HOME:-}" && -d "$DEFAULT_JAVA_HOME" ]]; then
  export JAVA_HOME="$DEFAULT_JAVA_HOME"
fi

cd "$ROOT_DIR"
ANDROID_HOME="$SDK_DIR" ./gradlew :app:assembleDebug

mkdir -p "$OUT_DIR"
cp "$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk" "$OUT_DIR/appdate-debug.apk"

"$APKSIGNER" verify --verbose "$OUT_DIR/appdate-debug.apk"

if [[ -d "$ROOT_DIR/site/downloads" ]]; then
  cp "$OUT_DIR/appdate-debug.apk" "$ROOT_DIR/site/downloads/appdate-debug.apk"
fi

echo "Built $OUT_DIR/appdate-debug.apk"
