# Open questions, assumptions, and decisions needing review

Phase 0 autopilot log. Each item: **context**, **choice**, **alternatives**.

## Branch naming vs cloud agent prefix

- **Context:** Owner rule prefers `feat/...` / `chore/...`; Cursor Cloud Agent requires `cursor/<name>-2ab3` for PR association.
- **Choice:** Use `cursor/phase-0-foundation-2ab3` (feature branch, never `main`).
- **Alternatives:** Rename locally to `feat/phase-0-foundation` for humans and keep cloud mapping documented.

## XcodeGen vs Tuist

- **Context:** Reproducible Apple project without committing brittle `.xcodeproj` as source of truth.
- **Choice:** **XcodeGen** (`iosApp/project.yml`).
- **Alternatives:** Tuist (stronger for large native graphs); hand-maintained Xcode project.

## Library versions (Oct 2026)

- **Context:** Owner requires latest **stable** as of Oct 2026.
- **Choice:** Kotlin **2.4.21**, AGP **9.4.1**, Gradle **9.8.2**, SQLDelight **2.4.1**, Compose BOM **2026.09.00**, Material 3 **1.4.0**, Wear Compose **1.7.1**, Coroutines **1.11.0**, Datetime **0.8.0**, Serialization **1.12.0**. Kotlin **2.5.0** and Material3 **1.5.0** were still pre-release.
- **Alternatives:** Track 2.5/1.5 betas for Material 3 Expressive APIs earlier.

## Material 3 Expressive

- **Context:** Product asks for Material 3 Expressive on Android.
- **Choice:** Ship Material 3 **1.4.0** (latest stable) via BOM; treat full Expressive component set as follow-up when 1.5 stable lands.
- **Alternatives:** Depend on `1.5.0-beta02` now.

## Biological sex model

- **Context:** Mifflin–St Jeor, CUN-BAE, EFSA water are binary sex equations.
- **Choice:** `BiologicalSex { MALE, FEMALE }` only in Phase 0.
- **Alternatives:** Add `UNSPECIFIED` with explicit equation opt-in; separate gender identity field for UI.

## Discipline level thresholds

- **Context:** Spec defines structures (4 strength levels; running newbie+5; swimming newbie+4) but not numeric cut-offs.
- **Choice:** Placeholder thresholds in `DisciplineClassifier` (months / weekly km / pace / 5 km / swim metres). Documented here as **provisional**.
- **Alternatives:** Import coach-authored tables from content packs.

## Water target extras

- **Context:** EFSA base + drink fraction are cited; weight factor, training bonus, heat bonus, and 4 L cap are product rules.
- **Choice:** Implement exactly as Phase 0 spec.
- **Alternatives:** ACSM sweat-rate protocols; climate heat-index instead of fixed 27 °C.

## Readiness heuristic

- **Context:** Point bands and SWC framing are product rules; Buchheit 2014 informs HRV SWC concept only.
- **Choice:** Implement literal Phase 0 scoring.
- **Alternatives:** Banister / TRIMP load; validated readiness questionnaires.

## Database encryption

- **Context:** Spec plans encrypted DB; SQLCipher KMP story varies by platform.
- **Choice:** Plain SQLite in Phase 0; keep `DatabaseDriverFactory` as the injection point.
- **Alternatives:** SQLCipher Android + SQLCipher Apple; EncryptedSharedPreferences for keys only.

## watchOS KMP framework linking

- **Context:** Both iOS and watchOS consume `Shared.framework` from Gradle.
- **Choice:** Separate Apple targets produce `Shared` for ios* and watchos*; XcodeGen points `FRAMEWORK_SEARCH_PATHS` at `shared/build/xcode-frameworks/...`.
- **Alternatives:** Fat XCFramework via `./gradlew assembleXCFramework` (preferred once verified on macOS).

## iOS 26 / watchOS 26 / Liquid Glass

- **Context:** Owner has iOS 27.2 / watchOS 27.2 simulators; deployment target set to 26.0.
- **Choice:** Deployment target **26.0**; rely on system Liquid Glass appearance without custom glass APIs in Phase 0.
- **Alternatives:** Require 27.0; add explicit glass modifiers when public API is confirmed.

## In-memory SQLDelight tests location

- **Context:** `JdbcSqliteDriver` is JVM-only.
- **Choice:** Repository tests in `androidUnitTest` (runs on Linux CI).
- **Alternatives:** Add a `jvm()` KMP target solely for tests.

## Wear applicationId

- **Context:** Wear apps often share the phone applicationId for Play packaging; Robolectric also needs a distinct package for reliable activity resolution.
- **Choice:** `com.omnigart.wear` for the Wear module in Phase 0.
- **Alternatives:** Share `com.omnigart.android` for Play multi-APK when packaging is wired.

## compileSdk 37

- **Context:** Compose BOM 2026.09.00 / Compose UI 1.12.1 requires compileSdk ≥ 37.
- **Choice:** `compileSdk = 37`, `targetSdk = 37`, platforms `android-37.0` / `android-37.2`.
- **Alternatives:** Pin an older Compose BOM that still supports compileSdk 36.

## AGP 9 KMP Android library plugin

- **Context:** AGP 9 rejects pairing `com.android.library` with `org.jetbrains.kotlin.multiplatform`.
- **Choice:** `com.android.kotlin.multiplatform.library` with `kotlin { android { ... } }` and `androidHostTest`.
- **Alternatives:** Temporary `android.builtInKotlin=false` / `android.newDsl=false` flags.
