#!/usr/bin/env bash
set -euo pipefail

version_input="${1:-}"
track_override="${2:-auto}"
release_status_input="${3:-completed}"

if [[ -z "$version_input" ]]; then
  echo "Usage: $0 <semver-or-tag> [auto|internal|alpha|beta|production] [completed|draft|halted|inProgress]" >&2
  exit 2
fi

version="${version_input#refs/tags/}"
version="${version#v}"
semver_re='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-([0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*))?(\+[0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*)?$'

if [[ ! "$version" =~ $semver_re ]]; then
  echo "Release tags must be SemVer, for example v1.2.3, v1.2.3-beta.1, or v1.2.3+45." >&2
  exit 2
fi

major="${BASH_REMATCH[1]}"
minor="${BASH_REMATCH[2]}"
patch="${BASH_REMATCH[3]}"
prerelease="${BASH_REMATCH[5]:-}"

if (( major > 20 || minor > 99 || patch > 99 )); then
  echo "Version is outside the encoded Play versionCode range: major<=20, minor<=99, patch<=99." >&2
  exit 2
fi

sequence=0
if [[ -n "$prerelease" ]]; then
  IFS='.' read -r -a prerelease_parts <<< "$prerelease"
  for part in "${prerelease_parts[@]}"; do
    if [[ "$part" =~ ^[0-9]+$ ]]; then
      sequence="$part"
    fi
  done
fi

if (( sequence > 999 )); then
  sequence=999
fi

prerelease_lower="$(printf '%s' "$prerelease" | tr '[:upper:]' '[:lower:]')"
track="production"
rank=9999

if [[ -n "$prerelease_lower" ]]; then
  if [[ "$prerelease_lower" == *rc* ]]; then
    track="beta"
    rank=$((8000 + sequence))
  elif [[ "$prerelease_lower" == *beta* ]]; then
    track="beta"
    rank=$((4000 + sequence))
  elif [[ "$prerelease_lower" == *alpha* ]]; then
    track="alpha"
    rank=$((2000 + sequence))
  elif [[ "$prerelease_lower" == *internal* || "$prerelease_lower" == *dev* || "$prerelease_lower" == *snapshot* ]]; then
    track="internal"
    rank=$((1000 + sequence))
  else
    track="internal"
    rank=$((1000 + sequence))
  fi
fi

case "$track_override" in
  ""|"auto")
    ;;
  "internal"|"alpha"|"beta"|"production")
    track="$track_override"
    ;;
  *)
    echo "Unsupported Play track override: $track_override" >&2
    exit 2
    ;;
esac

case "$release_status_input" in
  "completed"|"draft"|"halted")
    release_status="$release_status_input"
    ;;
  "inProgress"|"inprogress"|"in_progress")
    release_status="inProgress"
    ;;
  *)
    echo "Unsupported release status: $release_status_input" >&2
    exit 2
    ;;
esac

version_code=$(( major * 100000000 + minor * 1000000 + patch * 10000 + rank ))

emit() {
  local name="$1"
  local value="$2"
  printf '%s=%s\n' "$name" "$value"
  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    printf '%s=%s\n' "$name" "$value" >> "$GITHUB_OUTPUT"
  fi
  if [[ -n "${GITHUB_ENV:-}" ]]; then
    printf '%s=%s\n' "$name" "$value" >> "$GITHUB_ENV"
  fi
}

emit "APPDATE_VERSION_NAME" "$version"
emit "APPDATE_VERSION_CODE" "$version_code"
emit "PLAY_TRACK" "$track"
emit "PLAY_RELEASE_STATUS" "$release_status"
