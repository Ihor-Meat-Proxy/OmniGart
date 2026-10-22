# Bundle-first content

Owner decision (all phases): ship a **bundled offline baseline** inside every app binary. The server is optional and only for signed between-release updates, large media (e.g. videos), food offline packs, and remote config.

## Layering

```
bundled baseline  <  verified update pack (newer packVersion)
```

1. Apps always start from `generated/content-bundle/` (Android assets / iOS+watchOS resources).
2. If a downloaded update pack passes **schema** + **signature** checks and has a higher `packVersion`, its files overlay the baseline. Missing update files fall through to the bundle.
3. Core product features must work with **zero** network access.

## What is bundled

| Section | Source | Notes |
|---------|--------|-------|
| `exercises/` | `content/exercises/` | data, templates, locales, schema, equipment presets |
| `evidence/` | `docs/evidence/` | citations + schema |
| `icons/` | `design/icons/` | SVG + manifest |
| `body-maps/` | `design/body-map/generated/paths/` | path JSON only (not 13MB previews) |
| `achievements/` | `content/achievements/` | stub pack until phase content lands |
| `exercise-images/` | `content/exercise-images/` | stills; videos stay remote/optional |

## Generate / verify

```bash
python3 scripts/pack_bundled_content.py          # write generated/content-bundle/
python3 scripts/pack_bundled_content.py --verify # CI: fail if stale
./gradlew packBundledContent verifyBundledContent
```

Commit `generated/content-bundle/` whenever sources change. CI runs `--verify` inside existing jobs.

## Shared loader

`com.omnigart.shared.content.LayeredContentLoader` — see unit tests in `shared/src/commonTest/.../content/`.

Update packs must include `signature.txt` (`test-ok:<manifest.sha256>` until production Ed25519 keys are wired).

## Platform wiring

- **Android / Wear:** `assets` source set → `generated/content-bundle`
- **iOS / watchOS:** Xcode “Pack OmniGart Content” build phase runs the same Python script, then copies the folder into the app resource bundle
