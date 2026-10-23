# Phase 2 — Food barcode + water

## Goal

Offline-first food logging via Open Food Facts pack + barcode lookup, and water logging with quick-log and reminders.

## In scope

- Ship/consume an offline OFF subset pack (schema + loader); barcode → product nutrients.
- Manual food entry fallback; meal log day view.
- Water: quick log amounts; daily target from Phase 0 water calc; local notifications / reminders (platform adapters).
- Design quick-log affordances (water / barcode / food-log icons).
- Tests: pack loader, barcode match, water day totals; fakes for notifications.

## Out of scope

- Full recipe builder; restaurant menus; cloud sync of food diary (backup is Phase 5).

## Dependencies

- Phase 1 (shell navigation + persistence patterns).
