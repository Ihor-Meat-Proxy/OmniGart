# Wire bundled content into strength logging

## Goal

Consume the offline `generated/content-bundle/` via shared `LayeredContentLoader` /
`ContentStore` so the strength logger seeds SQLDelight from real exercise JSON instead of
only the four hard-coded demos.

## Shipped (this PR)

- `BundledExerciseCatalog` — lists `exercises/data/*.json`, maps id / contentKey / muscles /
  unilateral / loadModel / bodyweightFraction → `DemoStrengthExercise`; falls back to
  `StrengthLoggingService.demoCatalog()` when the pack is empty.
- `StrengthLoggingService.loadExercises(loader?)` / `ensureDemoExercises()` prefer an
  injectable `ContentStore` (Android/Wear: `AssetContentStore` on app assets).
- Host test: LayeredContentLoader-backed fixture catalog returns exercises; existing
  `StrengthLoggingServiceTest` keeps demo fallback (no store injected).
- Docs: `docs/CONTENT_BUNDLE.md` notes strength logger consumption.

Offline-first only — no network reads.

## Deferred module PRs (land when green, not merged in this PR)

These stacks may already have green CI on their own branches; **do not merge them into this
wire-content PR**:

| Area | Typical branch / concern |
|------|--------------------------|
| Sensors / Health Connect deepening | health / sensor adapter follow-ups |
| Hydration / water reminders | phase-2 food-water |
| Body map UI consumption | `shared/bodymap` + phone shells |
| Achievements pack UI | phase-6 |
| Insights / readiness surfaces | phase-3+ |

Land each when its own CI is green; keep this PR scoped to content → strength catalog wiring.
