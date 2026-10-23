# Optional exercise video delivery

Videos are **not** bundled. Still images (WebP/HEIC) are. Choose a delivery path before shipping video UX:

| Option | Pros | Cons |
|--------|------|------|
| **Plain CDN** (Bunny) + app disk cache | Simple; matches content CDN; works on sideload/F-Droid-style builds | Manual background download; storage quotas DIY |
| **iOS Background Assets** | System-managed download/scheduling | Apple-only; App Store plumbing |
| **Android Play Asset Delivery** | Install-time or on-demand packs via Play | Play-only; large asset packs complicate sideload |

**Provisional default:** plain CDN downloads with Wi-Fi preference and size caps; evaluate Background Assets / PAD in a later spike (logged in `OPEN_QUESTIONS.md` #19).
