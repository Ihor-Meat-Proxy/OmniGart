# Secrets the owner must add (never commit values)

CI **does not** apply infrastructure and **must not** receive production secrets. PR workflows only `fmt` + `init -backend=false` + `validate`.

## Required for hosted Bunny path (default)

| Secret / env | Where | Purpose |
|--------------|-------|---------|
| `BUNNYNET_API_KEY` | Bunny account → API | OpenTofu provider + release upload/purge |
| Bunny Storage Zone password | Output after first apply (`password`) | S3/FTP-style upload of packs (store in release environment) |
| `MINISIGN_SECRET_KEY` | Offline / GitHub Environment `content-release` | Sign manifests and pack archives (prefer offline signing; CI only if Environment approval gates it) |
| `MINISIGN_PUBLIC_KEY` | Public (apps + repo) | Verify signatures |

## Optional (only if `enable_origin_vm = true`)

| Secret / env | Purpose |
|--------------|---------|
| `HCLOUD_TOKEN` | Create CAX11, firewall, SSH keys |
| SSH private key matching `ssh_public_keys` | Operator access (local only; never in GitHub) |

## Optional Hetzner Object Storage origin

Not managed by OpenTofu as a bucket resource. Create in Hetzner Console (EU: FSN1 / NBG1 / HEL1), then either:

1. Point Bunny Pull Zone `origin.type = OriginUrl` + `s3_auth_*` at the bucket, or  
2. Sync objects into Bunny Storage with the release tool.

| Secret | Purpose |
|--------|---------|
| Hetzner S3 access key / secret | Upload + CDN origin auth |

## GitHub Environments

Create Environment **`content-release`** with required reviewers. Store:

- `BUNNYNET_API_KEY`
- `BUNNY_STORAGE_PASSWORD` (or access key)
- `BUNNY_STORAGE_ZONE` / hostname
- `BUNNY_PULLZONE_ID`
- `MINISIGN_SECRET_KEY` (if signing in CI; otherwise sign offline and upload artifacts)

## Local `terraform.tfvars`

Copy `terraform.tfvars.example` → `terraform.tfvars` (gitignored). Never commit real keys.
