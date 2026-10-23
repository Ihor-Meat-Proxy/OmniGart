# Food logging

Offline-first food diary for OmniGart: catalogue search, barcode validation, nutrition-label parsing, meal groups, import/export, micronutrient reference intakes, and optional HealthKit / Health Connect dietary writes.

## Scope split (parallel PRs)

| Concern | Owner |
| --- | --- |
| Diary, USDA/OFF catalogue, barcode, label OCR parse, micros reference intakes, best sources / fill gaps, CSV/JSON import, morning review | **This PR** (`com.omnigart.shared.food`) |
| Calorie / macro targets, adaptive TDEE, body measurements, reminders | Nutrition-model PR (`com.omnigart.shared.nutrition`) via [`NutritionTargetsReader`](../shared/src/commonMain/kotlin/com/omnigart/shared/food/targets/NutritionTargetsReader.kt) |

Food logging depends on a **small read interface** for energy/macro targets and day-start minutes. Until the nutrition-model PR merges, [`StubNutritionTargetsReader`](../shared/src/commonMain/kotlin/com/omnigart/shared/food/targets/NutritionTargetsReader.kt) supplies gentle placeholders (`dataInsufficient = true`).

## Package layout

| Path | Role |
| --- | --- |
| `shared/.../food/models` | `FoodLogEntry`, catalogue foods, meal groups, sources |
| `shared/.../food/diary` | Store API, in-memory + JSON-backed persistence |
| `shared/.../food/catalogue` | Bundled USDA-style demo + composite offline/online |
| `shared/.../food/barcode` | GS1 check-digit validation |
| `shared/.../food/label` | Nutrition-label OCR text parser |
| `shared/.../food/nutrients` | EFSA/IOM-style reference intakes, gaps, best sources |
| `shared/.../food/targets` | `NutritionTargetsReader` seam |
| `shared/.../food/review` | Rule-based + on-device AI morning review |
| `shared/.../food/health` | Append-only dietary sync contracts |
| `shared/.../food/importing` | CSV / JSON export-import |
| `shared/nutrition-data` | Open Food Facts offline pack parser (existing) |

## Logging features

- Meal groups: breakfast / lunch / dinner / snacks (+ custom label)
- Quick servings (100 g default from catalogue; quick kcal/macro add)
- Recents, favourites, custom foods, repeat item, copy a day
- Each row records `FoodSource`: `CUSTOM` / `USDA` / `OPEN_FOOD_FACTS`
- CSV and JSON import/export (reviews included in JSON)

## Nutrient catalogue

- **USDA FoodData Central** (Foundation / SR Legacy / FNDDS) — public domain. Ship a compressed SQLite or optional download; this PR includes a tiny bundled demo slice for tests/UI.
- **Open Food Facts** — ODbL; Search-a-licious with legacy fallback for branded/barcode. Names follow the user’s language when available.
- **Never NCCDB.**
- Offline-only mode: never send queries online. Online mode privacy note: the query string leaves the device.

## Reference intakes

Bundled adult tables approximate EFSA DRVs and IOM/NIH DRIs by age/sex. UI wording: **reference intake**. User overrides and upper limits are supported. Calorie/macro **targets** come from the nutrition-model reader — not duplicated here.

## Platform health writes

Dietary samples (energy, protein, carbs, fat, …) may be appended to HealthKit / Health Connect with sync ids `omnigart.food.<localId>`.

Hard rules (same as workouts):

1. Exist-check sync id / `clientRecordId` before insert; if present, **skip**.
2. **Never** delete or rewrite platform objects (`scripts/check_no_health_platform_mutate.sh`).
3. Local diary edits stay in OmniGart.

See [HEALTH_DATA_MAPPING.md](HEALTH_DATA_MAPPING.md) and [AI_FOOD_REVIEW.md](AI_FOOD_REVIEW.md).

## Privacy & storage

- Diary is local-first. JSON-backed store avoids SQLDelight migration clashes with parallel nutrition work; SQLCipher encryption of the app DB remains Phase 5.
- Erase-all clears OmniGart food data only — never platform Health samples.
- Attribution: [third_party/FOOD_DATA_ATTRIBUTION.md](third_party/FOOD_DATA_ATTRIBUTION.md).

## UI

- iOS: `FoodTabView` + `iosApp/OmniGart/Food/*`
- Android: `FoodLoggingSection` on the Food tab

## Tests

```bash
./gradlew :shared:testDebugUnitTest :shared:compileKotlinIosSimulatorArm64
# or host tests:
./gradlew :shared:testAndroidHostTest
```

Food domain tests: `shared/src/commonTest/.../food/FoodLoggingTest.kt`.

## i18n

```bash
python3 scripts/sync_food_logging_i18n.py
```

Locales: en-GB, uk, pl, be, kk, ka, es, fr, de, cs, zh-Hans. **Never Russian.**
