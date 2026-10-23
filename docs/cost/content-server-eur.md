# Content plane — cost estimate (EUR, excl. VAT)

**Bundle-first** means most users never download library/translations/images again after install. CDN traffic is dominated by (1) occasional signed updates, (2) optional exercise videos, (3) Open Food Facts packs, (4) rare remote-config fetches.

Prices checked against public pages (Oct 2026): Hetzner Object Storage base **€6.49/mo** (1 TB storage + 1 TB egress included); Bunny CDN Standard EU/NA **$0.01/GB** (~**€0.009/GB** at 0.92 USD/EUR illustrative — use live FX); Bunny Storage Standard single region **$0.01/GB**; CAX11 **€6.49/mo** if optional VM enabled. Treat FX and promotions as approximate.

## Recommended hosted stack (default)

| Component | Role | Monthly baseline |
|-----------|------|------------------|
| Bunny Storage `DE` | Origin for updates / media / OFF / config | ~€0.01–1 (few GB) + $1 CDN minimum often covers early stage |
| Bunny Pull Zone | CDN, anonymized logs, CORS, TLS | Bandwidth only (+ ~$1 min) |
| Hetzner Object Storage | Optional alternate origin | €6.49 if used |
| CAX11 + Caddy | **Optional**, off by default | €0 (default) or €6.49 |

## Traffic scenarios (CDN egress, order-of-magnitude)

Assumptions: 10 MB average *update* pull per updating user per month; 5% of MAU pull an update; video watchers are a small cohort; OFF pack ~50 MB when opted in.

| Scenario | MAU | Updates | Video | OFF | Est. CDN egress | Est. CDN €/mo | Storage €/mo | Total €/mo (no VM) |
|----------|-----|---------|-------|-----|-----------------|---------------|--------------|--------------------|
| Launch / dogfood | 1k | 50×10 MB = 0.5 GB | ~2 GB | ~1 GB | ~4 GB | ~€1–2 | ~€1 | **~€2–5** |
| Early growth | 50k | 25k×10 MB = 250 GB | 100 GB | 50 GB | ~400 GB | ~€35–40 | ~€2–5 | **~€40–50** |
| Scale | 500k | 100k×10 MB = 1 TB | 1 TB | 200 GB | ~2.2 TB | ~€200 | ~€10–20 | **~€210–230** |

With a **VM always on**, add **~€6.49/mo**. Bundle-first avoids multi-TB “full library resync” patterns that would dominate a CDN-only content model.

## Cost controls

- Keep still images in-bundle; videos optional and adaptive bitrate.  
- Immutable versioned paths → high cache hit ratio.  
- OFF packs user-initiated only.  
- No log storage / forwarding by default.  
- Do not enable Origin VM until a concrete need appears.

## Sources

- https://www.hetzner.com/storage/object-storage/  
- https://www.hetzner.com/cloud/cost-optimized/  
- https://bunny.net/pricing/  
- https://bunny.net/pricing/storage/  

Re-verify before budget approval; providers change list prices.
