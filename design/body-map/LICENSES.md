# Body-map asset licences

## Adapted anatomical figures

| Item | Detail |
|------|--------|
| Asset | Vector muscle path data for male/female × front/back |
| Upstream | [`@musclemap/assets`](https://www.npmjs.com/package/@musclemap/assets) **1.1.0** from [Jsplice/MuscleMap](https://github.com/Jsplice/MuscleMap) |
| Licence | **MIT** (see [`vendor/musclemap/LICENSE`](vendor/musclemap/LICENSE)) |
| Copyright | Copyright (c) 2026 Jsplice / MuscleMap contributors |
| Provenance | Original Inkscape-traced SVG paths (upstream `ASSET_PROVENANCE.md`). Photoreal demo webps are **not** vendored. |
| Vendored copy | [`vendor/musclemap/`](vendor/musclemap/) |
| OmniGart changes | Remapped / subdivided paths to stable OmniGart region ids; restyled fills/strokes to design tokens (light + dark); female/male figures retained from upstream sex variants. |

MIT is compatible with AGPL-3.0 distribution of OmniGart. The MIT notice above must be preserved in distributions that include these paths.

## OmniGart-authored files

| Item | Licence |
|------|---------|
| `mapping/taxonomy-to-regions.json`, intensity scale, tooling, `shared/bodymap` | Same as the OmniGart repository (AGPL-3.0) |
| Preview PNGs generated from the adapted SVGs | Derived from the MIT paths + OmniGart tooling |

## Rejected / not used

The following were evaluated and **not** vendored:

- Wikimedia `Muscles front and back.svg` — **CC BY-SA 4.0** (ShareAlike friction with AGPL packaging of adapted assets)
- `kit-g/flutter-body-atlas` SVGs — CC BY 4.0 (compatible, but MuscleMap already provides MIT male/female front/back with higher fitness-app fidelity)
- Geometric procedural figures previously in this package — rejected as non-human / “robotic”
