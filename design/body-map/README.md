# OmniGart body maps

Human anatomical figures for exercise highlighting and workout / weekly volume heatmaps.

## Asset source & licence

Figures are **adapted** from MIT-licensed MuscleMap path data (`@musclemap/assets` 1.1.0 / [Jsplice/MuscleMap](https://github.com/Jsplice/MuscleMap)) — original Inkscape-traced vectors, not atlas tracings. See [`LICENSES.md`](LICENSES.md) and [`vendor/musclemap/`](vendor/musclemap/).

OmniGart remaps / subdivides surfaces to stable region ids, restyles with design tokens (light + dark), and keeps male/female front/back variants.

## What ships here

| Asset | Path |
|-------|------|
| Base SVGs (male/female × front/back) | [`svg/`](svg/) |
| Themed SVG variants (light/dark) | [`svg/themed/`](svg/themed/) |
| Vendored MIT source paths | [`vendor/musclemap/`](vendor/musclemap/) |
| Taxonomy → region mapping | [`mapping/taxonomy-to-regions.json`](mapping/taxonomy-to-regions.json) |
| Intensity colour scale (token refs) | [`mapping/intensity-scale.json`](mapping/intensity-scale.json) |
| Exported path JSON (runtime tint) | [`generated/paths/`](generated/paths/) |
| Sample exercise PNGs | [`generated/previews/samples/`](generated/previews/samples/) |
| Base figures sheet | [`generated/previews/figures/base-figures-sheet.png`](generated/previews/figures/base-figures-sheet.png) |
| Contact sheet (all exercises) | [`generated/previews/contact-sheet-all-exercises.png`](generated/previews/contact-sheet-all-exercises.png) |
| Shared KMP computation | [`../../shared/bodymap`](../../shared/bodymap) |

## Design approach

- **Source of truth:** SVG. Each muscle region is its own `<path id="…">` (viewBox `0 0 1024 1536`).
- **Runtime tinting:** export path `d` strings to JSON for SwiftUI `Path` / Compose `PathParser`. Apps do **not** ship 129 baked exercise images.
- **Style:** minimalist human muscle chart, light theme first (+ dark), neutrals + teal accent. Heatmaps use Okabe–Ito-inspired `color.hrZone.z1`–`z5`.

## Anatomy & naming

- Region naming follows **Terminologia Anatomica (TA2)** preferred English terms where the exercise taxonomy is coarser (clavicular/sternal pecs, vasti, gastroc/soleus, upper/middle traps, brachialis, forearm flexors/extensors, …).
- Exercise-library muscle ids stay **stable**; fine regions are reached via the mapping table.
- Proxies: `transverse_abdominis` → rectus + obliques; `rotator_cuff` → scapular proxy; `full_body` → all drawable regions.

## Tooling

```bash
cd design/body-map/tools
npm ci
npm run build    # adapt MuscleMap → figures → paths → previews
npm run check    # mapping ↔ exercises ↔ SVG ↔ golden hashes
```

Adapter: `adapt_musclemap.py` (Python 3 + `shapely` + `svgpathtools`). Raster: `@resvg/resvg-js@2.6.2` + `sharp@0.35.5`.

### Golden / snapshot tests

- `tools/golden/paths-index.sha256` — hash of exported path JSON digests.
- Kotlin unit tests cover fractional load math, intensity bins, view choice, and the exercise↔mapping validator.

## CI

GitHub Actions job **Body map** (`.github/workflows/body-map.yml`).

## Heatmap method (`shared/bodymap`)

```text
effectiveSets(muscle) = Σ hardSets × weight(involvement)
```

Default weights (configurable): primary **1.0**, secondary **0.5** — Pelland et al. 2025 fractional set counting (SportRχiv [10.51224/srxiv.460](https://doi.org/10.51224/srxiv.460)). Normalise to 5 intensity levels; weekly view vs `VolumeLandmarks`.

## Limitations

- Some fine regions (pec heads, VL strip, soleus band, serratus, hip flexors) are geometric subdivisions of coarser MuscleMap surfaces — organic outer edges retained, internal cut lines are softer approximations.
- Deep muscles (TVA, rotator cuff) use surface proxies.
- L/R paths are bilateral; unilateral laterality not yet visualised.
