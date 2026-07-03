#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_DIR="${ANDROID_HOME:-${HOME}/Library/Android/sdk}"
BUILD_TOOLS="${BUILD_TOOLS:-${SDK_DIR}/build-tools/36.1.0}"
PLATFORM="${ANDROID_PLATFORM:-${SDK_DIR}/platforms/android-36}"
APP_DIR="${ROOT_DIR}/app"
BUILD_DIR="${ROOT_DIR}/build/manual"
OUT_DIR="${ROOT_DIR}/build/outputs/apk"
KEYSTORE="${ROOT_DIR}/build/debug.keystore"

AAPT2="${BUILD_TOOLS}/aapt2"
D8="${BUILD_TOOLS}/d8"
ZIPALIGN="${BUILD_TOOLS}/zipalign"
APKSIGNER="${BUILD_TOOLS}/apksigner"
ANDROID_JAR="${PLATFORM}/android.jar"

for tool in "$AAPT2" "$D8" "$ZIPALIGN" "$APKSIGNER" "$ANDROID_JAR"; do
  if [[ ! -e "$tool" ]]; then
    echo "Missing required Android SDK file: $tool" >&2
    exit 1
  fi
done

rm -rf "$BUILD_DIR" "$OUT_DIR"
mkdir -p "$BUILD_DIR/compiled" "$BUILD_DIR/gen" "$BUILD_DIR/classes" "$BUILD_DIR/dex" "$BUILD_DIR/apk" "$OUT_DIR"

MANIFEST="$BUILD_DIR/AndroidManifest.xml"
perl -0pe 's/(<manifest\b[^>]*?)>/$1 package="com.nikolay.appdate">/s' \
  "$APP_DIR/src/main/AndroidManifest.xml" > "$MANIFEST"

"$AAPT2" compile --dir "$APP_DIR/src/main/res" -o "$BUILD_DIR/compiled/resources.zip"

"$AAPT2" link \
  -I "$ANDROID_JAR" \
  --manifest "$MANIFEST" \
  -A "$APP_DIR/src/main/assets" \
  --java "$BUILD_DIR/gen" \
  --custom-package com.nikolay.appdate \
  --min-sdk-version 26 \
  --target-sdk-version 36 \
  --version-code 1 \
  --version-name 0.1.0 \
  -o "$BUILD_DIR/apk/appdate-unsigned.apk" \
  "$BUILD_DIR/compiled/resources.zip"

find "$APP_DIR/src/main/java" "$BUILD_DIR/gen" -name '*.java' -print > "$BUILD_DIR/sources.txt"

javac \
  --release 8 \
  -classpath "$ANDROID_JAR" \
  -d "$BUILD_DIR/classes" \
  @"$BUILD_DIR/sources.txt"

"$D8" \
  --lib "$ANDROID_JAR" \
  --output "$BUILD_DIR/dex" \
  $(find "$BUILD_DIR/classes" -name '*.class' -print)

(
  cd "$BUILD_DIR/dex"
  zip -q "$BUILD_DIR/apk/appdate-unsigned.apk" classes.dex
)

"$ZIPALIGN" -f -p 4 "$BUILD_DIR/apk/appdate-unsigned.apk" "$BUILD_DIR/apk/appdate-aligned.apk"

if [[ ! -f "$KEYSTORE" ]]; then
  keytool -genkeypair \
    -keystore "$KEYSTORE" \
    -storepass android \
    -keypass android \
    -alias appdate-debug \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Appdate Debug,O=Droid Appdate,C=US" >/dev/null
fi

"$APKSIGNER" sign \
  --ks "$KEYSTORE" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$OUT_DIR/appdate-debug.apk" \
  "$BUILD_DIR/apk/appdate-aligned.apk"

"$APKSIGNER" verify --verbose "$OUT_DIR/appdate-debug.apk"

if [[ -d "$ROOT_DIR/site/downloads" ]]; then
  cp "$OUT_DIR/appdate-debug.apk" "$ROOT_DIR/site/downloads/appdate-debug.apk"
fi

echo "Built $OUT_DIR/appdate-debug.apk"
