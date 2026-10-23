#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
for app in androidApp wearApp; do
  mkdir -p "$ROOT/$app/src/main/kotlin/com/omnigart/design/theme"
  mkdir -p "$ROOT/$app/src/main/kotlin/com/omnigart/design/icons"
  cp "$ROOT/design/generated/compose/OmniGartTheme.kt" \
    "$ROOT/$app/src/main/kotlin/com/omnigart/design/theme/OmniGartTheme.kt"
  cp "$ROOT/design/generated/compose/OmniGartIcons.kt" \
    "$ROOT/$app/src/main/kotlin/com/omnigart/design/icons/OmniGartIcons.kt"
done
echo "Synced design Compose outputs into androidApp and wearApp."
