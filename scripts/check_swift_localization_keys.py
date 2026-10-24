#!/usr/bin/env python3
"""Fail if SwiftUI localization keys used in sources are missing from the String Catalog.

Checks iOS and watchOS targets independently against their Localizable.xcstrings.

Also verifies iosApp/project.yml wires the watch String Catalog under OmniGartWatch
sources (not resources-only / not excluded). A resources-only entry does not run
CompileXCStrings for the watch target, so Text keys render raw at runtime.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROJECT_YML = ROOT / "iosApp" / "project.yml"

TARGETS = [
    {
        "name": "OmniGart (iOS)",
        "sources": [ROOT / "iosApp" / "OmniGart"],
        "catalog": ROOT / "iosApp" / "OmniGart" / "Localizable.xcstrings",
    },
    {
        "name": "OmniGartWatch",
        "sources": [ROOT / "watchApp" / "OmniGartWatch"],
        "catalog": ROOT / "watchApp" / "OmniGartWatch" / "Localizable.xcstrings",
    },
]

REQUIRED_LOCALES = [
    "en-GB",
    "uk",
    "pl",
    "be",
    "kk",
    "ka",
    "es",
    "fr",
    "de",
    "cs",
    "zh-Hans",
]

# Call sites whose first string argument is a localization key.
CALL_FIRST_ARG = re.compile(
    r"""(?:Text|Button|Label|Section|Toggle|navigationTitle|NavigationLink)\(\s*"([^"\\]+)"\s*[,)]""",
    re.MULTILINE,
)
LOCALIZED_STRING_KEY = re.compile(r"""LocalizedStringKey\(\s*"([^"\\]+)"\s*\)""")
STRING_LOCALIZED = re.compile(
    r"""String\(\s*localized:\s*(?:String\.LocalizationValue\(\s*)?"([^"\\]+)"\s*\)"""
)
# LocalizedStringKey parameters / returns (not StringCatalog.t / catalog("…")).
# Watch/iOS helpers that take LocalizedStringKey (not StringCatalog String keys).
LABEL_KEY_PARAM = re.compile(r"""labelKey\s*:\s*"([^"\\]+)"\s*[,)]""")
RETURN_KEY = re.compile(
    r'return\s+"([a-z][a-z0-9]*(?:[._][a-z0-9]+)+)"',
    re.MULTILINE,
)
# Swift `case .food: "tab_food"` (LocalizedStringKey sugar).
CASE_KEY = re.compile(
    r'case\s+\.[A-Za-z0-9_]+\s*:\s*"([a-z][a-z0-9]*(?:[._][a-z0-9]+)+)"'
)
# Localization keys are identifiers (no spaces / punctuation beyond . _).
KEY_SHAPE = re.compile(r"^[a-z][a-zA-Z0-9_.]*$")

# Start-screen keys that previously rendered raw when the catalog was not compiled.
WATCH_SMOKE_KEYS = (
    "app_name",
    "watch_subtitle",
    "watch.zone2.start",
    "treadmill_run_title",
)


def catalog_keys(path: Path) -> set[str]:
    data = json.loads(path.read_text(encoding="utf-8"))
    strings = data.get("strings")
    if not isinstance(strings, dict):
        raise SystemExit(f"{path}: missing top-level 'strings' object")
    return set(strings.keys())


def catalog_incomplete_locales(path: Path, keys: set[str]) -> list[str]:
    data = json.loads(path.read_text(encoding="utf-8"))
    strings = data["strings"]
    problems: list[str] = []
    for key in sorted(keys):
        locs = strings.get(key, {}).get("localizations") or {}
        missing = [loc for loc in REQUIRED_LOCALES if loc not in locs]
        if missing:
            problems.append(f"{key}: missing locales {', '.join(missing)}")
            continue
        for loc in REQUIRED_LOCALES:
            value = (
                locs.get(loc, {})
                .get("stringUnit", {})
                .get("value")
            )
            if not isinstance(value, str) or not value.strip():
                problems.append(f"{key}: empty value for {loc}")
    return problems


def extract_keys_from_swift(text: str) -> set[str]:
    keys: set[str] = set()
    for pattern in (
        CALL_FIRST_ARG,
        LOCALIZED_STRING_KEY,
        STRING_LOCALIZED,
        LABEL_KEY_PARAM,
        RETURN_KEY,
        CASE_KEY,
    ):
        keys.update(pattern.findall(text))
    # Drop prose labels, interpolations, and format strings.
    return {
        k
        for k in keys
        if k and "\\(" not in k and "%" not in k and KEY_SHAPE.match(k)
    }


def iter_swift_files(roots: list[Path]) -> list[Path]:
    files: list[Path] = []
    for root in roots:
        if not root.is_dir():
            continue
        files.extend(sorted(root.rglob("*.swift")))
    return files


