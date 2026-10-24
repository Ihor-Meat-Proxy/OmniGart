# OmniGart equipment & exercise illustrations

Flat, friendly cartoon badge set for every equipment id and exercise id. Neutral grey figure, worked muscles in brand accent (`#0F766E`), equipment in dark grey, white round badge, uniform line weight.

## Layout

| Path | Role |
|------|------|
| `equipment/equip_<id>.svg` | Equipment badge sources |
| `exercises/ex_<id>.svg` | Exercise badge sources |
| `parts/` | Reusable body poses + equipment primitives (JS modules used by the generator) |
| `index.json` | Id → asset map (built from content + equipment seed) |
| `equipment-catalogue-seed.json` | Anticipated ~150-item equipment catalogue (coordinates with the parallel catalogue PR) |
| `tools/` | Generator, coverage check, contact sheets |
| `generated/previews/` | Contact sheets |
| `../generated/ios/OmniGartIllustrations.xcassets` | iOS / watch PDF + PNG imagesets |
| `../generated/android/drawable/` | Android / Wear VectorDrawables (`il_og_*`) |

## Style

- ViewBox `0 0 128 128`, circular white badge
- Figure fill `#94A3B8`, stroke `#64748B`
- Accent (primary muscles) `#0F766E`
- Equipment `#334155`
- Stroke weight ≈ 2.25px

## Build

```bash
cd design/illustrations/tools
npm ci
npm run build    # SVGs + index + platform assets + contact sheets
npm run check    # structural validity; coverage gaps are warnings
```

`npm run build` is also invoked from the top-level design pack hooks and regenerates `index.json` from `content/exercises/data/*.json`, `EquipmentCatalogue` ids, and `equipment-catalogue-seed.json`.

## Coverage CI

The illustrations job reports missing art as a **warning** (exit 0) until the expanded equipment catalogue lands and ids stabilize. After that, flip `--coverage-warn` off in the workflow to fail on gaps.

## Runtime helpers

- Swift: `IllustrationAsset` in `design/generated/ios/Sources/`
- Kotlin / Compose: `OmniGartIllustrations` in `design/generated/compose/`
