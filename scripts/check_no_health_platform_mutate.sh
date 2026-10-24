#!/usr/bin/env bash
# CI grep gate: OmniGart must never delete or rewrite Apple Health / Health Connect data.
# Platform writes are append-only (exists-check + skip). See docs/HEALTH_DATA_MAPPING.md.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail=0

echo "Checking for forbidden HealthKit delete APIs..."
# Production Swift only (exclude *Tests*).
if rg -n --glob '*.swift' --glob '!*Tests*' --glob '!*Test*' \
  -e 'deleteObjects\s*\(' \
  -e '\.deleteObjects\s*\(' \
  -e 'healthStore\.delete\s*\(' \
  -e 'HKHealthStore\s*\(\s*\)\.delete\s*\(' \
  -e '\.delete\s*\(\s*objects?\b' \
  iosApp/OmniGart watchApp/OmniGartWatch 2>/dev/null; then
  echo "ERROR: HealthKit delete / deleteObjects is forbidden (append-only platform writes)." >&2
  fail=1
fi

echo "Checking for forbidden Health Connect delete/update APIs..."
# Production Kotlin main sources only.
if rg -n \
  -e '\bdeleteRecords\s*\(' \
  -e '\bupdateRecords\s*\(' \
  shared/src/commonMain shared/src/androidMain shared/src/appleMain \
  androidApp/src/main wearApp/src/main 2>/dev/null; then
  echo "ERROR: Health Connect deleteRecords/updateRecords are forbidden (append-only)." >&2
  fail=1
fi

if [[ "$fail" -ne 0 ]]; then
  echo "Platform health mutation gate failed." >&2
  exit 1
fi

echo "OK: no HealthKit delete / Health Connect deleteRecords|updateRecords in production sources."
