# OmniGart exercise content

Self-contained library for merge alongside Phase 0 scaffold / design-system PRs.

| Path | Purpose |
| --- | --- |
| `schema/` | JSON Schema for exercises and workout templates |
| `data/*.json` | One file per exercise (original copy) |
| `templates/*.json` | Starter “desired workouts” |
| `locales/en-GB.json`, `locales/uk.json` | Display names only (no `ru`) |
| `equipment-presets.json` | Home / commercial / travel / bodyweight / pool |

Kotlin models, search/filter, content validator, and planning engine: `shared/planning`.

Regenerate helpers (optional): `scripts/generate_exercise_library.py`, `scripts/generate_templates.py`.
