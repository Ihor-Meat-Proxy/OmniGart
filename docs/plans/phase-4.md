# Phase 4 — Calculation settings

## Goal

User-selectable formulas (simple / advanced modes) with history recalculation when the selection changes.

## In scope

- Settings UI: BMR, HRmax, body fat, 1RM, HR zones — strategy catalog from health/calc work.
- Simple mode: curated defaults; Advanced: full strategy list with short explanations + ⓘ.
- Recalculate derived history (stored estimates) when formula changes; audit which rows were touched.
- Tests: strategy registry, recalculation idempotence, migration of stored formula ids.

## Dependencies

- Formula-strategy pattern (Phase H / Phase 0 calcs); strength history (Phase 1).
