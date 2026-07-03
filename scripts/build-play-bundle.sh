#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_JAVA_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home"

export JAVA_HOME="${JAVA_HOME:-$DEFAULT_JAVA_HOME}"
export ANDROID_HOME="${ANDROID_HOME:-${HOME}/Library/Android/sdk}"

required_vars=(
  APPDATE_RELEASE_STORE_FILE
  APPDATE_RELEASE_STORE_PASSWORD
  APPDATE_RELEASE_KEY_ALIAS
  APPDATE_RELEASE_KEY_PASSWORD
)

missing=()
for name in "${required_vars[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    missing+=("$name")
  fi
done

if (( ${#missing[@]} > 0 )); then
  printf 'Missing release signing environment variables:\n' >&2
  printf '  %s\n' "${missing[@]}" >&2
  printf '\nCreate an upload keystore and set these before building for Google Play. See PLAY_STORE.md.\n' >&2
  exit 2
fi

if [[ ! -f "$APPDATE_RELEASE_STORE_FILE" ]]; then
  echo "Release keystore does not exist: $APPDATE_RELEASE_STORE_FILE" >&2
  exit 2
fi

cd "$ROOT_DIR"
./gradlew :app:bundleRelease

AAB="$ROOT_DIR/app/build/outputs/bundle/release/app-release.aab"
"$JAVA_HOME/bin/jarsigner" -verify -verbose -certs "$AAB" >/dev/null

echo "Built signed Play upload bundle:"
echo "$AAB"
