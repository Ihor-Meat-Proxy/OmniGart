# Self-hosting the content plane

Official apps embed **baseline** packs and verify CDN updates with the OmniGart minisign **public** key. Self-hosters can:

1. **Mirror** official signed packs (integrity unchanged), or  
2. **Fork** — build packs from source, sign with your own key, and ship a forked app that trusts that key.

There are **no user accounts** on this plane.

## Architecture (matches hosted default)

- Object storage (S3-compatible) + HTTPS reverse proxy (Caddy)  
- Optional CDN in front  
- Same pack layout: `packs/{id}/v/{semver}/`, `packs/{id}/current.json`, signatures alongside  

A VM is **optional** for hosted OmniGart; for self-host it is the usual way to run Caddy + Garage.

## MinIO status (Oct 2026)

MinIO Community Edition’s GitHub repository is **archived / unmaintained** (AGPLv3 source remains, but no maintained releases). **Do not** use MinIO CE for new OmniGart self-host guides. Prefer:

- **Garage** (AGPL-3.0, actively released) — default below  
- **SeaweedFS** (Apache-2.0) — alternative  

## Docker Compose (Caddy + Garage)

Files: [`infra/self-host/`](../infra/self-host/).

```bash
cd infra/self-host
cp .env.example .env   # set DOMAIN, EMAIL, GARAGE keys
docker compose up -d
# Then upload packs with AWS-compatible tooling to Garage bucket `omnigart-content`
```

Images pinned in compose:

- `caddy:2.11.7-alpine`  
- `dxflrs/garage:v2.4.1`  

## Pointing an app build at your server

- Remote config / build flavor: `cdn.pointerBaseUrl = https://content.your.domain`  
- Either keep OmniGart public key (official packs only) or replace with your public key in a fork.

## Hardening checklist

- [ ] TLS via Caddy  
- [ ] Firewall: 80/443 only (SSH restricted)  
- [ ] Anonymized access logs  
- [ ] minisign verify in the app  
- [ ] No cookies / no auth gate that logs identities  
- [ ] Backups of Garage data + secret key offline  

See also [`docs/security/content-server.md`](security/content-server.md).
