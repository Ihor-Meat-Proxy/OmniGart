#!/usr/bin/env python3
"""App Review / health-app compliance gates for OmniGart.

Scans string resources + store metadata for banned medical-claim terms,
competitor names, and placeholder/lorem/TODO copy; verifies required usage
description keys exist and are non-empty for all required locales; checks that
debug menus are not wired into release source paths; and confirms feature flags
default unfinished work off for release builds.

Intended to run inside existing CI jobs (not as a separate required job).
"""

from __future__ import annotations

import argparse
import glob
import os
import plistlib
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CONFIG = ROOT / ".github" / "ci" / "banned-terms.yml"


def _parse_scalar(raw: str) -> Any:
    text = raw.strip()
    if not text or text == "~" or text == "null":
        return None
    if text in ("true", "True"):
        return True
    if text in ("false", "False"):
        return False
    if re.fullmatch(r"-?\d+", text):
        return int(text)
    if (text.startswith('"') and text.endswith('"')) or (
        text.startswith("'") and text.endswith("'")
    ):
        return text[1:-1]
    return text


def load_simple_yaml(text: str) -> dict[str, Any]:
    """Minimal YAML subset loader (maps/lists/scalars) — no PyYAML needed in CI."""
    result: dict[str, Any] = {}
    # stack entries: (indent, container, parent_key_or_None)
    stack: list[tuple[int, Any, str | None]] = [(-1, result, None)]
    lines = text.splitlines()

    def peek_container(idx: int, current_indent: int) -> Any:
        for nxt in lines[idx + 1 :]:
            if not nxt.strip() or nxt.lstrip().startswith("#"):
                continue
            nxt_indent = len(nxt) - len(nxt.lstrip(" "))
            if nxt_indent <= current_indent:
                break
            if nxt.strip().startswith("- "):
                return []
            return {}
        return {}

    for idx, raw_line in enumerate(lines):
        if not raw_line.strip() or raw_line.lstrip().startswith("#"):
            continue
        indent = len(raw_line) - len(raw_line.lstrip(" "))
        line = raw_line.strip()
        while len(stack) > 1 and indent <= stack[-1][0]:
            stack.pop()
        parent = stack[-1][1]

        if line.startswith("- "):
            if not isinstance(parent, list):
                raise SystemExit(f"YAML list item without list parent: {line!r}")
            parent.append(_parse_scalar(line[2:].strip()))
            continue

        if ":" not in line:
            raise SystemExit(f"Unsupported YAML: {line!r}")
        key, _, rest = line.partition(":")
        key = key.strip()
        rest = rest.strip()
        if not isinstance(parent, dict):
            raise SystemExit(f"YAML key under non-map: {key!r}")
        if rest == "":
            container = peek_container(idx, indent)
            parent[key] = container
            stack.append((indent, container, key))
        else:
            parent[key] = _parse_scalar(rest)
    return result

USAGE_KEY_RE = re.compile(
    r"<(key)>(NS[A-Za-z]+UsageDescription)</\1>\s*<string>(.*?)</string>",
    re.DOTALL,
)
INFOPLIST_ASSIGN_RE = re.compile(
    r'^\s*"(NS[A-Za-z]+UsageDescription)"\s*=\s*"(.*)";\s*$',
    re.MULTILINE,
)
FEATURE_FLAG_DEFAULT_RE = re.compile(
    r"val\s+(?P<name>[A-Za-z0-9_]+)\s*:\s*Boolean\s*=\s*(?P<value>true|false)"
)


def load_config(path: Path) -> dict[str, Any]:
    text = path.read_text(encoding="utf-8")
    data = load_simple_yaml(text)
    if not isinstance(data, dict):
        raise SystemExit(f"Invalid config: {path}")
    return data


def expand_globs(patterns: list[str]) -> list[Path]:
    files: list[Path] = []
    for pattern in patterns:
        matched = glob.glob(str(ROOT / pattern), recursive=True)
        for item in matched:
            p = Path(item)
            if p.is_file():
                files.append(p)
    # Stable unique order
    return sorted(set(files), key=lambda p: str(p))


