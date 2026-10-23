# Optional origin VM — disabled by default (enable_origin_vm = false).
# Use when you need Caddy in front of Hetzner Object Storage or custom origin logic.
# Firewall: 80/443 world; SSH only from ssh_allowed_cidrs (or none).

locals {
  vm_enabled = var.enable_origin_vm
}

resource "hcloud_ssh_key" "origin" {
  for_each   = local.vm_enabled ? { for i, k in var.ssh_public_keys : tostring(i) => k } : {}
  name       = "${var.project_name}-origin-${each.key}"
  public_key = each.value
}

resource "hcloud_firewall" "origin" {
  count = local.vm_enabled ? 1 : 0
  name  = "${var.project_name}-origin"

  rule {
    direction   = "in"
    protocol    = "tcp"
    port        = "80"
    source_ips  = ["0.0.0.0/0", "::/0"]
    description = "HTTP (ACME + redirect)"
  }

  rule {
    direction   = "in"
    protocol    = "tcp"
    port        = "443"
    source_ips  = ["0.0.0.0/0", "::/0"]
    description = "HTTPS"
  }

  dynamic "rule" {
    for_each = length(var.ssh_allowed_cidrs) > 0 ? [1] : []
    content {
      direction   = "in"
      protocol    = "tcp"
      port        = "22"
      source_ips  = var.ssh_allowed_cidrs
      description = "Restricted SSH"
    }
  }

  rule {
    direction       = "out"
    protocol        = "tcp"
    port            = "1-65535"
    destination_ips = ["0.0.0.0/0", "::/0"]
  }

  rule {
    direction       = "out"
    protocol        = "udp"
    port            = "1-65535"
    destination_ips = ["0.0.0.0/0", "::/0"]
  }

  rule {
    direction       = "out"
    protocol        = "icmp"
    destination_ips = ["0.0.0.0/0", "::/0"]
  }
}

resource "hcloud_server" "origin" {
  count        = local.vm_enabled ? 1 : 0
  name         = "${var.project_name}-origin"
  server_type  = var.vm_server_type
  location     = var.vm_location
  image        = "ubuntu-24.04"
  ssh_keys     = [for k in hcloud_ssh_key.origin : k.id]
  firewall_ids = [hcloud_firewall.origin[0].id]

  user_data = templatefile("${path.module}/cloud-init/user-data.yaml.tftpl", {
    admin_email     = var.admin_email
    origin_hostname = var.origin_hostname
    ssh_port        = 22
  })

  labels = {
    role    = "content-origin"
    project = var.project_name
  }

  public_net {
    ipv4_enabled = true
    ipv6_enabled = true
  }
}
