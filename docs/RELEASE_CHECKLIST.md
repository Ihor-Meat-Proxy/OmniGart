# OmniGart release checklist (health / wellness apps)

Owner-mandated App Review and store health-app rules. Keep this file updated when Apple, Google, or product policy changes. CI enforces a subset via `scripts/check_app_review_compliance.py` and `.github/ci/banned-terms.yml` inside existing CI jobs.

## Every release (both stores)

- [ ] **Wellness, not medical** — readiness, stress, HRV, Baevsky index, body-fat estimates, hydration, and plans are framed as wellness estimates. No diagnose / detect-disease / treat / cure / medical-grade / clinically-proven claims unless literally true and cited.
- [ ] **In-app disclaimer** — onboarding and every ⓘ info sheet include: not medical advice; consult a professional.
- [ ] **Health warning** — stop-if-chest-pain (and similar) copy where activity is started or guided.
- [ ] **No competitor product names** in app UI, store listing, screenshots, or test notes (list lives only in `.github/ci/banned-terms.yml`).
- [ ] **No release dead ends** — no debug menus, lorem ipsum, TODO/FIXME user strings, or “coming soon” surfaces. Debug UI behind `#if DEBUG` / debuggable-only source sets; unfinished work behind `FeatureFlags` (defaults `false`).
- [ ] **Locales** — en-GB + uk only; no `ru`. Usage descriptions present and non-empty for both.
- [ ] **CI green** — Design check, KMP + Android + Wear (includes compliance script), iOS + watchOS (includes compliance script), Body map as applicable.
- [ ] **Privacy** — `PrivacyInfo.xcprivacy`: no tracking; Health/Fitness data not for ads or third-party marketing.

## Permissions (just-in-time)

- [ ] Pre-permission explainer screen shown before the OS dialog.
- [ ] Purpose strings are specific and honest for HealthKit read/write, Bluetooth, camera, notifications, and Health Connect rationales (en-GB + uk).
- [ ] App remains fully usable if the user denies any permission (manual entry / degraded mode).

### iOS / watchOS keys

| Key | Purpose |
|-----|---------|
| `NSHealthShareUsageDescription` | Read wellness samples from Apple Health |
| `NSHealthUpdateUsageDescription` | Write workouts / chosen metrics to Apple Health |
| `NSBluetoothAlwaysUsageDescription` | Optional HR / fitness accessories |
| `NSCameraUsageDescription` | Food barcode scan only |
| `NSUserNotificationsUsageDescription` | Opt-in reminders |

Localised in `*/en-GB.lproj/InfoPlist.strings` and `*/uk.lproj/InfoPlist.strings`.

### Android / Health Connect

- [ ] `HealthConnectPrivacyRationaleActivity` exported with `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` (and View Permission Usage alias).
- [ ] Privacy policy URL reachable from the rationale screen.
- [ ] Play Console **Health apps declaration** completed (Health Connect data types, data use, privacy policy).
- [ ] Camera / Bluetooth / notifications requested only after explainers; denial is non-blocking.

## TestFlight / App Store

- [ ] Age rating and HealthKit usage questionnaire answered accurately (wellness / fitness, not clinical diagnosis).
- [ ] App Privacy nutrition labels match `PrivacyInfo.xcprivacy` (no tracking; health data for app functionality only).
- [ ] Review notes describe wellness estimates; do not name competitors; do not claim medical outcomes.
- [ ] Export compliance / encryption answers current.
- [ ] Screenshots and What’s New use en-GB (and uk if localised listing) without banned terms.

## Google Play

- [ ] **Health apps policy** — declare Health Connect (or health-related) categories; complete the Health Connect declaration form in Play Console.
- [ ] Data safety form: health data collected/shared accurately; not sold; not used for advertising.
- [ ] Prominent disclosure + Privacy Policy URL in store listing and in-app rationale.
- [ ] Target API / Health Connect permissions match manifest; unused permissions removed before production.
- [ ] Store listing + release notes scanned by the copy linter (see `store/`).

## Pre-submit commands

```bash
python3 scripts/check_app_review_compliance.py
./scripts/check_no_ru_locale.sh
./gradlew checkNoRuLocale checkAppReviewCompliance ktlintCheck detekt
```

## When you change policy or copy

1. Update strings in Android `res/values*`, iOS `Localizable.xcstrings`, shared `i18n`, and `InfoPlist.strings`.
2. Update `.github/ci/banned-terms.yml` if new banned phrases or competitor names are needed.
3. Update this checklist if Apple/Google requirements change.
4. Re-run the compliance script locally before pushing.
