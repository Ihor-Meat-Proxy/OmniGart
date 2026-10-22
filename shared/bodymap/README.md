# shared/bodymap

Pure Kotlin Multiplatform module (JVM target for tests) for OmniGart body-map **computation**. No iOS/Android UI wiring.

## Responsibilities

- Fractional per-muscle training load (`WorkoutHeatmapCalculator`) with configurable `InvolvementWeights` (default primary 1.0 / secondary 0.5 per Pelland et al. 2025).
- Normalise loads into 5 intensity levels + idle (`IntensityNormalizer`, `HeatmapLegend`).
- Weekly volume vs `VolumeLandmarks` from `:shared:planning`.
- Exercise view choice (`ViewChooser`) and taxonomy → region expansion (`TaxonomyRegions`).
- JVM validator that every exercise muscle id exists in `design/body-map` mapping.

## Tests

```bash
./gradlew :shared:bodymap:jvmTest
```

See `design/body-map/README.md` for SVG source of truth and rendering guidance.
