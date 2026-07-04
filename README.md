# appdate

appdate contains Appdate, a local-first Android utility prototype for finding disabled apps and routing them to safe update or uninstall flows.

Android does not allow an ordinary third-party app to silently update, uninstall, enable, or disable other apps. Appdate therefore keeps disabled apps disabled by never changing their enabled state. It detects disabled packages with public `PackageManager` APIs, highlights packages that match a local update catalog, and opens system-owned Play Store, Galaxy Store, app-info, or uninstall screens for the user to complete.

## Project layout

- `app/` - native Android app source, written in Java with no external runtime dependencies.
- `scripts/build-apk.sh` - offline-friendly APK builder using the installed Android SDK tools.
- `scripts/manual/AndroidManifest.xml` - manifest used only by the manual APK builder because `aapt2` requires a `package` attribute.
- `scripts/resolve-play-release.sh` - SemVer-to-Play-track resolver used by GitHub Actions.
- `site/` - static GitHub Pages-ready website for `https://droidappdate.com`.
- `site/updates/catalog.json` - local static update catalog shape for future hosting.

The Android application id is `com.nikolay.appdate`.

## Build locally for sideload testing

From this directory:

```bash
./scripts/build-apk.sh
```

The signed debug APK is written to:

```text
build/outputs/apk/appdate-debug.apk
```

If the site download folder exists, the script also copies the APK to:

```text
site/downloads/appdate-debug.apk
```

## Build for Google Play

Google Play uploads should use the Gradle Android App Bundle build, not the debug APK:

```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=$HOME/Library/Android/sdk
./scripts/build-play-bundle.sh
```

Release signing is configured through these environment variables or Gradle properties:

```text
APPDATE_RELEASE_STORE_FILE
APPDATE_RELEASE_STORE_PASSWORD
APPDATE_RELEASE_KEY_ALIAS
APPDATE_RELEASE_KEY_PASSWORD
```

See `PLAY_STORE.md` for signing, upload, and `QUERY_ALL_PACKAGES` policy notes.

## GitHub Actions releases

The `Android Release` workflow publishes tagged builds to Google Play. Push a SemVer tag to choose the Play track:

- `v1.2.3` -> `production`
- `v1.2.3-rc.1` or `v1.2.3-beta.1` -> `beta`
- `v1.2.3-alpha.1` -> `alpha`
- `v1.2.3-internal.1`, `v1.2.3-dev.1`, `v1.2.3-snapshot.1`, or any other prerelease label -> `internal`

The workflow also supports manual dispatch with an explicit track override.

Configure these GitHub repository secrets before publishing:

```text
APPDATE_UPLOAD_KEYSTORE_BASE64
APPDATE_RELEASE_STORE_PASSWORD
APPDATE_RELEASE_KEY_ALIAS
APPDATE_RELEASE_KEY_PASSWORD
```

`APPDATE_UPLOAD_KEYSTORE_BASE64` should be the base64-encoded upload keystore file.

Configure these GitHub repository variables before publishing:

```text
GCP_WORKLOAD_IDENTITY_PROVIDER
GCP_SERVICE_ACCOUNT_EMAIL
```

The workflow uses Google Cloud Workload Identity Federation, so no long-lived service-account key JSON is stored in GitHub.

## Theme support

The Android app and static site both follow the system light/dark setting.

- Android uses night-qualified resources in `app/src/main/res/values-night/` and avoids hardcoded light-theme colors in the activity UI.
- The static site uses `color-scheme: light dark` and `prefers-color-scheme: dark` CSS token overrides.

## Run locally

Install the APK on a connected Android device:

```bash
$HOME/Library/Android/sdk/platform-tools/adb install -r build/outputs/apk/appdate-debug.apk
```

Open the static site directly from `site/index.html`, or serve it locally:

```bash
python3 -m http.server 8080 --directory site
```

## Current limitations

- Appdate cannot know Google Play or Galaxy Store pending updates for every package through public Android APIs.
- The first local version uses a static catalog file to mark known updates. Packages outside the catalog are shown with a `Check store` action.
- Silent update while disabled requires privileged/system, device-owner, root, or ADB-level authority. This prototype intentionally avoids pretending otherwise.
- `QUERY_ALL_PACKAGES` is included because listing disabled packages requires broad package visibility on modern Android. That permission has Play Store policy implications and must be declared/justified in Play Console.
