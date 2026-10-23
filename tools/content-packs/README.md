# Content pack tooling (bundle-first)

Produces **baseline** artefacts for app embedding and **update/media/off/config** artefacts for the CDN from the same sources (`catalog.json`).

## Commands

```bash
node src/build.mjs --catalog catalog.json --out dist
node src/verify.mjs --catalog-dist dist
node src/check-bundle-size.mjs
# Signing (offline preferred):
# MINISIGN_SECRET_KEY_FILE=./keys/test.key node src/sign.mjs --dir dist/updates/exercises/v1.0.0
```

Requires: Node ≥ 24, GNU/BSD `tar` with `--sort`/`--mtime`, `minisign` for sign/verify-with-sig.

See [`docs/content-packs.md`](../../docs/content-packs.md) and [`docs/APP_SIZE_BUDGET.md`](../../docs/APP_SIZE_BUDGET.md).
