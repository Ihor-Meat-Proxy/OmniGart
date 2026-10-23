# Threat model — content distribution (bundle-first)

## Assets

| Asset | Sensitivity | Notes |
|-------|-------------|-------|
| Bundled baseline packs | Integrity | Ship inside AGPL app; tampering requires app binary compromise |
| CDN update / media / OFF packs | Integrity | Public reads; **authenticity** via minisign |
| minisign secret key | Critical | Offline preferred; never in app |
| Bunny / Hetzner API keys | High | Upload + purge only |
| Access logs | Low (PII risk) | Truncated IPs only |

**Out of scope for this plane:** user accounts, workouts, health data, auth tokens. Those never touch the content CDN.

## Trust boundaries

1. **App binary** — trusted baseline + pinned minisign public key.  
2. **CDN / object storage** — untrusted for integrity (anyone can MITM or compromise origin). Apps **must** verify signatures before applying.  
3. **Release operators** — trusted to sign with the private key after Environment approval.  
4. **Self-hosters** — may replace CDN URL; must either use OmniGart public key (official packs) or rebuild apps with their own key (fork).

## Threats and mitigations

| Threat | Mitigation |
|--------|------------|
| Malicious pack on CDN | minisign verify; hash manifest; reject on failure; keep baseline |
| Rollback / downgrade attack | Monotonic `version`; app ignores `version ≤` active; pin `minAppVersion` |
| Compromised CDN cache poisoning | Immutable `/v/{semver}/` paths; purge only `current.json`; signatures cover body |
| Supply-chain (CI signs malware) | Manual Environment approval; prefer offline signing; code review on content |
| Log retention / IP tracking | Bunny `log_anonymized=OneDigit`; no product analytics; disable log storage by default |
| Optional origin VM compromise | VM off by default; firewall 80/443; restricted SSH; unattended upgrades |
| Oversized update DoS on device | Size budgets; app enforces max download; streaming hash |
| Feature-flag kill-switch abuse | Bundled safe defaults; config channel optional; flags cannot exfiltrate user data |

## Security headers (CDN / optional Caddy)

- TLS 1.2+ (`ModernOnly` on Bunny)  
- HSTS on custom origin (Caddy)  
- CSP `default-src 'none'` for origin file server  
- CORS: GET/HEAD only for pack extensions  
- Strip cookies; block POST on pull zone  

## Privacy

- No cookies, no user IDs, no authenticated pack APIs.  
- Logs: truncated client IP (`OneDigit` / `/16` IPv4 & `/32` IPv6 mask on Caddy).  
- EU hosting: Bunny storage `DE`; optional Hetzner EU locations.

## Non-goals

- DRM or encrypted media (packs are public).  
- Per-user personalization on the content plane.  
- Replacing app-store code signing.
