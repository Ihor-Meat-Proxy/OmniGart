# OmniGart pilates content (bundle-first)

Mat and reformer exercise library plus coached sessions, shipped in the app binary.

## Layout

| Path | Role |
| --- | --- |
| `schema/exercise.schema.json` | Mat / reformer exercise schema |
| `schema/session.schema.json` | Timed sessions with blocks |
| `data/exercises/` | ~50 exercises |
| `data/sessions/` | ~10 sessions |
| `locales/en-GB.json` / `locales/uk.json` | Copy keys |
| `known-evidence-ids.json` | Evidence registry entry ids this pack may cite |

## Apparatus

- `mat` — matwork classical / contemporary vocabulary
- `reformer` — spring carriage repertoire
- Sessions may be `mat`, `reformer`, or `mixed`

## Safety framing

`contraindicationNotes` are **general safety** guidance, not diagnosis.

## Regenerate

```bash
python3 scripts/generate_yoga_pilates.py
```
