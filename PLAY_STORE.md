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
```

Create the keystore secret with:

```bash
base64 -i /absolute/path/to/appdate-upload.jks | pbcopy
```

Paste the copied value into `APPDATE_UPLOAD_KEYSTORE_BASE64`.

The workflow authenticates to Google Play through Google Cloud Workload Identity Federation, not a service-account JSON key. Add these GitHub repository variables:

```text
GCP_WORKLOAD_IDENTITY_PROVIDER
GCP_SERVICE_ACCOUNT_EMAIL
```

`GCP_WORKLOAD_IDENTITY_PROVIDER` is the full provider resource, for example:

```text
projects/123456789/locations/global/workloadIdentityPools/github/providers/appdate
```

`GCP_SERVICE_ACCOUNT_EMAIL` is the service account invited in Play Console, for example:

```text
appdate-github-actions@your-project-id.iam.gserviceaccount.com
```

Create the Google Cloud side with:

```bash
export PROJECT_ID=your-project-id
export REPO=nikolay/appdate
export SERVICE_ACCOUNT=appdate-github-actions

gcloud services enable \
  androidpublisher.googleapis.com \
  iamcredentials.googleapis.com \
  --project="${PROJECT_ID}"

gcloud iam service-accounts create "${SERVICE_ACCOUNT}" \
  --project="${PROJECT_ID}" \
  --display-name="Appdate GitHub Actions"

gcloud iam workload-identity-pools create "github" \
  --project="${PROJECT_ID}" \
  --location="global" \
  --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers create-oidc "appdate" \
  --project="${PROJECT_ID}" \
  --location="global" \
  --workload-identity-pool="github" \
  --display-name="Appdate repository" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner" \
  --attribute-condition="assertion.repository == '${REPO}'"

export WORKLOAD_IDENTITY_POOL="$(gcloud iam workload-identity-pools describe "github" \
  --project="${PROJECT_ID}" \
  --location="global" \
  --format="value(name)")"

gcloud iam service-accounts add-iam-policy-binding \
  "${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --project="${PROJECT_ID}" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/${WORKLOAD_IDENTITY_POOL}/attribute.repository/${REPO}"

gcloud iam workload-identity-pools providers describe "appdate" \
  --project="${PROJECT_ID}" \
  --location="global" \
  --workload-identity-pool="github" \
  --format="value(name)"
```

Use the final command output as `GCP_WORKLOAD_IDENTITY_PROVIDER`. Use `${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com` as `GCP_SERVICE_ACCOUNT_EMAIL`.

In Play Console, invite the same service account email under Users and permissions and grant it release access for the Appdate app.

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
