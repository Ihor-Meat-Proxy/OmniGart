# OmniGart body maps

Original anatomical figures for exercise highlighting and workout / weekly volume heatmaps.

## What ships here

| Asset | Path |
|-------|------|
| Base SVGs (male/female × front/back) | [`svg/`](svg/) |
| Themed SVG variants (light/dark) | [`svg/themed/`](svg/themed/) |
| Taxonomy → region mapping | [`mapping/taxonomy-to-regions.json`](mapping/taxonomy-to-regions.json) |
| Intensity colour scale (token refs) | [`mapping/intensity-scale.json`](mapping/intensity-scale.json) |
| Exported path JSON (runtime tint) | [`generated/paths/`](generated/paths/) |
| Sample exercise PNGs | [`generated/previews/samples/`](generated/previews/samples/) |
| Contact sheet (all exercises) | [`generated/previews/contact-sheet-all-exercises.png`](generated/previews/contact-sheet-all-exercises.png) |
| Shared KMP computation | [`../../shared/bodymap`](../../shared/bodymap) |

## Design approach

- **Source of truth:** SVG. Each muscle region is its own `<path id="…">`.
- **Runtime tinting (chosen):** export path `d` strings to JSON so SwiftUI `Path` and Compose `PathParser` can fill regions dynamically. Apps do **not** ship 129 baked exercise images.
- **Why not per-exercise rasters in-app?** Binary size, theme switching, and heatmap intensity levels all require dynamic fills. PNGs here are **preview / CI artifacts** only.
- **Style:** minimalist filled regions, light theme first (+ dark variants), neutrals + teal accent from design tokens. Intensity heatmaps use the Okabe–Ito-inspired HR-zone sequential scale (`color.hrZone.z1`–`z5`).

## Anatomy & naming

- Figures are **original geometric constructions** from artistic proportions (≈8-head canon) and sex-specific shoulder/waist/hip ratios. They are **not** traced from copyrighted atlases (e.g. no Netter / Gray plate tracing).
- Region naming follows **Terminologia Anatomica (TA2)** preferred English terms where the exercise taxonomy is coarser (e.g. `pectoralis_major` → clavicular + sternal heads; `quadriceps` → rectus femoris + vasti; `calves` → gastrocnemius + soleus; `trapezius` → upper + middle).
- Exercise-library muscle ids are **stable**. Fine regions are reached via the mapping table, never by renaming taxonomy ids.

### Covered regions (minimum)

Pectoralis major (clavicular/sternal), anterior / lateral / posterior deltoid, biceps, brachialis, triceps, forearm flexors/extensors, rectus abdominis, obliques, serratus anterior, upper/middle trapezius, rhomboids, latissimus dorsi, erector spinae, gluteus maximus/medius, hip adductors, hip flexors, quadriceps (rectus femoris/vasti), hamstrings, gastrocnemius, soleus, tibialis anterior, neck. Proxies: `transverse_abdominis` → rectus + obliques surface; `rotator_cuff` → small scapular proxy; `full_body` → all drawable regions.

## Exercise highlights

Primary muscles → strong accent (`color.brand.accent`). Secondary → same hue at ~35% opacity. Front / back / both chosen from the mapping’s `views` (see also `ViewChooser` in `shared/bodymap`).

## Workout heatmap (`shared/bodymap`)

```text
effectiveSets(muscle) = Σ hardSets × weight(involvement)
```

Default weights (configurable):

| Involvement | Weight | Basis |
|-------------|--------|-------|
| Primary (direct) | 1.0 | Direct sets in Pelland et al. 2025 |
| Secondary (indirect) | 0.5 | **Fractional** indirect counting |

**Evidence (honest):** Pelland et al., *The Resistance Training Dose-Response* (SportRχiv [10.51224/srxiv.460](https://doi.org/10.51224/srxiv.460); also MSSE 2025 abstract). Their multi-level meta-regressions found **fractional** quantification of indirect sets (×0.5) fit hypertrophy dose–response better than total or direct-only. OmniGart defaults match that convention; weights remain injectable for experiments. This is **not** a claim that every synergist receives exactly half the stimulus.

Loads normalise into **5 intensity levels** (+ idle) with the colourblind-safe sequential scale above. Figure sex follows the user’s biological sex from profile (`female` / `male`; unspecified → male figure). Legend: `HeatmapLegend`.

Weekly volume heatmap compares fractional sets to `VolumeLandmarks` from `shared/planning`.

## Tooling

```bash
cd design/body-map/tools
npm ci
npm run build    # figures → paths → previews
npm run check    # mapping ↔ exercises ↔ SVG ↔ golden hashes
```

Python 3 generates the four base SVGs (`generate_figures.py`). Node exports paths (`export-paths.mjs`) and renders previews with `@resvg/resvg-js@2.6.2` + `sharp@0.35.5` (latest stable, matching design/tools).

### Golden / snapshot tests

- `tools/golden/paths-index.sha256` — hash of exported path JSON content digests.
- `npm run check -- --golden` asserts the hash; regenerate with `--write-golden` after intentional path edits.
- Kotlin unit tests cover fractional load math, intensity bins, view choice, and the exercise↔mapping validator.

## CI

GitHub Actions job **Body map** (`.github/workflows/body-map.yml`) runs `npm ci` + `npm run check` and `./gradlew :shared:bodymap:jvmTest`.

## How apps should render

1. Load `generated/paths/{sex}-{view}.json`.
2. For each region id, build a platform path from `regions[id].d`.
3. Fill with idle / primary / secondary / intensity colour.
4. Overlay legend from `HeatmapLegend` / `intensity-scale.json`.

No app-shell wiring is included in this package (Phase 0 scaffold owns UI).

## Limitations

- Geometric silhouettes are schematic, not medical-illustration grade.
- Deep muscles (TVA, rotator cuff) use surface proxies.
- Left/right paths are mirrors; unilateral exercise bias is not yet visualised.
- Contact-sheet PNGs are male/light only to keep the artifact bounded.
