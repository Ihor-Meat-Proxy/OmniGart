# Content packs (bundle-first)

## Model

```
content/ (and design/icons, formulas, …)
        │
        ▼
 tools/content-packs  (deterministic build)
        │
        ├─► dist/baseline/{id}/     ← embedded in iOS/Android/KMP assets
        ├─► dist/updates/{id}/vX/   ← signed, uploaded to CDN (between app releases)
        └─► dist/cdn/{id}/vX/       ← media / OFF only (never baseline)
```

One **content epoch** (hash of sources) ties baseline and update artefacts from the same pipeline run.

### Channels

| Channel | Bundled? | CDN? | Role |
|---------|----------|------|------|
| `baseline` | Yes | No (except mirroring for transparency) | Offline baseline inside binaries |
| `update` | No | Yes | Newer than baseline; signed override |
| `media` | No | Yes | Optional videos / large assets |
| `off` | No | Yes | Open Food Facts offline packs |
| `config` | Defaults yes | Optional override | Feature flags; safe defaults always bundled |

### Manifest (`omnigart.content-pack/1`)

See [`tools/content-packs/schema/manifest.schema.json`](../tools/content-packs/schema/manifest.schema.json). Important fields:

- `version` — pack SemVer  
- `channel` — as above  
- `minAppVersion` / optional `maxAppVersion`  
- `baselineVersion` — required for `update` (which bundled generation this may replace)  
- `contentEpoch` — shared source fingerprint  
- `files[]` — path + sha256 + size  
- `archive` — `pack.tar` sha256 + size  
- `sizeBudgetBytes` — enforced for `baseline`  

Pointer object `current.json` (+ `.minisig`) at `packs/{id}/current.json` on the CDN.

### App apply rules

1. Always boot from bundled baseline.  
2. Download update only if remote config allows.  
3. Verify minisign with **pinned public key** in the app.  
4. Verify archive + file hashes.  
5. Check `minAppVersion` / `baselineVersion`.  
6. Atomic swap to override store; on failure delete partial data and keep previous good state.

### Signing

- **minisign** (offline key preferred).  
- Sign `manifest.json`, `pack.tar`, and `current.json`.  
- Public key ships in the app; rotation runbook: [`docs/security/key-rotation.md`](security/key-rotation.md).

### Commands

```bash
cd tools/content-packs
node src/build.mjs --catalog catalog.json --out dist
node src/verify.mjs --catalog-dist dist
# optional signing (offline):
# MINISIGN_SECRET_KEY_FILE=~/.minisign/omnigart.key node src/sign.mjs --dir dist/updates/exercises/v1.0.0
node src/check-bundle-size.mjs
```
