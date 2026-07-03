# Google Play release notes

Google Play expects a signed Android App Bundle (`.aab`) for new apps. Use the Gradle build for Play uploads, not the debug APK from `scripts/build-apk.sh`.

Appdate uses this package name:

```text
com.nikolay.appdate
```

## Build the Play bundle

Use JDK 17. On this machine it is available at:

```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home
```

Create an upload keystore once and keep it private:

```bash
keytool -genkeypair \
  -v \
  -keystore appdate-upload.jks \
  -storetype JKS \
  -alias appdate-upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Then build the signed release bundle:

```bash
export APPDATE_RELEASE_STORE_FILE=/absolute/path/to/appdate-upload.jks
export APPDATE_RELEASE_STORE_PASSWORD=...
export APPDATE_RELEASE_KEY_ALIAS=appdate-upload
export APPDATE_RELEASE_KEY_PASSWORD=...

export ANDROID_HOME=$HOME/Library/Android/sdk
./scripts/build-play-bundle.sh
```

The Play upload artifact is:

```text
app/build/outputs/bundle/release/app-release.aab
```

Running `./gradlew :app:bundleRelease` directly can produce a local unsigned bundle if signing values are absent. Use `scripts/build-play-bundle.sh` for Play uploads because it fails fast when signing is not configured.

## GitHub Actions publishing

The repository includes `.github/workflows/android-release.yml`. It uses Gradle Play Publisher and publishes App Bundles to tracks based on the pushed SemVer tag:

```text
v1.2.3              -> production
v1.2.3-rc.1         -> beta
v1.2.3-beta.1       -> beta
v1.2.3-alpha.1      -> alpha
v1.2.3-internal.1   -> internal
```

Add these GitHub repository secrets before pushing release tags:

```text
APPDATE_UPLOAD_KEYSTORE_BASE64
APPDATE_RELEASE_STORE_PASSWORD
APPDATE_RELEASE_KEY_ALIAS
APPDATE_RELEASE_KEY_PASSWORD
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON
```

Create the keystore secret with:

```bash
base64 -i /absolute/path/to/appdate-upload.jks | pbcopy
```

Paste the copied value into `APPDATE_UPLOAD_KEYSTORE_BASE64`.

## Package visibility policy

Appdate currently declares `QUERY_ALL_PACKAGES` because the core feature is showing disabled apps across the device. Google Play treats this as a restricted broad package visibility permission.

Before production submission:

- Complete the Play Console declaration for `QUERY_ALL_PACKAGES`.
- Keep the app description and in-app copy aligned with the core purpose: local disabled-app discovery and user-initiated update/uninstall routing.
- Do not collect, upload, sell, or share the installed-app inventory unless the privacy policy and Play data safety disclosures explicitly cover it.
- Consider an enterprise/device-owner edition separately if silent update behavior becomes a hard requirement.

## Store listing draft

Short description:

```text
Find disabled apps and open safe update or uninstall flows.
```

Full description draft:

```text
Appdate helps you review disabled Android apps in one place. It lists disabled packages on your device, highlights any updates known to its update catalog, and opens system-owned Play Store, Galaxy Store, app info, or uninstall confirmation screens.

Appdate does not enable disabled apps automatically and does not silently update or uninstall apps. Android keeps those sensitive actions under user, store, or device-owner control.
```
