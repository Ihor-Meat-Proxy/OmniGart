#!/usr/bin/env python3
"""Sync equipment_item_* labels (11 locales) into shared JSON, xcstrings, Android, Wear.

Source of truth: scripts/data/equipment_item_i18n.json (ids from EquipmentDefinitions.kt).
Keeps phone, watch, Wear, and shared i18n from drifting on equipment names.

The watch UI builds keys dynamically as equipment_item_<id>; those must exist in
watchApp/OmniGartWatch/Localizable.xcstrings or SwiftUI shows the raw key.
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


def upsert_android_xml(path: Path, pairs: dict[str, str]) -> None:
    if not path.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        text = "<resources>\n</resources>\n"
    else:
        text = path.read_text(encoding="utf-8")
    for key, value in pairs.items():
        line = f'    <string name="{key}">{escape_android(value)}</string>'
        pattern = re.compile(
            rf'    <string name="{re.escape(key)}">.*?</string>', re.DOTALL
        )
        if pattern.search(text):
            text = pattern.sub(line, text)
        else:
            text = text.replace("</resources>", f"{line}\n</resources>")
    path.write_text(text, encoding="utf-8")


def upsert_xcstrings(path: Path, pairs_by_locale: dict[str, dict[str, str]]) -> None:
    catalog = json.loads(path.read_text(encoding="utf-8"))
    strings = catalog.setdefault("strings", {})
    for key, locales in pairs_by_locale.items():
        entry = strings.setdefault(
            key, {"extractionState": "manual", "localizations": {}}
        )
        locs = entry.setdefault("localizations", {})
        for locale, value in locales.items():
            locs[locale] = {"stringUnit": {"state": "translated", "value": value}}
    path.write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )


def main() -> int:
    ids = equip_ids_from_defs()
    xl = load_translations()
    missing = [eid for eid in ids if eid not in xl]
    if missing:
        print(f"Missing translations for: {missing[:20]}…", file=sys.stderr)
        return 1
    extra = sorted(set(xl) - set(ids))
    if extra:
        print(f"Warning: extra ids in data not in defs: {extra[:10]}", file=sys.stderr)

    # Validate locale coverage + no empty + no ru
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

    # Shared JSON
    for loc in LOCALES:
        path = ROOT / "shared/src/commonMain/resources/i18n" / loc / "strings.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        for key, locales in by_key.items():
            data[key] = locales[loc]
        path.write_text(
            json.dumps(dict(sorted(data.items())), ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

    # Android + Wear XML (full catalogue — Wear previously only had the legacy 22)
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
            upsert_android_xml(target, pairs)

    # iOS + watch String Catalogs
    for xc in XCSTRINGS:
        if xc.exists():
            upsert_xcstrings(xc, by_key)

    print(
        f"Synced {len(by_key)} equipment_item_* keys × {len(LOCALES)} locales "
        f"→ shared JSON, Android, Wear, iOS + watch xcstrings"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
