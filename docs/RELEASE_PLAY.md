# Android Play internal testing release

Automation lives in [`fastlane/`](../fastlane/) (gem **2.241.1**). Workflows run on `workflow_dispatch` or version tags and **exit successfully without uploading** when secrets, artefacts, or the Android project are missing.

## Prerequisites

1. Google Play Console app listing (package name reserved).  
2. **Service account** JSON with Play Console API access (Release to testing tracks).  
3. Link the Cloud project in Play Console → Setup → API access.  
4. Native Android target + a signed **AAB** (preferred) or APK.

## Secrets (GitHub Actions)

| Secret | Required | Description |
|--------|----------|-------------|
| `PLAY_JSON_KEY_CONTENT` | Yes* | Full service-account JSON body |
| `PLAY_JSON_KEY_FILE` | Alt* | Path to JSON on a self-hosted runner |
| `ANDROID_APPLICATION_ID` | Recommended | Application id |
| `ANDROID_AAB_PATH` | Optional | Override path to `.aab` |
| `ANDROID_APK_PATH` | Optional | Override path to `.apk` if no AAB |

\* Provide content **or** file path.

## Local commands

```bash
cd fastlane
bundle install
bundle exec fastlane android internal_dry_run
# After assembling a release bundle:
# export PLAY_JSON_KEY_FILE=./play-api.json
# export ANDROID_AAB_PATH=../android/app/build/outputs/bundle/release/app-release.aab
bundle exec fastlane android internal
```

## Internal testing track

1. Play Console → Testing → **Internal testing**.  
2. Create a release; CI/fastlane uploads to the `internal` track with `release_status: completed`.  
3. **Testers:** create an email list (or Google Group) and add it to the internal track.  
4. Share the opt-in URL from the internal testing page with those accounts.  
5. Internal testing does not require a full production review; useful for dogfood builds.

## Promotion path (manual)

Internal → closed → open → production remains a Console (or later fastlane) promotion step. This repo only automates **internal** until production lanes are explicitly added.

## Tag / workflow triggers

- Manual: Actions → **Release Android Play internal** → Run workflow.  
- Tag: `v*` — same skip-if-no-secrets behaviour as iOS.

## Signing the app binary

Play upload signing is separate from API access:

- Prefer Play App Signing with an upload keystore stored in CI secrets (`ANDROID_KEYSTORE_*`) when the Android module exists.  
- Until then, the workflow skips when no AAB/APK is produced.
