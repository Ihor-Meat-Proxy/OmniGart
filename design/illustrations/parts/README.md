# Illustration parts

Reusable geometry lives as JavaScript modules under `../tools/parts/`:

- `body.mjs` — cartoon figure poses and muscle-region highlighting
- `equipment.mjs` — dark-grey equipment primitives keyed by `shape` id

SVG sources in `equipment/` and `exercises/` are **generated** from those parts by `../tools/generate.mjs`. Do not hand-edit the `equip_*.svg` / `ex_*.svg` files; change the parts or generator instead.
