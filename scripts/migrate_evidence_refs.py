#!/usr/bin/env python3
"""Migrate exercise/template evidenceRefs from raw DOI/PMID/textbook ids to evidenceId."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAP = json.loads((ROOT / "content/evidence/legacy-id-map.json").read_text(encoding="utf-8"))["map"]
SUPPORTS = {
    "technique",
    "safety",
    "hypertrophy",
    "strength",
    "endurance",
    "mobility",
    "programming",
    "biomechanics",
    "emg",
}


def resolve(kind: str, raw_id: str) -> str:
    for key in (raw_id, f"{kind}:{raw_id}"):
        if key in MAP:
            return MAP[key]
    raise KeyError(f"No evidence id mapping for kind={kind!r} id={raw_id!r}")


def migrate_refs(refs: list[dict]) -> list[dict]:
    out: list[dict] = []
    seen: set[str] = set()
    for ref in refs:
        if "evidenceId" in ref and "kind" not in ref:
            eid = ref["evidenceId"]
            supports = [s for s in ref.get("supports", []) if s in SUPPORTS]
            if eid not in seen:
                out.append({"evidenceId": eid, "supports": supports or ["programming"]})
                seen.add(eid)
            continue
        kind = ref["kind"]
        eid = resolve(kind, ref["id"])
        supports = [s for s in ref.get("supports", []) if s in SUPPORTS]
        if eid in seen:
            # merge supports
            for item in out:
                if item["evidenceId"] == eid:
                    item["supports"] = sorted(set(item["supports"]) | set(supports))
            continue
        out.append({"evidenceId": eid, "supports": supports or ["programming"]})
        seen.add(eid)
    return out


def migrate_file(path: Path) -> bool:
    data = json.loads(path.read_text(encoding="utf-8"))
    if "evidenceRefs" not in data:
        return False
    new_refs = migrate_refs(data["evidenceRefs"])
    if new_refs == data["evidenceRefs"]:
        return False
    data["evidenceRefs"] = new_refs
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return True


def main() -> None:
    changed = 0
    for folder in (
        ROOT / "content/exercises/data",
        ROOT / "content/exercises/templates",
    ):
        for path in sorted(folder.glob("*.json")):
            if migrate_file(path):
                changed += 1
                print(f"migrated {path.relative_to(ROOT)}")
    print(f"files changed: {changed}")


if __name__ == "__main__":
    main()
