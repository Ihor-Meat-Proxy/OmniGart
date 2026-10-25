#!/usr/bin/env python3
"""Sync equipment_item_* labels (11 locales) into shared JSON, xcstrings, Android, Wear.

Source of truth: scripts/data/equipment_item_i18n.json (ids from EquipmentDefinitions.kt).

Updates are surgical: existing key order / surrounding formatting is preserved;
new keys are appended. This avoids full-file JSON re-sort churn.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "scripts" / "data" / "equipment_item_i18n.json"
DEFS = (
    ROOT
    / "shared"
    / "src"
    / "commonMain"
    / "kotlin"
    / "com"
    / "omnigart"
    / "shared"
    / "strength"
    / "EquipmentDefinitions.kt"
)
LOCALES = [
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
ANDROID_FOLDERS = {
    "en-GB": "values-en-rGB",
    "uk": "values-uk",
    "pl": "values-pl",
    "be": "values-be",
    "kk": "values-kk",
    "ka": "values-ka",
    "es": "values-es",
    "fr": "values-fr",
    "de": "values-de",
    "cs": "values-cs",
    "zh-Hans": "values-zh-rCN",
}
XCSTRINGS = [
    ROOT / "iosApp" / "OmniGart" / "Localizable.xcstrings",
    ROOT / "watchApp" / "OmniGartWatch" / "Localizable.xcstrings",
]


def equip_ids_from_defs() -> list[str]:
    text = DEFS.read_text(encoding="utf-8")
    return re.findall(r'EquipmentDefinition\.of\("([a-z0-9_]+)"', text)


def load_translations() -> dict[str, dict[str, str]]:
    data = json.loads(DATA.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise SystemExit(f"{DATA}: expected object keyed by equipment id")
    return data


def escape_android(value: str) -> str:
    return (
        value.replace("\\", "\\\\")
        .replace("'", r"\'")
        .replace('"', r"\"")
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    )


def escape_json_string(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)[1:-1]


def upsert_android_xml(path: Path, pairs: dict[str, str]) -> tuple[int, int]:
    if not path.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        text = "<resources>\n</resources>\n"
    else:
        text = path.read_text(encoding="utf-8")
    updated = added = 0
    missing: list[str] = []
    for key, value in pairs.items():
        esc = escape_android(value)
        line = f'    <string name="{key}">{esc}</string>'
        pattern = re.compile(
            rf'    <string name="{re.escape(key)}">.*?</string>', re.DOTALL
        )
        m = pattern.search(text)
        if m:
            if m.group(0) != line:
                text = text[: m.start()] + line + text[m.end() :]
                updated += 1
        else:
            missing.append(line)
            added += 1
    if missing:
        block = "\n".join(missing) + "\n"
        text = text.replace("</resources>", block + "</resources>")
    path.write_text(text, encoding="utf-8")
    return updated, added


def upsert_flat_json(path: Path, pairs: dict[str, str]) -> tuple[int, int]:
    """In-place value replace for existing keys; append new keys before closing brace."""
    text = path.read_text(encoding="utf-8")
    data = json.loads(text)
    updated = added = 0
    for key, value in pairs.items():
        if key in data:
            if data[key] == value:
                continue
            # Replace only the JSON string value for this key (first occurrence).
            pattern = re.compile(
                rf'("{re.escape(key)}"\s*:\s*)"(?:\\.|[^"\\])*"',
            )
            repl = rf'\1"{escape_json_string(value)}"'
            new_text, n = pattern.subn(repl, text, count=1)
            if n != 1:
                raise SystemExit(f"{path}: failed to rewrite {key}")
            text = new_text
            data[key] = value
            updated += 1
        else:
            data[key] = value
            added += 1

    if added:
        # Re-serialize preserving prior key order + append new keys sorted.
        original_order = list(json.loads(path.read_text(encoding="utf-8")).keys())
        # After in-place edits, re-load from text for updated values.
        data = json.loads(text)
        new_keys = sorted(k for k in pairs if k not in original_order)
        ordered = {k: data[k] for k in original_order}
        for k in new_keys:
            ordered[k] = pairs[k]
        text = json.dumps(ordered, ensure_ascii=False, indent=2) + "\n"
    path.write_text(text, encoding="utf-8")
    return updated, added


def upsert_xcstrings(path: Path, pairs_by_locale: dict[str, dict[str, str]]) -> tuple[int, int]:
    catalog = json.loads(path.read_text(encoding="utf-8"))
    strings = catalog.setdefault("strings", {})
    original_keys = list(strings.keys())
    updated = added = 0
    for key, locales in pairs_by_locale.items():
        if key in strings:
            entry = strings[key]
            locs = entry.setdefault("localizations", {})
            changed = False
            for locale, value in locales.items():
                cur = (
                    (locs.get(locale) or {})
                    .get("stringUnit", {})
                    .get("value")
                )
                if cur != value:
                    locs[locale] = {
                        "stringUnit": {"state": "translated", "value": value}
                    }
                    changed = True
            if changed:
                updated += 1
        else:
            strings[key] = {
                "extractionState": "manual",
                "localizations": {
                    locale: {"stringUnit": {"state": "translated", "value": value}}
                    for locale, value in locales.items()
                },
            }
            added += 1
    new_order = {k: strings[k] for k in original_keys if k in strings}
    for k in sorted(k for k in strings if k not in original_keys):
        new_order[k] = strings[k]
    catalog["strings"] = new_order
    path.write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return updated, added


def main() -> int:
    ids = equip_ids_from_defs()
    xl = load_translations()
    missing = [eid for eid in ids if eid not in xl]
    if missing:
        print(f"Missing translations for: {missing[:20]}…", file=sys.stderr)
        return 1

    for eid in ids:
        entry = xl[eid]
        for loc in LOCALES:
            val = entry.get(loc)
            if not isinstance(val, str) or not val.strip():
                print(f"{eid}: missing/empty {loc}", file=sys.stderr)
                return 1
        if "ru" in entry:
            print(f"{eid}: forbidden locale ru", file=sys.stderr)
            return 1

    by_key: dict[str, dict[str, str]] = {
        f"equipment_item_{eid}": {loc: xl[eid][loc] for loc in LOCALES} for eid in ids
    }

    for loc in LOCALES:
        path = ROOT / "shared/src/commonMain/resources/i18n" / loc / "strings.json"
        pairs = {key: locales[loc] for key, locales in by_key.items()}
        upsert_flat_json(path, pairs)

    for loc in LOCALES:
        folder = ANDROID_FOLDERS[loc]
        pairs = {key: locales[loc] for key, locales in by_key.items()}
        targets = [
            ROOT / "androidApp/src/main/res" / folder / "strings.xml",
            ROOT / "wearApp/src/main/res" / folder / "strings.xml",
        ]
        if loc == "en-GB":
            targets.extend(
                [
                    ROOT / "androidApp/src/main/res/values/strings.xml",
                    ROOT / "wearApp/src/main/res/values/strings.xml",
                ]
            )
        for target in targets:
            use = (
                {k: by_key[k]["en-GB"] for k in by_key}
                if target.name == "strings.xml"
                and target.parent.name == "values"
                else pairs
            )
            upsert_android_xml(target, use)

    for xc in XCSTRINGS:
        if xc.exists():
            upsert_xcstrings(xc, by_key)

    print(
        f"Synced {len(by_key)} equipment_item_* keys × {len(LOCALES)} locales "
        f"(order-preserving) → shared JSON, Android, Wear, iOS + watch xcstrings"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
