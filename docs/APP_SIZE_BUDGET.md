# App size budget (bundle-first)

OmniGart ships a **fully offline baseline** inside the app binaries. The content CDN only delivers signed updates, optional videos, Open Food Facts packs, and optional remote config. This document is the size contract enforced by CI (`tools/content-packs/src/check-bundle-size.mjs` + `docs/APP_SIZE_BUDGET.json`).

## Install-size targets

| Platform | Target download / install (uncompressed guidance) | Notes |
|----------|---------------------------------------------------|--------|
| iOS (App Store) | **≤ 200 MB** thinned install preference; keep universal under store warnings | Prefer asset catalogs + on-demand for video |
| Android (Play) | **≤ 150 MB** base module before PAD | Evaluate Play Asset Delivery for video later |
| Combined bundled *content* (all baseline packs) | **≤ 80 MB** (`totals.maxBundledContentBytes`) | Leaves headroom for code, fonts, native libs |

Machine-readable limits: [`APP_SIZE_BUDGET.json`](APP_SIZE_BUDGET.json).

## What is bundled (always offline)

| Content | Pack id (planned) | Archive budget | Per-asset limits |
|---------|-------------------|----------------|------------------|
| Exercise library + workout templates | `exercises`, `templates` | 5 MB JSON total (gzipped tar) | ≤ 32 KB per exercise JSON |
| Translations | `translations` | 8 MB | ≤ 1.5 MB per locale pack slice |
| Evidence library | `evidence` | 3 MB | citations text only; no PDFs in-bundle |
| Icons / body maps (vector) / achievements | `icons`, `body-maps`, `achievements` | 4 MB | SVG/PDF vectors; no raster body maps |
| Equipment / exercise illustrations | `illustrations` | 3 MB | SVG badge sources in bundle; platform PDF/PNG + VectorDrawable live under `design/generated/` |
| Exercise still images | `exercise-images` | **45 MB** | **WebP ≤ 80 KB**; HEIC allowed on iOS if ≤ 100 KB; max edge 1024 px |
| Formulas config | `formulas` | 64 KB | single JSON |
| App config safe defaults | `app-config` | 16 KB | feature flags with offline defaults |

Sum of the above must stay under **80 MB** compressed pack archives (CI measures `pack.tar` bytes from the deterministic builder).

## What is *not* bundled (CDN / on-demand)

| Content | Channel | Delivery |
|---------|---------|----------|
| Signed content updates newer than the bundled baseline | `update` | CDN; apply only after minisign + schema + `minAppVersion`; rollback on failure |
| Exercise videos | `media` | On-demand download + disk cache; evaluate **iOS Background Assets** and **Android Play Asset Delivery** vs plain CDN (see open questions) |
| Open Food Facts offline DBs | `off` | Large, frequent updates — CDN only |
| Remote config overrides | `config` | Optional; bundled defaults always win if fetch/verify fails |

## Image pipeline rules (exercise stills)

1. Source masters stay out of the app repo (or under LFS with CI conversion).  
2. Builder emits **WebP** (and optional HEIC for iOS) into `exercise-images` baseline pack.  
3. Reject assets over per-file limits in CI (wire when media pipeline lands).  
4. Prefer one hero still per exercise; additional angles are CDN/`media` only.

## Update / rollback behaviour (app)

1. App starts from **bundled baseline** (always works offline).  
2. If updates enabled, fetch `packs/{id}/current.json` + `.minisig`.  
3. Apply downloaded pack only when: signature valid, `minAppVersion` ≤ app version, `version` > active version, and (for `update`) compatible `baselineVersion`.  
4. On any failure (hash, signature, schema, I/O): **discard** the download and keep the last good baseline or previously verified override.

## CI

```bash
cd tools/content-packs
node src/build.mjs --catalog catalog.json --out dist
node src/verify.mjs --catalog-dist dist
node src/check-bundle-size.mjs --index dist/build-index.json --budget ../../docs/APP_SIZE_BUDGET.json
```

Production catalogs will point at `content/` sources instead of fixtures; budgets stay in `APP_SIZE_BUDGET.json`.
