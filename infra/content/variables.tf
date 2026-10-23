variable "project_name" {
  type        = string
  description = "Short name prefix for Bunny / Hetzner resources."
  default     = "omnigart-content"
}

variable "bunnynet_api_key" {
  type        = string
  description = "Bunny.net account API key. Prefer BUNNYNET_API_KEY env; leave empty in CI validate."
  default     = ""
  sensitive   = true
}

variable "hcloud_token" {
  type        = string
  description = "Hetzner Cloud API token. Required only if enable_origin_vm = true."
  default     = ""
  sensitive   = true
}

variable "enable_origin_vm" {
  type        = bool
  description = "When true, create optional CAX11 origin VM with Caddy (firewall 80/443 + restricted SSH)."
  default     = false
}

variable "storage_region" {
  type        = string
  description = "Bunny storage region (EU). DE = Frankfurt."
  default     = "DE"
}

variable "cdn_name" {
  type        = string
  description = "Bunny pull zone name (globally unique on bunny.net)."
  default     = "omnigart-content"
}

variable "cors_extensions" {
  type        = set(string)
  description = "File extensions allowed for CORS responses at the CDN."
  default     = ["json", "minisig", "bin", "webp", "png", "jpg", "jpeg", "mp4", "webm"]
}

variable "ssh_public_keys" {
  type        = list(string)
  description = "SSH public keys for the optional origin VM."
  default     = []
}

variable "ssh_allowed_cidrs" {
  type        = list(string)
  description = "CIDRs allowed to SSH to the optional VM. Empty = no SSH ingress (console/serial only)."
  default     = []
}

variable "vm_location" {
  type        = string
  description = "Hetzner location for optional VM (EU)."
  default     = "nbg1"
}

variable "vm_server_type" {
  type        = string
  description = "Hetzner server type for optional origin."
  default     = "cax11"
}

variable "admin_email" {
  type        = string
  description = "Email for Caddy / Let's Encrypt on the optional VM."
  default     = "ops@example.com"
}

variable "origin_hostname" {
  type        = string
  description = "Public hostname for optional Caddy origin (DNS must point at the VM)."
  default     = "origin.content.example.com"
}
