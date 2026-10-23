# OmniGart exercise content

Self-contained library for merge alongside Phase 0 scaffold / design-system PRs.

| Path | Purpose |
| --- | --- |
| `schema/` | JSON Schema for exercises and workout templates |
| `data/*.json` | One file per exercise (original copy) |
| `templates/*.json` | Starter “desired workouts” |
| `locales/*.json` | Full catalogs (names + technique/setup/breathing/mistakes/safety/notes + template copy). Locales: see `i18n/locales.yml` (no `ru`) |
| `equipment-presets.json` | Home / commercial / travel / bodyweight / pool |

Glossary, review status, Weblate export, and CI: `docs/TRANSLATION.md`, `content/i18n/`, `scripts/content_i18n/`.

Kotlin models, search/filter, content validator, and planning engine: `shared/planning`.

Regenerate helpers (optional): `scripts/generate_exercise_library.py`, `scripts/generate_templates.py`, `scripts/content_i18n/extract_en.py --write`.

