# OmniGart yoga content (bundle-first)

Pose library and guided flows shipped in the app binary. Updates may later arrive via signed packs; v1 is **bundled**.

## Layout

| Path | Role |
| --- | --- |
| `schema/pose.schema.json` | Pose schema (English + Sanskrit keys, muscles, props, general-safety notes) |
| `schema/flow.schema.json` | Timed sequences referencing pose ids |
| `data/poses/` | ~60 poses |
| `data/flows/` | ~12 flows with durations and levels |
| `locales/en-GB.json` / `locales/uk.json` | Copy keys |
| `known-evidence-ids.json` | Evidence registry entry ids this pack may cite |

## Muscles

`primaryMuscles` / `secondaryMuscles` use the shared exercise **taxonomy** ids (same set mapped to body-map regions in the body-map pack).

## Evidence

Items cite `evidenceEntryIds` such as `garber-acsm-2011` and `acsm-rt-2026` (evidence registry). When `content/evidence/entries` is present, validators require those files to exist.

## Regenerate

```bash
python3 scripts/generate_yoga_pilates.py
```

## Locales

**en-GB** + **uk** only here. Never add `ru`. Other languages are owned by the translation agent.
