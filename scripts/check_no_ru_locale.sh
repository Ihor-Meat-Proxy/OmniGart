#!/usr/bin/env bash
# Fails if a Russian (ru) locale appears in OmniGart string catalogs or resource folders.
# Ignores toolchains (Android SDK, Gradle caches, build outputs).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FOUND=0

echo "Checking for forbidden 'ru' locales under ${ROOT} (project sources only)..."

FIND_PRUNE=(
  \( -path "${ROOT}/.android-sdk" -o
     -path "${ROOT}/.gradle" -o
     -path "${ROOT}/.git" -o
     -path "${ROOT}/**/build" -o
     -path "${ROOT}/build" -o
     -path "${ROOT}/.idea" -o
     -path "${ROOT}/node_modules" \) -prune -o
)

# Android values-ru* resource directories in project sources
while IFS= read -r -d '' dir; do
  echo "FORBIDDEN Android locale directory: ${dir}"
  FOUND=1
done < <(find "${ROOT}" "${FIND_PRUNE[@]}" -type d \( -name 'values-ru' -o -name 'values-ru-*' -o -name 'values-b+ru*' \) -print0 2>/dev/null)

# iOS/String Catalog / lproj
while IFS= read -r -d '' dir; do
  echo "FORBIDDEN lproj: ${dir}"
  FOUND=1
done < <(find "${ROOT}" "${FIND_PRUNE[@]}" -type d \( -name 'ru.lproj' -o -name 'ru-*.lproj' \) -print0 2>/dev/null)

# xcstrings / JSON locale keys "ru"
while IFS= read -r -d '' file; do
  if grep -E '"ru"[[:space:]]*:|"ru-[A-Za-z]+"[[:space:]]*:' "${file}" >/dev/null 2>&1; then
    echo "FORBIDDEN ru locale key in: ${file}"
    FOUND=1
  fi
done < <(find "${ROOT}" "${FIND_PRUNE[@]}" \( -name '*.xcstrings' -o -name 'strings.json' -o -path '*/resources/i18n/*' \) -type f -print0 2>/dev/null)

if [ -d "${ROOT}/shared/src/commonMain/resources/i18n/ru" ]; then
  echo "FORBIDDEN shared i18n/ru directory"
  FOUND=1
fi

if [ "${FOUND}" -ne 0 ]; then
  echo "ERROR: Russian locale (ru) must never be added. Source locale is en-GB; fallback is en-GB."
  exit 1
fi

echo "OK: no ru locale found."
