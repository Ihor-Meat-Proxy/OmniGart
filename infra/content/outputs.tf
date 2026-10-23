output "architecture" {
  value       = "object-storage-cdn-default"
  description = "Default: Bunny Storage + Pull Zone. Optional VM is off unless enable_origin_vm=true."
}

output "storage_zone_id" {
  value       = bunnynet_storage_zone.content.id
  description = "Bunny storage zone ID (origin)."
}

output "storage_hostname" {
  value       = bunnynet_storage_zone.content.hostname
  description = "Bunny storage HTTP API hostname."
}

output "cdn_domain" {
  value       = bunnynet_pullzone.content.cdn_domain
  description = "CDN CNAME target for content.example.com."
}

output "pullzone_id" {
  value       = bunnynet_pullzone.content.id
  description = "Pull zone ID (purge API / UI)."
}

output "origin_vm_ipv4" {
  value       = try(hcloud_server.origin[0].ipv4_address, null)
  description = "Optional CAX11 IPv4 when enable_origin_vm=true."
}

output "object_key_layout" {
  value = {
    pack_versioned = "packs/{packId}/v/{semver}/"
    pack_pointer   = "packs/{packId}/current.json(+.minisig)"
    app_config     = "config/app-config.json(+.minisig)"
    media          = "media/{hash-or-key}"
  }
}
