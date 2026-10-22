# OmniGart icons

Original minimal line icons on a **24×24** grid, **1.75px** rounded strokes. Geometry is authored in [`../tools/iconsource.mjs`](../tools/iconsource.mjs) (not copied from any icon library).

## Pipeline

| Stage | Output |
|-------|--------|
| SVG sources | `svg/*.svg` |
| Manifest | `manifest.json` |
| Contact sheet | `preview.png` |
| iOS | `../generated/ios/OmniGartIcons.xcassets/*` — SVG imagesets with **Preserve Vector Data** + template rendering |
| Android | `../generated/android/drawable/ic_og_*.xml` VectorDrawables |
| Compose | `../generated/compose/OmniGartIcons.kt` `ImageVector`s |

### Why SVG (not PDF / SF Symbols templates) on iOS

- One vector source feeds Android, Compose, and iOS.
- Asset catalog SVG + Preserve Vector Data is supported for template (monochrome) icons and scales cleanly with Dynamic Type layouts.
- Custom SF Symbol templates add hierarchical/variable colour layers we do not need for v1 line icons; can be added later without changing sources.

```bash
cd design/tools && npm ci && npm run icons && npm run check
```

CI: `.github/workflows/design-check.yml` runs `npm run check`.
