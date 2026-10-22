# OmniGart achievements content

Data-driven achievement definitions, challenge templates, and en-GB / uk copy. Evaluation lives in `shared/achievements` (pure Kotlin; no app wiring).

## Layout

| Path | Role |
| --- | --- |
| `schema/achievement.schema.json` | Catalog + rule schema |
| `schema/challenge-template.schema.json` | Local-only challenge templates |
| `data/achievements.json` | ~80 achievements across 10 categories |
| `data/challenge-templates.json` | Personal time-boxed challenges |
| `locales/en-GB.json` / `locales/uk.json` | Title & description strings |
| `BEHAVIOURAL_SCIENCE.md` | Healthy-by-design rationale + verified refs |

## Rule kinds

| Kind | Meaning |
| --- | --- |
| `counter` | Accumulate a metric (lifetime or windowed) |
| `streak` | Consecutive local-calendar days; planned rest + streak freeze supported |
| `threshold` | Single-event value (e.g. one run ≥ 5 km) |
| `personal_record` | First / successive PRs |
| `challenge` | Completing a personal challenge type |

## Regenerate

```bash
python3 scripts/generate_achievements.py
```

Icon ids must exist in `design/icons/manifest.json` (reuse existing set; extend via `design/tools` if new glyphs are needed).

## Locales

Ship **en-GB** and **uk** only here. Other languages are owned by the translation agent — list new keys in `docs/OPEN_QUESTIONS.md`. Never add `ru`.
