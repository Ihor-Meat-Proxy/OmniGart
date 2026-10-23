# OmniGart terminology glossary

Canonical draft terms for exercise content localisation live in `glossary.csv`.

## Columns

| Column | Meaning |
|--------|---------|
| `term_id` | Stable machine id (`domain.slug`, optional `.gym` for vernacular) |
| `en-GB` | Source English (British spelling where it differs) |
| `uk` … `zh-Hans` | Target locales from `content/i18n/meta/languages.json` |
| `pos` | Part of speech (`n`, `v`, `adj`) |
| `domain` | Topic bucket (`anatomy`, `gym`, `movement`, `equipment`, …) |
| `source` | Primary citation or authority |
| `notes` | Disambiguation, script, or usage notes |
| `status` | Always `draft` until human review |

Russian (`ru`) is **never** a column, pivot, or source language. Ukrainian sports-science forms are preferred over Russian calques; Belarusian uses Narkamaŭka; Kazakh is Cyrillic only.

## Coverage

- Anatomical muscles from the exercise schema (Terminologia Anatomica) plus gym common names where they differ
- Movement patterns and equipment enums used by the library
- Training-science abbreviations (RIR, RPE, 1RM, …)
- Yoga Sanskrit names with local glosses; Pilates Method Alliance exercise names
- Swimming strokes/drills, technique cues, and high-frequency exercise stems

## Sources

See [SOURCES.md](./SOURCES.md) for the bibliography referenced in the `source` column.

## Regeneration

```bash
python3 scripts/content_i18n/generate_glossary.py
```

Edit the generator (or the CSV) carefully: keep column order exact and do not add a Russian locale.
