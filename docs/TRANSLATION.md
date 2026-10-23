# Content & UI translation workflow

OmniGart ships exercise/template copy and (when present) app UI strings in multiple locales. **Source locale is always `en-GB`.** Russian (`ru` / `ru-*`) is never added, never used as a pivot, and CI fails if it appears.

## Single source of truth

[`i18n/locales.yml`](../i18n/locales.yml) lists:

- `source`: `en-GB`
- `required`: `en-GB`, `uk`, `pl`, `be`, `kk` (Cyrillic), `ka`, `es`, `fr`, `de`, `cs`, `zh-Hans`
- `forbidden`: `ru` and variants
- CLDR plural categories per locale
- Paths for content catalogs, glossary, status sidecars, and app UI trees

Every script under `scripts/content_i18n/` and the **Translations** GitHub Actions job reads this file. Do not hard-code locale lists elsewhere.

## Layout

| Path | Role |
| --- | --- |
| `content/exercises/locales/<locale>.json` | Flat key→string catalogs (exercises + templates) |
| `content/exercises/data/*.json` | Exercise records; English body text remains canonical for schema/planning |
| `content/exercises/templates/*.json` | Templates; English `rationale` remains canonical; translations live in locales |
| `content/i18n/glossary/glossary.csv` | Terminology dictionary (TA anatomy + gym names, equipment, training science, yoga/Pilates/swim/run) |
| `content/i18n/status/<locale>.json` | Per-key `status` (`machine-drafted` \| `reviewed`) + `source_hash` |
| `content/i18n/weblate/` | Optional export tree for community review (generated) |
| `shared/src/commonMain/resources/i18n/<locale>/strings.json` | App UI (when Phase 0 scaffold is merged) |
| Android / Wear `values-*` / iOS `Localizable.xcstrings` | Platform mirrors (checked when present) |

Content locale keys look like:

- `exercise.back_squat.name`
- `exercise.back_squat.technique.0`
- `exercise.back_squat.breathing`
- `template.full_body_beginner_3x.rationale`

## Glossary-first rule

1. Add or update terms in `content/i18n/glossary/glossary.csv` **before** translating new exercise prose.
2. Prefer Terminologia Anatomica–based scientific names; record gym vernacular on a separate `*.gym` term id when it differs.
3. Use national sports-science wording (Ukrainian federation/coach norms for `uk`; Belarusian standard for `be`; Kazakh Cyrillic for `kk`). Never pivot through Russian.
4. Cite sources in the `source` column (`content/i18n/glossary/SOURCES.md`).

Regenerate helper (optional): `python scripts/content_i18n/generate_glossary.py`.

## Translating a new exercise

1. Author the exercise JSON in English under `content/exercises/data/`.
2. Run `python scripts/content_i18n/extract_en.py --write` to refresh `en-GB.json` body keys (names must already be in the catalog).
3. Update the glossary if new anatomy/equipment/cues appear.
4. Translate every new key into **all** required target locales (same key set; no empties).
5. Mark new keys `machine-drafted` via `python scripts/content_i18n/build_status.py` (does not downgrade `reviewed`).
6. Run `python scripts/content_i18n/validate.py` and fix errors before opening/updating the PR.
7. Native speakers flip status to `reviewed` after reading the string in context.

## Review status

- `machine-drafted` — provisional; reported in the CI summary, does **not** fail the job by itself.
- `reviewed` — native-speaker approved. If `en-GB` later changes and `source_hash` no longer matches, CI **fails** (stale reviewed string).

## Weblate-oriented export/import

```bash
python scripts/content_i18n/weblate_export.py
# → content/i18n/weblate/{exercises,templates,glossary}/<locale>.json

python scripts/content_i18n/weblate_import.py --from content/i18n/weblate
```

## CI: Translations job

Workflow: [`.github/workflows/translations.yml`](../.github/workflows/translations.yml)

- Triggers: `pull_request` → `main`, `push` → `main`, `workflow_dispatch`
- Single job name: **`Translations`** (use this exact name as a required check)
- Concurrency cancel-in-progress; `contents: read`; SHA-pinned `actions/checkout` + `actions/setup-python`; pip cache
- No path filters — the check always reports on every PR to `main`

Checks (missing translations **fail**):

1. Every `en-GB` key exists in every required locale (content + app UI when those paths exist; absent app paths are skipped with an info note; present-but-malformed paths fail)
2. No orphan keys
3. ICU placeholders / plural categories valid per CLDR (`uk`/`pl`/`cs`/`be` need `few`/`many`)
4. No empty strings
5. Glossary column coverage + consistency warnings
6. Fail on any `ru` / `ru-*` locale file or `values-ru` / `ru.lproj`
7. Stale translations: warning if `machine-drafted`, **error** if `reviewed`
8. Review-status report in `$GITHUB_STEP_SUMMARY` (per-language coverage table + annotations)

Locally:

```bash
pip install -r scripts/content_i18n/requirements.txt
python scripts/content_i18n/validate.py
python -m pytest scripts/content_i18n/tests -q
```

## App UI strings

UI catalogs land with the Phase 0 scaffold. This content PR does not invent UI copy; the validator discovers:

- `shared/src/commonMain/resources/i18n/`
- `androidApp/src/main/res`, `wearApp/src/main/res`
- `iosApp/OmniGart/Localizable.xcstrings`

If those trees are missing, content validation still runs. Once merged, required locales must be complete there too.
