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

## Shipped (this PR)

- `FormulaSelection` / `FormulaMode` / `FormulaDomain` + catalog metadata (`FormulaOptionMeta`)
- SQLDelight: `formula_settings`, `derived_estimate`, `formula_recalc_audit`
- `FormulaSettingsService`: persist selection, recalculate estimates, write audit rows; unknown keys migrate to defaults
- `OmniGartCore.formulaSettings` + summary lines use selected formulas
- i18n: `StringKeys` / `StringCatalog` (en-GB + uk) for calc settings + all `formula.*.title` / `formula.*.explain` keys; mirrored in `shared/.../i18n/*/strings.json` and Android `values` / `values-en-rGB` / `values-uk` (domain/mode/shell keys; formula dotted keys via StringCatalog)
- Android: `CalculationsSettingsSection` on Profile in `OmniGartPhoneShell` (Simple/Advanced + per-domain options, `setMode` / `updateDomain`)
- iOS: `CalculationsSettingsView` on Profile in `PhoneRootView` via Shared `StringCatalog` + `FormulaSettingsService`
- Host tests: migration, recalculation + audit, idempotent values, simple-mode reset
