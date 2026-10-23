#!/usr/bin/env bash
# Xcode pre-build: link Shared for the active platform and sync into a
# version-agnostic path used by FRAMEWORK_SEARCH_PATHS:
#   shared/build/xcode-frameworks/$(CONFIGURATION)/$(PLATFORM_NAME)/
#
# Set OMNIGART_SKIP_XCODE_GRADLE=1 (CI) to require a prior Gradle sync and skip
# re-linking inside xcodebuild.
set -euo pipefail

ROOT="${SRCROOT:?SRCROOT not set}/.."
cd "$ROOT"

CONFIGURATION="${CONFIGURATION:?CONFIGURATION not set}"
PLATFORM_NAME="${PLATFORM_NAME:?PLATFORM_NAME not set}"

OUT="$ROOT/shared/build/xcode-frameworks/${CONFIGURATION}/${PLATFORM_NAME}/Shared.framework"

if [[ "${OMNIGART_SKIP_XCODE_GRADLE:-}" == "1" ]]; then
  if [[ ! -d "$OUT" ]]; then
    echo "error: Shared.framework missing at $OUT (run :shared:syncXcodeFrameworks first)" >&2
    exit 1
  fi
  exit 0
fi

case "${CONFIGURATION}" in
  Debug) link_kind="Debug" ;;
  Release) link_kind="Release" ;;
  *)
    echo "warning: unknown CONFIGURATION=${CONFIGURATION}; using Debug link tasks" >&2
    link_kind="Debug"
    ;;
esac

case "${PLATFORM_NAME}" in
  iphonesimulator) target="IosSimulatorArm64" ;;
  iphoneos) target="IosArm64" ;;
  watchsimulator) target="WatchosSimulatorArm64" ;;
  watchos) target="WatchosArm64" ;;
  *)
    echo "error: unsupported PLATFORM_NAME=${PLATFORM_NAME}" >&2
    exit 1
    ;;
esac

link_task="link${link_kind}Framework${target}"
echo "Linking Shared via :shared:${link_task} → ${CONFIGURATION}/${PLATFORM_NAME}"
./gradlew ":shared:${link_task}" :shared:syncXcodeFrameworks --stacktrace

if [[ ! -d "$OUT" ]]; then
  echo "error: expected Shared.framework at $OUT after sync" >&2
  exit 1
fi