def extract_user_facing_strings(path: Path) -> list[tuple[str, str]]:
    """Return (locator, value) pairs from a string resource / store file."""
    text = path.read_text(encoding="utf-8")
    rel = str(path.relative_to(ROOT))
    values: list[tuple[str, str]] = []

    if path.suffix == ".xml":
        for m in re.finditer(
            r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>',
            text,
            re.DOTALL,
        ):
            name, raw = m.group(1), m.group(2)
            values.append((f"{rel}:{name}", unescape_xml(raw.strip())))
    elif path.suffix == ".json":
        # shared i18n flat JSON or xcstrings-like — keep it simple for flat maps
        try:
            import json

            data = json.loads(text)
        except Exception:
            return values
        if rel.endswith(".xcstrings") or '"sourceLanguage"' in text[:200]:
            strings = data.get("strings", {})
            for key, entry in strings.items():
                locs = entry.get("localizations", {}) if isinstance(entry, dict) else {}
                for locale, unit in locs.items():
                    su = unit.get("stringUnit", {}) if isinstance(unit, dict) else {}
                    val = su.get("value")
                    if isinstance(val, str):
                        values.append((f"{rel}:{key}:{locale}", val))
        elif isinstance(data, dict):
            for key, val in data.items():
                if isinstance(val, str):
                    values.append((f"{rel}:{key}", val))
    elif path.suffix == ".strings":
        for m in INFOPLIST_ASSIGN_RE.finditer(text):
            values.append((f"{rel}:{m.group(1)}", unescape_strings(m.group(2))))
        # Also generic Localizable-style if present
        for m in re.finditer(
            r'^\s*"([^"]+)"\s*=\s*"(.*)";\s*$', text, re.MULTILINE
        ):
            key, val = m.group(1), unescape_strings(m.group(2))
            if not key.startswith("NS"):
                values.append((f"{rel}:{key}", val))
    elif path.suffix in {".txt", ".md"}:
        # Store metadata: scan non-empty lines (skip markdown headings markers lightly)
        for i, line in enumerate(text.splitlines(), start=1):
            stripped = line.strip()
            if not stripped or stripped.startswith("#"):
                continue
            values.append((f"{rel}:{i}", stripped))
    return values


def unescape_xml(value: str) -> str:
    return (
        value.replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", '"')
        .replace("&apos;", "'")
    )


def unescape_strings(value: str) -> str:
    return value.replace('\\"', '"').replace("\\n", "\n")


def whole_word_pattern(term: str, ignore_case: bool) -> re.Pattern[str]:
    # Allow spaces inside multi-word phrases
    parts = [re.escape(p) for p in term.split()]
    body = r"\s+".join(parts)
    flags = re.IGNORECASE if ignore_case else 0
    return re.compile(rf"(?<![A-Za-z0-9_]){body}(?![A-Za-z0-9_])", flags)


