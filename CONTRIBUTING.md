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
- Prefer latest **stable** dependency versions; update `gradle/libs.versions.toml` and let Renovate/Dependabot assist.

## Checks to run

```bash
./scripts/check_no_ru_locale.sh
./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest :wearApp:testDebugUnitTest
./gradlew ktlintCheck detekt
```

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