def check_target(target: dict) -> list[str]:
    errors: list[str] = []
    catalog: Path = target["catalog"]
    name: str = target["name"]
    if not catalog.is_file():
        return [f"{name}: missing String Catalog at {catalog.relative_to(ROOT)}"]

    try:
        present = catalog_keys(catalog)
    except json.JSONDecodeError as exc:
        return [f"{name}: malformed catalog {catalog.relative_to(ROOT)}: {exc}"]

    used: set[str] = set()
    for swift in iter_swift_files(target["sources"]):
        used |= extract_keys_from_swift(swift.read_text(encoding="utf-8"))

    missing = sorted(used - present)
    for key in missing:
        errors.append(
            f"{name}: key {key!r} used in Swift sources but missing from "
            f"{catalog.relative_to(ROOT)}"
        )

    # Require full locale coverage for every key that Swift references.
    referenced = used & present
    for problem in catalog_incomplete_locales(catalog, referenced):
        errors.append(f"{name}: {problem}")

    return errors


def _omni_gart_watch_block(yml: str) -> str | None:
    """Return the OmniGartWatch target block from project.yml (best-effort)."""
    marker = "  OmniGartWatch:"
    start = yml.find(marker)
    if start < 0:
        return None
    # Next top-level target at same indent (two spaces + name + colon).
    rest = yml[start + len(marker) :]
    next_target = re.search(r"\n  [A-Za-z][A-Za-z0-9_]*:", rest)
    end = start + len(marker) + (next_target.start() if next_target else len(rest))
    return yml[start:end]


def check_watch_project_yml_wiring() -> list[str]:
    """Ensure the watch String Catalog is compiled via sources, not resources-only."""
    errors: list[str] = []
    if not PROJECT_YML.is_file():
        return [f"missing {PROJECT_YML.relative_to(ROOT)}"]

    text = PROJECT_YML.read_text(encoding="utf-8")
    block = _omni_gart_watch_block(text)
    if not block:
        return ["iosApp/project.yml: OmniGartWatch target not found"]

    # resources-only listing (the failure mode that skipped CompileXCStrings).
    if re.search(
        r"resources:\s*(?:.*\n)*?\s*-\s*path:\s*\.\./watchApp/OmniGartWatch/Localizable\.xcstrings",
        block,
    ):
        errors.append(
            "iosApp/project.yml: OmniGartWatch lists Localizable.xcstrings under "
            "resources — Xcode will not CompileXCStrings it. Include it via the "
            "watchApp/OmniGartWatch sources path instead (same as iOS OmniGart)."
        )

    if re.search(
        r"excludes:\s*(?:.*\n)*?\s*-\s*Localizable\.xcstrings\b",
        block,
    ):
        errors.append(
            "iosApp/project.yml: OmniGartWatch excludes Localizable.xcstrings from "
            "sources — remove that exclude so XcodeGen emits text.json.xcstrings."
        )

    # sources path must cover the watch app tree that contains the catalog.
    if not re.search(
        r"sources:\s*(?:.*\n)*?\s*-\s*path:\s*\.\./watchApp/OmniGartWatch\b",
        block,
    ):
        errors.append(
            "iosApp/project.yml: OmniGartWatch sources must include "
            "path: ../watchApp/OmniGartWatch (so Localizable.xcstrings is compiled)."
        )

    catalog = ROOT / "watchApp" / "OmniGartWatch" / "Localizable.xcstrings"
    if not catalog.is_file():
        errors.append("watchApp/OmniGartWatch/Localizable.xcstrings is missing")
    else:
        present = catalog_keys(catalog)
        for key in WATCH_SMOKE_KEYS:
            if key not in present:
                errors.append(
                    f"OmniGartWatch: smoke key {key!r} missing from "
                    "watchApp/OmniGartWatch/Localizable.xcstrings"
                )
            else:
                errors.extend(
                    f"OmniGartWatch: {problem}"
                    for problem in catalog_incomplete_locales(catalog, {key})
                )

    return errors


def main() -> int:
    errors: list[str] = []
    errors.extend(check_watch_project_yml_wiring())
    for target in TARGETS:
        errors.extend(check_target(target))

    if errors:
        print("Swift localization key check FAILED:", file=sys.stderr)
        for err in errors:
            print(f"  - {err}", file=sys.stderr)
        return 1

    print("Swift localization key check OK")
    for target in TARGETS:
        catalog = target["catalog"]
        used: set[str] = set()
        for swift in iter_swift_files(target["sources"]):
            used |= extract_keys_from_swift(swift.read_text(encoding="utf-8"))
        present = catalog_keys(catalog)
        print(
            f"  {target['name']}: {len(used)} keys referenced, "
            f"{len(present)} in catalog, all present"
        )
    print("  OmniGartWatch: project.yml sources wiring OK (catalog not resources-only)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
