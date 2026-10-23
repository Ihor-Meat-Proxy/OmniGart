# OmniGart achievements content

| Path | Purpose |
| --- | --- |
| `schema/achievement.schema.json` | JSON Schema (Draft 2020-12) for achievement definitions + rule DSL |
| `data/*.json` | Catalogue by category (~120 achievements) |
| `catalogue.json` | Index with counts |
| `locales/en-GB.json`, `locales/uk.json` | Title/description strings only (no `ru`) |
| `proposed-icons.json` | Icon names not yet in `design/icons` |
| `tools/` | Generator + Node validator (CI) |

Kotlin models, rule DSL checks, and evaluation engine: `shared/achievements`.

```bash
python3 tools/generate_catalogue.py
cd tools && npm ci && npm run validate
./gradlew :shared:achievements:jvmTest
```

Design notes and evidence: `docs/ACHIEVEMENTS.md`.