def scan_copy(config: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    files = expand_globs(config.get("scan_globs", []))
    medical = [
        whole_word_pattern(t, ignore_case=True)
        for t in config.get("medical_claim_terms", [])
    ]
    competitors = [
        whole_word_pattern(t, ignore_case=False)
        for t in config.get("competitor_names", [])
    ]
    placeholders = [
        whole_word_pattern(t, ignore_case=True)
        for t in config.get("placeholder_patterns", [])
    ]

    if not files:
        errors.append("No string/store files matched scan_globs")
        return errors

    for path in files:
        for locator, value in extract_user_facing_strings(path):
            for pat, label in (
                *((p, "medical_claim") for p in medical),
                *((p, "competitor_name") for p in competitors),
                *((p, "placeholder") for p in placeholders),
            ):
                if pat.search(value):
                    errors.append(
                        f"[{label}] {locator}: matched /{pat.pattern}/ in {value!r}"
                    )
    return errors


def collect_usage_descriptions() -> dict[str, dict[str, str]]:
    """locale -> key -> value from Info.plist defaults + InfoPlist.strings."""
    found: dict[str, dict[str, str]] = {}

    def ensure(locale: str) -> dict[str, str]:
        return found.setdefault(locale, {})

    plist_paths = [
        ROOT / "iosApp" / "OmniGart" / "Info.plist",
        ROOT / "watchApp" / "OmniGartWatch" / "Info.plist",
    ]
    for plist_path in plist_paths:
        if not plist_path.is_file():
            continue
        with plist_path.open("rb") as fh:
            data = plistlib.load(fh)
        # Base plist values count as en-GB (CFBundleDevelopmentRegion)
        locale = str(data.get("CFBundleDevelopmentRegion", "en-GB"))
        bucket = ensure(locale)
        for key, val in data.items():
            if key.endswith("UsageDescription") and isinstance(val, str):
                bucket[key] = val

    for strings_path in expand_globs(
        [
            "iosApp/**/InfoPlist.strings",
            "watchApp/**/InfoPlist.strings",
        ]
    ):
        # Infer locale from parent folder name: en-GB.lproj / uk.lproj
        parent = strings_path.parent.name
        locale = parent.replace(".lproj", "") if parent.endswith(".lproj") else "en-GB"
        bucket = ensure(locale)
        text = strings_path.read_text(encoding="utf-8")
        for m in INFOPLIST_ASSIGN_RE.finditer(text):
            bucket[m.group(1)] = unescape_strings(m.group(2))

    return found


def check_usage_descriptions(config: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    required_keys = config.get("required_usage_description_keys", [])
    required_locales = config.get("required_locales", [])
    by_locale = collect_usage_descriptions()

    for locale in required_locales:
        bucket = by_locale.get(locale, {})
        for key in required_keys:
            value = bucket.get(key, "").strip()
            if not value:
                # Fall back: en-GB plist defaults may cover when locale strings omit a key
                if locale != "en-GB":
                    fallback = by_locale.get("en-GB", {}).get(key, "").strip()
                    if fallback:
                        errors.append(
                            f"[usage_description] {locale} missing non-empty {key} "
                            f"(en-GB fallback exists but locale-specific string is required)"
                        )
                        continue
                errors.append(
                    f"[usage_description] {locale} missing non-empty {key}"
                )
    return errors


def check_privacy_manifest() -> list[str]:
    errors: list[str] = []
    path = ROOT / "iosApp" / "OmniGart" / "PrivacyInfo.xcprivacy"
    if not path.is_file():
        errors.append("[privacy] missing iosApp/OmniGart/PrivacyInfo.xcprivacy")
        return errors
    with path.open("rb") as fh:
        data = plistlib.load(fh)
    tracking = data.get("NSPrivacyTracking")
    if tracking is not True and tracking is not False:
        errors.append("[privacy] NSPrivacyTracking must be set (expected false)")
    elif tracking is True:
        errors.append("[privacy] NSPrivacyTracking must be false (no tracking)")
    domains = data.get("NSPrivacyTrackingDomains") or []
    if domains:
        errors.append("[privacy] NSPrivacyTrackingDomains must be empty")
    # Health data must not be marked for tracking / advertising
    for entry in data.get("NSPrivacyCollectedDataTypes") or []:
        dtype = entry.get("NSPrivacyCollectedDataType")
        if dtype == "NSPrivacyCollectedDataTypeHealth":
            if entry.get("NSPrivacyCollectedDataTypeTracking") is True:
                errors.append(
                    "[privacy] Health data must not set NSPrivacyCollectedDataTypeTracking=true"
                )
            purposes = entry.get("NSPrivacyCollectedDataTypePurposes") or []
            banned_purposes = {
                "NSPrivacyCollectedDataTypePurposeThirdPartyAdvertising",
                "NSPrivacyCollectedDataTypePurposeDeveloperAdvertising",
            }
            bad = banned_purposes.intersection(set(purposes))
            if bad:
                errors.append(
                    f"[privacy] Health data must not list advertising purposes: {sorted(bad)}"
                )
    return errors


def path_is_debug_allowlisted(path: Path, allow_substrings: list[str]) -> bool:
    text = str(path)
    return any(s in text for s in allow_substrings)


_SWIFT_DEBUG_BLOCK_RE = re.compile(
    r"#if\s+DEBUG\b.*?#endif",
    re.DOTALL | re.IGNORECASE,
)


def strip_swift_debug_blocks(text: str) -> str:
    """Remove #if DEBUG ... #endif so gated call sites do not fail the release scan."""
    return _SWIFT_DEBUG_BLOCK_RE.sub("", text)


def check_debug_menus(config: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    markers = config.get("debug_menu_markers", [])
    allow = config.get("debug_allowlist_path_substrings", [])
    # Scan app sources (not docs / CI config). Exclude src/debug (debug APK only).
    patterns = [
        "androidApp/src/main/**/*.{kt,kts,xml}",
        "androidApp/src/release/**/*.{kt,kts,xml}",
        "wearApp/src/main/**/*.{kt,kts,xml}",
        "wearApp/src/release/**/*.{kt,kts,xml}",
        "iosApp/OmniGart/**/*.{swift,plist}",
        "watchApp/OmniGartWatch/**/*.{swift,plist}",
        "shared/src/commonMain/**/*.kt",
    ]
    # glob does not expand {a,b}; expand manually
    expanded: list[str] = []
    for pat in patterns:
        if "{kt,kts,xml}" in pat:
            for ext in ("kt", "kts", "xml"):
                expanded.append(pat.replace("{kt,kts,xml}", ext))
        elif "{swift,plist}" in pat:
            for ext in ("swift", "plist"):
                expanded.append(pat.replace("{swift,plist}", ext))
        else:
            expanded.append(pat)

    for path in expand_globs(expanded):
        text = path.read_text(encoding="utf-8", errors="ignore")
        if path_is_debug_allowlisted(path, allow):
            # Still require an explicit DEBUG gate inside debug folders for iOS
            if path.suffix == ".swift" and any(m in text for m in markers):
                if "#if DEBUG" not in text and "#ifDEBUG" not in text.replace(" ", ""):
                    errors.append(
                        f"[debug_menu] {path.relative_to(ROOT)} mentions debug UI "
                        f"but lacks #if DEBUG gate"
                    )
            continue
        scan_text = strip_swift_debug_blocks(text) if path.suffix == ".swift" else text
        for marker in markers:
            if whole_word_pattern(marker, ignore_case=False).search(scan_text):
                errors.append(
                    f"[debug_menu] release path {path.relative_to(ROOT)} contains {marker!r}; "
                    f"move under debug/ + BuildConfig.DEBUG / #if DEBUG"
                )
    return errors


def check_feature_flags(config: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    rel = config.get("feature_flags_file")
    if not rel:
        return errors
    path = ROOT / rel
    if not path.is_file():
        errors.append(f"[feature_flags] missing {rel}")
        return errors
    text = path.read_text(encoding="utf-8")
    # Unfinished / gated features must default to false for release.
    # releaseDefaults (or object FeatureFlags) vals ending with Enabled must be false.
    for m in FEATURE_FLAG_DEFAULT_RE.finditer(text):
        name, value = m.group("name"), m.group("value")
        if name.endswith("Enabled") and value != "false":
            errors.append(
                f"[feature_flags] {rel}: {name} must default to false for release (found {value})"
            )
    if "debugMenuEnabled" in text:
        # Prefer explicit false default
        if re.search(r"debugMenuEnabled\s*[:=]\s*true", text):
            errors.append(
                f"[feature_flags] {rel}: debugMenuEnabled must not default true in release"
            )
    return errors


def check_android_build_config_gates() -> list[str]:
    """Release main sources must not call BuildConfig.DEBUG without a guard pattern we accept.

    We only ensure a DebugMenu.kt (if any) lives under src/debug or src/main/.../debug/.
    """
    errors: list[str] = []
    main_debug_files = list((ROOT / "androidApp" / "src" / "main").rglob("*DebugMenu*"))
    main_debug_files += list((ROOT / "wearApp" / "src" / "main").rglob("*DebugMenu*"))
    for path in main_debug_files:
        rel = str(path.relative_to(ROOT))
        if "/debug/" not in rel.replace("\\", "/"):
            errors.append(
                f"[debug_menu] {rel} must live under a debug/ package or src/debug source set"
            )
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--config",
        type=Path,
        default=DEFAULT_CONFIG,
        help="Path to banned-terms.yml",
    )
    args = parser.parse_args()
    config = load_config(args.config)

    errors: list[str] = []
    errors.extend(scan_copy(config))
    errors.extend(check_usage_descriptions(config))
    errors.extend(check_privacy_manifest())
    errors.extend(check_debug_menus(config))
    errors.extend(check_feature_flags(config))
    errors.extend(check_android_build_config_gates())

    if errors:
        print("App Review compliance check FAILED:")
        for err in errors:
            print(f"  - {err}")
        print(f"\n{len(errors)} issue(s). See docs/RELEASE_CHECKLIST.md")
        return 1

    print("OK: App Review compliance checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
