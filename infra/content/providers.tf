provider "bunnynet" {
  # Prefer env BUNNYNET_API_KEY. Placeholder lets `tofu validate` run without secrets.
  api_key = var.bunnynet_api_key != "" ? var.bunnynet_api_key : "ci-validate-placeholder"
}

provider "hcloud" {
  # Required by provider config even when enable_origin_vm=false (no resources created).
  # Prefer env HCLOUD_TOKEN for real plans/applies.
  token = var.hcloud_token != "" ? var.hcloud_token : "ci-validate-placeholder"
}
