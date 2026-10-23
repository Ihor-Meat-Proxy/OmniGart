# OmniGart content infrastructure

**Bundle-first:** exercise library, templates, translations, evidence, icons/body maps/achievements, exercise stills (WebP/HEIC), and formulas ship **inside app binaries** and work offline. The content plane is only for:

| CDN role | Channel |
|----------|---------|
| (a) Signed updates between app releases | `update` |
| (b) Optional exercise videos (on-demand) | `media` |
| (c) Open Food Facts offline packs | `off` |
| (d) Optional remote config (safe defaults bundled) | `config` |

No user accounts, no tracking, EU hosting, self-hostable. Packs are minisign-signed; apps verify before override and roll back on failure.

## Architecture decision

| Option | Verdict |
|--------|---------|
| **Object storage + CDN only** | **Default.** Smallest attack surface; enough for static signed objects |
| Always-on VM (Caddy) | **Optional** (`enable_origin_vm = true`) for custom origin / self-host parity |

Hosted default: **Bunny Storage (DE) + Bunny Pull Zone**. Optional Hetzner CAX11 documented in OpenTofu but off. Hetzner Object Storage remains a documented S3 origin alternative (not a first-class `hcloud` bucket resource).

Size budgets: [`docs/APP_SIZE_BUDGET.md`](../docs/APP_SIZE_BUDGET.md). Pack format: [`docs/content-packs.md`](../docs/content-packs.md).

## Layout

```
infra/content/       # OpenTofu — plan/validate in CI; apply is owner-only
infra/self-host/     # docker compose: Caddy 2.11.7 + Garage v2.4.1
```

## Tooling (verified Oct 2026)

| Tool | Version |
|------|---------|
| OpenTofu | 1.13.2 |
| hetznercloud/hcloud | 1.70.0 |
| BunnyWay/bunnynet | 0.19.2 |

```bash
cd infra/content
tofu init -backend=false
tofu fmt -check -recursive
tofu validate
```

Secrets the owner must add: [`content/SECRETS.md`](content/SECRETS.md).  
Threat model: [`docs/security/content-server.md`](../docs/security/content-server.md).  
Costs: [`docs/cost/content-server-eur.md`](../docs/cost/content-server-eur.md).  
Self-host: [`docs/SELF_HOSTING.md`](../docs/SELF_HOSTING.md).
