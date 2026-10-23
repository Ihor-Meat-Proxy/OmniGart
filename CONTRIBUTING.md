# Contributing to OmniGart

License: **AGPL-3.0** (see `LICENSE`).

## Branching

- Never commit to `main`.
- Use feature branches (`feat/...`, `chore/...`, or the Cloud Agent `cursor/...-2ab3` form).
- Open a PR with a **detailed** description: summary, motivation, changes by module, architecture decisions, testing + results, screenshots if UI, known limitations, follow-ups, and how to verify.

## Code standards

- Shared business logic belongs in `shared/`, not in SwiftUI/Compose views.
- No Kotlin `!!`, no `GlobalScope`, no force-unwraps in Swift for recoverable state.
- Do not add a `ru` locale. Source/fallback is `en-GB`.
- Do not name competitor apps or products in code, docs, commits, or PRs; describe categories and patterns neutrally. Platform APIs (HealthKit, Health Connect, Google Health API, Polar BLE SDK) are fine. The CI ban list lives only in `.github/ci/banned-terms.yml`.
- Wellness framing only: no medical-claim language in user-facing copy; disclaimers on onboarding and ⓘ sheets. See `docs/RELEASE_CHECKLIST.md`.
- Debug menus only under `#if DEBUG` / debuggable source sets; unfinished features behind `FeatureFlags` (off in release).
- Prefer latest **stable** dependency versions; update `gradle/libs.versions.toml` and let Renovate/Dependabot assist.

## Checks to run

```bash
./scripts/check_no_ru_locale.sh
python3 scripts/check_app_review_compliance.py
python3 scripts/pack_bundled_content.py
python3 scripts/pack_bundled_content.py --verify
./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest :wearApp:testDebugUnitTest
./gradlew checkNoRuLocale checkAppReviewCompliance verifyBundledContent ktlintCheck detekt
```

After editing `content/`, `docs/evidence/`, or bundled design assets, regenerate and commit `generated/content-bundle/` (see `docs/CONTENT_BUNDLE.md`).

On macOS (Apple targets):

```bash
cd iosApp && xcodegen generate
cd ..
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkDebugFrameworkWatchosSimulatorArm64
# then Xcode schemes OmniGart / OmniGartWatch — see README
```

## Content vs UI strings

- UI copy: Android `res/values*`, iOS `Localizable.xcstrings`, and `shared/.../i18n`.
- Exercises / evidence / translations of content: `content/packs/**` and `docs/evidence/**`.
