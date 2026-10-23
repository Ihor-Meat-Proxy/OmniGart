# Primary architecture: Bunny Storage (EU) + Pull Zone CDN.
# Apps download immutable packs from the CDN hostname and verify minisign signatures.

resource "bunnynet_storage_zone" "content" {
  name      = var.project_name
  region    = var.storage_region
  zone_tier = "Standard"
  type      = "Standard"
}

resource "bunnynet_pullzone" "content" {
  name = var.cdn_name

  origin {
    type        = "StorageZone"
    storagezone = bunnynet_storage_zone.content.id
  }

  routing {
    tier    = "Standard"
    filters = ["all"]
    # Prefer EU/EEA edge affinity when available; still global Standard network.
    zones = ["EU", "US", "ASIA", "SA", "AF"]
  }

  # TLS
  tls_level            = "ModernOnly"
  disable_letsencrypt  = false
  block_post_requests  = true
  strip_cookies        = true
  add_canonical_header = true

  # Caching — packs are immutable under /v/{version}/; pointer can be short-TTL via Bunny UI rules if needed
  cache_enabled                 = true
  cache_expiration_time         = 31536000
  cache_expiration_time_browser = 86400
  use_background_update         = true
  sort_querystring              = true

  # CORS — mobile / web clients fetching packs
  cors_enabled    = true
  cors_extensions = var.cors_extensions

  # Privacy-preserving logs (truncate / drop identifying octets)
  log_enabled          = true
  log_anonymized       = true
  log_anonymized_style = "OneDigit"
  log_storage_enabled  = false
  log_forward_enabled  = false

  # Hardening
  block_root_path    = false
  optimizer_enabled  = false
  websockets_enabled = false
}

# Bundle-first object layout (enforced by release tooling, not Bunny):
#   packs/{id}/v/{semver}/manifest.json(+.minisig)   # update | media | off | config
#   packs/{id}/v/{semver}/pack.tar(+.minisig)
#   packs/{id}/current.json(+.minisig)               # pointer to newest verified pack
#   media/videos/{exerciseId}/{asset}                # optional large video objects
# Baseline packs are NOT served as the source of truth — they ship in app binaries.
# CDN may optionally mirror baselines for transparency; apps must not require them.
