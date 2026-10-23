#!/usr/bin/env python3
"""Extract en-GB catalog from exercise data + templates + existing name strings."""
from __future__ import annotations

import argparse
import json
from collections import OrderedDict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EXERCISES = ROOT / "content/exercises"
DATA = EXERCISES / "data"
TEMPLATES = EXERCISES / "templates"
LOCALES = EXERCISES / "locales"


def sid(value: str) -> str:
    return value.replace("-", "_")


def extract() -> OrderedDict[str, str]:
    catalog: OrderedDict[str, str] = OrderedDict()
    names_path = LOCALES / "en-GB.json"
    if names_path.exists():
        existing = json.loads(names_path.read_text(encoding="utf-8"))
        # Prefer existing .name keys; rebuild body keys from data.
        for key, value in sorted(existing.items()):
            if key.endswith(".name"):
                catalog[key] = value

    for path in sorted(DATA.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        base = f"exercise.{sid(data['id'])}"
        name_key = f"{base}.name"
        if name_key not in catalog:
            raise SystemExit(f"Missing name key in en-GB for {data['id']}")
        for i, step in enumerate(data.get("technique", [])):
            catalog[f"{base}.technique.{i}"] = step
        for i, step in enumerate(data.get("setup", [])):
            catalog[f"{base}.setup.{i}"] = step
        if data.get("breathing"):
            catalog[f"{base}.breathing"] = data["breathing"]
        for i, step in enumerate(data.get("commonMistakes", [])):
            catalog[f"{base}.common_mistakes.{i}"] = step
        for i, step in enumerate(data.get("safety", [])):
            catalog[f"{base}.safety.{i}"] = step
        for i, step in enumerate(data.get("contraindications", [])):
            catalog[f"{base}.contraindications.{i}"] = step
        for link in data.get("regressions", []):
            catalog[f"{base}.regressions.{sid(link['exerciseId'])}.note"] = link["note"]
        for link in data.get("progressions", []):
            catalog[f"{base}.progressions.{sid(link['exerciseId'])}.note"] = link["note"]

    for path in sorted(TEMPLATES.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        tbase = f"template.{sid(data['id'])}"
        if data.get("rationale"):
            catalog[f"{tbase}.rationale"] = data["rationale"]
        for session in data.get("sessions", []):
            for block in session.get("blocks", []):
                for idx, item in enumerate(block.get("items", [])):
                    note = item.get("notes")
                    if note:
                        catalog[
                            f"{tbase}.session.{session['id']}.item.{idx}.notes"
                        ] = note

    # Keep any leftover non-name template keys already present
    if names_path.exists():
        existing = json.loads(names_path.read_text(encoding="utf-8"))
        for key, value in sorted(existing.items()):
            if key not in catalog:
                catalog[key] = value

    return catalog


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--write",
        action="store_true",
        help="Write content/exercises/locales/en-GB.json",
    )
    args = parser.parse_args()
    catalog = extract()
    print(f"keys={len(catalog)}")
    if args.write:
        LOCALES.mkdir(parents=True, exist_ok=True)
        (LOCALES / "en-GB.json").write_text(
            json.dumps(catalog, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        print(f"wrote {LOCALES / 'en-GB.json'}")


if __name__ == "__main__":
    main()
