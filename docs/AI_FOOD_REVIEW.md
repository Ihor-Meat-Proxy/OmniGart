# On-device morning food review

Optional wellness feature (off by default). Each morning, at a user-chosen local time, OmniGart can summarise the previous nutrition day’s food log against targets and suggest gentle improvements.

## Platforms

| Platform | On-device stack | Availability checks |
| --- | --- | --- |
| iOS | Apple Foundation Models (Apple Intelligence, iOS 26+) | Device eligible, Apple Intelligence enabled, model ready |
| Android | Gemini Nano via ML Kit GenAI / AICore | Feature availability + download status |

**Fully on-device. No cloud fallback.** If unavailable, hide or explain the AI toggle and offer the non-AI rule-based summary (totals vs targets, top nutrient gaps).

## Feature flag & settings

- `FeatureFlags.morningFoodReviewEnabled` defaults to **false** until product-ready.
- User toggle `FoodSettings.morningAiReviewEnabled` (off by default).
- Notification time: `FoodSettings.morningReviewMinutes` (local minutes after midnight).
- Day boundary: prefer `NutritionTargetsReader.dayStartMinutes` (nutrition-model PR); food settings may mirror it.

## Prompt contract

The prompt includes **only**:

- Aggregated day totals (energy + macros)
- Nutrient gaps (consumed vs reference intake)
- Food names from the log

Output must be **grounded** in those numbers — never invent calories. See `MorningReviewPromptBuilder`.

## Safety

- Wellness wording only; **not medical advice** footer on every review.
- No deficit / weight-loss pushing for under-18s or pregnancy mode (`NutritionTargetsReader.isUnderEighteen` / `isPregnancyMode`).
- Suppressed when the low-intake guard is active (`isLowIntakeGuardActive`).
- Users can dismiss a review or turn the feature off.
- Reviews are stored locally and included in JSON exports.

## Locales

Respond in the app language (11 locales: en-GB, uk, pl, be, kk, ka, es, fr, de, cs, zh-Hans). **Never Russian.**

## Fallback

`RuleBasedMorningReview` builds a short totals-vs-targets summary and up to three gentle gap suggestions without any generative model.

## Related

- [FOOD_LOGGING.md](FOOD_LOGGING.md)
- `shared/.../food/review/MorningFoodReview.kt`
- iOS: `FoodMorningReviewSupport.swift`
- Android: availability checks live beside `FoodLoggingSection` (Gemini Nano when product-ready)
