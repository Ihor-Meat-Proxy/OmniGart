#!/usr/bin/env bash
# Xcode pre-build: pack offline content into a temp dir and stage it into
# SCRIPT_OUTPUT_DIR (or iosApp/OmniGart/ContentBundle). Does not rewrite the
# committed generated/content-bundle/ baseline (keeps the working tree clean).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:-$ROOT/iosApp/OmniGart/ContentBundle}"
TMP="$(mktemp -d "${TMPDIR:-/tmp}/omnigart-content.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT

python3 "$ROOT/scripts/pack_bundled_content.py" --out "$TMP"

rm -rf "$OUT"
mkdir -p "$(dirname "$OUT")"
cp -R "$TMP" "$OUT"
echo "Staged content bundle → $OUT"
