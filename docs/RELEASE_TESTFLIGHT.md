# iOS TestFlight release

Automation lives in [`fastlane/`](../fastlane/) (gem **2.241.1**). Workflows run on `workflow_dispatch` or version tags and **exit successfully without uploading** when secrets or the Xcode project are missing.

## Prerequisites

1. Apple Developer Program membership and an App Store Connect app record.  
2. **App Store Connect API key** (Issuer ID + Key ID + `.p8`).  
3. Native iOS target in-repo (not required for dry-run / secret-skip CI).  
4. Signing: either **match** (git-backed certs) or **manual** / Xcode automatic signing on the build machine.

## Secrets (GitHub Actions)

| Secret | Required | Description |
|--------|----------|-------------|
| `APP_STORE_CONNECT_KEY_ID` | Yes | API key id |
| `APP_STORE_CONNECT_ISSUER_ID` | Yes | Issuer UUID |
| `APP_STORE_CONNECT_API_KEY_CONTENT` | Yes* | `.p8` private key PEM (or base64 if `APP_STORE_CONNECT_API_KEY_IS_BASE64=true`) |
| `APP_STORE_CONNECT_API_KEY_PATH` | Alt* | Path to `.p8` on a self-hosted runner |
| `APPLE_TEAM_ID` | Recommended | Team ID |
| `IOS_BUNDLE_ID` | Recommended | e.g. `app.omnigart.ios` |
| `MATCH_GIT_URL` | Optional | Private certs repo for match |
| `MATCH_PASSWORD` | Optional | match encryption passphrase |
| `MATCH_GIT_BASIC_AUTHORIZATION` | Optional | HTTPS auth for match git |

\* Provide content **or** path.

## Signing options

### A. match (recommended for CI once certs exist)

1. Create a private git repo for certificates (empty).  
2. Locally (once): `bundle exec fastlane match appstore` with Apple credentials to seed the repo.  
3. Set `MATCH_GIT_URL` + `MATCH_PASSWORD` in CI.  
4. Lane calls `match(type: "appstore", readonly: true)` before `build_app`.

### B. Manual / Xcode-managed

1. Omit `MATCH_GIT_URL`.  
2. Ensure the CI macOS runner (or local Mac) has a valid distribution certificate + App Store profile installed, or uses automatic signing with an unlocked keychain.  
3. Document the keychain unlock step in your runner image; do not commit `.p12` files.

## Local commands

```bash
cd fastlane
bundle install
bundle exec fastlane ios beta_dry_run   # always safe
bundle exec fastlane ios beta           # uploads when secrets + project exist
```

## Invites & distribution

### Internal testing

1. App Store Connect → TestFlight → **Internal Testing**.  
2. Add App Store Connect Users (Admin / App Manager / Developer / Marketing) to an internal group.  
3. Builds are available shortly after processing; internal testers do not consume external slots.  
4. Optional: enable automatic distribution of new builds to the internal group.

### External testing

1. Create an **External** group; submit the build for Beta App Review (first external build).  
2. Add testers by email or enable a **public link**.  
3. Public link: TestFlight → group → **Public Link** → enable → share URL. Anyone with the link can join (subject to the group’s tester cap).  
4. Revoke by disabling the public link or removing the group.

### Public link notes

- Suitable for wider betas without collecting emails first.  
- Still requires a passed Beta App Review for external groups.  
- Rotate/disable the link if it leaks beyond the intended audience.

## Tag / workflow triggers

- Manual: Actions → **Release iOS TestFlight** → Run workflow.  
- Tag: `v*` (e.g. `v0.1.0`) pushes run the same job; missing secrets → skip with exit 0.

## After first real upload

Wire `IOS_SCHEME`, export options, and changelog (`TESTFLIGHT_CHANGELOG`) to match the Xcode project when it lands.
