terraform {
  required_version = ">= 1.13.2"

  required_providers {
    bunnynet = {
      source  = "BunnyWay/bunnynet"
      version = "0.19.2"
    }
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "1.70.0"
    }
  }

  # Remote state is owner-configured (e.g. Hetzner Object Storage / S3 backend).
  # CI uses: tofu init -backend=false
}
