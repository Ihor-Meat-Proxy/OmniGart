# minisign key rotation runbook (content packs)

## Key roles

| Key | Location | Use |
|-----|----------|-----|
| **Secret** | Offline (encrypted USB / password manager) or GitHub Environment `content-release` | Sign manifests, archives, pointers |
| **Public** | App binaries + `tools/content-packs/keys/omnigart-content.pub` (when published) | Verify before apply |

## Rotate (planned downtime: none for readers)

1. Generate new keypair offline: `minisign -G -p omnigart-content-YYYY.pub -s omnigart-content-YYYY.key`  
2. Ship an **app release** that trusts **both** old and new public keys (overlap window).  
3. Sign all new CDN packs with the **new** secret only.  
4. After ≥ 95% of active users are on a dual-trust or new-only build (or after N weeks), ship an app that drops the old public key.  
5. Destroy or archive the old secret; record rotation in this doc’s changelog table below.  
6. Purge CDN `current.json` entries so clients refresh pointers.

## Compromise response

1. Revoke by shipping an app build that **removes** the compromised public key ASAP.  
2. Re-sign baselines in the next store release; yank CDN `current.json` to force baseline-only until resign complete.  
3. Rotate Bunny/Hetzner API tokens.  
4. Audit recent pack uploads (object listing by time).

## Offline signing (preferred)

```bash
# On air-gapped machine with built artefacts copied in:
minisign -Sm manifest.json -s omnigart-content.key
minisign -Sm pack.tar -s omnigart-content.key
minisign -Sm current.json -s omnigart-content.key
# Copy *.minisig next to objects, then upload from an online machine.
```

## CI signing (optional)

Only with GitHub Environment **`content-release`** required reviewers. Secret stored as `MINISIGN_SECRET_KEY`. Prefer offline for production launches.

## Rotation log

| Date | Old key id | New key id | Notes |
|------|------------|------------|-------|
| — | — | — | Initial key TBD by owner (Q in OPEN_QUESTIONS) |
