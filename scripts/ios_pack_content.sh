#!/usr/bin/env bash
# Xcode pre-build: refresh generated/content-bundle and stage it into SCRIPT_OUTPUT_DIR
# (or iosApp/OmniGart/ContentBundle) for Copy Bundle Resources.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:-$ROOT/iosApp/OmniGart/ContentBundle}"

python3 "$ROOT/scripts/pack_bundled_content.py"

rm -rf "$OUT"
mkdir -p "$(dirname "$OUT")"
cp -R "$ROOT/generated/content-bundle" "$OUT"
echo "Staged content bundle → $OUT"
