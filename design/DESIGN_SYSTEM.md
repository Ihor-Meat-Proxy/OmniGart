# OmniGart Design System

Single source of truth for the open-source fitness, nutrition, and recovery apps (iOS 26+ Liquid Glass / SwiftUI, watchOS, Android Compose Material 3 Expressive, Wear OS). Tokens live in [`tokens/omnigart.tokens.json`](tokens/omnigart.tokens.json) (W3C Design Tokens / DTCG). Generators under [`tools/`](tools/) emit Swift, asset catalogs, Android resources, and Compose theme Kotlin into [`generated/`](generated/).

## Principles

### One-hand first
- Primary actions sit in the **bottom thumb zone** (`space.thumbZone` ≈ 120pt/dp from the home indicator / gesture bar).
- Prefer **bottom sheets** and the **quick-log** control over top-bar actions.
- Minimum hit targets: **44pt** (iOS) / **48dp** (Android comfort). No critical controls in the status-bar / large-title collision band.
- Tab bar hosts Food · Workouts · Recovery · Planner · Profile; quick-log FAB / sheet sits above the tab bar, offset for reachability.

### Informative, minimal, light
- **Light theme first**, with a full dark theme. Restrained neutrals + **one accent** (teal).
- Semantic readiness: **Go / Moderate / Easy / Rest**.
- HR zones **1–5** use an Okabe–Ito–inspired colourblind-safe scale.
- Generous whitespace; data cards use **progressive disclosure** (summary → sheet → detail).

### Accessibility
- WCAG **AA** contrast for text and essential iconography, including labels drawn over Liquid Glass / Material translucent surfaces (prefer opaque text scrims or `text.onGlass` tokens; never rely on hairline weight alone).
- Dynamic Type / font scaling: type roles map to system text styles; metrics scale with environment size categories.
- Motion tokens honour Reduce Motion (`motion.reduceMotion`).

## Colour

| Role | Token | Notes |
|------|-------|-------|
| Accent | `color.brand.accent` | `#0F766E` — CTAs, selected tab, quick-log |
| Readiness Go | `color.readiness.go` | Green |
| Moderate | `color.readiness.moderate` | Amber |
| Easy | `color.readiness.easy` | Blue |
| Rest | `color.readiness.rest` | Violet (distinct from HR zones) |
| HR Z1–Z5 | `color.hrZone.z1`…`z5` | Okabe–Ito: blue → green → yellow → orange → vermillion |

Surfaces: `canvas` / `raised` / `sunken` / `glass` / `glassBorder` under `color.surface.light|dark`. Text roles under `color.text.light|dark`.

**Liquid Glass:** Use `surface.*.glass` + `glassBorder` for bars and sheets. Body text over glass must meet AA against the **worst-case blended** backdrop; pair with `text.*.onGlass` or a solid material fallback when contrast fails.

## Typography

Platforms map `font.family.sans|display` to **SF Pro** (iOS/watchOS) and **Roboto / Material type** (Android/Wear) until a custom face ships. Roles: caption → heroMetric. Numeric logging prefers `font.family.mono` / tabular figures.

## Spacing, radius, elevation, motion

- 4px base scale (`space.1`…`space.10`), plus `hitMin`, `hitComfort`, `thumbZone`.
- Radii: `sm`…`xl`, `sheet` (28), `pill`.
- Elevation 0–3 (soft slate-tinted shadows; prefer borders on glass).
- Durations: instant 80 → sheet 420ms; standard / emphasized / accel / decel curves.

## Navigation

### Phone tab bar
1. **Food** — logging, barcode, water  
2. **Workouts** — sessions, sets, disciplines  
3. **Recovery** — sleep, HRV, stress, readiness  
4. **Planner** — schedule, periodisation  
5. **Profile** — body, achievements, settings  

Icons: [`icons/`](icons/) (`food`, `workouts`, `recovery`, `planner`, `profile`). Selected = accent fill/tint; unselected = secondary text.

### Quick log
Persistent control in the thumb zone opening a sheet: Water · Barcode · Food · Add set (`water`, `barcode`, `food-log`, `add-set`).

## Watch guidelines (watchOS / Wear OS)

- Prefer **complications + short flows** over mirroring phone IA.
- Primary watch jobs: log set, log water, glance readiness, start workout, HR zone glance.
- Padding `layout.watchPadding` (6); avoid nested scroll where digital crown / rotary can page.
- Crowded tabs → **app list / launcher** + 2–3 top-level pages max.
- Always-on / ambient: reduce motion, increase contrast, hide non-essential chrome.
- Wear: Material 3 Expressive motion, large touch targets, tile + complication parity with readiness and next workout.

## Data cards & progressive disclosure

1. **Glance** — one metric + readiness chip  
2. **Card expand / sheet** — sparkline, evidence ⓘ, primary CTA  
3. **Full screen** — history, edits, sources  

Do not put stats strips or schedules in the first viewport of marketing surfaces; in-app home may show **one** readiness card above the fold.

## Generation

```bash
cd design/tools
npm ci
npm run build        # tokens + icons + logo
npm run check        # SVG validity + generated freshness
```

| Output | Path |
|--------|------|
| Swift Color/Font/Metrics | `generated/ios/Sources/` |
| Colour asset catalog | `generated/ios/OmniGartColors.xcassets/` |
| Icon imagesets (SVG, Preserve Vector Data) | `generated/ios/OmniGartIcons.xcassets/` |
| Android colours / dimens | `generated/android/values/` |
| VectorDrawables | `generated/android/drawable/` |
| Compose theme + ImageVectors | `generated/compose/` |

Tooling (Oct 2026 verified): **Style Dictionary 5.7.0**, **@resvg/resvg-js 2.6.2** (latest stable), **sharp 0.35.5**.

## Related

- Icons: [`icons/README.md`](icons/README.md)  
- App icon picker: [`logo/APP_ICON.md`](logo/APP_ICON.md)  
- Wireframes: [`wireframes/`](wireframes/)  
- Open questions: [`OPEN_QUESTIONS.md`](OPEN_QUESTIONS.md)
